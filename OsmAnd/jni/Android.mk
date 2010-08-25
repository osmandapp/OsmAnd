LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := NativeLibrary
LOCAL_SRC_FILES := hello-jni.c

include $(BUILD_SHARED_LIBRARY)