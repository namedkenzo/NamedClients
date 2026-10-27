# namedP 1.0.2 · Connection resilience

## English

- Windows and Android now share an established-tunnel health watchdog. One missing synthetic probe no longer tears down the whole-device VPN while authenticated payload/ACK traffic is still arriving.
- A real silent blackhole still disconnects after repeated failed checks and a bounded silence grace period. Startup credential/reachability checks remain unchanged.
- Receive activity uses monotonic time and counts only packets that pass tunnel peer/protocol/credential checks, not ordinary kernel echo replies.
- Added loss-of-probes-with-live-data, true-blackhole, cancellation and unauthenticated-echo regression tests.
- No UI, profile-format or encryption-mode changes. Android retains the existing stable signing identity and a higher versionCode for in-place updates.

For the matching panel-side queue/backpressure fixes use namedICMP v0.5.1 or newer. This does not bypass provider filtering or promise a particular ping/speed.

## فارسی

- کلاینت ویندوز و اندروید با از دست‌رفتن یک تست پینگ، در حالی که داده یا ACK معتبر هنوز می‌رسد، کل VPN را قطع نمی‌کنند.
- قطعی واقعی و سکوت کامل همچنان پس از چند بررسی ناموفق و مهلت محدود شناسایی می‌شود؛ بررسی اولیهٔ اتصال و رمز تغییر نکرده است.
- زمان‌سنجی بر پایهٔ ساعت یکنواخت است و پاسخ معمولی Ping کرنل به‌عنوان زنده‌بودن تونل پذیرفته نمی‌شود.
- تست‌های از دست‌رفتن پاسخ تست سلامت، ترافیک زنده، قطعی واقعی و توقف اضافه شدند.
- رابط، کانفیگ و روش رمزنگاری عوض نشده‌اند. امضای پایدار اندروید حفظ شده تا آپدیت روی نسخهٔ فعلی نصب شود.

برای اصلاحات صف و دانلود سمت پنل، namedICMP را هم به ۰.۵.۱ یا جدیدتر ارتقا دهید. محدودیت ارائه‌دهنده با این تغییرات دور زده نمی‌شود و سرعت یا پینگ مشخصی تضمین نمی‌شود.
