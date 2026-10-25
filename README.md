# namedP · NamedClients

A little companion. A private connection.

[فارسی](README.fa.md) · [Download the latest release](https://github.com/namedkenzo/NamedClients/releases/latest) · [NamedICMP web panel](https://github.com/namedkenzo/NamedICMP) · [MIT license](LICENSE)

Native Windows and Android clients for configurations exported by the **NamedICMP web panel**.
Both platforms now share one **1.0.0 stable GitHub release**, independently versioned from the panel.

| Download | Platform | How to run |
| --- | --- | --- |
| [Setup EXE](https://github.com/namedkenzo/NamedClients/releases/download/namedp-v1.0.0/namedP-1.0.0-windows-x64-setup.exe) | Windows 10/11 x64 | Choose an install folder and shortcuts |
| [Portable ZIP](https://github.com/namedkenzo/NamedClients/releases/download/namedp-v1.0.0/namedP-1.0.0-windows-x64.zip) | Windows 10/11 x64 | Extract **all files**, run `namedP.exe` |
| [Android APK](https://github.com/namedkenzo/NamedClients/releases/download/namedp-v1.0.0/namedP-1.0.0-android.apk) | Android 8+, arm64 | Back up alpha profiles before reinstalling |

## Connect, without the clutter

Copy the complete `pingtunnel://` link from your panel → **Paste** → **Connect**.
The configuration chooses VPN or local SOCKS5; there is no separate home-screen mode switch.
Android also offers **+** for manual input and a profile Details screen.

- **Made for your desktop and phone:** matching light/dark palettes, system theme by default,
  refined connection cards and an original animated dachshund. Reduced-motion settings are respected.
- **Remembered, not exposed:** Windows restores the last link using Windows account-bound DPAPI;
  **Forget** removes it. Android profiles use Android Keystore. Credentials remain masked and off process command lines.
- **Useful diagnostics:** manually test verified HTTPS relay delay through the selected profile
  at `https://www.gstatic.com/generate_204`. This is not a speed test or game ping.
- **Gaming, when you want it:** optional shorter UDP queues discard stale queued packets.
  Reconnect to apply; packet loss may increase and a lower RTT is not guaranteed.
- **Your existing profiles:** separate TCP/UDP flows, numeric **or** encryption-only credentials,
  IPv4/IPv6 endpoints and domains. No web-panel update is needed for the 1.0 visual refresh.

[Windows guide](docs/NAMEDP.md) · [Android guide](android/README.md) · [1.0.0 changes](docs/RELEASE-namedp-v1.0.0.md)

## A look inside

| Windows | Android |
| --- | --- |
| ![Windows dark interface](https://github.com/namedkenzo/NamedClients/releases/download/namedp-v1.0.0/windows-preview-dark.png) | ![Android light interface](https://github.com/namedkenzo/NamedClients/releases/download/namedp-v1.0.0/android-light-idle.png) |

Screenshots are credential-free interface previews, not evidence of a live VPN connection.

## Before updating

Close old Windows copies, then run Setup or extract the new ZIP into a clean folder.
Administrator access is required for the raw ICMP socket and Wintun.

**Android alpha → 1.0.0 needs a one-time reinstall.** The old signing key was not preserved by the earlier build script.
Before uninstalling, open each profile → Copy and save its complete link somewhere private.
Then uninstall the alpha, install 1.0.0 and Paste your links. Uninstalling erases Keystore profiles.
The new non-debuggable release uses a new signing identity, with the owner's approval.
Future updates must match this identity; the private key is not placed in public Actions caches.
See [signing and recovery](docs/ANDROID_SIGNING.md). This is not a Play Store build.
Windows Setup/app still lack publisher Authenticode signing; SmartScreen may warn.
A stable GitHub label is not a security audit or a promise of bug-free operation.

Windows full-device VPN carries **IPv4 TCP/UDP** over IPv4/IPv6 outer endpoints and guards other Internet IPv6;
more-specific LAN routes remain. Android uses VpnService; rootless ICMP depends on device/kernel support.
HTTP outbounds cannot carry UDP. Network behavior still depends on your device, VPS and ICMP path.

## Repository layout

```text
windows/             Windows UI, installer and render/persistence checks
android/             Android UI, profile storage and VPN service
cmd/                 Independently built Windows/Android engines
internal/client/     Compatible ICMP transport and SOCKS5 TCP/UDP
internal/wire/       Protocol codec and encryption
internal/reliable/   Ordered, deduplicated TCP transport
```

Only clients live here. The web panel, its managed tunneling core and Linux installer stay in
[NamedICMP](https://github.com/namedkenzo/NamedICMP). Installing the web panel does not download these clients.

## Other PingTunnel clients

- [Original PingTunnel CLI](https://github.com/esrrhs/pingtunnel/releases)
- [Community PingTunnel Client for Android/desktop](https://github.com/itismoej/pingtunnel-client/releases)

These are independent upstream/community projects; capabilities and encryption flags vary.

## Build & verify

Go 1.26+, .NET SDK 8 on Windows; JDK 17, Android SDK/NDK and Gradle for Android.
The shared workflow gates publication on both platforms: Go race/protocol compatibility tests,
WPF DPAPI/restart/theme checks, real firewall/Wintun setup and cleanup checks with global routes mocked,
Setup install/uninstall, Android unit/emulator tests, signing continuity and profile-preserving upgrades.
Packages include checksums and dependency notices. See [migration notes](docs/MIGRATION.md).

```bash
go test -race ./...
go vet ./...
node scripts/test_versions.cjs
node scripts/test_android_version.cjs
node scripts/test_client_release.cjs
```
