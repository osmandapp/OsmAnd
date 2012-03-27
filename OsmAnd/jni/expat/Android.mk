LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := expat_static
else
LOCAL_MODULE := expat_static_neon
LOCAL_ARM_NEON := true
endif

ifeq ($(OSMAND_EXPAT_LOC),)
  OSMAND_EXPAT_LOC := ./expat_library
endif
ifeq ($(OSMAND_EXPAT_ABS),)
  OSMAND_EXPAT_ABS := $(LOCAL_PATH)/expat_library
endif

ifneq ($(OSMAND_USE_PREBUILT),true)

LOCAL_SRC_FILES := \
	$(OSMAND_EXPAT_LOC)/lib/xmlparse.c \
	$(OSMAND_EXPAT_LOC)/lib/xmlrole.c \
	$(OSMAND_EXPAT_LOC)/lib/xmltok.c
	
LOCAL_CFLAGS += -Wall -Wmissing-prototypes -Wstrict-prototypes -fexceptions -DHAVE_EXPAT_CONFIG_H

LOCAL_C_INCLUDES += \
	$(OSMAND_EXPAT_ABS) \
	$(OSMAND_EXPAT_ABS)/lib

include $(BUILD_STATIC_LIBRARY)

else
LOCAL_SRC_FILES := \
	../../jni-prebuilt/$(TARGET_ARCH_ABI)/lib$(LOCAL_MODULE).a
include $(PREBUILT_STATIC_LIBRARY)
endif
