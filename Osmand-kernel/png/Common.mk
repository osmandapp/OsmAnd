OSMAND_PNG_LOC := ./png_library

OSMAND_PNG_ABS := $(LOCAL_PATH)/png_library
LOCAL_SRC_FILES:= \
	$(OSMAND_PNG_LOC)/png.c \
	$(OSMAND_PNG_LOC)/pngerror.c \
	$(OSMAND_PNG_LOC)/pnggccrd.c \
	$(OSMAND_PNG_LOC)/pngget.c \
	$(OSMAND_PNG_LOC)/pngmem.c \
	$(OSMAND_PNG_LOC)/pngpread.c \
	$(OSMAND_PNG_LOC)/pngread.c \
	$(OSMAND_PNG_LOC)/pngrio.c \
	$(OSMAND_PNG_LOC)/pngrtran.c \
	$(OSMAND_PNG_LOC)/pngrutil.c \
	$(OSMAND_PNG_LOC)/pngset.c \
	$(OSMAND_PNG_LOC)/pngtrans.c \
	$(OSMAND_PNG_LOC)/pngvcrd.c \
	$(OSMAND_PNG_LOC)/pngwio.c \
	$(OSMAND_PNG_LOC)/pngwrite.c \
	$(OSMAND_PNG_LOC)/pngwtran.c \
	$(OSMAND_PNG_LOC)/pngwutil.c

LOCAL_C_INCLUDES += 


