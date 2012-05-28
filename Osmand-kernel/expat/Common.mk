OSMAND_EXPAT_ABS := ./expat_library
OSMAND_EXPAT_LOC := $(LOCAL_PATH)/expat_library

# Include paths
LOCAL_C_INCLUDES += \
	$(OSMAND_EXPAT_ABS) \
	$(OSMAND_EXPAT_ABS)/lib
	
LOCAL_CFLAGS += -fPIC -Wall -Wmissing-prototypes -Wstrict-prototypes -fexceptions -DHAVE_EXPAT_CONFIG_H

LOCAL_SRC_FILES := \
	$(OSMAND_EXPAT_LOC)/lib/xmlparse.c \
	$(OSMAND_EXPAT_LOC)/lib/xmlrole.c \
	$(OSMAND_EXPAT_LOC)/lib/xmltok.c	
