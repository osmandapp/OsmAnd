LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(OSMAND_GIF_LOC),)
  OSMAND_GIF_LOC := ./gif_library
endif
ifeq ($(OSMAND_GIF_ABS),)
  OSMAND_GIF_ABS := $(LOCAL_PATH)/gif_library
endif

ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := gif
else
LOCAL_MODULE := gif_neon
endif

ifneq ($(OSMAND_USE_PREBUILT),true)

LOCAL_SRC_FILES := \
	$(OSMAND_GIF_LOC)/dgif_lib.c \
	$(OSMAND_GIF_LOC)/gifalloc.c \
	$(OSMAND_GIF_LOC)/gif_err.c
	
LOCAL_C_INCLUDES += \
	$(OSMAND_GIF_ABS)

LOCAL_CFLAGS += -Wno-format -DHAVE_CONFIG_H

include $(BUILD_STATIC_LIBRARY)

else
LOCAL_SRC_FILES := \
	../../jni-prebuilt/$(TARGET_ARCH_ABI)/lib$(LOCAL_MODULE).a
include $(PREBUILT_STATIC_LIBRARY)
endif