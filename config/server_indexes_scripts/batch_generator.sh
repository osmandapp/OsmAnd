cd ~/gen_indexes
rm osmand.log
rm console
# update map creator
cd OsmAndMapCreator
yes | unzip /var/www-download/latest-night-build/OsmAndMapCreator-development.zip
cd ..

# remove backup and create new backup
rm -r backup
mkdir backup
mv indexes/uploaded/*.* backup

# remove all previous files
rm -r indexes
mkdir indexes
mkdir indexes/osm
mkdir indexes/uploaded

#run batch creator
./batch_indexing.sh > /dev/null 2&>console &



