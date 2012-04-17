#ifndef _OSMAND_MAP_OBJECTSTagValuePairClass
#define _OSMAND_MAP_OBJECTS

#include <jni.h>
#include <vector>

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


void loadJniMapObjects()
{

}


void unloadJniMapObjects()
{
}

#endif /*_OSMAND_MAP_OBJECTS*/
