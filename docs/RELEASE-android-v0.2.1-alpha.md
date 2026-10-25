# namedP Android 0.2.1-alpha — HTTPS relay delay

- Test delay sends a fresh GET to https://www.gstatic.com/generate_204 over the
  selected profile's authenticated ICMP stream and configured outbound.
- Displays TCP + verified TLS + HTTP-response delay in milliseconds on Home,
  Connections and Details; only HTTP 204 counts as success.
- Redirects, certificate errors, non-204 responses and timeouts report failure.
  No direct connection/system-proxy fallback and no target DNS lookup on the phone.
- Tests run before connection or alongside an active profile without changing the
  VPN service, per-app selection or local SOCKS port. Disconnect cancels an active
  test. Results are temporary, profile-specific and invalidated after profile edits.
- Unit tests cover HTTPS/status/TLS/timeout failures; controlled integration tests
  cover HTTPS over actual ICMPv4/ICMPv6 with numeric and AES-256 credentials.
- Windows sources, engine, version and releases are unchanged.

This is relay reachability/latency, not raw ICMP ping, throughput or a leak test.
Timing starts after ICMP authentication. A manual test contacts Google and consumes
a small amount of the profile's quota; there is no automatic background testing.
Still a debug-signed Android alpha; real-phone/carrier/VPS acceptance is required.

فارسی: «تست تأخیر» در خانه و جزئیات کانفیگ اضافه شد. درخواست HTTPS از مسیر همان
تونل فرستاده می‌شود و فقط پاسخ معتبر ۲۰۴، میلی‌ثانیه نمایش می‌دهد. تست هنگام اتصال
هم بدون قطع VPN قابل اجراست. این عدد سرعت دانلود یا پینگ خام ICMP نیست. ویندوز
تغییر نکرده و نسخهٔ اندروید همچنان آزمایشی است.
