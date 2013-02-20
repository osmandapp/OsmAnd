APP_STL := gnustl_shared
APP_ABI := all
APP_CPPFLAGS := -std=c++11 -fno-rtti -fno-exceptions
NDK_TOOLCHAIN_VERSION := 4.7

ifdef OSMAND_X86_ONLY
	APP_ABI := x86
else
ifdef OSMAND_ARM_ONLY
	APP_ABI := armeabi armeabi-v7a
else
ifdef OSMAND_ARMv5_ONLY
	APP_ABI := armeabi
endif
ifdef OSMAND_ARMv7a_ONLY
	APP_ABI := armeabi-v7a
endif
ifdef OSMAND_MIPS_ONLY
	APP_ABI := mips
endif
endif

endif

ifndef OSMAND_DEBUG_NATIVE
	# Force release compilation in release optimizations, even if application is debuggable by manifest
	APP_OPTIM := release
endif