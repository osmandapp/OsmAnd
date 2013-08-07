include $(CLEAR_VARS)

LOCAL_PATH := $(call my-dir)

ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
    LOCAL_MODULE := OsmAndJNI
else
    LOCAL_MODULE := OsmAndJNI_neon
    LOCAL_ARM_NEON := true
endif

ifeq ($(LOCAL_ARM_NEON),true)
    OSMAND_BINARY_SUFFIX := _neon
else
    OSMAND_BINARY_SUFFIX :=
endif

LOCAL_STATIC_LIBRARIES := OsmAndCore$(OSMAND_BINARY_SUFFIX)

ifneq ($(OSMAND_USE_PREBUILT),true)
    LOCAL_SRC_FILES := src/swig.c

    include $(BUILD_SHARED_LIBRARY)
else
    LOCAL_SRC_FILES := \
        $(OSMAND_ANDROID_PREBUILT_ROOT)/$(TARGET_ARCH_ABI)/lib$(LOCAL_MODULE).so
    include $(PREBUILT_SHARED_LIBRARY)
endif
