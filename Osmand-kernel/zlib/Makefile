# Include tools definitions
include ../Makefile.vars

# Include project files
LOCAL_PATH = .
OSMAND_ZLIB_LOC := ./zlib_library

OSMAND_ZLIB_ABS := $(LOCAL_PATH)/zlib_library

LOCAL_SRC_FILES:= \
	$(OSMAND_ZLIB_LOC)/adler32.c \
	$(OSMAND_ZLIB_LOC)/compress.c \
	$(OSMAND_ZLIB_LOC)/crc32.c \
	$(OSMAND_ZLIB_LOC)/deflate.c \
	$(OSMAND_ZLIB_LOC)/inffast.c \
	$(OSMAND_ZLIB_LOC)/inflate.c \
	$(OSMAND_ZLIB_LOC)/infback.c \
	$(OSMAND_ZLIB_LOC)/inftrees.c \
	$(OSMAND_ZLIB_LOC)/trees.c \
	$(OSMAND_ZLIB_LOC)/uncompr.c \
	$(OSMAND_ZLIB_LOC)/zutil.c

# Set library name
LIBNAME = $(LIBRARY_PREFIX)z
LIBTYPE = $(STATICLIB_EXT)

# Finally, include generic rules
ifdef ZLIB_BUILD
include ../Makefile.rules
endif