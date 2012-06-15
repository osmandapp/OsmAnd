# Include tools definitions
include ../Makefile.vars

# Include project files
LOCAL_PATH = .
include Common.mk

# Set library name
LIBNAME = $(LIBRARY_PREFIX)skia
LIBTYPE = $(STATICLIB_EXT)

# Library content
LOCAL_SRC_FILES += \
	$(OSMAND_SKIA_LOC)/src/opts/SkBlitRow_opts_none.cpp \
	$(OSMAND_SKIA_LOC)/src/opts/SkBitmapProcState_opts_none.cpp \
	$(OSMAND_SKIA_LOC)/src/opts/SkUtils_opts_none.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkThread_pthread.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkDebug_stdio.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_FreeType.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_linux.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkOSFile_stdio.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkTime_Unix.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkMemory_malloc.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_gamma.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_tables.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkGlobalInitialization_default.cpp
	
SKIAFLAGS = \
	-DSK_RELEASE \
	-DGR_RELEASE=1 \
	-DSK_ENABLE_LIBPNG \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DNDEBUG \
	-fno-rtti \
	-fno-exceptions \
	-DSK_BUILD_FOR_UNIX

CFLAGS += $(SKIAFLAGS)
CXXFLAGS += $(SKIAFLAGS)
	
# Finally, include generic rules
include ../Makefile.rules