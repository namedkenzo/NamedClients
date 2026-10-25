# namedP Android 0.1.3-alpha — profile-driven routing and corrected key masking

Includes the configuration-owned VPN/Proxy/per-app routing and Details screen
from [0.1.2](RELEASE-android-v0.1.2-alpha.md). There are no separate home mode
toggles. Tap a configuration to edit/save/copy it, run an authenticated ICMP Test
or Connect/Disconnect. The URI controls the service mode after restart too.

Fixes a visual privacy bug found during screenshot review: Android's single-line
editor replaced the password transformation, showing the key despite an
unchecked Show key control. The password transform is now applied last and an
emulator regression checks masked → explicit reveal → masked behavior. Prefer
this version over 0.1.2; existing APKs/checksums are not silently overwritten.

For server-side speed-test stability and reversible network tuning, update
[NamedICMP to 0.3.1](https://github.com/namedkenzo/NamedICMP/releases/tag/v0.3.1)
using the current installer and choose Update. That work is separate from the
client. Tests passed in controlled environments do not establish that the
reported VPS/carrier speed-test disconnect is fully fixed.

This remains a debug-signed Android 8+ alpha (arm64 phones/x86_64 emulators), not
a production/Play Store release. Phone/VPS throughput, per-app routing, leak
and vendor acceptance testing remains necessary. Numeric keys are 1–2147483647
as required by the existing wire format, not arbitrary-length decimal passwords.
Windows is unchanged. Provider anti-DDoS rules cannot be overridden by sysctls.

## فارسی

حالت اتصال از `mode` کانفیگ خوانده می‌شود و دو دکمهٔ جدا در خانه وجود ندارد.
لمس کانفیگ، صفحهٔ جزئیات، ویرایش، ذخیره، کپی، تست ICMP و اتصال را باز می‌کند.
نمایش کلید در کادر تک‌خطی اصلاح شد؛ کلید فقط با انتخاب «نمایش کلید» آشکار است.
نسخهٔ ۰.۱.۳ را به‌جای ۰.۱.۲ استفاده کنید. اصلاحات سرور در NamedICMP ۰.۳.۱
جداگانه منتشر شده‌اند و تست روی VPS خودتان همچنان لازم است.
