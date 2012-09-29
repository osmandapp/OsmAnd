#ifndef _OSMAND_BINARY_READ_H
#define _OSMAND_BINARY_READ_H

#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
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
	uint32_t length;
	uint32_t filePointer;
	uint32_t mapDataBlock;
	uint32_t left ;
	uint32_t right ;
	uint32_t top ;
	uint32_t bottom;
	bool ocean;

	MapTreeBounds() {
		ocean = -1;
	}
};

struct RouteSubregion {
	uint32_t length;
	uint32_t filePointer;
	uint32_t mapDataBlock;
	uint32_t left;
	uint32_t right;
	uint32_t top;
	uint32_t bottom;
	std::vector<RouteSubregion> subregions;

	RouteSubregion() : length(0), filePointer(0), mapDataBlock(0){
	}
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
	uint32_t length;
	int filePointer;
	PART_INDEXES type;
	std::string name;

	BinaryPartIndex(PART_INDEXES tp) : type(tp) {}
};

struct RoutingIndex : BinaryPartIndex {
	UNORDERED(map)< int, tag_value > decodingRules;
	std::vector<RouteSubregion> subregions;
	RoutingIndex() : BinaryPartIndex(ROUTING_INDEX) {
	}

	void initRouteEncodingRule(uint32_t id, std::string tag, std::string val) {
		tag_value pair = tag_value(tag, val);
		// DEFINE hash
		//encodingRules[pair] = id;
		decodingRules[id] = pair;
	}
};

struct RouteDataObject {
	RoutingIndex* region;
	std::vector<uint32_t> types ;
	std::vector<uint32_t> pointsX ;
	std::vector<uint32_t> pointsY ;
	std::vector<uint64_t> restrictions ;
	std::vector<std::vector<uint32_t> > pointTypes;
	int64_t id;

	UNORDERED(map)<int, std::string > names;
	vector<pair<uint32_t, uint32_t> > namesIds;
};



struct MapIndex : BinaryPartIndex {

	std::vector<MapRoot> levels;

	UNORDERED(map)<int, tag_value > decodingRules;
	// DEFINE hash
	//UNORDERED(map)<tag_value, int> encodingRules;

	int nameEncodingType;
	int refEncodingType;
	int coastlineEncodingType;
	int coastlineBrokenEncodingType;
	int landEncodingType;
	int onewayAttribute ;
	int onewayReverseAttribute ;
	UNORDERED(set)< int > positiveLayers;
	UNORDERED(set)< int > negativeLayers;

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

	void initMapEncodingRule(uint32_t type, uint32_t id, std::string tag, std::string val) {
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
	uint32_t version;
	uint64_t dateCreated;
	std::vector<MapIndex> mapIndexes;
	std::vector<RoutingIndex> routingIndexes;
	std::vector<BinaryPartIndex*> indexes;
	int fd;
	int routefd;
	bool basemap;

	bool isBasemap(){
		return basemap;
	}

	~BinaryMapFile() {
		close(fd);
		close(routefd);
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
	bool mixed;

	int numberOfVisitedObjects;
	int numberOfAcceptedObjects;
	int numberOfReadSubtrees;
	int numberOfAcceptedSubtrees;

	SearchQuery(int l, int r, int t, int b, RenderingRuleSearchRequest* req, ResultPublisher* publisher) :
			req(req), left(l), right(r), top(t), bottom(b),publisher(publisher) {
		numberOfAcceptedObjects = numberOfVisitedObjects = 0;
		numberOfAcceptedSubtrees = numberOfReadSubtrees = 0;
		ocean = mixed = false;
	}
	SearchQuery(int l, int r, int t, int b) :
				left(l), right(r), top(t), bottom(b) {
	}

	SearchQuery(){

	}

	bool publish(MapDataObject* obj) {
		return publisher->publish(obj);
	}
};

void searchRouteRegion(SearchQuery* q, std::vector<RouteDataObject*>& list, RoutingIndex* rs, RouteSubregion* sub);

ResultPublisher* searchObjectsForRendering(SearchQuery* q, bool skipDuplicates, std::string msgNothingFound);

BinaryMapFile* initBinaryMapFile(std::string inputName);

bool initMapFilesFromCache(std::string inputName) ;

bool closeBinaryMapFile(std::string inputName);

#endif
