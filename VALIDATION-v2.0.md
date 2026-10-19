# Salah Abu Dhabi 2.0 — release validation

16 October 2026. Package `ae.prayertimes.abudhabi`; version code 12; Android API 26 minimum / API 35 target.

## Passed on the final source/build

- Android API 35 resource compilation, Java compilation and DEX generation using Android Build Tools 35.0.0 and Eclipse ECJ 3.40.0 on Java 21.
- 447 continuous saved days from 2026-10-11 through 2027-12-31; exact original 365-row preservation; all 283 overlapping 2027 rows match the freshly retrieved official Awqaf responses. No duplicate/missing dates, invalid ordering or projections.
- 2,235 prayer boundaries/deliveries, 6,705 advance-reminder boundaries and 2,235 adhan/estimated-iqamah transitions; 64 Friday Dhuhr exclusions, custom/disabled intervals, UAE midnight, final Isha, previous end-date continuation, new expiry and date-picker bounds.
- 15 time/numeral/countdown checks plus eight widget-date/locale/time-zone checks.
- Nine grouped native Android/Skia Robolectric test scenarios, comprising 18 English/Arabic layout profiles, six additional narrow/normal iqamah renders, 40 live phase transitions, Android 15 system insets, calendar selection through the final date, actual dark/navy/sand preference changes, settings/recreation, expiry, switch geometry/accessibility/on-off and startup on API 26, 29 and 35 in both languages.
- No home scrolling containers. All five individually identified prayer names/times, active badge, hero, countdown units, date controls, settings/language controls, alert control and source footer fit the tested visible bounds. Profiles include 320×480, 360×640, 360×800, 412×892, 393×852, 412×915 and 640×360, with font scale up to 2.0. Extra inset cases reserve 24dp each for top/bottom system bars.
- Arabic and English main-screen and estimated-iqamah PNGs visually reviewed after the final changes. Exact Figma-exported icons are present locally, retain their slot dimensions and have production callsites. No temporary Figma URL is used at runtime.
- Signed APK verified with the identical v1.9.1 signing certificate. Package/version metadata checked. APK timetable bytes equal the final source CSV; existing chime bytes are unchanged; APK contains no bundled TTF/OTF files or private signing material.

Public certificate SHA-256: `4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`.

APK SHA-256: `d34be166eb327a93019f5ce455945c50c705a49d0c5c55297a83f7bea3b913cf`.

## Reproducibility and evidence

- `build-local.sh` runs the production build and pure-Java suites.
- `tests/native/` includes pinned dependencies, setup/run scripts, source tests and the native run report. Downloaded test/SDK binaries and caches are excluded.
- `provenance/awqaf-2027/` contains official raw monthly responses, request/source mapping, hashes and offline validation.
- Original v1.9.1 source was recovered from the latest saved complete source archive, rather than replacing it with an older GitHub snapshot. No repository push, merge or publication was performed.

## Important limits

This was **not a physical-phone installation or emulator-boot test**. No connected physical phone or hardware-accelerated emulator was available. Host-side tests execute the production activity/classes and compiled resources under Android runtimes; they do not establish behavior of the installed DEX on every OEM device.

Real notification delivery/timing, permission dialogs, sound, battery-saving/reboot behavior and launcher widget appearance still need device verification. Widget code and layouts were retained; this release does not claim a new physical-launcher test.

The user's earlier v1.9 crash remains device-specific and unconfirmed. The safer v1.9.1 platform-font approach is preserved, with no runtime bundled-font decoding. A successful host-side startup cannot prove that particular phone's crash has been resolved.

The small-screen fixes affect only responsive sizing. Typical-screen design, saved preferences and official timing remain unchanged. Fonts use Android's supported platform fallback chain, so exact Arabic font metrics can vary by device. Future official timetable revisions remain possible; no automatic data updates are implemented.
