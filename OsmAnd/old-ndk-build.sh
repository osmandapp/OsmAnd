#!/bin/bash

SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
NAME=$(basename $(dirname "${BASH_SOURCE[0]}") )


if [ ! -d "$ANDROID_SDK" ]; then
	echo "ANDROID_SDK is not set"
	exit
fi
if [ ! -d "$ANDROID_NDK" ]; then
	echo "ANDROID_NDK is not set"
	exit
fi
export ANDROID_SDK_ROOT=$ANDROID_SDK
export ANDROID_NDK_ROOT=$ANDROID_NDK
export ANDROID_NDK_TOOLCHAIN_VERSION=4.7

if [ -z "$OSMAND_X86_ONLY" ] && [ -z "$OSMAND_ARM_ONLY" ] && [ -z "$OSMAND_ARMv5_ONLY" ] && [ -z "$OSMAND_ARMv7a_ONLY" ] && [ -z "$OSMAND_MIPS_ONLY" ]; then
	BUILD_ALL=1
	echo "BUILD_ALL set to true"
fi
export BUILD_ONLY_OLD_LIB=1
"$SCRIPT_LOC/../../core/externals/configure.sh"
(cd "$SCRIPT_LOC" && "$ANDROID_NDK/ndk-build" -j`nproc`)