# namedP changelog

## 1.0.0 — 2026-10-25

- Fix lost connection links after restart: account-bound DPAPI autosave, atomic
  writes, immediate-close flush, corruption protection and explicit Forget.
- Refresh dark/light styling, connection card, theme picker, focus rings,
  Gaming control and relay/traffic hierarchy; keep lightweight animations.
- One stable GitHub release with Windows Setup, portable ZIP and Android APK.
- Keep the known-working protocol, config-owned routing and cleanup behavior.

## 0.1.0-alpha — 2026-10-24

- First native Windows x64 client for namedICMP.
- Full-device IPv4 VPN with TCP/UDP and local SOCKS5 proxy modes.
- System, dark and light themes with an animated dachshund companion.
- Independent ICMP client transport, authenticated readiness check, bounded
  sessions and shared protocol/reliability code with the server.
- Session-scoped routes and DNS/IPv6 guards with a separate cleanup guardian.
- Self-contained Windows packaging; extract all files and run namedP.exe.

Client versions and releases are independent of the server. This initial alpha
still requires Windows/VPS acceptance testing; see docs/NAMEDP.md.

