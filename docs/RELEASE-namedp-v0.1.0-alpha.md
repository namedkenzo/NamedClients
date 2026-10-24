# namedP 0.1.0-alpha — Windows client

The Windows client is now maintained independently in NamedClients.
The web panel/server stays in NamedICMP and is not bundled here.

- Native Windows 10/11 x64 UI with system/dark/light themes and animated dachshund.
- Full-device IPv4 VPN (TCP/UDP) and local SOCKS5 proxy modes.
- Independent Go module, protocol codec, per-flow reliability and encryption.
- Windows guardian, server-route bypass and temporary DNS/IPv6 guards.
- Self-contained archive, SHA256SUMS and dependency licenses.
- Cross-repository compatibility tests against a pinned namedICMP server reference.

Download `namedP-0.1.0-alpha-windows-x64.zip`, extract every file and run
`namedP.exe` as Administrator. Do not copy the EXE alone. The application is
unsigned; supplier Wintun signature is checked during build.

This remains alpha. Automated build/render/protocol checks do not establish
real Windows/VPS throughput, leak or forced-crash cleanup acceptance. VPN is
IPv4-only and preserves more-specific LAN routes. HTTP server outbounds are TCP-only.

Android has not been implemented or built and is not included in this release.

## فارسی

کلاینت ویندوز namedP به مخزن مستقل NamedClients منتقل شد. پنل و هسته سرور
در NamedICMP باقی می‌مانند. ZIP را کامل استخراج و برنامه را با دسترسی مدیر
اجرا کنید. نسخه همچنان آلفاست؛ برنامه یا APK اندروید در این ریلیز وجود ندارد.
