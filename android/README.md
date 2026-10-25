# namedP for Android · 1.0.0

A native, connection-first client for the NamedICMP web panel.
Android 8+ (API 26), arm64 phones and x86_64 emulators.
Download `namedP-1.0.0-android.apk` from the [shared stable release](https://github.com/namedkenzo/NamedClients/releases/latest).

## Experience

Home, Connections and Settings share the Windows client's forest/mint palette,
refined companion cards and original vector navigation marks.
System/light/dark themes, English/Persian layout, clear profile selection,
clean detail fields and a sticky Connect action keep the app usable on compact screens.
Dachshund and button animations remain lightweight and respect animation preferences.
No upstream UI/source is copied and no new Android UI dependencies are added.

Copy the full panel Config link → **Paste connection** → **Connect**.
Importing never connects automatically. **+** opens manual server/credential input; names are optional.
Tap a profile for Details, Save/Copy, relay-delay testing or per-app options.
The URI's `mode` owns routing; Home has no separate VPN/proxy switch.

Credentials remain masked until explicitly revealed.
Profiles are encrypted with Android Keystore AES-GCM; cloud backups are disabled.
Keys stay off process arguments. A numeric key and encryption-only key cannot both be active.

## Delay test and Gaming

**Test delay** sends `https://www.gstatic.com/generate_204` through the selected authenticated tunnel
and outbound. Verified TLS and HTTP 204 are required, with no redirect/direct fallback or automatic polling.
Results appear on Home, Connections and Details and remain in memory only.
It works before/during a connection without changing VPN routes or binding the SOCKS port.
TCP, TLS and response headers are timed; prior ICMP authentication is excluded.
This is not game ping, download speed or a leak test. The manual test contacts Google and uses some quota.

Settings → Gaming is optional and requires reconnecting.
It bounds UDP queues to 32 packets and drops datagrams queued for more than 150 ms.
This trades stale data for possible loss; lower RTT is not guaranteed.
Use Direct or UDP-capable SOCKS outbound, not HTTP. NamedICMP 0.4.2 adds corresponding output scheduling.

## Transport

The independent Go engine uses rootless ICMP ping sockets where the vendor/kernel permits.
VPN mode uses Android VpnService, excludes the app's own UID to avoid routing loops,
and passes the TUN descriptor to tun2socks 2.6.0 over a randomly named same-UID socket.
TCP and UDP are distinct flows. Proxy mode exposes loopback SOCKS5 only.
Per-app VPN requires at least one selected application other than namedP.

IPv4/IPv6 endpoints and AAAA-only family selection are supported.
Literal IPv6 URIs use brackets; Details accepts a bare address.
The VPN captures IPv6 rather than deliberately bypassing it; complete inner IPv6 forwarding is not promised.
DNS uses 1.1.1.1 / 1.0.0.1 through the VPN. Vendor restrictions or ICMP blocking are reported as failures.
Connected validates the tunnel and local startup, not access to every website.

## Updating and signing

Install the APK **over the previous app**; do not uninstall or clear data if you need saved profiles.
Version code 8 uses the same legacy development signing identity as 0.2.2-alpha,
with a **non-debuggable release** manifest. CI verifies the old/new certificate digest and
an emulator upgrade that preserves an encrypted profile. A missing signing cache fails the build
rather than generating a new incompatible identity.

This is a stable GitHub release, not a Play Store distribution or new owner-controlled production signing setup.
Keep backup links. Device/VPS throughput, leaks and vendor lifecycle behavior still need real-world validation.
Windows and Android binaries now live in one release; the web panel is unaffected.

## Build

JDK 17, Android SDK 35/build-tools 35.0.0, NDK 28.0.13004108, Gradle 8.11.1 and Go 1.26+:

```bash
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/28.0.13004108"
bash scripts/build_android_native.sh
gradle -p android testDebugUnitTest assembleDebug assembleRelease
```

The repository does not contain signing keys. Local builds use your own debug identity
and cannot update an APK signed with a different key.

## فارسی

هر دو کلاینت در یک ریلیز عادی و Latest منتشر شده‌اند. ظاهر اندروید با ویندوز هماهنگ‌تر شده،
کارت‌ها، فرم‌ها، تنظیمات و آیکن‌های ناوبری مرتب‌ترند و دکمهٔ اتصال هنگام اسکرول در دسترس است.
تم سیستم/روشن/تیره و متن فارسی/انگلیسی حفظ شده‌اند.

لینک Config پنل ← چسباندن کانفیگ ← اتصال. **+** برای ورود دستی است و نام اختیاری است.
حالت از خود کانفیگ خوانده می‌شود. تست تأخیر HTTPS و Gaming اختیاری مثل قبل هستند.

APK دیباگ‌پذیر نیست و با همان امضای توسعهٔ قبلی ساخته می‌شود تا روی نسخهٔ قبلی نصب شود و
کانفیگ‌ها باقی بمانند. برنامه را حذف نکنید و داده‌های آن را پاک نکنید. این نسخهٔ Play Store نیست.
