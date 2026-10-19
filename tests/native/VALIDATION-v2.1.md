Historical v2.1 report. Its hash manifest is now `app-source-v2.1.sha256` and its output is `results-v2.1.txt`.

# Salah Abu Dhabi 2.1 native UI validation

Recorded 16 October 2026 (UTC). Package `ae.prayertimes.abudhabi`, version name **2.1**, version code **13**.

## Final result

**OK (23 tests)** in 42.684 seconds using Robolectric 4.16.1, JUnit 4.13.2 and native Android/Skia rendering. The portable `run-native.sh` ran the existing nine `NativeUiTest` cases plus fourteen `WidgetUiTest` cases. All production source/resource/asset hashes were checked before and after the final run and did not change. Concise output is in `results.txt`.

These are compiled native Android View and RemoteViews tests, including framework layout selection, actual action application, text measurement, native raster drawing and callbacks. They are not an OEM launcher or physical-phone run.

## Widget rendering and behavior

- **84 supported responsive profiles**: English/Arabic, both widget providers, Small/Normal/Large text preferences at 140×160, 160×160, 140×219, 140×220, 160×240, 200×300 and 300×300 dp (mdpi fixture). Both providers adapt to available height; the five-row layout starts at 220 dp. The exact 219/220 boundary is tested.
- **12 additional 110×110 shrink profiles** successfully inflate, apply and draw, but this is below the declared **140×160 dp** resize minimum. They are explicitly smoke-only; no content-bounds or supported-size claim is made for them.
- Every supported profile checks visible TextView geometry against host and ancestor bounds, nonzero text area, line widths, layout heights and ellipsis. The tolerance is 2 px for Android font-metric rounding. Every capture must contain nontrivial painted native pixels; inflation alone cannot pass.
- **96 opacity/theme profiles** cover 0%, 65%, 85%, 100%; Green/Navy/Sand; Light/Dark; both layouts and both languages. Bitmap center alpha/RGB are checked against preferences; root/text remain fully opaque. Active prayer names/times and row backgrounds retain the warm accent, including transparent mode.
- **24 additional minimum-size profiles** cover 12-hour time, Western/Eastern Arabic digits and 1.0/1.3/2.0 font scales. Numeric time fields remain LTR in Arabic layouts. Static values and minute snapshots honor app numeral preferences; the native live Chronometer uses the device's timer numerals, as disclosed in settings.
- **80 phase reapplications** cover every prayer immediately before adhan, at adhan, just before estimated iqamah and at that estimated target, for both languages/layouts. Phase label, next prayer, target clock time and chronometer base are checked. The five rows retain official adhan times. Current-row tint/accessibility state clears when the phase moves.
- Friday Dhuhr never invents an estimated iqamah. UAE midnight updates dates and rows. Before coverage, after expiry and missing-timetable fallback stop/hide the old live clock and clear stale phase state.
- The native Chronometer visibly changes for three seconds without new RemoteViews or app refresh. Switching seconds off stops it and shows an approximate HH:MM snapshot, calculation time and accessible spoken amount. Reapplication updates the snapshot and restores live seconds when enabled.
- Minute-mode scheduling is verified as one **inexact, non-wakeup RTC** alarm with no idle allowance and no repeating alarm. Repeated scheduling does not accumulate alarms, switching back to seconds cancels it, unavailable coverage schedules none, and the routine preserves prayer/reminder/iqamah alarms. This verifies scheduling choices, not actual battery cost or delivery precision.
- Actual refresh clicks send the explicit widget refresh broadcast. Settings clicks carry the direct-widget-settings extra. The two controls have localized accessibility labels, native clickable semantics and at least 40×40 dp hit areas. The root's generic content description is absent so it does not override prayer/time child descriptions.
- Cold, warm and repeated settings shortcuts are checked. The root menu and all six submenus survive activity recreation. Actual mode/color/opacity/text/seconds choices save correctly. Back and Cancel behave correctly. Saved-state process recreation restores an open submenu, consumes the original shortcut, and does not reopen a dismissed page when the original launch Intent remains flagged.
- Both providers are exercised through enable, update, options/size change, refresh, minute, delete and disable callbacks without crashes. **32 API26/29** profiles additionally parcel/unparcel, apply and reapply RemoteViews with native bounds assertions.

## Main-app regression retained

All nine pre-existing native app cases pass unchanged against the new build:

- 18 English/Arabic profiles at 320×480, 360×640, 360×800, 412×892, 412×915, 393×852 and 640×360 landscape; normal/1.3/2.0 fonts and Green/Navy/Sand plus Light/Dark configurations.
- Individually tagged official prayer names/times, main controls, footer, countdown units and active badge remain visible, without scroll containers, text ellipsis or overflow outside the 2 px tolerance.
- Native Figma-comparison renders, 12-hour time and Eastern numerals; four synthetic Android15 system-inset profiles; six narrow estimated-iqamah profiles.
- 40 live adhan/iqamah transitions, UAE midnight, calendar/today navigation, final saved day and 1 January 2028 expiry.
- Actual appearance settings, language switching and activity recreation; two-unit countdown mode; native 52×48 Switch checked/accessibility state and click behavior.
- Startup and rendering on Android8/API26 and Android10/API29 as well as full Android15/API35 coverage.

## Corrections caught by this widget validation

The first run caught invalid equal minimum/maximum auto-size settings in Small layouts, Arabic label height clipping, undersized controls and unsupported older-Android RemoteViews `setGravity` actions. Those were corrected before the successful final run. Review also identified widget-settings recreation and stale original-Intent replay cases; dedicated regression tests now cover both.

## Evidence and exact tested source

`screenshots/` retains refreshed main-app samples. `screenshots/widgets/` contains 19 curated native PNGs: minimum/expanded English and Arabic layouts, 219/220 boundary, several opacity/themes, adhan-to-iqamah state, hidden seconds, large-font Eastern numerals, API26/29, expiry and one clearly named unsupported-shrink diagnostic. Running the harness recreates the complete screenshot set in the isolated cache.

SHA-256:

- MainActivity.java: `398439b06d49e665043743c2954a133420e85aa590a6709dd4f049a5c1eb43c4`
- PrayerWidget.java: `659f1836efdfe6061196ae5fa52df5419a9f9980539104a5b3456c548954e9b3`
- Unsigned aligned APK: `632ed5924887ac4b774773160638d57d984fb8550e0a25028fc8384269f75083`
- Timetable CSV: `972f2e4b08c57822bdb82020e48de01e7ca689d1ba539dc62be3294df228790a`

`app-source.sha256` records every production source/resource/asset/manifest file. From the project root, run `sha256sum -c tests/native/app-source.sha256`. Signing changes the APK checksum without changing the tested production classes/resources.

Build environment: OpenJDK runtime21.0.12.1, Eclipse ECJ3.40.0, official Android API35 platform revision2, Android Build Tools35.0.0. Test dependencies are isolated and excluded from source delivery. No signing material was handled by the test harness.

## Remaining device checks and known constraints

- Install/open the signed APK on the actual phone; verify its launcher grid, resize handles, widget padding, refresh/settings interaction and actual TalkBack traversal.
- Verify notification/precise-alarm permissions, Test chime and real notification delivery separately.
- Exercise screen off, Doze and battery-saving behavior. Host tests do not measure physical battery cost, OEM batching, or background restrictions.
- The native live counter may reach zero or go negative while Android delays the next phase update. The absolute target remains visible; the widget footer and settings help direct the user to refresh. Minute mode is explicitly approximate and timestamped, and its refresh does not wake a sleeping phone.
- 110×110 dp is intentionally below the supported resize minimum. Transparent white text requires a readable wallpaper.

Robolectric emits harmless `Invalid ID 0x00000000` diagnostics; API26 also reports the ignored newer style key `0x0101056c`. Neither caused an exception or failed assertion in the final run.
