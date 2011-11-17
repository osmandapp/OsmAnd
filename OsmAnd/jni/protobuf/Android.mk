LOCAL_PATH := $(call my-dir)

CC_LITE_SRC_FILES := \
    google/protobuf/stubs/common.cc                              \
    google/protobuf/stubs/once.cc                                \
    google/protobuf/stubs/hash.cc                                \
    google/protobuf/stubs/hash.h                                 \
    google/protobuf/stubs/map-util.h                             \
    google/protobuf/stubs/stl_util-inl.h                         \
    google/protobuf/extension_set.cc                             \
    google/protobuf/generated_message_util.cc                    \
    google/protobuf/message_lite.cc                              \
    google/protobuf/repeated_field.cc                            \
    google/protobuf/wire_format_lite.cc                          \
    google/protobuf/io/coded_stream.cc                           \
    google/protobuf/io/coded_stream_inl.h                        \
    google/protobuf/io/zero_copy_stream.cc                       \
    google/protobuf/io/zero_copy_stream_impl_lite.cc
    

include $(CLEAR_VARS)

LOCAL_MODULE := proto
LOCAL_MODULE_TAGS := optional

LOCAL_CPP_EXTENSION := .cc

LOCAL_SRC_FILES := $(CC_LITE_SRC_FILES) \
 	google/protobuf/io/zero_copy_stream_impl.cc

LOCAL_C_INCLUDES := $(LOCAL_PATH)

LOCAL_CFLAGS := -DGOOGLE_PROTOBUF_NO_RTTI
LOCAL_LDLIBS := -llog

include $(BUILD_STATIC_LIBRARY)