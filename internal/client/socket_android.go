package client

import (
 "context"
 "crypto/rand"
 "errors"
 "fmt"
 "net"
 "sync"
 "time"

 "golang.org/x/sys/unix"
 "namedp/internal/wire"
)

// Android ping sockets do not require CAP_NET_RAW. The kernel assigns their
// echo identifier; using an unrelated random ID would discard valid replies.
// The VpnService excludes its own UID before establishing the device route.
type androidPing struct { mu sync.RWMutex; fd int;family int }

func (c *androidPing) ReadFromIP(b []byte)(int,*net.IPAddr,error) {
 for {
  c.mu.RLock()
  if c.fd < 0 { c.mu.RUnlock(); return 0,nil,net.ErrClosed }
  n,from,err:=unix.Recvfrom(c.fd,b,0)
  c.mu.RUnlock()
  if err==unix.EAGAIN || err==unix.EWOULDBLOCK || err==unix.EINTR {continue}
  if err!=nil{return 0,nil,err}
  switch addr:=from.(type){case *unix.SockaddrInet4:return n,&net.IPAddr{IP:net.IP(addr.Addr[:])},nil;case *unix.SockaddrInet6:return n,&net.IPAddr{IP:net.IP(addr.Addr[:])},nil}
 }
}
func(c *androidPing) WriteToIP(b []byte,peer *net.IPAddr)(int,error){
 c.mu.RLock();defer c.mu.RUnlock();if c.fd<0{return 0,net.ErrClosed}
 if c.family==6{
  ip:=peer.IP.To16();if ip==nil||peer.IP.To4()!=nil{return 0,errors.New("IPv6 ICMP endpoint required")}
  addr:=&unix.SockaddrInet6{};copy(addr.Addr[:],ip);if err:=unix.Sendto(c.fd,b,0,addr);err!=nil{return 0,err};return len(b),nil
 }
 ip:=peer.IP.To4();if ip==nil{return 0,errors.New("IPv4 ICMP endpoint required")}
 addr:=&unix.SockaddrInet4{};copy(addr.Addr[:],ip)
 if err:=unix.Sendto(c.fd,b,0,addr);err!=nil{return 0,err};return len(b),nil
}
func(c *androidPing) Close()error{c.mu.Lock();defer c.mu.Unlock();if c.fd<0{return nil};fd:=c.fd;c.fd=-1;return unix.Close(fd)}

func OpenAndroid(ctx context.Context,p Profile)(*Tunnel,error){
 ips,family,err:=resolveEndpoint(ctx,p);if err!=nil{return nil,err}
 domain,protocol:=unix.AF_INET,unix.IPPROTO_ICMP;if family==6{domain,protocol=unix.AF_INET6,unix.IPPROTO_ICMPV6}
 fd,err:=unix.Socket(domain,unix.SOCK_DGRAM|unix.SOCK_CLOEXEC,protocol)
 if err!=nil{return nil,fmt.Errorf("this Android device does not allow rootless ICMP ping sockets: %w",err)}
 c:=&androidPing{fd:fd,family:family}
 fail:=func(e error)(*Tunnel,error){c.Close();return nil,e}
 var bind unix.Sockaddr=&unix.SockaddrInet4{};if family==6{bind=&unix.SockaddrInet6{}}
 if err=unix.Bind(fd,bind);err!=nil{return fail(err)}
 from,err:=unix.Getsockname(fd);if err!=nil{return fail(err)}
 port:=0;switch addr:=from.(type){case *unix.SockaddrInet4:port=addr.Port;case *unix.SockaddrInet6:port=addr.Port}
 if port==0{return fail(errors.New("kernel did not allocate an ICMP identifier"))}
 if err=unix.SetsockoptTimeval(fd,unix.SOL_SOCKET,unix.SO_RCVTIMEO,&unix.Timeval{Sec:1});err!=nil{return fail(err)}
 if err=unix.SetsockoptTimeval(fd,unix.SOL_SOCKET,unix.SO_SNDTIMEO,&unix.Timeval{Sec:3});err!=nil{return fail(err)}
 _=unix.SetsockoptInt(fd,unix.SOL_SOCKET,unix.SO_RCVBUF,4<<20)
 _=unix.SetsockoptInt(fd,unix.SOL_SOCKET,unix.SO_SNDBUF,4<<20)
 sec:=wire.NewSecurity();if err=sec.SetCredentials([]wire.Credential{p.Credential});err!=nil{return fail(err)}
 // Assert crypto/rand availability early rather than failing in a live flow.
 var entropy [1]byte;if _,err=rand.Read(entropy[:]);err!=nil{return fail(err)}
 t:=&Tunnel{conn:c,peer:&net.IPAddr{IP:ips[0]},security:sec,key:p.Credential.Key,id:uint16(port),flows:map[string]chan wire.Message{},probe:make(chan wire.Message,8),done:make(chan struct{}),family:family,activityStart:time.Now()}
 go t.read();go func(){select{case <-ctx.Done():t.Close();case <-t.done:}}()
 return t,nil
}

