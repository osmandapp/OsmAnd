LOCAL_PATH := $(call my-dir)
ROOT_PATH := $(LOCAL_PATH)/../../..

include $(CLEAR_VARS)
LOCAL_MODULE := sqlitejdbc
LOCAL_SRC_FILES := ../libsqlitejdbc/$(TARGET_ARCH_ABI)/libsqlitejdbc.so
include $(PREBUILT_SHARED_LIBRARY)

$(info OsmAnd root: $(ROOT_PATH))
ifdef BUILD_ONLY_OLD_LIB
OSMAND_MAKEFILES := \
    $(all-subdir-makefiles) \
    $(call all-makefiles-under,$(ROOT_PATH)/core-legacy/targets/android)
else 
OSMAND_MAKEFILES := \
    $(all-subdir-makefiles) \
	$(call all-makefiles-under,$(ROOT_PATH)/core/wrappers/android)
endif

$(info OsmAnd makefiles: $(OSMAND_MAKEFILES))

LOCAL_SHARED_LIBRARIES += sqlitejdbc
# By default, include makefiles only once
include $(OSMAND_MAKEFILES)
