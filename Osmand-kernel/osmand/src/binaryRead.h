#ifndef _OSMAND_BINARY_READ_H
#define _OSMAND_BINARY_READ_H

#include <stdio.h>
#include <fstream>
#include <algorithm>
#include <map>
#include <string>
#include <stdint.h>
#include "google/protobuf/wire_format_lite.h"
#include "common.h"
#include "mapObjects.h"



struct MapTreeBounds {
	uint32 length;
	uint32 filePointer;
	uint32 mapDataBlock;
	uint32 left ;
	uint32 right ;
	uint32 top ;
	uint32 bottom;
	bool ocean;

	MapTreeBounds() {
		ocean = -1;
	}
};

struct MapRoot: MapTreeBounds {
	int minZoom ;
	int maxZoom ;
	std::vector<MapTreeBounds> bounds;
};


struct MapIndex {
	uint32 length;
	int filePointer;
	std::string name;
	std::vector<MapRoot> levels;

	HMAP::hash_map<int, tag_value > decodingRules;
	// DEFINE hash
	//HMAP::hash_map<tag_value, int> encodingRules;

	int nameEncodingType;
	int refEncodingType;
	int coastlineEncodingType;
	int coastlineBrokenEncodingType;
	int landEncodingType;
	int onewayAttribute ;
	int onewayReverseAttribute ;
	HMAP::hash_set< int > positiveLayers;
	HMAP::hash_set< int > negativeLayers;

	MapIndex(){
		nameEncodingType = refEncodingType = coastlineBrokenEncodingType = coastlineEncodingType = -1;
		landEncodingType = onewayAttribute = onewayReverseAttribute = -1;
	}

	void finishInitializingTags() {
		int free = decodingRules.size() * 2 + 1;
		coastlineBrokenEncodingType = free++;
		initMapEncodingRule(0, coastlineBrokenEncodingType, "natural", "coastline_broken");
		if (landEncodingType == -1) {
			landEncodingType = free++;
			initMapEncodingRule(0, landEncodingType, "natural", "land");
		}
	}

	void initMapEncodingRule(uint32 type, uint32 id, std::string tag, std::string val) {
		tag_value pair = tag_value(tag, val);
		// DEFINE hash
		//encodingRules[pair] = id;
		decodingRules[id] = pair;

		if ("name" == tag) {
			nameEncodingType = id;
		} else if ("natural" == tag && "coastline" == val) {
			coastlineEncodingType = id;
		} else if ("natural" == tag && "land" == val) {
			landEncodingType = id;
		} else if ("oneway" == tag && "yes" == val) {
			onewayAttribute = id;
		} else if ("oneway" == tag && "-1" == val) {
			onewayReverseAttribute = id;
		} else if ("ref" == tag) {
			refEncodingType = id;
		} else if ("layer" == tag) {
			if (val != "" && val != "0") {
				if (val[0] == '-') {
					negativeLayers.insert(id);
				} else {
					positiveLayers.insert(id);
				}
			}
		}
	}
};


struct BinaryMapFile {
	std::string inputName;
	std::vector<MapIndex> mapIndexes;
	FILE* f;
	bool basemap;

	bool isBasemap(){
		return basemap;
	}

	~BinaryMapFile() {
		fclose(f);
	}
};

extern "C" JNIEXPORT jboolean JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_initBinaryMapFile(JNIEnv* ienv,
		jobject obj, jobject path);


#endif
