# Salah Abu Dhabi 2.6

Offline native Android prayer times for Abu Dhabi. Saved official Awqaf adhan data covers **11 October 2026–31 December 2027**, inclusive (447 days). Independent personal app, not an official Awqaf product.

## Install / update

Install `Salah-Abu-Dhabi-v2.6.apk` on Android 8.0 or later. This build retains package `ae.prayertimes.abudhabi`, increases version code to 25, and uses the same personal signing certificate as v1.9.1. Install over the existing app to retain settings. Do not uninstall or clear data to work around an installation error.

The app continues to use platform fonts, preserving the conservative v1.9.1 recovery change. No bundled font is loaded at runtime. The earlier phone-specific crash was not reproduced and no phone crash log was supplied; passing host-side tests is not proof that the reported phone issue is fixed.

## New in 2.6: Shurooq notification

- **Settings → Sunrise / الشروق** controls a separate daily notification at the official saved Awqaf sunrise time for Abu Dhabi.
- Enabled by default in this update. It runs only when Android notifications and its own Sunrise channel are allowed; opening the update does not automatically request or change system permissions.
- Its own channel uses the same short chime by default, with independent notification sound settings. It respects notification volume, channel mute and Do Not Disturb.
- Prayer alerts, advance reminders, estimated iqamah, five-prayer widgets and saved appearance settings are preserved. Sunrise is not a prayer or iqamah and is never added to the prayer countdown.
- Separate alarm and delivered-event records avoid collisions and repeat notifications. Past alerts over 15 minutes late are skipped. Reboot/unlock, app update, clock/timezone changes and reopening restore scheduling. A force-stop requires reopening.
- Uses only the existing 447-day official timetable, fixed to Asia/Dubai. No inferred dates, internet access or automatic timetable update. The final sunrise notification is 31 December 2027.
- Exact-alarm permission is optional; without it Android may delay the chime. Physical-phone delivery/sound and an actual upgrade still require a device check.
- Full validation and limitations are in `VALIDATION-v2.6.md`. Install over the existing app without uninstalling.

## Retained from 2.5.1: widget regressions corrected

- Retains v2.5’s two widgets, rounded prayer highlights, lighter names, amber estimated-iqamah state and next-adhan/iqamah header.
- Horizontal names stay at least 9dp; short strips reclaim decorative vertical padding so times stay readable too.
- Compact reserves a readable header at normal sizes, measuring English/Arabic timer labels and both AM/PM forms. The date gives way when necessary. The smallest 110×110 and 130×117 cells may omit the header; enlarge the widget for easier reading.
- Live seconds remain when Android’s native digits exactly match the selected Western or Eastern Arabic numerals. Other digit scripts use localized H:MM snapshots.
- In the last minute, Compact shows the absolute target time (for example, “Iqamah at 12:30”) and stops its live timer. One non-wakeup display alarm requests this change and the actual boundary refresh.
- **Android may delay either refresh. If the pre-boundary refresh itself is withheld, an old live timer can still pass zero until the launcher updates.** This is an explicit platform limitation, not a guaranteed zero clamp. Opening the app refreshes the widgets.
- Main-app layout, official adhan times, chime, prayer/reminder alarm policy, saved settings and placed widget identities are preserved. A sub-millisecond display-boundary bug is corrected in the shared phase calculation.
- Build, native test evidence and limitations are recorded in `VALIDATION-v2.5.1.md`. Version code 24; install over the existing app.

Earlier release details are in `CHANGELOG.md` and the versioned validation reports.

## Main features

- Five adhan rows; sunrise is shown separately, has its own notification setting, and is never treated as a prayer.
- Live next-adhan countdown and optional estimated-iqamah countdown.
- Previous/next day, Today and calendar picker, bounded to the saved timetable.
- Arabic/English; 12/24-hour time; Western or Eastern Arabic numerals; optional countdown seconds.
- Light/Dark/Follow phone and Green/Navy/Sand themes.
- Settings, source details, widget controls and test chime under the top more menu.
- Fixed Asia/Dubai time even while travelling. No location permission.
- Offline; no account, tracking, internet permission or automatic timetable updates.

## Prayer chime and advance reminders

Turn on **Prayer alerts**, allow Android notifications (Android 13+), and allow **Alarms & reminders** for precise timing (Android 12+). Use **Settings → Test chime** and check notification volume and Do Not Disturb.

Five prayers use the existing soft 1.05-second chime, not a full adhan recording. Sunrise uses a separate setting and notification category described above. Notifications open the app. Alerts restore after reboot/unlock, app updates and clock changes. A force-stop cancels alarms until the app is opened again. Android battery restrictions, notification/channel settings and Do Not Disturb may delay or silence alerts; alerts more than 15 minutes late are skipped. Existing channel sound preferences are preserved. Notification category labels follow the app language.

**Settings → Advance reminders** offers Off/5/10/15 minutes and individual prayer selection. Reminders are independent of the adhan switch, default off, and skip delivery more than two minutes late or at/after the prayer. They expire at adhan. Reminders use a separate non-idle exact alarm so they do not consume the main prayer alert's idle-alarm quota.

Physical-phone notification timing, permission dialogs, sound, reboot recovery and battery behavior still need a real-device check.

## Widgets

Choose **Compact · 2 × 2** or **Horizontal · 4 × 1** in the app's Add a widget menu or the home-screen widget picker. These are the only two entries. Both show today's five official saved adhan times, fixed to Asia/Dubai even while travelling. Sunrise is not included as a prayer.

The next adhan today has a warm highlight. The short date appears when space allows. The five rows/columns remain visible in English or Arabic and follow Western/Eastern numerals, 12/24-hour format, theme, opacity and text preferences. Background mode, Green/Navy/Sand, 100/85/65/0% opacity and Small/Normal/Large text are supported. Transparent mode uses white text and a warm next-prayer accent; choose a contrasting wallpaper.

Tap anywhere to open the timetable and refresh. Compact's header shows the next adhan, or estimated iqamah only during its selected interval. Live seconds use Android's Chronometer when its native digits match the saved numeral choice; otherwise the header uses a localized H:MM snapshot. In the final minute it requests an absolute target-time display. Horizontal remains timetable-only. Android may defer the non-wakeup refreshes; see the timing limits above.

Android can defer the countdown/date/highlight update during sleep or battery restrictions. Opening the app refreshes both widgets. No fabricated times appear outside the saved timetable: the five time fields show dashes and the widget's accessibility message explains that saved times are unavailable.

Host-side native Android tests cover API 26/29/35 rendering, sizes, density, languages/numerals, settings, resizing, lifecycle and failure recovery. Actual OEM launcher cell counts, TalkBack focus grouping and phone sleep/wake behavior still require a real-device check.

## Estimated iqamah

Under **Settings → Estimated iqamah**, disable the feature or choose each prayer's interval from 0–60 minutes (0 disables that prayer). Defaults: Fajr 25; Dhuhr 20; Asr 20; Maghrib 5; Isha 20 minutes after the scheduled adhan timestamp.

These are editable estimates, not verified current official iqamah times. Defaults originate from an Awqaf official's statement reported by Al Bayan on 28 January 2009 and Al Ittihad on 29 January 2009. The historical articles do not explicitly distinguish start/end of adhan; counting from the saved adhan timestamp is the app's estimate. Follow your mosque. Friday Dhuhr is excluded because Jumuah has a separate schedule. No extra iqamah sound is added; official adhan rows and alerts remain unchanged.

Historical source: https://www.albayan.ae/across-the-uae/2009-01-28-1.397198

## Timetable provenance

Official page: https://www.awqaf.gov.ae/prayer-times

- Original 365 days (2026-10-11–2027-10-10) were retrieved on 11 October 2026 for Abu Dhabi and are preserved byte-for-byte. `Source-Timetable.pdf` is the original 365-day compilation, not the extended timetable.
- All 365 dates for 2027 were retrieved from the public service used by the official Awqaf website on 16 October 2026: location **Abu Dhabi - Abu Dhabi**, official ID **36**, method **Official Times**, `official: true`.
- All 283 overlapping dates match all six existing prayer/sunrise values exactly. The final 82 dates were appended.
- Raw monthly responses, exact request bodies, hashes, normalized data and reproducible checks are in `provenance/awqaf-2027/`. No authentication material is included.
- `app/src/main/assets/times.csv` is the production asset. All times are HH:mm UAE local time. No extrapolation, forecast or astronomical calculation is used.

Run `python3 provenance/awqaf-2027/validate_timetable.py` for the offline provenance check. Future official revisions remain possible; the app does not fetch updates. A new verified timetable is required after 31 December 2027. Friday Dhuhr is not a separate Jumuah service schedule.

## Build

Open in Android Studio with JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, API 35 and Build Tools 35.0.0. Run `gradle :app:assembleDebug`. The SDK, JDK, Gradle distribution and wrapper are not bundled.

For the standalone Linux/macOS build:

```sh
export ANDROID_PLATFORM_JAR=/absolute/path/to/platforms/android-35/android.jar
export ANDROID_BUILD_TOOLS=/absolute/path/to/build-tools/35.0.0
bash build-local.sh
```

This uses `javac` by default, or Eclipse ECJ when `ECJ_JAR` is set. Without signing credentials it produces `build/aligned.apk` (unsigned) and runs the pure-Java tests.

For an update-compatible signed build, use the original private key kept **outside this project**. Set `SALAH_KEYSTORE`, `SALAH_KEY_ALIAS`, `SALAH_STORE_PASSWORD` and `SALAH_KEY_PASSWORD` privately, then run the build. Credentials are read via environment variables; do not put them in source or commit them. No replacement key is generated. Android Studio's default debug signature cannot update the supplied APK.

## Validation

See `VALIDATION-v2.6.md` for this release's actual checks and limitations; earlier validation files record historical releases. Older `VALIDATION-v1.9*.md` files are historical, not claims about the current release. Pure-Java tests cover every saved prayer/reminder/iqamah/sunrise boundary, 64 Friday exclusions, expiry, midnight, custom offsets, numeral/time formats and source preservation. The source includes the native Android host-side test harness under `tests/native/`.

This is a personal installable build, not a Google Play production-readiness certification. No physical phone was available, and a hardware-accelerated emulator was unavailable.

## Design and third-party sources

Approved design: https://www.figma.com/design/qUiRjBYfMHu0CNmyAjStrC?node-id=2-840

Exact asset export provenance and density conversion notes are in `design/README.md`. Noto design-source fonts remain outside Android resources, with their SIL OFL license and attribution. They are not loaded or packaged in the APK; see `THIRD_PARTY_NOTICES.md`.
