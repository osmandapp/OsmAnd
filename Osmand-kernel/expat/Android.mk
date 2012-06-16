LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
include $(LOCAL_PATH)/Common.mk
LOCAL_CFLAGS += -fPIC

ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := expat_static
else
LOCAL_MODULE := expat_static_neon
LOCAL_ARM_NEON := true
endif

ifneq ($(OSMAND_USE_PREBUILT),true)

include $(BUILD_STATIC_LIBRARY)

else
LOCAL_SRC_FILES := \
	../lib/$(TARGET_ARCH_ABI)/lib$(LOCAL_MODULE).a
include $(PREBUILT_STATIC_LIBRARY)
endif
