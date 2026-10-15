# Salah Abu Dhabi 1.6

Offline Android prayer times for Abu Dhabi, using UAE Awqaf's saved timetable from 11 October 2026 to 10 October 2027. Independent personal app; not an official Awqaf product.

## Install or update

Open `Salah-Abu-Dhabi-v1.6.apk` on an Android 8.0+ phone and install. Version 1.6 uses the same package and signing certificate as the previous APK, so it can update it without uninstalling. The home-screen subtitle shows **v1.6** to confirm the new build is installed. If Android requests permission to install from Files, allow that installation source, then turn the permission off again afterwards if desired.

## Enable the prayer chime

1. Open the app and turn on **Prayer chime alerts**.
2. Allow notifications when Android asks (Android 13+).
3. Allow **Alarms & reminders** for precise prayer-time delivery (Android 12+).
4. Open the **⋮ menu → Test chime**. Check notification volume and Do Not Disturb if silent.

The five prayers use a soft 1.05-second notification chime. Sunrise is excluded. The notification displays the scheduled UAE time and opens the app when tapped. There is no full adhan recording.

Alerts work with the app closed using Android's alarm system, with no continuously running service. They restore after reboot and unlocking, app updates and clock changes. Without precise-alarm permission Android may delay delivery; notifications more than 15 minutes late are skipped. Force-stop cancels alarms until the app is reopened. Phone battery restrictions, notification/channel settings, silent mode and Do Not Disturb can affect delivery or sound. The app respects those settings.

In **⋮ → Prayer alert settings**, finish setup, change notification sound, manage permissions or read how background alerts work. Changing a channel's sound in Android overrides the bundled default. Alerts are off until enabled in the app; installing the update does not itself grant permissions.

## Add a widget

Open **⋮ → Add a home-screen widget** in the app and choose:

- **2 × 2**: next prayer and its time.
- **2 × 3**: next prayer plus today's five prayer times.

Version 1.6 refines the Arabic 2 × 3 layout: larger prayer names and times, names aligned to the right and times to the left, and five evenly stretched rows that use the remaining widget height. The English and compact layouts keep their previous design.

Alternatively, long-press an empty home-screen area → Widgets → Salah Abu Dhabi. Both widgets can be resized. Actual cell sizes depend on the launcher. The tall widget switches to compact content when shortened. Tap a widget to open the app.

Widgets follow the app's Arabic/English setting and use UAE time. They refresh at prayer boundaries and midnight when allowed, with Android's periodic refresh as a fallback. If background timing is restricted, the display can remain stale until Android refreshes it or you open the app. During the estimated iqamah interval, both sizes show a live native countdown and the estimated target time. Android may postpone the transition at zero: if a minus sign appears, the estimate has passed; tap the widget to refresh. The widget's live countdown always includes seconds and follows the phone's numeral locale; all static times follow the app's format and numeral preferences.

## App features and polish

- Five full-width horizontal prayer rows stacked vertically, with sunrise in the footer, in your chosen 12/24-hour format.
- Live next-adhan countdown changes to estimated iqamah when adhan starts, then advances to the next adhan.
- Calendar picker, previous/next day and Today.
- Arabic/English with RTL layout and shared widget/notification language.
- One-page home screen with no scrolling container: five prayer rows fill the available space below the countdown and date controls, with the alert switch always visible. Landscape uses two side-by-side panels. Text sizes adapt to the available tile space.
- Test chime, widgets and source details are available from the top-right ⋮ menu.
- Green and gold styling and 48dp touch targets for interactive controls.
- Fixed Asia/Dubai time even if travelling; no location permission.
- No account, tracking, internet permission or automatic data updates.

## Customization

Open **⋮ → Appearance** for Light, Dark or Follow phone, with Green, Navy or Sand colours. The home screen keeps its one-page layout.

Open **⋮ → Time & numerals** for 12/24-hour time, Western (`123`) or Eastern Arabic (`١٢٣`) digits, and app countdown seconds. Time and numeral preferences apply to the app, static widget times and notification content; Android controls the system notification timestamp itself. Hiding countdown seconds rounds the remaining duration up to the next minute.

Open **⋮ → Widget style** for background mode (Match app / Light / Dark), colour, 100/85/65/0% background opacity, and Small/Normal/Large text. These settings apply to both widget sizes and all placed widgets. Text remains opaque; its maximum size adapts to available space. Transparent widgets use white text with a shadow, so readability depends on your wallpaper.

## Advance reminders

Open **⋮ → Advance reminders** to select Off, 5, 10 or 15 minutes before prayer, then choose which prayers receive reminders. Reminders start off. They are independent of the existing prayer-time chime switch; no prayers selected pauses reminders. Sunrise is excluded, and the Awqaf timetable is unchanged.

Allow notifications and precise timing when requested. The separate advance-reminder notification category lets you manage its sound in Android settings. Battery saving can delay or skip advance reminders. Reminders over two minutes late, or at/after the prayer itself, are skipped. Reminder notifications automatically expire at the prayer time.

Regular prayer alerts retain their idle-capable alarm. Advance reminders use a separate non-idle exact alarm when permitted, to avoid consuming Android's idle-alarm quota shortly before the prayer alert. Existing reboot, clock-change and update recovery also restores reminder scheduling. Force-stop requires reopening the app.

## Estimated iqamah countdown

At the scheduled adhan time, the home countdown and both widgets switch to **Estimated iqamah**, showing the estimated start time and remaining duration. They return to the next adhan when the interval ends. The five prayer rows continue to show the official saved adhan times. There is no extra iqamah sound.

Open **⋮ → Estimated iqamah** to turn this feature off, or set each prayer's interval from 1–60 minutes; 0 disables that prayer's countdown. Defaults are:

| Prayer | Minutes after scheduled adhan |
| --- | ---: |
| Fajr | 25 |
| Dhuhr | 20 |
| Asr | 20 |
| Maghrib | 5 |
| Isha | 20 |

These defaults come from an Awqaf deputy's statement reported by Al Bayan on **28 January 2009**, corroborated by Al Ittihad on 29 January 2009. No current Awqaf publication confirming all five intervals was found. The articles do not explicitly distinguish the start from the end of the adhan; this app counts from the scheduled adhan timestamp as an implementation estimate. Follow your mosque's current practice and adjust the intervals. These are **not verified 2026 official iqamah times**.

- Historical source: https://www.albayan.ae/across-the-uae/2009-01-28-1.397198
- Friday notice: https://www.awqaf.gov.ae/web.files/KhutbaAttachment/12-12-2025-ar.pdf

Friday Dhuhr is excluded from iqamah estimates because Jumuah has a separate schedule. The app does not calculate a Jumuah start from Dhuhr. The regular and advance adhan alerts retain their existing behavior.

A separate silent widget refresh is scheduled at the estimated iqamah target without using the prayer alert's idle-alarm quota. Android may defer this refresh under battery restrictions. The final saved Isha may finish its estimated countdown after its adhan; no later adhan times are invented.

## Timetable source

https://www.awqaf.gov.ae/prayer-times

UAE General Authority of Islamic Affairs, Endowments & Zakat; Abu Dhabi > Corniche; retrieved 11 October 2026. `Source-Timetable.pdf` is the 365-day compilation made in this conversation. The app's CSV matches all 2,190 times. These are adhan times, not iqamah; Friday Dhuhr is not a separate Jumuah service schedule. No adhan times are calculated or extrapolated; iqamah targets are separate editable estimates. A new verified timetable is required after 10 October 2027.

## Validation and limits

The APK compiles against API 35 and its signature is verified. Pure Java tests cover every one of the 1,825 prayer boundaries, 5,475 advance-reminder boundaries and 1,825 estimated-iqamah transitions, plus all 52 Friday exclusions, custom/disabled intervals, UAE midnight, final Isha and timetable expiry. Fifteen formatting checks cover noon/midnight, Arabic numerals and countdown rounding. Resource compilation checks both widgets and Arabic strings. The package and signing certificate support updating the previous build. The home screen passed 18 Android API 35 native-graphics layout/theme profiles, 40 live phase transitions, four date-browsing checks, and settings recreation through Robolectric. Profiles include 320×480, 360×640 and 640×360, English/Arabic, and font scales up to 2.0, with no scrolling, clipping or overflow. Both widgets also passed four style profiles and 24 adhan/iqamah boundary cases, including the 110×90 compact and 110×180 tall minimums in Arabic with large text. These checks exercise Android View measurement and rendering rather than a physical phone or launcher.

Version 1.6 additionally passed 12 Arabic tall-widget cases using Android AppWidgetHostView with responsive layout selection and launcher padding: 180×280 and 200×300 host sizes, plus the 110×180 content minimum, normal and large system fonts, before adhan and during estimated iqamah. The content filled the available host area with no clipped labels. English tall and Arabic compact regression samples also passed.

No phone or emulator was available: actual notification delivery, sound playback, permission dialogs and launcher rendering still need a device check. Use Test chime after installation and check the next scheduled prayer alert.

## Build

Android Studio: open this project with JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, API 35 and Build Tools 35.0.0. There is no bundled Gradle wrapper or SDK. Run `gradle :app:assembleDebug` or use Android Studio's build action.

Direct build (Linux/macOS): set `ANDROID_PLATFORM_JAR` to the API 35 `android.jar` and `ANDROID_BUILD_TOOLS` to Build Tools 35.0.0. Run `bash build-local.sh` with Java 17 and zip. It uses javac by default, or Eclipse ECJ if `ECJ_JAR` is set. Version 1.6 was compiled with javac from JDK 17.

For an update-compatible personal build, copy `signing/personal-test.keystore` to `build/personal-test.keystore` before building. Test alias: `androiddebugkey`; store/key password: `android`. This is a personal test identity. Use a separate protected release key before public distribution.

## Android implementation references

- Alarms: https://developer.android.com/develop/background-work/services/alarms
- Notification permission: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Notification channels: https://developer.android.com/develop/ui/compose/notifications/channels
- Widgets: https://developer.android.com/develop/ui/views/appwidgets/layouts

- Doze and alarms: https://developer.android.com/training/monitoring-device-state/doze-standby
