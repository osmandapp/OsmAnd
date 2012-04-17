#ifndef _OSMAND_MAP_OBJECTS_H
#define _OSMAND_MAP_OBJECTS_H

#include <jni.h>
#include <vector>
#include <hash_map>
#include <string>

#include "common.h"

typedef std::pair<std::string, std::string> tag_value;
typedef std::pair<int, int> int_pair;
typedef std::vector< std::pair<int, int> > coordinates;


class MapDataObject
{
	static const unsigned int UNDEFINED_STRING = INT_MAX;
public:

	std::vector<tag_value>  types;
	std::vector<tag_value>  additionalTypes;
	int objectType;
	coordinates  points;
	std::vector < coordinates > polygonInnerCoordinates;

	std::hash_map< std::string, unsigned int> stringIds;

	std::hash_map< std::string, std::string > objectNames;
	bool area;
	long long id;

	//

	bool cycle(){
		return points[0] == points[points.size() -1];
	}
	bool containsAdditional(std::string key, std::string val) {
		std::vector<tag_value>::iterator it = additionalTypes.begin();
		while (it != additionalTypes.end()) {
			if (it->first == key) {
				return it->second == val;
			}
			it++;
		}
		return false;
	}

	int getSimpleLayer() {
		std::vector<tag_value>::iterator it = additionalTypes.begin();
		while (it != additionalTypes.end()) {
			if (it->first == "layer") {
				if(it->second.length() > 0) {
					if(it->second[0] == '-'){
						return -1;
					} else {
						return 0;
					}
				} else {
					return 0;
				}
			}
			it++;
		}
		return 0;
	}
};

struct SearchResult {
	std::vector< MapDataObject* > result;
};


//std::vector <BaseMapDataObject* > marshalObjects(jobjectArray binaryMapDataObjects);

void deleteObjects(std::vector <MapDataObject* > & v);

void loadJniMapObjects();

void unloadJniMapObjects();


#endif /*_OSMAND_MAP_OBJECTS_H*/
