#ifndef _OSMAND_MAP_OBJECTS
#define _OSMAND_MAP_OBJECTS

#include "common.h"
#include "mapObjects.h"

void deleteObjects(std::vector <MapDataObject* > & v)
{
	for(size_t i = 0; i< v.size(); i++)
	{
		delete v.at(i);
	}
	v.clear();
}

#endif /*_OSMAND_MAP_OBJECTS*/
