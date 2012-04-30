include Common.mk
LIBNAME := build/libproto.a
OBJECTS = $(LOCAL_SRC_FILES:%.cc=build/obj/%.o)
CPPFLAGS := -DGOOGLE_PROTOBUF_NO_RTTI -I.
LDLIBS = -lpthread
LDFLAGS = -shared

$(LIBNAME): $(OBJECTS) 
	ar -rf $(LIBNAME) $(OBJECTS)
	
build/obj/%.o :
	@mkdir -p `dirname $@`
	$(CXX) -o $@ -c $*.cc $(CPPFLAGS)

clean:
	$(RM) $(OBJECTS)