# remove backup and create new backup
# we should not rm, just do incremental updates for now! rm -rf backup 

# remove all previous files
mkdir ~/indexes
mkdir ~/indexes/uploaded

rm -rf .work
mkdir .work
mkdir .work/osm
if [ -z $INDEXES_FILE ]; then INDEXES_FILE="build-scripts/regions/indexes.xml"; echo "$INDEXES_FILE"; fi

echo 'Running java net.osmand.data.index.IndexBatchCreator with $INDEXES_FILE'
java -XX:+UseParallelGC -Xmx2048M -Xmn256M -Djava.util.logging.config.file=build-scripts/batch-logging.properties -cp "DataExtractionOSM/OsmAndMapCreator.jar:DataExtractionOSM/lib/*.jar" net.osmand.data.index.IndexBatchCreator build-scripts/indexes-batch-generate.xml "$INDEXES_FILE"
