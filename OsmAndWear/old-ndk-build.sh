#!/bin/bash

SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
NAME=$(basename $(dirname "${BASH_SOURCE[0]}") )
if [ -d "$ANDROID_HOME" ]; then
    # for backwards compatbility
    export ANDROID_SDK_ROOT=$ANDROID_HOME
fi
if [ -d "$ANDROID_NDK" ]; then
    export ANDROID_NDK_ROOT=$ANDROID_NDK
fi
if [ ! -d "$ANDROID_SDK_ROOT" ]; then
    echo "ANDROID_SDK is not set"
    exit
fi
if [ ! -d "$ANDROID_NDK_ROOT" ]; then
	echo "ANDROID_NDK is not set"
	exit
fi
export BUILD_ONLY_OLD_LIB=1
"$SCRIPT_LOC/../../core-legacy/externals/configure.sh"
(cd "$SCRIPT_LOC" && "$ANDROID_NDK_ROOT/ndk-build" -j2)
