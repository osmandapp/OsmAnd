LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

ifeq ($(OSMAND_JPEG_LOC),)
  OSMAND_JPEG_LOC := ./jpeg_library
endif
ifeq ($(OSMAND_JPEG_ABS),)
  OSMAND_JPEG_ABS := $(LOCAL_PATH)/jpeg_library
endif

LOCAL_SRC_FILES := \
	$(OSMAND_JPEG_LOC)/jcapimin.c \
	$(OSMAND_JPEG_LOC)/jcapistd.c \
	$(OSMAND_JPEG_LOC)/jccoefct.c \
	$(OSMAND_JPEG_LOC)/jccolor.c \
	$(OSMAND_JPEG_LOC)/jcdctmgr.c \
	$(OSMAND_JPEG_LOC)/jchuff.c \
	$(OSMAND_JPEG_LOC)/jcinit.c \
	$(OSMAND_JPEG_LOC)/jcmainct.c \
	$(OSMAND_JPEG_LOC)/jcmarker.c \
	$(OSMAND_JPEG_LOC)/jcmaster.c \
	$(OSMAND_JPEG_LOC)/jcomapi.c \
	$(OSMAND_JPEG_LOC)/jcparam.c \
	$(OSMAND_JPEG_LOC)/jcphuff.c \
	$(OSMAND_JPEG_LOC)/jcprepct.c \
	$(OSMAND_JPEG_LOC)/jcsample.c \
	$(OSMAND_JPEG_LOC)/jctrans.c \
	$(OSMAND_JPEG_LOC)/jdapimin.c \
	$(OSMAND_JPEG_LOC)/jdapistd.c \
	$(OSMAND_JPEG_LOC)/jdatadst.c \
	$(OSMAND_JPEG_LOC)/jdatasrc.c \
	$(OSMAND_JPEG_LOC)/jdcoefct.c \
	$(OSMAND_JPEG_LOC)/jdcolor.c \
	$(OSMAND_JPEG_LOC)/jddctmgr.c \
	$(OSMAND_JPEG_LOC)/jdhuff.c \
	$(OSMAND_JPEG_LOC)/jdinput.c \
	$(OSMAND_JPEG_LOC)/jdmainct.c \
	$(OSMAND_JPEG_LOC)/jdmarker.c \
	$(OSMAND_JPEG_LOC)/jdmaster.c \
	$(OSMAND_JPEG_LOC)/jdmerge.c \
	$(OSMAND_JPEG_LOC)/jdphuff.c \
	$(OSMAND_JPEG_LOC)/jdpostct.c \
	$(OSMAND_JPEG_LOC)/jdsample.c \
	$(OSMAND_JPEG_LOC)/jdtrans.c \
	$(OSMAND_JPEG_LOC)/jerror.c \
	$(OSMAND_JPEG_LOC)/jfdctflt.c \
	$(OSMAND_JPEG_LOC)/jfdctfst.c \
	$(OSMAND_JPEG_LOC)/jfdctint.c \
	$(OSMAND_JPEG_LOC)/jidctflt.c \
	$(OSMAND_JPEG_LOC)/jidctfst.c \
	$(OSMAND_JPEG_LOC)/jidctint.c \
	$(OSMAND_JPEG_LOC)/jidctred.c \
	$(OSMAND_JPEG_LOC)/jquant1.c \
	$(OSMAND_JPEG_LOC)/jquant2.c \
	$(OSMAND_JPEG_LOC)/jutils.c \
	$(OSMAND_JPEG_LOC)/jmemmgr.c \
	$(OSMAND_JPEG_LOC)/armv6_idct.S

# the original android memory manager.
# use sdcard as libjpeg decoder's backing store
LOCAL_SRC_FILES += \
	$(OSMAND_JPEG_LOC)/jmem-android.c

LOCAL_CFLAGS += -DAVOID_TABLES 
LOCAL_CFLAGS += -O3 -fstrict-aliasing -fprefetch-loop-arrays

# enable tile based decode
LOCAL_CFLAGS += -DANDROID_TILE_BASED_DECODE

# enable armv6 idct assembly
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS += -DANDROID_ARMV6_IDCT
endif

ifneq ($(OSMAND_BUILDING_NEON_LIBRARY),true)
LOCAL_MODULE := libjpeg
else
LOCAL_MODULE := libjpeg_neon
endif

include $(BUILD_STATIC_LIBRARY)