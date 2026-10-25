# NamedClients

**خانهٔ کلاینت‌های namedP برای اتصال با کانفیگ‌های وب‌پنل NamedICMP.**

[English](README.md) · [دانلود ویندوز](https://github.com/namedkenzo/NamedClients/releases) · [وب‌پنل](https://github.com/namedkenzo/NamedICMP)

| پلتفرم | وضعیت | محل کد |
| --- | --- | --- |
| ویندوز ۱۰/۱۱، x64 | namedP نسخهٔ `0.2.0-alpha` | [`windows/namedP`](windows/namedP) |
| اندروید ۸ به بالا، arm64 | APK آزمایشی `0.2.2-alpha` | [`android`](android) · [دانلود](https://github.com/namedkenzo/NamedClients/releases/tag/android-v0.2.2-alpha) |

## ویندوز

فایل ZIP را از Releases دانلود و کامل استخراج کنید، سپس `namedP.exe` را
با دسترسی Administrator اجرا کنید. لینک کامل `pingtunnel://` را از پنل
کپی کنید و VPN کل سیستم یا پروکسی SOCKS5 را انتخاب کنید.

- تم پیش‌فرض مطابق سیستم، به‌همراه حالت روشن و تیره.
- داچهوند متحرک برای حالت‌های آماده، اتصال و وصل‌شده.
- تونل مستقل TCP و UDP، رمز عددی یا رمزنگاری مطابق پروفایل کاربر.
- ترافیک برنامه‌ها در VPN ویندوز IPv4 است و می‌تواند از اتصال بیرونی IPv4 یا IPv6 عبور کند؛ سایر مقصدهای اینترنت IPv6 مسدود می‌شوند، اما آدرس دقیق اتصال و ICMPv6 ضروری محلی حفظ می‌شوند. مسیرهای LAN باقی می‌مانند.

این کلاینت هنوز آلفا است. تست ساخت، پروتکل و رابط انجام می‌شود؛ آزمون کامل
شبکهٔ واقعی روی ویندوز و VPS همچنان لازم است. [راهنمای ویندوز](docs/NAMEDP.md).

## اندروید

**جدید در ۰٫۲٫۱ آلفا:** «تست تأخیر» درخواست `https://www.gstatic.com/generate_204` را از داخل کانفیگ و خروجی انتخاب‌شده می‌فرستد، نه اینترنت مستقیم گوشی. زمان TCP، TLS معتبر و پاسخ ۲۰۴ در خانه، فهرست و جزئیات دیده می‌شود؛ قبل یا هنگام اتصال قابل اجراست و تست سرعت نیست. نسخهٔ ویندوز تغییر نکرده است.

حالت از `mode` کانفیگ خوانده می‌شود؛ انتخاب دوتایی در خانه حذف شده است. با لمس پروفایل، جزئیات، ذخیره/کپی، تست تأخیر HTTPS و گزینه‌های حالت اتصال باز می‌شوند. تنظیمات شبکه و کنترل ارسال سرور در [NamedICMP 0.3.1](https://github.com/namedkenzo/NamedICMP/releases/tag/v0.3.1) هستند.

لینک Config پنل را کپی کنید ← **Paste** ← **اتصال**. دکمهٔ **+** برای ورود دستی سرور و رمز عددی یا رمزنگاری است؛ نام اختیاری است.

نسخهٔ اولیهٔ اندروید با رابط تازه، Home/Connections/Settings، متن‌های فارسی/انگلیسی، تم خودکار سیستم و داچهوند متحرک ساخته شده است. APK امضای توسعه دارد و نسخهٔ تولیدی یا Play Store نیست. تست رابط و اجرای هسته بدون روت روی شبیه‌ساز پاس شده‌اند؛ اتصال واقعی گوشی به VPS، سرعت و نشت شبکه هنوز به آزمون نیاز دارند. [راهنمای اندروید](android/README.md) · [دانلود APK آزمایشی](https://github.com/namedkenzo/NamedClients/releases/tag/android-v0.2.2-alpha).

[تصویر روشن](https://github.com/namedkenzo/NamedClients/releases/download/android-v0.2.2-alpha/light-idle.png) · [تصویر تیره](https://github.com/namedkenzo/NamedClients/releases/download/android-v0.2.2-alpha/dark-idle.png). ظاهر و امکانات ویندوز در این مرحله تغییر نکرده‌اند.

## کلاینت‌های دیگر PingTunnel

- [کلاینت خط فرمان اصلی PingTunnel](https://github.com/esrrhs/pingtunnel/releases)
- [کلاینت گرافیکی جامعهٔ PingTunnel برای اندروید و دسکتاپ](https://github.com/itismoej/pingtunnel-client/releases)

این مخزن فقط برای کلاینت‌هاست. نصب‌کنندهٔ وب‌پنل در
[NamedICMP](https://github.com/namedkenzo/NamedICMP) نگهداری می‌شوند.

نسخهٔ ۰.۲ از کانفیگ IPv6 و دامنه با رکورد AAAA و پارامتر `family=ipv6` پشتیبانی می‌کند؛ وب‌پنل باید نسخهٔ ۰.۴ یا جدیدتر باشد. قالب آی‌پی در لینک، `[IPv6]` است. آزمایش روی گوشی/ویندوز و VPS واقعی همچنان لازم است.
