# NamedClients

**The home of namedP clients for namedICMP.**

[فارسی](README.fa.md) · [Windows downloads](https://github.com/namedkenzo/NamedClients/releases) · [Web panel](https://github.com/namedkenzo/NamedICMP) · [MIT license](LICENSE)

| Platform | Status | Source |
| --- | --- | --- |
| Windows 10/11 x64 | namedP `0.2.0-alpha` | [windows/namedP](windows/namedP) |
| Android 8+ (arm64) | `0.2.2-alpha` development APK | [android](android) · [Download](https://github.com/namedkenzo/NamedClients/releases/tag/android-v0.2.2-alpha) |

## Windows: connect in three steps

1. Download the ZIP from [Releases](https://github.com/namedkenzo/NamedClients/releases) and extract **all files**.
2. Run `namedP.exe` as Administrator and paste the complete `pingtunnel://` profile exported by the panel.
3. Choose **Full-device VPN** or **Local SOCKS5 proxy**, then Connect.

The app follows the system theme by default, supports dark/light overrides and includes an animated dachshund companion. Its independent Go transport carries separate TCP/UDP flows and accepts either a numeric key or an encryption-only profile. Full VPN carries IPv4 application traffic over an IPv4 or IPv6 endpoint; other Internet IPv6 is guarded while the exact endpoint and essential local ICMPv6 remain reachable. More-specific LAN routes remain available. See the [Windows guide](docs/NAMEDP.md).

> Windows remains alpha. Build, protocol and UI checks do not replace real Windows/VPS network, throughput and cleanup acceptance testing.

## Android

**New in 0.2.1-alpha:** Test delay measures `https://www.gstatic.com/generate_204` through the selected profile (TCP + verified TLS + HTTP 204), not the phone's direct connection. Works before or during VPN use; results appear on Home, Connections and Details. It is not a speed test. Windows remains unchanged.

Routing follows the configuration's `mode`; the home screen has no separate mode switch. Tap a profile for Details, Save/Copy, HTTPS relay-delay Test, and VPN/Proxy/per-app options. Server-side burst control and reversible installer tuning are in [NamedICMP 0.3.1](https://github.com/namedkenzo/NamedICMP/releases/tag/v0.3.1).

Copy the panel Config link → **Paste** → **Connect**. Or tap **+** to enter the server and numeric/encryption key manually. Names are optional.

The Android app is now available as a **debug-signed development APK**, not a production/Play Store release. It has its own connection-first UI, Home/Connections/Settings, system/light/dark themes, Persian/English labels and animated dachshund. Profiles are protected with Android Keystore. Rootless ICMP depends on device/kernel support. [Android guide](android/README.md) · [Development APK](https://github.com/namedkenzo/NamedClients/releases/tag/android-v0.2.2-alpha).

Emulator profile/UI tests and native rootless socket execution passed. A real phone/VPS connection, throughput and leak acceptance are still pending. Windows functionality was not redesigned as part of the Android work.

[Light interface preview](https://github.com/namedkenzo/NamedClients/releases/download/android-v0.2.2-alpha/light-idle.png) · [Dark interface preview](https://github.com/namedkenzo/NamedClients/releases/download/android-v0.2.2-alpha/dark-idle.png)

IPv6 endpoints are supported by namedP 0.2.0-alpha: use `pingtunnel://[IPv6]?lport=1080&mode=vpn&family=ipv6&key=...`, or a domain with an AAAA record and `family=ipv6`. Update the web panel to 0.4.0. Windows retains a physical endpoint bypass and guards other IPv6 traffic; real-device/VPS acceptance remains necessary.

## Repository layout

```text
windows/             Native namedP UI and render checks
android/             Native Android client and VPN service
cmd/namedp-engine/   Standalone Windows client engine
internal/client/     ICMP transport and SOCKS5 TCP/UDP
internal/wire/       Compatible protocol codec and encryption
internal/reliable/   Ordered, deduplicated TCP transport
```

Client versions and releases are independent of the web panel. This repository contains client apps only; the web panel, its managed tunneling core, Linux installer and panel data belong in [NamedICMP](https://github.com/namedkenzo/NamedICMP).

## Other PingTunnel clients

- [Original PingTunnel CLI and releases](https://github.com/esrrhs/pingtunnel/releases)
- [Community Android/desktop PingTunnel Client](https://github.com/itismoej/pingtunnel-client/releases)

These are separate upstream/community projects. Client capabilities and encryption flags vary by version.

## Build & verify

Go 1.26+; .NET SDK 8 for Windows. The workflow bundles the .NET runtime, pinned tun2socks and supplier-signed Wintun with licenses.

```bash
go test -race ./...
go vet ./...
node scripts/test_versions.cjs
```

CI also checks out a pinned server reference and runs its original client/server compatibility tests against this repository's current transport sources. This reference is for tests only, not a runtime or module dependency. See [migration notes](docs/MIGRATION.md).
