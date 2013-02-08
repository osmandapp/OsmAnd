#!/bin/bash

# Get pid of our process
pid=`adb shell ps | grep 'net.osmand' | head -n1 | awk '{print $2}'`
package=`adb shell ps | grep 'net.osmand' | head -n1 | awk '{print $9}'`
echo "OsmAnd package: $package"
echo "OsmAnd pid: $pid"

# Launch gdbserver on target
adb shell run-as $package /data/data/$package/lib/gdbserver :5039 --attach $pid &

# Launch gdb on host
"$ANDROID_NDK/toolchains/arm-linux-androideabi-4.7/prebuilt/windows/bin/arm-linux-androideabi-gdb" 