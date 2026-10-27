package client

import (
	"context"
	"errors"
	"net"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"namedp/internal/wire"
)

func TestHealthDecisionNeedsRepeatedFailuresAndSilence(t *testing.T) {
	policy := watchdogPolicy{failures: 3, silenceGrace: 30 * time.Second}
	var health healthFailures
	if health.failed(false, time.Hour, policy) || health.failed(false, time.Hour, policy) {
		t.Fatal("one or two probe failures killed a tunnel")
	}
	if health.failed(false, time.Second, policy) {
		t.Fatal("authenticated traffic was ignored")
	}
	if !health.failed(false, 30*time.Second, policy) {
		t.Fatal("a persistent blackhole did not fail")
	}
	if health.failed(true, time.Hour, policy) || health.consecutive != 0 {
		t.Fatal("a successful probe did not reset failure state")
	}
	if health.failed(false, time.Hour, policy) {
		t.Fatal("failure state survived recovery")
	}
}

func TestAuthenticatedReceiveAgeUsesElapsedTime(t *testing.T) {
	start := time.Now()
	tunnel := &Tunnel{activityStart: start}
	if age := tunnel.authenticatedReceiveAge(start.Add(20 * time.Second)); age != 20*time.Second {
		t.Fatal(age)
	}
	tunnel.recordAuthenticatedReceive(start.Add(15 * time.Second))
	if age := tunnel.authenticatedReceiveAge(start.Add(20 * time.Second)); age != 5*time.Second {
		t.Fatal(age)
	}
	if age := tunnel.authenticatedReceiveAge(start.Add(14 * time.Second)); age != 0 {
		t.Fatal("a racing newer receive produced a negative age")
	}
}

type healthPacketConn struct {
	input   chan []byte
	closed  chan struct{}
	once    sync.Once
	peer    *net.IPAddr
	onWrite func([]byte)
	writes  atomic.Uint64
}

func (c *healthPacketConn) ReadFromIP(b []byte) (int, *net.IPAddr, error) {
	select {
	case <-c.closed:
		return 0, nil, net.ErrClosed
	case p := <-c.input:
		return copy(b, p), c.peer, nil
	}
}
func (c *healthPacketConn) WriteToIP(b []byte, _ *net.IPAddr) (int, error) {
	c.writes.Add(1)
	if c.onWrite != nil {
		c.onWrite(b)
	}
	return len(b), nil
}
func (c *healthPacketConn) Close() error {
	c.once.Do(func() { close(c.closed) })
	return nil
}

func testHealthTunnel(t *testing.T) (*Tunnel, *healthPacketConn) {
	t.Helper()
	peer := &net.IPAddr{IP: net.IPv4(192, 0, 2, 1)}
	conn := &healthPacketConn{input: make(chan []byte, 64), closed: make(chan struct{}), peer: peer}
	security := wire.NewSecurity()
	if err := security.SetCredentials([]wire.Credential{{Key: 123, Mode: "none"}}); err != nil {
		t.Fatal(err)
	}
	tunnel := &Tunnel{conn: conn, peer: peer, security: security, key: 123, id: 17, family: 4,
		activityStart: time.Now(), flows: make(map[string]chan wire.Message), probe: make(chan wire.Message, 8), done: make(chan struct{})}
	finished := make(chan struct{})
	go func() { defer close(finished); tunnel.read() }()
	t.Cleanup(func() {
		tunnel.Close()
		select {
		case <-finished:
		case <-time.After(time.Second):
			t.Error("receive goroutine leaked")
		}
	})
	return tunnel, conn
}

func enqueueHealthReply(tunnel *Tunnel, conn *healthPacketConn, m wire.Message) {
	m.Key, m.ReplyType = tunnel.key, -1
	encoded, err := tunnel.security.Encode(m)
	if err != nil {
		panic(err)
	}
	packet := wire.Echo(0, tunnel.id, 1, encoded)
	select {
	case conn.input <- packet:
	case <-conn.closed:
	}
}

// Exercise the actual Probe + authenticated receive loop, not only a mocked
// success flag: synthetic probe replies are blackholed while data is received.
func TestWatchHealthKeepsActiveTrafficWhenProbeRepliesAreLost(t *testing.T) {
	tunnel, conn := testHealthTunnel(t)
	conn.onWrite = func([]byte) {
		enqueueHealthReply(tunnel, conn, wire.Message{Connection: "data-flow", Payload: []byte("download remains active")})
	}
	ctx, cancel := context.WithTimeout(context.Background(), 700*time.Millisecond)
	defer cancel()
	err := tunnel.watchHealth(ctx, watchdogPolicy{interval: 5 * time.Millisecond, probeTimeout: 15 * time.Millisecond, silenceGrace: 200 * time.Millisecond, failures: 3})
	if !errors.Is(err, context.DeadlineExceeded) {
		t.Fatalf("lost probe closed a tunnel receiving authenticated data: %v", err)
	}
	if conn.writes.Load() < 3 || tunnel.lastAuthenticatedReceive.Load() == 0 {
		t.Fatal("test did not exercise repeated failed probes and authenticated traffic")
	}
	select {
	case <-tunnel.Done():
		t.Fatal("active transport was closed")
	default:
	}
}

func TestWatchHealthIdleBlackholeEventuallyFails(t *testing.T) {
	tunnel, conn := testHealthTunnel(t)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()
	err := tunnel.watchHealth(ctx, watchdogPolicy{interval: time.Millisecond, probeTimeout: 2 * time.Millisecond, silenceGrace: 8 * time.Millisecond, failures: 3})
	if !errors.Is(err, ErrTunnelUnresponsive) || conn.writes.Load() < 3 {
		t.Fatalf("idle blackhole was not detected after repeated attempts: %v, probes=%d", err, conn.writes.Load())
	}
}

func TestWatchHealthKernelEchoDoesNotKeepBlackholeAlive(t *testing.T) {
	tunnel, conn := testHealthTunnel(t)
	conn.onWrite = func([]byte) { conn.input <- wire.Echo(0, tunnel.id, 1, []byte("ordinary kernel echo")) }
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()
	err := tunnel.watchHealth(ctx, watchdogPolicy{interval: time.Millisecond, probeTimeout: 2 * time.Millisecond, silenceGrace: 8 * time.Millisecond, failures: 3})
	if !errors.Is(err, ErrTunnelUnresponsive) || tunnel.lastAuthenticatedReceive.Load() != 0 {
		t.Fatalf("unauthenticated echo was treated as tunnel health: %v", err)
	}
}
