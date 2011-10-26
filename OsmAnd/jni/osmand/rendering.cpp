#include <jni.h>
#include <math.h>
#include <android/log.h>
#include <stdio.h>
#include <vector>
#include <set>
#include <hash_map>
#include <time.h>
#include "SkTypes.h"
#include "SkBitmap.h"
#include "SkShader.h"
#include "SkBitmapProcShader.h"
#include "SkPathEffect.h"
#include "SkBlurDrawLooper.h"
#include "SkDashPathEffect.h"
#include "SkCanvas.h"
#include "SkPaint.h"
#include "SkPath.h"

#include "common.cpp"
#include "renderRules.cpp"
#include "textdraw.cpp"
#include "mapObjects.cpp"


char debugMessage[1024];


 void calcPoint(MapDataObject* mObj, jint ind, RenderingContext* rc) {
	rc->pointCount++;

	float tx = mObj->points.at(ind).first/ (rc->tileDivisor);
	float ty = mObj->points.at(ind).second / (rc->tileDivisor);

	float dTileX = tx - rc->leftX;
	float dTileY = ty - rc->topY;
	rc->calcX = rc->cosRotateTileSize * dTileX - rc->sinRotateTileSize * dTileY;
	rc->calcY = rc->sinRotateTileSize * dTileX + rc->cosRotateTileSize * dTileY;

	if (rc->calcX >= 0 && rc->calcX < rc->width && rc->calcY >= 0 && rc->calcY < rc->height) {
		rc->pointInsideCount++;
	}
}

 void calcMultipolygonPoint(int xt, int yt, jint ind, jint b, RenderingContext* rc) {
	rc->pointCount++;
	float tx = xt/ (rc->tileDivisor);
	float ty = yt / (rc->tileDivisor);

	float dTileX = tx - rc->leftX;
	float dTileY = ty - rc->topY;
	rc->calcX = rc->cosRotateTileSize * dTileX - rc->sinRotateTileSize * dTileY;
	rc->calcY = rc->sinRotateTileSize * dTileX + rc->cosRotateTileSize * dTileY;

	if (rc->calcX >= 0 && rc->calcX < rc->width && rc->calcY >= 0 && rc->calcY < rc->height) {
		rc->pointInsideCount++;
	}
}

std::hash_map<std::string, SkPathEffect*> pathEffects;
SkPathEffect* getDashEffect(std::string input){
	if(pathEffects.find(input) != pathEffects.end()) {
		return pathEffects[input];
	}
	const char* chars = input.c_str();
	int i = 0;
	char fval[10];
	int flength = 0;
	float primFloats[20];
	int floatLen = 0;
	for(;;i++)
	{
		if(chars[i] == 0)
		{
			if(flength > 0)	{ fval[flength] = 0;
			primFloats[floatLen++] = atof(fval); flength = 0;}
			break;
		}
		else
		{
			if(chars[i] != '_')
			{
				// suppose it is a character
				fval[flength++] = chars[i] ;
			} else {
				if(flength > 0)	{ fval[flength] = 0;
				primFloats[floatLen++] = atof(fval); flength = 0;}
			}
		}
	}
	SkPathEffect* r = new SkDashPathEffect(primFloats, floatLen, 0);
	pathEffects[input] = r;
	return r;
}




int updatePaint(RenderingRuleSearchRequest* req, SkPaint* paint, int ind, int area, RenderingContext* rc) {
	RenderingRuleProperty* rColor;
	RenderingRuleProperty* rStrokeW;
	RenderingRuleProperty* rCap;
	RenderingRuleProperty* rPathEff;
	if (ind == 0) {
		rColor = req->props()->R_COLOR;
		rStrokeW = req->props()->R_STROKE_WIDTH;
		rCap = req->props()->R_CAP;
		rPathEff = req->props()->R_PATH_EFFECT;
	} else if (ind == 1) {
		rColor = req->props()->R_COLOR_2;
		rStrokeW = req->props()->R_STROKE_WIDTH_2;
		rCap = req->props()->R_CAP_2;
		rPathEff = req->props()->R_PATH_EFFECT_2;
	} else {
		rColor = req->props()->R_COLOR_3;
		rStrokeW = req->props()->R_STROKE_WIDTH_3;
		rCap = req->props()->R_CAP_3;
		rPathEff = req->props()->R_PATH_EFFECT_3;
	}
	if (area) {
		paint->setStyle(SkPaint::kStrokeAndFill_Style);
		paint->setStrokeWidth(0);
	} else {
		float stroke = req->getFloatPropertyValue(rStrokeW);
		if (!(stroke > 0)) {
			return 0;
		}

		paint->setStyle(SkPaint::kStroke_Style);
		paint->setStrokeWidth(stroke);
		std::string cap = req->getStringPropertyValue(rCap);
		std::string pathEff = req->getStringPropertyValue(rPathEff);

		if (cap == "BUTT" || cap == "") {
			paint->setStrokeCap(SkPaint::kButt_Cap);
		} else if (cap == "ROUND") {
			paint->setStrokeCap(SkPaint::kRound_Cap);
		} else if (cap == "SQUARE") {
			paint->setStrokeCap(SkPaint::kSquare_Cap);
		} else {
			paint->setStrokeCap(SkPaint::kButt_Cap);
		}

		if (pathEff.size() > 0) {
			SkPathEffect* p = getDashEffect(pathEff);
			paint->setPathEffect(p);
		} else {
			paint->setPathEffect(NULL);
		}

	}

	int color = req->getIntPropertyValue(rColor);
	paint->setColor(color);

	if (ind == 0) {
		std::string shader = req->getStringPropertyValue(req->props()->R_SHADER);
		if (shader.size() > 0) {
			SkBitmap* bmp = getCachedBitmap(rc, shader);
			if (bmp == NULL) {
				paint->setShader(NULL);
			} else {
				paint->setShader(new SkBitmapProcShader(*bmp, SkShader::kRepeat_TileMode, SkShader::kRepeat_TileMode))->unref();
			}
		} else {
			paint->setShader(NULL);
		}
	} else {
		paint->setShader(NULL);
	}

	// do not check shadow color here
	if (rc->shadowRenderingMode != 1 || ind != 0) {
		paint->setLooper(NULL);
	} else {
		int shadowColor = req->getIntPropertyValue(req->props()->R_SHADOW_COLOR);
		int shadowLayer = req->getIntPropertyValue(req->props()->R_SHADOW_RADIUS);
		if (shadowColor == 0) {
			shadowLayer = 0;
		}
		if (shadowLayer > 0) {
			paint->setLooper(new SkBlurDrawLooper(shadowLayer, 0, 0, shadowColor))->unref();
		}
		paint->setLooper(NULL);
	}
	return 1;
}

void drawPointText(RenderingRuleSearchRequest* req, RenderingContext* rc, std::string tag, std::string value,
		float xText, float yText, std::string name, SkPath* path)
{
	if (name.at(0) == REF_CHAR) {
		std::string ref = name.substr(1);
		name = ""; //$NON-NLS-1$
		for (uint k = 0; k < ref.length(); k++) {
			if (ref.at(k) == REF_CHAR) {
				if (k < ref.length() - 1) {
					name = ref.substr(k + 1);
				}
				ref = ref.substr(0, k);
				break;
			}
		}
		if (ref.length() > 0) {
			req->setInitialTagValueZoom(tag, value, rc->zoom);
			req->setIntFilter(req->props()->R_TEXT_LENGTH, ref.length());
			req->setBooleanFilter(req->props()->R_REF, true);
			if (req->searchRule(RenderingRulesStorage::TEXT_RULES)) {
				if (req->getIntPropertyValue(req->props()->R_TEXT_SIZE) > 0) {
					TextDrawInfo* text = new TextDrawInfo(ref);
					fillTextProperties(text, req, xText, yText);
					if (path != NULL) {
						text->path = new SkPath(*path);
					}
					rc->textToDraw.push_back(text);
				}
			}
		}
	}

	req->setInitialTagValueZoom(tag, value, rc->zoom);
	req->setIntFilter(req->props()->R_TEXT_LENGTH, name.length());
	req->setBooleanFilter(req->props()->R_REF, false);
	if (req->searchRule(RenderingRulesStorage::TEXT_RULES) &&
			req->getIntPropertyValue(req->props()->R_TEXT_SIZE) > 0) {
		TextDrawInfo* info = new TextDrawInfo(name);
		info->drawOnPath = (path != NULL) && (req->getIntPropertyValue(req->props()->R_TEXT_ON_PATH, 0) > 0);
		if (path != NULL) {
			info->path = new SkPath(*path);
		}
		fillTextProperties(info, req, xText, yText);
		rc->textToDraw.push_back(info);
	}
}


void drawPolylineShadow(SkCanvas* cv, SkPaint* paint, RenderingContext* rc, SkPath* path, int shadowColor,
		int shadowRadius) {
	// blurred shadows
	if (rc->shadowRenderingMode == 2 && shadowRadius > 0) {
		// simply draw shadow? difference from option 3 ?
		// paint->setColor(0xffffffff);
		paint->setLooper(new SkBlurDrawLooper(shadowRadius, 0, 0, shadowColor))->unref();
		NAT_COUNT(rc, cv->drawPath(*path, *paint));
	}

	// option shadow = 3 with solid border
	if (rc->shadowRenderingMode == 3 && shadowRadius > 0) {
		paint->setLooper(NULL);
		paint->setStrokeWidth(paint->getStrokeWidth() + shadowRadius * 2);
		paint->setColor(0xffbababa);
//		paint->setColor(shadowColor);
		NAT_COUNT(rc, cv->drawPath(*path, *paint));
	}
}

std::vector<SkPaint> oneWayPaints;
SkPaint* oneWayPaint(){
	SkPaint* oneWay = new SkPaint;
	oneWay->setStyle(SkPaint::kStroke_Style);
	oneWay->setColor(0xff6c70d5);
	oneWay->setAntiAlias(true);
	return oneWay;
}
void drawOneWayPaints(RenderingContext* rc, SkCanvas* cv, SkPath* p) {
	if (oneWayPaints.size() == 0) {
		SkPathEffect* arrowDashEffect1 = new SkDashPathEffect((float []){ 0, 12, 10, 152 }, 4, 0);
		SkPathEffect* arrowDashEffect2 = new SkDashPathEffect((float[]){ 0, 12, 9, 153 }, 4, 1);
		SkPathEffect* arrowDashEffect3 = new SkDashPathEffect((float[]){ 0, 18, 2, 154 }, 4, 1);
		SkPathEffect* arrowDashEffect4 = new SkDashPathEffect((float[]){ 0, 18, 1, 155 }, 4, 1);

		SkPaint* p = oneWayPaint();
		p->setStrokeWidth(1);
		p->setPathEffect(arrowDashEffect1)->unref();
		oneWayPaints.push_back(*p);

		p = oneWayPaint();
		p->setStrokeWidth(2);
		p->setPathEffect(arrowDashEffect2)->unref();
		oneWayPaints.push_back(*p);

		p = oneWayPaint();
		p->setStrokeWidth(3);
		p->setPathEffect(arrowDashEffect3)->unref();
		oneWayPaints.push_back(*p);

		p = oneWayPaint();
		p->setStrokeWidth(4);
		p->setPathEffect(arrowDashEffect4)->unref();
		oneWayPaints.push_back(*p);
	}

	for (size_t i = 0; i < oneWayPaints.size(); i++) {
		NAT_COUNT(rc, cv->drawPath(*p, oneWayPaints.at(i)));
	}
}

bool isOneWayWay(int highwayAttributes) {
	return (highwayAttributes & 1) > 0;
}

bool isRoundabout(int highwayAttributes) {
	return ((highwayAttributes >> 2) & 1) > 0;
}

void drawPolyline(MapDataObject* mObj, RenderingRuleSearchRequest* req, SkCanvas* cv, SkPaint* paint,
		RenderingContext* rc, std::pair<std::string, std::string> pair, int layer, int drawOnlyShadow) {
	jint length = mObj->points.size();
	if (length < 2) {
		return;
	}
	std::string tag = pair.first;
	std::string value = pair.second;

	req->setInitialTagValueZoom(tag, value, rc->zoom);
	req->setIntFilter(req->props()->R_LAYER, layer);
	bool oneway = false;
	if (rc->zoom >= 16 && "highway" == pair.first && isOneWayWay(mObj->highwayAttributes)) {
		oneway = true;
	}

	bool rendered = req->searchRule(2);
	if (!rendered || !updatePaint(req, paint, 0, 0, rc)) {
		return;
	}

	rc->visible++;
	SkPath path;
	int i = 0;
	SkPoint middlePoint;
	int middle = length / 2;
	for (; i < length; i++) {
		calcPoint(mObj, i, rc);
		if (i == 0) {
			path.moveTo(rc->calcX, rc->calcY);
		} else {
			if(i == middle){
				middlePoint.set(rc->calcX, rc->calcY);
			}
			path.lineTo(rc->calcX, rc->calcY);
		}
	}
	if (i > 0) {
		if (drawOnlyShadow) {
			int shadowColor = req->getIntPropertyValue(req->props()->R_SHADOW_COLOR);
			int shadowRadius = req->getIntPropertyValue(req->props()->R_SHADOW_RADIUS);
			drawPolylineShadow(cv, paint, rc, &path, shadowColor, shadowRadius);
		} else {
			NAT_COUNT(rc, cv->drawPath(path, *paint));
			if (updatePaint(req, paint, 1, 0, rc)) {
				NAT_COUNT(rc, cv->drawPath(path, *paint));
				if (updatePaint(req, paint, 2, 0, rc)) {
					NAT_COUNT(rc, cv->drawPath(path, *paint));
				}
			}
			if (oneway && !drawOnlyShadow) {
				drawOneWayPaints(rc, cv, &path);
			}
			if (!drawOnlyShadow && mObj->name.length() > 0) {
				drawPointText(req, rc,pair.first, pair.second, middlePoint.fX, middlePoint.fY, mObj->name,
						&path);
			}
		}
	}
}

void drawMultiPolygon(MultiPolygonObject* mapObject,RenderingRuleSearchRequest* req, SkCanvas* cv, SkPaint* paint,
 		  		RenderingContext* rc) {
	if (req == NULL) {
		return;
	}
	req->setInitialTagValueZoom(mapObject->tag, mapObject->value, rc->zoom);
	bool rendered = req->searchRule(3);

	if (!rendered || !updatePaint(req, paint, 0, 1, rc)) {
		return;
	}

	int boundsCount = mapObject->points.size();
	rc->visible++;
	SkPath path;

	for (int i = 0; i < boundsCount; i++) {
		int cnt = mapObject->points.at(i).size();
		float xText = 0;
		float yText = 0;
		for (int j = 0; j < cnt; j++) {
			std::pair<int,int> pair = mapObject->points.at(i).at(j);
			calcMultipolygonPoint(pair.first, pair.second, j, i, rc);
			xText += rc->calcX;
			yText += rc->calcY;
			if (j == 0) {
				path.moveTo(rc->calcX, rc->calcY);
			} else {
				path.lineTo(rc->calcX, rc->calcY);
			}
		}
		if (cnt > 0) {
			std::string name = mapObject->names.at(i);
			if (name.length() > 0) {
				drawPointText(req, rc, mapObject->tag, mapObject->value, xText / cnt, yText / cnt, name, NULL);
			}
		}
	}

	NAT_COUNT(rc, cv->drawPath(path, *paint));
	// for test purpose
	// render.strokeWidth = 1.5f;
	// render.color = Color.BLACK;
	if (updatePaint(req, paint, 1, 0, rc)) {
		NAT_COUNT(rc, cv->drawPath(path, *paint));
	}
}


void drawPolygon(MapDataObject* mObj, RenderingRuleSearchRequest* req, SkCanvas* cv, SkPaint* paint,
		  		RenderingContext* rc, std::pair<std::string, std::string> pair) {
	jint length = mObj->points.size();
	if (length <= 2) {
		return;
	}
	std::string tag = pair.first;
	std::string value = pair.second;

	req->setInitialTagValueZoom(tag, value, rc->zoom);
	bool rendered = req->searchRule(3);

	float xText = 0;
	float yText = 0;
	if (!rendered || !updatePaint(req, paint, 0, 1, rc)) {
		return;
	}

	rc->visible++;
	SkPath path;
	int i = 0;
	for (; i < length; i++) {
		calcPoint(mObj, i, rc);
		if (i == 0) {
			path.moveTo(rc->calcX, rc->calcY);
		} else {
			path.lineTo(rc->calcX, rc->calcY);
		}
		xText += rc->calcX;
		yText += rc->calcY;
	}

	NAT_COUNT(rc, cv->drawPath(path, *paint));
	if (updatePaint(req, paint, 1, 0, rc)) {
		NAT_COUNT(rc, cv->drawPath(path, *paint));
	}
	std::string name = mObj->name;
	if (name.length() > 0) {
		drawPointText(req, rc, tag, value, xText / length, yText / length, name, NULL);
	}
}

void drawPoint(MapDataObject* mObj,	RenderingRuleSearchRequest* req, SkCanvas* cv, SkPaint* paint,
 		RenderingContext* rc, std::pair<std::string, std::string>  pair, int renderText)
 {
	std::string tag = pair.first;
	std::string value = pair.second;

	req->setInitialTagValueZoom(tag, value, rc->zoom);
	req->searchRule(1);
	std::string resId = req->getStringPropertyValue(req-> props()-> R_ICON);
	SkBitmap* bmp = getCachedBitmap(rc, resId);
	std::string name = EMPTY_STRING;
	if (renderText) {
		name = mObj->name;
	}
	if (!bmp && name.length() == 0) {
		return;
	}


	jint length = mObj->points.size();
	rc->visible++;
	float px = 0;
	float py = 0;
	int i = 0;
	for (; i < length; i++) {
		calcPoint(mObj, i, rc);
		px += rc->calcX;
		py += rc->calcY;
	}
	if (length > 1) {
		px /= length;
		py /= length;
	}

	if (bmp != NULL) {
		IconDrawInfo ico;
		ico.x = px;
		ico.y = py;
		ico.bmp = bmp;
		rc->iconsToDraw.push_back(ico);
	}
	if (name.length() > 0) {
		drawPointText(req, rc, tag, value, px, py, name, NULL);
	}

}

// 0 - normal, -1 - under, 1 - bridge,over
int getNegativeWayLayer(int type) {
	int i = (3 & (type >> 12));
	if (i == 1) {
		return -1;
	} else if (i == 2) {
		return 1;
	}
	return 0;
}

void drawObject(RenderingContext* rc, BaseMapDataObject* mapObject, SkCanvas* cv, RenderingRuleSearchRequest* req,
		SkPaint* paint, int l, int renderText, int drawOnlyShadow) {
	rc->allObjects++;
	if (mapObject-> type == BaseMapDataObject::MULTI_POLYGON) {
		if (!drawOnlyShadow) {
			drawMultiPolygon((MultiPolygonObject*) mapObject, req, cv, paint, rc);
		}
		return;
	}
	MapDataObject* mObj = (MapDataObject*) mapObject;

	jint mainType = mObj->types.at(l);
	int t = mainType & 3;

	std::pair<std::string, std::string> pair = mObj->tagValues.at(l);
	if (t == 1 && !drawOnlyShadow) {
		// point
		drawPoint(mObj, req, cv, paint, rc, pair, renderText);
	} else if (t == 2) {
		// polyline
		int layer = getNegativeWayLayer(mainType);
//			__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Draw polyline");
		drawPolyline(mObj, req, cv, paint, rc, pair, layer, drawOnlyShadow);
	} else if (t == 3 && !drawOnlyShadow) {
		// polygon
		drawPolygon(mObj, req, cv, paint, rc, pair);
	}
}




void copyRenderingContext(jobject orc, RenderingContext* rc) 
{
	rc->leftX = env->GetFloatField( orc, getFid( RenderingContextClass, "leftX", "F" ) );
	rc->topY = env->GetFloatField( orc, getFid( RenderingContextClass, "topY", "F" ) );
	rc->width = env->GetIntField( orc, getFid( RenderingContextClass, "width", "I" ) );
	rc->height = env->GetIntField( orc, getFid( RenderingContextClass, "height", "I" ) );
	
	
	rc->zoom = env->GetIntField( orc, getFid( RenderingContextClass, "zoom", "I" ) );
	rc->rotate = env->GetFloatField( orc, getFid( RenderingContextClass, "rotate", "F" ) );
	rc->tileDivisor = env->GetFloatField( orc, getFid( RenderingContextClass, "tileDivisor", "F" ) );
	
	rc->pointCount = env->GetIntField( orc, getFid( RenderingContextClass, "pointCount", "I" ) );
	rc->pointInsideCount = env->GetIntField( orc, getFid( RenderingContextClass, "pointInsideCount", "I" ) );
	rc->visible = env->GetIntField( orc, getFid( RenderingContextClass, "visible", "I" ) );
	rc->allObjects = env->GetIntField( orc, getFid( RenderingContextClass, "allObjects", "I" ) );
	
	rc->cosRotateTileSize = env->GetFloatField( orc, getFid( RenderingContextClass, "cosRotateTileSize", "F" ) );
	rc->sinRotateTileSize = env->GetFloatField( orc, getFid( RenderingContextClass, "sinRotateTileSize", "F" ) );
	rc->density = env->GetFloatField( orc, getFid( RenderingContextClass, "density", "F" ) );
	rc->highResMode = env->GetBooleanField( orc, getFid( RenderingContextClass, "highResMode", "Z" ) );
	rc->mapTextSize = env->GetFloatField( orc, getFid( RenderingContextClass, "mapTextSize", "F" ) );


	rc->shadowRenderingMode = env->GetIntField( orc, getFid( RenderingContextClass, "shadowRenderingMode", "I" ) );
	rc->shadowLevelMin = env->GetIntField( orc, getFid( RenderingContextClass, "shadowLevelMin", "I" ) );
	rc->shadowLevelMax = env->GetIntField( orc, getFid( RenderingContextClass, "shadowLevelMax", "I" ) );
	rc->androidContext = env->GetObjectField(orc, getFid( RenderingContextClass, "ctx", "Landroid/content/Context;"));
	
	rc->originalRC = orc; 

}


void mergeRenderingContext(jobject orc, RenderingContext* rc) 
{
	env->SetIntField( orc, getFid(RenderingContextClass, "pointCount", "I" ) , rc->pointCount);
	env->SetIntField( orc, getFid(RenderingContextClass, "pointInsideCount", "I" ) , rc->pointInsideCount);
	env->SetIntField( orc, getFid(RenderingContextClass, "visible", "I" ) , rc->visible);
	env->SetIntField( orc, getFid(RenderingContextClass, "allObjects", "I" ) , rc->allObjects);
	env->SetIntField( orc, getFid(RenderingContextClass, "textRenderingTime", "I" ) , rc->textRendering.getElapsedTime());
	env->DeleteLocalRef(rc->androidContext);

}

void loadLibrary(jobject rc) {
	loadJniCommon(rc);
	loadJNIRenderingRules();
	loadJniMapObjects();
}

void unloadLibrary() {
	unloadJniMapObjects();
	unloadJniRenderRules();
	unloadJniCommon();
}


void drawIconsOverCanvas(RenderingContext* rc, SkCanvas* canvas)
{
	int skewConstant = (int) getDensityValue(rc, 16);
	int iconsW = rc -> width / skewConstant;
	int iconsH = rc -> height / skewConstant;
	int len = (iconsW * iconsH) / 32;
	int alreadyDrawnIcons[len];
	memset(alreadyDrawnIcons, 0, sizeof(int)*len);
	size_t ji = 0;
	SkPaint p;
	p.setStyle(SkPaint::kStroke_Style);
	for(;ji< rc->iconsToDraw.size(); ji++)
	{
		IconDrawInfo icon = rc->iconsToDraw.at(ji);
		if (icon.y >= 0 && icon.y < rc -> height && icon.x >= 0 && icon.x < rc -> width &&
				icon.bmp != NULL) {
			int z = (((int) icon.x / skewConstant) + ((int) icon.y / skewConstant) * iconsW);
			int i = z / 32;
			if (i >= len) {
				continue;
			}
			int ind = alreadyDrawnIcons[i];
			int b = z % 32;
			// check bit b if it is set
			if (((ind >> b) & 1) == 0) {
				alreadyDrawnIcons[i] = ind | (1 << b);
				SkBitmap* ico = icon.bmp;
				NAT_COUNT(rc, canvas->drawBitmap(*ico, icon.x - ico->width() / 2, icon.y - ico->height() / 2, &p));
			}
		}
		if(rc->interrupted()){
			return;
		}
	}
}

std::hash_map<int, std::vector<int> > sortObjectsByProperOrder(std::vector <BaseMapDataObject* > mapDataObjects,
			RenderingRuleSearchRequest* req, RenderingContext* rc) {
	std::hash_map<int, std::vector<int> > orderMap;
	if (req != NULL) {
		req->clearState();
		const size_t size = mapDataObjects.size();
		size_t i = 0;
		for (; i < size; i++) {
			uint sh = i << 8;
			BaseMapDataObject* obj = mapDataObjects.at(i);
			if (obj->type == BaseMapDataObject::MULTI_POLYGON) {
				MultiPolygonObject* mobj = (MultiPolygonObject*) obj;

				req->setTagValueZoomLayer(mobj->tag, mobj->value, rc->zoom, mobj->layer);
				req->setIntFilter(req->props()->R_ORDER_TYPE, RenderingRulesStorage::POLYGON_RULES);
				if (req->searchRule(RenderingRulesStorage::ORDER_RULES)) {
					int order = req->getIntPropertyValue(req->props()->R_ORDER);
					orderMap[order].push_back(sh);
					if (req->getIntPropertyValue(req->props()->R_SHADOW_LEVEL) > 0) {
						rc->shadowLevelMin = std::min(rc->shadowLevelMin, order);
						rc->shadowLevelMax = std::max(rc->shadowLevelMax, order);
						req->clearIntvalue(req->props()->R_SHADOW_LEVEL);
					}
				}
			} else {
				MapDataObject* mobj = (MapDataObject*) obj;
				size_t sizeTypes = mobj->types.size();
				size_t j = 0;
				for (; j < sizeTypes; j++) {
					int wholeType = mobj->types.at(j);
					int mask = wholeType & 3;
					int layer = 0;
					if (mask != 1) {
						layer = getNegativeWayLayer(wholeType);
					}
					std::pair<std::string, std::string> pair = mobj->tagValues.at(j);
					req->setTagValueZoomLayer(pair.first, pair.second, rc->zoom, layer);
					req->setIntFilter(req->props()->R_ORDER_TYPE, mask);
					if (req->searchRule(RenderingRulesStorage::ORDER_RULES)) {
						int order = req->getIntPropertyValue(req->props()->R_ORDER);
						orderMap[order].push_back(sh + j);
						if (req->getIntPropertyValue(req->props()->R_SHADOW_LEVEL) > 0) {
							rc->shadowLevelMin = std::min(rc->shadowLevelMin, order);
							rc->shadowLevelMax = std::max(rc->shadowLevelMax, order);
							req->clearIntvalue(req->props()->R_SHADOW_LEVEL);
						}
					}
				}

			}
		}
	}
	return orderMap;
}

void doRendering(std::vector <BaseMapDataObject* > mapDataObjects, SkCanvas* canvas, SkPaint* paint,
		RenderingRuleSearchRequest* req, RenderingContext* rc) {
	// put in order map
	std::hash_map<int, std::vector<int> > orderMap = sortObjectsByProperOrder(mapDataObjects, req, rc);
	std::set<int> keys;
	std::hash_map<int, std::vector<int> >::iterator it = orderMap.begin();
	while(it != orderMap.end())
	{
		keys.insert(it->first);
		it++;
	}
	bool shadowDrawn = false;

	for (std::set<int>::iterator ks = keys.begin(); ks != keys.end() ; ks++) {
		if (!shadowDrawn && *ks >= rc->shadowLevelMin && *ks <= rc->shadowLevelMax &&
				rc->shadowRenderingMode > 1) {
			for (std::set<int>::iterator ki = ks; ki != keys.end() ; ki++) {
				if (*ki > rc->shadowLevelMax || rc->interrupted()) {
					break;
				}
				std::vector<int> list = orderMap[*ki];
				for (std::vector<int>::iterator ls = list.begin(); ls != list.end(); ls++) {
					int i = *ls;
					int ind = i >> 8;
					int l = i & 0xff;
					BaseMapDataObject* mapObject = mapDataObjects.at(ind);

					// show text only for main type
					drawObject(rc, mapObject, canvas, req, paint, l, l == 0, true);
				}
			}
			shadowDrawn = true;
		}

		std::vector<int> list = orderMap[*ks];
		for (std::vector<int>::iterator ls = list.begin(); ls != list.end(); ls++) {
			int i = *ls;
			int ind = i >> 8;
			int l = i & 0xff;

			BaseMapDataObject* mapObject = mapDataObjects.at(ind);
			// show text only for main type
			drawObject(rc, mapObject, canvas, req, paint, l, l == 0, false);

		}
		if (rc->interrupted()) {
			return;
		}

	}

	drawIconsOverCanvas(rc, canvas);

	rc->textRendering.start();
	drawTextOverCanvas(rc, canvas);
	rc->textRendering.pause();
}


#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jstring JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering( JNIEnv* ienv,
		jobject obj, jobject renderingContext, jobjectArray binaryMapDataObjects, jobject bmpObj,
		jboolean useEnglishNames, jobject renderingRuleSearchRequest, jint defaultColor) {
	if(!env) {
	   env = ienv;
	   loadLibrary(renderingContext);
	}
	SkBitmap* bmp = getNativeBitmap(bmpObj);
	sprintf(debugMessage, "Image w:%d h:%d !", bmp->width(), bmp->height());
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", debugMessage);
	SkCanvas* canvas = new SkCanvas(*bmp);
	canvas->drawColor(defaultColor);

	SkPaint* paint = new SkPaint;
	paint->setAntiAlias(true);

	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Initializing rendering");
	watcher initObjects;
	initObjects.start();


	RenderingRuleSearchRequest* req =  initSearchRequest(renderingRuleSearchRequest);

    RenderingContext rc;
    copyRenderingContext(renderingContext, &rc);
    std::vector <BaseMapDataObject* > mapDataObjects = marshalObjects(binaryMapDataObjects);


    __android_log_print(ANDROID_LOG_WARN, "net.osmand", "Rendering image");
    initObjects.pause();
    

    // Main part do rendering
    rc.nativeOperations.start();
    doRendering(mapDataObjects, canvas, paint, req, &rc);
    rc.nativeOperations.pause();

    mergeRenderingContext(renderingContext, &rc);
    __android_log_print(ANDROID_LOG_WARN, "net.osmand", "End Rendering image");

    // delete  variables
    delete paint;
    delete canvas;
    delete req;
    deleteObjects(mapDataObjects);

#ifdef DEBUG_NAT_OPERATIONS
    sprintf(debugMessage, "Native ok (init %d, native op %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#else
    sprintf(debugMessage, "Native ok (init %d, rendering %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#endif
    jstring result = env->NewStringUTF( debugMessage);

//  unloadLibrary();
	return result;
}

#ifdef __cplusplus
}
#endif
