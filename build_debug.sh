#!/bin/bash
set -e

export JAVA_HOME=/home/avnixm/development/jdk21
FLUTTER=/home/avnixm/development/flutter/bin/flutter
ADB=/home/avnixm/Android/Sdk/platform-tools/adb
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK="$PROJECT_DIR/build/app/outputs/flutter-apk/app-debug.apk"

cd "$PROJECT_DIR"

echo "==> Using incremental build (no build/ deletion)..."

echo "==> Getting dependencies..."
$FLUTTER pub get

echo "==> Building debug APK..."
$FLUTTER build apk --debug

echo "==> Detecting connected Android device..."
DEVICE_ID="$($ADB devices | awk 'NR>1 && $2=="device" {print $1; exit}')"

if [ -z "$DEVICE_ID" ]; then
	echo "No connected device found. APK built at: $APK"
	exit 0
fi

echo "==> Installing update on device $DEVICE_ID ..."
$ADB -s "$DEVICE_ID" install -r "$APK"

echo "==> Launching app..."
$ADB -s "$DEVICE_ID" shell monkey -p com.avdibook.app -c android.intent.category.LAUNCHER 1 >/dev/null

echo ""
echo "Done. Updated and launched on device $DEVICE_ID"
echo "APK: $APK"
