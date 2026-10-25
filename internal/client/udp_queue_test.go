package client
import("testing";"time";"sync")
func TestGamingQueueKeepsLatestBoundedPackets(t *testing.T){q:=make(chan int,32);for i:=0;i<1000;i++{offerLatest(q,i)};if len(q)!=32{t.Fatal(len(q))};for want:=968;want<1000;want++{if got:=<-q;got!=want{t.Fatal(got,want)}}}
func TestGamingExpiryDoesNotChangeDefaultPolicy(t *testing.T){old:=time.Now().Add(-time.Second);if expiredUDP(old,0)||!expiredUDP(old,150*time.Millisecond)||expiredUDP(time.Now(),150*time.Millisecond){t.Fatal("expiry policy")}}
func TestLatestQueueConcurrentProducersStayBounded(t *testing.T){q:=make(chan int,32);var wg sync.WaitGroup;for p:=0;p<8;p++{wg.Add(1);go func(){defer wg.Done();for i:=0;i<1000;i++{offerLatest(q,i)}}()};wg.Wait();if len(q)>32{t.Fatal("unbounded")}}
