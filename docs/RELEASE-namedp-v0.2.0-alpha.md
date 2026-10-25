# namedP Windows 0.2.0-alpha — ICMPv6 endpoints

IPv6 literals and domains with `family=ipv6` now select ICMPv6 and AAAA resolution.
IPv4 remains supported. Full-device VPN adds a physical /128 endpoint bypass;
the IPv6 guard excludes exactly that endpoint instead of blocking its transport.
Other IPv6 destinations remain blocked. Outer IPv6 transport is not a guarantee
of complete inner IPv6 forwarding. UI design is unchanged.

Build, loopback and guard-range checks gate publication. Real Windows/VPS routing,
DNS and cleanup acceptance remain necessary. Extract all files; run as Administrator.

The Windows loopback responder also handles the kernel-reflected probe when
Winsock does not expose Echo Requests to that raw receiver. It returns the
authenticated protocol reply, rather than treating ordinary kernel ping replies
as a successful tunnel handshake.
