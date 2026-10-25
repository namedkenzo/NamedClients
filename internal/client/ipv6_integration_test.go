//go:build namedicmp_integration

package client

import (
 "bytes"
 "context"
 "io"
 "net"
 "path/filepath"
 "testing"
 "time"
 "namedp/internal/core"
 "namedp/internal/store"
 "namedp/internal/wire"
)
// CI overlays this test into the web-panel module. No panel dependency is
// linked into the client module or either distributed client application.
func TestIPv6PanelTCPUDPAndReaders(t *testing.T){
 for _,reader:=range []string{"raw_ip","raw_packet"}{for _,mode:=range []string{"none","aes256"}{t.Run(reader+"/"+mode,func(t *testing.T){
  ctx,cancel:=context.WithTimeout(context.Background(),20*time.Second);defer cancel()
  db,err:=store.Open(filepath.Join(t.TempDir(),"state.json"));if err!=nil{t.Fatal(err)};defer db.Close()
  user,err:=db.PutUser(store.User{Name:"IPv6-loopback",Enabled:true,Encryption:mode});if err!=nil{t.Fatal(err)}
  transport,_,err:=core.ListenDual("127.0.0.1","::1",reader);if err!=nil{t.Fatal(err)}
  conf:=store.DefaultConfig();conf.AllowPrivate=true;server,err:=core.New(db,conf,transport);if err!=nil{t.Fatal(err)};defer server.Stop();go server.Serve(ctx)
  if server.ReaderMode()!=reader{t.Fatalf("requested reader is not active: %s",server.ReaderWarning())}
  tunnel,err:=Open(ctx,Profile{Host:"::1",Port:1080,Family:6,Mode:"vpn",Credential:wire.Credential{Key:user.Key,Mode:mode,Secret:user.EncryptionKey}});if err!=nil{t.Fatal(err)};defer tunnel.Close()
  if err=tunnel.Probe(ctx);err!=nil{t.Fatal(err)}
  tcp,err:=net.Listen("tcp4","127.0.0.1:0");if err!=nil{t.Fatal(err)};defer tcp.Close()
  go func(){c,e:=tcp.Accept();if e==nil{defer c.Close();_,_=io.Copy(c,c)}}()
  app,local:=net.Pipe();defer app.Close();ready:=make(chan struct{})
  go tunnel.Stream(ctx,local,tcp.Addr().String(),func()error{close(ready);return nil})
  select{case <-ready:case <-ctx.Done():t.Fatal("IPv6 TCP setup timed out")}
  payload:=bytes.Repeat([]byte("IPv6 transport integrity"),4096);go writeAll(app,payload)
  _=app.SetReadDeadline(time.Now().Add(10*time.Second));actual:=make([]byte,len(payload));if _,err=io.ReadFull(app,actual);err!=nil||!bytes.Equal(actual,payload){t.Fatal("IPv6 TCP corrupt/stalled",err)}
  udp,err:=net.ListenUDP("udp4",&net.UDPAddr{IP:net.IPv4(127,0,0,1)});if err!=nil{t.Fatal(err)};defer udp.Close()
  go func(){b:=make([]byte,2000);n,p,e:=udp.ReadFromUDP(b);if e==nil{_,_=udp.WriteToUDP(b[:n],p)}}()
  id,in,err:=tunnel.register();if err!=nil{t.Fatal(err)};defer tunnel.unregister(id,udp.LocalAddr().String(),false)
  if err=tunnel.send(wire.Message{Connection:id,Target:udp.LocalAddr().String(),Payload:[]byte("UDP via ICMPv6")});err!=nil{t.Fatal(err)}
  select{case response:=<-in:if string(response.Payload)!="UDP via ICMPv6"||response.TCP{t.Fatal("IPv6 UDP corrupt")};case <-ctx.Done():t.Fatal("IPv6 UDP timeout")}
 })}}
}
