# Salah Abu Dhabi 2.1.2 — widget corrections

Validated 17 October 2026. Android package `ae.prayertimes.abudhabi`, version name2.1.2, code15. Started from the latest saved v2.1.1 source (source version14) and APK (version13).

## Changes requested

1. Both widget providers retain today's five prayer rows at every supported size, including compact2×2 and heights below220dp. Twelve compact layout variants now include the list. Compact date, target, countdown and independent40dp refresh/settings controls remain. Roomier layouts at220dp and above, including full-height Arabic2×3, are byte-identical to v2.1.1.
2. Countdown numerals follow the same saved choice as prayer times. Android's native Chronometer formats through DateUtils/system locale, rather than the app's numeral preference. Compare its formatter output with the chosen numeral mapping: matching numerals retain live native seconds; a mismatch uses the existing localized approximate HH:MM snapshot, calculation timestamp and one inexact non-wakeup minute refresh. This avoids per-second app alarms/services and mixed numerals. Changing back to matching numerals automatically restores live seconds and cancels the snapshot alarm. Main-screen seconds and stored preferences are not changed. Widget help explains this behavior in Arabic and English.

Framework source inspected: https://android.googlesource.com/platform/frameworks/base/+/HEAD/core/java/android/widget/Chronometer.java (17 October2026). Its elapsed-time formatting uses DateUtils; no caller tick listener can be installed into the launcher's RemoteViews to substitute app numerals. The localized minute fallback is deliberate, not a claim that custom live numerals are supported.

## Verification

- Signed API35 build succeeded with Java17.0.20, ECJ3.40.0, platform35 revision2 and Build Tools35.0.0. Five existing deprecation warnings, no compile errors.
- Native Android/Skia/Robolectric suite: **OK (29 tests), 44.75 seconds**. Existing nine main-app, fourteen widget and four injected widget-failure cases pass, plus two new grouped numeral/fallback tests.
- Updated size assertions require all five rows at every supported size, for both languages/providers and all three text-size choices, through the real RemoteViews responsive selector. Existing bounds checks cover140×160 minimum,160×160,219/220 boundary,160×240,200×300 and300×300;110×110 remains unsupported smoke-only. Existing transparency/theme, large-font,12-hour, expiry, iqamah/adhan, callback/settings lifecycle and API26/29 parcel/apply/reapply checks pass.
- Sixteen explicit system-language (en-US/ar-AE), app-language, Western/Eastern-numeral and widget-size combinations check both prayer-row and countdown numerals, matching live-timer visibility, stopped mismatched timer, approximation disclosure, timestamped snapshot advancement and measured native bounds. A separate test checks one non-wakeup fallback refresh, replacement without accumulation and cancellation on return to matching numerals.
- Existing snapshot alarm isolation test preserves prayer/reminder/iqamah alarms. The existing four launcher-rejection cases remain passing, preserving v2.1.1 crash containment.
- Pure-Java tests passed: all2,235 prayer delivery/boundary cases,6,705 reminder boundaries,2,235 estimated-iqamah boundaries,64 Friday exclusions,15 formatting and8 date/locale/time-zone checks, plus midnight/coverage/expiry/custom-offset checks.
- Native rendered Arabic minimum compact, Eastern-numeral fallback and full-height tall images visually inspected. Current examples are retained under `tests/native/screenshots/v2.1.2/`; older screenshots remain historical.
- All scheduling, notification, preference and estimated-iqamah policy classes are byte-identical to v2.1.1. Timetable and chime are byte-identical in both source and signed APK. Full-height tall XML variants are byte-identical. The main screen changes only the About version/help text, not its one-page layout or five rows.
- Same package and original signing certificate; APK v2/v3 verification succeeds. Certificate SHA256 `4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`. No new permission or Internet access.
- Complete source includes build files/script, tests, history, designs/licenses and Awqaf provenance; private signing material and downloaded build/test dependencies are excluded.

## Exact artifacts

Signed APK SHA256: `96959be71976d3324e5b0a881af861ba6fecb5ed7e141b9ba86fff95d5990a73`.
Tested unsigned resource APK SHA256: `6c2df6ed8bd108a9d1dd61b7e2152f7c45546972826b5af5a7ecb1a7dc312437`.
PrayerWidget.java SHA256: `8e2dbb025cc70effc316eca748b45cb62d02b2314e95e28ab9b92664e0d7bd84`.
Timetable SHA256: `972f2e4b08c57822bdb82020e48de01e7ca689d1ba539dc62be3294df228790a`.
Chime SHA256: `9ad738a832c2ef5819e1494e60ccc77ef4751b0100316379d6d2bd9a7640b551`.

Current production hashes: `tests/native/app-source.sha256`; current full test output: `tests/native/results.txt`. Prior v2.1.1 output/hashes have versioned filenames. The earlier detailed visual test matrix remains in `tests/native/VALIDATION-v2.1.md`; its below220dp next-only expectation has been superseded by this release's all-five requirement.

## Limits

Host native tests are not a physical phone, OEM launcher, TalkBack or real notification/sound-delivery check. Launcher padding/grid choices can change actual content size. Very small unsupported sizes may clip; resize taller for larger text. Android may defer minute snapshots and adhan/estimated-iqamah phase changes, particularly during sleep/Doze. The target time, approximation mark, timestamp and refresh control remain available; snapshots do not wake a sleeping phone. No new notification scheduling policy was introduced. A phone-specific crash cause remains unconfirmed without that device's crash log.
