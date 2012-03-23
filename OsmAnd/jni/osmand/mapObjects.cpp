#ifndef _OSMAND_MAP_OBJECTS
#define _OSMAND_MAP_OBJECTS

#include <jni.h>
#include <vector>

#include "common.h"
#include "mapObjects.h"



jclass TagValuePairClass;
jfieldID TagValuePair_tag;
jfieldID TagValuePair_value;

void deleteObjects(std::vector <BaseMapDataObject* > & v)
{
	for(size_t i = 0; i< v.size(); i++)
	{
		delete v.at(i);
	}
	v.clear();
}


void loadJniMapObjects()
{

	TagValuePairClass = findClass("net/osmand/binary/BinaryMapIndexReader$TagValuePair");
	TagValuePair_tag = getGlobalJniEnv()->GetFieldID(TagValuePairClass, "tag", "Ljava/lang/String;");
	TagValuePair_value = getGlobalJniEnv()->GetFieldID(TagValuePairClass, "value", "Ljava/lang/String;");

}


void unloadJniMapObjects()
{
	getGlobalJniEnv()->DeleteGlobalRef( TagValuePairClass );
}

int getNegativeWayLayer(int type) {
	int i = (3 & (type >> 12));
	if (i == 1) {
		return -1;
	} else if (i == 2) {
		return 1;
	}
	return 0;
}

#endif /*_OSMAND_MAP_OBJECTS*/
