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

	const size_t size = globalEnv()->GetArrayLength(binaryMapDataObjects);
	size_t i = 0;
	for (; i < size; i++) {
		jobject binaryMapDataObject = (jobject) globalEnv()->GetObjectArrayElement(binaryMapDataObjects, i);
		if (globalEnv()->IsInstanceOf(binaryMapDataObject, MultiPolygonClass)) {
			MultiPolygonObject* o = new MultiPolygonObject();
			v.push_back((BaseMapDataObject* )o);
			o->layer = globalEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getLayer);
			o->tag = getStringMethod(binaryMapDataObject, MultiPolygon_getTag);
			o->value = getStringMethod(binaryMapDataObject, MultiPolygon_getValue);

			int boundsCount = globalEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getBoundsCount);
			for (int ji = 0; ji < boundsCount; ji++) {
				int cnt = globalEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getBoundPointsCount, ji);
				std::vector<std::pair<int, int> > vs;
				for (int js = 0; js < cnt; js++) {
					int xt = globalEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getPoint31XTile, js, ji);
					int yt = globalEnv()->CallIntMethod(binaryMapDataObject, MultiPolygon_getPoint31YTile, js, ji);
					vs.push_back( std::pair<int, int> (xt, yt) );
				}

				o->points.push_back(vs);
				o->names.push_back(getStringMethod(binaryMapDataObject, MultiPolygon_getName, ji));
			}



		} else {
			jintArray types = (jintArray) globalEnv()->CallObjectMethod(binaryMapDataObject, BinaryMapDataObject_getTypes);
			if (types != NULL) {
				MapDataObject* o = new MapDataObject();
				jint sizeTypes = globalEnv()->GetArrayLength(types);
				jint* els = globalEnv()->GetIntArrayElements(types, NULL);
				int j = 0;
				for (; j < sizeTypes; j++) {
					int wholeType = els[j];
					o->types.push_back(wholeType);
					jobject pair = globalEnv()->CallObjectMethod(binaryMapDataObject, BinaryMapDataObject_getTagValue, j);
					if (pair != NULL) {
						std::string tag = getStringField(pair, TagValuePair_tag);
						std::string value = getStringField(pair, TagValuePair_value);
						o->tagValues.push_back( std::pair<std:: string, std::string>(tag, value));
						globalEnv()->DeleteLocalRef(pair);
					} else {
						o->tagValues.push_back( std::pair<std:: string, std::string>(EMPTY_STRING, EMPTY_STRING));
					}
				}

				jint sizePoints = globalEnv()->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPointsLength);
				for (j = 0; j < sizePoints; j++) {
					int tx = globalEnv()->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPoint31XTile, j);
					int ty = globalEnv()->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPoint31YTile, j);
					o->points.push_back(std::pair<int, int>(tx, ty));
				}
				o->name = getStringMethod(binaryMapDataObject, BinaryMapDataObject_getName);
				o->highwayAttributes = globalEnv()->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getHighwayAttributes);
				globalEnv()->ReleaseIntArrayElements(types, els, JNI_ABORT);
				globalEnv()->DeleteLocalRef(types);
				v.push_back((BaseMapDataObject* )o);
			}
		}
		globalEnv()->DeleteLocalRef(binaryMapDataObject);
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
	MultiPolygonClass = globalRef(globalEnv()->FindClass("net/osmand/osm/MultyPolygon"));
	MultiPolygon_getTag = globalEnv()->GetMethodID(MultiPolygonClass, "getTag", "()Ljava/lang/String;");
	MultiPolygon_getValue = globalEnv()->GetMethodID(MultiPolygonClass, "getValue", "()Ljava/lang/String;");
	MultiPolygon_getName = globalEnv()->GetMethodID(MultiPolygonClass, "getName", "(I)Ljava/lang/String;");
	MultiPolygon_getLayer = globalEnv()->GetMethodID(MultiPolygonClass, "getLayer", "()I");
	MultiPolygon_getPoint31XTile = globalEnv()->GetMethodID(MultiPolygonClass, "getPoint31XTile", "(II)I");
	MultiPolygon_getPoint31YTile = globalEnv()->GetMethodID(MultiPolygonClass, "getPoint31YTile", "(II)I");
	MultiPolygon_getBoundsCount = globalEnv()->GetMethodID(MultiPolygonClass, "getBoundsCount", "()I");
	MultiPolygon_getBoundPointsCount = globalEnv()->GetMethodID(MultiPolygonClass, "getBoundPointsCount", "(I)I");

	BinaryMapDataObjectClass = globalRef(globalEnv()->FindClass("net/osmand/binary/BinaryMapDataObject"));
	BinaryMapDataObject_getPointsLength = globalEnv()->GetMethodID(BinaryMapDataObjectClass, "getPointsLength", "()I");
	BinaryMapDataObject_getPoint31YTile = globalEnv()->GetMethodID(BinaryMapDataObjectClass, "getPoint31YTile", "(I)I");
	BinaryMapDataObject_getPoint31XTile = globalEnv()->GetMethodID(BinaryMapDataObjectClass, "getPoint31XTile", "(I)I");
	BinaryMapDataObject_getHighwayAttributes = globalEnv()->GetMethodID(BinaryMapDataObjectClass, "getHighwayAttributes", "()I");
	BinaryMapDataObject_getTypes = globalEnv()->GetMethodID(BinaryMapDataObjectClass, "getTypes", "()[I");
	BinaryMapDataObject_getName = globalEnv()->GetMethodID(BinaryMapDataObjectClass, "getName", "()Ljava/lang/String;");
	BinaryMapDataObject_getTagValue = globalEnv()->GetMethodID(BinaryMapDataObjectClass, "getTagValue",
			"(I)Lnet/osmand/binary/BinaryMapIndexReader$TagValuePair;");

	TagValuePairClass = globalRef(globalEnv()->FindClass("net/osmand/binary/BinaryMapIndexReader$TagValuePair"));
	TagValuePair_tag = globalEnv()->GetFieldID(TagValuePairClass, "tag", "Ljava/lang/String;");
	TagValuePair_value = globalEnv()->GetFieldID(TagValuePairClass, "value", "Ljava/lang/String;");

}


void unloadJniMapObjects()
{
	globalEnv()->DeleteGlobalRef( MultiPolygonClass );
	globalEnv()->DeleteGlobalRef( BinaryMapDataObjectClass );
	globalEnv()->DeleteGlobalRef( TagValuePairClass );
}

#endif /*_OSMAND_MAP_OBJECTS*/
