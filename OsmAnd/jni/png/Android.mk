LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq ($(OSMAND_PNG_LOC),)
  OSMAND_PNG_LOC := ./png_library
endif
ifeq ($(OSMAND_PNG_ABS),)
  OSMAND_PNG_ABS := $(LOCAL_PATH)/png_library
endif

common_SRC_FILES := \
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

common_CFLAGS := -fvisibility=hidden ## -fomit-frame-pointer

ifeq ($(HOST_OS),windows)
  ifeq ($(USE_MINGW),)
    # Case where we're building windows but not under linux (so it must be cygwin)
    # In this case, gcc cygwin doesn't recognize -fvisibility=hidden
    $(info libpng: Ignoring gcc flag $(common_CFLAGS) on Cygwin)
    common_CFLAGS := 
  endif
endif

common_C_INCLUDES += 

common_COPY_HEADERS_TO := libpng
common_COPY_HEADERS := png.h pngconf.h pngusr.h

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += $(common_CFLAGS)
LOCAL_C_INCLUDES += $(common_C_INCLUDES) \
	external/zlib
LOCAL_SHARED_LIBRARIES := \
	libz

ifneq ($(OSMAND_NEON),true)
LOCAL_MODULE := libpng
else
LOCAL_MODULE := libpng_neon
endif

include $(BUILD_STATIC_LIBRARY)


