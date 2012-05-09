#!/bin/sh
make && ./osmand_main -renderingOutputFile=/home/victor/1.png -zoom=11 -lt=12,8 -lbox=-80,22.5 \
	-renderingInputFile="/home/victor/projects/OsmAnd/data/osm-gen/Cuba2.obf" -renderingInputFile="/home/victor/projects/OsmAnd/data/osm-gen/basemap_2.obf" \
	-renderingStyleFile="/home/victor/projects/OsmAnd/git/DataExtractionOSM/src/net/osmand/render/default.render.xml" \
	-imagesBasePathFile="/home/victor/projects/OsmAnd/git/OsmAnd/res/drawable-mdpi/"
