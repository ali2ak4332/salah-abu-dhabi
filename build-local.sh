#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_PLATFORM_JAR:?Set ANDROID_PLATFORM_JAR to API 35 android.jar}"
: "${ANDROID_BUILD_TOOLS:?Set ANDROID_BUILD_TOOLS to SDK Build Tools 35.0.0 directory}"
rm -rf build/classes build/dex build/generated build/tests
mkdir -p build/classes build/dex build/generated
if [ -n "${ECJ_JAR:-}" ]; then COMPILER=(java -jar "$ECJ_JAR"); else COMPILER=(javac); fi
"$ANDROID_BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$ANDROID_BUILD_TOOLS/aapt2" link -0 ttf -o build/base.apk -I "$ANDROID_PLATFORM_JAR" --manifest app/src/main/AndroidManifest.xml -A app/src/main/assets --java build/generated --min-sdk-version 26 --target-sdk-version 35 build/resources.zip
"${COMPILER[@]}" -encoding UTF-8 -source 8 -target 8 -classpath "$ANDROID_PLATFORM_JAR" -d build/classes app/src/main/java/ae/prayertimes/abudhabi/*.java build/generated/ae/prayertimes/abudhabi/R.java
"$ANDROID_BUILD_TOOLS/d8" --lib "$ANDROID_PLATFORM_JAR" --min-api 26 --output build/dex build/classes/ae/prayertimes/abudhabi/*.class
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$ANDROID_BUILD_TOOLS/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
# Signing material must stay outside the source project. Without it, keep
# the aligned unsigned APK for later signing; never generate a replacement key.
if [ -n "${SALAH_KEYSTORE:-}" ]; then
  : "${SALAH_KEY_ALIAS:?Set SALAH_KEY_ALIAS from your private signing instructions}"
  : "${SALAH_STORE_PASSWORD:?Set SALAH_STORE_PASSWORD privately}"
  : "${SALAH_KEY_PASSWORD:?Set SALAH_KEY_PASSWORD privately}"
  export SALAH_STORE_PASSWORD SALAH_KEY_PASSWORD
  "$ANDROID_BUILD_TOOLS/apksigner" sign --ks "$SALAH_KEYSTORE" --ks-key-alias "$SALAH_KEY_ALIAS" --ks-pass env:SALAH_STORE_PASSWORD --key-pass env:SALAH_KEY_PASSWORD --out build/Salah-Abu-Dhabi.apk build/aligned.apk
  "$ANDROID_BUILD_TOOLS/apksigner" verify --verbose build/Salah-Abu-Dhabi.apk
else
  echo "Unsigned APK: build/aligned.apk. Supply external signing credentials to create an installable update."
fi
mkdir -p build/tests
"${COMPILER[@]}" -encoding UTF-8 -source 8 -target 8 -d build/tests app/src/main/java/ae/prayertimes/abudhabi/Schedule.java app/src/main/java/ae/prayertimes/abudhabi/AlertPolicy.java app/src/main/java/ae/prayertimes/abudhabi/DisplayFormat.java app/src/main/java/ae/prayertimes/abudhabi/PrayerPhase.java tests/*.java
java -cp build/tests ScheduleTest app/src/main/assets/times.csv
java -cp build/tests AlertPolicyTest app/src/main/assets/times.csv

java -cp build/tests DisplayFormatTest

java -cp build/tests PrayerPhaseTest app/src/main/assets/times.csv

java -cp build/tests SunriseAlertTest app/src/main/assets/times.csv
