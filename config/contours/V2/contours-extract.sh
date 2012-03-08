#Extracts contours from a database built following the method here: 
# http://wiki.openstreetmap.org/wiki/Contours#The_PostGIS_approach
# using polygon files

LOGFILE="contours.log"
FAILED="failed.lst"
# update: previous version had problems loading long 
# wkt from the command line ('argument list too long')
# here the wkt is loaded from file in pgsql

base=$(basename $1) # get the basename without the path
bname=`echo "${base%.*}"` #remove the extension
./poly2wkt.pl polys/$bname.poly > /tmp/tmp.wkt
continent="europe"
echo 'processing '${bname}
# use the following line to create the table for working with geometries loaded from a wkt file
#echo "CREATE TABLE tmpgeom (wkt text, geom geometry);" | psql -U mapnik -d contour
# load wkt from file
echo "COPY tmpgeom (wkt) from '/tmp/tmp.wkt';" | psql -U mapnik -d contour
# create a geometry from the wkt text
echo "update tmpgeom set geom = st_geomFromText(wkt, -1);" | psql -U mapnik -d contour
# extract shapefile
pgsql2shp -f shp/${bname}.shp -u mapnik -P mapnik contour " \
SELECT ST_simplify(intersection(way, geom),0.00005),  \
height from contours , tmpgeom \
 where ST_Intersects(way, tmpgeom.geom); "
echo "DELETE FROM tmpgeom WHERE wkt is not null;" | psql -U mapnik -d contour

if [ $? -ne 0 ]
then
    echo $(date) ${bname}' shapefile failed'>> $LOGFILE
    echo ${bname}>> $FAILED
    exit 2
else
    echo $(date) ${bname}' shapefile OK'>> $LOGFILE
fi
rm osm/*
./srtmshp2osm.py -f shp/$bname -o osm/${bname}_${continent}_srtm_elevation_contour_lines.osm
if [ $? -ne 0 ]
then
    echo $(date) ${bname}' osm file failed'>> $LOGFILE
    echo ${bname}>> $FAILED
    exit 2
else
    echo $(date) ${bname}' osm file OK'>> $LOGFILE
fi
gzip -c osm/${bname}_${continent}_srtm_elevation_contour_lines.osm > osm-files/${bname}_${continent}_srtm_elevation_contour_lines.osm.gz

rm index/*
./batch_indexing.sh
if [ $? -ne 0 ]
then
    echo $(date) ${bname}' obf file failed'>> $LOGFILE
    echo ${bname}>> $FAILED
    exit 2
else
    echo $(date) ${bname}' obf file OK'>> $LOGFILE
fi
# capitalize first letter:
extractname=${bname}_${continent}_srtm_elevation_contour_lines_1.obf
for i in $extractname; do B=`echo -n "${i:0:1}" | tr "[a-z]" "[A-Z]"`; cap=`echo -n "${B}${i:1}" `; done
# zip with comment
echo "SRTM Elevation contour lines for ${name}" | zip -j index/${cap}.zip -z index/${cap}

scp index/${cap}.zip jenkins@osmand.net:/var/lib/jenkins/indexes/
if [ $? -ne 0 ]
then
    echo $(date) ${bname}' obf file sent to server failed'>> $LOGFILE
    echo ${bname}>> $FAILED
    exit 2
else
    echo $(date) ${bname}' obf file sent to server'>> $LOGFILE
fi



