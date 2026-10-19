#!/usr/bin/env bash
# Isolated, pinned public dependencies. Does not change app/signing/system settings.
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
CACHE=${SALAH_NATIVE_CACHE:-"$HERE/.native-qa"}
mkdir -p "$CACHE/deps" "$CACHE/android"
command -v mvn >/dev/null || { echo "Install Apache Maven 3.9+ or put it on PATH." >&2; exit 1; }
mvn -B -f "$HERE/pom.xml" -Dmaven.repo.local="$CACHE/m2" org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DoutputDirectory="$CACHE/deps"
for version in 8.0.0_r4-robolectric-r1-i7 10-robolectric-5803371-i7 15-robolectric-13954326-i7; do
  file="android-all-instrumented-$version.jar"
  url="https://repo.maven.apache.org/maven2/org/robolectric/android-all-instrumented/$version/$file"
  if [ ! -f "$CACHE/android/$file" ]; then
    curl -fL "$url" -o "$CACHE/android/$file"
    curl -fL "$url.sha1" -o "$CACHE/android/$file.sha1"
    (cd "$CACHE/android"; printf '%s  %s\n' "$(cat "$file.sha1")" "$file" | sha1sum -c -)
  fi
done
printf 'Native QA dependencies: %s\n' "$CACHE"
