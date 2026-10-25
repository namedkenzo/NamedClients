// Package androidrelay measures Android profile reachability through its
// authenticated tunnel. It is not linked into the Windows client.
package androidrelay

import (
	"context"
	"crypto/tls"
	"errors"
	"io"
	"net"
	"net/http"
	"sync"
	"time"
)

const Endpoint = "https://www.gstatic.com/generate_204"
const Timeout = 15 * time.Second

var ErrTimeout = errors.New("relay test timed out")
var ErrRequest = errors.New("relay HTTPS request failed; check tunnel, outbound and TLS access")
var ErrStatus = errors.New("relay endpoint did not return HTTP 204")

type Streamer interface {
	Stream(context.Context, net.Conn, string, func() error) error
}

// Measure includes the relayed TCP connection, verified TLS handshake and HTTP
// response headers. Authentication is completed by the caller before this timer.
// No system proxy, local target DNS lookup, redirect or direct fallback is used.
func Measure(ctx context.Context, tunnel Streamer) (time.Duration, error) {
	config, err := deviceTLSConfig()
	if err != nil { return 0, ErrRequest }
	return measure(ctx, tunnel, Endpoint, config)
}

// ValidateTLSRoots is used by the native Android acceptance self-test only.
func ValidateTLSRoots() error { _, err := deviceTLSConfig(); return err }

func measure(parent context.Context, tunnel Streamer, endpoint string, tlsConfig *tls.Config) (time.Duration, error) {
	ctx, cancel := context.WithTimeout(parent, Timeout)
	defer cancel()
	transport := &http.Transport{
		Proxy: nil,
		DialContext: func(dialCtx context.Context, network, address string) (net.Conn, error) {
			return dialStream(dialCtx, ctx, tunnel, address)
		},
		TLSClientConfig: tlsConfig,
		DisableKeepAlives: true,
		ForceAttemptHTTP2: false,
		MaxResponseHeaderBytes: 8192,
	}
	defer transport.CloseIdleConnections()
	client := &http.Client{Transport: transport, CheckRedirect: func(*http.Request, []*http.Request) error {
		return http.ErrUseLastResponse
	}}
	request, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil || request.URL.Scheme != "https" {
		return 0, ErrRequest
	}
	request.Header.Set("Cache-Control", "no-cache, no-store")
	request.Header.Set("User-Agent", "namedP-Android/relay-delay")
	started := time.Now()
	response, err := client.Do(request)
	if err != nil {
		if ctx.Err() != nil { return 0, ErrTimeout }
		return 0, ErrRequest
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusNoContent { return 0, ErrStatus }
	return max(time.Millisecond, time.Since(started)), nil
}

type streamConn struct { net.Conn; cancel context.CancelFunc }
func (c *streamConn) Close() error { c.cancel(); return c.Conn.Close() }

func dialStream(dialCtx, measurementCtx context.Context, tunnel Streamer, address string) (net.Conn, error) {
	local, remote := net.Pipe()
	streamCtx, cancel := context.WithCancel(measurementCtx)
	ready := make(chan error, 1)
	var once sync.Once
	emit := func(err error) { once.Do(func() { ready <- err }) }
	go func() {
		defer remote.Close()
		err := tunnel.Stream(streamCtx, remote, address, func() error { emit(nil); return nil })
		if err == nil { err = io.EOF }
		emit(err)
	}()
	select {
	case err := <-ready:
		if err == nil { return &streamConn{Conn: local, cancel: cancel}, nil }
		cancel(); local.Close(); return nil, err
	case <-dialCtx.Done():
		cancel(); local.Close(); return nil, dialCtx.Err()
	case <-measurementCtx.Done():
		cancel(); local.Close(); return nil, measurementCtx.Err()
	}
}
