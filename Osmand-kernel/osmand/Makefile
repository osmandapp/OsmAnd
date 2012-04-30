LOCAL_PATH := .
PREBUILT_DIR = ../jni-prebuilt/linux-x86/
LIBNAME := osmand.lib
include Common.mk

LDFLAGS = -Wl,--hash-style=both -shared
LDLIBS = -L$(PREBUILT_DIR) -lskia -lproto
CPP_FILE_EXTENSION = cpp
OBJECTS = $(LOCAL_SRC_FILES:src/%.$(CPP_FILE_EXTENSION)=build/obj/%.o)

C_INCLUDES := $(addprefix -I, $(LOCAL_C_INCLUDES))

CPPFLAGS := \
	-DGOOGLE_PROTOBUF_NO_RTTI \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_RELEASE \
	-DGR_RELEASE=1 \
	-c -fpic -I$(JAVA_HOME)/include \
	-DHAVING_HASH \
	$(C_INCLUDES)

$(PREBUILT_DIR)/$(LIBNAME) : build/$(LIBNAME) 
	cp build/$(LIBNAME) $(PREBUILT_DIR)/$(LIBNAME)


build/$(LIBNAME): $(OBJECTS) $(PREBUILT_DIR)/libskia.a $(PREBUILT_DIR)/libproto.a 
	$(CXX) $(LDFLAGS) -o build/$(LIBNAME)  $(OBJECTS) $(LDLIBS)
	strip build/$(LIBNAME)
	
build/obj/%.o : src/%.$(CPP_FILE_EXTENSION) $(LOCAL_C_INCLUDES)
	@mkdir -p `dirname $@`
	$(CXX) -o $@ -c src/$*.$(CPP_FILE_EXTENSION) $(CPPFLAGS)
	
clean:
	$(RM) $(OBJECTS) build/$(LIBNAME)