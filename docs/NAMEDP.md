# namedP for Windows · 1.0.0

Download the [shared stable release](https://github.com/namedkenzo/NamedClients/releases/latest).
Choose `namedP-1.0.0-windows-x64-setup.exe` for an installer with folder selection,
or extract **all files** from `namedP-1.0.0-windows-x64.zip` and launch `namedP.exe`.
Windows 10/11 x64 is supported; the .NET runtime is bundled.
Administrator access is required for raw ICMP and Wintun. Close older copies before updating.

## Your connection, remembered

Paste the complete `pingtunnel://` profile from the NamedICMP web panel, then Connect.
The configuration's `mode=vpn` or `mode=proxy` determines routing. There is no manual mode override.
The engine validates credentials with an ICMP handshake before Connected.

The last link is now saved automatically and restored on launch, including closing immediately after pasting.
Only DPAPI CurrentUser-protected ciphertext is written to
`%LOCALAPPDATA%/namedP/profile.dat`, with atomic replacement.
It belongs to your Windows account, not to the install directory; Setup and portable copies share it.
A different Windows account cannot normally decrypt it. Local processes running as you can access your data;
this is not protection from a compromised account.

**Forget** or clearing the field removes the saved link. No automatic connection occurs on startup.
Unreadable data is reported rather than silently discarded. Keep a backup configuration link.
The link stays masked in the UI and is passed to the engine through stdin, never process arguments.
Uninstall preserves these account-local preferences; use Forget before uninstalling if desired.

## Modes, tests and Gaming

- VPN forwards **IPv4 TCP/UDP** with Wintun + pinned tun2socks 2.6.0.
  A physical /32 or /128 endpoint route prevents recursive tunneling.
  Other Internet IPv6 is blocked while the exact outer endpoint and essential local ICMPv6 remain reachable.
  More-specific LAN routes remain; this is not a strict LAN kill switch.
- Proxy exposes local SOCKS5 TCP CONNECT + UDP ASSOCIATE on `127.0.0.1:<lport>`.
  Configure apps to use SOCKS5; HTTP proxy negotiation is not implemented.
- **Test delay** sends verified TLS + HTTP to `https://www.gstatic.com/generate_204`
  through the selected profile/outbound, not a direct fallback. Only HTTP 204 produces milliseconds.
  The timer includes TCP, TLS and response headers, excludes prior authentication,
  and is not game ping or a speed test. A failed test leaves the VPN running.
- **Gaming** is optional. Reconnect to apply 32-packet UDP queues and a 150 ms queued-packet age limit.
  Loss can increase under load and lower ping is not guaranteed. HTTP outbounds cannot carry UDP.
  NamedICMP web panel 0.4.2 includes the matching UDP scheduling improvements.

## Appearance and cleanup

System theme is the default; Dark and Light override it. The shared forest/mint palette,
quiet companion card, clear profile/relay/traffic hierarchy and themed controls
keep the interface compact. The dachshund idles, runs while connecting and wags when connected.
Button/mascot animations respect Windows reduced-motion preferences.

A separate guardian removes only its session-created routes/firewall rules on normal disconnect,
engine failure or GUI EOF. Original adapter DNS is not edited. Power loss, forced guardian termination,
sleep/resume and vendor firewall/network variations still need real-device acceptance.
The known NetSecurity `::/0` error is fixed using accepted /1 prefixes.

## Release and signing

The Windows app and Android APK share one **namedp-v1.0.0 stable GitHub release**.
The web panel keeps a separate version and does not download client packages during installation.
Windows Setup/app are not publisher Authenticode signed; SmartScreen/UAC warnings may appear.
Wintun is supplier-signed. Checksums and dependency licenses ship with the packages.
Stable metadata is not a security audit or a promise of bug-free network behavior.

## Build

On Windows with Go 1.26+ and .NET SDK 8:

```
dotnet publish windows/namedP/namedP.csproj -c Release -r win-x64 --self-contained true -o dist/namedP
go build -o dist/namedP/namedp-engine.exe ./cmd/namedp-engine
dotnet run --project windows/namedP.Smoke -c Release
```

Bundle tun2socks 2.6.0, amd64 Wintun 0.14.1 and licenses as in the Windows workflow.
The shared release gates both platforms on their tests before publishing.

## فارسی

نسخهٔ ۱٫۰٫۰ ویندوز و اندروید در یک ریلیز عادی منتشر می‌شوند. ویندوز دو بستهٔ نصبی و ZIP پوشه‌ای دارد.
لینک بعد از بستن برنامه حفظ می‌شود و با محافظت حساب ویندوز ذخیره می‌شود؛ **Forget** آن را پاک می‌کند.
VPN یا پروکسی از کانفیگ خوانده می‌شود. ظاهر روشن/تیره، کارت‌ها و نمایش تأخیر/ترافیک مرتب‌تر شده‌اند؛
Gaming و تست HTTPS حفظ شده‌اند. ویندوز هنوز امضای ناشر ندارد. کانفیگ پشتیبان نگه دارید.

