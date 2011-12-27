# remove backup and create new backup
# we should not rm, just do incremental updates for now! rm -rf backup 
mkdir -p ~/backup
mv ~/indexes/uploaded/*.* backup

# remove all previous files
rm -rf ~/indexes
mkdir ~/indexes
mkdir ~/indexes/uploaded
mkdir ~/indexes/osm

java -Djava.util.logging.config.file=logging.properties -Xms64M -Xmx720M -cp "DataExtractionOSM/OsmAndMapCreator.jar:DataExtractionOSM/lib/*.jar" net.osmand.data.index.IndexBatchCreator build-scripts/indexes-batch-generate.xml build-scripts/indexes-regions.xml
