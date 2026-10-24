# Android client

Reserved for the namedP Android implementation. No app or APK exists yet.

The Windows raw-socket implementation cannot simply be cross-compiled for
unrooted Android. Android needs a VpnService, an appropriate foreground service,
protected/bypassed ICMP transport and a TUN-to-SOCKS TCP/UDP path. The service
must release its TUN descriptor on stop/revoke and guard against IPv6/DNS leaks.
Rootless ICMP socket support must be checked on actual target devices.

The interface should match the Windows themes and animated dachshund, default
to the system theme, and expose VPN and local proxy modes. Signing and build
artifacts must not be published until the owner explicitly resolves Android's
public/private delivery. This public repository currently contains only this
development note, not Android implementation or personal configuration.
