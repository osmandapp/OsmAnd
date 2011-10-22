LOCAL_CFLAGS := -Wall -g

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/jni 
#	$(LOCAL_PATH)/../../../skia/trunk/include/core \
#   $(LOCAL_PATH)/../../../skia/trunk/include/utils \
#	$(LOCAL_PATH)/../../../skia/trunk/include/config
# $(LOCAL_PATH)/../libvorbis-1.3.2/include


#LOCAL_SHARED_LIBRARIES := \
#	libskia 
#    libcutils \
#    libutils \
#    libandroid_runtime \
#    libGLESv2

LOCAL_MODULE := osmand

LOCAL_SRC_FILES := osmand/rendering.cpp
# LOCAL_SHARED_LIBRARIES := libvorbis libogg libFLAC

LOCAL_CFLAGS := -Wall -g 
LOCAL_LDLIBS := -ldl -lGLESv1_CM -llog

include $(BUILD_SHARED_LIBRARY)
