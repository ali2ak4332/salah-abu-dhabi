# Salah Abu Dhabi 2.1 — widget release validation

16 October 2026. Package `ae.prayertimes.abudhabi`; version code **13**; Android minimum API 26 / target API 35.

## Result

The signed update and complete source are built and validated. The approved v2.0 main-screen design and official 447-day Awqaf timetable are preserved. Native widget views now support responsive content, live pre-adhan/estimated-iqamah countdowns, persistent active accents and separate refresh/settings controls.

**No physical phone or launcher was available.** Host-side Android rendering and lifecycle tests passed; installation, real launcher behavior, TalkBack focus order, notification delivery/sound and OEM battery/Doze behavior remain untested.

## Passed on this exact build

- API 35 resource compilation, Java compilation and DEX generation using Android Build Tools 35.0.0, ECJ 3.40.0 and Java 21.
- **23 grouped native Android/Skia tests:** nine existing main-app groups and 14 widget groups under Robolectric 4.16.1.
- **60 supported responsive widget profiles:** both providers, English/Arabic, three text sizes, at 140×160, 160×160, 160×240, 200×300 and 300×300 dp. Visible bounds and text layouts were checked, not just inflation. The 219/220 dp expansion boundary was also tested. Twelve 110×110 cases are labelled unsupported-shrink smoke tests, not readable-layout passes.
- **24 additional minimum-size profiles:** 12-hour times, selected numeral preferences and font scale 1.0/1.3/2.0. Widget metadata declares a supported 140×160 dp minimum; both providers expose the full list from 220 dp height.
- **96 theme/opacity combinations:** English/Arabic, compact/tall, Green/Navy/Sand, light/dark, opacity 0/65/85/100. Active text/row accents remain distinct, including transparent mode.
- **80 native adhan/iqamah boundary reapplications**, including reusing the same RemoteViews hierarchy, alongside Friday Dhuhr exclusion, midnight, missing/out-of-range timetable and timer cleanup at expiry.
- Native Chronometer visibly ticks without a new app/widget update. Hidden seconds stop and hide that clock, display an approximate localized HH:MM amount and calculation time, and request exactly one inexact **RTC non-wakeup** minute refresh. Tests verify it does not replace prayer/reminder/iqamah alarms and cancels when unnecessary.
- Distinct immutable tap actions: body opens timetable, refresh broadcasts without opening an activity, settings opens the widget page. Localized labels and 40 dp control targets checked. Generic root description removed so it does not hide live child values; the spoken minute snapshot includes its actual amount.
- Actual provider enable/update/options-change/resize/refresh/minute/delete/disable callbacks, Android API 26/29 parcel/apply/reapply, and API 35 responsive layout selection.
- Direct settings cold/warm/repeated shortcuts; root and all six widget submenus survive activity recreation; actual preference selections, Back/Cancel, saved open-page restoration and dismissed-page process restart. Consumed launch commands do not reopen old dialogs.
- All existing main-screen checks: native startup in Arabic/English on API 26/29/35, responsive layouts and font scales, settings/calendar, source/date expiry, switch accessibility, system insets and 40 live main-screen phase transitions. **English and Arabic 412×892 main-screen renders are pixel-identical to the delivered v2.0 previews.**
- Pure-Java suites: all 2,235 prayer boundaries/deliveries, 6,705 reminder boundaries, 2,235 adhan/estimated-iqamah boundaries, 64 Friday exclusions, custom/disabled estimates, UAE midnight, expiry, time/numeral/countdown formatting.
- Official coverage remains **2026-10-11 through 2027-12-31, 447 consecutive days**. CSV is byte-identical to v2.0; the original 365 rows remain unchanged; all 283 overlapping 2027 rows match archived official responses. Chime bytes and notification/schedule policy source files are unchanged.
- Signed APK verifies under the same certificate as the previous builds. No new Android permissions, runtime bundled fonts, credentials, signing files, internet access or repository publication.

## Countdown accuracy and battery limitations

Seconds are rendered by Android's launcher-hosted Chronometer, avoiding per-second app wakeups. Its numeral style follows the phone's timer locale. Static times and hidden-seconds snapshots follow the app's selected numerals.

Adhan/iqamah transitions still depend on Android delivering the existing boundary update. A delayed update can leave the previous phase and a negative native timer visible. The absolute target remains visible and the widget says to refresh at zero; the separate refresh control recomputes the phase. Help explicitly explains this limitation. This build does **not** promise exact launcher transitions under all battery modes.

With seconds hidden, the display is intentionally an **approximate HH:MM snapshot**, rounded up to a minute, showing its calculation time. Its minute alarm does not wake a sleeping phone and can be batched. It must not be presented as an always-live minute counter. Use refresh if its timestamp is old.

Estimated iqamah remains an editable estimate, not official mosque-specific iqamah. Friday Dhuhr is excluded. No additional sound is introduced.

## Evidence and reproduction

- `build-local.sh`: production build and pure-Java tests.
- `tests/native/`: portable pinned test harness, full source, concise verified results, source checksums and curated native screenshots.
- `tests/native/make-widget-previews.py`: arranges the native screenshots into the delivered preview sheets; it does not redraw the widget UI. The sheets are host-side renders, not photos/screenshots of a physical phone launcher.
- `provenance/awqaf-2027/validate_timetable.py`: offline official-data validation; raw responses and provenance are retained.
- `README.md`: build, update and widget instructions.

Public signing-certificate SHA-256: `4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`.

APK SHA-256: `a6b8549e22a0d8ef9b4593e7f105ff210aae838e81f632167d602f3c1ca7881e`.

## Small physical-device check still needed

1. Install over the existing app without uninstalling or clearing data; confirm saved settings remain.
2. Add both widgets, resize them, and check Arabic/English and the refresh/settings controls on your launcher.
3. Try transparent/light/dark backgrounds and hide seconds, then lock/unlock and return after a phase boundary. Refresh any stale display.
4. Check TalkBack focus and, if alerts are used, notification permissions, Test chime, reboot recovery and battery restrictions.

The earlier phone-specific app crash remains unconfirmed; this release preserves the platform-font recovery approach. Passing host-side startup tests cannot prove that particular device issue resolved.
