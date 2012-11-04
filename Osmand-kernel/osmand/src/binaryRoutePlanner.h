#ifndef _OSMAND_BINARY_ROUTE_PLANNER_H
#define _OSMAND_BINARY_ROUTE_PLANNER_H
#include "common.h"
#include "binaryRead.h"

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
	UNORDERED(map)<int64_t, SHARED_PTR<RouteSegment> > routes;

	RoutingSubregionTile(RouteSubregion& sub) : access(0), loaded(0), subregion(sub) {

	}
	~RoutingSubregionTile(){
	}
	bool isLoaded(){
		return loaded > 0;
	}

	void setLoaded(){
		loaded++;
	}

	void add(SHARED_PTR<RouteDataObject> o) {
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
	return (o->id << 10) + ind;
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
		memoryLimitation = (int)parseFloat("memoryLimitInMB", memoryLimitation);
		zoomToLoad = (int)parseFloat("zoomToLoadTiles", 16);
		routerName = parseString("name", "default");
		routerProfile = parseString("baseProfile", "car");
	}

	RoutingConfiguration(vector<ROUTE_TRIPLE>& config, int memLimit = 30, float initDirection = 0) :
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
			}
		}
		return 0;
	}

// TODO
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


struct RoutingContext {
	int visitedSegments;
	int loadedTiles;
	ElapsedTimer timeToLoad;
	ElapsedTimer timeToCalculate;
	int firstRoadDirection;
	int64_t firstRoadId;
	RoutingConfiguration config;

	int startX;
	int startY;
	int endX;
	int endY;

	vector<SHARED_PTR<RouteSegment> > segmentsToVisitNotForbidden;
	vector<SHARED_PTR<RouteSegment> > segmentsToVisitPrescripted;

	SHARED_PTR<FinalRouteSegment> finalRouteSegment;
	UNORDERED(map)<int64_t, SHARED_PTR<RoutingSubregionTile> > subregionTiles;
	UNORDERED(map)<int64_t, std::vector<SHARED_PTR<RoutingSubregionTile> > > indexedSubregions;

	RoutingContext(RoutingConfiguration& config) : finalRouteSegment(), firstRoadDirection(0), loadedTiles(0), visitedSegments(0),
			config(config){
	}

	bool acceptLine(SHARED_PTR<RouteDataObject> r) {
		return config.acceptLine(r);
	}

	void loadHeaderObjects(int64_t tileId) {
		vector<SHARED_PTR<RoutingSubregionTile> > subregions = indexedSubregions[tileId];
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
				int64_t key = ((int64_t)rs.left << 31)+ rs.length;
				if(subregionTiles.find(key) == subregionTiles.end()) {
					subregionTiles[key] = SHARED_PTR<RoutingSubregionTile>(new RoutingSubregionTile(rs));
				}
				collection.push_back(subregionTiles[key]);
			}
			//osmand_log_print(LOG_INFO, "Native load %d %d (%d)", xloc, yloc, tempResult.size());
			indexedSubregions[tileId] = collection;
		}
		loadHeaderObjects(tileId);
		timeToLoad.pause();
	}


	// FIXME replace with adequate method
	SHARED_PTR<RouteSegment> loadSegmentAround(int x31, int y31) {
		timeToLoad.start();
		SHARED_PTR<RouteSegment> r;
		float dist = -1;
		int z  = config.zoomToLoad;
		uint32_t xloc = x31 >> (31 - z);
		uint32_t yloc = y31 >> (31 - z);
		uint64_t l = (((uint64_t) x31) << 31) + (uint64_t) y31;
		int64_t tileId = (xloc << z) + yloc;
		loadHeaders(xloc, yloc);
		vector<SHARED_PTR<RoutingSubregionTile> > subregions = indexedSubregions[tileId];
		for(int j = 0; j<subregions.size(); j++) {
			if(subregions[j]->isLoaded()) {
				UNORDERED(map)<int64_t, SHARED_PTR<RouteSegment> >::iterator s = subregions[j]->routes.begin();
				while(s != subregions[j]->routes.end()) {
					SHARED_PTR<RouteSegment> seg = s->second;
					if(seg.get() != NULL) {
						double d = measuredDist(x31, y31, seg->road->pointsX[seg->getSegmentStart()],
								seg->road->pointsY[seg->getSegmentStart()]);
						if(dist == -1 || d < dist) {
							r = seg;
							dist = d;
						}
					}
					s++;
				}
			}
		}
		timeToLoad.pause();
		return r;
	}

	// void searchRouteRegion(SearchQuery* q, std::vector<RouteDataObject*>& list, RoutingIndex* rs, RouteSubregion* sub)
	SHARED_PTR<RouteSegment> loadRouteSegment(int x31, int y31) {
		int z  = config.zoomToLoad;
		int64_t xloc = x31 >> (31 - z);
		int64_t yloc = y31 >> (31 - z);
		uint64_t l = (((uint64_t) x31) << 31) + (uint64_t) y31;
		int64_t tileId = (xloc << z) + yloc;
		loadHeaders(xloc, yloc);
		vector<SHARED_PTR<RoutingSubregionTile> > subregions = indexedSubregions[tileId];
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
