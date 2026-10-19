# Salah Abu Dhabi 2.4 — two fresh widgets

17 October 2026. Package `ae.prayertimes.abudhabi`; version **2.4 / code 20**. Built from the verified v2.3 project and signed with the original app certificate.

## Result

Exactly two launcher widgets are registered:

- **Compact · 2 × 2:** five horizontal prayer rows.
- **Horizontal · 4 × 1:** five side-by-side prayer columns.

All five earlier provider registrations and old widget layouts are removed. The four obsolete subclasses are deleted; the shared base is rewritten without a launcher registration. Neither new type changes into the other when resized. Both always display the five exact saved adhan times, with one system-font family/size and a subtle highlight for today's next adhan. Sunrise is excluded. A short localized date appears only if it does not materially shrink the prayer text.

Countdowns, estimated iqamah and seconds preferences remain on the main screen. Neither widget has a timer, duplicate target time, settings/refresh button, or per-minute refresh. Tapping anywhere opens the app and refreshes both. The widgets use rounded XML backgrounds, preserving proper corners and avoiding bitmap payloads at every size. Saved theme, opacity, language, numeral, time-format and text-size preferences are retained.

Android 12+ uses the launcher's exact size list and explicit 2 × 2 / 4 × 1 target cells. Earlier hosts use correctly paired portrait/landscape dimensions and conservative minimum placement/resize hints of **110 × 110dp** / **250 × 40dp**. Actual cell sizes and padding depend on the launcher. These tight dimensions require small text; larger slots improve readability. Font fitting honours font-scale/text preferences only as far as the available area permits.

## Verification

- Full real Android View / native Skia / Robolectric run: **OK (36 tests), 67.735 seconds**. The unchanged main-screen cases remain active; current widget tests preserve the four earlier failure-isolation regressions.
- **3,360 / 3,360 widget profiles pass:** both fixed shapes, English/Arabic, Western/Eastern numerals, 12/24-hour format, five font-scale settings, all three text preferences, declared minimums, intermediate/boundary sizes and larger areas.
- Tight-size diagnostic checks: **10.75dp smallest fitted font** at Compact 110 × 110 and Horizontal 250 × 40; Pixel-like 130 × 117 and 276 × 51 profiles retain at least 11.75dp. All ten prayer-name/time TextViews are visible without ellipsis, overlap or missing string endings. Actual visible text edges account for single-line TextView scrolling.
- **192 density profiles** check actual physical-pixel bounds and fallback glyph heights at hdpi, xhdpi, xxhdpi and xxxhdpi. Native Arabic 12-hour rendering, including Eastern numerals and day-period markers, passes width/height checks.
- API **26 / 29 / 35** compatibility, portrait/landscape/portrait reapplication, fixed shape identity, fractional exact-size keys, parcel round trips, 16-size lists, null/empty/malformed/oversized options, repeated callbacks, missing-data recovery, midnight/expiry, theme/opacity, and per-instance rejected update/options recovery pass.
- Both **16-variant RemoteViews parcels use 0 bitmap bytes**. No widget minute alarm is created. Old request-705 pending intents are cancelled for the legacy base and current receivers. Old request-704 iqamah-only expiry alarms and their saved scheduling fields are cleared and never rearmed, including all five iqamah windows. Prayer/reminder alarm preservation and widget removal cases pass.
- Actual 4×-density PNGs for both shapes in Arabic/Eastern/12-hour and English/Western/24-hour were visually inspected. Rounded backgrounds, row/column alignment, all five times and readable glyphs are intact. The compact date may be omitted in Arabic when it would reduce the prayer font too much.
- Android API35 native build succeeds, with six existing/deprecated-API warnings and no errors. Pure-Java tests pass: 2,235 prayer boundaries and deliveries, 6,705 reminder boundaries, all estimated-iqamah transitions, 64 Friday Dhuhr exclusions, custom offsets, disabled options, midnight/expiry, 15 format/countdown and 8 localized-date checks.
- Offline Awqaf provenance passes: 447 saved days, 365 official 2027 dates, all 283 overlap dates exact, original 365-day CSV prefix byte-identical. Timetable and chime bytes also exactly match the signed v2.3 APK.
- Nine non-widget calculation/preference/receiver/drawing classes remain byte-identical to v2.3. `AlertScheduler` changes only remove the now-unused widget-iqamah wakeup scheduling block; its legacy cancellation and all prayer/reminder logic remain. `MainActivity` changes are confined to widget settings/help/picker, related explanatory text and version. Its visual layout/countdown/iqamah calculations are unchanged.
- Code review caught and corrected an unsupported old-Android RemoteViews gravity action and Arabic/Eastern 12-hour wrapping; the final tests verify actual width and visible edges rather than masking overflow. A review also removed unnecessary legacy iqamah-only widget wakeups.
- APK v2/v3 signature verification and 4-byte zip alignment pass. Certificate SHA-256: `4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`. The package and signing identity are unchanged, and version code increases from 19 to 20.
- Source ZIP excludes private signing keys/credentials, SDK/test caches, compiled build outputs and generated APKs. Full production source, assets, provenance, native/pure-Java tests, release evidence and build instructions are included. Historical removed-widget tests are clearly labelled and excluded from the current runner.

Signed APK SHA-256: `e8f28b69cbc2f132314a22bcc2a511a117e9c2aa3e92f4aee618d71e84bccc07`.

Exact native run output: `tests/native/results.txt`. Production file hashes: `tests/native/app-source.sha256`. Current preview PNGs: `tests/native/screenshots/v2.4/`.

## Install and limits

Install **Salah-Abu-Dhabi-v2.4.apk over the existing app**. Do not uninstall or clear data. Earlier home-screen widget components were deliberately removed and may disappear or leave an unavailable placeholder. Remove any leftover placeholder, then add **Compact** or **Horizontal** from the launcher widget picker (or the app's Add a widget menu).

No physical phone or OEM launcher was available. Device installation, exact cell counts, launcher picker caching, TalkBack focus grouping, real notification sound/timing, Doze/sleep/wake, reboot and battery behavior remain real-device checks. Host-side passing tests do not prove those outcomes or independently resolve a prior phone-specific crash without its log.

Android may delay date/highlight changes. Tap the widget to open/refresh if needed. Outside the saved timetable it shows dashes rather than fabricated times. The saved schedule ends **31 December 2027** and does not update itself.

## Platform references

- Android widget sizing, target cells and exact/legacy size options: https://developer.android.com/develop/ui/views/appwidgets/layouts
- Launcher widget behavior: https://developer.android.com/develop/ui/views/appwidgets/overview
- RemoteViews-supported operations: https://developer.android.com/reference/android/widget/RemoteViews
