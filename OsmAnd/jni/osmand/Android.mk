LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
// $(LOCAL_PATH)/../libvorbis-1.3.2/include \

LOCAL_MODULE := osmand

LOCAL_SRC_FILES := rendering.c

// LOCAL_SHARED_LIBRARIES := libvorbis libogg libFLAC

LOCAL_LDLIBS := -ldl -lGLESv1_CM -llog

include $(BUILD_SHARED_LIBRARY)
