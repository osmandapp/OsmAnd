#!/bin/bash
THIS_LOCATION="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Get native library path on host
nativelib=$1
if [ -z "$nativelib" ]; then
	echo "Native library was not specified"
	exit 1
fi
if [ ! -f "$nativelib" ]; then
	echo "Specified '$nativelib' native library can not be found."
	exit 1
fi

# Get pid of our process
pid=`adb shell ps | grep 'net.osmand' | head -n1 | awk '{print $2}'`
package=`adb shell ps | grep 'net.osmand' | head -n1 | awk '{print $9}'`
echo "OsmAnd package: $package"
echo "OsmAnd pid: $pid"

# Launch gdbserver on target
adb shell run-as $package /data/data/$package/lib/gdbserver :5039 --attach $pid &

# Forward port
adb forward tcp:5039 tcp:5039

# Launch gdb on host
echo "Execute manually in gdb: target remote :5039"
"$ANDROID_NDK/toolchains/arm-linux-androideabi-4.7/prebuilt/windows/bin/arm-linux-androideabi-gdb" $nativelib
