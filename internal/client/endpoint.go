package client

import (
 "context"
 "errors"
 "net"
)
func resolveEndpoint(ctx context.Context,p Profile)([]net.IP,int,error){
 family:=p.Family;if family==0{family=4;if ip:=net.ParseIP(p.Host);ip!=nil&&ip.To4()==nil{family=6}}
 network:="ip4";if family==6{network="ip6"}
 ips,err:=net.DefaultResolver.LookupIP(ctx,network,p.Host)
 if err!=nil||len(ips)==0 {return nil,family,errors.New("cannot resolve endpoint in the selected IP family; check its A/AAAA record and connectivity")}
 return ips,family,nil
}
