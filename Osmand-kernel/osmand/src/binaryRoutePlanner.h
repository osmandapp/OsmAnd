#ifndef _OSMAND_BINARY_ROUTE_PLANNER_H
#define _OSMAND_BINARY_ROUTE_PLANNER_H
#include "common.h"
#include "binaryRead.h"
#include <algorithm>

typedef UNORDERED(map)<string, float> MAP_STR_FLOAT;
typedef UNORDERED(map)<string, string> MAP_STR_STR;

static double measuredDist(int x1, int y1, int x2, int y2) {
	return getDistance(get31LatitudeY(y1), get31LongitudeX(x1), get31LatitudeY(y2),
			get31LongitudeX(x2));
}

struct RouteSegment {
public :
	int segmentStart;
	SHARED_PTR<RouteDataObject> road;
	// needed to store intersection of routes
	SHARED_PTR<RouteSegment> next;

	// search context (needed for searching route)
	// Initially it should be null (!) because it checks was it segment visited before
	SHARED_PTR<RouteSegment> parentRoute;
	int parentSegmentEnd;

	// distance measured in time (seconds)
	float distanceFromStart;
	float distanceToEnd;

	inline int getSegmentStart() {
		return segmentStart;
	}

	RouteSegment(SHARED_PTR<RouteDataObject> road, int segmentStart) : road(road), segmentStart(segmentStart),
			parentSegmentEnd(0), distanceFromStart(0), distanceToEnd(0),next(), parentRoute(){
	}
	~RouteSegment(){
	}
};

struct RouteSegmentResult {
	SHARED_PTR<RouteDataObject> object;
	int startPointIndex;
	int endPointIndex;
	vector<vector<RouteSegmentResult> > attachedRoutes;
	RouteSegmentResult(SHARED_PTR<RouteDataObject> object, int startPointIndex, int endPointIndex) :
		object(object), startPointIndex(startPointIndex), endPointIndex (endPointIndex) {

	}
};

struct FinalRouteSegment {
	SHARED_PTR<RouteSegment> direct;
	bool reverseWaySearch;
	SHARED_PTR<RouteSegment> opposite;
	float distanceFromStart;
};



struct RoutingSubregionTile {
	RouteSubregion subregion;
	// make it without get/set for fast access
	int access;
	int loaded;
	int size ;
	UNORDERED(map)<int64_t, SHARED_PTR<RouteSegment> > routes;

	RoutingSubregionTile(RouteSubregion& sub) : access(0), loaded(0), subregion(sub) {
		size = sizeof(RoutingSubregionTile);
	}
	~RoutingSubregionTile(){
	}
	bool isLoaded(){
		return loaded > 0;
	}

	void setLoaded(){
		loaded = abs(loaded) + 1;
	}

	void unload(){
		routes.clear();
		size = 0;
		loaded = - abs(loaded);
	}

	int getUnloadCount() {
		return abs(loaded);
	}

	int getSize(){
		return size + routes.size() * sizeof(std::pair<int64_t, SHARED_PTR<RouteSegment> >);
	}

	void add(SHARED_PTR<RouteDataObject> o) {
		size += o->getSize() + sizeof(RouteSegment)* o->pointsX.size();
		for (int i = 0; i < o->pointsX.size(); i++) {
			uint64_t x31 = o->pointsX[i];
			uint64_t y31 = o->pointsY[i];
			uint64_t l = (((uint64_t) x31) << 31) + (uint64_t) y31;
			SHARED_PTR<RouteSegment> segment =  SHARED_PTR<RouteSegment>(new RouteSegment(o, i));
			if (routes[l].get() == NULL) {
				routes[l] = segment;
			} else {
				SHARED_PTR<RouteSegment> orig = routes[l];
				int cnt = 0;
				while (orig->next.get() != NULL) {
					orig = orig->next;
					cnt++;
				}
				orig->next = segment;
			}
		}
	}
};
static int64_t calcRouteId(SHARED_PTR<RouteDataObject> o, int ind) {
	return ((int64_t) o->id << 10) + ind;
}

typedef std::pair<int, std::pair<string, string> > ROUTE_TRIPLE;
struct RoutingConfiguration {
	// 0 index in triple
	MAP_STR_FLOAT highwaySpeed ;
	// 1 index in triple
	MAP_STR_FLOAT highwayPriorities ;
	// 2 index in triple
	MAP_STR_FLOAT avoid ;
	// 3 index in triple
	MAP_STR_FLOAT obstacles;
	// 4 index in triple
	MAP_STR_FLOAT routingObstacles;
	// 5 index in triple
	MAP_STR_STR attributes;

	int zoomToLoad;
	float heurCoefficient;
	float maxDefaultSpeed;
	float minDefaultSpeed;
	bool restrictions;
	bool onewayAware;
	bool followLimitations;
	int memoryLimitation;
	int planRoadDirection;
	string routerName;
	float initialDirection;
	float distanceRecalculate;
	string routerProfile;
	float roundaboutTurn;
	float leftTurn;
	float rightTurn;

	float parseFloat(string key, float def) {
		if(attributes.find(key) != attributes.end() && attributes[key] != "") {
			return atof(attributes[key].c_str());
		}
		return def;
	}
	bool parseBool(string key, bool def) {
		if (attributes.find(key) != attributes.end() && attributes[key] != "") {
			return attributes[key] == "true";
		}
		return def;
	}

	string parseString(string key, string def) {
		if (attributes.find(key) != attributes.end() && attributes[key] != "") {
			return attributes[key];
		}
		return def;
	}
	void defaultParams() {
		planRoadDirection = (int) parseFloat("planRoadDirection", 0);
		restrictions = parseBool("restrictionsAware", true);
		followLimitations = parseBool("followSpeedLimitations", true);
		onewayAware = parseBool("onewayAware", true);
		roundaboutTurn = parseFloat("roundaboutTurn", 0);
		leftTurn = parseFloat("leftTurn", 0);
		rightTurn = parseFloat("rightTurn", 0);
		minDefaultSpeed = parseFloat("minDefaultSpeed", 45) / 3.6;
		maxDefaultSpeed = parseFloat("maxDefaultSpeed", 130) / 3.6;
		heurCoefficient = parseFloat("heuristicCoefficient", 1);
		// don't use file limitations?
		memoryLimitation = (int)parseFloat("nativeMemoryLimitInMB", memoryLimitation);
		zoomToLoad = (int)parseFloat("zoomToLoadTiles", 16);
		routerName = parseString("name", "default");
		routerProfile = parseString("baseProfile", "car");
		distanceRecalculate = parseFloat("recalculateDistanceHelp", 10000) ;
	}

	RoutingConfiguration(vector<ROUTE_TRIPLE>& config, float initDirection = -360, int memLimit = 48) :
			memoryLimitation(memLimit), initialDirection(initDirection) {
		for(int j = 0; j<config.size(); j++) {
			ROUTE_TRIPLE r = config[j];
			if(r.first == 0) {
				highwaySpeed[r.second.first] = atof(r.second.second.c_str());
			} else if(r.first == 1) {
				highwayPriorities[r.second.first] = atof(r.second.second.c_str());
			} else if(r.first == 2) {
				avoid[r.second.first] = atof(r.second.second.c_str());
			} else if(r.first == 3) {
				obstacles[r.second.first] = atof(r.second.second.c_str());
			} else if(r.first == 4) {
				routingObstacles[r.second.first] = atof(r.second.second.c_str());
			} else if(r.first == 5) {
				string v = r.second.second;
				attributes[r.second.first] = v;
			}
		}
		defaultParams();
	}

	bool acceptLine(SHARED_PTR<RouteDataObject> r) {
		std::vector<uint32_t>::iterator t = r->types.begin();
		bool accepted = false;
		for(; t != r->types.end(); t++) {
			tag_value type = r->region->decodingRules[*t];
			if(type.first=="highway" && highwaySpeed[type.second] > 0) {
				accepted = true;
				break;
			} else if(highwaySpeed[type.first + '$' + type.second] > 0) {
				accepted = true;
				break;
			}
		}
		if(!accepted) {
			return false;
		}
		t = r->types.begin();
		for(; t != r->types.end(); t++) {
			tag_value type = r->region->decodingRules[*t];
			if(avoid.find(type.first + '$' + type.second) != avoid.end()) {
				return false;
			}
		}
		return true;
	}

	string getHighway(SHARED_PTR<RouteDataObject> r) {
		std::vector<uint32_t>::iterator t = r->types.begin();
		for(; t != r->types.end(); t++) {
			tag_value type = r->region->decodingRules[*t];
			if(type.first=="highway") {
				return type.second;
			}
		}
		return "";
	}

	float defineSpeedPriority(SHARED_PTR<RouteDataObject> r) {
		float priority = 1;
		std::vector<uint32_t>::iterator t = r->types.begin();
		for(; t != r->types.end(); t++) {
			tag_value type = r->region->decodingRules[*t];
			if(highwayPriorities.find(type.first+"$"+type.second) != highwaySpeed.end()) {
				priority *= highwayPriorities[type.first+"$"+type.second];
			}
		}
		return priority;
	}

	float getMinDefaultSpeed() {
		return minDefaultSpeed;
	}
	float getMaxDefaultSpeed() {
		return maxDefaultSpeed;
	}

	int isOneWay(SHARED_PTR<RouteDataObject> r) {
		if(!onewayAware){
			return 0;
		}
		std::vector<uint32_t>::iterator t = r->types.begin();
		for(; t != r->types.end(); t++) {
			tag_value type = r->region->decodingRules[*t];
			if(type.first == "oneway") {
				string v = type.second;
				if("-1" ==v || "reverse" == v) {
					return -1;
				} else if("1" == v || "yes" == v) {
					return 1;
				}
			} else if(type.first == "roundabout") {
				return 1;
			} else if(type.first == "junction" && type.second == "roundabout") {
				return 1;
			}
		}
		return 0;
	}

// TODO FIX
	float calculateTurnTime(SHARED_PTR<RouteSegment> segment, int index, SHARED_PTR<RouteSegment> next, int nextIndex) {
		return 0;
	}

	float defineRoutingObstacle(SHARED_PTR<RouteDataObject> road, int segmentEnd) {
		if(road->pointTypes.size() <= segmentEnd) {
			return 0;
		}
		std::vector<uint32_t> pointTypes = road->pointTypes[segmentEnd];
		std::vector<uint32_t>::iterator t = pointTypes.begin();
		for(; t != pointTypes.end(); t++) {
			tag_value type = road->region->decodingRules[*t];
			if(routingObstacles.find(type.first + "$" + type.second) != routingObstacles.end()) {
				return routingObstacles[type.first + "$" + type.second];
			}
		}
		t = pointTypes.begin();
		for(; t != pointTypes.end(); t++) {
			tag_value type = road->region->decodingRules[*t];
			if(routingObstacles.find(type.first + "$" ) != routingObstacles.end()) {
				return routingObstacles[type.first + "$" ];
			}
		}
		return 0;
	}

	bool restrictionsAware() {
		return restrictions;
	}

	float maxSpeed(SHARED_PTR<RouteDataObject> r) {
		std::vector<uint32_t>::iterator t = r->types.begin();
		for(; t != r->types.end(); t++) {
			tag_value type = r->region->decodingRules[*t];
			if(type.first=="maxspeed") {
				std::string v = type.second;
				int i = 0;
				while(i < v.length() && v[i] >= '0' && v[i] <= '9') {
					i++;
				}
				if(i > 0) {
					float f = atoi(v.substr(0, i).c_str());
					f = f / 3.6;
					if(v.find("mph") != std::string::npos ) {
						f *= 1.6;
					}
					return f;
				}
				return 0;
			}
		}
		return 0;
	}

	float defineSpeed(SHARED_PTR<RouteDataObject> r) {
		if (followLimitations) {
			float m = maxSpeed(r);
			if(m > 0) {
				return m;
			}
		}
		std::vector<uint32_t>::iterator t = r->types.begin();
		for(; t != r->types.end(); t++) {
			tag_value type = r->region->decodingRules[*t];
			if(highwaySpeed.find(type.first+"$"+type.second) != highwaySpeed.end()) {
				return highwaySpeed[type.first+"$"+type.second] / 3.6;
			}
		}
		return getMinDefaultSpeed();
	}

};

bool compareRoutingSubregionTile(SHARED_PTR<RoutingSubregionTile> o1, SHARED_PTR<RoutingSubregionTile> o2);

class RouteCalculationProgress {
protected:
	int segmentNotFound ;
	float distanceFromBegin;
	int directSegmentQueueSize;
	float distanceFromEnd;
	int reverseSegmentQueueSize;

	bool cancelled;
public:
	RouteCalculationProgress() : segmentNotFound(-1), distanceFromBegin(0),
		distanceFromEnd(0), directSegmentQueueSize(0), reverseSegmentQueueSize(0), cancelled(false){
	}

	virtual bool isCancelled(){
		return cancelled;
	}

	virtual void setSegmentNotFound(int s){
		segmentNotFound = s;
	}

	virtual void updateStatus(float distanceFromBegin,	int directSegmentQueueSize,	float distanceFromEnd,
			int reverseSegmentQueueSize) {
		this->distanceFromBegin = max(distanceFromBegin, this->distanceFromBegin );
		this->distanceFromEnd = max(distanceFromEnd,this->distanceFromEnd);
		this->directSegmentQueueSize = directSegmentQueueSize;
		this->reverseSegmentQueueSize = reverseSegmentQueueSize;
	}

};


struct RoutingContext {
	typedef UNORDERED(map)<int64_t, SHARED_PTR<RoutingSubregionTile> > MAP_SUBREGION_TILES;

	int visitedSegments;
	int loadedTiles;
	ElapsedTimer timeToLoad;
	ElapsedTimer timeToCalculate;
	int firstRoadDirection;
	int64_t firstRoadId;
	RoutingConfiguration config;
	SHARED_PTR<RouteCalculationProgress> progress;

	int gcCollectIterations;

	int startX;
	int startY;
	int endX;
	int endY;

	vector<SHARED_PTR<RouteSegment> > segmentsToVisitNotForbidden;
	vector<SHARED_PTR<RouteSegment> > segmentsToVisitPrescripted;

	SHARED_PTR<FinalRouteSegment> finalRouteSegment;
	MAP_SUBREGION_TILES subregionTiles;
	UNORDERED(map)<int64_t, std::vector<SHARED_PTR<RoutingSubregionTile> > > indexedSubregions;

	RoutingContext(RoutingConfiguration& config) : finalRouteSegment(), firstRoadDirection(0), loadedTiles(0), visitedSegments(0),
			config(config){
	}

	bool acceptLine(SHARED_PTR<RouteDataObject> r) {
		return config.acceptLine(r);
	}

	int getSize() {
		// multiply 2 for to maps
		int sz = subregionTiles.size() * sizeof(pair< int64_t, SHARED_PTR<RoutingSubregionTile> >)  * 2;
		MAP_SUBREGION_TILES::iterator it = subregionTiles.begin();
		for(;it != subregionTiles.end(); it++) {
			sz += it->second->getSize();
		}
		return sz;
	}

	void unloadUnusedTiles(int memoryLimit) {
		int sz = getSize();
		float critical = 0.9f * memoryLimit * 1024 * 1024;
		if(sz < critical) {
			return;
		}
		float occupiedBefore = sz / (1024. * 1024.);
		float desirableSize = memoryLimit * 0.7f * 1024 * 1024;
		vector<SHARED_PTR<RoutingSubregionTile> > list;
		MAP_SUBREGION_TILES::iterator it = subregionTiles.begin();
		int loaded = 0;
		int unloadedTiles = 0;
		for(;it != subregionTiles.end(); it++) {
			if(it->second->isLoaded()) {
				list.push_back(it->second);
				loaded++;
			}
		}
		sort(list.begin(), list.end(), compareRoutingSubregionTile);
		int i =0;
		while(sz >= desirableSize && i < list.size()) {
			SHARED_PTR<RoutingSubregionTile> unload = list[i];
			i++;
			sz -= unload->getSize();
			unload->unload();
			unloadedTiles ++;
		}
		for(i = 0; i<list.size(); i++) {
			list[i]->access /= 3;
		}
		osmand_log_print(LOG_INFO, "Run GC (before %f Mb after %f Mb) unload %d of %d tiles",
				occupiedBefore, getSize() / (1024.0*1024.0),
				unloadedTiles, loaded);
	}

	void loadHeaderObjects(int64_t tileId) {
		vector<SHARED_PTR<RoutingSubregionTile> >& subregions = indexedSubregions[tileId];
		bool gc = false;
		for(int j = 0; j<subregions.size() && !gc; j++) {
			if(!subregions[j]->isLoaded()) {
				gc = true;
			}
		}
		if(gc) {
			unloadUnusedTiles(config.memoryLimitation);
		}
		for(int j = 0; j<subregions.size(); j++) {
			if(!subregions[j]->isLoaded()) {
				loadedTiles++;
				subregions[j]->setLoaded();
				SearchQuery q;
				vector<RouteDataObject*> res;
				searchRouteDataForSubRegion(&q, res, &subregions[j]->subregion);
				vector<RouteDataObject*>::iterator i = res.begin();
				for(;i!=res.end(); i++) {
					if(*i != NULL) {
						SHARED_PTR<RouteDataObject> o(*i);
						if(acceptLine(o)) {
							subregions[j]->add(o);
						}
					}
				}
			}
		}
	}

	void loadHeaders(uint32_t xloc, uint32_t yloc) {
		timeToLoad.start();
		int z  = config.zoomToLoad;
		int tz = 31 - z;
		int64_t tileId = (xloc << z) + yloc;
		if (indexedSubregions.find(tileId) == indexedSubregions.end()) {
			SearchQuery q((uint32_t) (xloc << tz),
							(uint32_t) ((xloc + 1) << tz), (uint32_t) (yloc << tz), (uint32_t) ((yloc + 1) << tz));
			std::vector<RouteSubregion> tempResult;
			searchRouteSubregions(&q, tempResult);
			std::vector<SHARED_PTR<RoutingSubregionTile> > collection;
			for(int i=0; i<tempResult.size(); i++) {
				RouteSubregion& rs = tempResult[i];
				int64_t key = ((int64_t)rs.left << 31)+ rs.filePointer;
				if(subregionTiles.find(key) == subregionTiles.end()) {
					subregionTiles[key] = SHARED_PTR<RoutingSubregionTile>(new RoutingSubregionTile(rs));
				}
				collection.push_back(subregionTiles[key]);
			}
			indexedSubregions[tileId] = collection;
		}
		loadHeaderObjects(tileId);
		timeToLoad.pause();
	}

	void loadTileData(int x31, int y31, int zoomAround, vector<SHARED_PTR<RouteDataObject> >& dataObjects ) {
		int t = config.zoomToLoad - zoomAround;
		int coordinatesShift = (1 << (31 - config.zoomToLoad));
		if(t <= 0) {
			t = 1;
			coordinatesShift = (1 << (31 - zoomAround));
		} else {
			t = 1 << t;
		}
		UNORDERED(set)<int64_t> ids;
		int z  = config.zoomToLoad;
		for(int i = -t; i <= t; i++) {
			for(int j = -t; j <= t; j++) {
				uint32_t xloc = (x31 + i*coordinatesShift) >> (31 - z);
				uint32_t yloc = (y31+j*coordinatesShift) >> (31 - z);
				int64_t tileId = (xloc << z) + yloc;
				loadHeaders(xloc, yloc);
				vector<SHARED_PTR<RoutingSubregionTile> >& subregions = indexedSubregions[tileId];
				for(int j = 0; j<subregions.size(); j++) {
					if(subregions[j]->isLoaded()) {
						UNORDERED(map)<int64_t, SHARED_PTR<RouteSegment> >::iterator s = subregions[j]->routes.begin();
						while(s != subregions[j]->routes.end()) {
							SHARED_PTR<RouteSegment> seg = s->second;
							if(seg.get() != NULL) {
								if(ids.find(seg->road->id) == ids.end()) {
									dataObjects.push_back(seg->road);
									ids.insert(seg->road->id);
								}
								seg = seg->next;
							}
							s++;
						}
					}
				}
			}
		}
	}

	// void searchRouteRegion(SearchQuery* q, std::vector<RouteDataObject*>& list, RoutingIndex* rs, RouteSubregion* sub)
	SHARED_PTR<RouteSegment> loadRouteSegment(int x31, int y31) {
		int z  = config.zoomToLoad;
		int64_t xloc = x31 >> (31 - z);
		int64_t yloc = y31 >> (31 - z);
		uint64_t l = (((uint64_t) x31) << 31) + (uint64_t) y31;
		int64_t tileId = (xloc << z) + yloc;
		loadHeaders(xloc, yloc);
		vector<SHARED_PTR<RoutingSubregionTile> >& subregions = indexedSubregions[tileId];
		UNORDERED(map)<int64_t, SHARED_PTR<RouteDataObject> > excludeDuplications;
		SHARED_PTR<RouteSegment> original;
		for(int j = 0; j<subregions.size(); j++) {
			if(subregions[j]->isLoaded()) {
				SHARED_PTR<RouteSegment> segment = subregions[j]->routes[l];
				subregions[j]->access++;
				while (segment.get() != NULL) {
					SHARED_PTR<RouteDataObject> ro = segment->road;
					SHARED_PTR<RouteDataObject> toCmp = excludeDuplications[calcRouteId(ro, segment->getSegmentStart())];
					if (toCmp.get() == NULL || toCmp->pointsX.size() < ro->pointsX.size()) {
						excludeDuplications[calcRouteId(ro, segment->getSegmentStart())] =  ro;
						SHARED_PTR<RouteSegment> s = SHARED_PTR<RouteSegment>(new RouteSegment(ro, segment->getSegmentStart()));
						s->next = original;
						original = 	s;
					}
					segment = segment->next;
				}
			}
		}

		return original;
	}


	bool isInterrupted(){
		return false;
	}
	float getHeuristicCoefficient(){
		return config.heurCoefficient;
	}

	bool planRouteIn2Directions() {
		return getPlanRoadDirection() == 0;
	}
	int getPlanRoadDirection() {
		return config.planRoadDirection;
	}

};


vector<RouteSegmentResult> searchRouteInternal(RoutingContext* ctx, bool leftSideNavigation);
#endif /*_OSMAND_BINARY_ROUTE_PLANNER_H*/
