# namedP 1.0.1 · Windows + Android

One stable **Latest** release containing both clients; not a prerelease.
The NamedICMP web panel is unchanged and independently versioned.

## Download

- **Windows Setup EXE:** `namedP-1.0.1-windows-x64-setup.exe`
- **Windows Portable folder ZIP:** `namedP-1.0.1-windows-x64.zip` — extract every file.
- **Android APK:** `namedP-1.0.1-android.apk` — Android 8+, arm64 / x86_64.
- `SHA256SUMS` verifies the three packages. PNG files are credential-free interface previews.

## Windows

- The last link is remembered after closing and reopening, protected by Windows
  DPAPI CurrentUser. Atomic encrypted autosave, immediate-close flush, unreadable
  file protection and explicit **Forget** are included.
- Clearing an unsaved link before the data directory exists no longer reports an error.
- System/light/dark themes now share the refined forest/mint palette, cleaner
  companion/profile cards, keyboard focus rings, Gaming control and relay/traffic hierarchy.
- Connect and Test delay stay visible on short windows. Lightweight dachshund
  and button motion respect reduced-motion preferences.

## Android

- Matching visual refresh, original vector navigation, selected-profile accents,
  cleaner detail fields/settings, and a sticky Connect action.
- Fix a horizontal header child taking the entire row width. It could collapse
  the title and create a tall blank hero area, hiding the dog and connection card.
  Header labels now use bounded/wrap-content widths with ellipsis.
- New screenshot/layout assertions require a compact hero and a visible companion,
  in addition to theme, Persian layout and functional profile tests.
- **Same signing identity as 1.0.0.** Updating 1.0.0 → 1.0.1 is in-place; do not uninstall.

Config-owned VPN/proxy routing, Gaming, HTTPS relay-delay tests, TCP/UDP and
IPv4/IPv6 endpoints are retained. No web-panel or protocol changes are needed.
Windows VPN still carries IPv4 application TCP/UDP, guards other Internet IPv6
and retains LAN routes. Relay delay is not game ping or download speed.

## Important for old Android alpha users

The old alpha signing key was not retained by the earlier build script. The owner
approved the new 1.0.x identity. **Before removing an alpha, open each profile's
Details, Copy its full link and save it somewhere private.** Then uninstall the
alpha, install this APK and Paste your links. Uninstalling deletes old Keystore profiles;
they do not migrate across the certificate change. This reinstall is unnecessary
if you already installed stable 1.0.0.

The release APK is not debuggable. Private signing material is not committed,
printed, attached to public releases or cached publicly. Future builds either
use owner secrets or receive an authenticated encrypted, one-run handoff of the
same key; the APK certificate must match stable 1.0.0. See [signing](ANDROID_SIGNING.md).

Automated gates cover Go race/protocol compatibility, Windows DPAPI/restart/theme
and firewall/Wintun cleanup checks (global routes mocked), Setup install/uninstall,
Android unit/emulator UI tests and a profile-preserving same-key release upgrade.
Real-device/network behavior still varies. Windows app/Setup remain without
publisher Authenticode signing; SmartScreen may warn. This is not a Play Store build
or a security audit.

## فارسی

- هر دو کلاینت در یک ریلیز عادی و Latest منتشر شدند؛ وب‌پنل تغییر نکرده است.
- فراموش‌شدن کانفیگ ویندوز رفع شد؛ ذخیرهٔ خودکار محافظت‌شده، بازیابی بعد از بستن،
  ذخیره هنگام خروج و گزینهٔ Forget اضافه شده‌اند.
- ظاهر هر دو نسخه یک‌دست‌تر شد: رنگ‌های روشن/تیره، کارت‌ها، فرم‌ها و ناوبری مرتب‌تر.
  دکمهٔ اتصال همیشه در دسترس است و انیمیشن‌های سبک حفظ شده‌اند.
- ارتفاع نامتعارف بخش بالای اندروید که سگ و کارت اتصال را پایین می‌برد اصلاح شد
  و تست تصویری برای دیده‌شدن آن‌ها اضافه شد.
- اگر اندروید ۱٫۰٫۰ دارید، همین APK را روی آن نصب کنید؛ امضا ثابت مانده است.
- اگر نسخهٔ آلفا دارید، **اول لینک همهٔ کانفیگ‌ها را Copy و جای امن ذخیره کنید**؛
  سپس آلفا را حذف، APK جدید را نصب و لینک‌ها را Paste کنید. انتقال خودکار نداریم.
- ویندوز دو بستهٔ نصبی EXE و ZIP پوشه‌ای دارد؛ قبل از آپدیت برنامه را ببندید.
