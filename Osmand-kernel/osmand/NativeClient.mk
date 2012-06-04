THIS_MAKE:=$(abspath $(lastword $(MAKEFILE_LIST)))
LIBNAME:=osmand

include ../Makefile.vars

SRC_LIBRARY_x64 = ../lib/nacl-x64-release/libosmand.nexe
SRC_LIBRARY_x86 = ../lib/nacl-x86-release/libosmand.nexe


LIBRARY_x64 = nacl/osmand-x64.nexe
LIBRARY_x86 = nacl/osmand-x86.nexe

compile : all libraries

libraries :	$(LIBRARY_x64) $(LIBRARY_x86)

all : x86 x64
x86:
	make -f Makefile ARCH=x86 TARGET=nacl
	
x64:
	make -f Makefile ARCH=x64 TARGET=nacl
    
$(LIBRARY_x86) : $(SRC_LIBRARY_x86)
	cp $(SRC_LIBRARY_x86) $(LIBRARY_x86)

$(LIBRARY_x64) : $(SRC_LIBRARY_x64)
	cp $(SRC_LIBRARY_x64) $(LIBRARY_x64)
	
#NMF_FILE = $(LIBNAME).nmf	
## NMF Manifiest generation
##
## Use the python script create_nmf to scan the binaries for dependencies using
## objdump.  Pass in the (-L) paths to the default library toolchains so that we
## can find those libraries and have it automatically copy the files (-s) to
## the target directory for us.
#NMF:=python $(NACL_SDK_ROOT)/tools/create_nmf.py
#NMF_ARGS:=-D $(TC_PATH)/x86_64-nacl/bin/objdump
#NMF_PATHS:=-L $(TC_PATH)/x86_64-nacl/lib32 -L $(TC_PATH)/x86_64-nacl/lib
#
#$(NMF_FILE) : $(LIBRARY_x64) $(LIBRARY_x86)
#	$(NMF) $(NMF_ARGS) -s . -o $@ $(NMF_PATHS) $^

# Define a phony rule so it always runs, to build nexe and start up server.
.PHONY: run 
run : all libraries
	python  httpd.py 5100 &
	
stop :
	wget -q -O - 'http://localhost:5100?quit=1' > /dev/null