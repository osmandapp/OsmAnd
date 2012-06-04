# Include tools definitions
include ../Makefile.vars

# Include project files
LOCAL_PATH = .
include Common.mk

OSMAND_FLAGS = \
	-DGOOGLE_PROTOBUF_NO_RTTI \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_RELEASE \
	-DGR_RELEASE=1 \
	-DHASH_MAP_GNU
LIBNAME = $(LIBRARY_PREFIX)osmand
LIBTYPE = $(DYNAMICLIB_EXT)
LDLIBS += -lskia -lproto -lpthread -lft2 -lexpat -lpng -lz

ifeq ($(TARGET),nacl)
OSMAND_FLAGS += \
	-DRT_NOT_SUPPORTED
LOCAL_SRC_FILES += \
	src/osmand_nacl.cpp
else 
LDLIBS += -lrt
LOCAL_SRC_FILES += \
	src/java_wrap.cpp
# Set library name


ifeq ($(findstring CYGWIN,$(shell uname)),CYGWIN)
	FIXED_JAVA_HOME := $(shell cygpath -u "$(JAVA_HOME)")
else
	FIXED_JAVA_HOME := $(JAVA_HOME)
endif
ifeq ($(JAVA_OS),)
	ifeq ($(findstring CYGWIN,$(shell uname)),CYGWIN)
	   JAVA_OS := win32
	endif
	ifeq ($(findstring MINGW,$(shell uname)),MINGW)
	   JAVA_OS := win32
	endif
endif
ifeq ($(JAVA_OS),)
	JAVA_OS := linux
endif
OSMAND_FLAGS += -c -I"$(FIXED_JAVA_HOME)/include" -I"$(FIXED_JAVA_HOME)/include/$(JAVA_OS)"
endif
	
CFLAGS += $(OSMAND_FLAGS)
CXXFLAGS += $(OSMAND_FLAGS)

# Finally, include generic rules
include ../Makefile.rules