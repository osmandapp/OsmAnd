#!/bin/bash

SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
NAME=$(basename $(dirname "${BASH_SOURCE[0]}") )


if [ -d "$ANDROID_HOME" ]; then
    # for backwards compatbility
    export ANDROID_SDK=$ANDROID_HOME
else
	echo "ANDROID_HOME is not set, trying ANDROID_SDK:"
    if [ -d "$ANDROID_SDK" ]; then
        export ANDROID_HOME=$ANDROID_SDK
    else
	    echo "ANDROID_SDK is also not set"
	    exit
    fi
fi
if [ ! -d "$ANDROID_NDK" ]; then
	echo "ANDROID_NDK is not set"
	exit
fi
export ANDROID_SDK_ROOT=$ANDROID_HOME
export ANDROID_NDK_ROOT=$ANDROID_NDK
#export ANDROID_NDK_TOOLCHAIN_VERSION=4.7
export BUILD_ONLY_OLD_LIB=1
"$SCRIPT_LOC/../../core-legacy/externals/configure.sh"
(cd "$SCRIPT_LOC" && "$ANDROID_NDK/ndk-build" -j2)
