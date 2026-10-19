# Salah Abu Dhabi 1.9.1 — recovery validation

16 October 2026. Package ae.prayertimes.abudhabi, version code 11.

## Recovery change

The release uses system fonts throughout the app and widgets. It makes no Resources.getFont calls and applies no fontVariationSettings. Noto design files are outside Android resources and are absent from the APK. The visual colour/icon/spacing/card changes are retained.

The reported phone’s exception was not supplied. Font loading was the principal new startup-risk path identified from the v1.9 changes. Removing that dependency is a conservative recovery, not a confirmed device-specific diagnosis.

## Checks

- API 35 build with JDK 17 and Build Tools 35.0.0; all existing prayer/reminder/date/iqamah tests passed.
- Native Android/Skia Robolectric checks cover the 16 layout profiles documented in VALIDATION-v1.9.md, plus English and Arabic startup with font-resource loading deliberately rejected. The recovery app opens independently of bundled-font decoding, with all five rows visible.
- APK certificate matches v1.9; version code rises from 10 to 11. Official timetable and chime audio bytes are unchanged. The APK has no TTF files; all custom-font and variation references were removed.
- DEX inspection found no missing new classes or missing desugaring references in the reported v1.9 APK.

## Installed-device limitation

An official AOSP API 35 software emulator was attempted without a hardware accelerator. It reached ADB online but did not complete boot within the bounded 200-second attempt, so no actual APK launch was performed. No Samsung handset or crash log was available. Native host-side tests do not prove the reported phone’s crash is resolved; installation on that phone is still needed. Update over the existing installation to keep preferences; do not clear data or uninstall for this recovery.
