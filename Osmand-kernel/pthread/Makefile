# Include tools definitions
include ../Makefile.vars

# Include project files
LOCAL_PATH = .
OSMAND_PTHREAD_LOC := ./pthreads_library

OSMAND_PTHREAD_ABS := $(LOCAL_PATH)/pthreads_library

LOCAL_SRC_FILES:= \
	$(OSMAND_PTHREAD_LOC)/attr.c \
	$(OSMAND_PTHREAD_LOC)/barrier.c \
	$(OSMAND_PTHREAD_LOC)/cancel.c \
	$(OSMAND_PTHREAD_LOC)/cleanup.c \
	$(OSMAND_PTHREAD_LOC)/condvar.c \
	$(OSMAND_PTHREAD_LOC)/create.c \
	$(OSMAND_PTHREAD_LOC)/dll.c \
	$(OSMAND_PTHREAD_LOC)/exit.c \
	$(OSMAND_PTHREAD_LOC)/fork.c \
	$(OSMAND_PTHREAD_LOC)/global.c \
	$(OSMAND_PTHREAD_LOC)/misc.c \
	$(OSMAND_PTHREAD_LOC)/mutex.c \
	$(OSMAND_PTHREAD_LOC)/private.c \
	$(OSMAND_PTHREAD_LOC)/rwlock.c \
	$(OSMAND_PTHREAD_LOC)/sched.c \
	$(OSMAND_PTHREAD_LOC)/semaphore.c \
	$(OSMAND_PTHREAD_LOC)/spin.c \
	$(OSMAND_PTHREAD_LOC)/sync.c \
	$(OSMAND_PTHREAD_LOC)/tsd.c \
	$(OSMAND_PTHREAD_LOC)/nonportable.c


# Set library name
LIBNAME = $(LIBRARY_PREFIX)pthread
LIBTYPE = $(STATICLIB_EXT)

# Finally, include generic rules
ifdef PTHREAD_BUILD
include ../Makefile.rules
endif