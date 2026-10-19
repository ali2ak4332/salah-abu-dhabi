# Salah Abu Dhabi 2.4.1 — Compact iqamah minutes

17 October 2026. Package `ae.prayertimes.abudhabi`; version **2.4.1 / code 21**. Built from the delivered v2.4/code20 source, using the original signing certificate.

## Result and timing semantics

Exactly two launcher widgets remain: **Compact · 2 × 2** and **Horizontal · 4 × 1**.

Compact shows digits-only whole minutes remaining at the top left, with the date on the right in both Arabic and English. For a 20-minute estimate the displayed values are **20 → 19 → … → 1**, rounded up. They are not seconds or an HH:MM clock. There is no `~`, visible caption, repeated target time, or pre-adhan widget timer. The saved Western/Eastern numeral preference is respected. Accessibility and app help identify the value as minutes until **estimated iqamah**.

The existing enabled setting, custom per-prayer 0–60-minute intervals, Friday Dhuhr exclusion and phase calculations are reused. A render strictly before adhan or at/after the target clears and hides the value. Horizontal remains timetable-only. The main-screen seconds preference is unchanged and does not affect this minute-only widget display.

**This is a best-effort minute snapshot. Android can defer starting, updating or removing it while asleep or battery-restricted. A stale positive value can remain until the next delivered update. Opening the app refreshes it. Exact real-device start/clear timing is not guaranteed.** The code never emits a negative value, and there is no running Chronometer which could cross zero while an expiry broadcast is delayed.

Only one non-wakeup, one-shot RTC display alarm is requested for the next minute boundary, bounded by iqamah, when Compact exists and the phase is active. It does not use allow-while-idle, create an alarm-clock indicator, add a service or sound, or consume the prayer alert's idle-alarm quota. No minute work runs before/after the phase, when iqamah is disabled, or for Horizontal alone. Exact-alarm permission denial falls back to an ordinary alarm.

## Layout and lifecycle

- All five prayer-name/time font sizes match v2.4 for the same dimensions and preferences. The compact header uses a smaller fitted font and recovers outer padding/row slack instead of shrinking prayer text.
- The countdown remains physically left of the date in both languages; neither overlaps prayer rows.
- Supported-size tests include Compact 110×110, 130×117, 140×140, 160×160, intermediate sizes, large and unusual shapes. Nonstandard dimensions below safe text bounds can omit the header rather than overlap content.
- Rendering and scheduling use the same captured timestamp, retaining an expiry alarm if rendering itself crosses the deadline. Add/update/resize/removal callbacks refresh all remaining instances before replacing the global display alarm, preventing partial lifecycle changes from abandoning a stale timer on another widget.
- Existing per-instance failure isolation, resize variants, parcel round trips and fixed row/column identities remain tested.

## Verification

Final full native run: **OK (43 tests), 110.911 seconds**. The exact output is recorded in `tests/native/results.txt`.

- The existing **3,360-profile** widget matrix passes across both shapes, English/Arabic, Western/Eastern numerals, 12/24-hour clocks, five font-scale settings and all text preferences.
- Added **1,680 Compact countdown profiles** verify phase visibility, left/right header ordering, exact preserved prayer font, native glyph/container bounds, successive minute values, expiry clearing and preference variants. All profiles show the countdown without header omissions.
- **192 existing density profiles** verify physical-pixel and fallback glyph bounds at hdpi/xhdpi/xxhdpi/xxxhdpi. Actual 640×640 native previews for active Compact Arabic/Eastern/12-hour and English/Western/12-hour were visually inspected.
- API **26 / 29 / 35** boundary tests cover each prayer, adhan−1ms, exact adhan, adhan+1ms, target−1ms, exact target and after target; disabled/global and 0/60-minute settings; Friday Dhuhr; cross-midnight final Isha; and missing-data clearing.
- A deliberately suppressed-refresh case confirms the displayed snapshot remains positive rather than becoming a negative live timer, then is cleared by the next post-expiry render. This explicitly records the stale-snapshot limitation rather than masking it.
- Alarm tests verify one-shot/non-wakeup RTC type, no allow-while-idle flag or alarm-clock indicator, each minute boundary including expiry, disablement, exact-permission fallback, Horizontal-only and removal cancellation. Shared pre-expiry timestamp and all-instance lifecycle coverage are checked.
- Existing main-screen native tests, settings, language changes, themes/opacity, timetable expiry, background update failures and startup/resume checks remain active. Both 16-variant RemoteViews parcels contain **0 bitmap bytes**.
- API35 native build passes with six existing deprecated-API warnings. Pure Java tests pass all 2,235 prayer/iqamah boundaries, 6,705 reminder boundaries, 64 Friday exclusions, custom offsets, disabled settings, midnight/expiry and localized formats.
- Offline provenance passes: 447 saved days, 365 official 2027 dates, all 283 overlap dates exact, original 365-row prefix byte-identical. The final APK timetable and chime are byte-identical to v2.4.
- Production changes are limited to `PrayerWidget.java`, the two Compact layout files, widget help/about strings in `MainActivity.java`, and version metadata. `AlertScheduler`, prayer/reminder policy, receiver, schedule, phase, preferences and other application/drawing classes are byte-identical to v2.4. No unrelated Doze patch was imported.
- APK v2/v3 signature verification and 4-byte alignment pass. Certificate SHA-256: `4da457f76f4450e583dcecf809dbc50d63d9a14461d83b0a4cc046aa8b7e25af`.
- Full source excludes private signing material, SDK/test caches, compiled outputs and generated APKs. It includes production source/assets, provenance, build instructions, pure/native tests, release evidence and previews.

Signed APK SHA-256: `aedf4c442670c48a84bbbe349e00f7a35ce62408c8e774bec0e7e8229326dfe6`.

## Install and remaining checks

Install **Salah-Abu-Dhabi-v2.4.1.apk over the existing app**. Do not uninstall or clear data. Package/signing identity is unchanged and code21 updates code20 while retaining preferences and the two existing widget components. Pre-v2.4 obsolete widget placeholders may still need replacing with Compact or Horizontal.

No physical phone or OEM launcher was available. Installed upgrade/data retention, actual launcher dimensions and caching, TalkBack grouping, permission dialogs, notification sound, sleep/Doze/wake, force-stop recovery and reboot remain physical-device checks. Host tests do not independently resolve the earlier phone-specific crash without its log. The saved official timetable ends **31 December 2027** and does not update automatically.

## Platform references

- [RemoteViews supported operations and view types](https://developer.android.com/reference/android/widget/RemoteViews)
- [Android Chronometer implementation, including negative durations](https://android.googlesource.com/platform/frameworks/base/+/HEAD/core/java/android/widget/Chronometer.java)
- [Doze and deferred alarms](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [AlarmManager RTC/exact/idle behavior](https://developer.android.com/reference/android/app/AlarmManager)
