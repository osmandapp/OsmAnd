# Do not build for NEON
ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# Name of the local module
LOCAL_MODULE := cpufeatures_proxy

LOCAL_SRC_FILES := \
	cpuCheck.cpp

LOCAL_STATIC_LIBRARIES := cpufeatures

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)

endif
