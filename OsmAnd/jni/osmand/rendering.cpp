#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <math.h>
#include <stdio.h>
#include <vector>
#include <set>
#include <hash_map>
#include <time.h>

#include <SkTypes.h>
#include <SkBitmap.h>
#include <SkColorFilter.h>
#include <SkShader.h>
#include <SkBitmapProcShader.h>
#include <SkPathEffect.h>
#include <SkBlurDrawLooper.h>
#include <SkDashPathEffect.h>
#include <SkCanvas.h>
#include <SkPaint.h>
#include <SkPath.h>

#include "common.h"
#include "renderRules.h"
#include "textdraw.cpp"
#include "mapObjects.h"

jclass jclass_JUnidecode;
jmethodID jmethod_JUnidecode_unidecode;

void calcPoint(MapDataObject* mObj, jint ind, RenderingContext* rc)
{
	rc->pointCount++;

	float tx = mObj->points.at(ind).first/ (rc->tileDivisor);
	float ty = mObj->points.at(ind).second / (rc->tileDivisor);

	float dTileX = tx - rc->leftX;
	float dTileY = ty - rc->topY;
	rc->calcX = rc->cosRotateTileSize * dTileX - rc->sinRotateTileSize * dTileY;
	rc->calcY = rc->sinRotateTileSize * dTileX + rc->cosRotateTileSize * dTileY;

	if (rc->calcX >= 0 && rc->calcX < rc->width && rc->calcY >= 0 && rc->calcY < rc->height)
		rc->pointInsideCount++;
}

void calcMultipolygonPoint(int xt, int yt, jint ind, jint b, RenderingContext* rc)
{
	rc->pointCount++;
	float tx = xt/ (rc->tileDivisor);
	float ty = yt / (rc->tileDivisor);

	float dTileX = tx - rc->leftX;
	float dTileY = ty - rc->topY;
	rc->calcX = rc->cosRotateTileSize * dTileX - rc->sinRotateTileSize * dTileY;
	rc->calcY = rc->sinRotateTileSize * dTileX + rc->cosRotateTileSize * dTileY;

	if (rc->calcX >= 0 && rc->calcX < rc->width && rc->calcY >= 0 && rc->calcY < rc->height)
		rc->pointInsideCount++;
}

std::hash_map<std::string, SkPathEffect*> pathEffects;
SkPathEffect* getDashEffect(std::string input)
{
	if(pathEffects.find(input) != pathEffects.end())
		return pathEffects[input];

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
				fval[flength++] = chars[i];
			}
			else
			{
				if(flength > 0)
				{
					fval[flength] = 0;
					primFloats[floatLen++] = atof(fval); flength = 0;
				}
			}
		}
	}
	SkPathEffect* r = new SkDashPathEffect(primFloats, floatLen, 0);
	pathEffects[input] = r;
	return r;
}

int updatePaint(RenderingRuleSearchRequest* req, SkPaint* paint, int ind, int area, RenderingContext* rc)
{
	RenderingRuleProperty* rColor;
	RenderingRuleProperty* rStrokeW;
	RenderingRuleProperty* rCap;
	RenderingRuleProperty* rPathEff;
	if (ind == 0)
	{
		rColor = req->props()->R_COLOR;
		rStrokeW = req->props()->R_STROKE_WIDTH;
		rCap = req->props()->R_CAP;
		rPathEff = req->props()->R_PATH_EFFECT;
	}
	else if (ind == 1)
	{
		rColor = req->props()->R_COLOR_2;
		rStrokeW = req->props()->R_STROKE_WIDTH_2;
		rCap = req->props()->R_CAP_2;
		rPathEff = req->props()->R_PATH_EFFECT_2;
	}
	else
	{
		rColor = req->props()->R_COLOR_3;
		rStrokeW = req->props()->R_STROKE_WIDTH_3;
		rCap = req->props()->R_CAP_3;
		rPathEff = req->props()->R_PATH_EFFECT_3;
	}
	paint->setColorFilter(NULL);
	paint->setShader(NULL);
	paint->setLooper(NULL);
	if (area)
	{
		paint->setStyle(SkPaint::kStrokeAndFill_Style);
		paint->setStrokeWidth(0);
	}
	else
	{
		float stroke = req->getFloatPropertyValue(rStrokeW);
		if (!(stroke > 0))
			return 0;

		paint->setStyle(SkPaint::kStroke_Style);
		paint->setStrokeWidth(stroke);
		std::string cap = req->getStringPropertyValue(rCap);
		std::string pathEff = req->getStringPropertyValue(rPathEff);

		if (cap == "BUTT" || cap == "")
			paint->setStrokeCap(SkPaint::kButt_Cap);
		else if (cap == "ROUND")
			paint->setStrokeCap(SkPaint::kRound_Cap);
		else if (cap == "SQUARE")
			paint->setStrokeCap(SkPaint::kSquare_Cap);
		else
			paint->setStrokeCap(SkPaint::kButt_Cap);

		if (pathEff.size() > 0)
		{
			SkPathEffect* p = getDashEffect(pathEff);
			paint->setPathEffect(p);
		}
		else
		{
			paint->setPathEffect(NULL);
		}
	}

	int color = req->getIntPropertyValue(rColor);
	paint->setColor(color);

	if (ind == 0)
	{
		std::string shader = req->getStringPropertyValue(req->props()->R_SHADER);
		if (shader.size() > 0)
		{
			SkBitmap* bmp = getCachedBitmap(rc, shader);
			if (bmp != NULL)
				paint->setShader(new SkBitmapProcShader(*bmp, SkShader::kRepeat_TileMode, SkShader::kRepeat_TileMode))->unref();
		}
	}

	// do not check shadow color here
	if (rc->shadowRenderingMode == 1 && ind == 0)
	{
		int shadowColor = req->getIntPropertyValue(req->props()->R_SHADOW_COLOR);
		int shadowLayer = req->getIntPropertyValue(req->props()->R_SHADOW_RADIUS);
		if (shadowColor == 0)
			shadowLayer = 0;

		if (shadowLayer > 0)
			paint->setLooper(new SkBlurDrawLooper(shadowLayer, 0, 0, shadowColor))->unref();
	}
	return 1;
}

void drawPointText(RenderingRuleSearchRequest* req, RenderingContext* rc, std::string tag, std::string value, float xText, float yText, std::string name, SkPath* path)
{
	if(rc->useEnglishNames)
	{
		jstring n = getGlobalJniEnv()->NewStringUTF(name.c_str());
		name = getString((jstring) getGlobalJniEnv()->CallStaticObjectMethod(jclass_JUnidecode, jmethod_JUnidecode_unidecode, n));
		getGlobalJniEnv()->DeleteLocalRef(n);
	}

	if (name.at(0) == REF_CHAR)
	{
		std::string ref = name.substr(1);
		name = "";
		for (uint k = 0; k < ref.length(); k++)
		{
			if (ref.at(k) == REF_CHAR)
			{
				if (k < ref.length() - 1) 
				{
					name = ref.substr(k + 1);
				}
				ref = ref.substr(0, k);
				break;
			}
		}
		if (ref.length() > 0)
		{
			req->setInitialTagValueZoom(tag, value, rc->zoom);
			req->setIntFilter(req->props()->R_TEXT_LENGTH, ref.length());
			req->setBooleanFilter(req->props()->R_REF, true);
			if (req->searchRule(RenderingRulesStorage::TEXT_RULES))
			{
				if (req->getIntPropertyValue(req->props()->R_TEXT_SIZE) > 0)
				{
					TextDrawInfo* text = new TextDrawInfo(ref);
					fillTextProperties(text, req, xText, yText);
					if (path != NULL)
						text->path = new SkPath(*path);

					rc->textToDraw.push_back(text);
				}
			}
		}
	}

	req->setInitialTagValueZoom(tag, value, rc->zoom);
	req->setIntFilter(req->props()->R_TEXT_LENGTH, name.length());
	req->setBooleanFilter(req->props()->R_REF, false);
	if (req->searchRule(RenderingRulesStorage::TEXT_RULES) &&
		req->getIntPropertyValue(req->props()->R_TEXT_SIZE) > 0)
	{
			TextDrawInfo* info = new TextDrawInfo(name);
			info->drawOnPath = (path != NULL) && (req->getIntPropertyValue(req->props()->R_TEXT_ON_PATH, 0) > 0);
			if (path != NULL)
				info->path = new SkPath(*path);

			fillTextProperties(info, req, xText, yText);
			rc->textToDraw.push_back(info);
	}
}

void drawPolylineShadow(SkCanvas* cv, SkPaint* paint, RenderingContext* rc, SkPath* path, int shadowColor, int shadowRadius)
{
		// blurred shadows
		if (rc->shadowRenderingMode == 2 && shadowRadius > 0) {
			// simply draw shadow? difference from option 3 ?
			// paint->setColor(0xffffffff);
			paint->setLooper(new SkBlurDrawLooper(shadowRadius, 0, 0, shadowColor))->unref();
			PROFILE_NATIVE_OPERATION(rc, cv->drawPath(*path, *paint));
		}

		// option shadow = 3 with solid border
		if (rc->shadowRenderingMode == 3 && shadowRadius > 0) {
			paint->setLooper(NULL);
			paint->setStrokeWidth(paint->getStrokeWidth() + shadowRadius * 2);
			//		paint->setColor(0xffbababa);
			paint->setColorFilter(SkColorFilter::CreateModeFilter(shadowColor, SkXfermode::kSrcIn_Mode))->unref();
			//		paint->setColor(shadowColor);
			PROFILE_NATIVE_OPERATION(rc, cv->drawPath(*path, *paint));
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
		PROFILE_NATIVE_OPERATION(rc, cv->drawPath(*p, oneWayPaints.at(i)));
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
				PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
				if (updatePaint(req, paint, 1, 0, rc)) {
					PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
					if (updatePaint(req, paint, 2, 0, rc)) {
						PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
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

		PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
		// for test purpose
		//	paint->setStyle(SkPaint::kStroke_Style);
		//	paint->setStrokeWidth(2);
		//	paint->setPathEffect(NULL);
		//	paint->setColor(BLACK_COLOR);
		//	PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
		if (updatePaint(req, paint, 1, 0, rc)) {
			PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
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

		PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
		if (updatePaint(req, paint, 1, 0, rc)) {
			PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
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
	std::string name;
	if (renderText)
		name = mObj->name;
	
	if (!bmp && name.length() == 0)
		return;
	
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
			//			__android_log_print(ANDROID_LOG_WARN, LOG_TAG, "Draw polyline");
			drawPolyline(mObj, req, cv, paint, rc, pair, layer, drawOnlyShadow);
		} else if (t == 3 && !drawOnlyShadow) {
			// polygon
			drawPolygon(mObj, req, cv, paint, rc, pair);
		}
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
					PROFILE_NATIVE_OPERATION(rc, canvas->drawBitmap(*ico, icon.x - ico->width() / 2, icon.y - ico->height() / 2, &p));
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
			rc->lastRenderedKey = *ks;
			if (rc->interrupted()) {
				return;
			}

		}

		drawIconsOverCanvas(rc, canvas);

		rc->textRendering.start();
		drawTextOverCanvas(rc, canvas);
		rc->textRendering.pause();
}

void loadJniRendering()
{
	jclass_JUnidecode = findClass("net/sf/junidecode/Junidecode");
	jmethod_JUnidecode_unidecode = getGlobalJniEnv()->GetStaticMethodID(jclass_JUnidecode, "unidecode", "(Ljava/lang/String;)Ljava/lang/String;");
}

extern "C" JNIEXPORT jobject JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering_1Direct( JNIEnv* ienv, jobject obj,
	jobject renderingContext, jint searchResult,
	jobject targetBitmap, 
	jboolean useEnglishNames, jobject renderingRuleSearchRequest, jint defaultColor) {
		setGlobalJniEnv(ienv);

		// Gain information about bitmap
		AndroidBitmapInfo bitmapInfo;
		if(AndroidBitmap_getInfo(getGlobalJniEnv(), targetBitmap, &bitmapInfo) != ANDROID_BITMAP_RESUT_SUCCESS)
			__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to execute AndroidBitmap_getInfo");

		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Creating SkBitmap in native w:%d h:%d s:%d f:%d!", bitmapInfo.width, bitmapInfo.height, bitmapInfo.stride, bitmapInfo.format);

		SkBitmap* bitmap = new SkBitmap();
		if(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
			int rowBytes = bitmapInfo.stride;
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Row bytes for RGBA_8888 is %d", rowBytes);
			bitmap->setConfig(SkBitmap::kARGB_8888_Config, bitmapInfo.width, bitmapInfo.height, rowBytes);
		} else if(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565) {
			int rowBytes = bitmapInfo.stride;
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Row bytes for RGB_565 is %d", rowBytes);
			bitmap->setConfig(SkBitmap::kRGB_565_Config, bitmapInfo.width, bitmapInfo.height, rowBytes);
		} else {
			__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Unknown target bitmap format");
		}

		void* lockedBitmapData = NULL;
		if(AndroidBitmap_lockPixels(getGlobalJniEnv(), targetBitmap, &lockedBitmapData) != ANDROID_BITMAP_RESUT_SUCCESS || !lockedBitmapData) {
			__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to execute AndroidBitmap_lockPixels");
		}
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Locked %d bytes at %p", bitmap->getSize(), lockedBitmapData);

		bitmap->setPixels(lockedBitmapData);

		SkCanvas* canvas = new SkCanvas(*bitmap);
		canvas->drawColor(defaultColor);

		SkPaint* paint = new SkPaint;
		paint->setAntiAlias(true);
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Initializing rendering");
		ElapsedTimer initObjects;
		initObjects.start();

		RenderingRuleSearchRequest* req = initSearchRequest(renderingRuleSearchRequest);
		RenderingContext rc;
		pullFromJavaRenderingContext(renderingContext, &rc);
		rc.useEnglishNames = useEnglishNames;
		SearchResult* result = ((SearchResult*) searchResult);
		//    std::vector <BaseMapDataObject* > mapDataObjects = marshalObjects(binaryMapDataObjects);

		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Rendering image");
		initObjects.pause();


		// Main part do rendering
		rc.nativeOperations.start();
		if(result != NULL) {
			doRendering(result->result, canvas, paint, req, &rc);
		}
		rc.nativeOperations.pause();

		pushToJavaRenderingContext(renderingContext, &rc);
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "End Rendering image");
		if(AndroidBitmap_unlockPixels(ienv, targetBitmap) != ANDROID_BITMAP_RESUT_SUCCESS) {
			__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to execute AndroidBitmap_unlockPixels");
		}

		// delete  variables
		delete paint;
		delete canvas;
		delete req;
		delete bitmap;
		//    deleteObjects(mapDataObjects);

		jclass resultClass = findClass("net/osmand/plus/render/NativeOsmandLibrary$RenderingGenerationResult");
		
		jmethodID resultClassCtorId = getGlobalJniEnv()->GetMethodID(resultClass, "<init>", "(Ljava/nio/ByteBuffer;)V");

#ifdef DEBUG_NAT_OPERATIONS
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG,"Native ok (init %d, native op %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#else
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Native ok (init %d, rendering %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#endif

		/* Construct a result object */
		jobject resultObject = getGlobalJniEnv()->NewObject(resultClass, resultClassCtorId, NULL);

		return resultObject;
}

void* bitmapData = NULL;
size_t bitmapDataSize = 0;
extern "C" JNIEXPORT jobject JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering_1Indirect( JNIEnv* ienv, jobject obj,
	jobject renderingContext, jint searchResult,
	jint requestedBitmapWidth, jint requestedBitmapHeight, jint rowBytes, jboolean isTransparent, 
	jboolean useEnglishNames, jobject renderingRuleSearchRequest, jint defaultColor) {
		setGlobalJniEnv(ienv);

		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Creating SkBitmap in native w:%d h:%d!", requestedBitmapWidth, requestedBitmapHeight);

		SkBitmap* bitmap = new SkBitmap();
		if(isTransparent == JNI_TRUE)
			bitmap->setConfig(SkBitmap::kARGB_8888_Config, requestedBitmapWidth, requestedBitmapHeight, rowBytes);
		else
			bitmap->setConfig(SkBitmap::kRGB_565_Config, requestedBitmapWidth, requestedBitmapHeight, rowBytes);

		if(bitmapData != NULL && bitmapDataSize != bitmap->getSize()) {
			free(bitmapData);
			bitmapData = NULL;
			bitmapDataSize = 0;
		}
		if(bitmapData == NULL && bitmapDataSize == 0) {
			bitmapDataSize = bitmap->getSize();
			bitmapData = malloc(bitmapDataSize);

			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Allocated %d bytes at %p", bitmapDataSize, bitmapData);
		}

		bitmap->setPixels(bitmapData);

		SkCanvas* canvas = new SkCanvas(*bitmap);
		canvas->drawColor(defaultColor);

		SkPaint* paint = new SkPaint;
		paint->setAntiAlias(true);
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Initializing rendering");
		ElapsedTimer initObjects;
		initObjects.start();

		RenderingRuleSearchRequest* req = initSearchRequest(renderingRuleSearchRequest);
		RenderingContext rc;
		pullFromJavaRenderingContext(renderingContext, &rc);
		rc.useEnglishNames = useEnglishNames;
		SearchResult* result = ((SearchResult*) searchResult);
		//    std::vector <BaseMapDataObject* > mapDataObjects = marshalObjects(binaryMapDataObjects);

		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Rendering image");
		initObjects.pause();


		// Main part do rendering
		rc.nativeOperations.start();
		if(result != NULL) {
			doRendering(result->result, canvas, paint, req, &rc);
		}
		rc.nativeOperations.pause();

		pushToJavaRenderingContext(renderingContext, &rc);
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "End Rendering image");

		// delete  variables
		delete paint;
		delete canvas;
		delete req;
		delete bitmap;
		//    deleteObjects(mapDataObjects);

		jclass resultClass = findClass("net/osmand/plus/render/NativeOsmandLibrary$RenderingGenerationResult");
		
		jmethodID resultClassCtorId = ienv->GetMethodID(resultClass, "<init>", "(Ljava/nio/ByteBuffer;)V");

#ifdef DEBUG_NAT_OPERATIONS
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Native ok (init %d, native op %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#else
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Native ok (init %d, rendering %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#endif

		// Allocate ctor paramters
		jobject bitmapBuffer = ienv->NewDirectByteBuffer(bitmapData, bitmap->getSize());

		/* Construct a result object */
		jobject resultObject = ienv->NewObject(resultClass, resultClassCtorId, bitmapBuffer);

		return resultObject;
}
