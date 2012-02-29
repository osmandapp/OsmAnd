LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# Set 'protobuf' folder only if it's not externally set
ifeq ($(PROTOBUF),)
  PROTOBUF := $(LOCAL_PATH)/../protobuf
endif

# Set 'skia' folder only if it's not externally set
ifeq ($(SKIA_ABS),)
  SKIA_ABS := $(LOCAL_PATH)/../skia
endif

# Name of the local module
LOCAL_MODULE := osmand

# Include paths
LOCAL_C_INCLUDES := $(LOCAL_PATH) \
    $(PROTOBUF) \
	$(SKIA_ABS)/trunk/include/core \
	$(SKIA_ABS)/trunk/include/utils \
	$(SKIA_ABS)/trunk/include/config \
	$(SKIA_ABS)/trunk/include/effects \
	$(SKIA_ABS)/trunk/include/utils/android \
	$(SKIA_ABS)/trunk/src/core
	
LOCAL_CPP_EXTENSION := .cpp
LOCAL_SRC_FILES := common.cpp mapObjects.cpp \
	renderRules.cpp rendering.cpp \
	binaryRead.cpp 
	
LOCAL_CFLAGS := \
	-DGOOGLE_PROTOBUF_NO_RTTI \
	-DSK_BUILD_FOR_ANDROID \
	-DSK_BUILD_FOR_ANDROID_NDK \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_USE_POSIX_THREADS \
	-DSK_RELEASE \
	-DGR_RELEASE=1

LOCAL_STATIC_LIBRARIES := proto skia

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)