LOCAL_PATH = .
include Common.mk

PREBUILT_DIR = ../jni-prebuilt/linux-x86/
LIBNAME := libskia.a

CPP_FILE_EXTENSION = cpp
LOCAL_SRC_FILES += \
	$(OSMAND_SKIA_LOC)/src/opts/SkBlitRow_opts_none.cpp \
	$(OSMAND_SKIA_LOC)/src/opts/SkBitmapProcState_opts_none.cpp \
	$(OSMAND_SKIA_LOC)/src/opts/SkUtils_opts_none.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkThread_pthread.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkDebug_stdio.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_FreeType.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_linux.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkOSFile_stdio.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkTime_Unix.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkMemory_malloc.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkFontHost_gamma.cpp \
	$(OSMAND_SKIA_LOC)/src/ports/SkGlobalInitialization_default.cpp
	
OBJECTS = $(LOCAL_SRC_FILES:%.$(CPP_FILE_EXTENSION)=build/obj/%.o)
C_INCLUDES := $(addprefix -I, $(LOCAL_C_INCLUDES))

CPPFLAGS := -DSK_RELEASE \
	-DGR_RELEASE=1 \
	-DSK_ENABLE_LIBPNG \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DNDEBUG \
	-fno-rtti \
	-fno-exceptions \
	$(C_INCLUDES)
	
LDLIBS = -lpthread

target : $(PREBUILT_DIR)/$(LIBNAME)

$(PREBUILT_DIR)/$(LIBNAME) : build/$(LIBNAME) 
	cp build/$(LIBNAME) $(PREBUILT_DIR)/$(LIBNAME)

build/$(LIBNAME): $(OBJECTS) 
	ar -rf build/$(LIBNAME) $(OBJECTS)
	cp build/$(LIBNAME) ../jni-prebuilt/linux-x86/$(LIBNAME)
	
build/obj/%.o : %.$(CPP_FILE_EXTENSION) $(LOCAL_C_INCLUDES)
	@mkdir -p `dirname $@`
	$(CXX) -o $@ -c $*.$(CPP_FILE_EXTENSION) $(CPPFLAGS)

clean:
	$(RM) $(OBJECTS) build/$(LIBNAME)