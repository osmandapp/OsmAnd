THIS_MAKE:=$(abspath $(lastword $(MAKEFILE_LIST)))
LIBNAME:=osmand
TARGET:=nacl
include ../Makefile.vars


LOCAL_PATH=.
include Common.mk
LOCAL_SRC_FILES += \
	src/osmand_nacl.c


LDFLAGS:=-lppapi -expat -protobuf

# Default target is everything
all : build-release/$(LIBNAME)_$(TARGET)_32.nexe build-release/$(LIBNAME)_$(TARGET)_64.nexe build-release/$(LIBNAME).nmf

# Compilation
include ../Makefile.rules
build-release/$(LIBNAME)_$(TARGET)_32.nexe : $(addprefix build-release/obj-$(TARGET)-32/,$(OBJECTS_NAMES))
	$(CXX) -o $@ $^ -m32 $(CXXFLAGS) $(LDFLAGS)
	
build-release/$(LIBNAME)_x86_64.nexe : $(addprefix build-release/obj-$(TARGET)-64/,$(OBJECTS_NAMES))
	$(CXX) -o $@ $^ -m64 $(CXXFLAGS) $(LDFLAGS)

	
# NMF Manifiest generation
#
# Use the python script create_nmf to scan the binaries for dependencies using
# objdump.  Pass in the (-L) paths to the default library toolchains so that we
# can find those libraries and have it automatically copy the files (-s) to
# the target directory for us.
NMF:=python $(NACL_SDK_ROOT)/tools/create_nmf.py
NMF_ARGS:=-D $(TC_PATH)/x86_64-nacl/bin/objdump
NMF_PATHS:=-L $(TC_PATH)/x86_64-nacl/lib32 -L $(TC_PATH)/x86_64-nacl/lib

build-release/$(PROJECT).nmf : build-release/$(PROJECT)_x86_64.nexe build-release/$(PROJECT)_x86_32.nexe
	echo $(NMF) $(NMF_ARGS) -s . -o $@ $(NMF_PATHS) $^
	$(NMF) $(NMF_ARGS) -s . -o $@ $(NMF_PATHS) $^

# Define a phony rule so it always runs, to build nexe and start up server.
.PHONY: RUN 
RUN: all
	python  httpd.py 5100 &
