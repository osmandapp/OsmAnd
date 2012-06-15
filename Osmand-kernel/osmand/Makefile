# Include tools definitions
include ../Makefile.vars

# Include project files
LOCAL_PATH = .
include Common.mk

OSMAND_FLAGS = \
	-std=c++0x \
	-DGOOGLE_PROTOBUF_NO_RTTI \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_RELEASE \
	-DGR_RELEASE=1
LIBNAME = $(LIBRARY_PREFIX)osmand
LIBTYPE = $(DYNAMICLIB_EXT)
LDLIBS += -Wl,--whole-archive -lskia -Wl,--no-whole-archive -lproto -lpthread -lft2 -lexpat -lpng -lz 

ifndef RT_NOT_SUPPORTED
LDLIBS += -lrt
endif

ifeq ($(TARGET),nacl)
OSMAND_FLAGS += \
	-DRT_NOT_SUPPORTED
LOCAL_SRC_FILES += \
	src/osmand_nacl.cpp
else 
LOCAL_SRC_FILES += \
	src/java_wrap.cpp

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