include Common.mk
LIBNAME := libproto.a
PREBUILT_DIR = ../jni-prebuilt/linux-x86/
OBJECTS = $(LOCAL_SRC_FILES:%.cc=build/obj/%.o)
CPPFLAGS := -DGOOGLE_PROTOBUF_NO_RTTI -I.
LDLIBS = -lpthread

target : $(PREBUILT_DIR)/$(LIBNAME)

$(PREBUILT_DIR)/$(LIBNAME) : build/$(LIBNAME) 
	cp build/$(LIBNAME) $(PREBUILT_DIR)/$(LIBNAME) 

build/$(LIBNAME): $(OBJECTS) 
	ar -rf build/$(LIBNAME) $(OBJECTS)
	
build/obj/%.o :
	@mkdir -p `dirname $@`
	$(CXX) -o $@ -c $*.cc $(CPPFLAGS)

clean:
	$(RM) $(OBJECTS) build/$(LIBNAME)