#!/bin/bash

set -e
BUILD_FOLDER=Release
BUILD_VARIANT=release
if [[ ! -z "$DEBUG_CPP" ]]; then
	BUILD_FOLDER=Debug
	BUILD_VARIANT=debug
fi
SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CORE_LOC="$SCRIPT_LOC/../../core-legacy"

if [[ "$(uname -a)" =~ Linux ]]; then
	if [[ -z "$OSMAND_BUILD_CPU_CORES_NUM" ]]; then
		OSMAND_BUILD_CPU_CORES_NUM=`nproc`
	fi
fi

function copyLibs {
	if [ -d "$CORE_LOC/binaries/$1/$2" ]; then 
		echo "Copy binaries $1 $2";
		mkdir -p "$SCRIPT_LOC"/src/main/resources/
		cp "$CORE_LOC"/binaries/$1/$2/${BUILD_FOLDER}/libosmand.so "$SCRIPT_LOC"/src/main/resources/osmand-$1-$3.lib
	fi
}

function compile {
	"$CORE_LOC/externals/configure.sh"
	ARCH=$1
	#COMPILER=gcc
	COMPILER=clang
	if [ ! -d "$CORE_LOC/targets/$ARCH-linux-$COMPILER-$ARCH-linux-$COMPILER-${BUILD_VARIANT}.baked" ]; then 
		"$CORE_LOC/targets/$ARCH-linux-$COMPILER.sh" ${BUILD_VARIANT}
	fi
	(cd "$CORE_LOC/targets/$ARCH-linux-$COMPILER-$ARCH-linux-$COMPILER-${BUILD_VARIANT}.baked" && make -j$OSMAND_BUILD_CPU_CORES_NUM)

}

compile amd64
#compile i686
copyLibs linux amd64 amd64 so
#copyLibs linux i686 x86 so
