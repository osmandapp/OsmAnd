APP_STL := stlport_shared
APP_ABI := armeabi armeabi-v7a
APP_CPPFLAGS := -fno-rtti -fno-exceptions

ifndef OSMAND_DEBUG_NATIVE
# Force release compilation in release optimizations, even if application is debuggable by manifest
APP_OPTIM := release
endif