#ifndef _OSMAND_BINARY_READ
#define _OSMAND_BINARY_READ

#include <math.h>
#include <android/log.h>
#include <stdio.h>
#include <fstream>
#include <map>
#include <hash_map>
#include "google/protobuf/io/zero_copy_stream_impl.h"
#include "google/protobuf/wire_format_lite.h"
#include "google/protobuf/wire_format_lite.cc"

#include "renderRules.h"
#include "common.h"
#include "mapObjects.h"
#include "multipolygons.h"
#include "proto/osmand_odb.pb.h"

char errorMsg[1024];
#define	INT_MAX		0x7fffffff	/* max value for an int */
#define DO_(EXPRESSION) if (!(EXPRESSION)) return false
using namespace google::protobuf;
using namespace google::protobuf::internal;

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

	jobject o;
	jfieldID interruptedField;

	std::vector<std::pair<int, int> > cacheCoordinates;
	std::vector<int> cacheTypes;
	std::vector<std::pair< std::string, std::string> > cacheTagValues;

	int numberOfVisitedObjects;
	int numberOfAcceptedObjects;
	int numberOfReadSubtrees;
	int numberOfAcceptedSubtrees;

	SearchQuery(int l, int r, int t, int b, RenderingRuleSearchRequest* req, jobject o,	jfieldID interruptedField) :
			req(req), left(l), right(r), top(t), bottom(b), o(o), interruptedField(interruptedField) {
		numberOfAcceptedObjects = numberOfVisitedObjects = 0;
		numberOfAcceptedSubtrees = numberOfReadSubtrees = 0;
	}

	bool isCancelled(){
		return globalEnv()->GetBooleanField(o, interruptedField);
	}
};

struct MapTreeBounds {
	uint32 length;
	int filePointer;
	int left ;
	int right ;
	int top ;
	int bottom;
};

struct MapRoot {
	uint32 length;
	int filePointer;
	int minZoom ;
	int maxZoom ;
	int left ;
	int right ;
	int top ;
	int bottom;
	vector<MapTreeBounds> bounds;
};

struct MapIndex {
	uint32 length;
	int filePointer;
	std::string name;
	std::hash_map<int, tag_value > decodingRules;
	vector<MapRoot> levels;
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
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case MapTree::kLeftFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &tree->left)));
			tree->left += root->left;
			break;
		}
		case MapTree::kRightFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &tree->right)));
			tree->right += root->right;
			break;
		}
		case MapTree::kTopFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &tree->top)));
			tree->top += root->top;
			break;
		}
		case MapTree::kBottomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &tree->bottom)));
			tree->bottom += root->bottom;
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
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case MapRootLevel::kMaxZoomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &root->maxZoom)));
			break;
		}
		case MapRootLevel::kMinZoomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &root->minZoom)));
			break;
		}
		case MapRootLevel::kBottomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &root->bottom)));
			break;
		}
		case MapRootLevel::kTopFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &root->top)));
			break;
		}
		case MapRootLevel::kLeftFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &root->left)));
			break;
		}
		case MapRootLevel::kRightFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &root->right)));
			break;
		}
		case MapRootLevel::kRootFieldNumber: {
			MapTreeBounds mapBounds;
			readInt(input, &mapBounds.length);
			mapBounds.filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(mapBounds.length);
			readMapTreeBounds(input, &mapBounds, root);
			input->Skip(input->BytesUntilLimit());
			input->PopLimit(oldLimit);
			root->bounds.push_back(mapBounds);
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

bool readMapEncodingRule(io::CodedInputStream* input, MapIndex* index) {
	int tag;
	std::string tagS;
	std::string value;
	uint32 type = 0;
	uint32  subtype = 0;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case MapEncodingRule::kValueFieldNumber: {
			DO_((WireFormatLite::ReadString(input, &value)));
			break;
		}
		case MapEncodingRule::kTagFieldNumber: {
			DO_((WireFormatLite::ReadString(input, &tagS)));
			break;
		}
		case MapEncodingRule::kTypeFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &type)));
			break;
		}
		case MapEncodingRule::kSubtypeFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &subtype)));
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
	int ind = ((subtype << 5) | type);
	// Special case for check to not replace primary with primary_link
	if(index->decodingRules.find(ind) ==  index->decodingRules.end()) {
		index->decodingRules[ind] = std::pair < std::string, std::string > (tagS, value);
	}
	return true;
}

bool readMapIndex(io::CodedInputStream* input, MapIndex* mapIndex) {
	uint32 tag;
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
			readMapEncodingRule(input, mapIndex);
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
		__android_log_print(ANDROID_LOG_WARN, "net.osmand",
				"Corrupted file. It should be ended as it starts with version");
		return false;
	}
	return true;
}

void loadJniBinaryRead() {
}


extern "C" JNIEXPORT void JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_deleteSearchResult(JNIEnv* ienv,
		jobject obj, jint searchResult) {
	setGlobalEnv(ienv);
	SearchResult* result = (SearchResult*) searchResult;
	if(result != NULL){
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
static const int MASK_10 = (1 << 10) - 1;

MapDataObject* readMapDataObject(io::CodedInputStream* input, int left, int right, int top, int bottom, SearchQuery* req,
			MapIndex* root) {
	uint32 tag = input->ReadTag();
	if (MapData::kCoordinatesFieldNumber != WireFormatLite::GetTagFieldNumber(tag)) {
		return NULL;
	}
	req->cacheCoordinates.clear();
	uint32 size;
	input->ReadVarint32(&size);
	int old = input->PushLimit(size);
	int px = left & MASK_TO_READ;
	int py = top & MASK_TO_READ;
	bool contains = false;
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
	tag = input->ReadTag();
	if (MapData::kTypesFieldNumber != WireFormatLite::GetTagFieldNumber(tag)) {
		return NULL;
	}
	req->cacheTypes.clear();
	req->cacheTagValues.clear();
	uint32 sizeL;
	input->ReadVarint32(&sizeL);
	unsigned char* buff = new unsigned char[sizeL];
	if (!input->ReadRaw(buff, sizeL)) {
		return NULL;
	}

	bool accept = false;
	RenderingRuleSearchRequest* r = req->req;
	for (uint32 i = 0; i < sizeL / 2; i++) {
		int s = (((int) buff[i * 2 + 1]) << 8) | buff[i * 2];
		int mask = s & 3;
		int type = s >> 2;
		if (mask != RenderingRulesStorage::POINT_RULES) {
			type = type  & MASK_10;
		}
		tag_value pair = root->decodingRules[type];
		if (r != NULL && !accept) {
			if(mask == RenderingRulesStorage::MULTI_POLYGON_TYPE){
				mask = RenderingRulesStorage::POLYGON_RULES;
			}
			r->setIntFilter(r->props()->R_MINZOOM, req->zoom);
			r->setStringFilter(r->props()->R_TAG, pair.first);
			r->setStringFilter(r->props()->R_VALUE, pair.second);
			accept |= r->search(mask, false);
			if (mask == RenderingRulesStorage::POINT_RULES && !accept) {
				r->setStringFilter(r->props()->R_TAG, pair.first);
				r->setStringFilter(r->props()->R_VALUE, pair.second);
				accept |= r->search(RenderingRulesStorage::TEXT_RULES, false);
			}
		} else {
			accept = true;
		}
		req->cacheTagValues.push_back(pair);
		req->cacheTypes.push_back(s);
	}
	delete buff;
	if (!accept) {
		return NULL;
	}

	req->numberOfAcceptedObjects++;

	MapDataObject* dataObject = new MapDataObject();
	dataObject->points = req->cacheCoordinates;
	dataObject->types = req->cacheTypes;
	dataObject->tagValues = req->cacheTagValues;

	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
//case MapData::kRestrictionsFieldNumber : {
//sizeL = codedIS.readRawVarint32();
//TLongArrayList list = new TLongArrayList();
//old = codedIS.pushLimit(sizeL);
//while(codedIS.getBytesUntilLimit() > 0){
//list.add(codedIS.readSInt64());
//}
//codedIS.popLimit(old);
//dataObject.restrictions = list.toArray();
//break; }
		case MapData::kHighwayMetaFieldNumber:
			WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_INT32>(input, &dataObject->highwayAttributes);
			break;
		case MapData::kIdFieldNumber:
			WireFormatLite::ReadPrimitive<int64, WireFormatLite::TYPE_SINT64>(input, &dataObject->id);
			break;
		case MapData::kStringIdFieldNumber:
			WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &dataObject->stringId);
			break;
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
	return dataObject;

}

bool searchMapTreeBounds(io::CodedInputStream* input, int pleft, int pright, int ptop, int pbottom, SearchQuery* req, MapIndex* root) {
	int init = 0;
	int lastIndexResult = -1;
	int cright = 0;
	int cleft = 0;
	int ctop = 0;
	int cbottom = 0;
	int tag;
	req->numberOfReadSubtrees++;
	while ((tag = input->ReadTag()) != 0) {
		if (req->isCancelled()) {
			return false;
		}
		if (init == 0xf) {
			init = 0;
			// coordinates are init
			if (cright < req->left || cleft > req->right || ctop > req->bottom || cbottom < req->top) {
				return false;
			} else {
				req->numberOfAcceptedSubtrees++;
			}
		}
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		// required uint32 version = 1;
		case MapTree::kLeftFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &cleft)));
			cleft += pleft;
			init |= 1;
			break;
		}
		case MapTree::kRightFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &cright)));
			cright += pright;
			init |= 2;
			break;
		}
		case MapTree::kTopFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &ctop)));
			ctop += ptop;
			init |= 4;
			break;
		}
		case MapTree::kBottomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32, WireFormatLite::TYPE_SINT32>(input, &cbottom)));
			cbottom += pbottom;
			init |= 8;
			break;
		}
		case MapTree::kLeafsFieldNumber: {
			uint32 length;
			input->ReadVarint32(&length);
			int oldLimit = input->PushLimit(length);
			if (lastIndexResult == -1) {
				lastIndexResult = req->result.size();
			}
			MapDataObject* mapObject = readMapDataObject(input, cleft, cright, ctop, cbottom, req, root);
			if (mapObject != NULL) {
				req->result.push_back(mapObject);
			}
			input->Skip(input->BytesUntilLimit());
			input->PopLimit(oldLimit);
			break;
		}
		case MapTree::kSubtreesFieldNumber: {
			uint32 length;
			readInt(input, &length);
//			int filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(length);
			searchMapTreeBounds(input, cleft, cright, ctop, cbottom, req, root);
			input->Skip(input->BytesUntilLimit());
			input->PopLimit(oldLimit);
//			input->Seek(filePointer + length);
			if (lastIndexResult >= 0) {
				return false;
			}
			break;
		}
		case MapTree::kOldbaseIdFieldNumber:
		case MapTree::kBaseIdFieldNumber: {
			uint64 baseId;
			input->ReadVarint64(&baseId);
			if (lastIndexResult != -1) {
				for (uint32 i = lastIndexResult; i < req->result.size(); i++) {
					BaseMapDataObject* rs = req->result.at(i);
					rs->id += baseId;
					// restrictions are not supported
//					if (rs.restrictions != null) {
//						for (int j = 0; j < rs.restrictions.length; j++) {
//							rs.restrictions[j] += baseId;
//						}
//					}
				}
			}
			break;
		}
		case MapTree::kStringTableFieldNumber:
		case MapTree::kOldstringTableFieldNumber: {
			uint32 length;
			input->ReadVarint32(&length);
			int oldLimit = input->PushLimit(length);
			std::vector<std::string> stringTable;
			readStringTable(input, stringTable);
			input->PopLimit(oldLimit);
			if (lastIndexResult != -1) {
				for (uint32 i = lastIndexResult; i < req->result.size(); i++) {
					BaseMapDataObject* rs = req->result.at(i);
					if (rs->stringId != BaseMapDataObject::UNDEFINED_STRING) {
						rs->name = stringTable.at(rs->stringId);
					}
				}
			}
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
		input->Seek(i->filePointer);
		int oldLimit = input->PushLimit(i->length);
		searchMapTreeBounds(input, root->left, root->right, root->top, root->bottom, req, ind);
		input->PopLimit(oldLimit);
	}

}

extern "C" JNIEXPORT jint JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_searchObjectsForRendering(JNIEnv* ienv,
		jobject obj, jint sleft, jint sright, jint stop, jint sbottom, jint zoom, jstring mapName,
		jobject renderingRuleSearchRequest, bool skipDuplicates, jint searchResult, jobject objInterrupted) {
	// TODO skipDuplicates not supported
	setGlobalEnv(ienv);
	SearchResult* result = (SearchResult*) searchResult;
	if(result == NULL) {
		result = new SearchResult();
	}
	std::string map = getString(mapName);
	std::map<std::string, BinaryMapFile*>::iterator i = openFiles.find(map);
	if(i == openFiles.end()) {
		return (jint) result;
	}
	BinaryMapFile* file =  i->second;
	RenderingRuleSearchRequest* req = initSearchRequest(renderingRuleSearchRequest);
	jclass clObjInterrupted = globalEnv()->GetObjectClass(objInterrupted);
	jfieldID interruptedField =  getFid(clObjInterrupted, "interrupted", "Z");
	globalEnv()->DeleteLocalRef(clObjInterrupted);
	SearchQuery q(sleft,sright, stop, sbottom, req, objInterrupted, interruptedField);

	fseek(file->f, 0, 0);
	io::FileInputStream input(fileno(file->f));
	input.SetCloseOnDelete(false);
	io::CodedInputStream cis(&input);
	cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);
	if(req != NULL){
		req->clearState();
	}
	q.zoom = zoom;
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
	std::map<tagValueType, std::vector<MapDataObject*> > multyPolygons;
	std::vector<MapDataObject*>::iterator mdo = q.result.begin();
	for(;mdo!= q.result.end(); mdo++) {
		for(size_t j = 0; j<(*mdo)->types.size(); j++) {
			int type = (*mdo)->types.at(j);
			if((type & 0x3) == RenderingRulesStorage::MULTI_POLYGON_TYPE) {
				tagValueType tagValue((*mdo)->tagValues.at(j), type);
				multyPolygons[tagValue].push_back(*mdo);
			}
		}
	}

	proccessMultiPolygons(multyPolygons, q.left, q.right, q.bottom, q.top, q.zoom, result->result);
	if(q.result.size() > 0) {
		sprintf(errorMsg, "Search : tree - read( %d), accept( %d), objs - visit( %d), accept(%d), in result(%d) ", q.numberOfReadSubtrees,
				q.numberOfAcceptedSubtrees, q.numberOfVisitedObjects, q.numberOfAcceptedObjects, result->result.size());
		__android_log_print(ANDROID_LOG_INFO, "net.osmand", errorMsg);
	}
	delete req;
	return (jint)result;
}

extern "C" JNIEXPORT jboolean JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_initBinaryMapFile(JNIEnv* ienv,
		jobject obj, jobject path) {
	// Verify that the version of the library that we linked against is
	setGlobalEnv(ienv);
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
		sprintf(errorMsg, "File could not be open to read from C : %s", inputName.c_str());
		__android_log_print(ANDROID_LOG_WARN, "net.osmand", errorMsg);
		return false;
	}
	BinaryMapFile* mapFile = new BinaryMapFile();
	mapFile->f = file;
	io::FileInputStream input(fileno(file));
	input.SetCloseOnDelete(false);
	io::CodedInputStream cis(&input);
	cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);
	if (!initMapStructure(&cis, mapFile)) {
		sprintf(errorMsg, "File not initialised : %s", inputName.c_str());
		__android_log_print(ANDROID_LOG_WARN, "net.osmand", errorMsg);
		delete mapFile;
		return false;
	}
	mapFile->inputName = inputName;

	openFiles.insert(std::pair<std::string, BinaryMapFile*>(inputName, mapFile));
	return true;
}

#undef DO_
#endif
