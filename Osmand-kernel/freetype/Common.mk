OSMAND_FREETYPE_LOC := ./freetype_library

OSMAND_FREETYPE_ABS := $(LOCAL_PATH)/freetype_library

LOCAL_SRC_FILES:= \
	$(OSMAND_FREETYPE_LOC)/src/base/ftbbox.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftbitmap.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftfstype.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftglyph.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftlcdfil.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftstroke.c \
	$(OSMAND_FREETYPE_LOC)/src/base/fttype1.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftxf86.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftbase.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftsystem.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftinit.c \
	$(OSMAND_FREETYPE_LOC)/src/base/ftgasp.c \
	$(OSMAND_FREETYPE_LOC)/src/raster/raster.c \
	$(OSMAND_FREETYPE_LOC)/src/sfnt/sfnt.c \
	$(OSMAND_FREETYPE_LOC)/src/smooth/smooth.c \
	$(OSMAND_FREETYPE_LOC)/src/autofit/autofit.c \
	$(OSMAND_FREETYPE_LOC)/src/truetype/truetype.c \
	$(OSMAND_FREETYPE_LOC)/src/cff/cff.c \
	$(OSMAND_FREETYPE_LOC)/src/psnames/psnames.c \
	$(OSMAND_FREETYPE_LOC)/src/pshinter/pshinter.c

LOCAL_C_INCLUDES += \
	$(OSMAND_FREETYPE_ABS)/builds \
	$(OSMAND_FREETYPE_ABS)/include

LOCAL_CFLAGS += -W -Wall
LOCAL_CFLAGS += "-DDARWIN_NO_CARBON"
LOCAL_CFLAGS += "-DFT2_BUILD_LIBRARY"

# the following is for testing only, and should not be used in final builds
# of the product
#LOCAL_CFLAGS += "-DTT_CONFIG_OPTION_BYTECODE_INTERPRETER"

LOCAL_CFLAGS += -O2

