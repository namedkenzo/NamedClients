# namedP Windows 0.3.0-alpha — VPN repair, profile-owned modes and installer

- Reproduced the exact screenshot error with Windows NetSecurity: `::/0` is
  rejected. The IPv6 guard now uses provider-validated `::/1` + `8000::/1`.
- Tests use real disabled firewall rules, real Wintun/IP/DNS setup and guardian
  EOF cleanup. CI mocks only global route mutations so it cannot disconnect its
  own runner. Physical endpoint bypass and split /1 IPv4 routes remain intact.
- Removed manual VPN/Proxy buttons. The validated profile's `mode` selects either
  full-device IPv4 TCP/UDP over Wintun or local SOCKS5; engine readiness confirms it.
- Added HTTPS relay delay through the selected ICMP profile to Google's
  generate_204 endpoint. Verified TLS + HTTP 204 required; no direct fallback.
  Delay test failures do not tear down the active VPN.
- Optional Gaming uses the same short UDP association queues as Android. Reconnect
  to apply; stale queued UDP may be dropped instead of delivered late.
- Setup EXE supports installation folder selection and shortcuts; portable ZIP
  remains available. Both contain the engine, tun2socks, signed Wintun and licenses.
- Single-instance protection prevents installed/portable copies changing routes
  concurrently. Errors identify the failing VPN setup stage.

Installer/application are unsigned development builds; Windows may show SmartScreen.
The supplier Wintun DLL signature is verified during build. Extract all portable
files; administrator rights are required. Close namedP before install/uninstall.
Android and panel versions are unchanged.

The VPN carries IPv4 TCP/UDP; other Internet IPv6 is guarded rather than leaked,
while an outer ICMPv6 endpoint is preserved. Existing LAN routes remain available;
this is not a strict LAN kill switch or arbitrary-IP-protocol OpenVPN replacement.
Physical Windows/VPS/carrier throughput, DNS/leak and game testing remain necessary.

References (behavior only, no source copied): [v2rayN TUN documentation](https://github.com/2dust/v2rayN/wiki/Description-of-some-ui),
[OpenVPN route documentation](https://github.com/OpenVPN/openvpn/blob/master/doc/man-sections/vpn-network-options.rst),
[Microsoft NetSecurity](https://learn.microsoft.com/powershell/module/netsecurity/new-netfirewallrule),
[Inno Setup directory selection](https://jrsoftware.org/ishelp/topic_setup_defaultdirname.htm).
