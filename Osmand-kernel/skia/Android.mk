# This file is based on external/skia/Android.mk from Android sources

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
include $(LOCAL_PATH)/Common.mk

ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := skia
else
LOCAL_MODULE := skia_neon
LOCAL_ARM_NEON := true
endif


ifneq ($(OSMAND_USE_PREBUILT),true)

LOCAL_ARM_MODE := arm

# need a flag to tell the C side when we're on devices with large memory
# budgets (i.e. larger than the low-end devices that initially shipped)
ifeq ($(ARCH_ARM_HAVE_VFP),true)
    LOCAL_CFLAGS += -DANDROID_LARGE_MEMORY_DEVICE
endif

ifneq ($(ARCH_ARM_HAVE_VFP),true)
	LOCAL_CFLAGS += -DSK_SOFTWARE_FLOAT
endif

ifeq ($(LOCAL_ARM_NEON),true)
	LOCAL_CFLAGS += -D__ARM_HAVE_NEON
endif

# This file is replacement of $(OSMAND_SKIA_LOC)/src/ports/FontHostConfiguration_android.cpp 
LOCAL_SRC_FILES += \
	$(OSMAND_SKIA_LOC)/src/core/SkMMapStream.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkDebug_android.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkGlobalInitialization_default.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_FreeType.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_sandbox_none.cpp	\
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_android.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_gamma.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_tables.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkMemory_malloc.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkOSFile_stdio.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkThread_pthread.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkTime_Unix.cpp \
	FontHostConfiguration_android.cpp
LOCAL_C_INCLUDES += \
	$(OSMAND_SKIA_ABS)/src/ports \
	$(OSMAND_SKIA_ABS)/include/utils/android
	
ifeq ($(TARGET_ARCH),arm)

ifeq ($(LOCAL_ARM_NEON),true)
LOCAL_SRC_FILES += \
	$(OSMAND_SKIA_LOC)/src/opts/memset16_neon.S \
	$(OSMAND_SKIA_LOC)/src/opts/memset32_neon.S
endif

LOCAL_SRC_FILES += \
	$(OSMAND_SKIA_LOC)/src/opts/opts_check_arm.cpp \
	$(OSMAND_SKIA_LOC)/src/opts/memset.arm.S \
	$(OSMAND_SKIA_LOC)/src/opts/SkBitmapProcState_opts_arm.cpp \
	$(OSMAND_SKIA_LOC)/src/opts/SkBlitRow_opts_arm.cpp
		
else

LOCAL_SRC_FILES += \
	$(OSMAND_SKIA_LOC)/src/opts/SkBlitRow_opts_none.cpp \
	$(OSMAND_SKIA_LOC)/src/opts/SkBitmapProcState_opts_none.cpp \
	$(OSMAND_SKIA_LOC)/src/opts/SkUtils_opts_none.cpp
		
endif

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libz
	
ifeq ($(LOCAL_ARM_NEON),true)
LOCAL_STATIC_LIBRARIES += \
	libjpeg_neon \
	libft2_static_neon \
	libpng_neon \
	libgif_neon \
	libexpat_static_neon
else
LOCAL_STATIC_LIBRARIES += \
	libjpeg \
	libft2_static \
	libpng \
	libgif \
	libexpat_static
endif

ifeq ($(NO_FALLBACK_FONT),true)
	LOCAL_CFLAGS += -DNO_FALLBACK_FONT
endif

LOCAL_CFLAGS += \
	-DSK_BUILD_FOR_ANDROID \
	-DSK_BUILD_FOR_ANDROID_NDK \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_RELEASE \
	-DGR_RELEASE=1 \
	-DNDEBUG

LOCAL_CPPFLAGS := \
	-fno-rtti \
	-fno-exceptions
	
LOCAL_LDLIBS += -lz -llog

include $(BUILD_STATIC_LIBRARY)

else
LOCAL_SRC_FILES := \
	../lib/$(TARGET_ARCH_ABI)/lib$(LOCAL_MODULE).a
include $(PREBUILT_STATIC_LIBRARY)
endif

# Fix some errors
BUILD_HOST_EXECUTABLE := $(LOCAL_PATH)/FakeHost.mk
BUILD_HOST_STATIC_LIBRARY := $(LOCAL_PATH)/FakeHost.mk 
