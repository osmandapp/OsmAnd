#!/bin/sh
DIRECTORY=$(cd `dirname $0` && pwd)

## VARIABLES ###
LOG_DIR="$DIRECTORY"/logs
DATE=$(date +%d-%m-%y)
CLOG_FILE="$LOG_DIR/${DATE}.log"
LOG_FILE="$LOG_DIR/tmp.log"

mkdir -p $LOG_DIR
echo > $LOG_FILE
touch  $CLOG_FILE

cd "${DIRECTORY}"
git pull --rebase 2>&1 >>$LOG_FILE

# 1. Update git directory
"${DIRECTORY}/update_git.sh" >>$LOG_FILE 2>&1

# 2. Go through branches and generates builds
"${DIRECTORY}/build_branches.sh" >>$LOG_FILE 2>&1

# exit if nothing was changed
 if [ $? == 0 ]; then
   exit 0
 fi

# 3. upload to ftp server 
#"${DIRECTORY}/upload_ftp.sh" 2>&1 >>$LOG_FILE

# 3. upload to ftp server 
"${DIRECTORY}/copyto_dir.sh" >>$LOG_FILE 2>&1

# 4. Synchronize github with googlecode mercurial
"${DIRECTORY}/sync_git_google.sh" >>$LOG_FILE 2>&1

# 5. update site files 
"${DIRECTORY}/update_site.sh" >>$LOG_FILE 2>&1

cat $LOG_FILE >> $CLOG_FILE
BUILD=`ls *.fixed *.failed 2> /dev/null | wc -l`
if [ ! $BUILD -eq 0 ]; then
  # if some status change, print out complete log
  echo "Builds status changed"
  echo "-------------"
  echo `ls *.fixed *.failed`
  echo "-------------"
  echo "Complete log file:"
  echo "-------------"
  cat $LOG_FILE
fi
