#ifndef _OSMAND_BINARY_READ_H
#define _OSMAND_BINARY_READ_H

#include <stdio.h>
#include <fstream>
#include <map>
#include <string>
#include <stdint.h>


#include "mapObjects.h"
#include "multipolygons.h"
#include "common.h"


#include "mapObjects.h"
#include "renderRules.h"

static const int MAP_VERSION = 2;
static const int BASEMAP_ZOOM = 11;


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

struct RouteSubregion {
	uint32 length;
	uint32 filePointer;
	uint32 mapDataBlock;
	uint32 left;
	uint32 right;
	uint32 top;
	uint32 bottom;
	std::vector<RouteSubregion> subregions;
};


struct MapRoot: MapTreeBounds {
	int minZoom ;
	int maxZoom ;
	std::vector<MapTreeBounds> bounds;
};

enum PART_INDEXES {
	MAP_INDEX = 1,
	POI_INDEX,
	ADDRESS_INDEX,
	TRANSPORT_INDEX,
	ROUTING_INDEX,
};

struct BinaryPartIndex {
	uint32 length;
	int filePointer;
	PART_INDEXES type;
	std::string name;

	BinaryPartIndex(PART_INDEXES tp) : type(tp) {}
};

struct RoutingIndex : BinaryPartIndex {
	HMAP::hash_map<int, tag_value > decodingRules;
	std::vector<RouteSubregion> subregions;
	RoutingIndex() : BinaryPartIndex(ROUTING_INDEX) {
	}

	void initRouteEncodingRule(uint32 id, std::string tag, std::string val) {
		tag_value pair = tag_value(tag, val);
		// DEFINE hash
		//encodingRules[pair] = id;
		decodingRules[id] = pair;
	}
};

struct RouteDataObject {
	RoutingIndex* region;
	std::vector<uint32> types ;
	std::vector<uint32> pointsX ;
	std::vector<uint32> pointsY ;
	std::vector<uint64> restrictions ;
	std::vector<std::vector<uint32> > pointTypes;
	int64 id;
};



struct MapIndex : BinaryPartIndex {

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

	MapIndex() : BinaryPartIndex(MAP_INDEX) {
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
	uint32 version;
	uint64 dateCreated;
	std::vector<MapIndex> mapIndexes;
	std::vector<RoutingIndex> routingIndexes;
	std::vector<BinaryPartIndex*> indexes;
	FILE* f;
	bool basemap;

	bool isBasemap(){
		return basemap;
	}

	~BinaryMapFile() {
		fclose(f);
	}
};

struct ResultPublisher {
	std::vector< MapDataObject*> result;

	bool publish(MapDataObject* r) {
		result.push_back(r);
		return true;
	}
	bool publish(std::vector<MapDataObject*> r) {
		result.insert(result.begin(), r.begin(), r.end());
		return true;
	}
	bool isCancelled() {
		return false;
	}
	virtual ~ResultPublisher() {
		deleteObjects(result);
	}
};

struct SearchQuery {
	RenderingRuleSearchRequest* req;
	int left;
	int right;
	int top;
	int bottom;
	int zoom;
	ResultPublisher* publisher;

	coordinates cacheCoordinates;
	bool ocean;
	bool land;

	int numberOfVisitedObjects;
	int numberOfAcceptedObjects;
	int numberOfReadSubtrees;
	int numberOfAcceptedSubtrees;
	std::vector<RouteDataObject> routeObjects;

	SearchQuery(int l, int r, int t, int b, RenderingRuleSearchRequest* req, ResultPublisher* publisher) :
			req(req), left(l), right(r), top(t), bottom(b),publisher(publisher) {
		numberOfAcceptedObjects = numberOfVisitedObjects = 0;
		numberOfAcceptedSubtrees = numberOfReadSubtrees = 0;
		ocean = land = false;
	}
	SearchQuery(int l, int r, int t, int b, std::vector<RouteDataObject>& result) :
				req(req), left(l), right(r), top(t), bottom(b), routeObjects(result) {
	}

	bool publish(MapDataObject* obj) {
		return publisher->publish(obj);
	}
	bool publishRouteObject(RouteDataObject& obj) {
		routeObjects.push_back(obj);
		return true;
	}
};

void searchRouteRegion(SearchQuery* q);

ResultPublisher* searchObjectsForRendering(SearchQuery* q, bool skipDuplicates, std::string msgNothingFound);

BinaryMapFile* initBinaryMapFile(std::string inputName);

bool closeBinaryMapFile(std::string inputName);

#endif
