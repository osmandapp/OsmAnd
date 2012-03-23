# Do not build for NEON
ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# Name of the local module
LOCAL_MODULE := cpufeatures_proxy

ifneq ($(OSMAND_USE_PREBUILT),true)
LOCAL_SRC_FILES := \
	cpuCheck.cpp
LOCAL_STATIC_LIBRARIES := cpufeatures
include $(BUILD_SHARED_LIBRARY)
else
LOCAL_SRC_FILES := \
	../../jni-prebuilt/$(TARGET_ARCH_ABI)/lib$(LOCAL_MODULE).so
LOCAL_STATIC_LIBRARIES := cpufeatures
include $(PREBUILT_SHARED_LIBRARY)
endif

$(call import-module,android/cpufeatures)

endif
