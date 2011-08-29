# This file will start the Osm Extraction UI with custom memory settings for
# the JVM. With the below settings the heap size (Available memory for the application)
# will range from 64 megabyte up to 512 megabyte.

#/usr/lib/jvm/java-6-sun/jre/bin/
if [ -n "`ps auxw | grep batch_server.xml | grep IndexBatchCreator | grep -v grep`" ]; then
  echo "The gen_index process is already running!"
  echo "`ps auxw | grep batch_server.xml | grep IndexBatchCreator | grep -v grep`"
  exit 1
fi
if [ -n "`ps auxw | grep batch_server_upload.xml | grep IndexBatchCreator | grep -v grep`" ]; then
  echo "The upload process is already running!"
  echo "`ps auxw | grep batch_server_upload.xml | grep IndexBatchCreator | grep -v grep`"
  exit 1
fi
if [ -z "$JAVA_HOME" ] ; then
   export JAVA_HOME=/usr/lib/jvm/java-6-openjdk/
fi
"$JAVA_HOME/bin/java" -Djava.util.logging.config.file=logging.properties -XX:+UseParallelGC -Xmx2048M -cp "OsmAndMapCreator/OsmAndMapCreator.jar:OsmAndMapCreator/lib/*.jar" net.osmand.data.index.IndexBatchCreator ./batch_server.xml 
#./batch_uploading.sh > console.upload
