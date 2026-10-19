# Salah Abu Dhabi 2.3 — one adaptive widget

17 October 2026. Package `ae.prayertimes.abudhabi`; version 2.3 / code 19. Based on the verified latest v2.2 source (Library version 18) and APK (Library version 17). The original development task was completed and remained unchanged when checked before packaging.

## Delivered behavior

- One **Adaptive · resize freely** entry. All five official prayers stay visible; the widget changes between rows and columns to suit its shape and fits one common system-font size to the actual localized strings.
- Practical minimum content area **140 × 96 dp**, initial target 2 × 2 cells. Launcher grid and outer padding control actual cell counts and resize choices. This is not an arbitrary-any-size promise. Below-minimum 80 dp profiles are defensive rendering tests only, not an advertised readability level.
- Date appears when space permits (row layout ≥112 dp high; wide layout ≥80 dp high). The countdown and separate 40 dp refresh control appear at ≥160 dp high in rows, or ≥128 dp in columns. Small views prioritize the five times; tapping the body opens the app and refreshes.
- Pre-adhan and explicitly labelled estimated-iqamah phases share the existing calculation policy. Tomorrow's target is visibly marked without replacing today's UAE date. Live seconds remain a native launcher Chronometer only when its actual numeral repertoire matches the saved choice; otherwise an approximate minute snapshot uses the selected numerals and exposes its calculation time to accessibility.
- Arabic/English, Western/Eastern numerals, 12/24-hour clocks, themes, opacity and text preferences are shared. Phone font scale increases the requested text size, bounded by the space available. Text fitting measures Android's fallback glyph metrics, including Arabic and Eastern digits, rather than assuming Latin line heights. Supported sizes retain a 10 dp font floor.
- Android 12+ consumes exact launcher sizes, including rotation/fold states; older or incomplete hosts receive separate portrait/landscape layouts using the correct width/height pairs. Exact-size views share background bitmaps, keeping the maximum 16-state payload at 576,000 bitmap bytes.
- All four earlier widget providers, layouts and minima remain available for already-placed-widget compatibility. The in-app picker recommends Adaptive first. Existing widgets are not silently removed or replaced.

## Verification

- Full native Android/Skia/Robolectric run: **OK (56 tests), 130.745 seconds**, with no failures. All 36 prior app/widget cases pass plus 20 adaptive cases, including API-specific variants.
- Adaptive matrix: **2,688 / 2,688 native profiles pass**. It crosses English/Arabic, both numeral choices, 12/24-hour format, font scales 0.85/1/1.5/2, Small/Normal/Large preferences, 28 dimensions including supported minimums and boundary/intermediate sizes. Every profile checks five exact saved times, uniform system font/size, RTL and actual text/container bounds without ellipsis.
- Additional native checks pass for hdpi/xhdpi/xxxhdpi physical-pixel bounds in both languages (24 profiles); exact fractional size keys, full RemoteViews parcel/unparcel and framework selection; API 26/29/35 portrait-landscape-portrait reapplication; malformed/empty/oversized exact-size-list fallback; 16-state bitmap memory; live tick/numeral fallback; adhan/estimated-iqamah/Friday/midnight/expiry phases; missing/recovered data; preferences/themes; repeated callback and per-instance fault recovery.
- Full terminal output is in `tests/native/results.txt`; the exact production source hash list is `tests/native/app-source.sha256`.

- Signed Android API 35 build succeeds. Pure-Java regressions pass: 2,235 prayer boundaries/deliveries, 6,705 reminder boundaries, all estimated-iqamah transitions, 64 Friday Dhuhr exclusions, midnight/expiry/offset settings, 15 time/digit/countdown and 8 localized-date checks.
- Offline timetable provenance passes: 447 saved days; 365 official 2027 rows; all 283 overlap dates exactly match; the original 365 rows remain byte-identical. Timetable and chime bytes match v2.2 and the signed APK.
- Scheduling, alert policy, preferences, schedule/phase/format calculations, alarm receiver, prior Flexible/Horizontal/Tall classes and all existing widget XML layouts/provider metadata are byte-identical to v2.2. Shared base-provider changes only register Adaptive for presence detection/global refresh. MainActivity changes are picker/help/version text.
- Code review found and fixed duplicate per-size background allocation before release. Arabic/Eastern fallback-height clipping was reproduced by native tests and corrected without disabling fallback line spacing or relaxing bounds checks. One older numeral test was made portable by checking the real native formatter instead of assuming a locale name implies a digit shape; its independent actual-rendered-numeral assertions remain.
- Native Arabic/English minimum, compact, tall, wide and large PNGs inspected. No visible clipping in the inspected final previews. Evidence and reproducible test source are included under `tests/native/`.
- API 35 platform, Build Tools 35.0.0, ECJ 3.40.0, Robolectric 4.16.1. Six deprecation warnings, no build errors.
- APK v2/v3 signature verification and zip alignment pass. Original signing certificate SHA-256: `4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`. Same package and higher code preserve update compatibility. No private signing material, credentials, SDK/test caches or build products are in the source ZIP.

Signed APK SHA-256: `9d1ddddba369cd3a3f220baa07e9dac6bed17d020d4a05b350fd016286be7f6b`.

## Install and limitations

Install **Salah-Abu-Dhabi-v2.3.apk over the existing app**. Do not uninstall or clear data. Long-press the home screen → Widgets → Salah Abu Dhabi → **Adaptive**, add it, then drag the launcher resize handles. Existing placed widgets keep their earlier provider behavior; after adding Adaptive, remove older home-screen instances yourself if you want only one.

No physical phone or OEM launcher was available. Installation, exact cell fit, TalkBack grouping, sleep/wake/Doze, actual notification sound/delivery and battery behavior remain real-device checks. Native tests do not establish those outcomes.

Android can delay refreshes. A native countdown may reach zero or go negative before a delayed phase refresh; use the separate refresh control when that occurs. Approximate minute snapshots can be stale if the phone sleeps or Android batches the non-wakeup refresh. No unrelated Doze/alarm-policy patch is included and no guarantee of exact background transitions is made.

## Platform references

- Android flexible widget layouts and exact size lists: https://developer.android.com/develop/ui/views/appwidgets/layouts
- Launcher sizing and resize strategy: https://developer.android.com/develop/ui/views/appwidgets/overview
- Fallback-aware line metrics: https://developer.android.com/reference/android/text/StaticLayout.Builder#setUseLineSpacingFromFallbacks(boolean)
