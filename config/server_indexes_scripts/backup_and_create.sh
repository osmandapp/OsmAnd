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
cd ~/gen_indexes
rm osmand.log
rm console
# update map creator
rm -fr OsmAndMapCreator
mkdir -p OsmAndMapCreator
cd OsmAndMapCreator
wget http://download.osmand.net/latest-night-build/OsmAndMapCreator-development.zip
yes | unzip OsmAndMapCreator-development.zip
cd ..

# remove backup and create new backup
rm -rf backup
mkdir backup
# Not space enough - so that's commented right now
mv indexes/uploaded/*.* backup

# remove all previous files
rm -rf indexes
mkdir indexes
mkdir indexes/osm
mkdir indexes/uploaded

#run batch creator
./batch_indexing.sh &> console &
