#!/bin/bash

set -e

SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CORE_LOC="$SCRIPT_LOC/../../core"

if [[ "$(uname -a)" =~ Linux ]]; then
	if [[ -z "$OSMAND_BUILD_CPU_CORES_NUM" ]]; then
		OSMAND_BUILD_CPU_CORES_NUM=`nproc`
	fi
fi

function copyLibs {
	if [ -d "$CORE_LOC/binaries/$1/$2" ]; then 
		echo "Copy binaries $1 $2";
		cp "$CORE_LOC"/binaries/$1/$2/Release/libosmand.so bin/osmand-$1-$3.lib
	fi
}

function compile {
	"$CORE_LOC/externals/configure.sh"
	if [ ! -d "$CORE_LOC/targets/amd64-linux-gcc-amd64-linux-gcc-release.baked" ]; then 
		"$CORE_LOC/targets/amd64-linux-gcc.sh" release
	fi
	(cd "$CORE_LOC/targets/amd64-linux-gcc-amd64-linux-gcc-release.baked" && make -j$OSMAND_BUILD_CPU_CORES_NUM)
}

compile
copyLibs linux amd64 amd64 so
# copyLibs linux i686 x86 so
