#ifndef _OSMAND_MAP_OBJECTS_H
#define _OSMAND_MAP_OBJECTS_H

#include <jni.h>
#include <vector>
#include <string>

#include "common.h"

class BaseMapDataObject
{

public :
	static const unsigned int UNDEFINED_STRING = 0x7fffffff;
    const int type;
    static const int MAP_DATA_OBJECT = 1;
    static const int MULTI_POLYGON = 2;
    long long id;
    unsigned int stringId;
    std::string name;
protected :
	BaseMapDataObject(int t) : type(t), id(0), stringId(UNDEFINED_STRING){ }

};

struct SearchResult {
	std::vector< BaseMapDataObject*> result;
};

class MultiPolygonObject : public BaseMapDataObject
{
public:
	MultiPolygonObject() : BaseMapDataObject(MULTI_POLYGON)	{	}
	std::string tag;
	std::string value;
	std::vector< std::string > names;
	int layer;
	std::vector< std::vector< std::pair<int, int> > > points;
};

class MapDataObject : public BaseMapDataObject
{
public:
	MapDataObject() : BaseMapDataObject(MAP_DATA_OBJECT)	{	}

	std::vector< int>  types;
	std::vector< std::pair<int, int> >  points;
	std::vector< std::pair<std::string, std::string> >  tagValues;
	int highwayAttributes;
};


std::vector <BaseMapDataObject* > marshalObjects(jobjectArray binaryMapDataObjects);

void deleteObjects(std::vector <BaseMapDataObject* > & v);

void loadJniMapObjects();

void unloadJniMapObjects();

#endif /*_OSMAND_MAP_OBJECTS_H*/
