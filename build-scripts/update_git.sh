#!/bin/sh
DIRECTORY=$(cd `dirname $0` && pwd)

GIT_DIR="$DIRECTORY"/osmand-git
GIT_URL=git://github.com/osmandapp/Osmand.git
GIT_ORIGIN_NAME=origin

#rm -rf "${GIT_DIR}"
# initialize git if it is not present (clone it)
if [ ! -d "$GIT_DIR" ]; then
    git clone ${GIT_URL} "${GIT_DIR}"
fi
# update git
cd "${GIT_DIR}"
git reset --hard
git checkout -q master
git fetch ${GIT_ORIGIN_NAME}
