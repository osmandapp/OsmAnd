#ifndef _OSMAND_BINARY_READ
#define _OSMAND_BINARY_READ

#include <math.h>
#include <android/log.h>
#include <stdio.h>
#include <fstream>
#include <algorithm>
#include <map>
#include <hash_map>
#include <hash_set>
#include "google/protobuf/io/zero_copy_stream_impl.h"
#include "google/protobuf/wire_format_lite.h"
#include "google/protobuf/wire_format_lite.cc"

#include "renderRules.h"
#include "common.h"
#include "mapObjects.h"
//#include "multipolygons.h"
#include "proto/osmand_odb.pb.h"

#define DO_(EXPRESSION) if (!(EXPRESSION)) return false
using namespace google::protobuf;
using namespace google::protobuf::internal;

static const int MAP_VERSION = 2;



struct BinaryMapFile;
std::map<std::string, BinaryMapFile*> openFiles;

inline bool readInt(io::CodedInputStream* input, uint32* sz) {
	uint8 buf[4];
	if (!input->ReadRaw(buf, 4)) {
		return false;
	}
	*sz = ((buf[0] << 24) + (buf[1] << 16) + (buf[2] << 8) + (buf[3] << 0));
	return true;
}

bool skipFixed32(io::CodedInputStream* input) {
	uint32 sz;
	if (!readInt(input, &sz)) {
		return false;
	}
	return input->Skip(sz);
}

bool skipUnknownFields(io::CodedInputStream* input, int tag) {
	if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_FIXED32_LENGTH_DELIMITED) {
		if (!skipFixed32(input)) {
			return false;
		}
	} else if (!WireFormatLite::SkipField(input, tag)) {
		return false;
	}
	return true;
}


struct SearchQuery {
	RenderingRuleSearchRequest* req;
	int left;
	int right;
	int top;
	int bottom;
	int zoom;
	std::vector< MapDataObject*> result;
	JNIEnv* env;

	jobject o;
	jfieldID interruptedField;

	coordinates cacheCoordinates;
	bool ocean;
	bool land;

	int numberOfVisitedObjects;
	int numberOfAcceptedObjects;
	int numberOfReadSubtrees;
	int numberOfAcceptedSubtrees;

	SearchQuery(int l, int r, int t, int b, RenderingRuleSearchRequest* req, jobject o,	jfieldID interruptedField, JNIEnv* env) :
			req(req), left(l), right(r), top(t), bottom(b), o(o), interruptedField(interruptedField) {
		numberOfAcceptedObjects = numberOfVisitedObjects = 0;
		numberOfAcceptedSubtrees = numberOfReadSubtrees = 0;
		ocean = land = false;
		this->env = env;
	}

	bool isCancelled(){
		if(env != NULL) {
			return env->GetBooleanField(o, interruptedField);
		}
		return false;
	}
};

struct MapTreeBounds {
	uint32 length;
	uint32 filePointer;
	uint32 mapDataBlock;
	uint32 left ;
	uint32 right ;
	uint32 top ;
	uint32 bottom;
	bool ocean;

	MapTreeBounds() {
		ocean = -1;
	}
};

struct MapRoot: MapTreeBounds {
	int minZoom ;
	int maxZoom ;
	vector<MapTreeBounds> bounds;
};

struct MapIndex {
	uint32 length;
	int filePointer;
	std::string name;
	vector<MapRoot> levels;

	std::hash_map<int, tag_value > decodingRules;
	// DEFINE hash
	//std::hash_map<tag_value, int> encodingRules;

	int nameEncodingType;
	int refEncodingType;
	int coastlineEncodingType;
	int coastlineBrokenEncodingType;
	int landEncodingType;
	int onewayAttribute ;
	int onewayReverseAttribute ;
	std::hash_set< int > positiveLayers;
	std::hash_set< int > negativeLayers;

	MapIndex(){
		nameEncodingType = refEncodingType = coastlineBrokenEncodingType = coastlineEncodingType = -1;
		landEncodingType = onewayAttribute = onewayReverseAttribute = -1;
	}

	void finishInitializingTags() {
		int free = decodingRules.size() * 2 + 1;
		coastlineBrokenEncodingType = free++;
		initMapEncodingRule(0, coastlineBrokenEncodingType, "natural", "coastline_broken");
		if (landEncodingType == -1) {
			landEncodingType = free++;
			initMapEncodingRule(0, landEncodingType, "natural", "land");
		}
	}

	void initMapEncodingRule(uint32 type, uint32 id, std::string tag, std::string val) {
		tag_value pair = tag_value(tag, val);
		// DEFINE hash
		//encodingRules[pair] = id;
		decodingRules[id] = pair;

		if ("name" == tag) {
			nameEncodingType = id;
		} else if ("natural" == tag && "coastline" == val) {
			coastlineEncodingType = id;
		} else if ("natural" == tag && "land" == val) {
			landEncodingType = id;
		} else if ("oneway" == tag && "yes" == val) {
			onewayAttribute = id;
		} else if ("oneway" == tag && "-1" == val) {
			onewayReverseAttribute = id;
		} else if ("ref" == tag) {
			refEncodingType = id;
		} else if ("layer" == tag) {
			if (val != "" && val != "0") {
				if (val[0] == '-') {
					negativeLayers.insert(id);
				} else {
					positiveLayers.insert(id);
				}
			}
		}
	}
};

struct BinaryMapFile {
	std::string inputName;
	vector<MapIndex> mapIndexes;
	FILE* f;

	~BinaryMapFile() {
		fclose(f);
	}
};

bool readMapTreeBounds(io::CodedInputStream* input, MapTreeBounds* tree, MapRoot* root) {
	int init = 0;
	int tag;
	int32 si;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case OsmAndMapIndex_MapDataBox::kLeftFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &si)));
			tree->left = si + root->left;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kRightFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &si)));
			tree->right = si + root->right;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kTopFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &si)));
			tree->top = si + root->top;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kBottomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &si)));
			tree->bottom = si + root->bottom;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kOceanFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<bool, WireFormatLite::TYPE_BOOL>(input, &tree->ocean)));
			break;
		}
		default: {
			if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_END_GROUP) {
				return true;
			}
			if (!skipUnknownFields(input, tag)) {
				return false;
			}
			break;
		}
		}
		if (init == 0xf) {
			return true;
		}
	}
	return true;
}

bool readMapLevel(io::CodedInputStream* input, MapRoot* root) {
	int tag;
	int si;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case OsmAndMapIndex_MapRootLevel::kMaxZoomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &root->maxZoom)));
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kMinZoomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &root->minZoom)));
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kBottomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &si)));
			root->bottom = si;
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kTopFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &si)));
			root->top = si;
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kLeftFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &si)));
			root->left = si;
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kRightFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &si)));
			root->right = si;
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kBoxesFieldNumber: {
			MapTreeBounds bounds;
			readInt(input, &bounds.length);
			bounds.filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(bounds.length);
			readMapTreeBounds(input, &bounds, root);
			root->bounds.push_back(bounds);
			input->Skip(input->BytesUntilLimit());
			input->PopLimit(oldLimit);
			break;
		}

		case OsmAndMapIndex_MapRootLevel::kBlocksFieldNumber: {
			input->Skip(input->BytesUntilLimit());
			break;
		}

		default: {
			if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_END_GROUP) {
				return true;
			}
			if (!skipUnknownFields(input, tag)) {
				return false;
			}
			break;
		}
		}
	}
	return true;
}

bool readMapEncodingRule(io::CodedInputStream* input, MapIndex* index, uint32 id) {
	int tag;
	std::string tagS;
	std::string value;
	uint32 type = 0;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case OsmAndMapIndex_MapEncodingRule::kValueFieldNumber: {
			DO_((WireFormatLite::ReadString(input, &value)));
			break;
		}
		case OsmAndMapIndex_MapEncodingRule::kTagFieldNumber: {
			DO_((WireFormatLite::ReadString(input, &tagS)));
			break;
		}
		case OsmAndMapIndex_MapEncodingRule::kTypeFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &type)));
			break;
		}
		case OsmAndMapIndex_MapEncodingRule::kIdFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &id)));
			break;
		}
		default: {
			if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_END_GROUP) {
				return true;
			}
			if (!skipUnknownFields(input, tag)) {
				return false;
			}
			break;
		}
		}
	}
	// Special case for check to not replace primary with primary_link
	index->initMapEncodingRule(type, id, tagS, value);
	return true;
}

bool readMapIndex(io::CodedInputStream* input, MapIndex* mapIndex) {
	uint32 tag;
	uint32 defaultId = 1;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case OsmAndMapIndex::kNameFieldNumber: {
			DO_((WireFormatLite::ReadString(input, &mapIndex->name)));
			break;
		}
		case OsmAndMapIndex::kRulesFieldNumber: {
			int len;
			WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &len);
			int oldLimit = input->PushLimit(len);
			readMapEncodingRule(input, mapIndex, defaultId++);
			input->PopLimit(oldLimit);
			break;
		}
		case OsmAndMapIndex::kLevelsFieldNumber: {
			MapRoot mapLevel;
			readInt(input, &mapLevel.length);
			mapLevel.filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(mapLevel.length);
			readMapLevel(input, &mapLevel);
			input->PopLimit(oldLimit);
			input->Seek(mapLevel.filePointer + mapLevel.length);
			mapIndex->levels.push_back(mapLevel);
			break;
		}
		default: {
			if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_END_GROUP) {
				return true;
			}
			if (!skipUnknownFields(input, tag)) {
				return false;
			}
			break;
		}
		}
	}
	mapIndex->finishInitializingTags();
	return true;
}


//display google::protobuf::internal::WireFormatLite::GetTagWireType(tag)
// display google::protobuf::internal::WireFormatLite::GetTagFieldNumber(tag)
bool initMapStructure(io::CodedInputStream* input, BinaryMapFile* file) {
	uint32 tag;
	uint32 version = -1;
	uint32 versionConfirm = -2;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		// required uint32 version = 1;
		case OsmAndStructure::kVersionFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &version)));
			break;
		}
		case OsmAndStructure::kMapIndexFieldNumber: {
			MapIndex mapIndex;
			readInt(input, &mapIndex.length);
			mapIndex.filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(mapIndex.length);
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Map index %d length %d", mapIndex.filePointer, mapIndex.length);
			readMapIndex(input, &mapIndex);
			input->PopLimit(oldLimit);
			input->Seek(mapIndex.filePointer + mapIndex.length);
			file->mapIndexes.push_back(mapIndex);
			break;
		}
		case OsmAndStructure::kVersionConfirmFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &versionConfirm)));
			break;
		}
		default: {
			if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_END_GROUP) {
				return true;
			}
			if (!skipUnknownFields(input, tag)) {
				return false;
			}
			break;
		}
		}
	}
	if (version != versionConfirm) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Corrupted file. It should be ended as it starts with version");
		return false;
	}
	if (version != MAP_VERSION) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Version of the file is not supported.");
		return false;
	}
	return true;
}


extern "C" JNIEXPORT void JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_deleteSearchResult(JNIEnv* ienv,
		jobject obj, jint searchResult) {
	SearchResult* result = (SearchResult*) searchResult;
	if(result != NULL){
		deleteObjects(result->basemapCoastLines);
		deleteObjects(result->result);
		deleteObjects(result->coastLines);
		deleteObjects(result->result);
		delete result;
	}
}

bool readStringTable(io::CodedInputStream* input, std::vector<std::string>& list) {
	uint32 tag;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case StringTable::kSFieldNumber: {
			std::string s;
			WireFormatLite::ReadString(input, &s);
			list.push_back(s);
			break;
		}
		default: {
			if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_END_GROUP) {
				return false;
			}
			if (!skipUnknownFields(input, tag)) {
				return false;
			}
			break;
		}
		}
	}
	return true;
}

static const int SHIFT_COORDINATES = 5;
static const int MASK_TO_READ = ~((1 << SHIFT_COORDINATES) - 1);

bool acceptTypes(SearchQuery* req, std::vector<tag_value>& types, MapIndex* root) {
	RenderingRuleSearchRequest* r = req->req;
	bool accept = true;
	for (std::vector<tag_value>::iterator type = types.begin(); type != types.end(); type++) {
		for (int i = 1; i <= 3; i++) {
			r->setIntFilter(r->props()->R_MINZOOM, req->zoom);
			r->setStringFilter(r->props()->R_TAG, type->first);
			r->setStringFilter(r->props()->R_VALUE, type->second);
			if (r->search(i, false)) {
				return true;
			}
		}
		r->setStringFilter(r->props()->R_TAG, type->first);
		r->setStringFilter(r->props()->R_VALUE, type->second);
		if (r->search(RenderingRulesStorage::TEXT_RULES, false)) {
			return true;
		}
	}
	return false;
}

MapDataObject* readMapDataObject(io::CodedInputStream* input, MapTreeBounds* tree, SearchQuery* req,
			MapIndex* root) {
	uint32 tag = WireFormatLite::GetTagFieldNumber(input->ReadTag());
	bool area = MapData::kAreaCoordinatesFieldNumber == tag;
	if(!area && MapData::kCoordinatesFieldNumber != tag) {
		return NULL;
	}
	req->cacheCoordinates.clear();
	uint32 size;
	input->ReadVarint32(&size);
	int old = input->PushLimit(size);
	int px = tree->left & MASK_TO_READ;
	int py = tree->top & MASK_TO_READ;
	bool contains = false;
	long long id = 0;
	int minX = INT_MAX;
	int maxX = 0;
	int minY = INT_MAX;
	int maxY = 0;
	req->numberOfVisitedObjects++;
	int x;
	int y;
	while (input->BytesUntilLimit() > 0) {
		if (!WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &x)) {
			return NULL;
		}
		if (!WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &y)) {
			return NULL;
		}
		x = (x << SHIFT_COORDINATES) + px;
		y = (y << SHIFT_COORDINATES) + py;
		req->cacheCoordinates.push_back(std::pair<int, int>(x, y));
		px = x;
		py = y;
		if (!contains && req->left <= x && req->right >= x && req->top <= y && req->bottom >= y) {
			contains = true;
		}
		if (!contains) {
			minX = min(minX, x);
			maxX = max(maxX, x);
			minY = min(minY, y);
			maxY = max(maxY, y);
		}
	}
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Object %d %d %d %d", maxX, minX, minY, maxY);
	if (!contains) {
		if (maxX >= req->left && minX <= req->right && minY <= req->bottom && maxY >= req->top) {
			contains = true;
		}
	}
	input->PopLimit(old);
	if (!contains) {
		return NULL;
	}

	// READ types
	std::vector< coordinates > innercoordinates;
	std::vector< tag_value > additionalTypes;
	std::vector< tag_value > types;
	std::hash_map< std::string, unsigned int> stringIds;
	bool loop = true;
	while (loop) {
		uint32 t = input->ReadTag();
		switch (WireFormatLite::GetTagFieldNumber(t)) {
		case 0:
			loop = false;
			break;
		case MapData::kPolygonInnerCoordinatesFieldNumber: {
			coordinates polygon;

			px = tree->left & MASK_TO_READ;
			py = tree->top & MASK_TO_READ;
			input->ReadVarint32(&size);
			old = input->PushLimit(size);
			while (input->BytesUntilLimit() > 0) {
				WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &x);
				WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &y);
				x = (x << SHIFT_COORDINATES) + px;
				y = (y << SHIFT_COORDINATES) + py;
				polygon.push_back(std::pair<int, int>(x, y));
				px = x;
				py = y;
			}
			input->PopLimit(old);
			innercoordinates.push_back(polygon);
			break;
		}
		case MapData::kAdditionalTypesFieldNumber: {
			input->ReadVarint32(&size);
			old = input->PushLimit(size);
			while (input->BytesUntilLimit() > 0) {
				WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &x);
				if (root->decodingRules.find(x) != root->decodingRules.end()) {
					tag_value t = root->decodingRules[x];
					additionalTypes.push_back(t);
				}
			}
			input->PopLimit(old);
			break;
		}
		case MapData::kTypesFieldNumber: {
			input->ReadVarint32(&size);
			old = input->PushLimit(size);
			while (input->BytesUntilLimit() > 0) {
				WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &x);
				if (root->decodingRules.find(x) != root->decodingRules.end()) {
					tag_value t = root->decodingRules[x];
					types.push_back(t);
				}
			}
			input->PopLimit(old);
			bool acceptTps = acceptTypes(req, types, root);
			if (!acceptTps) {
				return NULL;
			}
			break;
		}
		case MapData::kIdFieldNumber:
			WireFormatLite::ReadPrimitive<int64, WireFormatLite::TYPE_SINT64>(input, &id);
			break;
		case MapData::kStringNamesFieldNumber:
			input->ReadVarint32(&size);
			old = input->PushLimit(size);
			while (input->BytesUntilLimit() > 0) {
				WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &x);
				WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &y);
				if (root->decodingRules.find(x) != root->decodingRules.end()) {
					tag_value t = root->decodingRules[x];
					stringIds[t.first] = y;
				}
			}
			input->PopLimit(old);
			break;
		default: {
			if (WireFormatLite::GetTagWireType(t) == WireFormatLite::WIRETYPE_END_GROUP) {
				return false;
			}
			if (!skipUnknownFields(input, t)) {
				return false;
			}
			break;
		}
		}
	}


	req->numberOfAcceptedObjects++;

	MapDataObject* dataObject = new MapDataObject();
	dataObject->points = req->cacheCoordinates;
	dataObject->additionalTypes = additionalTypes;
	dataObject->types = types;
	dataObject->id = id;
	dataObject->stringIds = stringIds;
	dataObject->polygonInnerCoordinates = innercoordinates;


	return dataObject;

}



bool searchMapTreeBounds(io::CodedInputStream* input, MapTreeBounds* current, MapTreeBounds* parent,
		SearchQuery* req, std::vector<MapTreeBounds>* foundSubtrees) {
	int init = 0;
	int tag;
	int si;
	req->numberOfReadSubtrees++;
	while ((tag = input->ReadTag()) != 0) {
		if (req->isCancelled()) {
			return false;
		}
		if (init == 0xf) {
			init = 0;
			// coordinates are init
			if (current->right < req->left || current->left > req->right || current->top > req->bottom || current->bottom < req->top) {
				return false;
			} else {
				req->numberOfAcceptedSubtrees++;
			}
		}
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		// required uint32 version = 1;
		case OsmAndMapIndex_MapDataBox::kLeftFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &si)));
			current->left = si + parent->left;
			init |= 1;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kRightFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &si)));
			current->right = si + parent->right;
			init |= 2;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kTopFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &si)));
			current->top = si + parent->top;
			init |= 4;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kBottomFieldNumber : {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &si)));
			current->bottom = si +  parent->bottom;
			init |= 8;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kShiftToMapDataFieldNumber : {
			readInt(input, &current->mapDataBlock);
			current->mapDataBlock += current->filePointer;
			foundSubtrees->push_back(*current);
			break;
		}
		case OsmAndMapIndex_MapDataBox::kOceanFieldNumber : {
			DO_((WireFormatLite::ReadPrimitive<bool, WireFormatLite::TYPE_BOOL>(input, &current->ocean)));
			if(current->ocean){
				req->ocean = true;
			} else {
				req->land = true;
			}
			break;
		}
		case OsmAndMapIndex_MapDataBox::kBoxesFieldNumber: {
			MapTreeBounds* child = new MapTreeBounds();
			readInt(input, &child->length);
			child->filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(child->length);
			if (current->ocean != -1) {
				child->ocean = current->ocean;
			}
			searchMapTreeBounds(input, child, current, req, foundSubtrees);
			input->PopLimit(oldLimit);
			input->Seek(child->filePointer + child->length);
			delete child;
			break;
		}
		default: {
			if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_END_GROUP) {
				return true;
			}
			if (!skipUnknownFields(input, tag)) {
				return false;
			}
			break;
		}
		}
	}
	return true;
}

bool readMapDataBlocks(io::CodedInputStream* input, SearchQuery* req, MapTreeBounds* tree, MapIndex* root) {
	long long baseId = 0;
	int tag;
	std::vector< MapDataObject* > results;
	while ((tag = input->ReadTag()) != 0) {
		if (req->isCancelled()) {
			return false;
		}
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		// required uint32 version = 1;
		case MapDataBlock::kBaseIdFieldNumber : {
			WireFormatLite::ReadPrimitive<int64, WireFormatLite::TYPE_SINT64>(input, &baseId);
			break;
		}
		case MapDataBlock::kStringTableFieldNumber: {
			uint32 length;
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			if(results.size() > 0) {
				std::vector<std::string> stringTable;
				readStringTable(input, stringTable);
				MapDataObject* o;
				for (std::vector<MapDataObject*>::iterator obj = results.begin(); obj != results.end(); obj++) {
					if ((*obj)->stringIds.size() > 0) {
						std::hash_map<std::string, unsigned int >::iterator  val=(*obj)->stringIds.begin();
						while(val != (*obj)->stringIds.end()){
							(*obj)->objectNames[val->first]=stringTable.at(val->second);
							val++;
						}
					}
				}
			}
			input->Skip(input->BytesUntilLimit());
			input->PopLimit(oldLimit);
			break;
		}
		case MapDataBlock::kDataObjectsFieldNumber: {
			uint32 length;
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			MapDataObject* mapObject = readMapDataObject(input, tree, req, root);
			if (mapObject != NULL) {
				mapObject->id += baseId;
				results.push_back(mapObject);
				req->result.push_back(mapObject);
			}
			input->Skip(input->BytesUntilLimit());
			input->PopLimit(oldLimit);
			break;
		}
		default: {
			if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_END_GROUP) {
				return true;
			}
			if (!skipUnknownFields(input, tag)) {
				return false;
			}
			break;
		}
		}
	}
	return true;
}

bool sortTreeBounds (const MapTreeBounds& i,const MapTreeBounds& j) { return (i.mapDataBlock<j.mapDataBlock); }

void searchMapData(io::CodedInputStream* input, MapRoot* root, MapIndex* ind, SearchQuery* req) {
	// search
	for (std::vector<MapTreeBounds>::iterator i = root->bounds.begin();
			i != root->bounds.end(); i++) {
		if (req->isCancelled()) {
			return;
		}
		if (i->right < req->left || i->left > req->right || i->top > req->bottom || i->bottom < req->top) {
			continue;
		}
		vector<MapTreeBounds> foundSubtrees;
		input->Seek(i->filePointer);
		int oldLimit = input->PushLimit(i->length);
		searchMapTreeBounds(input, i, root, req, &foundSubtrees);
		input->PopLimit(oldLimit);


		sort(foundSubtrees.begin(), foundSubtrees.end(), sortTreeBounds);
		uint32 length;
		for (std::vector<MapTreeBounds>::iterator tree = foundSubtrees.begin();
					tree != foundSubtrees.end(); tree++) {
			if (req->isCancelled()) {
				return;
			}
			input->Seek(tree->mapDataBlock);
			WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &length);
			int oldLimit = input->PushLimit(length);
			readMapDataBlocks(input, req, tree, ind);
			input->PopLimit(oldLimit);
		}
	}

}

extern "C" JNIEXPORT jint JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_searchObjectsForRendering(JNIEnv* ienv,
		jobject obj, jint sleft, jint sright, jint stop, jint sbottom, jint zoom, jstring mapName,
		jobject renderingRuleSearchRequest, bool skipDuplicates, jint searchResult, jobject objInterrupted) {
	// TODO skipDuplicates not supported
	SearchResult* result = (SearchResult*) searchResult;
	if(result == NULL) {
		result = new SearchResult();
	}
	std::string map = getString(ienv, mapName);
	std::map<std::string, BinaryMapFile*>::iterator i = openFiles.find(map);
	if(i == openFiles.end()) {
		return (jint) result;
	}
	BinaryMapFile* file =  i->second;
	RenderingRuleSearchRequest* req = initSearchRequest(ienv, renderingRuleSearchRequest);
	jclass clObjInterrupted = ienv->GetObjectClass(objInterrupted);
	jfieldID interruptedField =  getFid(ienv, clObjInterrupted, "interrupted", "Z");
	ienv->DeleteLocalRef(clObjInterrupted);
	SearchQuery q(sleft,sright, stop, sbottom, req, objInterrupted, interruptedField, ienv);
	q.zoom = zoom;

	fseek(file->f, 0, 0);
	io::FileInputStream input(fileno(file->f));
	input.SetCloseOnDelete(false);
	io::CodedInputStream cis(&input);
	cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);
	if(req != NULL){
		req->clearState();
	}

	for(vector<MapIndex>::iterator mapIndex = file->mapIndexes.begin();
			mapIndex != file->mapIndexes.end(); mapIndex++) {
		for (vector<MapRoot>::iterator mapLevel = mapIndex->levels.begin(); mapLevel != mapIndex->levels.end();
				mapLevel++) {
			if (q.isCancelled()) {
				break;
			}
			if(mapLevel->minZoom <= zoom && mapLevel->maxZoom >= zoom) {
				if(mapLevel->right >= q.left &&  q.right >= mapLevel->left &&
						mapLevel->bottom >= q.top && q.bottom >= mapLevel->top) {
					searchMapData(&cis, mapLevel, mapIndex, &q);
				}
			}
		}
	}
	result->result.insert(result->result.end(), q.result.begin(), q.result.end());
	// FIXME process multi polygons
//	std::map<tagValueType, std::vector<MapDataObject*> > multyPolygons;
//	std::vector<MapDataObject*>::iterator mdo = q.result.begin();
//	for(;mdo!= q.result.end(); mdo++) {
//		for(size_t j = 0; j<(*mdo)->types.size(); j++) {
//			int type = (*mdo)->types.at(j);
//			if((type & 0x3) == RenderingRulesStorage::MULTI_POLYGON_TYPE) {
//				tagValueType tagValue((*mdo)->tagValues.at(j), type);
//				multyPolygons[tagValue].push_back(*mdo);
//			}
//		}
//	}
//
//	proccessMultiPolygons(multyPolygons, q.left, q.right, q.bottom, q.top, q.zoom, result->result);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Search : tree - read( %d), accept( %d), objs - visit( %d), accept(%d), in result(%d) ", q.numberOfReadSubtrees,
				q.numberOfAcceptedSubtrees, q.numberOfVisitedObjects, q.numberOfAcceptedObjects, result->result.size());
	if(q.result.size() > 0) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Search : tree - read( %d), accept( %d), objs - visit( %d), accept(%d), in result(%d) ", q.numberOfReadSubtrees,
			q.numberOfAcceptedSubtrees, q.numberOfVisitedObjects, q.numberOfAcceptedObjects, result->result.size());
	}
	delete req;
	return (jint)result;
}

extern "C" JNIEXPORT void JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_closeBinaryMapFile(JNIEnv* ienv,
		jobject path) {
	const char* utf = ienv->GetStringUTFChars((jstring) path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring) path, utf);
	std::map<std::string, BinaryMapFile*>::iterator iterator;
	if ((iterator = openFiles.find(inputName)) != openFiles.end()) {
		delete iterator->second;
		openFiles.erase(iterator);
	}
}

extern "C" JNIEXPORT jboolean JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_initBinaryMapFile(JNIEnv* ienv,
		jobject obj, jobject path) {
	// Verify that the version of the library that we linked against is
	const char* utf = ienv->GetStringUTFChars((jstring) path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring) path, utf);

	GOOGLE_PROTOBUF_VERIFY_VERSION;
	std::map<std::string, BinaryMapFile*>::iterator iterator;
	if ((iterator = openFiles.find(inputName)) != openFiles.end()) {
		delete iterator->second;
		openFiles.erase(iterator);
	}

	FILE* file = fopen(inputName.c_str(), "r");
	if (file == NULL) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "File could not be open to read from C : %s", inputName.c_str());
		return false;
	}
	BinaryMapFile* mapFile = new BinaryMapFile();
	mapFile->f = file;
	io::FileInputStream input(fileno(file));
	input.SetCloseOnDelete(false);
	io::CodedInputStream cis(&input);
	cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);
	if (!initMapStructure(&cis, mapFile)) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "File not initialised : %s", inputName.c_str());
		delete mapFile;
		return false;
	}
	mapFile->inputName = inputName;

	openFiles.insert(std::pair<std::string, BinaryMapFile*>(inputName, mapFile));
	return true;
}

#undef DO_
#endif
