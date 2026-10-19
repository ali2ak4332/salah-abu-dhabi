# 2.6 · 19 October 2026

- Added a separate Shurooq notification at the official saved Abu Dhabi sunrise time, with English/Arabic on/off control and an independent sound channel.
- Default enabled, subject to existing Android notification/channel permission. No automatic system permission prompt or DND override.
- Independent alarm, daily rollover, lifecycle restoration, actual-timetable validation and duplicate/stale suppression. No sixth-prayer, reminder or iqamah behavior change.
- Retained all 2.5.1 widget corrections. Same app ID/signing identity; version code 25.

# 2.5.1 — 19 October 2026: widget regression fixes

- Reproduced v2.5’s nine native failures before edits. Corrected real name-size and Compact header-visibility issues; updated obsolete alarm expectations and removed live seconds from static geometry equality checks while retaining explicit timer target/format tests.
- Horizontal names retain the 0.78 visual scale with a 9dp floor. Short strips reclaim vertical pill padding rather than shrinking prayer times below the reading floor.
- Compact reserves a minimum readable header independently of timer phase; measures every localized target label and AM/PM time. Prayer fonts remain stable across timer phase changes.
- Live next-adhan/iqamah seconds remain when native and saved digits match exactly. Other numeral choices use localized H:MM snapshots. In the final minute both modes show the absolute target clock time.
- A one-shot non-wakeup guard requests the final-minute fallback, followed by a boundary refresh. No allow-while-idle quota, service, permission or sound added. If Android delays the guard itself, an old live timer can still go negative; the documentation and regression test explicitly record this limit.
- Corrected a display calculation that could skip an adhan within the last millisecond; notification scheduling is untouched.
- English/Arabic help and accessible target labels now describe v2.5.1 accurately.
- Version code 24, same package/signing certificate; timetable, chime, settings and widget provider identities unchanged.

# 2.5 — 18 October 2026: refined widgets

- Requested widget refinement, matching the approved mockup. Same two launcher entries (Compact 2 × 2, Horizontal 4 × 1), same official data, alerts, chime and preferences.
- **Next prayer pill:** the lit prayer now sits in a rounded pill instead of an edge-to-edge tint. Earlier prayers today fade back (~60% contrast) so the day reads at a glance.
- **Iqamah state:** during an estimated-iqamah window the called prayer stays lit in amber (both shapes), matching the main screen's Iqamah badge. Its row keeps the official adhan time.
- **Live countdown (Compact):** the header now reads e.g. "Asr in 2:21:05" / "Iqamah in 5:43", localized, beside the date. It uses Android's live Chronometer when the device's native digits match the saved numeral choice; otherwise an H:MM minute snapshot refreshed once a minute while Compact exists. Replaces 2.4.1's bare minute digits.
- **Typography:** times stay medium weight; names and the date use the regular face. Horizontal shows smaller names above larger times. One fitted size still governs all five rows; the header still only uses leftover room.
- **Shape:** 22dp corners and a 0.5dp edge on all six backgrounds; compact rows gain 6dp inner padding for the pill.
- **Refresh policy:** adhan and midnight refreshes are unchanged (existing alert alarm). Display-only alarms remain one-shot, non-wakeup and never allow-while-idle: at the end of an iqamah window (either shape), and per-minute only in snapshot mode.
- Version code 23, same package and signing identity. See VALIDATION-v2.5.md — the Robolectric suite and APK build were not run in this environment.

# 2.4.2 —18 October 2026

- Fixed duplicate widget updates: onUpdate now skips callback targets during its subsequent all-instance refresh, while still refreshing other installed Compact/Horizontal instances before scheduling display work.
- Preserved the shared timestamp, newly bound callback IDs, per-instance failure isolation, later recovery, and all-instance timer refresh. Added exact update-count regression cases on API26/29/35; all six fail against unchanged2.4.1.
- Version code22 with the original signing/package identity. Latest two-widget design, digits-only estimated-iqamah minutes, fonts, official data, preferences, chime and notification/iqamah alarm policies preserved. See VALIDATION-v2.4.2.md.

# 2.4.1 — Compact estimated-iqamah minutes

- Add positive whole-minute digits at Compact’s top left, with the date at right. No ~ sign or visible label, no pre-adhan widget countdown.
- Follow configured estimated-iqamah intervals, enablement, Friday exclusion and saved numeral preference. Hide on the first render at/after expiry.
- Preserve all five prayer font sizes and exactly two launcher types. Horizontal, timetable, main-screen design, notification scheduling and chime are unchanged.
- Schedule only non-wakeup display refreshes while Compact’s iqamah phase is active; Android may defer starting/updating/clearing while asleep or restricted. Stale positive snapshots remain possible, but a timer never runs negative.
- Update-compatible package/signing identity; version code 21.

# Version 2.4 — 17 October 2026

- Replaced all five earlier widget providers with exactly Compact (2 × 2) and Horizontal (4 × 1), rebuilt as five rows/five columns. Earlier placed widgets must be re-added after updating.
- Prioritized the five exact saved adhan times: uniform fitted system typography, RTL, numeral/time-format choices, subtle next-adhan highlight, optional compact date, no timer/control clutter.
- Removed background bitmap allocation, recurring widget minute refresh work and obsolete silent iqamah-widget wakeups; rounded XML backgrounds keep their shape at all aspect ratios. Per-instance exception isolation, exact-size/legacy orientation handling and old-alarm cleanup remain covered.
- Conservative legacy placement hints 110 × 110dp / 250 × 40dp; Android 12+ target-cell hints 2 × 2 / 4 × 1. Launcher grids determine exact cells. Tiny slots use smaller text.
- Main-screen design/countdowns, Awqaf data, alerts/chime, preferences and crash protections retained. Version code20, original signing identity.
- See VALIDATION-v2.4.md for evidence and real-device limits.

# Version 2.3 — 17 October 2026

- Added one Adaptive widget entry: exact-size font fitting, five rows or five columns, optional date/countdown as space permits, shared language/numeral/display preferences, practical 140 × 96 dp minimum.
- Correct portrait/landscape width-height pairing, Android 12+ exact-size lists and defensive malformed-size fallback.
- Kept all earlier providers and installed widget instances compatible. Shared refresh, presence detection, phase handling and single-instance failure isolation.
- Retained official timetable, notification and estimated-iqamah policies, saved settings and platform fonts. No new permissions or services.
- See VALIDATION-v2.3.md for actual coverage and limits.

# 2.2 —17 October2026

- Added separate Horizontal(4×1) and Flexible(2×2) timetable widgets. Horizontal shows five prayers side by side; Flexible shows all five rows with an80×80dp resize minimum, addressing the original widget’s160dp minimum-height constraint.
- Both adapt text/date presentation to available space and reuse saved official times, numerals/time format, themes/opacity and text preferences. They are timetable-only; original countdown widgets and their layouts remain unchanged.
- Integrated both providers into global/time/day/boundary refresh and widget-presence detection; retained per-instance refresh exception isolation. Added both to the in-app picker and localized launcher labels/descriptions.
- Version code18, same package/signing identity. No new permissions, services, accounts or dependencies. Existing official data, preferences, chime and notification/estimated-iqamah policies unchanged. See VALIDATION-v2.2.md.

# 2.1.4 — 17 October 2026

- Applied the user's marked widget removals: hidden settings icon, Next Adhan label, duplicate target time and visible timestamp/hint. Kept refresh, date, next prayer/countdown, all five rows, and an explicit estimated-iqamah label when active.
- Unified every text field's font and size within each widget; manual text-size options remain. A larger200×300dp responsive layout scales all text together for readability. Header wraps rather than clipping long Arabic dates.
- Kept the approximate ≈ mark and accessible calculation time/refresh guidance. Widget settings remain available in the app. Notification/timetable/chime/preferences are unchanged; version code17 with original signing identity. See VALIDATION-v2.1.4.md.

# 2.1.3 — 17 October 2026

- Optimized widget header space: date and phase label share one column alongside the existing refresh/settings controls. Compact prayer lists gain10dp at the supported minimum; larger widgets devote more space to prayer rows.
- Increased prayer-name/time text limits by2sp with automatic sizing retained. Kept the full-height Arabic2×3, compact2×2, all five rows, numeral consistency, countdown/timestamp and40dp control targets.
- Version code16, same package/signing identity. Timetable, chime, countdown/numeral behavior, preferences and notification/iqamah policy unchanged. See VALIDATION-v2.1.3.md.

# 2.1.2 — 17 October 2026

- Fixed missing prayer rows in smaller widgets: both providers now show all five prayers at every supported size, including 2×2, with a denser layout below 220dp. Full-height Arabic 2×3 remains unchanged.
- Fixed mixed countdown numerals. Countdown uses the saved numeral choice; when Android's native timer differs, a clearly approximate localized HH:MM snapshot and existing non-wakeup minute refresh are used. Matching numerals restore live seconds automatically. Saved preferences and main-screen seconds are retained.
- Added cross-system-language/numeral/size and refresh-recovery regression coverage. Retained per-widget failure containment from 2.1.1.
- Version code 15, same package/signing certificate. Official adhan data, chime, prayer/iqamah policies and one-page home layout preserved. See VALIDATION-v2.1.2.md.

# 2.1.1 — 17 October 2026

- Isolated widget refresh failures per instance. A rejected widget update/options lookup can no longer interrupt app startup, remaining widgets, or the provider callback's scheduling work. Later refreshes retry the instance normally.
- Added four native fault-injection regression cases covering startup/resume in English and Arabic, multiple widgets, options rejection, recovery and callback scheduling. All four failed on unchanged v2.1 before the fix.
- Version code 14; same package and signing certificate. Main-screen/widget layouts, official timetable, chime, preference keys and prayer/estimated-iqamah policies unchanged. See VALIDATION-v2.1.1.md for checks and device limits.

# 2.1 — 16 October 2026

- Responsive, themed 2×2/2×3 widgets with persistent warm active-prayer styling, live pre-adhan and estimated-iqamah countdowns, and separate refresh/settings controls.
- Enlarged 2×2 widgets can show all five adhan rows; minimum supported content area140×160dp and expanded cutoff220dp.
- Hide-seconds preference now uses an approximate localized HH:MM snapshot and a non-wakeup minute refresh, with a visible calculation timestamp.
- Honest refresh-at-zero guidance for delayed Android phase transitions; no per-second app wakeups, services or additional notification alarms.
- Fixed low-opacity accents, misleading tap description, and unsupported older-Android RemoteViews gravity calls.
- Direct widget settings preserve root/submenus across recreation and consume launch shortcuts once.
- Version code13, same package/signing certificate. Official447-day timetable, sound, notification policies, Friday logic and approved main-screen layout preserved.

# Change log

## 2.0 — 16 October 2026

- Applied the approved Figma home-screen design in native Android Views: warm canvas, large next-adhan card, centered gold countdown with aligned units, single five-prayer schedule card, sunrise footer, date navigation and persistent prayer-alert switch.
- Estimated iqamah uses a clearly marked gold-tinted card and an adjustable-estimate reminder. Official adhan rows remain unchanged during iqamah.
- Bundled exact Figma icon exports as local density-aware assets. Android retains its live status/navigation bars. Keeps the safer platform fonts from 1.9.1; no runtime bundled-font loading.
- Extended official Awqaf data by 82 days through 31 December 2027, for 447 consecutive saved dates from 11 October 2026. Original 365 rows are byte-identical; all 283 overlapping 2027 rows match the newly retrieved official source. No projected prayer times.
- Retained the language, numeral, 12/24-hour, theme, widget, advance reminder and iqamah preferences, chime audio and timing logic. Same Android package, version code 12.
- Source includes raw official 2027 responses, provenance and reproducible validation. Private signing keys/passwords are excluded. See VALIDATION-v2.0.md for the final test results and device-testing limits.

## 1.9.1 — 16 October 2026

- Recovery hotfix following the reported v1.9 crash: restored system fonts for all app/widget text and removed every runtime bundled-font load and font-variation setting. The colours, icons, spacing and hero-card design remain.
- Noto font design files are outside Android resources and absent from the APK. Copyright/license and derivation remain with source. Packaging is configured to keep future TTF resources uncompressed.
- Same package/signing identity, version code 11. Existing timetable, chime, preference keys and prayer/iqamah calculations are unchanged.
- Validation: Android native layout/startup checks and pure-Java timing tests passed; see VALIDATION-v1.9.1.md. No exact crash log was obtained from the reported phone, so the root cause remains unconfirmed.

## 1.9 — 16 October 2026

- Visual polish: coordinated warm light/dark palettes with Green/Navy/Sand preferences retained; quiet borders, clearer type hierarchy and restrained active gold across app/widgets.
- Added original outline calendar, settings, date-navigation/today and bell icons, localized descriptions/tooltips, theme-matched switch track, and full localized date accessibility text.
- Added a faint geometric corner detail and subtle shade to the next-prayer card. Preserved the single-page layout, five horizontal rows, full-height Arabic 2×3 and compact 2×2.
- Bundled the open-source Noto Sans Arabic font with its full SIL OFL 1.1 notice in source/APK, and aligned Arabic row times to the outer edge. Version is visible in About rather than the home subtitle.
- Prayer scheduling/iqamah logic, official Awqaf timetable, chime audio and existing preference keys are unchanged. Signing material remains separate.
- Version code 10. API 35 build, existing prayer/reminder/date/iqamah tests, signature compatibility and unchanged timetable/chime checks passed. Native Android/Skia layout evidence is recorded in VALIDATION-v1.9.md. No physical-phone test was available.

## 1.8 — 16 October 2026

- Added the current UAE weekday and date at the top of both widget sizes, reusing the existing meta row across all 18 layout variants. The header follows the app’s language and numeral preferences and stays visible during estimated iqamah.
- Compact widgets now retain an explicit Tomorrow heading when the next adhan falls on the next date, distinct from today’s date header.
- Kept the home layout, full-height Arabic prayer rows, timetable, chime, preferences and iqamah/alert timing logic unchanged.
- Version code 9, signed with the same certificate as v1.7; signing keys and passwords remain outside the source ZIP.
- Validation: API 35 build and signature verification, all existing timing tests, eight focused date/locale/time-zone checks, structural checks across 18 widget variants, and byte comparison of timetable/chime passed. No physical-device or rendered launcher test was performed in this release.

## Source packaging — 15 October 2026

- Complete v1.7 source export; app behavior and version unchanged.
- Validation: application and test files match the saved v1.7 archive byte for byte; build script passes Bash syntax validation. A fresh APK rebuild was unavailable in this session because Android SDK Build Tools are absent.
- Moved the original signing key and credentials into a separate private bundle. The local build accepts external signing credentials or produces an unsigned APK without creating a new key.

## 1.7 — 15 October 2026

- Notification category names and descriptions now follow the app’s Arabic or English language. Switching languages refreshes both categories immediately.
- Retained the existing channel IDs and sound configuration so Android keeps the user’s notification preferences.
- Version code increased from 7 to 8; signed with the same certificate as the saved v1.6 APK.
- Validation: API 35 release build and APK signature verification passed. Existing schedule, alert/reminder, formatting and estimated-iqamah tests passed. Inspected compiled Arabic/English label branches and the language-change refresh order. Compared against the saved v1.6 source/APK: timetable, chime, widget code/resources and timing logic unchanged.
- Android’s documented channel update behavior permits name/description changes without changing established notification behavior: https://developer.android.com/develop/ui/compose/notifications/channels
- No physical-device notification-settings UI test was performed for this release.

## 1.6 — Previous saved release

- Arabic 2×3 widget fills the available height with larger prayer names and times and five evenly stretched prayer rows. Existing English and compact widget designs retained.
