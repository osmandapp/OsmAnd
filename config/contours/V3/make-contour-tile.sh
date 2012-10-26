#!/bin/bash 
# usage make-contour-tile.sh input-file output-directory

# directory for python tools
TOOL_DIR="."
# temporary directory
TMP_DIR="/var/tmp/"

# ---------------------------------------------
# check arguments and build absolute filenames
# ---------------------------------------------

if [ $# -lt 2 ]; then
  echo "Error: 2 arguments needed"
  echo "Usage: "$(basename $0) "[input-file] [output-directory/]"
  exit 2
fi
if [ $# -eq  3 ]; then
  tmpfile="tmp"$3
else
  tmpfile="tmp"
fi
if [ ! -f $1 ]; then
  echo "input file not found"
  exit 3
fi
if [ ! -d $2 ]; then
  echo "output directory not found"
  exit 3
fi
working_dir=$(pwd)
inpath=$(dirname $1)
cd $inpath
filename=$(basename $1)
infile=$(pwd)/$filename
fileroot=${filename%.*}

cd $working_dir
cd $2
outdir=$(pwd)/
outfile=$outdir$fileroot.osm


# ---------------------------------------------
# process
# ---------------------------------------------
#if [ ! -f $outfile.bz2 ]
#then
	echo "----------------------------------------------"
	echo "Processing"$fileroot
	echo "----------------------------------------------"
	echo "Extracting shapefile …"
	if [ -f ${TMP_DIR}${tmpfile}.shp ]; then rm ${TMP_DIR}${tmpfile}.shp ${TMP_DIR}${tmpfile}.dbf ${TMP_DIR}${tmpfile}.prj ${TMP_DIR}${tmpfile}.shx; fi
	gdal_contour -i 10 -snodata -32768 -a height $infile ${TMP_DIR}${tmpfile}.shp
	if [ $? -ne 0 ]; then echo $(date)' Error creating shapefile' & exit 4;fi
	
	echo "Building osm file …"
	${working_dir}/srtmshp2osm.py -q -f $TMP_DIR/${tmpfile}.shp -o $outfile
	if [ $? -ne 0 ]; then echo $(date)' Error creating OSM file' & exit 5;fi
	
	echo "Compressing to osm.bz2 …"
	bzip2 -f $outfile
	if [ $? -ne 0 ]; then echo $(date)' Error compressing OSM file' & exit 6;fi
#else
#echo "Skipping, already existing file"
#fi
