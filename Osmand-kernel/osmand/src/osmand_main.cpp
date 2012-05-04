#include "binaryRead.h"
#include "rendering.h"
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
	println("\nUsage for print info : inspector [-vaddress] [-vmap] [-vpoi] [-vtransport] [-zoom=Zoom] [-bbox=LeftLon,TopLat,RightLon,BottomLan] [file]");
	println("  Prints information about [file] binary index of OsmAnd.");
	println("  -v.. more verbouse output (like all cities and their streets or all map objects with tags/values and coordinates)");
}

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
		for (int i = 1; i != argc; ++i) {
			if (strcmp(params[i], "-vaddress") == 0) {
				vaddress = true;
			} else if (strcmp(params[i], "-vmap") == 0) {
				vmap = true;
			} else if (strcmp(params[i], "-vpoi") == 0) {
				vpoi = true;
			} else if (strcmp(params[i], "-vtransport") == 0) {
				vtransport = true;
			} else {
				int z = 0;
				if (sscanf(params[i], "-zoom=%d", &z) != EOF) {
					zoom = z;
				} else if (sscanf(params[i], "-bbox=%le,%le,%le,%le", &lonleft, &lattop, &lonright, &latbottom) != EOF) {

				}
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

SkColor defaultMapColor = SK_ColorLTGRAY;
void runSimpleRendering(int zoom, int left, int right, int top, int bottom, int rowBytes) {
	// TODO
	initBinaryMapFile("");
	// TODO not implemented (read storage from file)
	RenderingRulesStorage* st = NULL; //createRenderingRulesStorage(env, storage);
	RenderingRuleSearchRequest* req = new RenderingRuleSearchRequest(st);
	// TODO init rule search request
//	initRenderingRuleSearchRequest(env, res, renderingRuleSearchRequest);

	SearchQuery q(left, right, top, bottom, req, new ResultPublisher());
	q.zoom = zoom;
	ResultPublisher* res = searchObjectsForRendering(&q, req, true, "Nothing found");

	SkBitmap* bitmap = new SkBitmap();
	bitmap->setConfig(SkBitmap::kRGB_565_Config, 800, 800, rowBytes);
//	  size_t bitmapDataSize = bitmap->getSize();
//	  void* bitmapData bitmapData = malloc(bitmapDataSize);
//	       bitmap->setPixels(bitmapData);

	osmand_log_print(LOG_INFO, "Initializing rendering");
	ElapsedTimer initObjects;
	initObjects.start();

	RenderingContext rc;
	osmand_log_print(LOG_INFO, "Rendering image");
	initObjects.pause();
	SkCanvas* canvas = new SkCanvas(*bitmap);
	canvas->drawColor(defaultMapColor);
	doRendering(res->result, canvas, req, &rc);

	osmand_log_print(LOG_INFO, "End Rendering image");
	osmand_log_print(LOG_INFO, "Native ok (init %d, rendering %d) ", initObjects.getElapsedTime(),
			rc.nativeOperations.getElapsedTime());
	return;
}

int main(int argc, char **argv) {
	if (argc <= 1) {
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
		} else {
			printUsage("Unknown command");
		}
	} else {
		printFileInformation(f, NULL);
	}

}
