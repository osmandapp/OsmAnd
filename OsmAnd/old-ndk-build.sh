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
# Only the externals the legacy Android core links; gdal, proj and sqlite are desktop-only and need the build repo
for external in protobuf skia; do
	"$SCRIPT_LOC/../../core-legacy/externals/$external/configure.sh"
done
# NDK_PROJECT_PATH=null: ndk-build must not read AndroidManifest.xml, minSdkVersion lives in Gradle
(cd "$SCRIPT_LOC" && "$ANDROID_NDK_ROOT/ndk-build" -j2 NDK_PROJECT_PATH=null APP_BUILD_SCRIPT=jni/Android.mk \
	NDK_APPLICATION_MK=jni/Application.mk NDK_OUT=obj NDK_LIBS_OUT=libs)
