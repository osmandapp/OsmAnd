#include "osmand_log.h"


#include <math.h>
#include <stdio.h>
#include <vector>
#include <algorithm>
#include <set>
#include <time.h>

#include <SkTypes.h>
#include <SkBitmap.h>
#include <SkCanvas.h>
#include <SkColorFilter.h>
#include <SkShader.h>
#include <SkBitmapProcShader.h>
#include <SkPathEffect.h>
#include <SkBlurDrawLooper.h>
#include <SkDashPathEffect.h>
#include <SkPaint.h>
#include <SkPath.h>

#include "common.h"
#include "renderRules.h"
#include "binaryRead.h"
#include "textdraw.cpp"
#include "mapObjects.h"
#include "rendering.h"



struct MapDataObjectPrimitive {
	MapDataObject* obj;
	int typeInd;
	float order;
	int objectType;
};

const int MAX_V = 32;
void calcPoint(std::pair<int, int>  c, RenderingContext* rc)
{
    rc->pointCount++;

	float tx = c.first/ (rc->tileDivisor);
	float ty = c.second / (rc->tileDivisor);

    float dTileX = tx - rc->getLeft();
    float dTileY = ty - rc->getTop();
    rc->calcX = rc->cosRotateTileSize * dTileX - rc->sinRotateTileSize * dTileY;
    rc->calcY = rc->sinRotateTileSize * dTileX + rc->cosRotateTileSize * dTileY;

    if (rc->calcX >= 0 && rc->calcX < rc->getWidth()&& rc->calcY >= 0 && rc->calcY < rc->getHeight())
        rc->pointInsideCount++;
}


UNORDERED(map)<std::string, SkPathEffect*> pathEffects;
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
    else if (ind == -1)
    {
        rColor = req->props()->R_COLOR_0;
        rStrokeW = req->props()->R_STROKE_WIDTH_0;
        rCap = req->props()->R_CAP_0;
        rPathEff = req->props()->R_PATH_EFFECT_0;
    }
    else if (ind == -2)
    {
        rColor = req->props()->R_COLOR__1;
        rStrokeW = req->props()->R_STROKE_WIDTH__1;
        rCap = req->props()->R_CAP__1;
        rPathEff = req->props()->R_PATH_EFFECT__1;
    }
    else
    {
        rColor = req->props()->R_COLOR_3;
        rStrokeW = req->props()->R_STROKE_WIDTH_3;
        rCap = req->props()->R_CAP_3;
        rPathEff = req->props()->R_PATH_EFFECT_3;
    }

    if (area)
    {
    	paint->setColorFilter(NULL);
    	paint->setShader(NULL);
    	paint->setLooper(NULL);
        paint->setStyle(SkPaint::kStrokeAndFill_Style);
        paint->setStrokeWidth(0);
    }
    else
    {
        float stroke = req->getFloatPropertyValue(rStrokeW);
        if (!(stroke > 0))
            return 0;
        paint->setColorFilter(NULL);
        paint->setShader(NULL);
        paint->setLooper(NULL);

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
    if (rc->getShadowRenderingMode() == 1 && ind == 0)
    {
        int shadowColor = req->getIntPropertyValue(req->props()->R_SHADOW_COLOR);
        int shadowLayer = req->getIntPropertyValue(req->props()->R_SHADOW_RADIUS);
        if (shadowColor == 0) {
			shadowColor = rc->getShadowRenderingColor();
		}
        if (shadowColor == 0)
            shadowLayer = 0;

        if (shadowLayer > 0)
            paint->setLooper(new SkBlurDrawLooper(shadowLayer, 0, 0, shadowColor))->unref();
    }
    return 1;
}

void renderText(MapDataObject* obj, RenderingRuleSearchRequest* req, RenderingContext* rc, std::string tag,
		std::string value, float xText, float yText, SkPath* path) {
	UNORDERED(map)<std::string, std::string>::iterator it = obj->objectNames.begin();
	while (it != obj->objectNames.end()) {
		if (it->second.length() > 0) {
			std::string name = it->second;
			name =rc->getTranslatedString(name);
			name =rc->getReshapedString(name);
			req->setInitialTagValueZoom(tag, value, rc->getZoom(), obj);
			req->setIntFilter(req->props()->R_TEXT_LENGTH, name.length());
			std::string tagName = it->first == "name" ? "" : it->first;
			req->setStringFilter(req->props()->R_NAME_TAG, tagName);
			if (req->searchRule(RenderingRulesStorage::TEXT_RULES)
					&& req->getIntPropertyValue(req->props()->R_TEXT_SIZE) > 0) {
				TextDrawInfo* info = new TextDrawInfo(name);
				info->drawOnPath = (path != NULL) && (req->getIntPropertyValue(req->props()->R_TEXT_ON_PATH, 0) > 0);
				if (path != NULL)
					info->path = new SkPath(*path);

				fillTextProperties(info, req, xText, yText);
				rc->textToDraw.push_back(info);
			}
		}

		it++;
	}

}

void drawPolylineShadow(SkCanvas* cv, SkPaint* paint, RenderingContext* rc, SkPath* path, int shadowColor, int shadowRadius)
{
    // blurred shadows
    if (rc->getShadowRenderingMode() == 2 && shadowRadius > 0) {
        // simply draw shadow? difference from option 3 ?
        // paint->setColor(0xffffffff);
        paint->setLooper(new SkBlurDrawLooper(shadowRadius, 0, 0, shadowColor))->unref();
        PROFILE_NATIVE_OPERATION(rc, cv->drawPath(*path, *paint));
    }

    // option shadow = 3 with solid border
    if (rc->getShadowRenderingMode() == 3 && shadowRadius > 0) {
        paint->setLooper(NULL);
        paint->setStrokeWidth(paint->getStrokeWidth() + shadowRadius * 2);
        //		paint->setColor(0xffbababa);
        paint->setColorFilter(SkColorFilter::CreateModeFilter(shadowColor, SkXfermode::kSrcIn_Mode))->unref();
        //		paint->setColor(shadowColor);
        PROFILE_NATIVE_OPERATION(rc, cv->drawPath(*path, *paint));
    }
}

std::vector<SkPaint> oneWayPaints;
std::vector<SkPaint> reverseWayPaints;
SkPaint* oneWayPaint(){
    SkPaint* oneWay = new SkPaint;
    oneWay->setStyle(SkPaint::kStroke_Style);
    oneWay->setColor(0xff6c70d5);
    oneWay->setAntiAlias(true);
    return oneWay;
}
void drawOneWayPaints(RenderingContext* rc, SkCanvas* cv, SkPath* p, int oneway) {
	if (oneWayPaints.size() == 0) {
        const float intervals_oneway[4][4] = {
            {0, 12, 10, 152},
            {0, 12, 9, 153},
            {0, 18, 2, 154},
            {0, 18, 1, 155}
        };
		SkPathEffect* arrowDashEffect1 = new SkDashPathEffect(intervals_oneway[0], 4, 0);
		SkPathEffect* arrowDashEffect2 = new SkDashPathEffect(intervals_oneway[1], 4, 1);
		SkPathEffect* arrowDashEffect3 = new SkDashPathEffect(intervals_oneway[2], 4, 1);
		SkPathEffect* arrowDashEffect4 = new SkDashPathEffect(intervals_oneway[3], 4, 1);

		SkPaint* p = oneWayPaint();
		p->setStrokeWidth(1);
		p->setPathEffect(arrowDashEffect1)->unref();
		oneWayPaints.push_back(*p);
		delete p;

		p = oneWayPaint();
		p->setStrokeWidth(2);
		p->setPathEffect(arrowDashEffect2)->unref();
		oneWayPaints.push_back(*p);
		delete p;

		p = oneWayPaint();
		p->setStrokeWidth(3);
		p->setPathEffect(arrowDashEffect3)->unref();
		oneWayPaints.push_back(*p);
		delete p;

		p = oneWayPaint();
		p->setStrokeWidth(4);
		p->setPathEffect(arrowDashEffect4)->unref();
		oneWayPaints.push_back(*p);
		delete p;
	}
	if (reverseWayPaints.size() == 0) {
            const float intervals_reverse[4][4] = {
                {0, 12, 10, 152},
                {0, 13, 9, 152},
                {0, 14, 2, 158},
                {0, 15, 1, 158}
            };
		SkPathEffect* arrowDashEffect1 = new SkDashPathEffect(intervals_reverse[0], 4, 0);
		SkPathEffect* arrowDashEffect2 = new SkDashPathEffect(intervals_reverse[1], 4, 1);
		SkPathEffect* arrowDashEffect3 = new SkDashPathEffect(intervals_reverse[2], 4, 1);
		SkPathEffect* arrowDashEffect4 = new SkDashPathEffect(intervals_reverse[3], 4, 1);
		SkPaint* p = oneWayPaint();
		p->setStrokeWidth(1);
		p->setPathEffect(arrowDashEffect1)->unref();
		reverseWayPaints.push_back(*p);
		delete p;

		p = oneWayPaint();
		p->setStrokeWidth(2);
		p->setPathEffect(arrowDashEffect2)->unref();
		reverseWayPaints.push_back(*p);
		delete p;

		p = oneWayPaint();
		p->setStrokeWidth(3);
		p->setPathEffect(arrowDashEffect3)->unref();
		reverseWayPaints.push_back(*p);
		delete p;

		p = oneWayPaint();
		p->setStrokeWidth(4);
		p->setPathEffect(arrowDashEffect4)->unref();
		reverseWayPaints.push_back(*p);
		delete p;
	}
	if (oneway > 0) {
		for (size_t i = 0; i < oneWayPaints.size(); i++) {
			PROFILE_NATIVE_OPERATION(rc, cv->drawPath(*p, oneWayPaints.at(i)));
		}
	} else {
		for (size_t i = 0; i < reverseWayPaints.size(); i++) {
			PROFILE_NATIVE_OPERATION(rc, cv->drawPath(*p, reverseWayPaints.at(i)));
		}
	}
}



void drawPolyline(MapDataObject* mObj, RenderingRuleSearchRequest* req, SkCanvas* cv, SkPaint* paint,
	RenderingContext* rc, tag_value pair, int layer, int drawOnlyShadow) {
	size_t length = mObj->points.size();
	if (length < 2) {
		return;
	}
	std::string tag = pair.first;
	std::string value = pair.second;

	req->setInitialTagValueZoom(tag, value, rc->getZoom(), mObj);
	req->setIntFilter(req->props()->R_LAYER, layer);
	bool rendered = req->searchRule(2);
	if (!rendered || !updatePaint(req, paint, 0, 0, rc)) {
		return;
	}
	int shadowColor = req->getIntPropertyValue(req->props()->R_SHADOW_COLOR);
	int shadowRadius = req->getIntPropertyValue(req->props()->R_SHADOW_RADIUS);
	if(drawOnlyShadow && shadowRadius == 0) {
		return;
	}
	if(shadowColor == 0) {
		shadowColor = rc->getShadowRenderingColor();
	}
	int oneway = 0;
	if (rc->getZoom() >= 16 && pair.first == "highway") {
		if (mObj->containsAdditional("oneway", "yes")) {
			oneway = 1;
		} else if (mObj->containsAdditional("oneway", "-1")) {
			oneway = -1;
		}
	}

	rc->visible++;
	SkPath path;
	int i = 0;
	SkPoint middlePoint;
	int middle = length / 2;
	float prevx;
	float prevy;
	bool intersect = false;
	int prevCross = 0;
	for (; i < length; i++) {
		calcPoint(mObj->points.at(i), rc);
		if (i == 0) {
			path.moveTo(rc->calcX, rc->calcY);
		} else {
			if (i == middle) {
				middlePoint.set(rc->calcX, rc->calcY);
			}
			path.lineTo(rc->calcX, rc->calcY);
		}
		if (!intersect) {
			if (rc->calcX >= 0 && rc->calcY >= 0 && rc->calcX < rc->getWidth() && rc->calcY < rc->getHeight()) {
				intersect = true;
			} else {
				int cross = 0;
				cross |= (rc->calcX < 0 ? 1 : 0);
				cross |= (rc->calcX > rc->getWidth() ? 2 : 0);
				cross |= (rc->calcY < 0 ? 4 : 0);
				cross |= (rc->calcY > rc->getHeight() ? 8 : 0);
				if(i > 0) {
					if((prevCross & cross) == 0) {
						intersect = true;
					}
				}
				prevCross = cross;
			}
		}
	}

	if (!intersect) {
		return;
	}

	if (i > 0) {
		if (drawOnlyShadow) {
			drawPolylineShadow(cv, paint, rc, &path, shadowColor, shadowRadius);
		} else {
			if (updatePaint(req, paint, -2, 0, rc)) {
				PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
			}
			if (updatePaint(req, paint, -1, 0, rc)) {
				PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
			}
			if (updatePaint(req, paint, 0, 0, rc)) {
				PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
			}
			PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
			if (updatePaint(req, paint, 1, 0, rc)) {
				PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
			}
			if (updatePaint(req, paint, 2, 0, rc)) {
				PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
			}
			if (oneway && !drawOnlyShadow) {
				drawOneWayPaints(rc, cv, &path, oneway);
			}
			if (!drawOnlyShadow) {
				renderText(mObj, req, rc, pair.first, pair.second, middlePoint.fX, middlePoint.fY, &path);
			}
		}
	}
}


void drawPolygon(MapDataObject* mObj, RenderingRuleSearchRequest* req, SkCanvas* cv, SkPaint* paint,
	RenderingContext* rc, tag_value pair) {
	size_t length = mObj->points.size();
	if (length <= 2) {
		return;
	}
	std::string tag = pair.first;
	std::string value = pair.second;

	req->setInitialTagValueZoom(tag, value, rc->getZoom(), mObj);
	bool rendered = req->searchRule(3);

	float xText = 0;
	float yText = 0;
	if (!rendered || !updatePaint(req, paint, 0, 1, rc)) {
		return;
	}

	rc->visible++;
	SkPath path;
	int i = 0;
	bool containsPoint = false;
	int bounds = 0;
	for (; i < length; i++) {
		calcPoint(mObj->points.at(i), rc);
		if (i == 0) {
			path.moveTo(rc->calcX, rc->calcY);
		} else {
			path.lineTo(rc->calcX, rc->calcY);
		}
		float tx = rc->calcX;
		if (tx < 0) {
			tx = 0;
		}
		if (tx > rc->getWidth()) {
			tx = rc->getWidth();
		}
		float ty = rc->calcY;
		if (ty < 0) {
			ty = 0;
		}
		if (ty > rc->getHeight()) {
			ty = rc->getHeight();
		}
		xText += tx;
		yText += ty;
		if (!containsPoint) {
			if (rc->calcX >= 0 && rc->calcY >= 0 && rc->calcX < rc->getWidth() && rc->calcY < rc->getHeight()) {
				containsPoint = true;
			}
			bounds |= (rc->calcX < 0 ? 1 : 0);
			bounds |= (rc->calcX >= rc->getWidth() ? 2 : 0);
			bounds |= (rc->calcY < 0 ? 4 : 0);
			bounds |= (rc->calcY >= rc->getHeight() ? 8 : 0);
		}
	}
	xText /= length;
	yText /= length;
	if(!containsPoint){
		if(bounds == 15) {
			xText = rc->getWidth() / 2;
			yText = rc->getHeight() / 2;
		} else {
			return;
		}
	}
	std::vector<coordinates> polygonInnerCoordinates = mObj->polygonInnerCoordinates;
	if (polygonInnerCoordinates.size() > 0) {
		path.setFillType(SkPath::kEvenOdd_FillType);
		for (int j = 0; j < polygonInnerCoordinates.size(); j++) {
			coordinates cs = polygonInnerCoordinates.at(j);
			for (int i = 0; i < cs.size(); i++) {
				calcPoint(cs[i], rc);
				if (i == 0) {
					path.moveTo(rc->calcX, rc->calcY);
				} else {
					path.lineTo(rc->calcX, rc->calcY);
				}
			}
		}
	}

	PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
	if (updatePaint(req, paint, 1, 0, rc)) {
		PROFILE_NATIVE_OPERATION(rc, cv->drawPath(path, *paint));
	}

	renderText(mObj, req, rc, pair.first, pair.second, xText, yText, NULL);
}

void drawPoint(MapDataObject* mObj,	RenderingRuleSearchRequest* req, SkCanvas* cv, SkPaint* paint,
	RenderingContext* rc, std::pair<std::string, std::string>  pair, int renderTxt)
{
	std::string tag = pair.first;
	std::string value = pair.second;

	req->setInitialTagValueZoom(tag, value, rc->getZoom(), mObj);
	req->searchRule(1);
	std::string resId = req->getStringPropertyValue(req-> props()-> R_ICON);
	SkBitmap* bmp = getCachedBitmap(rc, resId);
	
	if (!bmp && !renderText)
		return;
	
	size_t length = mObj->points.size();
	rc->visible++;
	float px = 0;
	float py = 0;
	int i = 0;
	for (; i < length; i++) {
		calcPoint(mObj->points.at(i), rc);
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
	if (renderTxt) {
		renderText(mObj, req, rc, pair.first, pair.second, px, py, NULL);
	}

}

void drawObject(RenderingContext* rc,  SkCanvas* cv, RenderingRuleSearchRequest* req,
	SkPaint* paint, vector<MapDataObjectPrimitive>& array, int objOrder) {

	double limit = 100;
	for (int i = 0; i < array.size(); i++) {
		rc->allObjects++;
		MapDataObject* mObj = array[i].obj;
		tag_value pair = mObj->types.at(array[i].typeInd);
		if (objOrder == 0) {
			if (array[i].order < limit) {
				return;
			}
			// polygon
			drawPolygon(mObj, req, cv, paint, rc, pair);
		} else if (objOrder == 1) {
			drawPolyline(mObj, req, cv, paint, rc, pair, mObj->getSimpleLayer(), true);
		} else if (objOrder == 2) {
			drawPolyline(mObj, req, cv, paint, rc, pair, mObj->getSimpleLayer(), false);
		} else if (objOrder == 3) {
			drawPoint(mObj, req, cv, paint, rc, pair, array[i].typeInd == 0);
		}
		if (i % 25 == 0 && rc->interrupted()) {
			return;
		}
	}
}


void drawIconsOverCanvas(RenderingContext* rc, SkCanvas* canvas)
{
	int skewConstant = (int) rc->getDensityValue(16);
	int iconsW = rc -> getWidth() / skewConstant;
	int iconsH = rc -> getHeight() / skewConstant;
	int len = (iconsW * iconsH) / 32;
	int alreadyDrawnIcons[len];
	memset(alreadyDrawnIcons, 0, sizeof(int)*len);
	size_t ji = 0;
	SkPaint p;
	p.setStyle(SkPaint::kStroke_Style);
	p.setFilterBitmap(true);
	for(;ji< rc->iconsToDraw.size(); ji++)
	{
		IconDrawInfo icon = rc->iconsToDraw.at(ji);
		if (icon.y >= 0 && icon.y < rc->getHeight() && icon.x >= 0 && icon.x < rc->getWidth() && icon.bmp != NULL) {
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
				float left = icon.x - rc->getDensityValue(ico->width() / 2);
				float top = icon.y - rc->getDensityValue(ico->height() / 2);
				SkRect r = SkRect::MakeXYWH(left, top, rc->getDensityValue(ico->width()),
						rc->getDensityValue(ico->height()));
				PROFILE_NATIVE_OPERATION(rc, canvas->drawBitmapRect(*ico, (SkIRect*) NULL, r, &p));
			}
		}
		if (rc->interrupted()) {
			return;
		}
	}
}

double polygonArea(MapDataObject* obj, float mult) {
	double area = 0.;
	int j = obj->points.size() - 1;
	for (int i = 0; i < obj->points.size(); i++) {
		int_pair x = obj->points[i] ;
		int_pair y = obj->points[j];
		area += (y.first + ((float) x.first) )* (y.second- ((float)x.second));
		j = i;
	}
	return std::abs(area) * mult * mult * .5;
}


bool sortByOrder(const MapDataObjectPrimitive& i,const MapDataObjectPrimitive& j) { return (i.order<j.order); }
bool sortByRevOrder(const MapDataObjectPrimitive& i,const MapDataObjectPrimitive& j) { return (i.order>j.order); }

void sortObjectsByProperOrder(std::vector <MapDataObject* > mapDataObjects,
	RenderingRuleSearchRequest* req, RenderingContext* rc,
		std::vector<MapDataObjectPrimitive>&  polygonsArray, std::vector<MapDataObjectPrimitive>&  pointsArray,
		std::vector<MapDataObjectPrimitive>&  linesArray) {
	if (req != NULL) {
		req->clearState();
		const int size = mapDataObjects.size();
		float mult = 1. / getPowZoom(max(31 - (rc->getZoom() + 8), 0));
		int i = 0;
		for (; i < size; i++) {
			uint32_t sh = i << 8;
			MapDataObject* mobj = mapDataObjects[i];
			size_t sizeTypes = mobj->types.size();
			size_t j = 0;
			for (; j < sizeTypes; j++) {
				int layer = mobj->getSimpleLayer();
				tag_value pair = mobj->types[j];
				req->setTagValueZoomLayer(pair.first, pair.second, rc->getZoom(), layer, mobj);
				req->setIntFilter(req->props()->R_AREA, mobj->area);
				req->setIntFilter(req->props()->R_POINT, mobj->points.size() == 1);
				req->setIntFilter(req->props()->R_CYCLE, mobj->cycle());
				if (req->searchRule(RenderingRulesStorage::ORDER_RULES)) {
					int objectType = req->getIntPropertyValue(req->props()->R_OBJECT_TYPE);
					int order = req->getIntPropertyValue(req->props()->R_ORDER);
					MapDataObjectPrimitive mapObj;
					mapObj.objectType = objectType;
					mapObj.order = order;
					mapObj.typeInd = j;
					mapObj.obj = mobj;
					// polygon
					if(objectType == 3) {
						MapDataObjectPrimitive pointObj = mapObj;
						pointObj.objectType = 1;
						mapObj.order = polygonArea(mobj, mult);
						polygonsArray.push_back(mapObj);
						pointsArray.push_back(pointObj);
					} else if(objectType == 1) {
						pointsArray.push_back(mapObj);
					} else {
						linesArray.push_back(mapObj);
					}
					if (req->getIntPropertyValue(req->props()->R_SHADOW_LEVEL) > 0) {
						rc->shadowLevelMin = std::min(rc->shadowLevelMin, order);
						rc->shadowLevelMax = std::max(rc->shadowLevelMax, order);
						req->clearIntvalue(req->props()->R_SHADOW_LEVEL);
					}
				}

			}
		}
		sort(polygonsArray.begin(), polygonsArray.end(), sortByRevOrder);
		sort(pointsArray.begin(), pointsArray.end(), sortByOrder);
		sort(linesArray.begin(), linesArray.end(), sortByOrder);
	}
}

void doRendering(std::vector <MapDataObject* > mapDataObjects, SkCanvas* canvas,
		RenderingRuleSearchRequest* req,
		RenderingContext* rc) {
	rc->nativeOperations.start();
	SkPaint* paint = new SkPaint;
	paint->setAntiAlias(true);

	std::vector<MapDataObjectPrimitive>  polygonsArray;
	std::vector<MapDataObjectPrimitive>  pointsArray;
	std::vector<MapDataObjectPrimitive>  linesArray;
	sortObjectsByProperOrder(mapDataObjects, req, rc, polygonsArray, pointsArray, linesArray);
	rc->lastRenderedKey = 0;

	drawObject(rc, canvas, req, paint, polygonsArray, 0);
	rc->lastRenderedKey = 5;
	if (rc->getShadowRenderingMode() > 1) {
		drawObject(rc, canvas, req, paint, linesArray, 1);
	}
	rc->lastRenderedKey = 40;
	drawObject(rc, canvas, req, paint, linesArray, 2);
	rc->lastRenderedKey = 60;

	drawObject(rc, canvas, req, paint, pointsArray, 3);
	rc->lastRenderedKey = 125;

	drawIconsOverCanvas(rc, canvas);

	rc->textRendering.start();
	drawTextOverCanvas(rc, canvas);
	rc->textRendering.pause();

	delete paint;
	rc->nativeOperations.pause();
	osmand_log_print(LOG_INFO,  "Native ok (rendering %d, text %d ms) \n (%d points, %d points inside, %d of %d objects visible)\n",
				rc->nativeOperations.getElapsedTime(),	rc->textRendering.getElapsedTime(),
				rc->pointCount, rc->pointInsideCount, rc->visible, rc->allObjects);
}
