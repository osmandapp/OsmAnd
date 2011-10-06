#!/bin/sh
DIRECTORY=$(cd `dirname $0` && pwd)

FTP_SITE=download.osmand.net
FTP_FOLDER=/var/www-download/night-builds
FTP_LATEST=/var/www-download/latest-night-build
# FTP_USER in local.properties
# FTP_PWD= in local.properties
BUILD_DIR="$DIRECTORY"/builds
LATEST_DIR="$DIRECTORY"/latests

#. "$DIRECTORY"/local.properties
# 3. upload to ftp server
#lftp -c "set net:timeout 45;open -u $FTP_USER,$FTP_PWD $FTP_SITE;mirror -R $BUILD_DIR $FTP_FOLDER;mirror -R $LATEST_DIR $FTP_LATEST"
#rm $FTP_FOLDER/*
#rm $FTP_LATEST/*
cp -f $BUILD_DIR/* $FTP_FOLDER 2> /dev/null
cp -f $LATEST_DIR/* $FTP_LATEST 2> /dev/null


#ftp -n -v $FTP_SITE <<SCRIPT 2>&1
#quote USER $FTP_USER
#quote PASS $FTP_PWD
#cd $FTP_FOLDER 
#ls
#quit



