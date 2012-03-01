# This is built only for ARMv5
ifneq ($(TARGET_ARCH_ABI),armeabi-v7a)
ifneq ($(LOCAL_ARM_NEON),true)

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
endif