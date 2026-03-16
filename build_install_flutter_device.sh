#!/bin/bash
set -euo pipefail

export JAVA_HOME=/home/avnixm/development/jdk21
export ANDROID_HOME=/home/avnixm/Android/Sdk
export ANDROID_SDK_ROOT=/home/avnixm/Android/Sdk

FLUTTER=/home/avnixm/development/flutter/bin/flutter
ADB=/home/avnixm/Android/Sdk/platform-tools/adb
APP_ID=com.avdibook.app
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK="$PROJECT_DIR/build/app/outputs/flutter-apk/app-debug.apk"

cd "$PROJECT_DIR"

DEVICE_ID="${1:-}"

if [[ -z "$DEVICE_ID" ]]; then
  echo "==> Detecting Android device via Flutter..."
  DEVICES_FILE="$(mktemp)"
  "$FLUTTER" devices > "$DEVICES_FILE"
  DEVICE_ID="$(awk -F '•' '/android-/{gsub(/^ +| +$/, "", $2); print $2; exit}' "$DEVICES_FILE")"
  rm -f "$DEVICES_FILE"
fi

if [[ -z "$DEVICE_ID" ]]; then
  echo "No Flutter Android device found. Connect a device and run:"
  echo "  $FLUTTER devices"
  exit 1
fi

echo "==> Using device: $DEVICE_ID"

echo "==> Getting dependencies..."
$FLUTTER pub get

echo "==> Building latest debug APK..."
$FLUTTER build apk --debug

echo "==> Installing debug APK (update in place)..."
$ADB -s "$DEVICE_ID" install -r "$APK"

echo "==> Launching app..."
$ADB -s "$DEVICE_ID" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null

echo ""
echo "Done. Installed latest build to Flutter device $DEVICE_ID"
echo "APK: $APK"
