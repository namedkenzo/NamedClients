# NamedClients

**خانهٔ کلاینت‌های namedP برای اتصال به سرور namedICMP.**

[English](README.md) · [دانلود ویندوز](https://github.com/namedkenzo/NamedClients/releases) · [پنل و سرور](https://github.com/namedkenzo/NamedICMP)

| پلتفرم | وضعیت | محل کد |
| --- | --- | --- |
| ویندوز ۱۰/۱۱، x64 | namedP نسخهٔ `0.1.0-alpha` | [`windows/namedP`](windows/namedP) |
| اندروید | در برنامهٔ توسعه؛ هنوز APK ساخته نشده است | [`android`](android) |

## ویندوز

فایل ZIP را از Releases دانلود و کامل استخراج کنید، سپس `namedP.exe` را
با دسترسی Administrator اجرا کنید. لینک کامل `pingtunnel://` را از پنل
کپی کنید و VPN کل سیستم یا پروکسی SOCKS5 را انتخاب کنید.

- تم پیش‌فرض مطابق سیستم، به‌همراه حالت روشن و تیره.
- داچهوند متحرک برای حالت‌های آماده، اتصال و وصل‌شده.
- تونل مستقل TCP و UDP، رمز عددی یا رمزنگاری مطابق پروفایل کاربر.
- حالت VPN روی IPv4 است؛ IPv6 هنگام اتصال مسدود می‌شود و مسیرهای LAN باقی می‌مانند.

این کلاینت هنوز آلفا است. تست ساخت، پروتکل و رابط انجام می‌شود؛ آزمون کامل
شبکهٔ واقعی روی ویندوز و VPS همچنان لازم است. [راهنمای ویندوز](docs/NAMEDP.md).

## اندروید

فعلاً فقط محل توسعه ایجاد شده است؛ کد برنامه یا فایل نصب اندروید موجود نیست.
وضعیت انتشار عمومی یا خصوصی اندروید باید جداگانه تعیین شود.

## کلاینت‌های دیگر PingTunnel

- [کلاینت خط فرمان اصلی PingTunnel](https://github.com/esrrhs/pingtunnel/releases)
- [کلاینت گرافیکی جامعهٔ PingTunnel برای اندروید و دسکتاپ](https://github.com/itismoej/pingtunnel-client/releases)

این مخزن فقط برای کلاینت‌هاست. نصب‌کنندهٔ سرور و وب‌پنل در
[NamedICMP](https://github.com/namedkenzo/NamedICMP) نگهداری می‌شوند.
