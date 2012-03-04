OSMAND_MAKEFILES := $(all-subdir-makefiles)

# By default, include makefiles only once
include $(OSMAND_MAKEFILES)


# If we may support NEON, include them once more
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

OSMAND_NEON := true
include $(OSMAND_MAKEFILES)

endif