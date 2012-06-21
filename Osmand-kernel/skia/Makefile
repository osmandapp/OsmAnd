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
	$(OSMAND_SKIA_LOC)/src/ports/SkDebug_stdio.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkOSFile_stdio.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkMemory_malloc.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_gamma.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_tables.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkGlobalInitialization_default.cpp
	
SKIAFLAGS += \
	-DSK_RELEASE \
	-DGR_RELEASE=1 \
	-DSK_ENABLE_LIBPNG \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DNDEBUG \
	-fno-rtti \
	-fno-exceptions
	
LOCAL_SRC_FILES_WINDOWS = \
	$(OSMAND_SKIA_LOC)/src/ports/SkThread_win.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkTime_win.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_win.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_sandbox_none.cpp
SKIA_FLAGS_WINDOWS = \
	-DSK_BUILD_FOR_WIN32 \
	-DMINGW_HAS_SECURE_API \
	-DSkUserConfig_DEFINED \
	-DPICTURE_VERSION_ICS=1 \
	-DPICTURE_VERSION_JB=2 \
	-DSK_SCALAR_IS_FLOAT \
	-DSK_CAN_USE_FLOAT \
	-DSK_CPU_LENDIAN
	
LOCAL_SRC_FILES_MAC = \
	$(OSMAND_SKIA_LOC)/src/ports/SkThread_pthread.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_mac.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_mac_coretext.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_freetype_mac.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkTime_Unix.cpp

SKIA_FLAGS_MAC = \
	-DSK_BUILD_FOR_MAC \
	-DSK_USE_CORETEXT \
	-DSkUserConfig_DEFINED \
	-DPICTURE_VERSION_ICS=1 \
	-DPICTURE_VERSION_JB=2 \
	-DSK_SCALAR_IS_FLOAT \
	-DSK_CAN_USE_FLOAT \
	-DSK_CPU_LENDIAN
	
ifeq ($(TARGET),windows)
	LOCAL_SRC_FILES += $(LOCAL_SRC_FILES_WINDOWS)
	LOCAL_C_INCLUDES += $(OSMAND_SKIA_ABS)/include/utils/win
	SKIAFLAGS += $(SKIA_FLAGS_WINDOWS)
else ifeq ($(TARGET),darwin)
	LOCAL_SRC_FILES += $(LOCAL_SRC_FILES_MAC)
	LOCAL_C_INCLUDES += $(OSMAND_SKIA_ABS)/include/utils/mac
	SKIAFLAGS += $(SKIA_FLAGS_MAC)
else
	LOCAL_C_INCLUDES += $(OSMAND_SKIA_ABS)/include/utils/unix
	LOCAL_SRC_FILES += \
		$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_sandbox_none.cpp	\
		$(OSMAND_SKIA_LOC)/src/ports/SkThread_pthread.cpp \
		$(OSMAND_SKIA_LOC)/src/ports/SkTime_Unix.cpp \
		$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_FreeType.cpp \
		$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_linux.cpp \
		$(OSMAND_SKIA_LOC)/src/core/SkMMapStream.cpp 
	SKIAFLAGS += \
		-DSK_BUILD_FOR_UNIX
endif

CFLAGS += $(SKIAFLAGS)
CXXFLAGS += $(SKIAFLAGS)
	
# Finally, include generic rules
include ../Makefile.rules