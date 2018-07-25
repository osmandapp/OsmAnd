APP_STL := gnustl_shared
APP_CPPFLAGS := -std=c++11 -fexceptions -frtti
APP_SHORT_COMMANDS := true

# Specify least supported Android platform version
APP_PLATFORM := android-14

# ifeq ($(wildcard $(ANDROID_NDK)/toolchains/*-4.7),)
# 	ifeq ($(wildcard $(ANDROID_NDK)/toolchains/*-4.8),)
# 		NDK_TOOLCHAIN_VERSION := 4.9
# 	else
# 		NDK_TOOLCHAIN_VERSION := 4.8
# 	endif
# else
# 	NDK_TOOLCHAIN_VERSION := 4.7
# endif

APP_ABI := x86 armeabi-v7a arm64-v8a
ifndef OSMAND_DEBUG_NATIVE
    # Force release compilation in release optimizations, even if application is debuggable by manifest
    APP_OPTIM := release
endif
