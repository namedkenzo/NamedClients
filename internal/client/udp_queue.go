package client

import "time"

// Configure before serving flows; immutable throughout a session.
func(t *Tunnel) EnableGamingMode(){t.udpQueueLimit=32;t.udpQueueAge=150*time.Millisecond}

// Producers never block behind stale UDP. TCP reliability is not changed.
func offerLatest[T any](queue chan T, value T) {
 select {case queue<-value:return;default:}
 select {case <-queue:default:}
 select {case queue<-value:default:}
}
func expiredUDP(at time.Time, limit time.Duration)bool{return limit>0&&time.Since(at)>limit}
