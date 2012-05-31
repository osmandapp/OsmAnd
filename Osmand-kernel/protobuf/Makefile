# Include tools definitions
include ../Makefile.vars

# Include project files
LOCAL_PATH = .
include Common.mk

# Set library name
LIBNAME = $(LIBRARY_PREFIX)proto
LIBTYPE = $(STATICLIB_EXT)
CXXFLAGS += -DGOOGLE_PROTOBUF_NO_RTTI
LDFLAGS += -lpthread
LOCAL_C_INCLUDES += .

# Finally, include generic rules
include ../Makefile.rules