LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
include $(LOCAL_PATH)/Common.mk
LOCAL_SHARED_LIBRARIES := \
	libz

ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := png
else
LOCAL_MODULE := png_neon
endif
ifneq ($(OSMAND_USE_PREBUILT),true)

common_CFLAGS := -fvisibility=hidden ## -fomit-frame-pointer

ifeq ($(HOST_OS),windows)
  ifeq ($(USE_MINGW),)
    # Case where we're building windows but not under linux (so it must be cygwin)
    # In this case, gcc cygwin doesn't recognize -fvisibility=hidden
    $(info libpng: Ignoring gcc flag $(common_CFLAGS) on Cygwin)
    common_CFLAGS := 
  endif
endif

common_C_INCLUDES += 

common_COPY_HEADERS_TO := libpng
common_COPY_HEADERS := png.h pngconf.h pngusr.h

LOCAL_CFLAGS += $(common_CFLAGS)
LOCAL_C_INCLUDES += $(common_C_INCLUDES) \
	external/zlib

include $(BUILD_STATIC_LIBRARY)

else
LOCAL_SRC_FILES := \
	../lib/$(TARGET_ARCH_ABI)/lib$(LOCAL_MODULE).a
include $(PREBUILT_STATIC_LIBRARY)
endif
