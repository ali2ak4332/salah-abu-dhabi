# Salah Abu Dhabi 1.9 — validation

16 October 2026. Package ae.prayertimes.abudhabi, version code 10.

## Build and update compatibility

Built with JDK 17 against Android API 35 and Build Tools 35.0.0 using build-local.sh. APK v2/v3 signature verification passed and the signing certificate matches the saved v1.8 APK. Signing credentials are external to the source project and not included in this ZIP.

## Timing and data

All existing pure Java checks passed: 1,825 prayer boundaries/deliveries, 5,475 advance reminders, estimated-iqamah boundaries and 52 Friday exclusions, date/time-zone/expiry behavior, 15 time/digit/countdown checks and eight widget-date/locale checks. Timetable CSV, prayer chime audio and the Schedule, AlertPolicy, AlertScheduler, PrayerPhase and DisplayFormat implementation files are byte-for-byte unchanged from saved v1.8.

## Native layout checks — 16 passed

Robolectric 4.16, Android API 35, native Skia graphics. Real Android View and RemoteViews measurement/drawing, rather than HTML mockups. Text views were checked for nonzero size, being inside the viewport, vertical layout clipping and ellipsis. Rendered screenshots were inspected.

Home profiles (six): English/light 360×640; Arabic/dark 360×640; English/light 320×480; Arabic/dark 320×480; Arabic/dark 640×360 landscape; Arabic/light 360×640 at system font scale 2.

Widget profiles (ten): Arabic/dark tall 176×264; Arabic/light tall 176×264 with Eastern numerals; English/dark compact 176×176; Arabic/light compact 176×176 at font scale 2 with Eastern numerals; Arabic/dark tall 176×264 during estimated iqamah; English/light tall 176×264; Arabic compact 110×90 before/during estimated iqamah; Arabic tall 110×180 before/during estimated iqamah.

The smallest compact name slot and Arabic tall name minimum were adjusted after rendering revealed clipping. The bundled Arabic font is used for larger Arabic names; small captions and numeric countdown/times retain system fonts so their compact metrics remain stable. The existing five horizontal home rows, top widget date, full-height Arabic tall rows and preference keys were retained. Text/muted/highlight palette contrast was also checked on opaque surfaces. Translucent widget contrast still depends on wallpaper as before.

## Limits

No physical Android phone or launcher was connected. Actual permission dialogs, notification delivery/sound, launcher padding and device-specific font/rendering behavior still require a device check. Preview images are native Android test renders. No app-store publication occurred.
