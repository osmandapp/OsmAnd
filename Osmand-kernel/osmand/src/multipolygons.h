#ifndef _MULTIPOLYGONS_H
#define _MULTIPOLYGONS_H

#include <stdio.h>
#include <map>
#include <set>

#include "renderRules.h"
#include "common.h"
#include "mapObjects.h"

/// !!! Fuly copied from MapRenderRepositories.java, should be carefully synchroinized
bool isClockwiseWay(std::vector<int_pair>& c) ;

bool calculateLineCoordinates(bool inside, int x, int y, bool pinside, int px, int py, int leftX, int rightX,
		int bottomY, int topY, std::vector<int_pair>& coordinates);

void combineMultipolygonLine(std::vector<coordinates>& completedRings, std::vector<coordinates>& incompletedRings,
			coordinates& coordinates);

void unifyIncompletedRings(std::vector<std::vector<int_pair> >& incompletedRings, std::vector<std::vector<int_pair> >& completedRings,
			int leftX, int rightX, int bottomY, int topY, long dbId, int zoom);


bool processCoastlines(std::vector<MapDataObject*>&  coastLines, int leftX, int rightX, int bottomY, int topY, int zoom,
		bool showIfThereIncompleted, bool addDebugIncompleted, std::vector<MapDataObject*>& res);

#endif
