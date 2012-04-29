LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# Set 'protobuf' folder only if it's not externally set
ifeq ($(PROTOBUF),)
  PROTOBUF := $(LOCAL_PATH)/../protobuf
endif

# Set 'skia' folder only if it's not externally set
ifeq ($(OSMAND_SKIA_ABS),)
  OSMAND_SKIA_ABS := $(LOCAL_PATH)/../skia/skia_library
endif

# Name of the local module
ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := osmand
else
LOCAL_MODULE := osmand_neon
LOCAL_ARM_NEON := true
endif

# Include paths
LOCAL_C_INCLUDES := $(LOCAL_PATH) \
    $(PROTOBUF) \
	$(LOCAL_PATH)/../skia \
	$(OSMAND_SKIA_ABS)/include/core \
	$(OSMAND_SKIA_ABS)/include/images \
	$(OSMAND_SKIA_ABS)/include/utils \
	$(OSMAND_SKIA_ABS)/include/config \
	$(OSMAND_SKIA_ABS)/include/effects \
	$(OSMAND_SKIA_ABS)/include/utils/android \
	$(OSMAND_SKIA_ABS)/src/core
	
LOCAL_SRC_FILES := \
	common.cpp \
	mapObjects.cpp \
	renderRules.cpp \
	rendering.cpp \
	binaryRead.cpp
	
LOCAL_CFLAGS := \
	-DGOOGLE_PROTOBUF_NO_RTTI \
	-DSK_BUILD_FOR_ANDROID \
	-DSK_BUILD_FOR_ANDROID_NDK \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_RELEASE \
	-DGR_RELEASE=1
	
ifdef OSMAND_PROFILE_NATIVE_OPERATIONS
LOCAL_CFLAGS += \
	-DPROFILE_NATIVE_OPERATIONS
endif

ifneq ($(LOCAL_ARM_NEON),true)
LOCAL_STATIC_LIBRARIES := \
	proto \
	libjpeg \
	libft2_static \
	libpng \
	libgif \
	libexpat_static
LOCAL_WHOLE_STATIC_LIBRARIES := skia
else
LOCAL_STATIC_LIBRARIES := \
	proto_neon \
	libjpeg_neon \
	libft2_static_neon \
	libpng_neon \
	libgif_neon \
	libexpat_static_neon
LOCAL_WHOLE_STATIC_LIBRARIES := skia_neon
endif

LOCAL_LDLIBS := -lz -llog -ldl

include $(BUILD_SHARED_LIBRARY)