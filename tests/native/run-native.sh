#!/usr/bin/env bash
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
APP=$(cd "$HERE/../.." && pwd)
CACHE=${SALAH_NATIVE_CACHE:-"$HERE/.native-qa"}
: "${ANDROID_PLATFORM_JAR:?Set ANDROID_PLATFORM_JAR to API 35 android.jar}"
test -f "$APP/build/base.apk" && test -f "$APP/build/classes/ae/prayertimes/abudhabi/MainActivity.class" || { echo 'Run bash build-local.sh first.' >&2; exit 1; }
test -f "$CACHE/deps/robolectric-4.16.1.jar" || { echo 'Run bash tests/native/setup-native.sh first.' >&2; exit 1; }
mkdir -p "$CACHE/classes/com/android/tools" "$CACHE/screenshots"
for f in "$CACHE"/deps/*.aar; do unzip -p "$f" classes.jar > "${f%.aar}-classes.jar"; done
cat > "$CACHE/classes/com/android/tools/test_config.properties" <<CONFIG
android_merged_manifest=$APP/app/src/main/AndroidManifest.xml
android_merged_resources=$APP/app/src/main/res
android_merged_assets=$APP/app/src/main/assets
android_resource_apk=$APP/build/base.apk
android_custom_package=ae.prayertimes.abudhabi
CONFIG
CP="$CACHE/classes:$APP/build/classes:$ANDROID_PLATFORM_JAR"
for f in "$CACHE"/deps/*.jar; do CP="$CP:$f"; done
if [ -n "${ECJ_JAR:-}" ]; then COMPILER=(java -jar "$ECJ_JAR"); else COMPILER=(javac); fi
"${COMPILER[@]}" -encoding UTF-8 -source 8 -target 8 -classpath "$CP" -d "$CACHE/classes" "$HERE"/src/ae/prayertimes/abudhabi/*.java
cd "$HERE"
TESTS=("$@")
if [ "${#TESTS[@]}" -eq 0 ]; then
  TESTS=(ae.prayertimes.abudhabi.NativeUiTest ae.prayertimes.abudhabi.TwoWidgetTest ae.prayertimes.abudhabi.WidgetUpdateOnceTest ae.prayertimes.abudhabi.SunriseNotificationTest)
fi
java -Xmx3g -Drobolectric.offline=true -Drobolectric.dependency.dir="$CACHE/android" -Dqa.output="$CACHE/screenshots" -cp "$CP" org.junit.runner.JUnitCore "${TESTS[@]}"
