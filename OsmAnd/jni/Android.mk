LOCAL_PATH := $(call my-dir)
ROOT_PATH := $(LOCAL_PATH)/../../..
$(info OsmAnd root: $(ROOT_PATH))

include $(CLEAR_VARS)

ifdef BUILD_ONLY_OLD_LIB
OSMAND_MAKEFILES := $(call all-makefiles-under,$(LOCAL_PATH)/) \
    $(call all-makefiles-under,$(ROOT_PATH)/core/targets/android)
$(info OsmAnd makefiles: $(OSMAND_MAKEFILES))
else 
OSMAND_MAKEFILES := \
    $(call all-makefiles-under,$(ROOT_PATH)/core/externals) \
    $(ROOT_PATH)/core/Android.mk \
    $(call all-makefiles-under,$(LOCAL_PATH)/) \
    $(ROOT_PATH)/jni/Android.mk
$(info OsmAnd makefiles: $(OSMAND_MAKEFILES))
endif

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
