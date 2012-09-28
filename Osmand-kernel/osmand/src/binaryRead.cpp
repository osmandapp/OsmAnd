#include "binaryRead.h"

#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <algorithm>
#include "google/protobuf/wire_format_lite.h"
#include "google/protobuf/io/zero_copy_stream_impl.h"
#include "google/protobuf/wire_format_lite.cc"
#include "proto/osmand_odb.pb.h"
#include "proto/osmand_index.pb.h"
#include "osmand_log.h"

using namespace std;
#define DO_(EXPRESSION) if (!(EXPRESSION)) return false
using google::protobuf::io::CodedInputStream;
using google::protobuf::io::FileInputStream;
using google::protobuf::internal::WireFormatLite;
//using namespace google::protobuf::internal;


std::map< std::string, BinaryMapFile* > openFiles;
OsmAndStoredIndex* cache = NULL;

inline bool readInt(CodedInputStream* input, uint32_t* sz ){
	uint8_t buf[4];
	if (!input->ReadRaw(buf, 4)) {
		return false;
	}
	*sz = ((buf[0] << 24) + (buf[1] << 16) + (buf[2] << 8) + (buf[3] << 0));
	return true;
}

bool skipFixed32(CodedInputStream* input) {
	uint32_t sz;
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
	int32_t si;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case OsmAndMapIndex_MapDataBox::kLeftFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &si)));
			tree->left = si + root->left;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kRightFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &si)));
			tree->right = si + root->right;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kTopFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &si)));
			tree->top = si + root->top;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kBottomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &si)));
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

bool readMapLevel(CodedInputStream* input, MapRoot* root, bool initSubtrees) {
	int tag;
	int si;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case OsmAndMapIndex_MapRootLevel::kMaxZoomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &root->maxZoom)));
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kMinZoomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &root->minZoom)));
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kBottomFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &si)));
			root->bottom = si;
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kTopFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &si)));
			root->top = si;
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kLeftFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &si)));
			root->left = si;
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kRightFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &si)));
			root->right = si;
			break;
		}
		case OsmAndMapIndex_MapRootLevel::kBoxesFieldNumber: {
			if (!initSubtrees) {
				input->Skip(input->BytesUntilLimit());
				break;
			}
			MapTreeBounds bounds;
			readInt(input, &bounds.length);
			bounds.filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(bounds.length);
			readMapTreeBounds(input, &bounds, root);
			root->bounds.push_back(bounds);
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

bool readRouteEncodingRule(CodedInputStream* input, RoutingIndex* index, uint32_t id) {
	int tag;
	std::string tagS;
	std::string value;
	uint32_t type = 0;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case OsmAndRoutingIndex_RouteEncodingRule::kValueFieldNumber: {
			DO_((WireFormatLite::ReadString(input, &value)));
			break;
		}
		case OsmAndRoutingIndex_RouteEncodingRule::kTagFieldNumber: {
			DO_((WireFormatLite::ReadString(input, &tagS)));
			break;
		}
		case OsmAndRoutingIndex_RouteEncodingRule::kIdFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &id)));
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
	index->initRouteEncodingRule(id, tagS, value);
	return true;
}

bool readMapEncodingRule(CodedInputStream* input, MapIndex* index, uint32_t id) {
	int tag;
	std::string tagS;
	std::string value;
	uint32_t type = 0;
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
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &type)));
			break;
		}
		case OsmAndMapIndex_MapEncodingRule::kIdFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &id)));
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


bool readRouteTree(CodedInputStream* input, RouteSubregion* thisTree, RouteSubregion* parentTree, int depth, bool readCoordinates) {
	bool readChildren = depth != 0;
	uint32_t tag;
	int i;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {

		case OsmAndRoutingIndex_RouteDataBox::kLeftFieldNumber: {
			WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &i);
			if (readCoordinates) {
				thisTree->left = i + (parentTree != NULL ? parentTree->left : 0);
			}
			break;
		}
		case OsmAndRoutingIndex_RouteDataBox::kRightFieldNumber: {
			WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &i);
			if (readCoordinates) {
				thisTree->right = i + (parentTree != NULL ? parentTree->right : 0);
			}
			break;
		}
		case OsmAndRoutingIndex_RouteDataBox::kTopFieldNumber: {
			WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &i);
			if (readCoordinates) {
				thisTree->top = i + (parentTree != NULL ? parentTree->top : 0);
			}
			break;
		}
		case OsmAndRoutingIndex_RouteDataBox::kBottomFieldNumber: {
			WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &i);
			if (readCoordinates) {
				thisTree->bottom = i + (parentTree != NULL ? parentTree->bottom : 0);
			}
			break;
		}
		case OsmAndRoutingIndex_RouteDataBox::kShiftToDataFieldNumber: {
			readInt(input, &thisTree->mapDataBlock);
			break;
		}
		case OsmAndRoutingIndex_RouteDataBox::kBoxesFieldNumber: {
			if (readChildren) {
				RouteSubregion subregion;
				readInt(input, &subregion.length);
				subregion.filePointer = input->getTotalBytesRead();
				int oldLimit = input->PushLimit(subregion.length);
				readRouteTree(input, &subregion, thisTree, depth - 1, true);
				input->PopLimit(oldLimit);
				input->Seek(subregion.filePointer + subregion.length);
				thisTree->subregions.push_back(subregion);
			} else {
				if (!skipUnknownFields(input, tag)) {
					return false;
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

bool readRoutingIndex(CodedInputStream* input, RoutingIndex* routingIndex) {
	uint32_t defaultId = 1;
	uint32_t tag;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case OsmAndRoutingIndex::kNameFieldNumber: {
			DO_((WireFormatLite::ReadString(input, &routingIndex->name)));
			break;
		}
		case OsmAndRoutingIndex::kRulesFieldNumber: {
			int len;
			WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &len);
			int oldLimit = input->PushLimit(len);
			readRouteEncodingRule(input, routingIndex, defaultId++);
			input->PopLimit(oldLimit);
			break;
		}
		case OsmAndRoutingIndex::kRootBoxesFieldNumber: {
			RouteSubregion subregion;
			readInt(input, &subregion.length);
			subregion.filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(subregion.length);
			readRouteTree(input, &subregion, NULL, 0, true);
			input->PopLimit(oldLimit);
			input->Seek(subregion.filePointer + subregion.length);
			routingIndex->subregions.push_back(subregion);
			// Finish reading
			input->Seek(routingIndex->filePointer + routingIndex->length);
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

bool readMapIndex(CodedInputStream* input, MapIndex* mapIndex, bool onlyInitEncodingRules) {
	uint32_t tag;
	uint32_t defaultId = 1;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case OsmAndMapIndex::kNameFieldNumber: {
			DO_((WireFormatLite::ReadString(input, &mapIndex->name)));
			break;
		}
		case OsmAndMapIndex::kRulesFieldNumber: {
			if(onlyInitEncodingRules) {
				int len;
				WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &len);
				int oldLimit = input->PushLimit(len);
				readMapEncodingRule(input, mapIndex, defaultId++);
				input->PopLimit(oldLimit);
			} else {
				skipUnknownFields(input, tag);
			}
			break;
		}
		case OsmAndMapIndex::kLevelsFieldNumber: {
			MapRoot mapLevel;
			readInt(input, &mapLevel.length);
			mapLevel.filePointer = input->getTotalBytesRead();
			if (!onlyInitEncodingRules) {
				int oldLimit = input->PushLimit(mapLevel.length);
				readMapLevel(input, &mapLevel, false);
				input->PopLimit(oldLimit);
				mapIndex->levels.push_back(mapLevel);
			}
			input->Seek(mapLevel.filePointer + mapLevel.length);
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
	if(onlyInitEncodingRules) {
		mapIndex->finishInitializingTags();
	}
	return true;
}


//display google::protobuf::internal::WireFormatLite::GetTagWireType(tag)
// display google::protobuf::internal::WireFormatLite::GetTagFieldNumber(tag)
bool initMapStructure(CodedInputStream* input, BinaryMapFile* file) {
	uint32_t tag;
	uint32_t versionConfirm = -2;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		// required uint32_t version = 1;
		case OsmAndStructure::kVersionFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &file->version)));
			break;
		}
		case OsmAndStructure::kDateCreatedFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint64_t, WireFormatLite::TYPE_UINT64>(input, &file->dateCreated)));
			break;
		}
		case OsmAndStructure::kMapIndexFieldNumber: {
			MapIndex mapIndex;
			readInt(input, &mapIndex.length);
			mapIndex.filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(mapIndex.length);
			readMapIndex(input, &mapIndex, false);
			input->PopLimit(oldLimit);
			input->Seek(mapIndex.filePointer + mapIndex.length);
			file->mapIndexes.push_back(mapIndex);
			file->indexes.push_back(&file->mapIndexes.back());
			file->basemap = file->basemap || mapIndex.name.find("basemap") != string::npos;
			break;
		}
		case OsmAndStructure::kRoutingIndexFieldNumber: {
			RoutingIndex routingIndex;
			readInt(input, &routingIndex.length);
			routingIndex.filePointer = input->getTotalBytesRead();
			int oldLimit = input->PushLimit(routingIndex.length);
			readRoutingIndex(input, &routingIndex);
			input->PopLimit(oldLimit);
			input->Seek(routingIndex.filePointer + routingIndex.length);
			file->routingIndexes.push_back(routingIndex);
			file->indexes.push_back(&file->routingIndexes.back());
			break;
		}
		case OsmAndStructure::kVersionConfirmFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &versionConfirm)));
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
	uint32_t tag;
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
static const int ROUTE_SHIFT_COORDINATES = 4;
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
			MapIndex* root, uint64_t baseId) {
	uint32_t tag = WireFormatLite::GetTagFieldNumber(input->ReadTag());
	bool area = MapData::kAreaCoordinatesFieldNumber == tag;
	if(!area && MapData::kCoordinatesFieldNumber != tag) {
		return NULL;
	}
	req->cacheCoordinates.clear();
	uint32_t size;
	input->ReadVarint32(&size);
	int old = input->PushLimit(size);
	int px = tree->left & MASK_TO_READ;
	int py = tree->top & MASK_TO_READ;
	bool contains = false;
	int64_t id = 0;
	int minX = INT_MAX;
	int maxX = 0;
	int minY = INT_MAX;
	int maxY = 0;
	req->numberOfVisitedObjects++;
	int x;
	int y;
	while (input->BytesUntilLimit() > 0) {
		if (!WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &x)) {
			return NULL;
		}
		if (!WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &y)) {
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
	UNORDERED(map)< std::string, unsigned int> stringIds;
	bool loop = true;
	while (loop) {
		uint32_t t = input->ReadTag();
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
				WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &x);
				WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &y);
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
				WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &x);
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
				WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &x);
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
			WireFormatLite::ReadPrimitive<int64_t, WireFormatLite::TYPE_SINT64>(input, &id);
			break;
		case MapData::kStringNamesFieldNumber:
			input->ReadVarint32(&size);
			old = input->PushLimit(size);
			while (input->BytesUntilLimit() > 0) {
				WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &x);
				WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_INT32>(input, &y);
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
//	if(req->cacheCoordinates.size() > 100 && types.size() > 0 /*&& types[0].first == "admin_level"*/) {
//		osmand_log_print(LOG_INFO, "TODO Object is %llu  (%llu) ignored %s %s", (id + baseId) >> 1, baseId, types[0].first.c_str(), types[0].second.c_str());
//		return NULL;
//	}


	req->numberOfAcceptedObjects++;

	MapDataObject* dataObject = new MapDataObject();
	dataObject->points = req->cacheCoordinates;
	dataObject->additionalTypes = additionalTypes;
	dataObject->types = types;
	dataObject->id = id;
	dataObject->area = area;
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
		// required uint32_t version = 1;
		case OsmAndMapIndex_MapDataBox::kLeftFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &si)));
			current->left = si + parent->left;
			init |= 1;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kRightFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &si)));
			current->right = si + parent->right;
			init |= 2;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kTopFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &si)));
			current->top = si + parent->top;
			init |= 4;
			break;
		}
		case OsmAndMapIndex_MapDataBox::kBottomFieldNumber : {
			DO_((WireFormatLite::ReadPrimitive<int32_t, WireFormatLite::TYPE_SINT32>(input, &si)));
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
				req->mixed = true;
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
	uint64_t baseId = 0;
	int tag;
	std::vector< MapDataObject* > results;
	while ((tag = input->ReadTag()) != 0) {
		if (req->publisher->isCancelled()) {
			return false;
		}
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		// required uint32_t version = 1;
		case MapDataBlock::kBaseIdFieldNumber : {
			WireFormatLite::ReadPrimitive<uint64_t, WireFormatLite::TYPE_UINT64>(input, &baseId);
			break;
		}
		case MapDataBlock::kStringTableFieldNumber: {
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			if(results.size() > 0) {
				std::vector<std::string> stringTable;
				readStringTable(input, stringTable);
				MapDataObject* o;
				for (std::vector<MapDataObject*>::iterator obj = results.begin(); obj != results.end(); obj++) {
					if ((*obj)->stringIds.size() > 0) {
						UNORDERED(map)<std::string, unsigned int >::iterator  val=(*obj)->stringIds.begin();
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
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			MapDataObject* mapObject = readMapDataObject(input, tree, req, root, baseId);
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
		uint32_t length;
		for (std::vector<MapTreeBounds>::iterator tree = foundSubtrees.begin();
					tree != foundSubtrees.end(); tree++) {
			if (req->publisher->isCancelled()) {
				return;
			}
			input->Seek(tree->mapDataBlock);
			WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length);
			int oldLimit = input->PushLimit(length);
			readMapDataBlocks(input, req, &(*tree), ind);
			input->PopLimit(oldLimit);
		}
	}

}




ResultPublisher* searchObjectsForRendering(SearchQuery* q, bool skipDuplicates, std::string msgNothingFound) {
	map<std::string, BinaryMapFile*>::iterator i = openFiles.begin();
	UNORDERED(set)<long long> ids;
	int count = 0;
	std::vector<MapDataObject*> basemapResult;
	std::vector<MapDataObject*> tempResult;
	std::vector<MapDataObject*> coastLines;
	std::vector<MapDataObject*> basemapCoastLines;

	bool basemapExists = false;
	for (; i != openFiles.end() && !q->publisher->isCancelled(); i++) {
		BinaryMapFile* file = i->second;
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
				basemapExists |= file->isBasemap();
				if (mapLevel->minZoom <= q->zoom && mapLevel->maxZoom >= q->zoom) {
					if (mapLevel->right >= q->left && q->right >= mapLevel->left && mapLevel->bottom >= q->top
							&& q->bottom >= mapLevel->top) {
						osmand_log_print(LOG_INFO, "Search map %s", mapIndex->name.c_str());
						// lazy initializing rules
						if (mapIndex->decodingRules.size() == 0) {
							lseek(file->fd, 0, SEEK_SET);
							FileInputStream input(file->fd);
							input.SetCloseOnDelete(false);
							CodedInputStream cis(&input);
							cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 1);
							cis.Seek(mapIndex->filePointer);
							int oldLimit = cis.PushLimit(mapIndex->length);
							readMapIndex(&cis, &(*mapIndex), true);
							cis.PopLimit(oldLimit);
						}
						// lazy initializing subtrees
						if (mapLevel->bounds.size() == 0) {
							lseek(file->fd, 0, SEEK_SET);
							FileInputStream input(file->fd);
							input.SetCloseOnDelete(false);
							CodedInputStream cis(&input);
							cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 1);
							cis.Seek(mapLevel->filePointer);
							int oldLimit = cis.PushLimit(mapLevel->length);
							readMapLevel(&cis, &(*mapLevel), true);
							cis.PopLimit(oldLimit);
						}
						lseek(file->fd, 0, SEEK_SET);
						FileInputStream input(file->fd);
						input.SetCloseOnDelete(false);
						CodedInputStream cis(&input);
						cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);
						searchMapData(&cis, &(*mapLevel), &(*mapIndex), q);
					}
				}
			}
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
		bool ocean = q->ocean;
		bool land = q->mixed;
		bool addBasemapCoastlines = true;
		bool emptyData = q->zoom > BASEMAP_ZOOM && tempResult.empty() && coastLines.empty();
		// determine if there are enough objects like land/lake..
		bool basemapMissing = q->zoom <= BASEMAP_ZOOM && basemapCoastLines.empty() && !basemapExists;
		bool detailedLandData = q->zoom >= 14 && tempResult.size() > 0;
		if (!coastLines.empty()) {
			bool coastlinesWereAdded = processCoastlines(coastLines, q->left, q->right, q->bottom, q->top, q->zoom,
					basemapCoastLines.empty(), true, tempResult);
			addBasemapCoastlines = (!coastlinesWereAdded && !detailedLandData) || q->zoom <= BASEMAP_ZOOM;
		} else {
			addBasemapCoastlines = !detailedLandData;
		}
		if (addBasemapCoastlines) {
			addBasemapCoastlines = false;
			bool coastlinesWereAdded = processCoastlines(basemapCoastLines, q->left, q->right, q->bottom, q->top, q->zoom,
					true, true, tempResult);
			addBasemapCoastlines = !coastlinesWereAdded;
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
			if (ocean && !land) {
				o->types.push_back(tag_value("natural", "coastline"));
			} else {
				o->types.push_back(tag_value("natural", "land"));
			}
			tempResult.push_back(o);
		}
		if (emptyData || basemapMissing) {
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
		} else {
			deleteObjects(basemapResult);
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


void searchRouteRegion(CodedInputStream* input, SearchQuery* q, RoutingIndex* ind, std::vector<RouteSubregion>& subregions,
		std::vector<RouteSubregion>& toLoad, BinaryMapFile* file) {
	for (std::vector<RouteSubregion>::iterator subreg = subregions.begin();
						subreg != subregions.end(); subreg++) {
		if (subreg->right >= q->left && q->right >= subreg->left && subreg->bottom >= q->top
				&& q->bottom >= subreg->top) {
			if(subreg->subregions.empty()){
				bool contains = subreg->right <= q->right && q->left <= subreg->left && subreg->top <= q->top
						&& subreg->bottom >= q->bottom;
				input->Seek(subreg->filePointer);
				uint32_t old = input->PushLimit(subreg->length);
				readRouteTree(input, &(*subreg), NULL, contains? -1 : 1, false);
				input->PopLimit(old);
			}
			searchRouteRegion(input, q, ind, subreg->subregions, toLoad, file);
			if(subreg->mapDataBlock != 0) {
				toLoad.push_back(*subreg);
			}
		}
	}
}

bool readRouteDataObject(CodedInputStream* input, uint32_t left, uint32_t top, RouteDataObject* obj) {
	int tag;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		case RouteData::kTypesFieldNumber: {
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			uint32_t t;
			while (input->BytesUntilLimit() > 0) {
				DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &t)));
				obj->types.push_back(t);
			}
			input->PopLimit(oldLimit);
			break;
		}
		case RouteData::kRouteIdFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<int64_t, WireFormatLite::TYPE_INT64>(input, &obj->id)));
			break;
		}
		case RouteData::kPointsFieldNumber: {
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			uint32_t t;
			int s;
			int px = left >> ROUTE_SHIFT_COORDINATES;
			int py = top >> ROUTE_SHIFT_COORDINATES;
			while (input->BytesUntilLimit() > 0) {
				DO_((WireFormatLite::ReadPrimitive<int, WireFormatLite::TYPE_SINT32>(input, &s)));
				uint32_t x = s + px;
				DO_((WireFormatLite::ReadPrimitive<int, WireFormatLite::TYPE_SINT32>(input, &s)));
				uint32_t y = s + py;

				obj->pointsX.push_back(x << ROUTE_SHIFT_COORDINATES);
				obj->pointsY.push_back(y << ROUTE_SHIFT_COORDINATES);
				px = x;
				py = y;
			}
			input->PopLimit(oldLimit);
			break;
		}
		case RouteData::kStringNamesFieldNumber: {
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			uint32_t s;
			uint32_t t;
			while (input->BytesUntilLimit() > 0) {
				DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &s)));
				DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &t)));

				obj->namesIds.push_back( pair<uint32_t, uint32_t>(s, t));
			}
			input->PopLimit(oldLimit)	;
			break;
		}
		case RouteData::kPointTypesFieldNumber: {
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			while (input->BytesUntilLimit() > 0) {
				uint32_t pointInd;
				uint32_t lens;
				uint32_t t;
				DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &pointInd)));
				DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &lens)));
				int oldLimits = input->PushLimit(lens);

				if (obj->pointTypes.size() <= pointInd) {
					obj->pointTypes.resize(pointInd + 1, std::vector<uint32_t>());
				}
				while (input->BytesUntilLimit() > 0) {
					DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &t)));
					obj->pointTypes[pointInd].push_back(t);
				}
				input->PopLimit(oldLimits);
			}
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

const static int RESTRICTION_SHIFT = 3;
const static int RESTRICTION_MASK = 7;
bool readRouteTreeData(CodedInputStream* input, RouteSubregion* s, std::vector<RouteDataObject*>& dataObjects) {
	int tag;
	std::vector<int64_t> idTables;
	UNORDERED(map)<int64_t, std::vector<uint64_t> > restrictions;
	std::vector<std::string> stringTable;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		// required uint32_t version = 1;
		case OsmAndRoutingIndex_RouteDataBlock::kDataObjectsFieldNumber: {
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			RouteDataObject* obj = new RouteDataObject;
			readRouteDataObject(input, s->left, s->top, obj);
			if(dataObjects.size() <= obj->id ) {
				dataObjects.resize((uint32_t) obj->id + 1, NULL);
			}
			dataObjects[obj->id] = obj;
			input->PopLimit(oldLimit);
			break;
		}
		case OsmAndRoutingIndex_RouteDataBlock::kStringTableFieldNumber: {
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			readStringTable(input, stringTable);
			input->Skip(input->BytesUntilLimit());
			input->PopLimit(oldLimit);
			break;
		}
		case OsmAndRoutingIndex_RouteDataBlock::kRestrictionsFieldNumber: {
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			uint64_t from = 0;
			uint64_t to = 0;
			uint64_t type = 0;
			int tm;
			int ts;
			while ((ts = input->ReadTag()) != 0) {
				switch (WireFormatLite::GetTagFieldNumber(ts)) {
				case RestrictionData::kFromFieldNumber: {
					DO_((WireFormatLite::ReadPrimitive<int, WireFormatLite::TYPE_INT32>(input, &tm)));
					from = tm;
					break;
				}
				case RestrictionData::kToFieldNumber: {
					DO_((WireFormatLite::ReadPrimitive<int, WireFormatLite::TYPE_INT32>(input, &tm)));
					to = tm;
					break;
				}
				case RestrictionData::kTypeFieldNumber: {
					DO_((WireFormatLite::ReadPrimitive<int, WireFormatLite::TYPE_INT32>(input, &tm)));
					type = tm;
					break;
				}
				default: {
					if (WireFormatLite::GetTagWireType(ts) == WireFormatLite::WIRETYPE_END_GROUP) {
						return true;
					}
					if (!skipUnknownFields(input, ts)) {
						return false;
					}
					break;
				}
				}
			}
			restrictions[from].push_back((to << RESTRICTION_SHIFT) + type);
			input->PopLimit(oldLimit);
			break;
		}
		case OsmAndRoutingIndex_RouteDataBlock::kIdTableFieldNumber: {
			uint32_t length;
			DO_((WireFormatLite::ReadPrimitive<uint32_t, WireFormatLite::TYPE_UINT32>(input, &length)));
			int oldLimit = input->PushLimit(length);
			int64_t routeId = 0;
			int ts;
			while ((ts = input->ReadTag()) != 0) {
				switch (WireFormatLite::GetTagFieldNumber(ts)) {
				case IdTable::kRouteIdFieldNumber: {
					int64_t val;
					DO_((WireFormatLite::ReadPrimitive<int64_t, WireFormatLite::TYPE_SINT64>(input, &val)));
					routeId += val;
					idTables.push_back(routeId);
					break;
				}
				default: {
					if (WireFormatLite::GetTagWireType(ts) == WireFormatLite::WIRETYPE_END_GROUP) {
						return true;
					}
					if (!skipUnknownFields(input, ts)) {
						return false;
					}
					break;
				}
				}
			}
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
	UNORDERED(map)<int64_t, std::vector<uint64_t> >::iterator itRestrictions = restrictions.begin();
	for (; itRestrictions != restrictions.end(); itRestrictions++) {
		RouteDataObject* fromr = dataObjects[itRestrictions->first];
		if (fromr != NULL) {
			fromr->restrictions = itRestrictions->second;
			for (int i = 0; i < fromr->restrictions.size(); i++) {
				uint32_t to = fromr->restrictions[i] >> RESTRICTION_SHIFT;
				uint64_t valto = (idTables[to] << RESTRICTION_SHIFT) | ((long) fromr->restrictions[i] & RESTRICTION_MASK);
				fromr->restrictions[i] = valto;
			}
		}
	}
	std::vector<RouteDataObject*>::iterator dobj = dataObjects.begin();
	for (; dobj != dataObjects.end(); dobj++) {
		if (*dobj != NULL) {
			if ((*dobj)->id < idTables.size()) {
				(*dobj)->id = idTables[(*dobj)->id];
			}
			if ((*dobj)->namesIds.size() > 0) {
				vector<pair<uint32_t, uint32_t> >::iterator itnames = (*dobj)->namesIds.begin();
				for(; itnames != (*dobj)->namesIds.end(); itnames++) {
					if((*itnames).second >= stringTable.size()) {
						osmand_log_print(LOG_ERROR, "ERROR VALUE string table %d", (*itnames).second );
					} else {
						(*dobj)->names[(int) (*itnames).first] = stringTable[(*itnames).second];
					}
				}
			}
		}
	}

	return true;

}

bool sortRouteRegions (const RouteSubregion& i,const RouteSubregion& j) { return (i.mapDataBlock<j.mapDataBlock); }

void searchRouteRegion(SearchQuery* q, std::vector<RouteDataObject*>& list, RoutingIndex* rs, RouteSubregion* sub){
	map<std::string, BinaryMapFile*>::iterator i = openFiles.begin();
	UNORDERED(set)<long long> ids;
	int count = 0;
	bool basemapExists = false;
	for (; i != openFiles.end() && !q->publisher->isCancelled(); i++) {
		BinaryMapFile* file = i->second;
		for (std::vector<RoutingIndex>::iterator routingIndex = file->routingIndexes.begin();
				routingIndex != file->routingIndexes.end(); routingIndex++) {
			if (q->publisher->isCancelled()) {
				break;
			}
			if(rs != NULL && (rs->name != routingIndex->name || rs->filePointer != routingIndex->filePointer)){
				continue;
			}

			// init decoding rules
			if (routingIndex->decodingRules.size() == 0) {
				routingIndex->subregions.clear();
				lseek(file->routefd, 0, SEEK_SET);
				FileInputStream input(file->routefd);
				input.SetCloseOnDelete(false);
				CodedInputStream cis(&input);
				cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);

				cis.Seek(routingIndex->filePointer);
				uint32_t old = cis.PushLimit(routingIndex->length);
				readRoutingIndex(&cis, &(*routingIndex));
				cis.PopLimit(old);
			}

			// could be simplified but it will be concurrency with init block
			lseek(file->routefd, 0, SEEK_SET);
			FileInputStream input(file->routefd);
			input.SetCloseOnDelete(false);
			CodedInputStream cis(&input);
			cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);


//			cis.Seek(sub->filePointer);
//			uint32_t old = cis.PushLimit(sub->length);
//			readRouteTree(&cis, &(*sub), NULL, 0, true);
//			cis.PopLimit(old);
			cis.Seek(sub->filePointer + sub->mapDataBlock);
			uint32_t length;
			cis.ReadVarint32(&length);
			uint32_t old = cis.PushLimit(length);
			readRouteTreeData(&cis, &(*sub), list);
			cis.PopLimit(old);
		}

	}
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

bool initMapFilesFromCache(std::string inputName) {
	GOOGLE_PROTOBUF_VERIFY_VERSION;
#if defined(_WIN32)
	int fileDescriptor = open(inputName.c_str(), O_RDONLY | O_BINARY);
#else
	int fileDescriptor = open(inputName.c_str(), O_RDONLY);
#endif
	if (fileDescriptor < 0) {
		osmand_log_print(LOG_ERROR, "Cache file could not be open to read : %s", inputName.c_str());
		return false;
	}
	FileInputStream input(fileDescriptor);
	CodedInputStream cis(&input);
	cis.SetTotalBytesLimit(INT_MAX, INT_MAX);
	OsmAndStoredIndex* c = new OsmAndStoredIndex();
	if(c->MergeFromCodedStream(&cis)){
		osmand_log_print(LOG_INFO, "Native Cache file initialized %s", inputName.c_str());
		cache = c;
		for (int i = 0; i < cache->fileindex_size(); i++) {
			FileIndex fi = cache->fileindex(i);
		}
		return true;
	}
	return false;
}

bool hasEnding (std::string const &fullString, std::string const &ending)
{
    if (fullString.length() >= ending.length()) {
        return (0 == fullString.compare (fullString.length() - ending.length(), ending.length(), ending));
    } else {
        return false;
    }
}

BinaryMapFile* initBinaryMapFile(std::string inputName) {
	GOOGLE_PROTOBUF_VERIFY_VERSION;
	std::map<std::string, BinaryMapFile*>::iterator iterator;
	if ((iterator = openFiles.find(inputName)) != openFiles.end()) {
		delete iterator->second;
		openFiles.erase(iterator);
	}

#if defined(_WIN32)
	int fileDescriptor = open(inputName.c_str(), O_RDONLY | O_BINARY);
	int routeDescriptor = open(inputName.c_str(), O_RDONLY | O_BINARY);
#else
	int fileDescriptor = open(inputName.c_str(), O_RDONLY);
	int routeDescriptor = open(inputName.c_str(), O_RDONLY);
#endif
	if (fileDescriptor < 0 || routeDescriptor < 0 || routeDescriptor == fileDescriptor) {
		osmand_log_print(LOG_ERROR, "File could not be open to read from C : %s", inputName.c_str());
		return NULL;
	}
	BinaryMapFile* mapFile = new BinaryMapFile();
	mapFile->fd = fileDescriptor;

	mapFile->routefd = routeDescriptor;
	FileIndex* fo = NULL;
	if (cache != NULL) {
		struct stat stat;
		fstat(fileDescriptor, &stat);
		for (int i = 0; i < cache->fileindex_size(); i++) {
			FileIndex fi = cache->fileindex(i);
			if (hasEnding(inputName, fi.filename()) && fi.size() == stat.st_size) {
				fo = cache->mutable_fileindex(i);
				break;
			}
		}
	}
	if (fo != NULL) {
		mapFile->version = fo->version();
		mapFile->dateCreated = fo->datemodified();
		for (int i = 0; i < fo->mapindex_size(); i++) {
			MapIndex mi;
			MapPart mp = fo->mapindex(i);
			mi.filePointer = mp.offset();
			mi.length = mp.size();
			mi.name = mp.name();
			for (int j = 0; j < mp.levels_size(); j++) {
				MapLevel ml = mp.levels(j);
				MapRoot mr;
				mr.bottom = ml.bottom();
				mr.left = ml.left();
				mr.right = ml.right();
				mr.top = ml.top();
				mr.maxZoom = ml.maxzoom();
				mr.minZoom = ml.minzoom();
				mr.filePointer = ml.offset();
				mr.length = ml.size();
				mi.levels.push_back(mr);
			}
			mapFile->basemap = mapFile->basemap || mi.name.find("basemap") != string::npos;
			mapFile->mapIndexes.push_back(mi);
			mapFile->indexes.push_back(&mapFile->mapIndexes.back());
		}

		for (int i = 0; i < fo->routingindex_size(); i++) {
			RoutingIndex mi;
			RoutingPart mp = fo->routingindex(i);
			mi.filePointer = mp.offset();
			mi.length = mp.size();
			mi.name = mp.name();
			for (int j = 0; j < mp.subregions_size(); j++) {
				RoutingSubregion ml = mp.subregions(j);
				RouteSubregion mr;
				mr.bottom = ml.bottom();
				mr.left = ml.left();
				mr.right = ml.right();
				mr.top = ml.top();
				mr.mapDataBlock = ml.shiftodata();
				mr.filePointer = ml.offset();
				mr.length = ml.size();
				osmand_log_print(LOG_DEBUG, "Native file init subregion %d %d %d ", mr.filePointer, mr.length, mr.mapDataBlock);
				mi.subregions.push_back(mr);
			}
			mapFile->routingIndexes.push_back(mi);
			mapFile->indexes.push_back(&mapFile->routingIndexes.back());
		}
		osmand_log_print(LOG_DEBUG, "Native file initialized from cache %s", inputName.c_str());
	} else {
		FileInputStream input(fileDescriptor);
		input.SetCloseOnDelete(false);
		CodedInputStream cis(&input);
		cis.SetTotalBytesLimit(INT_MAX, INT_MAX);

		if (!initMapStructure(&cis, mapFile)) {
			osmand_log_print(LOG_ERROR, "File not initialised : %s", inputName.c_str());
			delete mapFile;
			return NULL;
		}
	}
	mapFile->inputName = inputName;
	openFiles.insert(std::pair<std::string, BinaryMapFile*>(inputName, mapFile));
	return mapFile;
}
