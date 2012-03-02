#ifndef _OSMAND_MAP_OBJECTS
#define _OSMAND_MAP_OBJECTS

#include <jni.h>
#include <vector>

#include "common.h"
#include "mapObjects.h"


jclass MultiPolygonClass;
jmethodID MultiPolygon_getTag;
jmethodID MultiPolygon_getValue;
jmethodID MultiPolygon_getLayer;
jmethodID MultiPolygon_getPoint31XTile;
jmethodID MultiPolygon_getPoint31YTile;
jmethodID MultiPolygon_getBoundsCount;
jmethodID MultiPolygon_getBoundPointsCount;
jmethodID MultiPolygon_getName;


jclass BinaryMapDataObjectClass;
jmethodID BinaryMapDataObject_getPointsLength;
jmethodID BinaryMapDataObject_getPoint31YTile;
jmethodID BinaryMapDataObject_getPoint31XTile;
jmethodID BinaryMapDataObject_getHighwayAttributes;
jmethodID BinaryMapDataObject_getTypes;
jmethodID BinaryMapDataObject_getName;
jmethodID BinaryMapDataObject_getTagValue;

jclass TagValuePairClass;
jfieldID TagValuePair_tag;
jfieldID TagValuePair_value;

std::vector <BaseMapDataObject* > marshalObjects(jobjectArray binaryMapDataObjects)
{
	std::vector<BaseMapDataObject*> v;

	const size_t size = getGlobalJniEnv()->GetArrayLength(binaryMapDataObjects);
	size_t i = 0;
	for (; i < size; i++) {
		jobject binaryMapDataObject = (jobject) getGlobalJniEnv()->GetObjectArrayElement(binaryMapDataObjects, i);
		if (getGlobalJniEnv()->IsInstanceOf(binaryMapDataObject, MultiPolygonClass)) {
			MultiPolygonObject* o = new MultiPolygonObject();
			v.push_back((BaseMapDataObject* )o);
			o->layer = getGlobalJniEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getLayer);
			o->tag = getStringMethod(binaryMapDataObject, MultiPolygon_getTag);
			o->value = getStringMethod(binaryMapDataObject, MultiPolygon_getValue);

			int boundsCount = getGlobalJniEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getBoundsCount);
			for (int ji = 0; ji < boundsCount; ji++) {
				int cnt = getGlobalJniEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getBoundPointsCount, ji);
				std::vector<std::pair<int, int> > vs;
				for (int js = 0; js < cnt; js++) {
					int xt = getGlobalJniEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getPoint31XTile, js, ji);
					int yt = getGlobalJniEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getPoint31YTile, js, ji);
					vs.push_back( std::pair<int, int> (xt, yt) );
				}

				o->points.push_back(vs);
				o->names.push_back(getStringMethod(binaryMapDataObject, MultiPolygon_getName, ji));
			}



		} else {
			jintArray types = (jintArray) getGlobalJniEnv()->CallObjectMethod(binaryMapDataObject, BinaryMapDataObject_getTypes);
			if (types != NULL) {
				MapDataObject* o = new MapDataObject();
				jint sizeTypes = getGlobalJniEnv()->GetArrayLength(types);
				jint* els = getGlobalJniEnv()->GetIntArrayElements(types, NULL);
				int j = 0;
				for (; j < sizeTypes; j++) {
					int wholeType = els[j];
					o->types.push_back(wholeType);
					jobject pair = getGlobalJniEnv()->CallObjectMethod(binaryMapDataObject, BinaryMapDataObject_getTagValue, j);
					if (pair != NULL) {
						std::string tag = getStringField(pair, TagValuePair_tag);
						std::string value = getStringField(pair, TagValuePair_value);
						o->tagValues.push_back( std::pair<std:: string, std::string>(tag, value));
						getGlobalJniEnv()->DeleteLocalRef(pair);
					} else {
						o->tagValues.push_back( std::pair<std:: string, std::string>(std::string(), std::string()));
					}
				}

				jint sizePoints = getGlobalJniEnv()->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPointsLength);
				for (j = 0; j < sizePoints; j++) {
					int tx = getGlobalJniEnv()->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPoint31XTile, j);
					int ty = getGlobalJniEnv()->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPoint31YTile, j);
					o->points.push_back(std::pair<int, int>(tx, ty));
				}
				o->name = getStringMethod(binaryMapDataObject, BinaryMapDataObject_getName);
				o->highwayAttributes = getGlobalJniEnv()->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getHighwayAttributes);
				getGlobalJniEnv()->ReleaseIntArrayElements(types, els, JNI_ABORT);
				getGlobalJniEnv()->DeleteLocalRef(types);
				v.push_back((BaseMapDataObject* )o);
			}
		}
		getGlobalJniEnv()->DeleteLocalRef(binaryMapDataObject);
	}

	return v;
}

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
	MultiPolygonClass = findClass("net/osmand/osm/MultyPolygon");
	MultiPolygon_getTag = getGlobalJniEnv()->GetMethodID(MultiPolygonClass, "getTag", "()Ljava/lang/String;");
	MultiPolygon_getValue = getGlobalJniEnv()->GetMethodID(MultiPolygonClass, "getValue", "()Ljava/lang/String;");
	MultiPolygon_getName = getGlobalJniEnv()->GetMethodID(MultiPolygonClass, "getName", "(I)Ljava/lang/String;");
	MultiPolygon_getLayer = getGlobalJniEnv()->GetMethodID(MultiPolygonClass, "getLayer", "()I");
	MultiPolygon_getPoint31XTile = getGlobalJniEnv()->GetMethodID(MultiPolygonClass, "getPoint31XTile", "(II)I");
	MultiPolygon_getPoint31YTile = getGlobalJniEnv()->GetMethodID(MultiPolygonClass, "getPoint31YTile", "(II)I");
	MultiPolygon_getBoundsCount = getGlobalJniEnv()->GetMethodID(MultiPolygonClass, "getBoundsCount", "()I");
	MultiPolygon_getBoundPointsCount = getGlobalJniEnv()->GetMethodID(MultiPolygonClass, "getBoundPointsCount", "(I)I");

	BinaryMapDataObjectClass = findClass("net/osmand/binary/BinaryMapDataObject");
	BinaryMapDataObject_getPointsLength = getGlobalJniEnv()->GetMethodID(BinaryMapDataObjectClass, "getPointsLength", "()I");
	BinaryMapDataObject_getPoint31YTile = getGlobalJniEnv()->GetMethodID(BinaryMapDataObjectClass, "getPoint31YTile", "(I)I");
	BinaryMapDataObject_getPoint31XTile = getGlobalJniEnv()->GetMethodID(BinaryMapDataObjectClass, "getPoint31XTile", "(I)I");
	BinaryMapDataObject_getHighwayAttributes = getGlobalJniEnv()->GetMethodID(BinaryMapDataObjectClass, "getHighwayAttributes", "()I");
	BinaryMapDataObject_getTypes = getGlobalJniEnv()->GetMethodID(BinaryMapDataObjectClass, "getTypes", "()[I");
	BinaryMapDataObject_getName = getGlobalJniEnv()->GetMethodID(BinaryMapDataObjectClass, "getName", "()Ljava/lang/String;");
	BinaryMapDataObject_getTagValue = getGlobalJniEnv()->GetMethodID(BinaryMapDataObjectClass, "getTagValue",
			"(I)Lnet/osmand/binary/BinaryMapIndexReader$TagValuePair;");

	TagValuePairClass = findClass("net/osmand/binary/BinaryMapIndexReader$TagValuePair");
	TagValuePair_tag = getGlobalJniEnv()->GetFieldID(TagValuePairClass, "tag", "Ljava/lang/String;");
	TagValuePair_value = getGlobalJniEnv()->GetFieldID(TagValuePairClass, "value", "Ljava/lang/String;");

}


void unloadJniMapObjects()
{
	getGlobalJniEnv()->DeleteGlobalRef( MultiPolygonClass );
	getGlobalJniEnv()->DeleteGlobalRef( BinaryMapDataObjectClass );
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
