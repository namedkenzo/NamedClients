# NamedClients

**The home of namedP clients for namedICMP.**

[فارسی](README.fa.md) · [Windows downloads](https://github.com/namedkenzo/NamedClients/releases) · [Server & web panel](https://github.com/namedkenzo/NamedICMP) · [MIT license](LICENSE)

| Platform | Status | Source |
| --- | --- | --- |
| Windows 10/11 x64 | namedP `0.1.0-alpha` | [windows/namedP](windows/namedP) |
| Android | Planned; no application or APK built yet | [android](android) |

## Windows: connect in three steps

1. Download the ZIP from [Releases](https://github.com/namedkenzo/NamedClients/releases) and extract **all files**.
2. Run `namedP.exe` as Administrator and paste the complete `pingtunnel://` profile exported by the panel.
3. Choose **Full-device VPN** or **Local SOCKS5 proxy**, then Connect.

The app follows the system theme by default, supports dark/light overrides and includes an animated dachshund companion. Its independent Go transport carries separate TCP/UDP flows and accepts either a numeric key or an encryption-only profile. Full VPN is IPv4, temporarily blocks IPv6 and preserves more-specific LAN routes. See the [Windows guide](docs/NAMEDP.md).

> Windows remains alpha. Build, protocol and UI checks do not replace real Windows/VPS network, throughput and cleanup acceptance testing.

## Android

The `android/` directory is the development location. No Android implementation or APK is included yet. Android's public/private delivery is a separate decision; the Windows workflow does not publish Android files.

## Repository layout

```text
windows/             Native namedP UI and render checks
android/             Planned Android client
cmd/namedp-engine/   Standalone Windows client engine
internal/client/     ICMP transport and SOCKS5 TCP/UDP
internal/wire/       Compatible protocol codec and encryption
internal/reliable/   Ordered, deduplicated TCP transport
```

Client versions and releases are independent of the server. There is no web panel, server executable, Linux installer or server state database in this repository. Those live in [NamedICMP](https://github.com/namedkenzo/NamedICMP).

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
