#!/bin/sh
DIRECTORY=$(cd `dirname $0` && pwd)

GIT_DIR="$DIRECTORY"/osmand-git
GIT_URL=git://github.com/osmandapp/Osmand.git


if [ ! -d "$GIT_DIR" ]; then
    git clone ${GIT_URL} "${GIT_DIR}"
fi
cd "$GIT_DIR"
git checkout -q .
git reset HEAD --hard
git checkout -q master

# First time add entries to .git/config (!) and add .netrc file
# [remote "google"]
#	url = https://code.google.com/p/osmand/
#       fetch = +refs/heads/*:refs/remotes/google/*
git push -q --all --force google

echo "Synchronization is ok"

