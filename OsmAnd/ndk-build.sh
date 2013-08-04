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
fi

OSMAND_ANDROID_EXTERNAL_DEPENDENCIES=(expat freetype gdal giflib glm glsl-optimizer harfbuzz jpeg libpng protobuf qtbase-android skia)
"$SCRIPT_LOC/../core/externals/configure.sh" ${OSMAND_ANDROID_EXTERNAL_DEPENDENCIES[*]}
"$SCRIPT_LOC/../core/externals/build.sh" ${OSMAND_ANDROID_EXTERNAL_DEPENDENCIES[*]}
(cd "$SCRIPT_LOC" && "$ANDROID_NDK/ndk-build" -j`nproc`)
