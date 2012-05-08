#include "binaryRead.h"
#include "renderRules.h"
#include "rendering.h"
#include <SkImageEncoder.h>
#include <stdio.h>
#include <time.h>

void println(const char * msg) {
	printf("%s\n", msg);
}

void printUsage(std::string info) {
	if(info.size() > 0) {
		println(info.c_str());
	}
	println("Inspector is console utility for working with binary indexes of OsmAnd.");
	println("It allows print info about file, extract parts and merge indexes.");
	println("\nUsage for print info : inspector [-renderingOutputFile=..] [-vaddress] [-vmap] [-vpoi] [-vtransport] [-zoom=Zoom] [-bbox=LeftLon,TopLat,RightLon,BottomLan] [file]");
	println("  Prints information about [file] binary index of OsmAnd.");
	println("  -v.. more verbouse output (like all cities and their streets or all map objects with tags/values and coordinates)");
	println("  -renderingOutputFile= renders for specified zoom, bbox into a file");
}

class RenderingInfo {
public:
	int left, right, top, bottom;
	double lattop, lonleft;
	int tileWX, tileHY;
	std::string tileFileName;
	std::string renderingFileName;
	std::string imagesFileName;
	int zoom;
	int width ;
	int height;

	RenderingInfo(int argc, char **params) {
		double l1, l2;
		char s[100];
		int z, z1, z2 ;
		lattop = 85;
		tileHY = 2;
		lonleft = -180;
		tileWX = 2;
		zoom = 15;
		for (int i = 1; i != argc; ++i) {
			if (sscanf(params[i], "-renderingOutputFile=%s", s)) {
				tileFileName = s;
			} else if (sscanf(params[i], "-imagesBasePathFile=%s", s)) {
				imagesFileName = s;
			} else if (sscanf(params[i], "-renderingStyleFile=%s", s)) {
				renderingFileName = s;
			} else if (sscanf(params[i], "-zoom=%d", &z)) {
				zoom = z;
			} else if (sscanf(params[i], "-lbox=%lg,%lg", &l1, &l2)) {
				lonleft = l1;
				lattop = l2;
			} else if (sscanf(params[i], "-lt=%d,%d", &z1, &z2)) {
				tileWX = z1;
				tileHY = z2;
			}
		}

		left = get31TileNumberX(lonleft);
		top = get31TileNumberY(lattop);
		right = left + tileWX * (1 << (31 - zoom));
		bottom = top + tileHY * (1 << (31 - zoom));
		width = tileWX * TILE_SIZE;
		height = tileHY * TILE_SIZE;
	}
};

class VerboseInfo {
public:
	bool vaddress;
	bool vtransport;
	bool vpoi;
	bool vmap;
	double lattop, latbottom, lonleft, lonright;
	int zoom;

	VerboseInfo(int argc, char **params) {
		lattop = 85;
		latbottom = -85;
		lonleft = -180;
		lonright = 180;
		zoom = 15;
		int z;
		double l1, l2, l3, l4;
		for (int i = 1; i != argc; ++i) {
			if (strcmp(params[i], "-vaddress") == 0) {
				vaddress = true;
			} else if (strcmp(params[i], "-vmap") == 0) {
				vmap = true;
			} else if (strcmp(params[i], "-vpoi") == 0) {
				vpoi = true;
			} else if (strcmp(params[i], "-vtransport") == 0) {
				vtransport = true;
			} else if (sscanf(params[i], "-zoom=%d", &z)) {
				zoom = z;
			} else if (sscanf(params[i], "-bbox=%le,%le,%le,%le", &l1, &l2, &l3, &l4)) {
				lonleft = l1;
				lattop = l2;
				lonright = l3;
				latbottom = l4;

			}
		}
	}
};

//		public boolean contains(MapObject o){
//			return lattop >= o.getLocation().getLatitude() && latbottom <= o.getLocation().getLatitude()
//					&& lonleft <= o.getLocation().getLongitude() && lonright >= o.getLocation().getLongitude();
//
//		}


const char* formatBounds(int left, int right, int top, int bottom){
	float l = get31LongitudeX(left);
	float r = get31LongitudeX(right);
	float t = get31LatitudeY(top);
	float b = get31LatitudeY(bottom);
	char* ch = new char[150];
	sprintf(ch, "(left top - right bottom) : %g, %g NE - %g, %g NE", l, t,r, b);
	return ch;
}


void printFileInformation(const char* fileName, VerboseInfo* verbose) {
	BinaryMapFile* file = initBinaryMapFile(fileName);
	std::vector<BinaryPartIndex*>::iterator its = file->indexes.begin();
	time_t date = file->dateCreated/1000;
	printf("Obf file.\n Version %d, basemap %d, date %s \n", file->version,
			file->basemap, ctime(&date));

	int i = 1;
	for (; its != file->indexes.end(); its++, i++) {
		BinaryPartIndex* it = *its;
		std::string partname = "";
		if (it->type == MAP_INDEX) {
			partname = "Map";
		} else if (it->type == TRANSPORT_INDEX) {
			partname = "Transport";
		} else if (it->type == POI_INDEX) {
			partname = "Poi";
		} else if (it->type == ADDRESS_INDEX) {
			partname = "Address";
		}
		printf("%d. %s data %s - %d bytes\n", i, partname.c_str(), it->name.c_str(), it->length);
		if (it->type == MAP_INDEX) {
			MapIndex* m = ((MapIndex*) it);
			int j = 1;
			std::vector<MapRoot>::iterator rt = m->levels.begin();
			for (; rt != m->levels.end(); rt++) {
				const char* ch = formatBounds(rt->left, rt->right, rt->top, rt->bottom);
				printf("\t%d.%d Map level minZoom = %d, maxZoom = %d, size = %d bytes \n\t\t Bounds %s \n",
						i, j++, rt->minZoom, rt->maxZoom, rt->length, ch);
			}
			if ((verbose != NULL && verbose->vmap)) {
				// FIXME
				//printMapDetailInfo(verbose, index);
			}
		} else if (it->type == TRANSPORT_INDEX) {
			// FIXME
//			TransportIndex ti = ((TransportIndex) p);
//			int sh = (31 - BinaryMapIndexReader.TRANSPORT_STOP_ZOOM);
//			println(
//					"\t Bounds "
//							+ formatBounds(ti.getLeft() << sh, ti.getRight() << sh, ti.getTop() << sh,
//									ti.getBottom() << sh));
		} else if (it->type == POI_INDEX && (verbose != NULL && verbose->vpoi)) {
			//printPOIDetailInfo(verbose, index, (PoiRegion) p);
		} else if (it->type == ADDRESS_INDEX && (verbose != NULL && verbose->vaddress)) {
//			printAddressDetailedInfo(verbose, index);
		}

	}
}


void runSimpleRendering( string renderingFileName, string resourceDir, RenderingInfo* info) {
	SkColor defaultMapColor = SK_ColorLTGRAY;

	if (info->width > 10000 || info->height > 10000) {
		osmand_log_print(LOG_ERROR, "We don't rendering images more than 10000x10000 ");
		return;
	}

	osmand_log_print(LOG_INFO, "Rendering info bounds(%d, %d, %d, %d) zoom(%d), width/height(%d/%d) tilewidth/tileheight(%d/%d) fileName(%s)",
			info->left, info->top, info->right, info->bottom, info->zoom, info->width, info->height, info->tileWX, info->tileHY, info->tileFileName.c_str());
	RenderingRulesStorage* st = new RenderingRulesStorage(renderingFileName.c_str());
	st->parseRulesFromXmlInputStream(renderingFileName.c_str(), NULL);
	RenderingRuleSearchRequest* searchRequest = new RenderingRuleSearchRequest(st);
	SearchQuery q(floor(info->left), floor(info->right), ceil(info->top), ceil(info->bottom), searchRequest,
			new ResultPublisher());
	q.zoom = info->zoom;

	ResultPublisher* res = searchObjectsForRendering(&q, true, "Nothing found");
	osmand_log_print(LOG_INFO, "Found %d objects", res->result.size());

	SkBitmap* bitmap = new SkBitmap();
	bitmap->setConfig(SkBitmap::kRGB_565_Config, info->width, info->height);

	size_t bitmapDataSize = bitmap->getSize();
	void* bitmapData = malloc(bitmapDataSize);
	bitmap->setPixels(bitmapData);

	osmand_log_print(LOG_INFO, "Initializing rendering style and rendering context");
	ElapsedTimer initObjects;
	initObjects.start();

	RenderingContext rc;
	rc.setDefaultIconsDir(resourceDir);
	searchRequest->clearState();
	searchRequest->setIntFilter(st->PROPS.R_MINZOOM, info->zoom);
	if (searchRequest->searchRenderingAttribute(A_DEFAULT_COLOR)) {
		defaultMapColor = searchRequest->getIntPropertyValue(searchRequest->props()->R_ATTR_COLOR_VALUE);
	}
	searchRequest->clearState();
	searchRequest->setIntFilter(st->PROPS.R_MINZOOM, info->zoom);
	if (searchRequest->searchRenderingAttribute(A_SHADOW_RENDERING)) {
		rc.setShadowRenderingMode(searchRequest->getIntPropertyValue(searchRequest->props()->R_ATTR_INT_VALUE));
		//rc.setShadowRenderingColor(searchRequest->getIntPropertyValue(searchRequest->props()->R_SHADOW_COLOR));
	}
	rc.setLocation(
			((double)info->left)/getPowZoom(31-info->zoom),
			((double)info->top)/getPowZoom(31-info->zoom)
			);
	rc.setDimension(info->width, info->height);
	rc.setZoom(info->zoom);
	rc.setRotate(0);
	rc.setDensityScale(1);
	osmand_log_print(LOG_INFO, "Rendering image");
	initObjects.pause();
	SkCanvas* canvas = new SkCanvas(*bitmap);
	canvas->drawColor(defaultMapColor);
	doRendering(res->result, canvas, searchRequest, &rc);
	osmand_log_print(LOG_INFO, "End Rendering image");
	osmand_log_print(LOG_INFO, "Native ok (init %d, rendering %d) ", initObjects.getElapsedTime(),
			rc.nativeOperations.getElapsedTime());
	SkImageEncoder* enc = SkImageEncoder::Create(SkImageEncoder::kPNG_Type);
	if (enc != NULL && !enc->encodeFile(info->tileFileName.c_str(), *bitmap, 100)) {
		osmand_log_print(LOG_ERROR, "FAIL to save tile to %s", info->tileFileName.c_str());
	} else {
		osmand_log_print(LOG_INFO, "Tile successfully saved to %s", info->tileFileName.c_str());
	}
	delete canvas;
	delete bitmap;
	free(bitmapData);
	return;
}



void testRenderingRuleStorage(const char* basePath, const char* name) {
	string filePath = string(basePath) + string(name);
	RenderingRulesStorage* st = new RenderingRulesStorage(filePath.c_str());
	st->parseRulesFromXmlInputStream(filePath.c_str(),
			new BasePathRenderingRulesStorageResolver(string(basePath)));
	st->printDebug(RenderingRulesStorage::TEXT_RULES);
	RenderingRuleSearchRequest* searchRequest = new RenderingRuleSearchRequest(st);
	searchRequest->setStringFilter(st->PROPS.R_TAG, "highway");
	searchRequest->setStringFilter(st->PROPS.R_VALUE, "motorway");
	searchRequest->setIntFilter(st->PROPS.R_LAYER, 1);
	searchRequest->setIntFilter(st->PROPS.R_MINZOOM, 15);
	searchRequest->setIntFilter(st->PROPS.R_MAXZOOM, 15);
	//	searchRequest.setBooleanFilter(storage.PROPS.R_NIGHT_MODE, true);
	// searchRequest.setBooleanFilter(storage.PROPS.get("hmRendered"), true);

	bool res = searchRequest->search(RenderingRulesStorage::LINE_RULES, true);
	printf("Result %d\n", res);
	searchRequest->printDebugResult();
}



int main(int argc, char **argv) {
	if (argc <= 1) {
		// 1. Test Rendering rule storage
//		testRenderingRuleStorage("/home/victor/projects/OsmAnd/git/DataExtractionOSM/src/net/osmand/render/",
//				"test_depends.render.xml"
//				"default.render.xml"
//				);
		// 2. Test simple rendering
		printUsage("");
		return 1;
	}
	const char* f = argv[1];
	if (f[0] == '-') {
		// command
		if (f[1]=='v') {
			if (argc < 2) {
				printUsage("Missing file parameter");
			} else {
				VerboseInfo* vinfo = new VerboseInfo(argc, argv);
				printFileInformation(argv[argc -1], vinfo);
			}
		} else if (f[1]=='r') {
			if (argc < 2) {
				printUsage("Missing file parameter");
			} else {
				RenderingInfo* info = new RenderingInfo(argc, argv);
				char s[100];
				for (int i = 1; i != argc; ++i) {
					if (sscanf(argv[i], "-renderingInputFile=%s", s)) {
						BinaryMapFile* mf = initBinaryMapFile(s);
						osmand_log_print(LOG_INFO, "Init %d (success) binary map file %s.", mf->version,
								mf->inputName.c_str());
					}
				}
				runSimpleRendering(info->renderingFileName, info->imagesFileName, info);
			}
		} else {
			printUsage("Unknown command");
		}
	} else {
		printFileInformation(f, NULL);
	}

}
