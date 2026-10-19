# Salah Abu Dhabi2.1.4 — simplified, consistent widget

17 October2026; package ae.prayertimes.abudhabi; version2.1.4/code17. Started from the resolved latest v2.1.3 source/APK (source version16/APK version15).

## Requested changes

Removed the marked settings icon, Next Adhan label, duplicate target clock time and visible updated-at timestamp/hint. Date, refresh, next prayer/countdown, five prayer rows, active highlight and themes remain. The explicit estimated-iqamah label is retained only during estimated iqamah. The source footer yields space during that phase. Widget settings remain in the main app; the removed icon has no visible/tappable area.

All18 text fields use one common system font and size within each layout; Small/Normal/Large options remain. Compact sizes use9/10/11dp, expanded minimum sizes11/12/13dp, and roomy200×300dp-and-larger responsive layouts14/16/17dp. Every field scales together, avoiding independent per-field auto-size differences. The uniform fixed sizes are bounded for the tested layouts; the app's main-screen typography is unchanged. Wrapped headers accommodate longer Arabic dates without clipping. Next prayer and countdown share a row.

Approximate snapshots keep ≈. Calculation time and refresh guidance remain in the countdown's screen-reader description and app help, rather than a visible timestamp. Existing numeral matching/minute fallback, non-wakeup refresh, chime and notification policies remain.

## Final verification

- Signed API35 build succeeds; original signing certificate and package, increased version code. APK v2/v3 signature verification passes. Java17.0.20, ECJ3.40.0, platform35 revision2 and Build Tools35.0.0; five existing deprecation warnings.
- **OK(29 native Android/Skia/Robolectric tests),44.726 seconds.** Includes Arabic/English, both providers, supported minimum/resized layouts, all three text choices,12-hour and numeral choices, font-scale profiles, themes/transparency, phase/expiry/reapplication, numeral fallback/recovery and alarm isolation, API26/29 compatibility, activity/settings recreation and per-widget failure containment.
- Existing assertions were updated for explicitly requested removals: hidden hint/settings/duplicate time; direct widget-settings lifecycle remains exercised with its explicit app Intent. Bounds assertions and all unrelated behavior checks remain. No extra low-impact test was added.
- All24 XML widget variants checked to have one uniform text size. The roomy variant applies the same chosen size to every text ID. Final Arabic/English roomy, compact and large-text native images visually inspected. Current samples: tests/native/screenshots/v2.1.4/. Earlier samples remain historical.
- Initial uniform-font checks caught a wrapped Arabic date clipped in a fixed-height header; wrapping header geometry corrected it before the passing final run.
- Pure-Java checks pass:2,235 prayer and estimated-iqamah boundary/delivery cases,6,705 reminders,64 Friday exclusions,15 format/countdown and8 date/locale checks plus midnight/expiry/custom intervals.
- Timetable/chime bytes match v2.1.3 in source and APK. Scheduling, alert policy, preferences, estimated-iqamah calculation and timetable code are byte-identical. MainActivity edits are version/help wording only. No new permission, dependency, account, tracking or network access.
- Source ZIP includes all prior files, complete build files/script, tests/history, design/licenses and official Awqaf provenance. Private signing material, build products and downloaded SDK/test caches are excluded.

Public certificate SHA256:4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af.
Signed APK SHA256:b56a9da19f434c0c608642235a882bd946f9dd51b2ccb3bb0da5f4f2523061f9.
Current exact source/resource hashes:tests/native/app-source.sha256; full run:tests/native/results.txt. Prior hashes/output have versioned filenames.

Host native validation is not a physical-phone/OEM-launcher installation, real notification/sound-delivery or TalkBack traversal test. Launcher padding/grid can alter content area.110×110dp remains unsupported smoke-only. Existing Doze/minute-snapshot and delayed phase-update limits remain. Use the retained refresh control when stale. The earlier phone-specific crash cause remains unconfirmed.
