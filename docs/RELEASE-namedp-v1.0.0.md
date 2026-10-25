# namedP 1.0.0 · Windows + Android

The first coordinated, non-prerelease namedP download: both clients in one place.
The NamedICMP web panel is unchanged and keeps its own version/release.

## Downloads

- **Windows Setup:** `namedP-1.0.0-windows-x64-setup.exe` — choose the install folder and shortcuts.
- **Windows Portable:** `namedP-1.0.0-windows-x64.zip` — extract the entire folder; launch `namedP.exe`.
- **Android:** `namedP-1.0.0-android.apk` — Android 8+, arm64 phones / x86_64 emulators.
- `SHA256SUMS` covers all three application packages. Platform-prefixed PNG files are offline UI previews, not live connection evidence.

## Fixed

- Windows now remembers the last connection link after closing or restarting.
  Autosave uses Windows DPAPI **CurrentUser**, atomic ciphertext replacement and
  a final save on close, including closing immediately after pasting.
- Clearing the input removes the stored link. **Forget** removes it with confirmation.
  Unreadable encrypted data is reported, not silently overwritten with an empty profile.
- Installed and portable Windows copies use the same account-local saved link.
  Neither connects automatically on startup. Keys stay masked and off command lines.

## Visual polish on both platforms

- Matching forest/mint and warm-neutral palettes; clearer light/dark contrast.
- Refined companion cards, subtle gradients, quieter outlines, consistent spacing,
  stronger typography and cleaner connection/relay-delay/traffic hierarchy.
- Windows gets a themed appearance picker, compact Gaming switch and keyboard focus rings.
- Android gets original vector navigation marks, selected-profile accents, cleaner
  detail fields and settings cards. Connect stays visible above navigation while content scrolls.
- System theme remains the default; reduced-motion preferences and lightweight
  dachshund/button animations are preserved. No image downloads or new Android UI dependencies.

## Kept intact

Configuration-owned VPN/proxy routing, Gaming, verified HTTPS relay-delay tests,
TCP/UDP transport, IPv4/IPv6 endpoints and session cleanup. No protocol or
web-panel changes are required for this release. Windows full-device VPN carries
IPv4 TCP/UDP and guards other Internet IPv6; LAN routes remain. Relay delay is
not game ping and Gaming does not promise a lower RTT.

## Updating and verification

Close old Windows copies before updating.

**Android alpha → 1.0.0 requires a one-time reinstall.** The earlier workflow cached
the wrong keystore path, so the legacy private signing key could not be recovered.
The owner explicitly approved a new identity. Before uninstalling the old app,
open each profile's Details, Copy its complete link and keep it somewhere private.
Then uninstall the alpha, install the new APK and Paste your links. Uninstalling
removes the old Keystore profiles; they do not migrate automatically.

The new APK is non-debuggable. Future builds must match the stable 1.0.0 certificate
and use owner signing secrets; missing identity fails closed rather than silently
creating another incompatible update. No private key is cached publicly. A recovery
backup is encrypted to the owner's private workspace. See [signing](ANDROID_SIGNING.md).
The emulator also verifies a profile-preserving upgrade between builds sharing the new identity.

This is a stable **GitHub release**, not a security audit or Play Store release.
Windows Setup/app still lack publisher Authenticode signing; SmartScreen may warn.
Android is independently signed for APK distribution, not a Play Store release.
Actual device, network and VPS behavior still varies. Keep backup configuration links.

Automated gates cover Go race/protocol tests, WPF profile restart/DPAPI and theme
checks, real Windows firewall/Wintun setup + cleanup tests (global routes mocked),
installer install/uninstall, Android unit/emulator tests and signing continuity.

## فارسی

هر دو کلاینت در یک ریلیز عادی و Latest منتشر شده‌اند؛ وب‌پنل تغییر نکرده است.

- مشکل فراموش‌شدن کانفیگ ویندوز پس از بستن برنامه رفع شد؛ لینک با محافظت
  حساب ویندوز ذخیره می‌شود و با بازکردن دوباره برمی‌گردد. «Forget» برای پاک‌کردن است.
- ظاهر هر دو نسخه یک‌دست‌تر شد: رنگ‌های روشن/تیرهٔ هماهنگ، کارت‌ها و فرم‌های
  مرتب‌تر، ناوبری تازهٔ اندروید و نمایش واضح‌تر وضعیت، تأخیر و ترافیک.
- اتصال از خود `mode` کانفیگ پیروی می‌کند. Gaming، تست تأخیر HTTPS و انیمیشن‌های
  سبک حفظ شده‌اند؛ دکمهٔ اتصال اندروید هنگام اسکرول هم در دسترس است.
- ویندوز هر دو بستهٔ نصبی EXE و ZIP پرتابل دارد. پیش از آپدیت برنامه را ببندید.
- APK اندروید دیباگ‌پذیر نیست و با تأیید شما امضای جدید دارد. پیش از حذف آلفا،
  لینک همهٔ کانفیگ‌ها را از جزئیات Copy و جای امن ذخیره کنید؛ سپس آلفا را حذف،
  نسخهٔ جدید را نصب و لینک‌ها را Paste کنید. کانفیگ‌ها خودکار منتقل نمی‌شوند.
  ویندوز هنوز امضای ناشر ندارد. عادی‌شدن ریلیز به معنی تضمین امنیت یا نبود هرگونه باگ نیست.
