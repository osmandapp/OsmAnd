LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#include $(CLEAR_VARS)
#LOCAL_MODULE := skia2.2
#NDK_MODULE_PATH := $(LOCAL_PATH)
#LOCAL_SRC_FILES := ../libskia2.2.so
#LOCAL_PRELINK_MODULE := false
#
#include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := skia_built
#NDK_MODULE_PATH := $(LOCAL_PATH)
#LOCAL_SRC_FILES := ../skia_built.a
#include $(PREBUILT_STATIC_LIBRARY)
#
#include $(CLEAR_VARS)
#LOCAL_MODULE := png
#NDK_MODULE_PATH := $(LOCAL_PATH)
#LOCAL_SRC_FILES := ../libpng.a
#include $(PREBUILT_STATIC_LIBRARY)
#
#include $(CLEAR_VARS)
#LOCAL_MODULE := gif
#NDK_MODULE_PATH := $(LOCAL_PATH)
#LOCAL_SRC_FILES := ../libgif.a
#include $(PREBUILT_STATIC_LIBRARY)
#
#include $(CLEAR_VARS)
#LOCAL_MODULE := ft2
#NDK_MODULE_PATH := $(LOCAL_PATH)
#LOCAL_SRC_FILES := ../libft2.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

ANDROID_FOLDER := /home/victor/projects/android/
#PROTOBUF_FOLDER := /home/victor/projects/OsmAnd/libs/protobuf-2.3.0/src
PROTOBUF_FOLDER := $(LOCAL_PATH)/../protobuf

SKIA_FOLDER := $(ANDROID_FOLDER)/external/skia
SKIA_SRC := skia

LOCAL_MODULE := osmand

LOCAL_C_INCLUDES := $(LOCAL_PATH)/jni \
    $(PROTOBUF_FOLDER) \
	$(SKIA_FOLDER)/include/core \
	$(SKIA_FOLDER)/include/utils \
	$(SKIA_FOLDER)/include/config \
	$(SKIA_FOLDER)/include/effects \
	$(SKIA_FOLDER)/include/utils/android \
    $(SKIA_FOLDER)/src/core \
    $(ANDROID_FOLDER)/system/core/include \
    $(ANDROID_FOLDER)/frameworks/base/include

LOCAL_CPP_EXTENSION := .cpp
LOCAL_SRC_FILES := common.cpp \
	rendering.cpp \
	proto/osmand_odb.pb.cpp binaryRead.cpp 
	
LOCAL_CFLAGS := -Wall -g -DGOOGLE_PROTOBUF_NO_RTTI
# in that case libskia_2.2.so should be in NDK folder to be properly built
LOCAL_LDLIBS := -llog -lcutils -lskia_2.2
##LOCAL_LDLIBS := -ldl -llog -lcutils

LOCAL_STATIC_LIBRARIES := proto
#LOCAL_STATIC_LIBRARIES := skia_built gif png ft2
#LOCAL_SHARED_LIBRARIES := skia2.2

#LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)