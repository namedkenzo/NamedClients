# namedP for Android

Initial **0.1.0-alpha development** implementation. Android 8+ (API 26), arm64 phones and x86_64 emulators. The APK is debug-signed for testing, not a production/Play Store release.

## Experience

A fresh connection-first layout inspired by the clear navigation of [Psiphon](https://play.google.com/store/apps/details?id=com.psiphon3) and profile workflow of [PingTunnel Client](https://github.com/itismoej/pingtunnel-client), with original namedP styling and dachshund artwork. No upstream application UI/source is copied.

- Home, Connections and Settings; system/light/dark themes.
- English/Persian labels, masked URI entry, named profiles and in-app traffic counters.
- Dachshund breathing/blinking idle, running while connecting and wagging when connected; respects Android animation preferences.
- Authenticated probe before Connected; service notification and explicit Disconnect.
- Profiles encrypted using Android Keystore AES-GCM. Backups disabled; credentials stay off command lines.

## Transport

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

Both client sources may now be public at the owner's request. Android has its own `android-v0.1.0-alpha` development release; Windows stays unchanged. CI caches the development debug key for test builds; production distribution requires owner-controlled signing secrets. Never put signing keys or actual profiles in Git.

## فارسی

رابط جدید اندروید از ساختار سادهٔ اتصال و تنظیمات سایفون و مدیریت پروفایل PingTunnel Client الهام گرفته و با طراحی و داچهوند اختصاصی namedP ساخته شده است. برنامه حالت VPN دستگاه یا SOCKS5 محلی، تم خودکار/روشن/تیره و متن‌های فارسی/انگلیسی دارد. این نسخهٔ آلفا و امضاشده با کلید توسعه است و باید روی گوشی و سرور واقعی تست شود.
