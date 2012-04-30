LOCAL_PATH := $(call my-dir)
include Common.mk
include $(CLEAR_VARS)

ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := proto
else
LOCAL_MODULE := proto_neon
LOCAL_ARM_NEON := true
endif

ifneq ($(OSMAND_USE_PREBUILT),true)

LOCAL_MODULE_TAGS := optional
LOCAL_CPP_EXTENSION := .cc
LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_CFLAGS := -DGOOGLE_PROTOBUF_NO_RTTI
LOCAL_LDLIBS := -llog

include $(BUILD_STATIC_LIBRARY)

else
LOCAL_SRC_FILES := \
	../jni-prebuilt/$(TARGET_ARCH_ABI)/lib$(LOCAL_MODULE).a
include $(PREBUILT_STATIC_LIBRARY)
endif
