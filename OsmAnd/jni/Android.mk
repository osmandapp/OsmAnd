# Relative definition of kernel makefile
OSMAND_MAKEFILES := $(call my-dir)/../../Osmand-kernel/Android.mk

# Protect from previous builds
ifneq ($(TARGET_ARCH_ABI),armeabi-v7a)
OSMAND_BUILDING_NEON_LIBRARY := false
endif

# OSMAND_FORCE_NEON_SUPPORT is used to force only NEON support on ARMv7a
ifdef OSMAND_FORCE_NEON_SUPPORT
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
OSMAND_BUILDING_NEON_LIBRARY := true
endif
endif

# By default, include makefiles only once
include $(OSMAND_MAKEFILES)

# If we're not asked not to support NEON and not asked to support only NEON ARMv7a, then
# make additional build
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
ifndef OSMAND_SKIP_NEON_SUPPORT
ifndef OSMAND_FORCE_NEON_SUPPORT

OSMAND_BUILDING_NEON_LIBRARY := true
include $(OSMAND_MAKEFILES)

endif
endif
endif