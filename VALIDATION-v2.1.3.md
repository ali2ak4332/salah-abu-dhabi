# Salah Abu Dhabi 2.1.3 — widget spacing

17 October2026. Package ae.prayertimes.abudhabi; version2.1.3/code16. Latest saved v2.1.2 source/APK identities were resolved before editing; existing working files matched that release.

## Focused layout change

The date and phase label share the existing40dp control header instead of occupying separate rows. At the supported140×160dp minimum, the compact five-prayer list grows from62dp to72dp. Larger widgets allocate more height to prayer rows and less to the next-prayer/countdown area. Prayer-name/time auto-size limits increase by2sp in all24 layout variants. Both independent40dp controls, weekday/date, next target, countdown, refresh hint/timestamp, all five rows, themes and transparency remain. Full-height Arabic2×3 and compact2×2 remain supported. Existing numeral selection and fallback behavior is unchanged.

## Validation

- Signed API35 build and all existing pure-Java suites pass:2,235 prayer/estimated-iqamah boundaries,6,705 reminder boundaries,64 Friday exclusions,15 format/countdown and8 date/locale/time-zone checks, plus midnight/expiry/custom offsets.
- Unchanged native Android/Skia regression suite: **OK (29 tests),43.723 seconds**. Includes English/Arabic, both providers, supported minimum/resized layouts, all text-size choices, large system fonts,12-hour/Western/Eastern numerals, numeral fallback/recovery, themes/transparency, phase/reapplication/state expiry, settings lifecycle, API26/29 compatibility and rejected-widget startup containment.
- The first layout pass exposed a clipped localized minute counter at the smaller expanded size. A fixed20dp minimum countdown area corrected it before the passing final run. No regression assertion was relaxed or test added for this layout-only change.
- Final native Arabic compact140×160, expanded200×300 and large-font Eastern-numeral renders visually inspected. Current samples: tests/native/screenshots/v2.1.3/. Earlier renders/reports remain historical.
- PrayerWidget.java (including numeral/failure handling), scheduling, preference and estimated-iqamah logic are byte-identical to v2.1.2. Timetable/chime bytes are identical in both source and APK. MainActivity changes only its About version title. No main-screen layout, permission or notification policy changed.
- Original certificate SHA256:4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af. APK v2/v3 verifies; same package and increased code preserve update compatibility. No phone installation was performed.
- Java17.0.20, ECJ3.40.0, official Android platform35 revision2 / Build Tools35.0.0. Five existing deprecation warnings; no compilation errors.

Signed APK SHA256:99535527716d70e997a3aa0f736628528f9e49ac3bd83360017469906b99e922.
Unsigned tested resource APK SHA256:ccdb7bdf148a7124fc99e8f8fa4dc7b592daa2367ce29c2a8fb4470fbdcba018.
Exact current production hashes:tests/native/app-source.sha256; full native output:tests/native/results.txt. Prior v2.1.2 hashes/output have versioned filenames.

## Limits and packaging

Host rendering is not an OEM launcher, physical phone, TalkBack, real notification/chime or Doze check. Launcher padding/grid affects available space. Existing minute-snapshot and delayed-boundary limitations remain; use refresh when stale. Unsupported110×110dp is smoke-only. Private signing keys/passwords, build products and downloaded SDK/test caches are excluded from the complete source ZIP; build files, scripts, tests, history, licenses/designs and official Awqaf provenance are retained.
