#ifndef _OSMAND_MAP_OBJECTS_H
#define _OSMAND_MAP_OBJECTS_H

#include <vector>
#include <string>
#include <limits.h>

#include "common.h"

typedef pair<std::string, std::string> tag_value;
typedef pair<int, int> int_pair;
typedef vector< pair<int, int> > coordinates;


class MapDataObject
{
	static const unsigned int UNDEFINED_STRING = INT_MAX;
public:

	std::vector<tag_value>  types;
	std::vector<tag_value>  additionalTypes;
	coordinates points;
	std::vector < coordinates > polygonInnerCoordinates;

	HMAP::hash_map< std::string, unsigned int> stringIds;

	HMAP::hash_map< std::string, std::string > objectNames;
	bool area;
	long long id;

	//

	bool cycle(){
		return points[0] == points[points.size() -1];
	}
	bool containsAdditional(std::string key, std::string val) {
		std::vector<tag_value>::iterator it = additionalTypes.begin();
		while (it != additionalTypes.end()) {
			if (it->first == key && it->second == val) {
				return true;
			}
			it++;
		}
		return false;
	}

	bool contains(std::string key, std::string val) {
		std::vector<tag_value>::iterator it = types.begin();
		while (it != types.end()) {
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
						return 1;
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



void deleteObjects(std::vector <MapDataObject* > & v);


#endif /*_OSMAND_MAP_OBJECTS_H*/
