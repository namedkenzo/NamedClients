package client

import "time"

// Producers never block behind stale UDP. TCP reliability is not changed.
func offerLatest[T any](queue chan T, value T) {
 select {case queue<-value:return;default:}
 select {case <-queue:default:}
 select {case queue<-value:default:}
}
func expiredUDP(at time.Time, limit time.Duration)bool{return limit>0&&time.Since(at)>limit}
