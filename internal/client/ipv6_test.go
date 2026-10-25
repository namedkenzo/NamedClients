package client
import("context";"net";"os";"testing";"time";"namedp/internal/wire")
func TestEndpointFamily(t *testing.T){
 for _,link:=range []string{"pingtunnel://[2001:db8::1]?key=123","pingtunnel://vpn.example.com?key=123&family=ipv6"}{p,err:=ParseProfile(link);if err!=nil||p.Family!=6{t.Fatal("IPv6 profile failed",err)}}
 for _,link:=range []string{"pingtunnel://192.0.2.1?key=123&family=ipv6","pingtunnel://[fe80::1%25eth0]?key=123","pingtunnel://example.com?key=123&family=bad"}{if _,err:=ParseProfile(link);err==nil{t.Fatal("invalid family accepted")}}
}
func TestRawIPv6LoopbackHandshake(t *testing.T){
 if os.Getenv("NAMEDP_RAW_TESTS")!="1"{t.Skip("explicit capability-gated raw socket test")}
 server,err:=net.ListenIP("ip6:ipv6-icmp",&net.IPAddr{IP:net.IPv6loopback});if err!=nil{t.Fatal(err)};defer server.Close()
 ctx,cancel:=context.WithTimeout(context.Background(),8*time.Second);defer cancel()
 go func(){buffer:=make([]byte,65535);for{n,peer,err:=server.ReadFromIP(buffer);if err!=nil{return};typ,id,seq,payload,err:=wire.DecodeEcho6(buffer[:n]);if err!=nil||typ!=128{continue};m,err:=wire.DecodeMessage(payload);if err!=nil||m.Key!=123||m.Kind!=1{continue};m.ReplyType=-1;_,_=server.WriteToIP(wire.Echo6(129,id,seq,wire.EncodeMessage(m)),peer)}}()
 p,err:=ParseProfile("pingtunnel://[::1]?key=123");if err!=nil{t.Fatal(err)};tunnel,err:=Open(ctx,p);if err!=nil{t.Fatal(err)};defer tunnel.Close()
 if err=tunnel.Probe(ctx);err!=nil{t.Fatal(err)}
}
