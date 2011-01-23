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
hg push "${HG_URL}"

echo "Synchronization is ok"

