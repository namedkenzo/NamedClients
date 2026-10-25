# namedP Android 0.2.0-alpha — ICMPv6 profiles

IPv6 literals and selected domains use Android's rootless ICMPv6 ping socket,
the kernel-assigned Echo identifier and AAAA resolution. The details editor
preserves the selected family, bracketed URI and credentials. VPN/Proxy/per-app
mode still comes from the saved profile. IPv4 profiles remain supported.

Still a debug-signed alpha. Device/kernel/carrier support varies; no silent
fallback to an unwanted family. Real phone/VPS throughput, leak and vendor tests
remain necessary. Outer IPv6 does not guarantee inner IPv6 forwarding in all modes.
