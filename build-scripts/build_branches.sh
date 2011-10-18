#!/bin/sh
DIRECTORY=$(cd `dirname $0` && pwd)

BRANCHES_CHANGED=0
GIT_DIR="$DIRECTORY"/osmand-git
GIT_ORIGIN_NAME=origin
BUILD_DIR="$DIRECTORY"/builds
LATESTS_DIR="$DIRECTORY"/latests
VERSION_FILE=./DataExtractionOSM/src/net/osmand/Version.java 
DATE=$(date +%Y-%m-%d)
SHORT_DATE=$(date +%d-%m)

# clean all files in build directory
rm -r "$BUILD_DIR"
mkdir "$BUILD_DIR"
rm -r "$LATESTS_DIR"
mkdir "$LATESTS_DIR"
cd "${GIT_DIR}"

# clear old branches
git remote prune origin
 
git branch -r | grep origin | while read i 
do
  cd "${GIT_DIR}"
  ch=$(expr index "$i" ">")
  if [ $ch = 0 ]; then 
     BRANCH=${i#"$GIT_ORIGIN_NAME/"}

     #clear success status
     # what is it?
     rm -f "$DIRECTORY/$BRANCH.fixed"
     mv -f "$DIRECTORY/$BRANCH.failed" "$DIRECTORY/$BRANCH.stillfailing" > /dev/null

     git diff --exit-code "$BRANCH" "$GIT_ORIGIN_NAME/$BRANCH" --quiet
     RES_DIFF=$?	  
     if [ $RES_DIFF != 0 ]; then
        echo "Checkouting branch and create build for $BRANCH"
        ## reset all previous changes in working tree
        BRANCHES_CHANGED=1
	BUILD_LOG="$DIRECTORY"/build.log
	echo "Branch:" $BRANCH > $BUILD_LOG
        git checkout . 
        git reset HEAD --hard
        git checkout -f $BRANCH
        git reset $GIT_ORIGIN_NAME/$BRANCH --hard
	sed -e "s/\(APP_DESCRIPTION.*=.*\"\).*\(\".*\)/\1$SHORT_DATE $BRANCH\2/g" $VERSION_FILE >  ${VERSION_FILE}.bak
        mv ${VERSION_FILE}.bak ${VERSION_FILE}

        ## build map creator
        cd ./DataExtractionOSM/
        ant clean compile build -logfile $BUILD_LOG
 	if [ "$BRANCH" = "release" ]; then
           cp build.zip "$LATESTS_DIR/OsmAndMapCreator-stable.zip"
        elif [ "$BRANCH" = "master" ]; then
           cp build.zip "$LATESTS_DIR/OsmAndMapCreator-development.zip"
        fi
        mv build.zip "$BUILD_DIR/OsmAndMapCreator-$BRANCH-nb-$DATE.zip"

        ## build osmand app
        cd ../OsmAnd/
        cp "$DIRECTORY"/local.properties local.properties
        rm -r bin
        mkdir bin
        if [ ! -d assets ]; then
          mkdir assets
        fi
        ant clean debug -logfile $BUILD_LOG

	if [ "$BRANCH" = "release" ]; then
           cp bin/OsmAnd-debug.apk "$LATESTS_DIR/OsmAnd-stable.apk"
        elif [ "$BRANCH" = "master" ]; then
           cp bin/OsmAnd-debug.apk "$LATESTS_DIR/OsmAnd-development.apk"
        fi
        mv bin/OsmAnd-debug.apk "$BUILD_DIR/OsmAnd-$BRANCH-nb-$DATE.apk"

        #put the log to std out
	BUILD=`grep FAILED $DIRECTORY/build.log | wc -l`
	if [ ! $BUILD -eq 0 ]; then
	  mv -f "$DIRECTORY/$BRANCH.stillfailing" "$DIRECTORY/$BRANCH.failed" > /dev/null
	  java -version
	  cat $BUILD_LOG
	  touch "$DIRECTORY/$BRANCH.failed"
	else
	  if [ -f "$DIRECTORY/$BRANCH.stillfailing" ]; then
	     echo "Build fixed"
	     rm -f $DIRECTORY/$BRANCH.failed
	     rm -f $DIRECTORY/$BRANCH.stillfailing
	     touch $DIRECTORY/$BRANCH.fixed
	  fi
	fi
     fi
  fi
done

exit $BRANCHES_CHANGED
