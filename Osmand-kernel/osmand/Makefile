default : all

JAVA  := $$JAVA_HOME/bin/java
JAVAC := $$JAVA_HOME/bin/javac
CXX       := g++
STRIP     := strip
LIBNAME := osmand.lib

#LOCAL_PATH := $(call my-dir)
LOCAL_PATH := .
PROTOBUF := $(LOCAL_PATH)/../protobuf
OSMAND_SKIA_ABS := $(LOCAL_PATH)/../skia/skia_library

LOCAL_C_INCLUDES := -I$(LOCAL_PATH) \
    -I$(PROTOBUF) \
	-I$(LOCAL_PATH)/../skia \
	-I$(OSMAND_SKIA_ABS)/include/core \
	-I$(OSMAND_SKIA_ABS)/include/images \
	-I$(OSMAND_SKIA_ABS)/include/utils \
	-I$(OSMAND_SKIA_ABS)/include/config \
	-I$(OSMAND_SKIA_ABS)/include/effects \
	-I$(OSMAND_SKIA_ABS)/include/utils/android \
	-I$(OSMAND_SKIA_ABS)/src/core
	
#For hash_map compilation -Wno-deprecated

CPPFLAGS := \
	-DGOOGLE_PROTOBUF_NO_RTTI \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_RELEASE \
	-DGR_RELEASE=1 \
	-c -fpic -I$(JAVA_HOME)/include \
	-DLINUX_BUILD \
	$(LOCAL_C_INCLUDES)
	
# LDFLAGS contains flags passed to the compiler for use during linking
LDFLAGS = -Wl,--hash-style=both
# LIBS contains libraries to link with
LDLIBS = -L/home/victor/projects/OsmAnd/git/Osmand-kernel/jni-prebuilt/x86/ -lskia -lproto -lpng -ljpeg -lgif -lft2_static -lexpat_static -lrt -lm

TARGET = $(shell basename `pwd`)
SOURCES = osmand_log.cpp \
	common.cpp \
	mapObjects.cpp \
	renderRules.cpp \
	rendering.cpp \
	binaryRead.cpp
DEPS = $(wildcard *.h)
OBJECTS = $(SOURCES:%.cpp=%.o)

all: $(LIBNAME)

$(OBJECTS): $(SOURCES) $(DEPS)

$(LIBNAME): $(OBJECTS)
	$(CXX) $(LDFLAGS) -o $(LIBNAME)  $(OBJECTS) $(LDLIBS)
	$(STRIP) $(LIBNAME)
	
clean:
	$(RM) $(OBJECTS) $(TARGET) 