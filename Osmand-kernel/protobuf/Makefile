include ../Makefile.vars
include Common.mk

CPP_FILE_EXTENSION = cc
LIBNAME := libproto.a
PREBUILT_DIR = ../jni-prebuilt/$(os)-$(arch)
OBJECTS = $(LOCAL_SRC_FILES:%.$(CPP_FILE_EXTENSION)=build/obj-$(os)-$(arch)/%.o)
CPPFLAGS := -DGOOGLE_PROTOBUF_NO_RTTI -I. -fPIC
LDLIBS = -lpthread

target : $(PREBUILT_DIR)/$(LIBNAME)

$(PREBUILT_DIR)/$(LIBNAME) : build/$(LIBNAME) 
	mkdir -p $(PREBUILT_DIR)
	cp build/$(LIBNAME) $(PREBUILT_DIR)

build/$(LIBNAME): $(OBJECTS) 
	ar -rf build/$(LIBNAME) $(OBJECTS)
	
build/obj-$(os)-$(arch)/%.o : %.$(CPP_FILE_EXTENSION) $(LOCAL_C_INCLUDES)
	@mkdir -p `dirname $@`
	$(CXX) -o $@ -c $*.$(CPP_FILE_EXTENSION) $(CPPFLAGS)

clean:
	$(RM) $(OBJECTS) build/$(LIBNAME)