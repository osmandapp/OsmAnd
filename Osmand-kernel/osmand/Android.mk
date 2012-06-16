LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
include $(LOCAL_PATH)/Common.mk
LOCAL_CFLAGS += -fPIC

# Name of the local module
ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := osmand
else
LOCAL_MODULE := osmand_neon
LOCAL_ARM_NEON := true
endif

LOCAL_SRC_FILES += 	src/java_wrap.cpp

LOCAL_CFLAGS := \
	-DGOOGLE_PROTOBUF_NO_RTTI \
	-DANDROID_BUILD \
	-DSK_BUILD_FOR_ANDROID \
	-DSK_BUILD_FOR_ANDROID_NDK \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_RELEASE \
	-DGR_RELEASE=1
	
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