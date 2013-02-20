#!/bin/bash
SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# configure.sh 
"$SCRIPT_LOC"/../../core/externals/configure.sh
SRCLOC="$SCRIPT_LOC"/../../core/externals/qtbase-android
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

QTBASE_CONFIGURATION=\
"-opensource -confirm-license -xplatform android-g++ "\
"-nomake examples -nomake demos -nomake tests -nomake docs "\
"-qt-sql-sqlite "\
"-no-gui -no-widgets -no-opengl -no-accessibility -no-linuxfb -no-directfb -no-eglfs -no-xcb -no-qml-debug -no-javascript-jit "\
"-c++11 -shared -release "\
"-v"

if [ -n "$BUILD_ALL" ] || [ -n "$OSMAND_ARM_ONLY" ] || [ -n "$OSMAND_ARMv5_ONLY" ]; then
  if [ ! -d "$SRCLOC/upstream.patched.armeabi"]; then
	cp -rf "$SRCLOC/upstream.patched" "$SRCLOC/upstream.patched.armeabi"
	export ANDROID_TARGET_ARCH=armeabi
	export ANDROID_NDK_PLATFORM=android-8
	(cd "$SRCLOC/upstream.patched.armeabi" && \
		./configure $QTBASE_CONFIGURATION \
		-no-neon)
  fi
  (cd "$SRCLOC/upstream.patched.armeabi" && make -j`nproc`)
fi

if [ -n "$BUILD_ALL" ] || [ -n "$OSMAND_ARM_ONLY" ] || [ -n "$OSMAND_ARMv7a_ONLY" ]; then
  if [ ! -d "$SRCLOC/upstream.patched.armeabi-v7a" ]; then
	cp -rf "$SRCLOC/upstream.patched" "$SRCLOC/upstream.patched.armeabi-v7a"
	export ANDROID_TARGET_ARCH=armeabi-v7a
	export ANDROID_NDK_PLATFORM=android-8
	(cd "$SRCLOC/upstream.patched.armeabi-v7a" && \
		./configure $QTBASE_CONFIGURATION \
		-no-neon)
  fi
  (cd "$SRCLOC/upstream.patched.armeabi-v7a" && make -j`nproc`)
  
  if [ ! -d "$SRCLOC/upstream.patched.armeabi-v7a-neon" ]; then
	cp -rf "$SRCLOC/upstream.patched" "$SRCLOC/upstream.patched.armeabi-v7a-neon"
	export ANDROID_TARGET_ARCH=armeabi-v7a
	export ANDROID_NDK_PLATFORM=android-8
	(cd "$SRCLOC/upstream.patched.armeabi-v7a-neon" && \
		./configure $QTBASE_CONFIGURATION \
		-qtlibinfix _neon)
  fi
  (cd "$SRCLOC/upstream.patched.armeabi-v7a-neon" && make -j`nproc`)
fi

if [ -n "$BUILD_ALL" ] || [ -n "$OSMAND_X86_ONLY" ]; then
  if [ ! -d "$SRCLOC/upstream.patched.x86" ]; then
	cp -rf "$SRCLOC/upstream.patched" "$SRCLOC/upstream.patched.x86"
	export ANDROID_TARGET_ARCH=x86
	export ANDROID_NDK_PLATFORM=android-9
	(cd "$SRCLOC/upstream.patched.x86" && \
		./configure $QTBASE_CONFIGURATION)
  fi
  (cd "$SRCLOC/upstream.patched.x86" && make -j`nproc`)
fi

if [ -n "$BUILD_ALL" ] || [ -n "$OSMAND_MIPS_ONLY" ]; then
  if [ ! -d "$SRCLOC/upstream.patched.mips" ]; then
	cp -rf "$SRCLOC/upstream.patched" "$SRCLOC/upstream.patched.mips"
	export ANDROID_TARGET_ARCH=mips
	export ANDROID_NDK_PLATFORM=android-9
	(cd "$SRCLOC/upstream.patched.mips" && \
		./configure $QTBASE_CONFIGURATION)
  fi
  (cd "$SRCLOC/upstream.patched.mips" && make -j`nproc`)
fi

echo $SCRIPT_LOC
cd $SCRIPT_LOC
$ANDROID_NDK/ndk-build