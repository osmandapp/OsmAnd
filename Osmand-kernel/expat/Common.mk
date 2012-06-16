OSMAND_EXPAT_LOC := ./expat_library
OSMAND_EXPAT_ABS := $(LOCAL_PATH)/expat_library

# Include paths
LOCAL_C_INCLUDES += \
	$(OSMAND_EXPAT_ABS) \
	$(OSMAND_EXPAT_ABS)/lib
	
LOCAL_CFLAGS += -Wall -Wmissing-prototypes -Wstrict-prototypes -fexceptions -DHAVE_EXPAT_CONFIG_H

LOCAL_SRC_FILES := \
	$(OSMAND_EXPAT_LOC)/lib/xmlparse.c \
	$(OSMAND_EXPAT_LOC)/lib/xmlrole.c \
	$(OSMAND_EXPAT_LOC)/lib/xmltok.c	
