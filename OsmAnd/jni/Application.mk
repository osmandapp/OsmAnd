APP_STL := gnustl_shared
APP_CPPFLAGS := -std=c++11 -fexceptions -frtti
APP_SHORT_COMMANDS := true

# Specify least supported Android platform version
APP_PLATFORM := android-9

ifeq ($(wildcard $(ANDROID_NDK)/toolchains/*-4.7),)
	NDK_TOOLCHAIN_VERSION := 4.8
else
	NDK_TOOLCHAIN_VERSION := 4.7
endif

APP_ABI :=
ifneq ($(filter x86,$(OSMAND_ARCHITECTURES_SET)),)
    APP_ABI += x86
endif
ifneq ($(filter mips,$(OSMAND_ARCHITECTURES_SET)),)
    APP_ABI += mips
endif
ifneq ($(filter arm,$(OSMAND_ARCHITECTURES_SET)),)
    APP_ABI += armeabi armeabi-v7a
else
    ifneq ($(filter armv7,$(OSMAND_ARCHITECTURES_SET)),)
        APP_ABI += armeabi-v7a
    endif
endif
    
ifndef OSMAND_DEBUG_NATIVE
    # Force release compilation in release optimizations, even if application is debuggable by manifest
    APP_OPTIM := release
endif
