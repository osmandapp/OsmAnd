#include "binaryRead.h"
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

void printFileInformation(const char* fileName, VerboseInfo* info) {
	BinaryMapFile* file = initBinaryMapFile(fileName);
	std::vector<MapIndex>::iterator it = file->mapIndexes.begin();
	time_t date = file->dateCreated/1000;
	printf("Obf file.\n Version %d, basemap %d, date %s \n", file->version,
			file->basemap, ctime(&date));

	for (; it != file->mapIndexes.end(); it++) {
		printf("  Name : %s \n", it.base()->name.c_str());
		std::vector<MapRoot>::iterator rt = it.base()->levels.begin();
		for (; rt != it.base()->levels.end(); rt++) {
			printf("  Level : %d - %d  size %d\n", rt->minZoom, rt->maxZoom, rt->length);
		}
	}
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
