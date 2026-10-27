// namedp-engine accepts secrets on stdin, never in its command line.
package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"namedp/internal/androidrelay"
	"namedp/internal/client"
	"os"
	"sync"
	"time"
)

const version = "1.0.2"

func main() {
	if len(os.Args) == 2 && os.Args[1] == "--version" {
		fmt.Println(version)
		return
	}
	if err := run(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
func run() error {
	var input struct {
		Link           string `json:"link"`
		GamingMode     bool   `json:"gaming_mode"`
		RelayDelayOnly bool   `json:"relay_delay_only"`
	}
	dec := json.NewDecoder(io.LimitReader(os.Stdin, 16384))
	if err := dec.Decode(&input); err != nil {
		return err
	}
	p, err := client.ParseProfile(input.Link)
	if err != nil {
		return err
	}
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	if input.RelayDelayOnly {
		var stop context.CancelFunc
		ctx, stop = context.WithTimeout(ctx, 25*time.Second)
		defer stop()
	}
	// The GUI retains stdin for this process's lifetime. Closing the GUI's pipe
	// terminates the engine even if its parent was killed.
	go func() { io.Copy(io.Discard, os.Stdin); cancel() }()
	t, err := client.Open(ctx, p)
	if err != nil {
		return err
	}
	defer t.Close()
	if input.GamingMode {
		t.EnableGamingMode()
	}
	probe, stop := context.WithTimeout(ctx, 8*time.Second)
	err = t.Probe(probe)
	stop()
	if err != nil {
		return err
	}
	if input.RelayDelayOnly {
		delay, err := androidrelay.Measure(ctx, t)
		if err != nil {
			return err
		}
		return json.NewEncoder(os.Stdout).Encode(map[string]any{"event": "relay_delay", "url": androidrelay.Endpoint, "status": 204, "latency_ms": delay.Milliseconds()})
	}
	var mu sync.Mutex
	emit := func(v any) { mu.Lock(); defer mu.Unlock(); json.NewEncoder(os.Stdout).Encode(v) }
	go func() {
		tick := time.NewTicker(time.Second)
		defer tick.Stop()
		for {
			select {
			case <-ctx.Done():
				return
			case <-tick.C:
				emit(map[string]any{"event": "stats", "upload": t.Upload.Load(), "download": t.Download.Load()})
			}
		}
	}()
	go func() {
		if err := t.WatchHealth(ctx); err != nil && ctx.Err() == nil {
			fmt.Fprintln(os.Stderr, err)
			t.Close()
		}
	}()
	err = t.ServeSOCKS(ctx, p.Port, func() {
		emit(map[string]any{"event": "ready", "server_ip": t.ServerIP(), "port": p.Port, "mode": p.Mode})
	})
	if ctx.Err() != nil {
		return nil
	}
	return err
}
