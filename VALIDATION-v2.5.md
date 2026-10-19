# Validation — 2.5 (version code 23)

Refined widget UI, built from the 2.4.2 source as received.

## Executed in this environment

- `aapt2` compiled and linked all resources (new layouts, styles, four pill drawables, updated backgrounds and strings) against the Android API jar.
- All production Java compiled with `javac -Xlint:all -source 8 -target 8` against the Android API jar and the generated `R`: no errors; only the pre-existing deprecation warnings.
- Pure-Java suites passed unchanged: `ScheduleTest` (2,235 prayer boundaries), `AlertPolicyTest` (2,235 deliveries; 6,705 reminder boundaries), `DisplayFormatTest` (23 checks), `PrayerPhaseTest` (2,235 adhan/iqamah boundaries, 64 Friday exclusions).
- `tests/native/src/.../TwoWidgetTest.java` was updated for the new design and syntax-checked (parser only).
- `design/previews/Salah-2.5-widget-preview.png` is an **HTML reproduction** of the new layouts with the real palettes and 18 Oct 2026 official times. It is not a native render; Android fonts and launcher padding differ.

## Not run here — please run before installing

- **No APK.** This environment cannot reach Google's SDK/Maven servers (no `d8`, `apksigner`, Robolectric), and the private signing key is correctly not in the source. Build with `build-local.sh` and the original key so it installs over 2.4.2 and keeps settings and placed widgets.
- **Robolectric native suite** (`tests/native/run-native.sh`): not run. Updated assertions to check:
  - Times share the medium face; names the regular face. Compact names and times share one fitted size; Horizontal names are exactly 0.78× the time size.
  - Exactly the lit prayer has a background (rounded pill); earlier prayers have reduced text alpha. Lit = called prayer during its iqamah window, else next adhan today.
  - Compact timer: live `Chronometer` (`widget_countdown`, count-down, base = target) when native digits match the numeral setting, otherwise a snapshot `TextView` (`widget_iqamah_minutes`) with rounded-up H:MM and the saved numerals. Never both. Horizontal has neither.
  - Display alarms: one-shot `RTC`, never allow-while-idle; at iqamah end for either shape; per-minute only in snapshot mode with Compact present.
  Expect to adjust tolerances if Robolectric's `Chronometer` base or font metrics differ slightly.
- Real phone: launcher corner rendering, live Chronometer ticking across sleep/wake, Arabic header order, and TalkBack labels.

## Behaviour notes

- During iqamah the lit row keeps the **official adhan time**; the header carries the iqamah countdown. (The approval mockup showed the iqamah time in the row; switching is a one-line change in `PrayerWidget.render`.)
- A live Chronometer can only run past zero if Android delays the refresh at the boundary; adhan refreshes use the existing exact alert alarm, iqamah-end refreshes a non-wakeup alarm that fires as soon as the screen wakes.
