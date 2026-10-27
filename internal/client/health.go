package client

import (
	"context"
	"errors"
	"net"
	"time"
)

var ErrTunnelUnresponsive = errors.New("ICMP connection lost after repeated health checks and no authenticated traffic")

type watchdogPolicy struct {
	interval     time.Duration
	probeTimeout time.Duration
	silenceGrace time.Duration
	failures     int
}

// Record elapsed monotonic time, not a Unix timestamp: wall-clock changes must
// not turn a healthy link into a timeout or keep a blackholed link alive forever.
// Only packets that passed peer, echo ID, protocol and credential checks reach
// this method. Sending packets or receiving ordinary kernel echoes is not proof
// that the managed tunnel is reachable.
func (t *Tunnel) recordAuthenticatedReceive(now time.Time) {
	t.lastAuthenticatedReceive.Store(int64(max(0, now.Sub(t.activityStart))) + 1)
}

func (t *Tunnel) authenticatedReceiveAge(now time.Time) time.Duration {
	age := now.Sub(t.activityStart)
	if received := t.lastAuthenticatedReceive.Load(); received > 0 {
		age -= time.Duration(received - 1)
	}
	return max(0, age)
}

type healthFailures struct{ consecutive int }

func (h *healthFailures) failed(success bool, silence time.Duration, policy watchdogPolicy) bool {
	if success {
		h.consecutive = 0
		return false
	}
	h.consecutive = min(policy.failures, h.consecutive+1)
	return h.consecutive >= policy.failures && silence >= policy.silenceGrace
}

// WatchHealth is for an established tunnel. Initial credentials/firewall checks
// still use the short startup Probe deadline. Congestion may lose an individual
// probe while data/ACKs continue to arrive, so a failed synthetic probe alone
// must never tear down the entire device VPN. A real idle blackhole still fails
// after a bounded grace period and repeated independent checks.
func (t *Tunnel) WatchHealth(ctx context.Context) error {
	return t.watchHealth(ctx, watchdogPolicy{
		interval:     5 * time.Second,
		probeTimeout: 8 * time.Second,
		silenceGrace: 30 * time.Second,
		failures:     3,
	})
}

func (t *Tunnel) watchHealth(ctx context.Context, policy watchdogPolicy) error {
	var health healthFailures
	timer := time.NewTimer(policy.interval)
	defer timer.Stop()
	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-t.done:
			return net.ErrClosed
		case <-timer.C:
		}
		probe, cancel := context.WithTimeout(ctx, policy.probeTimeout)
		err := t.Probe(probe)
		cancel()
		if ctx.Err() != nil {
			return ctx.Err()
		}
		select {
		case <-t.done:
			return net.ErrClosed
		default:
		}
		if health.failed(err == nil, t.authenticatedReceiveAge(time.Now()), policy) {
			return ErrTunnelUnresponsive
		}
		// Do not replay queued ticker events after a slow probe: each check gets
		// a fresh interval, bounding retries rather than creating a probe burst.
		timer.Reset(policy.interval)
	}
}
