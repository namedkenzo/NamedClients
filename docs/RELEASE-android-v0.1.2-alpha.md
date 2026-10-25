# namedP Android 0.1.2-alpha — configuration-owned mode and connection details

- Removed the separate VPN/Proxy home toggles. The selected URI's `mode` now determines both Android permission/routing and native engine startup, including after app restart.
- Tap a configuration to open Details: optional name, host, mode, numeric OR encryption key, local port, Save, Copy, authenticated ICMP Test and Connect/Disconnect.
- Added Proxy per app through Android VpnService's application allowlist. Choose visible launcher apps; an empty selection or namedP itself is rejected at connection time, never silently converted to full-device routing.
- Keys stay masked unless explicitly revealed. Copy marks the clipboard item sensitive. Profiles remain encrypted on disk; editing updates the existing profile ID.
- Probe-only diagnostics return actual authenticated ICMP latency without opening a device VPN or showing a fake Connected state. This is not an internet speed/leak test.
- Regression tests for legacy-global-mode isolation, restart persistence, editing/copying, per-app transport mapping and credential round-trips. Windows is unchanged.

For the reported **server-side** speed-test disconnect, update NamedICMP to
[v0.3.1](https://github.com/namedkenzo/NamedICMP/releases/tag/v0.3.1). Server pacing,
network settings and new diagnostic counters are separate from this APK. The
VPS/carrier path still needs before/after testing; provider blocking cannot be
prevented by guest sysctls.

Still a **debug-signed alpha** for Android 8+ on arm64 phones/x86_64 emulators.
Real-device VPN/per-app routing, throughput, leaks and vendor behavior require
acceptance tests. Numeric credentials use the existing signed 32-bit wire field
(1–2147483647); longer decimal strings are NOT silently truncated or hashed. Use
a valid server-exported key. App selections do not change server credentials.

## فارسی

دو دکمهٔ جدا حذف شدند؛ حالت از خود کانفیگ خوانده می‌شود. لمس کانفیگ، صفحهٔ
جزئیات و ویرایش، کپی، ذخیره، تست ICMP و اتصال را باز می‌کند. گزینهٔ برنامه‌های
منتخب نیز داخل جزئیات است. مشکل گزارش‌شدهٔ اسپیدتست سمت سرور جداگانه در
NamedICMP نسخهٔ ۰.۳.۱ سخت‌سازی و قابل‌عیب‌یابی‌تر شده؛ تأیید رفع آن روی VPS
خودتان لازم است. این APK آلفا با امضای توسعه است و ویندوز تغییری ندارد.
