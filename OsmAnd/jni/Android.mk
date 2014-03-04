LOCAL_PATH := $(call my-dir)
ROOT_PATH := $(LOCAL_PATH)/../../..
$(info OsmAnd root: $(ROOT_PATH))

# LEGACY {
ifdef BUILD_ONLY_OLD_LIB
OSMAND_MAKEFILES := \
    $(all-subdir-makefiles) \
    $(call all-makefiles-under,$(ROOT_PATH)/core/targets/android)
else 
# } LEGACY
OSMAND_MAKEFILES := \
    $(all-subdir-makefiles) \
	$(call all-makefiles-under,$(ROOT_PATH)/core/wrappers/android)
# LEGACY {
endif
# } LEGACY
$(info OsmAnd makefiles: $(OSMAND_MAKEFILES))

# Protect from previous builds
ifneq ($(TARGET_ARCH_ABI),armeabi-v7a)
    OSMAND_BUILDING_NEON_LIBRARY := false
endif

# OSMAND_FORCE_NEON_SUPPORT is used to force only NEON support on ARMv7a
ifeq ($(OSMAND_FORCE_NEON_SUPPORT),true)
    ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
        OSMAND_BUILDING_NEON_LIBRARY := true
    endif
endif

# By default, include makefiles only once
include $(OSMAND_MAKEFILES)

# If we're not asked not to support NEON and not asked to support only NEON ARMv7a, then
# make additional build
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    ifneq ($(OSMAND_SKIP_NEON_SUPPORT),true)
        ifneq ($(OSMAND_FORCE_NEON_SUPPORT),true)

            OSMAND_BUILDING_NEON_LIBRARY := true
            include $(OSMAND_MAKEFILES)

        endif
    endif
endif
