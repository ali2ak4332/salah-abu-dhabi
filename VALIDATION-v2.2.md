# Salah Abu Dhabi2.2 — two additional timetable widgets

17 October2026. Package ae.prayertimes.abudhabi; version2.2/code18. Latest source identity version17 and APK version16 resolved; starting release2.1.4.

## Delivered

- Horizontal timetable: separate4×1 picker entry with all five official daily adhan times side by side. Minimum resize content180×48dp; responsive250×64 and360×80dp text/layout profiles.
- Flexible timetable: separate2×2 picker entry, with minimum content80×80dp. Five rows remain at minimum; the date appears at120×120dp and above. Responsive200×200dp text profile improves larger layouts. Both new providers allow horizontal and vertical resizing. Actual cell counts/padding are controlled by the launcher, not guaranteed by dp metadata.
- Original two countdown widgets, their XML layouts/provider minima and their recent simplified design are preserved. The new entries are timetable-only: no countdown, iqamah target or added sound. Tap to open the app; preferences and official data are shared.
- Added both providers to in-app Add a widget menu, localized launcher names/descriptions, global refresh and widget-presence detection. Existing prayer/day/time-change scheduling now refreshes them through the same path. Each failed instance is isolated. No new notification policy, permission, service, account, tracking, network dependency or paid package.

## Validation

- Signed API35 build succeeds, with all pure-Java timing checks:2,235 prayer/estimated-iqamah boundary/delivery cases,6,705 reminders,64 Friday exclusions,15 formatting and8 date/locale cases, plus coverage/midnight/expiry/custom intervals.
- Full native Android/Skia/Robolectric suite: **OK(36 tests),52.19 seconds**. Existing29 cases pass plus7 new framework-expanded cases: two grouped scenarios on API26/29/35 and one API35 provider/global-refresh lifecycle scenario.
- New render matrix covers216 combinations: two languages, two new shapes, three declared sizes each, three text preferences and Western24-hour/Eastern12-hour formatting, across API26/29/35. Checks actual framework responsive selection, complete-container parcel/unparcel, native RemoteViews apply/measurement, text/ancestor bounds, RTL, all five exact saved times and click semantics. No bounds assertion relaxed.
- Additional checks cover absent/expired data, UAE midnight, both new providers' update/resize/refresh callbacks, presence detection and global updates. An existing fault-injection shadow now returns no IDs for unrelated providers, preserving its original compact/tall failure fixtures.
- Launcher preview layouts include placeholder names/dashes and a theme background. Actual rendering always populates saved official times or unavailable placeholders; no calculated prayer times.
- Arabic and English horizontal250×64, flexible80×80 and120×120 native PNGs visually inspected. Current evidence:tests/native/screenshots/v2.2/. Tiny80dp text is deliberately small; resize larger for readability. Minimum80dp is smaller than the existing widgets'140×160dp minimum.
- Both original widgets' XML layouts/provider metadata and all scheduling, alert policy, preferences, timetable/estimated-iqamah calculation classes are byte-identical to2.1.4. Timetable/chime bytes match in source and APK. Existing one-page app regression coverage passes.
- Public signing certificate SHA256 matches previous releases:4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af. APK v2/v3 verifies. Same package/higher code preserve update compatibility; actual phone installation was not performed.
- Java17.0.20, ECJ3.40.0, official platform35 revision2 / Build Tools35.0.0. Five pre-existing deprecation warnings; no compilation errors.

Signed APK SHA256:519fea643be480f9de2264ad16cb11b366d4c745636cc57e0a5e7ee68596eb66.
Exact current production files:tests/native/app-source.sha256; full output:tests/native/results.txt. Prior2.1.4 hashes/output are retained with versioned names. Complete source includes build files, tests, history, designs/licenses and Awqaf provenance; private signing material, build products and downloaded SDK/test caches are excluded.

## Device limits / use

Install over the existing app, then add **Horizontal** or **Flexible** from the launcher’s Salah Abu Dhabi widget picker or the app’s Add a widget menu. Existing placed widgets remain their original providers/minima; select the new Flexible entry for the smaller resize limit. Drag its launcher resize handles after adding.

Native tests are not an OEM launcher/grid, physical install, TalkBack, actual sound/notification delivery or Doze test. Android can defer refreshes; opening the app refreshes all providers. Existing countdown widgets retain their previously disclosed minute/phase-delay limitations. The new timetable widgets have no timer. Actual display density and launcher padding vary; phone verification remains necessary.
