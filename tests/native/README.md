# Native Android UI regression harness

This standalone JUnit/Robolectric harness tests the compiled app using real Android View measurement and native Android/Skia rendering. No test dependency is packaged into the APK, and no signing key is created or read by these tests.

## Dependencies

- Java 17+ (Java 21 used for the recorded run), `javac` or `ECJ_JAR` pointing to Eclipse ECJ 3.40.0, Apache Maven 3.9+, curl, unzip, sha1sum.
- Android API 35 platform and Build Tools 35.0.0 for the app's existing direct build.
- Pinned Robolectric 4.16.1 and JUnit 4.13.2, resolved by `pom.xml` from Maven Central and Google's Android Maven repository.
- Robolectric instrumented Android runtime jars for API 26, 29 and 35, downloaded by `setup-native.sh` from Maven Central. Expect roughly 800 MB of isolated test dependencies. These are omitted from the source deliverable.

Official setup references: [Robolectric](https://robolectric.org/getting-started/), [configuration](https://robolectric.org/configuring/), [Android build tools](https://developer.android.com/tools).

## Run

From the project root, set the same `ANDROID_PLATFORM_JAR`, `ANDROID_BUILD_TOOLS`, and optional `ECJ_JAR` used by `build-local.sh`, then run:

```sh
bash build-local.sh
bash tests/native/setup-native.sh
bash tests/native/run-native.sh
```

Dependencies, class files and PNG screenshots default to `tests/native/.native-qa/`. Set `SALAH_NATIVE_CACHE` to an absolute path to put this cache elsewhere. Maven/proxy configuration follows the machine's normal Maven settings; the harness does not alter network configuration or automatically accept SDK licenses.

## Current v2.6 Sunrise coverage

The full default run also includes `SunriseNotificationTest`: separate alarm/channel/notification identity, permission denial and exact-alarm fallback, setting persistence in both languages, lifecycle restoration, saved-time validation, delayed/duplicate/disabled delivery, calendar rollover and end of coverage. Delivered sunrise events remain recorded so rolling the phone clock backward cannot replay an earlier date; a mistaken future clock does not suppress intervening undelivered dates.

The pure `SunriseAlertTest` checks every one of the 447 saved sunrise boundaries, rejects prayer times or fabricated times, and verifies sunrise remains outside the five prayers, reminders and iqamah. Run it through `build-local.sh`. See `../../VALIDATION-v2.6.md` for the final executed count and device limits.

## Retained v2.5.1 coverage

The recorded v2.5 baseline reproduces 40 passes and 9 failures. Current tests preserve the next-adhan/estimated-iqamah design, assert the 9dp prayer reading floor, verify stable layout geometry separately from live ticking, and cover the final-minute absolute-time fallback. Countdown labels reserve both AM and PM glyphs in both languages. Exact digit-script matching is tested with US, Arabic, Persian and Bengali system locales. Full results and limitations are in `../../VALIDATION-v2.5.1.md`.

The deliberate withheld-refresh case asserts an expired live base and calls Android’s actual Chronometer tick formatter at the advanced clock time. This records the negative-timer platform limitation even when the Robolectric host does not deliver launcher visibility/tick callbacks reliably. It does not pretend to emulate physical-device Doze delivery.

## Scope and limits

The test source documents the current profile matrix and assertions. It includes native startup on Android 8/API26, Android 10/API29 and Android 15/API35, English/Arabic rendering, narrow/landscape/large-font layouts, per-row text and visible bounds, all five daily live adhan/iqamah transitions, UAE midnight, date navigation, preference/recreation checks, countdown units/numerals, synthetic Android15 system insets and saved-timetable expiry. Native PNGs are saved for visual inspection, including the 412×892 Figma-comparison profile.

`run-native.sh` runs the current `NativeUiTest`, `TwoWidgetTest` and `WidgetUpdateOnceTest`. Pass fully qualified class names to run a focused subset. `TwoWidgetTest` applies actual production RemoteViews and checks the two provider registrations, removed old entries, fixed row/column shapes, all five exact official times, glyph/line/container bounds, shared system typography, language/numeral/time-format choices, density/font scales, preferences, background opacity and zero bitmap payloads. It also verifies exact-size selection and parcel round trips, legacy orientation pairs, invalid launcher options, callbacks/removal/legacy minute-alarm cancellation, dates/expiry/recovery, and repeated single-instance update failure recovery.

The four prior failure cases are preserved within `TwoWidgetTest`, including main-app startup/resume while a launcher rejects one widget. Superseded provider-specific tests are kept under `legacy/` as historical evidence and are not compiled against v2.4, which deliberately removes those providers. Historical screenshots and result files are labelled by version. See the project-root `VALIDATION-v2.4.1.md` for current-run evidence and limits.

The fixed test activity overrides only the current-time method; it uses the production MainActivity lifecycle, resources, settings and view-building code. No home layout is mocked. The harness loads the unsigned resource APK produced by the same build used for the release, alongside the compiled production classes.

Host-side tests are not a physical-device installation or notification/sound/launcher test. Native graphics and compatibility checks cannot prove behavior on every Samsung/OEM device. The actual phone must still install/open the signed APK and exercise notification permissions, Test chime and its launcher widgets.


`WidgetUpdateOnceTest` verifies exact per-instance update counts on API26/29/35, callback IDs not yet present in launcher discovery, null/empty batches, rejected options/updates and recovery on a later refresh. Its six framework-expanded cases fail on unchanged v2.4.1 because callback targets were rendered twice. The default runner includes this class.
