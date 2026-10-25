package main

import (
 "context"
 "encoding/json"
 "errors"
 "fmt"
 "io"
 "net"
 "os"
 "os/exec"
 "strings"
 "sync"
 "time"

 "golang.org/x/sys/unix"
 "namedp/internal/client"
 "namedp/internal/androidrelay"
)

type config struct { Link string `json:"link"`; Mode string `json:"mode"`; TunSocket string `json:"tun_socket"`; TunBinary string `json:"tun_binary"`; ProbeOnly bool `json:"probe_only"`; RelayDelayOnly bool `json:"relay_delay_only"`;GamingMode bool `json:"gaming_mode"` }
func main(){
 if len(os.Args)==2 && os.Args[1]=="--version"{fmt.Println("0.2.2-alpha");return}
 if len(os.Args)==2 && os.Args[1]=="--self-test-relay-roots"{
  if err:=androidrelay.ValidateTLSRoots();err!=nil{fmt.Fprintln(os.Stderr,err);os.Exit(1)}
  fmt.Println("relay-roots-ready");return
 }
 if len(os.Args)==2 && (os.Args[1]=="--self-test-socket"||os.Args[1]=="--self-test-socket6"){
  link:="pingtunnel://127.0.0.1?key=123";if os.Args[1]=="--self-test-socket6"{link="pingtunnel://[::1]?key=123"}
  p,e:=client.ParseProfile(link)
  if e==nil{var t *client.Tunnel;t,e=client.OpenAndroid(context.Background(),p);if e==nil{t.Close()}}
  if e!=nil{fmt.Fprintln(os.Stderr,e);os.Exit(1)};fmt.Println("ping-socket-ready");return
 }
 if e:=run();e!=nil{fmt.Fprintln(os.Stderr,e);os.Exit(1)}
}
func run()error{
 var input config
 dec:=json.NewDecoder(io.LimitReader(os.Stdin,16384));dec.DisallowUnknownFields();if e:=dec.Decode(&input);e!=nil{return errors.New("invalid engine request")}
 p,e:=client.ParseProfile(input.Link);if e!=nil{return e};if input.Mode!=p.Mode{return errors.New("engine mode must match the profile")}
 ctx,cancel:=context.WithCancel(context.Background());defer cancel()
 if input.RelayDelayOnly { var stop context.CancelFunc;ctx,stop=context.WithTimeout(ctx,25*time.Second);defer stop() }
 go func(){io.Copy(io.Discard,os.Stdin);cancel()}()
 tunnel,e:=client.OpenAndroid(ctx,p);if e!=nil{return e};defer tunnel.Close()
 if input.GamingMode{tunnel.EnableGamingMode()}
 started:=time.Now();probe,stop:=context.WithTimeout(ctx,8*time.Second);e=tunnel.Probe(probe);stop();if e!=nil{return e}
 if input.ProbeOnly {return json.NewEncoder(os.Stdout).Encode(map[string]any{"event":"probe_ok","latency_ms":time.Since(started).Milliseconds()})}
 if input.RelayDelayOnly {
  delay,err:=androidrelay.Measure(ctx,tunnel);if err!=nil{return err}
  return json.NewEncoder(os.Stdout).Encode(map[string]any{"event":"relay_delay","url":androidrelay.Endpoint,"status":204,"latency_ms":delay.Milliseconds()})
 }
 var mu sync.Mutex;emit:=func(v any){mu.Lock();defer mu.Unlock();_ =json.NewEncoder(os.Stdout).Encode(v)}
 errorsCh:=make(chan error,2)
 go func(){tick:=time.NewTicker(time.Second);defer tick.Stop();for{select{case <-ctx.Done():return;case <-tick.C:emit(map[string]any{"event":"stats","upload":tunnel.Upload.Load(),"download":tunnel.Download.Load()})}}}()
 go func(){tick:=time.NewTicker(5*time.Second);defer tick.Stop();for{select{case <-ctx.Done():return;case <-tick.C:probe,done:=context.WithTimeout(ctx,8*time.Second);err:=tunnel.Probe(probe);done();if err!=nil{tunnel.Close();return}}}}()
 var tun *exec.Cmd
 defer func(){if tun!=nil && tun.Process!=nil{_ =tun.Process.Kill()}}()
 e=tunnel.ServeSOCKS(ctx,p.Port,func(){
  emit(map[string]any{"event":"proxy_ready","port":p.Port,"server_ip":tunnel.ServerIP()})
  if input.Mode=="vpn"{
   fd,err:=receiveTun(input.TunSocket);if err!=nil{errorsCh<-err;cancel();return}
   if !strings.HasPrefix(input.TunBinary,"/") || !strings.HasSuffix(input.TunBinary,"/libtun2socks.so"){fd.Close();errorsCh<-errors.New("invalid TUN executable");cancel();return}
   tun=exec.Command(input.TunBinary,"-device","fd://0","-proxy",fmt.Sprintf("socks5://127.0.0.1:%d",p.Port),"-loglevel","silent")
   tun.Stdin=fd;tun.Stdout=os.Stderr;tun.Stderr=os.Stderr
   err=tun.Start();fd.Close();if err!=nil{errorsCh<-fmt.Errorf("TUN startup failed: %w",err);cancel();return}
   exited:=make(chan error,1);go func(){exited<-tun.Wait()}()
   select{case err:=<-exited:errorsCh<-fmt.Errorf("TUN stopped: %v",err);cancel();return;case <-time.After(300*time.Millisecond):}
   go func(){select{case <-exited:cancel();case <-ctx.Done():}}()
  }
  emit(map[string]any{"event":"ready","port":p.Port,"server_ip":tunnel.ServerIP()})
 })
 select{case err:=<-errorsCh:return err;default:}
 if ctx.Err()!=nil{return nil};return e
}
func receiveTun(name string)(*os.File,error){
 if !strings.HasPrefix(name,"namedp-") || len(name)>100{return nil,errors.New("invalid descriptor socket")}
 c,e:=net.DialUnix("unix",nil,&net.UnixAddr{Name:"@"+name,Net:"unix"});if e!=nil{return nil,e};defer c.Close();_ =c.SetReadDeadline(time.Now().Add(12*time.Second))
 data:=make([]byte,1);oob:=make([]byte,unix.CmsgSpace(4));_,n,flags,_,e:=c.ReadMsgUnix(data,oob);if e!=nil{return nil,e}
 if flags&unix.MSG_CTRUNC!=0{return nil,errors.New("truncated TUN descriptor")}
 msgs,e:=unix.ParseSocketControlMessage(oob[:n]);if e!=nil{return nil,e}
 var fds []int;for _,m:=range msgs{rights,err:=unix.ParseUnixRights(&m);if err==nil{fds=append(fds,rights...)}}
 if len(fds)!=1{for _,fd:=range fds{unix.Close(fd)};return nil,errors.New("one TUN descriptor required")}
 unix.CloseOnExec(fds[0]);return os.NewFile(uintptr(fds[0]),"namedP-tun"),nil
}
