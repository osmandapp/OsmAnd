LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := libexpat_static
else
LOCAL_MODULE := libexpat_static_neon
LOCAL_ARM_NEON := true
endif

ifeq ($(OSMAND_EXPAT_LOC),)
  OSMAND_EXPAT_LOC := ./expat_library
endif
ifeq ($(OSMAND_EXPAT_ABS),)
  OSMAND_EXPAT_ABS := $(LOCAL_PATH)/expat_library
endif

LOCAL_SRC_FILES := \
	$(OSMAND_EXPAT_LOC)/lib/xmlparse.c \
	$(OSMAND_EXPAT_LOC)/lib/xmlrole.c \
	$(OSMAND_EXPAT_LOC)/lib/xmltok.c
	
LOCAL_CFLAGS += -Wall -Wmissing-prototypes -Wstrict-prototypes -fexceptions -DHAVE_EXPAT_CONFIG_H

LOCAL_C_INCLUDES += \
	$(OSMAND_EXPAT_ABS) \
	$(OSMAND_EXPAT_ABS)/lib

include $(BUILD_STATIC_LIBRARY)
