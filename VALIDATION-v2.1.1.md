# Salah Abu Dhabi 2.1.1 validation

Recorded 17 October 2026 (UTC). Package `ae.prayertimes.abudhabi`, version name **2.1.1**, version code **14**. Started from the current saved v2.1 source archive and APK, not an earlier workspace copy.

## One focused improvement

Contain RuntimeExceptions within each widget refresh, including options lookup and RemoteViews construction/submission. Rejected or stale instances no longer abort later instances, app startup/resume, or provider callback scheduling. The error remains in the local Android log; the next refresh retries the instance normally. Options-change callbacks use the same guard. This does not promise a failing launcher's widget is rendered successfully.

## Before / after evidence

Four new native fault-injection cases all failed on unchanged v2.1 because the injected launcher/options exceptions escaped. The startup case's stack reached `MainActivity.onResume` through `PrayerWidget.updateAll`. A test assertion was corrected to check removal of the scheduling-error preference (the scheduler removes the key rather than storing false).

The final portable suite passed **27 tests in 40.874 seconds**: nine existing app cases, fourteen existing widget cases and four new failure-containment cases. The new cases check rejected updates, rejected options, continued refresh of both widget providers, later recovery, provider scheduling and app startup/resume in English and Arabic. This verifies a reproducible crash path; it does not establish the cause of the earlier phone-specific crash report.

The existing native Android/Skia matrix passes on API26/29/35: one-page home bounds, five prayer rows, English/Arabic, large fonts, preference recreation, responsive compact/expanded widgets, timer phases, accessibility semantics and minute alarm isolation. The full unchanged profile matrix is described in `tests/native/VALIDATION-v2.1.md`; the current run's output is `tests/native/results.txt`. No physical device or OEM launcher was available. No claim of actual installation, notification/sound delivery, TalkBack traversal or Doze behavior is made.

## Build and preservation checks

- Signed API35 release build succeeded using OpenJDK17.0.20, Eclipse ECJ3.40.0, Android platform35 revision2 and Build Tools35.0.0. Five existing deprecation warnings; no compile errors.
- Pure-Java tests passed: all 2,235 prayer deliveries/boundaries, 6,705 reminder boundaries, all 2,235 estimated-iqamah boundaries, 64 Friday Dhuhr exclusions, expiry/midnight/custom offsets, 15 format/countdown and 8 widget-date checks.
- APK v2/v3 signatures verify. Certificate SHA256 matches the saved v2.1 APK: `4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`. Same package and higher version code preserve signing/update compatibility; no phone install was performed.
- All 44 resource files are byte-identical to v2.1, including widget layouts/icons/chime. Timetable, scheduling/notification policies, preference code and estimated-iqamah calculation code are byte-identical. The only other production edits are version metadata and the About version title.
- Timetable remains the saved official 447 days, 11 October2026–31 December2027. Timetable CSV SHA256: `972f2e4b08c57822bdb82020e48de01e7ca689d1ba539dc62be3294df228790a`.
- Chime SHA256: `9ad738a832c2ef5819e1494e60ccc77ef4751b0100316379d6d2bd9a7640b551`. Timetable and chime bytes also match between the old and new signed APKs.
- Production source/resource/asset/manifest hashes checked before and after the native run. Run `sha256sum -c tests/native/app-source.sha256` from the project root. The v2.1 manifest is retained as `app-source-v2.1.sha256`.
- APK has no Internet permission or new permission. No accounts, tracking, advertising, services, paid dependency or additional notification policy was added.
- Complete source includes Gradle files, direct build script, tests, existing designs/licenses and official timetable provenance. Private signing keys/passwords, local build products, downloaded SDKs and test dependency caches are excluded. Signing remains external as documented in README.

## Checksums

- Signed APK: `5852f6c94bdee3c7e80717b7e26c91e29c169d7ceec017e8698db2b61f1419b1`.
- Tested unsigned resource APK: `1e207a9e0f8bc1e66795b6ad4bc093e433c2fd550c064037be3a86b51a32fb27`.
- Unsigned aligned APK: `31cde5da0d7c3dc14131f1ab5db7b8dab2650594d5dd8ba0a2934a4aeb443d22`.
- PrayerWidget.java: `29d45e31b371f5e6665caa0dc225ac842dda89813b3f145e01abf21eb6227be8`.

Robolectric's existing Invalid-ID and API26 newer-style diagnostics also occur in the passing run. The source retains historical validation/screenshots; no visual redesign was made in this patch.
