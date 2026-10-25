package androidrelay

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"errors"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
	"time"
)

type streamFunc func(context.Context, net.Conn, string, func() error) error
func (f streamFunc) Stream(ctx context.Context, c net.Conn, target string, ready func() error) error { return f(ctx,c,target,ready) }

func fixture(t *testing.T, handler http.HandlerFunc) (*httptest.Server, *tls.Config) {
	t.Helper()
	server := httptest.NewTLSServer(handler); t.Cleanup(server.Close)
	roots := x509.NewCertPool(); roots.AddCert(server.Certificate())
	return server, &tls.Config{RootCAs: roots, MinVersion: tls.VersionTLS12}
}

func bridge(server *httptest.Server, calls *atomic.Int32, target *string) Streamer {
	return streamFunc(func(ctx context.Context, c net.Conn, address string, ready func() error) error {
		calls.Add(1); *target = address
		remote,err := (&net.Dialer{}).DialContext(ctx,"tcp",server.Listener.Addr().String())
		if err != nil { return err }; defer remote.Close(); defer c.Close()
		if err = ready(); err != nil { return err }
		done := make(chan struct{}); defer close(done)
		go func(){ select{case <-ctx.Done(): remote.Close(); c.Close(); case <-done:} }()
		go io.Copy(remote,c)
		_,err=io.Copy(c,remote); return err
	})
}

func TestVerified204UsesOnlyRelayedStream(t *testing.T) {
	server, roots := fixture(t,func(w http.ResponseWriter,r *http.Request){
		if r.URL.Path != "/generate_204" || r.Method != "GET" { t.Error("wrong relay request") }
		if r.Header.Get("Cache-Control") != "no-cache, no-store" {t.Error("missing cache prevention")}
		w.WriteHeader(204)
	})
	var calls atomic.Int32; var target string
	// Ambient HTTP proxies must never bypass the authenticated stream.
	t.Setenv("HTTPS_PROXY","http://127.0.0.1:1")
	delay,err:=measure(context.Background(),bridge(server,&calls,&target),server.URL+"/generate_204",roots)
	if err!=nil || delay<time.Millisecond || calls.Load()!=1 || target!=server.Listener.Addr().String(){t.Fatalf("relayed HTTPS failed: %v, calls=%d, target=%q",err,calls.Load(),target)}
}

func TestWrongStatusAndRedirectNeverBecomeSuccess(t *testing.T) {
	for _,status:=range []int{200,302,403,500}{t.Run(http.StatusText(status),func(t *testing.T){
		server,roots:=fixture(t,func(w http.ResponseWriter,r *http.Request){w.Header().Set("Location","https://should-not-be-followed.invalid");w.WriteHeader(status)})
		var calls atomic.Int32;var target string
		if _,err:=measure(context.Background(),bridge(server,&calls,&target),server.URL,roots);!errors.Is(err,ErrStatus)||calls.Load()!=1{t.Fatalf("bad status/redirect accepted: %v",err)}
	})}
}

func TestUntrustedCertificateIsRejected(t *testing.T) {
	server,_:=fixture(t,func(w http.ResponseWriter,r *http.Request){w.WriteHeader(204)})
	var calls atomic.Int32;var target string
	if _,err:=measure(context.Background(),bridge(server,&calls,&target),server.URL,nil);!errors.Is(err,ErrRequest){t.Fatal("untrusted TLS accepted",err)}
}

func TestDialFailureHasNoDirectFallback(t *testing.T) {
	var calls atomic.Int32
	failed:=streamFunc(func(ctx context.Context,c net.Conn,target string,ready func()error)error{calls.Add(1);return errors.New("blocked outbound")})
	if _,err:=Measure(context.Background(),failed);!errors.Is(err,ErrRequest)||calls.Load()!=1{t.Fatal("dial failure not surfaced",err,calls.Load())}
}

func TestTimeoutCancelsStream(t *testing.T) {
	stopped:=make(chan struct{})
	blocked:=streamFunc(func(ctx context.Context,c net.Conn,target string,ready func()error)error{defer close(stopped);<-ctx.Done();return ctx.Err()})
	ctx,cancel:=context.WithTimeout(context.Background(),30*time.Millisecond);defer cancel()
	if _,err:=Measure(ctx,blocked);!errors.Is(err,ErrTimeout){t.Fatal("timeout not surfaced",err)}
	select{case <-stopped:case <-time.After(time.Second):t.Fatal("test leaked its stream")}
}

func TestHeadersTimeoutAndInvalidScheme(t *testing.T) {
	server,roots:=fixture(t,func(w http.ResponseWriter,r *http.Request){<-r.Context().Done()})
	var calls atomic.Int32;var target string
	ctx,cancel:=context.WithTimeout(context.Background(),100*time.Millisecond);defer cancel()
	if _,err:=measure(ctx,bridge(server,&calls,&target),server.URL,roots);!errors.Is(err,ErrTimeout){t.Fatal(err)}
	if _,err:=measure(context.Background(),nil,"http://example.invalid",nil);!errors.Is(err,ErrRequest){t.Fatal("HTTP accepted",err)}
}
