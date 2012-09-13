#ifndef _OSMAND_RENDERING_H
#define _OSMAND_RENDERING_H

#include "common.h"
#include "renderRules.h"
#include <vector>
#include <SkCanvas.h>


void doRendering(std::vector <MapDataObject* > mapDataObjects, SkCanvas* canvas,
		RenderingRuleSearchRequest* req,	RenderingContext* rc);


#endif
