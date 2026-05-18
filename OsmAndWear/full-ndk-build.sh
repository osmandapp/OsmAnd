#!/bin/bash

if [ ! -d "$ANDROID_SDK" ]; then
	echo "ANDROID_SDK is not set"
	exit
fi
if [ ! -d "$ANDROID_NDK" ]; then
	echo "ANDROID_NDK is not set"
	exit
fi

SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ -z "$OSMAND_ARCHITECTURES_SET" ]; then
	OSMAND_ARCHITECTURES_SET=(x86 mips arm armv5 armv7 armv7-neon)
	export OSMAND_ARCHITECTURES_SET
	echo Setting OSMAND_ARCHITECTURES_SET to default set of architectures
	echo WARNING: exporting a bash ARRAY to environment, this might not work depending on your bash version. See BUGS section of "bash(1)" for details.
fi

if [[ "$(uname -a)" =~ Linux ]]; then
	if [[ -z "$OSMAND_BUILD_CPU_CORES_NUM" ]]; then
		OSMAND_BUILD_CPU_CORES_NUM=`nproc`
	fi
fi
if [[ "$(uname -a)" =~ Darwin ]]; then
	if [[ -z "$OSMAND_BUILD_CPU_CORES_NUM" ]]; then
		OSMAND_BUILD_CPU_CORES_NUM=`sysctl hw.ncpu | awk '{print $2}'`
	fi
fi

OSMAND_ANDROID_EXTERNAL_DEPENDENCIES=(expat freetype gdal giflib glm glsl-optimizer jpeg libpng protobuf qtbase-android skia icu4c boost-android)
"$SCRIPT_LOC/../../core/externals/configure.sh" ${OSMAND_ANDROID_EXTERNAL_DEPENDENCIES[*]}
"$SCRIPT_LOC/../../core/externals/build.sh" ${OSMAND_ANDROID_EXTERNAL_DEPENDENCIES[*]}
(cd "$SCRIPT_LOC" && "$ANDROID_NDK/ndk-build" -j$OSMAND_BUILD_CPU_CORES_NUM "$@")
