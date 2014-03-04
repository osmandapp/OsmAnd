APP_STL := gnustl_shared
APP_CPPFLAGS := -std=c++11 -fexceptions -frtti

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
    OSMAND_SKIP_NEON_SUPPORT := false
    OSMAND_FORCE_NEON_SUPPORT := false
else
    ifneq ($(filter armv7,$(OSMAND_ARCHITECTURES_SET)),)
        APP_ABI += armeabi-v7a
        ifeq ($(filter armv7-neon,$(OSMAND_ARCHITECTURES_SET)),)
            OSMAND_SKIP_NEON_SUPPORT := true
        endif
        OSMAND_FORCE_NEON_SUPPORT := false
    endif
    ifneq ($(filter armv7-neon,$(OSMAND_ARCHITECTURES_SET)),)
        APP_ABI += armeabi-v7a
        OSMAND_SKIP_NEON_SUPPORT := false
        ifeq ($(filter armv7,$(OSMAND_ARCHITECTURES_SET)),)
            OSMAND_FORCE_NEON_SUPPORT := true
        endif
    endif
endif
    
ifndef OSMAND_DEBUG_NATIVE
    # Force release compilation in release optimizations, even if application is debuggable by manifest
    APP_OPTIM := release
endif
