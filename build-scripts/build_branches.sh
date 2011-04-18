#!/bin/sh
DIRECTORY=$(cd `dirname $0` && pwd)

GIT_DIR="$DIRECTORY"/osmand-git
GIT_ORIGIN_NAME=origin
BUILD_DIR="$DIRECTORY"/builds
VERSION_FILE=./DataExtractionOSM/src/net/osmand/Version.java 
DATE=$(date +%d-%m-%y)

# clean all files in build directory
rm -r "$BUILD_DIR"
mkdir "$BUILD_DIR"
cd "${GIT_DIR}"
 
git branch -r | while read i 
do
  cd "${GIT_DIR}"
  ch=$(expr index "$i" ">")
  if [ $ch = 0 ]; then 
     BRANCH=${i#"$GIT_ORIGIN_NAME/"}
     
     echo "Checking if there are changes : $BRANCH - $GIT_ORIGIN_NAME/$BRANCH"
	  
	  git diff --exit-code "$BRANCH" "$GIT_ORIGIN_NAME/$BRANCH" --quiet
	  RES_DIFF=$?	  
	  if [ $RES_DIFF != 0 ]; then
	     echo "Checkouting branch and create build for $BRANCH"
		  ## reset all previous changes in working tree
        git checkout . 
		  git reset HEAD --hard
        git checkout $BRANCH		  
		  git merge $GIT_ORIGIN_NAME/$BRANCH
		  
		  sed -e "s/\(APP_DESCRIPTION.*=.*\"\).*\(\".*\)/\1$DATE $BRANCH\2/g" $VERSION_FILE >  ${VERSION_FILE}.bak
	     mv ${VERSION_FILE}.bak ${VERSION_FILE}

        ## build map creator
        cd ./DataExtractionOSM/
        ant clean compile build
        mv build.zip "$BUILD_DIR/OsmAndMapCreator-$BRANCH-nb-$DATE.zip"

        ## build osmand app
        cd ../OsmAnd/
        cp "$DIRECTORY"/local.properties local.properties
        rm -r bin
        mkdir bin
        if [ ! -d assets ]; then
          mkdir assets
        fi
        ant clean debug
        mv bin/OsmAnd-debug.apk "$BUILD_DIR/OsmAnd-$BRANCH-nb-$DATE.apk" 
	  fi
  fi
done
