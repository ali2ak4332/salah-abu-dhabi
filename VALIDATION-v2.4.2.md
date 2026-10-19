# Salah Abu Dhabi2.4.2 — remove duplicate widget refreshes

18 October2026. Package ae.prayertimes.abudhabi; version2.4.2/code22. Started from the freshly materialized v2.4.1 source (saved version21) and APK (saved version20), not the stale2.2 workspace. Latest change history and2.4/2.4.1 validation were inspected; their exactly-two-widget design and digits-only Compact iqamah minutes are preserved.

## One focused improvement

Android's onUpdate callback first refreshed the IDs passed in the callback, then refreshed every discovered widget again. With callback targets101/102 and four installed instances, the actual submitted sequence was [101,102,101,102,201,202]. Both callback targets were rendered/submitted twice.

The second pass now skips the callback targets that were already attempted. It still refreshes every other Compact/Horizontal instance before scheduling the shared display alarm. Explicit callback IDs not yet returned by launcher discovery remain handled by the first pass. Null/empty callback batches still refresh discovered instances. The shared timestamp, options-change skip, per-instance exception handling and later recovery remain. No automatic retry loop or alarm policy was added. This reduces duplicate work; no battery saving or physical-device speed improvement is claimed.

## Before / after validation

Six framework-expanded regression cases fail against unchanged2.4.1 production classes on API26/29/35. They observe exact update counts rather than merely checking membership. Final full suite: **OK (49 tests),105.345 seconds**, comprising all43 existing cases and six new cases.

The new cases cover both providers, multi-ID callback batches, all remaining installed instances, callback IDs not yet in discovery, null/empty batches, rejected update/options failures, healthy-instance counts and later recovery. Baseline output:tests/native/widget-update-once-baseline.txt. Current full output:tests/native/results.txt. The default portable runner includes WidgetUpdateOnceTest.

All existing layout/countdown checks remain active and passed:3,360 widget profiles,1,680 Compact iqamah profiles,192 density profiles, exact adhan/expiry boundaries on API26/29/35, snapshot minute/expiry alarm isolation, all-instance lifecycle refresh, numeral/time choices, typography, missing/expired data, settings/recreation, main-screen startup and rejected launcher instances. Their detailed matrix is recorded in VALIDATION-v2.4.1.md. No assertion was relaxed, and TwoWidgetTest/NativeUiTest source is unchanged.

## Build and preservation

- Signed API35 build passes using Java17.0.20, ECJ3.40.0, official platform35 revision2 and Build Tools35.0.0; six existing deprecation warnings, no compilation errors. Rebuilt isolated public test dependencies after cached tools had been removed; no app dependency changed.
- Pure-Java tests pass all2,235 prayer/estimated-iqamah boundaries/deliveries,6,705 reminder boundaries,64 Friday exclusions,15 format/countdown and8 widget-date checks, plus midnight/expiry/custom intervals.
- Offline Awqaf provenance validator passes:447 saved days,365 official2027 dates, all283 overlap dates exact and original365-row prefix byte-identical. No source correction or prediction was introduced.
- Every saved source resource/asset is byte-identical to2.4.1, including layouts, fitted fonts, backgrounds, timetable and chime. Timetable/chime bytes also match between the old and new signed APKs. AlertScheduler, alert/reminder policy, receiver, preferences, phase and timetable classes are byte-identical. MainActivity changes only its About version title; its one-page home and rows are preserved.
- Only production behavior edit is PrayerWidget's callback skip list/helper; remaining production edits are version metadata. No permission, service, account, tracking, network access, paid dependency or new feature.
- APK v2/v3 signatures verify; zipalign4-byte check passes. Both actual old/new APKs have the same public certificate SHA256:4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af. Same package and higher code preserve signing/update compatibility; no phone installation was performed.
- Complete source ZIP retains all prior files, build configs/script, tests/history, design/licenses and official provenance. Private signing keys/passwords, SDK/test caches and compiled outputs/APKs are excluded. Historical2.4.1 outputs/hashes remain under their existing versioned filenames.

Signed APK SHA256:9ff6a651664ec521aaf7db2960bbd84b3f37f6429d36ae367e44488994497b5c.
Exact current production source/resource/asset/manifest hashes:tests/native/app-source.sha256. Run sha256sum -c tests/native/app-source.sha256 from the project root. Build output:tests/native/build-v2.4.2.txt.

## Limits

No physical phone or OEM launcher was available. Actual installed update/data retention, sound/notification timing, TalkBack traversal, battery cost and Doze behavior still need real-device checks. Existing Compact minute snapshots can remain stale when Android delays a refresh; the next delivered render recomputes/clears them. This fix does not change those timing guarantees or resolve the earlier phone-specific crash without its log. Install over the existing app rather than uninstalling or clearing data.
