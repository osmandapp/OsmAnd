LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := skia2.2
NDK_MODULE_PATH := $(LOCAL_PATH)
LOCAL_SRC_FILES := libskia2.2.so

include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := oskia
#NDK_MODULE_PATH := $(LOCAL_PATH)
#LOCAL_SRC_FILES := liboskia.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

#SKIA_FOLDER := $(LOCAL_PATH)/../../../skia/trunk
#SKIA_SRC := $(LOCAL_PATH)/../../../skia/trunk/src
#SKIA_FOLDER := $(LOCAL_PATH)/skia
ANDROID_FOLDER := /home/victor/projects/android/
SKIA_FOLDER := $(ANDROID_FOLDER)/external/skia
SKIA_SRC := skia

LOCAL_MODULE := osmand
LOCAL_C_INCLUDES := $(LOCAL_PATH)/jni \
	$(SKIA_FOLDER)/include/core \
	$(SKIA_FOLDER)/include/utils \
	$(SKIA_FOLDER)/include/config \
	$(SKIA_FOLDER)/include/effects \
	$(SKIA_FOLDER)/include/utils/android \
    $(SKIA_FOLDER)/src/core \
    $(ANDROID_FOLDER)/system/core/include \
    $(ANDROID_FOLDER)/frameworks/base/include


LOCAL_SRC_FILES := osmand/rendering.cpp 
	
	
LOCAL_CFLAGS := -Wall -g
LOCAL_LDLIBS := -ldl -llog -lcutils

#LOCAL_STATIC_LIBRARIES := oskia

LOCAL_SHARED_LIBRARIES := skia2.2
#    libcutils \
#    libutils \
#    libandroid_runtime \
#    libGLESv2

# LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
