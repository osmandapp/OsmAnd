LOCAL_PATH := .
include ../Makefile.vars
include ./Common.mk
PREBUILT_DIR = ../jni-prebuilt/$(os)-$(arch)
LIBNAME := osmand.lib
LIBOSNAME := osmand-$(os)-$(arch).lib

RUNFILE := osmand_main

LOCAL_SRC_FILES += src/osmand_main.cpp

#LDFLAGS = -Wl,--hash-style=both -fPIC
LDFLAGS = -Wall -fPIC -Wl,-Bsymbolic
#LDRUNFLAGS = -Wl,--hash-style=both
#  --trace -Bsymbolic -Bsymbolic-functions

LDLIBS = -L$(PREBUILT_DIR) -lskia -lproto -lpthread -lrt -lft2 -lexpat -lpng -lz 
CPP_FILE_EXTENSION = cpp
OBJECTS = $(LOCAL_SRC_FILES:src/%.$(CPP_FILE_EXTENSION)=build/obj-$(os)-$(arch)/%.o)

C_INCLUDES := $(addprefix -I, $(LOCAL_C_INCLUDES))

ifeq ($(findstring CYGWIN,$(shell uname)),CYGWIN)
	FIXED_JAVA_HOME := $(shell cygpath -u "$(JAVA_HOME)")
else
	FIXED_JAVA_HOME := $(JAVA_HOME)
endif

ifeq ($(java_os),)
	ifeq ($(findstring CYGWIN,$(shell uname)),CYGWIN)
		java_os := linux
	endif
	ifeq ($(findstring MINGW,$(shell uname)),MINGW)
		java_os := win32
	endif
endif
ifeq ($(os),)
	java_os := linux
endif

CPPFLAGS := \
	-DGOOGLE_PROTOBUF_NO_RTTI \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_RELEASE \
	-DGR_RELEASE=1 \
	-c -fPIC -I"$(FIXED_JAVA_HOME)/include" -I"$(FIXED_JAVA_HOME)/include/$(java_os)" \
	-DHASH_MAP_GNU \
	$(C_INCLUDES)
	
#target : ../$(RUNFILE)	
#target : $(PREBUILT_DIR)/$(LIBNAME)
target : $(PREBUILT_DIR)/$(LIBNAME) ../$(RUNFILE)

../$(RUNFILE) : $(OBJECTS) $(PREBUILT_DIR)/libskia.a $(PREBUILT_DIR)/libproto.a Makefile
	$(CXX) $(LDRUNFLAGS) -o ../$(RUNFILE)  $(OBJECTS) $(LDLIBS)
	@chmod +x ../$(RUNFILE)

$(PREBUILT_DIR)/$(LIBNAME) : build/$(LIBOSNAME)
	mkdir -p $(PREBUILT_DIR)
	cp build/$(LIBOSNAME) $(PREBUILT_DIR)

build/$(LIBOSNAME): $(OBJECTS) $(PREBUILT_DIR)/libskia.a $(PREBUILT_DIR)/libproto.a Makefile
	$(CXX) -shared -o build/$(LIBOSNAME) $(OBJECTS) $(LDFLAGS) $(LDLIBS)
	
build/obj-$(os)-$(arch)/%.o : src/%.$(CPP_FILE_EXTENSION) $(LOCAL_C_INCLUDES) src/textdraw.cpp
	@mkdir -p `dirname $@`
	$(CXX) -o $@ -c src/$*.$(CPP_FILE_EXTENSION) $(CPPFLAGS)
	
clean:
	$(RM) $(OBJECTS) build/$(LIBNAME)