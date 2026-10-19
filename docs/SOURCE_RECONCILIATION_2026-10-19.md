# Android source reconciliation — 19 October 2026

The repository is reconciled to the current saved **Salah Abu Dhabi 2.6 / code 25** source, rather than stopping at the previously discussed 2.5. This is a source recovery and validation, not a new signed APK release.

## Provenance

- Original main: [`04032e2dea5d4385a250886e28811dd4cf4db4bc`](https://github.com/ali2ak4332/salah-abu-dhabi/commit/04032e2dea5d4385a250886e28811dd4cf4db4bc).
- Recovered source: `Salah-Abu-Dhabi-v2.6-Source.zip`, SHA-256 `c2c9ac5a0f2a5cd7740087e19823b7c8f50dfa41b114b684ab682b160b65c242`. The saved 2.5 source item had subsequently been updated to 2.6; its current contents were inspected.
- Existing saved APK: `Salah-Abu-Dhabi-v2.6.apk`, SHA-256 `29163ab735609abf9fbd907af0bdb1933f677ec339856d983937d9fc7e14948e`. Independently checked metadata: `ae.prayertimes.abudhabi`, name `2.6`, code `25`; valid v2/v3 signatures. Both bundled asset files exactly match recovered source.
- Historical validation files are retained as historical evidence. The checks below were run again for this reconciliation.

## Confirmed differences

| Area | Original GitHub main | Recovered source |
|---|---|---|
| Version | 1.6 / code 7 | 2.6 / code 25, in Gradle and manifest |
| Widgets | Original Compact/Tall providers and layouts | Current Compact 2×2 and Horizontal 4×1 providers; resize-aware fitting, five visible prayer times, localized date, rounded highlights, amber iqamah state |
| Numerals | Native Chronometer could differ from saved numeral choice | Exact native numeral comparison; live timer only when digits match, otherwise localized H:MM snapshots |
| Iqamah calculation | Already restricted to adhan ≤ now < iqamah; Friday Dhuhr excluded | Retains those bounds, fixes the display's sub-millisecond next-adhan skip, adds widget final-minute fallback and display refresh handling |
| Timetable | 365 saved days, through 10 October 2027 | 447 days, through 31 December 2027; original 365 rows unchanged; 283 overlapping 2027 dates match archived official responses |
| Sunrise | No dedicated Sunrise notification policy | Separate Shurooq setting, notification channel, alarm identity and replay/stale-delivery protection using official saved sunrise values |
| Presentation | Earlier native presentation | Recovered platform-font main screen, Figma-exported icons and associated licensing/provenance |
| Evidence/build | Earlier standalone build and pure tests | Current standalone build, five pure suites, native API26/29/35 harness, timetable provenance and versioned evidence |

Source paths supporting the comparison: `app/build.gradle`, `app/src/main/AndroidManifest.xml`, `PrayerPhase.java`, `PrayerWidget.java`, `PrayerWidgetCompact.java`, `PrayerWidgetStrip.java`, `DisplayFormat.java`, `AlertPolicy.java`, `AlertScheduler.java` and `CHANGELOG.md`. Java files are under `app/src/main/java/ae/prayertimes/abudhabi/`.

## Identity, data and signing

Application ID, minimum API26, target API35, existing `MainActivity` SharedPreferences namespace and existing prayer/reminder identities are retained. The update adds independent sunrise records; it does not clear saved settings. The chime is unchanged. No internet permission, account or cloud storage is added.

The source package intentionally replaces obsolete widget components. A widget placed from the old 1.6 provider may need to be removed and re-added using Compact or Horizontal. This does not require uninstalling the app or clearing its data.

Verified saved-APK signing certificate SHA-256:

`4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`

The original private key was not present in the recovered source. No replacement key was generated and no signing secrets, APKs, SDKs or dependency caches are committed. Future installable updates must be signed externally with the original key. The repository's CI debug APK is not an update-compatible substitute for the personally signed installation.

## Checks run now

- API35 standalone source/resource/DEX build: passed, producing an unsigned aligned APK. Compiler source8/deprecation warnings remain.
- Additional Gradle8.9/AGP8.7.3 unsigned `assembleRelease` + `lintRelease`: passed with 0 errors and 74 warnings. These include advisory resource/version, accessibility, locale, synchronous-preference and deprecation concerns; they are retained in the current lint report.
- Five pure-Java suites: passed, including 2,235 prayer boundaries/deliveries, 6,705 reminder boundaries, 2,235 iqamah boundaries, 64 Friday exclusions, all 447 sunrise dates and localized formatting checks.
- Full Robolectric native suite: **88/88 passed**, API26/29/35; 151.604 seconds; all 3,360 widget matrix profiles passed. Native rendering emits resource diagnostics but no test failure. Includes widget sizing, numerals, lifecycle, final-minute fallback and sunrise policy/UI.
- Offline timetable provenance: passed, with exact original prefix and official overlap preservation.
- Existing saved APK: signature, package/version and asset checks passed. A newly signed APK was not produced.
- Private-material filename/content checks: no private-key or high-confidence API-token matches in the staged deliverables.

Raw current evidence and production hashes: [`reconciliation/2026-10-19/`](reconciliation/2026-10-19/). Existing GitHub workflows and setup instructions are retained; the source update does not require workflow permissions.

Two narrowly scoped reconciliation fixes supplement the recovered package: `buildToolsVersion` is pinned to 35.0.0 to match its documented standalone/CI tools; API27 navigation-bar theme attributes move to `values-v27` while API26 uses the same base themes without unsupported attributes. No Java behavior, application ID or preference key changes were introduced by these fixes. The final source hash inventory includes them.

Reproduce using `bash build-local.sh`, `bash tests/native/setup-native.sh`, `bash tests/native/run-native.sh` and `python3 provenance/awqaf-2027/validate_timetable.py`, with the documented API35 tools and JDK17.

## Remaining limits

No physical phone or OEM launcher was available. Installed upgrade/data retention, real launcher cell allocation, notification sound/delivery, permission dialogs, reboot and battery/Doze behavior remain untested. The existing phone-specific crash cannot be declared resolved without its device evidence.

The phase calculation is bounded correctly. The widget's rendered state still depends on Android refreshing it: in the final minute it requests an absolute-time fallback, but if Android withholds that refresh an old live timer can pass zero, or an iqamah label/snapshot can remain stale. This recovery therefore does **not** claim guaranteed disappearance precisely at iqamah. Opening the app refreshes widgets. Official timetable coverage ends on 31 December 2027.

Platform references: [Android app signing](https://developer.android.com/studio/publish/app-signing), [widget sizing](https://developer.android.com/develop/ui/views/appwidgets/layouts), [Doze restrictions](https://developer.android.com/training/monitoring-device-state/doze-standby).
