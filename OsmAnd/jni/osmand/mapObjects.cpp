#include <jni.h>
#include <vector>

#include "common.cpp"


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
jmethodID BinaryMapDataObject_getTypes;
jmethodID BinaryMapDataObject_getName;
jmethodID BinaryMapDataObject_getTagValue;

jclass TagValuePairClass;
jfieldID TagValuePair_tag;
jfieldID TagValuePair_value;

class BaseMapDataObject
{
public :
    const int type;
    static const int MAP_DATA_OBJECT = 1;
    static const int MULTI_POLYGON = 2;
protected :
	BaseMapDataObject(int t) : type(t) { }

};

class MultiPolygonObject : BaseMapDataObject
{
public:
	MultiPolygonObject() : BaseMapDataObject(MULTI_POLYGON)	{	}
	std::string tag;
	std::string value;
	std::vector< std::string > names;
	int layer;
	std::vector< std::vector< std::pair<int, int> > > points;
};

class MapDataObject : BaseMapDataObject
{
public:
	MapDataObject() : BaseMapDataObject(MAP_DATA_OBJECT)	{	}

	std::string name;
	std::vector< int>  types;
	std::vector< std::pair<int, int> >  points;
	std::vector< std::pair<std::string, std::string> >  tagValues;
};


std::vector <BaseMapDataObject* > marshalObjects(jobjectArray binaryMapDataObjects)
{
	std::vector<BaseMapDataObject*> v;

	const size_t size = env->GetArrayLength(binaryMapDataObjects);
	size_t i = 0;
	for (; i < size; i++) {
		jobject binaryMapDataObject = (jobject) env->GetObjectArrayElement(binaryMapDataObjects, i);
		if (env->IsInstanceOf(binaryMapDataObject, MultiPolygonClass)) {
			MultiPolygonObject* o = new MultiPolygonObject();
			v.push_back((BaseMapDataObject* )o);
			o->layer = env->CallIntMethod(binaryMapDataObject, MultiPolygon_getLayer);
			o->tag = getStringMethod(binaryMapDataObject, MultiPolygon_getTag);
			o->value = getStringMethod(binaryMapDataObject, MultiPolygon_getValue);

			int boundsCount = env->CallIntMethod(binaryMapDataObject, MultiPolygon_getBoundsCount);
			for (int ji = 0; ji < boundsCount; ji++) {
				int cnt = env->CallIntMethod(binaryMapDataObject, MultiPolygon_getBoundPointsCount, ji);
				std::vector<std::pair<int, int> > vs;
				for (int js = 0; js < cnt; js++) {
					int xt = env->CallIntMethod(binaryMapDataObject, MultiPolygon_getPoint31XTile, js, ji);
					int yt = env->CallIntMethod(binaryMapDataObject, MultiPolygon_getPoint31YTile, js, ji);
					vs.push_back( std::pair<int, int> (xt, yt) );
				}

				o->points.push_back(vs);
				o->names.push_back(getStringMethod(binaryMapDataObject, MultiPolygon_getName, ji));
			}



		} else {
			jintArray types = (jintArray) env->CallObjectMethod(binaryMapDataObject, BinaryMapDataObject_getTypes);
			if (types != NULL) {
				MapDataObject* o = new MapDataObject();
				jint sizeTypes = env->GetArrayLength(types);
				jint* els = env->GetIntArrayElements(types, NULL);
				int j = 0;
				for (; j < sizeTypes; j++) {
					int wholeType = els[j];
					o->types.push_back(wholeType);
					jobject pair = env->CallObjectMethod(binaryMapDataObject, BinaryMapDataObject_getTagValue, j);
					if (pair != NULL) {
						std::string tag = getStringField(pair, TagValuePair_tag);
						std::string value = getStringField(pair, TagValuePair_value);
						o->tagValues.push_back( std::pair<std:: string, std::string>(tag, value));
						env->DeleteLocalRef(pair);
					} else {
						o->tagValues.push_back( std::pair<std:: string, std::string>(EMPTY_STRING, EMPTY_STRING));
					}
				}

				jint sizePoints = env->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPointsLength);
				for (j = 0; j < sizePoints; j++) {
					int tx = env->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPoint31XTile, j);
					int ty = env->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPoint31YTile, j);
					o->points.push_back(std::pair<int, int>(tx, ty));
				}
				o->name = getStringMethod(binaryMapDataObject, BinaryMapDataObject_getName);
				env->ReleaseIntArrayElements(types, els, JNI_ABORT);
				env->DeleteLocalRef(types);
				v.push_back((BaseMapDataObject* )o);
			}
		}
		env->DeleteLocalRef(binaryMapDataObject);
	}

	return v;
}

void  deleteObjects(std::vector <BaseMapDataObject* > & v)
{
	for(size_t i = 0; i< v.size(); i++)
	{
		delete v.at(i);
	}
	v.clear();
}

void loadJniMapObjects()
{
	MultiPolygonClass = globalRef(env->FindClass("net/osmand/osm/MultyPolygon"));
	MultiPolygon_getTag = env->GetMethodID(MultiPolygonClass, "getTag", "()Ljava/lang/String;");
	MultiPolygon_getValue = env->GetMethodID(MultiPolygonClass, "getValue", "()Ljava/lang/String;");
	MultiPolygon_getName = env->GetMethodID(MultiPolygonClass, "getName", "(I)Ljava/lang/String;");
	MultiPolygon_getLayer = env->GetMethodID(MultiPolygonClass, "getLayer", "()I");
	MultiPolygon_getPoint31XTile = env->GetMethodID(MultiPolygonClass, "getPoint31XTile", "(II)I");
	MultiPolygon_getPoint31YTile = env->GetMethodID(MultiPolygonClass, "getPoint31YTile", "(II)I");
	MultiPolygon_getBoundsCount = env->GetMethodID(MultiPolygonClass, "getBoundsCount", "()I");
	MultiPolygon_getBoundPointsCount = env->GetMethodID(MultiPolygonClass, "getBoundPointsCount", "(I)I");

	BinaryMapDataObjectClass = globalRef(env->FindClass("net/osmand/binary/BinaryMapDataObject"));
	BinaryMapDataObject_getPointsLength = env->GetMethodID(BinaryMapDataObjectClass, "getPointsLength", "()I");
	BinaryMapDataObject_getPoint31YTile = env->GetMethodID(BinaryMapDataObjectClass, "getPoint31YTile", "(I)I");
	BinaryMapDataObject_getPoint31XTile = env->GetMethodID(BinaryMapDataObjectClass, "getPoint31XTile", "(I)I");
	BinaryMapDataObject_getTypes = env->GetMethodID(BinaryMapDataObjectClass, "getTypes", "()[I");
	BinaryMapDataObject_getName = env->GetMethodID(BinaryMapDataObjectClass, "getName", "()Ljava/lang/String;");
	BinaryMapDataObject_getTagValue = env->GetMethodID(BinaryMapDataObjectClass, "getTagValue",
			"(I)Lnet/osmand/binary/BinaryMapIndexReader$TagValuePair;");

	TagValuePairClass = globalRef(env->FindClass("net/osmand/binary/BinaryMapIndexReader$TagValuePair"));
	TagValuePair_tag = env->GetFieldID(TagValuePairClass, "tag", "Ljava/lang/String;");
	TagValuePair_value = env->GetFieldID(TagValuePairClass, "value", "Ljava/lang/String;");

}


void unloadJniMapObjects()
{
	env->DeleteGlobalRef( MultiPolygonClass );
	env->DeleteGlobalRef( BinaryMapDataObjectClass );
	env->DeleteGlobalRef( TagValuePairClass );
}
