# Salah Abu Dhabi 2.6 validation

19 October 2026. Package `ae.prayertimes.abudhabi`, version code **25**, Android 8/API26 minimum, API35 target.

## Shurooq notification

- Added a dedicated Sunrise / الشروق setting and notification channel. The first setting opens explicit Off/On choices. The sound/permissions item targets the Sunrise channel, or the necessary app notification permission/settings screen. English and Arabic settings and notification wording are covered.
- Enabled by default for this requested update, while scheduling and delivery both require the existing Android app notification permission and an allowed Sunrise channel. Opening the app or settings does not automatically request a permission. A user explicitly enabling/configuring notifications can use the existing permission flow.
- The daily timestamp comes directly from the unchanged official Awqaf timetable's `sunrise` field. No computed or extrapolated sunrise times are introduced. Fixed Asia/Dubai timezone, including when the phone timezone changes.
- Separate explicit immutable PendingIntent request 705/action, notification ID 751, channel `sunrise_chime_v1`, and preference records. Five-prayer alarms, reminders, iqamah and widgets retain their previous identities and behavior.
- Sunrise uses the existing short chime by default, respects channel sound/blocking, notification volume and Do Not Disturb, and adds no OS permission or DND bypass. Its title says Sunrise/الشروق rather than prayer or iqamah.
- Repeated rescheduling replaces the one future Sunrise alarm. Due pending delivery is settled before replacement. Early, forged, already-delivered and over-15-minute-late events are skipped. Notifications expire 15 minutes after delivery.
- Successfully delivered saved sunrise epochs are recorded in a bounded set (447 possible events in this timetable). Clock rollback cannot replay a prior day, while an accidentally future-set clock does not suppress intervening undelivered dates. The set and last-delivered marker are saved together.
- The existing receiver routes restore scheduling on boot/unlock, package replacement, clock/timezone changes and exact-alarm permission grant. Opening the app also restores scheduling. Disabling Sunrise cancels its future alarm without changing prayer/reminder settings. No new dates are scheduled after the last saved sunrise on 31 December 2027.
- Exact scheduling is used only when already allowed; denial or a race-time SecurityException uses the existing inexact idle-compatible fallback. A separate Sunrise scheduling-error flag prevents a failure from mislabelling prayer alert status, or vice versa.

## Executed checks

Final complete native run: **88/88 tests passed in 244.980 seconds**.

The full native run combines all **55 existing v2.5.1 tests**, including the widget matrix/final-minute fallback coverage, with **33 Sunrise cases** on API26/29/35. The new cases cover permission/global/channel denial; independent PendingIntent identities; replacement/cancellation; exact-alarm fallback and a revocation race; per-feature scheduling failures; receiver posting/rearming; dismissed-notification duplicate replay; multi-day clock rollback/future-date correction; early/stale/fake/disabled deliveries; pending settlement; coverage boundaries; lifecycle broadcasts; localized title/body/channel sound; setting persistence; permission request routing after activity recreation; and saved-timetable expiry messaging.

All **five pure-Java suites** pass: the four retained suites (2,235 prayer boundaries, 2,235 alert deliveries, 6,705 reminder boundaries, formatting/locale checks and 2,235 estimated-iqamah boundaries) plus independent Sunrise policy checks over all **447 saved dates**. Sunrise remains excluded from Schedule.PRAYERS, prayer reminders and the next-prayer calculation.

Raw final native output is `tests/native/results-v2.6.txt`; focused Sunrise output is `tests/native/results-v2.6-sunrise.txt`. The direct API35 build and pure suite output is `tests/native/build-v2.6.txt`. Java8/deprecation diagnostics remain host/compiler warnings, not failures.

Actual native English/Arabic Sunrise settings and Off/On dialogs are captured under `tests/native/screenshots/v2.6/` and visually inspected. No HTML mockup was used.

## Source, data and upgrade compatibility

Production edits are limited to `AlertPolicy.java`, `AlertScheduler.java`, `MainActivity.java` and version metadata. The full widget implementation, iqamah logic, Schedule class, other production classes, resources, official timetable and chime are byte-identical to v2.5.1. The main screen's five prayer rows and both five-prayer widgets remain intact.

Offline provenance validation passes: all 365 official 2027 dates, 283 exact overlap dates, unchanged original 365-row prefix, 447 total days and no extrapolation. The source archive includes the complete application source, assets, timetable provenance, build scripts, tests and validation records. It excludes signing secrets, toolchains, dependency caches and generated build classes.

Final production SHA-256 hashes are recorded in `tests/native/app-source-v2.6.sha256`. The signed APK's DEX and assets match the tested build. Original signing certificate compared against the actual delivered v2.5.1 APK:

`4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`

APK SHA-256: `29163ab735609abf9fbd907af0bdb1933f677ec339856d983937d9fc7e14948e`.

## Physical-device limitations

No phone was connected and no on-device upgrade, OEM notification/audio or actual Doze delivery was performed. Host tests exercise Android alarm/notification APIs and native rendering; they cannot guarantee that a particular phone will deliver or sound an alert on time. Battery restrictions, channel muting and Do Not Disturb still apply.

A force-stop requires opening the app again. Android revoking Alarms & reminders can stop the app and remove future exact alarms; inexact fallback is restored when the app next runs, not immediately upon revocation. Reopening after installation or permission changes is recommended.

Install this APK over the existing app without uninstalling or clearing data. Confirm Settings → Sunrise is On, Android permits its notification category, and the next real sunrise notification arrives using the phone's own sound/battery behavior. Package ID, original certificate and version code increase provide update compatibility but are not a claim that a physical install was tested.

The v2.5.1 widget limitation remains: Android may defer the final-minute guard and expiry refresh; a stale live Chronometer can pass zero until refreshed. This release does not claim to remove that platform limitation.

Platform references: [exact alarms and permission revocation](https://developer.android.com/develop/background-work/services/alarms#using-schedule-exact-alarm-permission), [notification channels](https://developer.android.com/develop/ui/compose/notifications/channels).
