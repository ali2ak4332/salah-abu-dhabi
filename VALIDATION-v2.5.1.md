# Salah Abu Dhabi 2.5.1 validation

19 October 2026. Package `ae.prayertimes.abudhabi`, version code **24**, Android 8/API26 minimum, API35 target.

## What was reproduced and fixed

The pristine v2.5 source ZIP was extracted completely and all 254 original files were verified byte-for-byte before its baseline run. The default native suite reproduced **40 passed, 9 failed** (49 tests; 170.261 seconds). Its raw log is `tests/native/results-v2.5-baseline.txt`.

- Six failing tests reflected Horizontal names below the 9dp reading floor. Names now use `max(9dp, 0.78 × time size)`. Short Horizontal widgets reclaim decorative top/bottom pill padding so Arabic fallback glyphs and the prayer times also retain the reading floor.
- Compact could omit the countdown at an ordinary 160×160dp size. Its prayer fit now reserves a readable header and measures every relevant localized countdown/clock label, including AM and PM. The optional date gives way first. Prayer fonts remain stable as adhan/iqamah/countdown phases change.
- A legacy test incorrectly required zero countdown display alarms. Its replacement checks the intended next-adhan/iqamah behavior, legacy receiver cancellation, one current one-shot RTC alarm, and no idle quota.
- The fractional-size geometry test compared live seconds as static content. A normal tick from 10:00 to 09:59 failed an otherwise identical layout. It now compares the timer's format, countdown mode and geometry; separate assertions still check its actual target base and displayed numeral script. Every static field retains exact-text checks.

## Countdown boundary safety

Live seconds remain when Android's native digits exactly match the selected Western or Eastern Arabic digits. Other scripts use localized H:MM snapshots. In the last minute, Compact stops the live timer and displays an absolute clock target, such as “Iqamah at 12:30”. This avoids freezing a relative “in 0:01” label. The accessible label includes the target clock, date and Abu Dhabi timezone.

One non-wakeup display alarm selects the earliest applicable minute snapshot, final-minute guard or target boundary. Horizontal alone still refreshes its amber iqamah highlight at expiry. No wakeup alarm, allow-while-idle display work, foreground service, new permission or extra sound was added.

**Residual Android limitation:** standard Chronometer has no RemoteViews zero-clamp. Android or the launcher can defer the guard update as well as expiry. If the guard itself is withheld beyond expiry, the old live Chronometer can still become negative until the launcher applies a refresh. This is a best-effort mitigation, not a guarantee. A delivered final-minute fallback is static and cannot tick negative; it can still remain stale if later refreshes are withheld. Opening the app requests a refresh.

The delayed-refresh test deliberately leaves an expired live base, then invokes Android's actual next-tick formatter at the advanced clock time. It accepts Android's Unicode minus sign as well as ASCII. This documents the platform behavior without depending on Robolectric's simulated launcher visibility callbacks or claiming to simulate real Doze delivery.

The display phase no longer unconditionally adds a millisecond before looking up the next adhan. Only an exactly reached adhan is advanced past. A still-future adhan in the last nanosecond remains the target. Alert/reminder scheduling is unchanged.

## Final execution

**Final complete native run: 55/55 tests passed, 180.411 seconds.** Raw output: `tests/native/results-v2.5.1.txt` (also copied to `tests/native/results.txt`). The run used the exact production files recorded by the bundled SHA-256 manifest and the same build that produced the signed APK.

- Full widget matrix: 3,360 profiles across both shapes, English/Arabic, Western/Eastern digits, 12/24-hour time, five font scales and three widget text preferences.
- Compact countdown phase/font stability: 1,680 profiles. Header omissions are limited to some 110×110 and 130×117dp profiles; all tested 160×160 profiles show the header. All ten prayer name/time fields remain visible.
- Final-minute clock labels: 240 profiles across both languages/numerals, 12/24-hour time, three font scales, all five adhan and iqamah targets.
- Physical-pixel/fallback glyph bounds at hdpi/xhdpi/xxhdpi/xxxhdpi; exact/fractional resize selection and parcel/reapply; 0 bitmap bytes for both 16-variant RemoteViews.
- API26/29/35 startup, boundaries, delayed-refresh cases and widget lifecycle; stale/rejected instance isolation, callbacks and removal; disabled/custom iqamah intervals and Friday Dhuhr exclusions; missing data, midnight and expiry.
- Native digit-script choices checked under US, Arabic, Persian and Bengali system locales. Unsupported scripts cannot override the user's saved numeral choice.
- All four pure-Java suites pass: 2,235 prayer boundaries; 2,235 alert deliveries and 6,705 reminder boundaries; 23 formatting checks; 2,235 estimated-iqamah boundaries including the last nanosecond, 64 Friday exclusions, custom offsets, disablement, midnight and final saved coverage.
- API35 direct build compiles and packages successfully. Java8/deprecation warnings remain; no compilation errors. Native harness resource-ID/deprecation diagnostics are host/runtime warnings, not suppressed test failures.

Native previews are produced from actual production RemoteViews and Android graphics, not HTML mockups. English/Arabic Compact and Horizontal previews, including the 250×40dp strip, were visually inspected.

## Data, signing and scope

- The official 447-day timetable and original chime are byte-identical in the old and new APKs.
- Offline provenance validation passes: 365 official 2027 dates, 283 overlapping dates exact, original 365-row prefix unchanged, no extrapolation.
- `AlertScheduler`, `AlarmReceiver`, `AppPreferences`, `Schedule`, providers and drawing/UI classes other than the stated changes are unchanged. MainActivity changes are English/Arabic explanatory text only. Production implementation changes are confined to PrayerWidget and the sub-millisecond PrayerPhase correction, plus version metadata.
- Same original certificate SHA-256: `4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`. This was compared to the actual prior v2.5 APK. The final APK passes v2/v3 signature verification and zip alignment; its DEX matches the final tested build. APK SHA-256: `025106956b36798f73209e9f73b44f62eeb76b42f1b7d88d3e201b024389a9eb`.
- `tests/native/app-source-v2.5.1.sha256` records final production file hashes. Final build/test/signature logs are bundled. Private signing material, SDKs, dependency caches and credentials are excluded from this source archive.

## Physical-device checks still required

No phone was connected. Host tests cannot establish Samsung/OEM launcher behavior, actual sleep/wake alarm delivery, notification permission/chime playback, or an on-device upgrade. Install over the existing app without uninstalling or clearing data; then check both placed widgets, change language/numerals, resize them and use Test chime. The unchanged package, original certificate and increased version code provide update compatibility, but are not a claim that a physical install was performed.

Platform references: [Chronometer source](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/Chronometer.java), [RemoteViews/widget supported views](https://developer.android.com/develop/ui/views/appwidgets#views), [AlarmManager RTC](https://developer.android.com/reference/android/app/AlarmManager#RTC), [Doze alarm restrictions](https://developer.android.com/training/monitoring-device-state/doze-standby).
