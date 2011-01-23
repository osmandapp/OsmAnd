#!/bin/sh
DIRECTORY=$(cd `dirname $0` && pwd)

FTP_SITE=download.osmand.net
FTP_FOLDER=night-builds
# FTP_USER in local.properties
# FTP_PWD= in local.properties
BUILD_DIR="$DIRECTORY"/builds

. "$DIRECTORY"/local.properties
# 3. upload to ftp server
lftp -c set net:timeout=45 || open $FTP_USER,$FTP_PWD $FTP_SITE || ls $FTP_FOLDER || mirror -R $FTP_FOLDER $BUILD_DIR

#ftp -n -v $FTP_SITE <<SCRIPT 2>&1
#quote USER $FTP_USER
#quote PASS $FTP_PWD
#cd $FTP_FOLDER 
#ls
#quit



