# namedP Android 0.1.0-alpha — development build

Initial Android client, independently maintained from the Windows UI and server.

- Clean, connection-first layout, Home/Connections/Settings and system/light/dark themes.
- Original animated dachshund: idle breathing, connecting run and connected tail wag.
- English/Persian labels and encrypted on-device profile storage using Android Keystore.
- VPN mode through Android VpnService and local SOCKS5 proxy mode.
- Independent Go ICMP engine with rootless Android ping sockets, TCP/UDP and exclusive credentials.
- TUN descriptor handoff over a same-UID local socket and native tun2socks v2.6.0.
- IPv4 VPN; IPv6 captured rather than bypassed, but IPv6 forwarding is not guaranteed.

The APK supports Android 8+ on arm64 devices and x86_64 emulators. It is a
**debug-signed development APK**, not a production-signed or Play Store release.
Its signing key is a development key; production distribution needs owner-managed signing.

Build, profile tests and emulator UI/render checks gate publication. These do
not establish a real ICMP connection on a phone, throughput or leak acceptance.
Some vendors/carriers block unprivileged ping sockets or ICMP; namedP reports
the failure and does not silently switch to another transport. No root required
where ping sockets are allowed. UDP requires a compatible application/outbound.

The displayed emulator-connected preview is explicitly marked as a UI preview,
not a live tunnel. Test with your own VPS/profile before relying on this alpha.

## فارسی

اولین نسخهٔ اندروید namedP با رابط تازه، داچهوند متحرک، تم سیستم/روشن/تیره،
پروفایل‌های محافظت‌شده و حالت VPN یا SOCKS5. اندروید ۸ به بالا و arm64 پشتیبانی
می‌شوند. این APK امضای توسعه دارد؛ نسخهٔ تولیدی یا انتشار Play Store نیست.
پشتیبانی ICMP بدون روت به دستگاه/شبکه وابسته است و باید روی گوشی واقعی
تست شود. بررسی ساخت و رابط شبیه‌ساز، جای آزمون شبکهٔ واقعی را نمی‌گیرد.
