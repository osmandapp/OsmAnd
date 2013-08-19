#!/bin/bash

set -e

SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_LOC="$SCRIPT_LOC/../.."

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

function copyLibs {
	if [ -d "$ROOT_LOC/binaries/$1/$2" ]; then 
		echo "Copy binaries $1 $2";
		cp "$ROOT_LOC"/binaries/$1/$2/Release/libOsmAndJNI.$4 bin/OsmAndJNI-$1-$3.lib
	fi
}

function compile {
	OSMAND_ANDROID_EXTERNAL_DEPENDENCIES=(expat freetype gdal giflib glew glm glsl-optimizer harfbuzz jpeg libpng protobuf qtbase-desktop skia zlib)
	"$ROOT_LOC/core/externals/configure.sh" ${OSMAND_ANDROID_EXTERNAL_DEPENDENCIES[*]}
	"$ROOT_LOC/core/externals/build.sh" ${OSMAND_ANDROID_EXTERNAL_DEPENDENCIES[*]}
	"$ROOT_LOC/tools/map-viewer/externals/configure.sh"
	if [ ! -d "$ROOT_LOC/amd64-linux-gcc-release.makefile" ]; then 
		"$ROOT_LOC/build/amd64-linux-gcc.sh" release
	fi
	(cd "$ROOT_LOC/baked/amd64-linux-gcc-release.makefile" && make -j$OSMAND_BUILD_CPU_CORES_NUM OsmAndJNI)
	if [ ! -d "$ROOT_LOC/baked/i686-linux-gcc-release.makefile" ]; then 
		"$ROOT_LOC/build/i686-linux-gcc.sh" release
	fi
	(cd "$ROOT_LOC/baked/i686-linux-gcc-release.makefile" && make -j$OSMAND_BUILD_CPU_CORES_NUM OsmAndJNI)
}

compile
copyLibs linux amd64 amd64 so
copyLibs linux i686 x86 so
