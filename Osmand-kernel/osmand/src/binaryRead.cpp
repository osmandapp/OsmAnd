#ifndef _OSMAND_BINARY_READ
#define _OSMAND_BINARY_READ

#include "binaryRead.h"
#include <algorithm>
#include "google/protobuf/wire_format_lite.h"
#include "google/protobuf/io/zero_copy_stream_impl.h"
#include "google/protobuf/wire_format_lite.cc"
#include "proto/osmand_odb.pb.h"
#include "osmand_log.h"

using namespace std;
#define DO_(EXPRESSION) if (!(EXPRESSION)) return false
using google::protobuf::io::CodedInputStream;
using google::protobuf::io::FileInputStream;
using google::protobuf::internal::WireFormatLite;
//using namespace google::protobuf::internal;


std::map< std::string, BinaryMapFile* > openFiles;

inline bool readInt(CodedInputStream* input, uint32* sz ){
	uint8 buf[4];
	if (!input->ReadRaw(buf, 4)) {
		return false;
	}
	*sz = ((buf[0] << 24) + (buf[1] << 16) + (buf[2] << 8) + (buf[3] << 0));
	return true;
}

bool skipFixed32(CodedInputStream* input) {
	uint32 sz;
	if (!readInt(input, &sz)) {
		return false;
	}
	return input->Skip(sz);
}

bool skipUnknownFields(CodedInputStream* input, int tag) {
	if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_FIXED32_LENGTH_DELIMITED) {
		if (!skipFixed32(input)) {
			return false;
		}
	} else if (!WireFormatLite::SkipField(input, tag)) {
		return false;
	}
	return true;
}




bool readMapTreeBounds(CodedInputStream* input, MapTreeBounds* tree, MapRoot* root) {
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

bool readMapLevel(CodedInputStream* input, MapRoot* root) {
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

bool readMapEncodingRule(CodedInputStream* input, MapIndex* index, uint32 id) {
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

bool readMapIndex(CodedInputStream* input, MapIndex* mapIndex) {
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
bool initMapStructure(CodedInputStream* input, BinaryMapFile* file) {
	uint32 tag;
	uint32 versionConfirm = -2;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		// required uint32 version = 1;
		case OsmAndStructure::kVersionFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &file->version)));
			break;
		}
		case OsmAndStructure::kDateCreatedFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint64, WireFormatLite::TYPE_UINT64>(input, &file->dateCreated)));
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
			file->indexes.push_back(new MapIndex(mapIndex));
			file->basemap = file->basemap || mapIndex.name.find("basemap") != string::npos;
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
	if (file->version != versionConfirm) {
		osmand_log_print(LOG_ERROR, "Corrupted file. It should be ended as it starts with version");
		return false;
	}
	if (file->version != MAP_VERSION) {
		osmand_log_print(LOG_ERROR, "Version of the file is not supported.");
		return false;
	}
	return true;
}



bool readStringTable(CodedInputStream* input, std::vector<std::string>& list) {
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

MapDataObject* readMapDataObject(CodedInputStream* input, MapTreeBounds* tree, SearchQuery* req,
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
			minX = std::min(minX, x);
			maxX = std::max(maxX, x);
			minY = std::min(minY, y);
			maxY = std::max(maxY, y);
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
	std::vector< coordinates > innercoordinates;
	std::vector< tag_value > additionalTypes;
	std::vector< tag_value > types;
	HMAP::hash_map< std::string, unsigned int> stringIds;
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
				return NULL;
			}
			if (!skipUnknownFields(input, t)) {
				return NULL;
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



bool searchMapTreeBounds(CodedInputStream* input, MapTreeBounds* current, MapTreeBounds* parent,
		SearchQuery* req, std::vector<MapTreeBounds>* foundSubtrees) {
	int init = 0;
	int tag;
	int si;
	req->numberOfReadSubtrees++;
	while ((tag = input->ReadTag()) != 0) {
		if (req->publisher->isCancelled()) {
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
			if (current->ocean) {
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

bool readMapDataBlocks(CodedInputStream* input, SearchQuery* req, MapTreeBounds* tree, MapIndex* root) {
	long long baseId = 0;
	int tag;
	std::vector< MapDataObject* > results;
	while ((tag = input->ReadTag()) != 0) {
		if (req->publisher->isCancelled()) {
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
						HMAP::hash_map<std::string, unsigned int >::iterator  val=(*obj)->stringIds.begin();
						while(val != (*obj)->stringIds.end()){
							(*obj)->objectNames[val->first]=stringTable[val->second];
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
				req->publish(mapObject);
				results.push_back(mapObject);
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

void searchMapData(CodedInputStream* input, MapRoot* root, MapIndex* ind, SearchQuery* req) {
	// search
	for (std::vector<MapTreeBounds>::iterator i = root->bounds.begin();
			i != root->bounds.end(); i++) {
		if (req->publisher->isCancelled()) {
			return;
		}
		if (i->right < req->left || i->left > req->right || i->top > req->bottom || i->bottom < req->top) {
			continue;
		}
		std::vector<MapTreeBounds> foundSubtrees;
		input->Seek(i->filePointer);
		int oldLimit = input->PushLimit(i->length);
		searchMapTreeBounds(input, &(*i), root, req, &foundSubtrees);
		input->PopLimit(oldLimit);


		sort(foundSubtrees.begin(), foundSubtrees.end(), sortTreeBounds);
		uint32 length;
		for (std::vector<MapTreeBounds>::iterator tree = foundSubtrees.begin();
					tree != foundSubtrees.end(); tree++) {
			if (req->publisher->isCancelled()) {
				return;
			}
			input->Seek(tree->mapDataBlock);
			WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &length);
			int oldLimit = input->PushLimit(length);
			readMapDataBlocks(input, req, &(*tree), ind);
			input->PopLimit(oldLimit);
		}
	}

}




ResultPublisher* searchObjectsForRendering(SearchQuery* q, bool skipDuplicates, std::string msgNothingFound) {
	map<std::string, BinaryMapFile*>::iterator i = openFiles.begin();
	HMAP::hash_set<long long> ids;
	int count = 0;
	bool ocean = false;
	std::vector<MapDataObject*> basemapResult;
	std::vector<MapDataObject*> tempResult;
	std::vector<MapDataObject*> coastLines;
	std::vector<MapDataObject*> basemapCoastLines;

	for (; i != openFiles.end() && !q->publisher->isCancelled(); i++) {
		BinaryMapFile* file = i->second;
		fseek(file->f, 0, 0);
		FileInputStream input(fileno(file->f));
		input.SetCloseOnDelete(false);
		CodedInputStream cis(&input);
		cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);
		if (q->req != NULL) {
			q->req->clearState();
		}
		q->publisher->result.clear();
		for (std::vector<MapIndex>::iterator mapIndex = file->mapIndexes.begin(); mapIndex != file->mapIndexes.end();
				mapIndex++) {
			for (std::vector<MapRoot>::iterator mapLevel = mapIndex->levels.begin(); mapLevel != mapIndex->levels.end();
					mapLevel++) {
				if (q->publisher->isCancelled()) {
					break;
				}
				if (mapLevel->minZoom <= q->zoom && mapLevel->maxZoom >= q->zoom) {
					if (mapLevel->right >= q->left && q->right >= mapLevel->left && mapLevel->bottom >= q->top
							&& q->bottom >= mapLevel->top) {
						osmand_log_print(LOG_INFO, "Search map %s", mapIndex->name.c_str());
						searchMapData(&cis, &(*mapLevel), &(*mapIndex), q);
					}
				}
			}
		}
		if (q->ocean) {
			ocean = true;
		}
		if (!q->publisher->isCancelled()) {
			std::vector<MapDataObject*>::iterator r = q->publisher->result.begin();
			tempResult.reserve((size_t)(q->publisher->result.size() + tempResult.size()));
			for (; r != q->publisher->result.end(); r++) {
				// TODO skip duplicates doesn't work correctly with basemap (id < 0?)
				if (skipDuplicates && (*r)->id > 0 && false) {
					if (ids.find((*r)->id) != ids.end()) {
						continue;
					}
					ids.insert((*r)->id);
				}

				count++;
				if ((*r)->contains("natural", "coastline")) {
					if (i->second->isBasemap()) {
						basemapCoastLines.push_back(*r);
					} else {
						coastLines.push_back(*r);
					}
				} else {
					// do not mess coastline and other types
					if (i->second->isBasemap()) {
						basemapResult.push_back(*r);
					} else {
						tempResult.push_back(*r);
					}
				}
			}
		}
	}

	// sort results/ analyze coastlines and publish back to publisher
	if (q->publisher->isCancelled()) {
		deleteObjects(coastLines);
		deleteObjects(tempResult);
		deleteObjects(basemapCoastLines);
		deleteObjects(basemapResult);
	} else {
		bool addBasemapCoastlines = true;
		bool emptyData = q->zoom > BASEMAP_ZOOM && tempResult.empty() && coastLines.empty();
		if (!coastLines.empty()) {
			std::vector<MapDataObject*> pcoastlines;
			processCoastlines(coastLines, q->left, q->right, q->bottom, q->top, q->zoom, basemapCoastLines.empty(), pcoastlines);
			addBasemapCoastlines = pcoastlines.empty() || q->zoom <= BASEMAP_ZOOM;
			tempResult.insert(tempResult.end(), pcoastlines.begin(), pcoastlines.end());
		}
		if (addBasemapCoastlines) {
			addBasemapCoastlines = false;
			std::vector<MapDataObject*> pcoastlines;
			processCoastlines(basemapCoastLines, q->left, q->right, q->bottom, q->top, q->zoom, true, pcoastlines);
			addBasemapCoastlines = pcoastlines.empty();
			tempResult.insert(tempResult.end(), pcoastlines.begin(), pcoastlines.end());
		}
		// processCoastlines always create new objects
		deleteObjects(basemapCoastLines);
		deleteObjects(coastLines);
		if (addBasemapCoastlines) {
			MapDataObject* o = new MapDataObject();
			o->points.push_back(int_pair(q->left, q->top));
			o->points.push_back(int_pair(q->right, q->top));
			o->points.push_back(int_pair(q->right, q->bottom));
			o->points.push_back(int_pair(q->left, q->bottom));
			o->points.push_back(int_pair(q->left, q->top));
			if (ocean) {
				o->types.push_back(tag_value("natural", "coastline"));
			} else {
				o->types.push_back(tag_value("natural", "land"));
			}
			tempResult.push_back(o);
		}
		if (emptyData) {
			// message
			// avoid overflow int errors
			MapDataObject* o = new MapDataObject();
			o->points.push_back(int_pair(q->left + (q->right - q->left) / 2, q->top + (q->bottom - q->top) / 2));
			o->types.push_back(tag_value("natural", "coastline"));
			o->objectNames["name"] = msgNothingFound;
			tempResult.push_back(o);
		}
		if (q->zoom <= BASEMAP_ZOOM || emptyData) {
			tempResult.insert(tempResult.end(), basemapResult.begin(), basemapResult.end());
		}
		q->publisher->result.clear();
		q->publisher->publish(tempResult);
		osmand_log_print(LOG_INFO,
				"Search : tree - read( %d), accept( %d), objs - visit( %d), accept(%d), in result(%d) ",
				q->numberOfReadSubtrees, q->numberOfAcceptedSubtrees, q->numberOfVisitedObjects, q->numberOfAcceptedObjects,
				q->publisher->result.size());
	}
	return q->publisher;
}

bool closeBinaryMapFile(std::string inputName) {
	std::map<std::string, BinaryMapFile*>::iterator iterator;
	if ((iterator = openFiles.find(inputName)) != openFiles.end()) {
		delete iterator->second;
		openFiles.erase(iterator);
		return true;
	}
	return false;
}

BinaryMapFile* initBinaryMapFile(std::string inputName) {
	GOOGLE_PROTOBUF_VERIFY_VERSION;
	std::map<std::string, BinaryMapFile*>::iterator iterator;
	if ((iterator = openFiles.find(inputName)) != openFiles.end()) {
		delete iterator->second;
		openFiles.erase(iterator);
	}

	FILE* file = fopen(inputName.c_str(), "r");
	if (file == NULL) {
		osmand_log_print(LOG_ERROR, "File could not be open to read from C : %s", inputName.c_str());
		return NULL;
	}
	BinaryMapFile* mapFile = new BinaryMapFile();
	mapFile->f = file;
	FileInputStream input(fileno(file));
	input.SetCloseOnDelete(false);
	CodedInputStream cis(&input);
	cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);
	if (!initMapStructure(&cis, mapFile)) {
		osmand_log_print(LOG_ERROR, "File not initialised : %s", inputName.c_str());
		delete mapFile;
		return NULL;
	}
	mapFile->inputName = inputName;

	openFiles.insert(std::pair<std::string, BinaryMapFile*>(inputName, mapFile));
	return mapFile;
}
#undef DO_
#endif
