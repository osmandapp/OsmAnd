# remove backup and create new backup
# we should not rm, just do incremental updates for now! rm -rf backup 
# TODO think about it
# mkdir -p ~/backup
# mv ~/indexes/uploaded/*.* backup

# remove all previous files
mkdir ~/indexes
mkdir ~/indexes/uploaded

rm -rf .work
mkdir .work
mkdir .work/osm
java -Djava.util.logging.config.file=logging.properties -Xms128M -Xmx1512M -cp "DataExtractionOSM/OsmAndMapCreator.jar:DataExtractionOSM/lib/*.jar" net.osmand.data.index.IndexBatchCreator build-scripts/indexes-batch-generate.xml build-scripts/indexes-regions.xml
