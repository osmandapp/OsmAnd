#!/bin/sh
DIRECTORY=$(cd `dirname $0` && pwd)

GIT_DIR="$DIRECTORY"/osmand-git
GIT_URL=git://github.com/osmandapp/Osmand.git
GIT_ORIGIN_NAME=origin
HG_DIR="$DIRECTORY"/osmand-hg
HG_URL=https://osmand.googlecode.com/hg
BUILD_DIR="$DIRECTORY"/builds

if [ ! -d "$HG_DIR" ]; then
    hg clone ${GIT_URL} "${HG_DIR}"
fi
cd "${HG_DIR}"
hg pull "${GIT_URL}"
hg update -c
# First time add entries to .hgrc (!)
# [paths]
# default = git://github.com/osmandapp/Osmand.git
# googlecode = https://username:password@osmand.googlecode.com/hg
hg push googlecode

echo "Synchronization is ok"

