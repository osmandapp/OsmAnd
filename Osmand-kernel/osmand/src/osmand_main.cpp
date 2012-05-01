#include "binaryRead.h"

int main() {
	BinaryMapFile* file = initBinaryMapFile("/home/victor/projects/OsmAnd/data/osm-gen/basemap_2.obf");
	std::vector<MapIndex>::iterator it =  file->mapIndexes.begin();
	printf("File initialsed %d \n", file != NULL);
	for(; it != file->mapIndexes.end(); it++) {
		printf("  Name : %s \n", it.base()->name.c_str());
		std::vector<MapRoot>::iterator rt = it.base()->levels.begin();
		for(; rt != it.base()->levels.end(); rt++) {
			printf("  Level : %d - %d  size %d\n", rt->minZoom, rt->maxZoom, rt->length);
		}
	}
}
