#!/bin/sh
# Builds "BLE Sensor Simulator.app" and the blesim command into test/ble-sensors/build/macos.
set -e
cd "$(dirname "$0")"
OUT=../build/macos
APP="$OUT/BLE Sensor Simulator.app"
rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS"
swiftc -O -o "$OUT/blesim" Sources/*.swift
cp "$OUT/blesim" "$APP/Contents/MacOS/blesim"
cat > "$APP/Contents/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>CFBundleExecutable</key><string>blesim</string>
	<key>CFBundleIdentifier</key><string>net.osmand.blesim</string>
	<key>CFBundleName</key><string>BLE Sensor Simulator</string>
	<key>CFBundlePackageType</key><string>APPL</string>
	<key>CFBundleShortVersionString</key><string>1.0</string>
	<key>LSMinimumSystemVersion</key><string>13.0</string>
	<key>NSBluetoothAlwaysUsageDescription</key><string>The simulator advertises BLE sensor services to test OsmAnd.</string>
	<key>NSPrincipalClass</key><string>NSApplication</string>
</dict>
</plist>
EOF
codesign --force --sign - "$APP"
echo "Built $(cd "$OUT" && pwd)"