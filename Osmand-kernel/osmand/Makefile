# Include tools definitions
include ../Makefile.vars

# Include project files
LOCAL_PATH = .
include Common.mk

OSMAND_FLAGS = \
	-DGOOGLE_PROTOBUF_NO_RTTI \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_RELEASE \
	-DGR_RELEASE=1
OSMAND_FLAGS_WINDOWS = \
	-DSK_BUILD_FOR_WIN32 \
	-DMINGW_HAS_SECURE_API \
	-DSkUserConfig_DEFINED \
	-DPICTURE_VERSION_ICS=1 \
	-DPICTURE_VERSION_JB=2 \
	-DSK_SCALAR_IS_FLOAT \
	-DSK_CAN_USE_FLOAT \
	-DSK_CPU_LENDIAN
OSMAND_FLAGS_MAC = \
	-DSK_BUILD_FOR_MAC \
	-DSK_USE_CORETEXT \
	-DSkUserConfig_DEFINED \
	-DPICTURE_VERSION_ICS=1 \
	-DPICTURE_VERSION_JB=2 \
	-DSK_SCALAR_IS_FLOAT \
	-DSK_CAN_USE_FLOAT \
	-DSK_CPU_LENDIAN

LIBNAME = $(LIBRARY_PREFIX)osmand
LIBTYPE = $(DYNAMICLIB_EXT)
ifeq ($(TARGET),darwin)
	LDLIBS += --whole-archive -lskia --no-whole-archive -lproto -lft2 -lexpat -lpng -lz 
else
	LDLIBS += -Wl,--whole-archive -lskia -Wl,--no-whole-archive -lproto -lft2 -lexpat -lpng -lz 
endif

ifeq ($(TARGET),windows)
	LDLIBS += -lwinmm -lgdi32 -lusp10
	OSMAND_FLAGS += $(OSMAND_FLAGS_WINDOWS)
else ifeq ($(TARGET),darwin)
	OSMAND_FLAGS += $(OSMAND_FLAGS_MAC)
	LDLIBS += -framework CoreText -lpthread
else ifeq ($(TARGET),nacl)
	LDLIBS += -lpthread
else
	LDLIBS += -lrt -lpthread
endif

ifeq ($(TARGET),nacl)
	OSMAND_FLAGS += \
		-DRT_NOT_SUPPORTED
	LOCAL_SRC_FILES += \
		src/osmand_nacl.cpp
else ifeq ($(TARGET),darwin)
	LOCAL_SRC_FILES += \
		src/java_wrap.cpp
		
	OSMAND_FLAGS += -I"$(JAVA_HOME)"
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
