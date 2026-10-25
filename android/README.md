# namedP for Android

**0.2.2-alpha development** implementation. Android 8+ (API 26), arm64 phones and x86_64 emulators. The APK is debug-signed for testing, not a production/Play Store release.

## Experience

A fresh connection-first layout inspired by the clear navigation of [Psiphon](https://play.google.com/store/apps/details?id=com.psiphon3) and profile workflow of [PingTunnel Client](https://github.com/itismoej/pingtunnel-client), with original namedP styling and dachshund artwork. No upstream application UI/source is copied.

- Home, Connections and Settings; system/light/dark themes.
- English/Persian labels, direct full-clipboard Paste import, optional names and a + manual form and in-app traffic counters.
- Dachshund breathing/blinking idle, running while connecting and wagging when connected; respects Android animation preferences.
- Authenticated probe before Connected; service notification and explicit Disconnect.
- Profiles encrypted using Android Keystore AES-GCM. Backups disabled; credentials stay off command lines.

## Add a connection

1. Copy the complete `pingtunnel://` Config link from your panel.
2. Tap **Paste** on Home or Connections. It imports the complete clipboard text directly, without an editor. The server is used as the default name.
3. Tap **Connect**. Importing does not connect automatically. The URI's `mode` selects routing; there is no separate home mode toggle.
4. Tap the configuration to edit Details, save/copy it or run a relayed HTTPS delay test. Changing Details updates the same saved profile.

VPN routes the device; Proxy exposes local SOCKS only. Proxy per app uses an Android application allowlist selected in Details. At least one app other than namedP is required; visible launcher apps are offered. The Test delay button sends a fresh HTTPS GET to `https://www.gstatic.com/generate_204` through the profile's authenticated ICMP stream and selected outbound. It requires verified TLS and HTTP 204; redirects and other statuses fail. The timer includes TCP setup, TLS and response headers, but excludes the preceding ICMP authentication. It is not raw ICMP ping, download speed or a leak test. Results appear on Home, Connections and Details, remain in memory only, and are invalidated when a profile changes. Tests work disconnected or while connected, without starting/stopping the device VPN or binding its SOCKS port. A test uses a small amount of the profile's quota and contacts Google; only a manual tap sends it, with no automatic background polling.

Alternatively tap **+** and enter the server IP/domain, optional name, numeric key **or** encryption method and Base64 key, local SOCKS port and connection mode. A missing key is not guessed or bypassed; copy the full link or obtain the key from your panel. Compatible encryption aliases are normalized before being sent to the engine. Secrets are not displayed in errors.

## Transport

## Gaming (0.2.2-alpha)

Settings → Gaming is opt-in; disconnect, enable it and reconnect. It bounds
association UDP queues to 32 packets and drops datagrams queued for over 150 ms.
This trades stale data for possible packet loss under load, not a promised ping.
Update the panel to 0.4.2 for separate UDP/TCP output scheduling. Choose Direct or
UDP-capable SOCKS, pause downloads and optionally route only the game per app.
The HTTPS relay test is not the game's own ping. Test with your game/network.
Windows releases are unchanged.

The independent Android Go executable reuses the compatible namedP codec/reliability/SOCKS implementation. Android's ping datagram socket avoids a root/raw-socket requirement when allowed by the vendor and kernel. The kernel-assigned ICMP ID is respected.

VPN mode uses VpnService and excludes namedP's own UID to avoid recursive routing. TUN descriptors pass over a randomly named Unix socket after same-UID verification. Native tun2socks v2.6.0 handles TCP/UDP. The IPv6 route is captured by the VPN; IPv6 forwarding is not guaranteed. DNS uses 1.1.1.1 / 1.0.0.1 through the VPN. Proxy mode exposes loopback SOCKS5 only.

Some vendors or networks block unprivileged ICMP. The app reports an error instead of silently changing transports. HTTP server outbounds cannot carry UDP. The UI's Connected state validates ICMP and local process startup, not access to every target site.

## Build

Install JDK 17, Android SDK 35 / build-tools 35.0.0, NDK 28.0.13004108, Gradle 8.11.1 and Go 1.26+ on Linux. Then:

```bash
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/28.0.13004108"
bash scripts/build_android_native.sh
gradle -p android testDebugUnitTest assembleDebug
```

CI additionally runs emulator interface checks and captures both themes. Connected previews are visibly labeled as interface previews. Real-phone/VPS connection, throughput, leaks, idle/background lifecycle and vendor behavior still require acceptance tests.

## Signing and publication

Both client sources may now be public at the owner's request. Android has its own `android-v0.2.2-alpha` development release; Windows stays unchanged. CI caches the development debug key for test builds; production distribution requires owner-controlled signing secrets. Never put signing keys or actual profiles in Git.

## فارسی

رابط جدید اندروید از ساختار سادهٔ اتصال و تنظیمات سایفون و مدیریت پروفایل PingTunnel Client الهام گرفته و با طراحی و داچهوند اختصاصی namedP ساخته شده است. برنامه حالت VPN دستگاه یا SOCKS5 محلی، تم خودکار/روشن/تیره و متن‌های فارسی/انگلیسی دارد. این نسخهٔ آلفا و امضاشده با کلید توسعه است و باید روی گوشی و سرور واقعی تست شود.

برای تست، دکمهٔ «تست تأخیر» را در خانه یا جزئیات کانفیگ بزنید. درخواست HTTPS به generate_204 گوگل از داخل همان تونل و خروجی عبور می‌کند؛ فقط پاسخ معتبر ۲۰۴، زمان میلی‌ثانیه‌ای نشان می‌دهد. این عدد پینگ خام ICMP یا سرعت دانلود نیست و تست مقدار کمی از حجم کانفیگ را مصرف می‌کند. هنگام اتصال نیز بدون قطع VPN قابل اجراست.

برای افزودن اتصال، لینک کامل Config پنل را کپی کنید و Paste، سپس اتصال را بزنید. دکمهٔ + برای ورود دستی سرور و رمز است؛ نام اختیاری است. لینک ناقص با پیام مشخص رد می‌شود و هیچ اتصال ساختگی نمایش داده نمی‌شود.

Numeric credentials remain 1–2147483647, as required by the existing signed 32-bit wire format. Longer values are not silently shortened. Server-side speed-test stability changes and network tuning are in [NamedICMP v0.3.1](https://github.com/namedkenzo/NamedICMP/releases/tag/v0.3.1), not this APK.

## IPv6 endpoints

The selected family is preserved in Details and sent to the ICMPv6 ping socket. Literals use `[IPv6]` in URIs, but the host field accepts a bare IPv6 address. Domains selected as IPv6 resolve AAAA only. The kernel supplies the Echo checksum/identifier. A socket opening test is not an end-to-end real-device VPN/throughput test. The advanced web-panel reader is a separate Linux-only setting, not a client-side NEW mode.
