LOCAL_PATH := $(call my-dir)
ROOT_PATH := $(LOCAL_PATH)/../../..
$(info OsmAnd root: $(ROOT_PATH))

# LEGACY {
ifdef BUILD_ONLY_OLD_LIB
OSMAND_MAKEFILES := \
    $(all-subdir-makefiles) \
    $(call all-makefiles-under,$(ROOT_PATH)/core-legacy/targets/android)
else 
# } LEGACY
OSMAND_MAKEFILES := \
    $(all-subdir-makefiles) \
	$(call all-makefiles-under,$(ROOT_PATH)/core/wrappers/android)
# LEGACY {
endif
# } LEGACY
$(info OsmAnd makefiles: $(OSMAND_MAKEFILES))

# By default, include makefiles only once
include $(OSMAND_MAKEFILES)
