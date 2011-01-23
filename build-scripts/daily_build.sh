#!/bin/sh
DIRECTORY=$(cd `dirname $0` && pwd)

## VARIABLES ###
LOG_DIR="$DIRECTORY"/logs
DATE=$(date +%d-%m-%y)
LOG_FILE="$LOG_DIR/${DATE}.log"


mkdir $LOG_DIR
# check build already run today
if [ -e "$LOG_FILE" ]; then
    echo "Build already ran today"
    exit
fi
touch $LOG_FILE


# 1. Update git directory
"${DIRECTORY}/update_git.sh" 2>&1 >>$LOG_FILE

# 2. Go through branches and generates builds
#"${DIRECTORY}/build_branches.sh" 2>&1 >>$LOG_FILE

# 3. upload to ftp server
#"${DIRECTORY}/upload_ftp.sh" 2>&1 >>$LOG_FILE

# 4. Synchronize github with googlecode mercurial
"${DIRECTORY}/sync_git_hg.sh" 2>&1 >>$LOG_FILE


