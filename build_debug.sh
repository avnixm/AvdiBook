#!/bin/bash
set -e

export JAVA_HOME=/home/avnixm/development/jdk21
FLUTTER=/home/avnixm/development/flutter/bin/flutter
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$PROJECT_DIR"

echo "==> Cleaning build..."
rm -rf build

echo "==> Getting dependencies..."
$FLUTTER pub get

echo "==> Building debug APK..."
$FLUTTER build apk --debug

APK="$PROJECT_DIR/build/app/outputs/flutter-apk/app-debug.apk"
echo ""
echo "Done: $APK"
