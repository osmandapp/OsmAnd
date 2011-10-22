#LOCAL_PATH := $(call my-dir)
#include $(CLEAR_VARS)
#
#LOCAL_SRC_FILES := $(LOCAL_PATH)/../../../skia/trunk/include/core/
#
#LOCAL_MODULE := skia
#LOCAL_CFLAGS := -Wall -g 
#include $(BUILD_SHARED_LIBRARY)


LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := osmand
LOCAL_C_INCLUDES := $(LOCAL_PATH)/jni \
   $(LOCAL_PATH)/../../../skia/trunk/include/core \
   $(LOCAL_PATH)/../../../skia/trunk/include/utils \
   $(LOCAL_PATH)/../../../skia/trunk/include/config


LOCAL_SHARED_LIBRARIES := skiaosm
LOCAL_SRC_FILES := osmand/rendering.cpp
LOCAL_CFLAGS := -Wall -g 
LOCAL_LDLIBS := -ldl -lGLESv1_CM -llog
#LOCAL_LDLIBS := -lskia -lGLESv1_CM -ldl -llog
   
#LOCAL_STATIC_LIBRARIES := libskia
#    libcutils \
#    libutils \
#    libandroid_runtime \
#    libGLESv2

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := skiaosm
NDK_MODULE_PATH := $(LOCAL_PATH)
LOCAL_SRC_FILES := libskiaosm.so

include $(PREBUILT_SHARED_LIBRARY)
