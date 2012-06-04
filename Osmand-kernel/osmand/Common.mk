PROTOBUF := $(LOCAL_PATH)/../protobuf
OSMAND_SKIA_ABS := $(LOCAL_PATH)/../skia/skia_library

# Include paths
LOCAL_C_INCLUDES := $(LOCAL_PATH)/src \
    $(PROTOBUF) \
	$(LOCAL_PATH)/../skia \
	$(LOCAL_PATH)/../expat/expat_library/lib \
	$(OSMAND_SKIA_ABS)/include/core \
	$(OSMAND_SKIA_ABS)/include/images \
	$(OSMAND_SKIA_ABS)/include/utils \
	$(OSMAND_SKIA_ABS)/include/config \
	$(OSMAND_SKIA_ABS)/include/effects \
	$(OSMAND_SKIA_ABS)/include/utils/android \
	$(OSMAND_SKIA_ABS)/src/core
	
LOCAL_SRC_FILES := \
	src/osmand_log.cpp \
	src/common.cpp \
	src/mapObjects.cpp \
	src/multipolygons.cpp \
	src/renderRules.cpp \
	src/rendering.cpp \
	src/binaryRead.cpp
	
ifdef OSMAND_PROFILE_NATIVE_OPERATIONS
LOCAL_CFLAGS += \
	-DPROFILE_NATIVE_OPERATIONS
endif