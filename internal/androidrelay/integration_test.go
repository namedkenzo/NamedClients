//go:build namedicmp_integration

package androidrelay

import (
	"context"
	"fmt"
	"net/http"
	"path/filepath"
	"sync/atomic"
	"testing"
	"time"
	"namedp/internal/client"
	"namedp/internal/core"
	"namedp/internal/store"
	"namedp/internal/wire"
)

// The panel module is overlaid in CI only; neither runtime imports it.
func TestRelayHTTPSOverActualICMPTunnel(t *testing.T) {
	for _,family:=range []int{4,6}{for _,mode:=range []string{"none","aes256"}{t.Run(fmt.Sprintf("IPv%d/%s",family,mode),func(t *testing.T){
		ctx,cancel:=context.WithTimeout(context.Background(),20*time.Second);defer cancel()
		db,err:=store.Open(filepath.Join(t.TempDir(),"state.json"));if err!=nil{t.Fatal(err)};defer db.Close()
		user,err:=db.PutUser(store.User{Name:"relay-test",Enabled:true,Encryption:mode});if err!=nil{t.Fatal(err)}
		transport,_,err:=core.ListenDual("127.0.0.1","::1","raw_ip");if err!=nil{t.Fatal(err)}
		conf:=store.DefaultConfig();conf.AllowPrivate=true
		server,err:=core.New(db,conf,transport);if err!=nil{t.Fatal(err)};defer server.Stop();go server.Serve(ctx)
		host:="127.0.0.1";if family==6{host="::1"}
		tunnel,err:=client.Open(ctx,client.Profile{Host:host,Family:family,Mode:"proxy",Credential:wire.Credential{Key:user.Key,Mode:mode,Secret:user.EncryptionKey}});if err!=nil{t.Fatal(err)};defer tunnel.Close()
		if err=tunnel.Probe(ctx);err!=nil{t.Fatal(err)}
		var hits atomic.Int32
		https,roots:=fixture(t,func(w http.ResponseWriter,r *http.Request){hits.Add(1);w.WriteHeader(204)})
		if delay,err:=measure(ctx,tunnel,https.URL+"/generate_204",roots);err!=nil||delay<time.Millisecond||hits.Load()!=1{t.Fatalf("HTTPS did not traverse authenticated ICMP: %v hits=%d",err,hits.Load())}
		if tunnel.Upload.Load()==0||tunnel.Download.Load()==0{t.Fatal("relay bypassed tunnel accounting")}
	})}}
}
