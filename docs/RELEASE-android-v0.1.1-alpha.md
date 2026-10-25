# namedP Android 0.1.1-alpha — simpler, safer profile import

- Paste reads the complete text directly from the Android clipboard, imports/selects the profile and returns to Connect. No link editor or mandatory name.
- The + button opens a scrollable manual form: server, optional name, authentication, local SOCKS port and VPN/proxy mode.
- Numeric authentication and AES-128/AES-256/ChaCha20 are separate choices, with only one credential field. No mixed numeric/encrypted profiles.
- Recognizes supported PingTunnel Client aliases for encryption/key/local port/mode and converts them to the native engine's canonical URI.
- Preserves literal and percent-encoded Base64 +, / and = characters. Missing keys give actionable English/Persian messages without printing credentials.
- Imports are stored encrypted. Re-importing an identical canonical URI selects its existing profile. Import never starts a connection or reports a fake Connected state.
- Regression tests cover clipboard import, manual numeric/encrypted entry, aliases, missing/duplicate credentials, Persian digits and process-facing canonical URIs.
- Windows, the web panel are unchanged.

Still a **debug-signed alpha** for Android 8+ (arm64 phones / x86_64 emulators).
Build/unit/emulator tests do not establish real-device/VPS connectivity, speed or
leak behavior. Rootless ICMP availability depends on the device and network.
Install over 0.1.0-alpha when Android accepts its development signature; do not
clear app data without saving your original profiles elsewhere first.

## فارسی

دکمهٔ Paste لینک کامل کلیپ‌بورد را مستقیم وارد می‌کند؛ سپس اتصال را بزنید.
با + می‌توانید سرور و رمز را دستی وارد کنید؛ نام اختیاری است. رمز عددی و
رمزنگاری انتخاب‌های جدا هستند. خواندن پارامترها و کلیدهای Base64 اصلاح شده
و خطای لینک ناقص روشن‌تر است. رمز جاافتاده ساخته یا حدس زده نمی‌شود.
ویندوز و پنل تغییری ندارند. نسخه همچنان آلفا با امضای توسعه و نیازمند تست
اتصال واقعی گوشی به VPS است.
