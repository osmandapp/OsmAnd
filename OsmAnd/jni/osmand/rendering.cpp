
#include <jni.h>
#include <android/log.h>
#include <time.h>
#include <stdio.h>
#include <stdlib.h>

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

JNIEnv* env;

jclass MultiPolygonClass;
jmethodID MultiPolygon_getTag;
jmethodID MultiPolygon_getValue;
jmethodID MultiPolygon_getPoint31XTile;
jmethodID MultiPolygon_getPoint31YTile;
jmethodID MultiPolygon_getBoundsCount;
jmethodID MultiPolygon_getBoundPointsCount;


jclass PathClass;
jmethodID Path_init;
jmethodID Path_moveTo;
jmethodID Path_lineTo;

jclass CanvasClass;
jmethodID Canvas_drawPath;
jfieldID Canvas_nativeCanvas;

jclass RenderingIconsClass;
jmethodID RenderingIcons_getIcon;

jclass RenderingRuleStoragePropertiesClass;
jclass RenderingRulePropertyClass;

jclass RenderingRuleSearchRequestClass;
jfieldID RenderingRuleSearchRequest_ALL;
jmethodID RenderingRuleSearchRequest_setInitialTagValueZoom;
jmethodID RenderingRuleSearchRequest_getIntPropertyValue;
jmethodID RenderingRuleSearchRequest_getFloatPropertyValue;
jmethodID RenderingRuleSearchRequest_getIntIntPropertyValue;
jmethodID RenderingRuleSearchRequest_getStringPropertyValue;
jmethodID RenderingRuleSearchRequest_setIntFilter;
jmethodID RenderingRuleSearchRequest_setStringFilter;
jmethodID RenderingRuleSearchRequest_setBooleanFilter;

jmethodID RenderingRuleSearchRequest_search;
jmethodID RenderingRuleSearchRequest_searchI;

jclass BinaryMapDataObjectClass;
jmethodID BinaryMapDataObject_getPointsLength;
jmethodID BinaryMapDataObject_getPoint31YTile;
jmethodID BinaryMapDataObject_getPoint31XTile;
jmethodID BinaryMapDataObject_getTypes;
jmethodID BinaryMapDataObject_getTagValue;

jclass TagValuePairClass;
jfieldID TagValuePair_tag;
jfieldID TagValuePair_value;

jclass RenderingContextClass;

char debugMessage[1024];


typedef struct RenderingContext {
		jobject originalRC;
		jobject androidContext;
		
		// public boolean interrupted = false;
		// boolean highResMode = false;
		// float mapTextSize = 1;
		// List<TextDrawInfo> textToDraw = new ArrayList<TextDrawInfo>();
		// List<IconDrawInfo> iconsToDraw = new ArrayList<IconDrawInfo>();

		float leftX;
		float topY;
		int width;
		int height;

		int zoom;
		float rotate;
		float tileDivisor;

		// debug purpose
		int pointCount ;
		int pointInsideCount;
		int visible;
		int allObjects;

		// use to calculate points
		float calcX;
		float calcY;
		
		float cosRotateTileSize;
		float sinRotateTileSize;
		
		int shadowRenderingMode;
		
		// not expect any shadow
		int shadowLevelMin;
		int shadowLevelMax;
} RenderingContext;



jfieldID getFid(jclass cls,const char* fieldName, const char* sig )
{
	return env->GetFieldID( cls, fieldName, sig);
}


 void calcPoint(jobject mapObject, jint ind, RenderingContext* rc) {
	rc->pointCount++;

	float tx = env->CallIntMethod(mapObject, BinaryMapDataObject_getPoint31XTile, ind) / (rc->tileDivisor);
	float ty = env->CallIntMethod(mapObject, BinaryMapDataObject_getPoint31YTile, ind) / (rc->tileDivisor);

	float dTileX = tx - rc->leftX;
	float dTileY = ty - rc->topY;
	rc->calcX = rc->cosRotateTileSize * dTileX - rc->sinRotateTileSize * dTileY;
	rc->calcY = rc->sinRotateTileSize * dTileX + rc->cosRotateTileSize * dTileY;

	if (rc->calcX >= 0 && rc->calcX < rc->width && rc->calcY >= 0 && rc->calcY < rc->height) {
		rc->pointInsideCount++;
	}
}

 void calcMultipolygonPoint(jobject mapObject, jint ind, jint b, RenderingContext* rc) {
	rc->pointCount++;
	float tx = env->CallIntMethod(mapObject, MultiPolygon_getPoint31XTile, ind, b) / (rc->tileDivisor);
	float ty = env->CallIntMethod(mapObject, MultiPolygon_getPoint31YTile, ind, b) / (rc->tileDivisor);

	float dTileX = tx - rc->leftX;
	float dTileY = ty - rc->topY;
	rc->calcX = rc->cosRotateTileSize * dTileX - rc->sinRotateTileSize * dTileY;
	rc->calcY = rc->sinRotateTileSize * dTileX + rc->cosRotateTileSize * dTileY;

	if (rc->calcX >= 0 && rc->calcX < rc->width && rc->calcY >= 0 && rc->calcY < rc->height) {
		rc->pointInsideCount++;
	}
}

int getIntPropertyValue(jobject renderingRuleSearch, const char* prop)
{
	jobject all = env->GetObjectField( renderingRuleSearch, RenderingRuleSearchRequest_ALL);
	jfieldID fid = env->GetFieldID( RenderingRuleStoragePropertiesClass, prop,
			"Lnet/osmand/render/RenderingRuleProperty;");
	jobject propObj = env->GetObjectField( all, fid);
	int res = env->CallIntMethod( renderingRuleSearch, RenderingRuleSearchRequest_getIntPropertyValue, propObj);
	env->DeleteLocalRef( all);
	env->DeleteLocalRef( propObj);
	return res;
}

jstring getStringPropertyValue(jobject renderingRuleSearch, const char* prop)
{
	jobject all = env->GetObjectField( renderingRuleSearch, RenderingRuleSearchRequest_ALL);
	jfieldID fid = env->GetFieldID( RenderingRuleStoragePropertiesClass, prop,
			"Lnet/osmand/render/RenderingRuleProperty;");
	jobject propObj = env->GetObjectField( all, fid);
	jstring res = (jstring) env->CallObjectMethod( renderingRuleSearch, RenderingRuleSearchRequest_getStringPropertyValue, propObj);
	env->DeleteLocalRef( all);
	env->DeleteLocalRef( propObj);
	return res;
}

void setIntPropertyFilter(jobject renderingRuleSearch, const char* prop, int filter)
{
	jobject all = env->GetObjectField( renderingRuleSearch, RenderingRuleSearchRequest_ALL);
	jfieldID fid = env->GetFieldID( RenderingRuleStoragePropertiesClass, prop,
			"Lnet/osmand/render/RenderingRuleProperty;");
	jobject propObj = env->GetObjectField( all, fid);
	env->CallVoidMethod( renderingRuleSearch, RenderingRuleSearchRequest_setIntFilter, propObj, filter);
	env->DeleteLocalRef( all);
	env->DeleteLocalRef( propObj);
}


float getFloatPropertyValue(jobject renderingRuleSearch, const char* prop)
{
	jobject all = env->GetObjectField( renderingRuleSearch, RenderingRuleSearchRequest_ALL);
	jfieldID fid = env->GetFieldID( RenderingRuleStoragePropertiesClass, prop,
			"Lnet/osmand/render/RenderingRuleProperty;");
	jobject propObj = env->GetObjectField( all, fid);
	float res = env->CallFloatMethod( renderingRuleSearch, RenderingRuleSearchRequest_getFloatPropertyValue, propObj);
	env->DeleteLocalRef( all);
	env->DeleteLocalRef( propObj);
	return res;
}

SkPathEffect* getDashEffect(const char* chars){
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
	return new SkDashPathEffect(primFloats, floatLen, 0);
}

SkBitmap* getNativeBitmap(jobject bmpObj){
	if(bmpObj == NULL){
		return NULL;
	}
	jclass bmpClass = env->GetObjectClass(bmpObj);
	SkBitmap* bmp = (SkBitmap*)env->CallIntMethod(bmpObj, env->GetMethodID(bmpClass, "ni", "()I"));
	env->DeleteLocalRef(bmpClass);
	return bmp;
}

SkBitmap* getCachedBitmap(RenderingContext* rc, jstring js)
{
	jobject bmp = env->CallStaticObjectMethod(RenderingIconsClass, RenderingIcons_getIcon, rc->androidContext, js);
	SkBitmap* res = getNativeBitmap(bmp);
	env->DeleteLocalRef(bmp);
	return res;
}


// TODO cache shaders
// TODO path effects
int updatePaint(jobject renderingRuleSearch, SkPaint* paint, int ind, int area, RenderingContext* rc) {
	const char* rColor;
	const char* rStrokeW;
	const char* rCap;
	const char* rPathEff;
	if (ind == 0) {
		rColor = "R_COLOR";
		rStrokeW = "R_STROKE_WIDTH";
		rCap = "R_CAP";
		rPathEff = "R_PATH_EFFECT";
	} else if (ind == 1) {
		rColor = "R_COLOR_2";
		rStrokeW = "R_STROKE_WIDTH_2";
		rCap = "R_CAP_2";
		rPathEff = "R_PATH_EFFECT_2";
	} else {
		rColor = "R_COLOR_3";
		rStrokeW = "R_STROKE_WIDTH_3";
		rCap = "R_CAP_3";
		rPathEff = "R_PATH_EFFECT_3";
	}
	if (area) {
		paint->setStyle(SkPaint::kStrokeAndFill_Style);
		paint->setStrokeWidth(0);
	} else {
		float stroke = getFloatPropertyValue(renderingRuleSearch, rStrokeW);
		if (!(stroke > 0)) {
			return 0;
		}

		paint->setStyle(SkPaint::kStroke_Style);
		paint->setStrokeWidth(stroke);
		jstring capStr = getStringPropertyValue(renderingRuleSearch, rCap);
		jstring pathEffStr = getStringPropertyValue(renderingRuleSearch, rPathEff);

		if (capStr != NULL && env->GetStringLength(capStr) > 0) {
			const char* cap = env->GetStringUTFChars(capStr, NULL);
			if (strcmp("BUTT", cap) == 0) {
				paint->setStrokeCap(SkPaint::kButt_Cap);
			} else if (strcmp("ROUND", cap) == 0) {
				paint->setStrokeCap(SkPaint::kRound_Cap);
			} else if (strcmp("SQUARE", cap) == 0) {
				paint->setStrokeCap(SkPaint::kSquare_Cap);
			}
			env->ReleaseStringUTFChars(capStr, cap);
		} else {
			paint->setStrokeCap(SkPaint::kButt_Cap);
		}

		if (pathEffStr != NULL && env->GetStringLength(pathEffStr) > 0) {
			const char* pathEff = env->GetStringUTFChars(pathEffStr, NULL);
			SkPathEffect* p = getDashEffect(pathEff);
			paint->setPathEffect(p);
			p->unref();
			env->ReleaseStringUTFChars(pathEffStr, pathEff);
		} else {
			paint->setPathEffect(NULL);
		}

		env->DeleteLocalRef(capStr);
		env->DeleteLocalRef(pathEffStr);
	}

	int color = getIntPropertyValue(renderingRuleSearch, rColor);
	paint->setColor(color);

	if (ind == 0) {
		jstring shader = getStringPropertyValue(renderingRuleSearch, "R_SHADER");
		if(shader != NULL){
			SkBitmap*  bmp = getCachedBitmap(rc, shader);
			if(bmp == NULL) {
				paint->setShader(NULL);
			} else {
				paint->setShader(new SkBitmapProcShader(*bmp, SkShader::kRepeat_TileMode,SkShader::kRepeat_TileMode))->
					unref();
			}
		} else {
			paint->setShader(NULL);
		}
		env->DeleteLocalRef(shader);
	} else {
		paint->setShader(NULL);
	}

	// do not check shadow color here
	if (rc->shadowRenderingMode != 1 || ind != 0) {
		paint->setLooper(NULL);
	} else {
		int shadowColor = getIntPropertyValue(renderingRuleSearch, "R_SHADOW_COLOR");
		int shadowLayer = getIntPropertyValue(renderingRuleSearch, "R_SHADOW_RADIUS");
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

 void drawPolyline(jobject binaryMapDataObject,	jobject renderingRuleSearch, SkCanvas* cv, SkPaint* paint,
 		RenderingContext* rc, jobject pair, int layer, int drawOnlyShadow)
 {
	if (renderingRuleSearch == NULL || pair == NULL) {
		return;
	}
	jint length = env->CallIntMethod( binaryMapDataObject,
			BinaryMapDataObject_getPointsLength);
	if (length < 2) {
		return;
	}
	jstring tag = (jstring ) env->GetObjectField( pair, TagValuePair_tag);
	jstring value = (jstring ) env->GetObjectField( pair, TagValuePair_value);

//	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "About to search");
	env->CallVoidMethod( renderingRuleSearch,
			RenderingRuleSearchRequest_setInitialTagValueZoom, tag, value,
			rc->zoom);
	setIntPropertyFilter(renderingRuleSearch, "R_LAYER", layer);
	// TODO oneway
	// int oneway = 0;
	//if(rc -> zoom >= 16 && "highway".equals(pair.tag) && MapRenderingTypes.isOneWayWay(obj.getHighwayAttributes())){
	//strcmp("highway") oneway = 1;
	//}

	int rendered = env->CallBooleanMethod( renderingRuleSearch,
			RenderingRuleSearchRequest_search, 2);
	env->DeleteLocalRef( tag);
	env->DeleteLocalRef( value);
	if (!rendered || !updatePaint(renderingRuleSearch, paint, 0, 0, rc)) {
		return;
	}

	rc->visible++;
	SkPath path ;
	int i = 0;
	// TODO calculate text
	for (; i < length; i++) {
		calcPoint(binaryMapDataObject, i, rc);
		if (i == 0) {
			path.moveTo(rc->calcX, rc->calcY);
		} else {
			path.lineTo(rc->calcX, rc->calcY);
		}
	}
	if (i > 0) {
		if (drawOnlyShadow) {
			// TODO
			//int shadowColor = render.getIntPropertyValue(render.ALL.R_SHADOW_COLOR);
			//int shadowRadius = render.getIntPropertyValue(render.ALL.R_SHADOW_RADIUS);
			//drawPolylineShadow(canvas, rc, path, shadowColor, shadowRadius);
		} else {
			cv->drawPath(path, *paint);
			if (updatePaint(renderingRuleSearch, paint, 1, 0, rc)) {
				cv->drawPath(path, *paint);
				if (updatePaint(renderingRuleSearch, paint, 2, 0, rc)) {
					cv->drawPath(path, *paint);
				}
			}
			// TODO
//			if (oneway && !drawOnlyShadow) {
//				Paint[] paints = getOneWayPaints();
//				for (int i = 0; i < paints.length; i++) {
//					canvas.drawPath(path, paints[i]);
//				}
//			}
//			if (!drawOnlyShadow && obj.getName() != null && obj.getName().length() > 0) {
//				calculatePolylineText(obj, render, rc, pair, path, pathRotate, roadLength, inverse, xMid, yMid,
//						middlePoint);
//			}
		}
	}
}

void drawMultiPolygon(jobject binaryMapDataObject,	jobject renderingRuleSearch, SkCanvas* cv, SkPaint* paint,
 		  		RenderingContext* rc) {
	if (renderingRuleSearch == NULL) {
		return;
	}
	jstring tag = (jstring) env->CallObjectMethod(binaryMapDataObject, MultiPolygon_getTag);
	jstring value = (jstring) env->CallObjectMethod(binaryMapDataObject, MultiPolygon_getValue);

	env->CallVoidMethod(renderingRuleSearch, RenderingRuleSearchRequest_setInitialTagValueZoom, tag, value, rc->zoom);

	int rendered = env->CallBooleanMethod(renderingRuleSearch, RenderingRuleSearchRequest_search, 3);
	env->DeleteLocalRef(tag);
	env->DeleteLocalRef(value);

	if (!rendered || !updatePaint(renderingRuleSearch, paint, 0, 1, rc)) {
		return;
	}

	int boundsCount = env->CallIntMethod(binaryMapDataObject, MultiPolygon_getBoundsCount);
	rc->visible++;
	SkPath path;

	for (int i = 0; i < boundsCount; i++) {
		int cnt = env->CallIntMethod(binaryMapDataObject, MultiPolygon_getBoundPointsCount, i);
		float xText = 0;
		float yText = 0;
		for (int j = 0; j < cnt; j++) {
			calcMultipolygonPoint(binaryMapDataObject, j, i, rc);
			xText += rc->calcX;
			yText += rc->calcY;
			if (j == 0) {
				path.moveTo(rc->calcX, rc->calcY);
			} else {
				path.lineTo(rc->calcX, rc->calcY);
			}
		}
		if (cnt > 0) {
			// TODO name
			// String name = ((MultyPolygon) obj).getName(i);
			// if (name != null) {
			// drawPointText(render, rc, new TagValuePair(tag, value), xText / cnt, yText / cnt, name);
			// }
		}
	}

	cv->drawPath(path, *paint);
	// for test purpose
	// render.strokeWidth = 1.5f;
	// render.color = Color.BLACK;
	if (updatePaint(renderingRuleSearch, paint, 1, 0, rc)) {
		cv->drawPath(path, *paint);
	}
}

void drawPolygon(jobject binaryMapDataObject,	jobject renderingRuleSearch, SkCanvas* cv, SkPaint* paint,
		  		RenderingContext* rc, jobject pair) {
	if (renderingRuleSearch == NULL || pair == NULL) {
		return;
	}
	jint length = env->CallIntMethod(binaryMapDataObject, BinaryMapDataObject_getPointsLength);
	if (length <= 2) {
		return;
	}
	jstring tag = (jstring) env->GetObjectField(pair, TagValuePair_tag);
	jstring value = (jstring) env->GetObjectField(pair, TagValuePair_value);

	env->CallVoidMethod(renderingRuleSearch, RenderingRuleSearchRequest_setInitialTagValueZoom, tag, value, rc->zoom);

	int rendered = env->CallBooleanMethod(renderingRuleSearch, RenderingRuleSearchRequest_search, 3);
	env->DeleteLocalRef(tag);
	env->DeleteLocalRef(value);

	float xText = 0;
	float yText = 0;
	if (!rendered || !updatePaint(renderingRuleSearch, paint, 0, 1, rc)) {
		return;
	}

	rc->visible++;
	SkPath path;
	int i = 0;
	float px = 0;
	float py = 0;
	for (; i < length; i++) {
		calcPoint(binaryMapDataObject, i, rc);
		if (i == 0) {
			path.moveTo(rc->calcX, rc->calcY);
		} else {
			path.lineTo(rc->calcX, rc->calcY);
		}
		xText += px;
		yText += py;
	}

	cv->drawPath(path, *paint);
	if (updatePaint(renderingRuleSearch, paint, 1, 0, rc)) {
		cv->drawPath(path, *paint);
	}
	// TODO polygon text
//			String name = obj.getName();
//			if(name != null){
//				drawPointText(render, rc, pair, xText / len, yText / len, name);
//			}
//		}
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

void drawObject(RenderingContext* rc, jobject binaryMapDataObject, SkCanvas* cv,
		jobject renderingRuleSearch,
		SkPaint* paint, int l, int renderText, int drawOnlyShadow) {
	rc->allObjects++;
	if (env->IsInstanceOf(binaryMapDataObject, MultiPolygonClass)) {
		if (!drawOnlyShadow) {
			drawMultiPolygon(binaryMapDataObject, renderingRuleSearch, cv, paint, rc);
		}
		return;
	}

	jintArray types = (jintArray) env->CallObjectMethod(binaryMapDataObject, BinaryMapDataObject_getTypes);
	jint mainType;
	env->GetIntArrayRegion(types, l, 1, &mainType);
	int t = mainType & 3;
	env->DeleteLocalRef(types);

	jobject pair = env->CallObjectMethod(binaryMapDataObject, BinaryMapDataObject_getTagValue, l);
	if (t == 1 && !drawOnlyShadow) {
		// point
		// TODO
		// drawPoint(obj, render, canvas, rc, pair, renderText);
	} else if (t == 2) {
		// polyline
		int layer = getNegativeWayLayer(mainType);
//			__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Draw polyline");
		drawPolyline(binaryMapDataObject, renderingRuleSearch, cv, paint, rc, pair, layer, drawOnlyShadow);
	} else if (t == 3 && !drawOnlyShadow) {
		// polygon
		drawPolygon(binaryMapDataObject, renderingRuleSearch, cv, paint, rc, pair);
	}
	env->DeleteLocalRef(pair);
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
	env->DeleteLocalRef(rc->androidContext);

}

jclass globalRef(jobject o)
{
	return  (jclass) env->NewGlobalRef( o);
}

jobject globalObj(jobject o)
{
	return  env->NewGlobalRef( o);
}

void initLibrary(jobject rc)
{
   MultiPolygonClass = globalRef(env->FindClass( "net/osmand/osm/MultyPolygon"));
   MultiPolygon_getTag = env->GetMethodID( MultiPolygonClass, "getTag", "()Ljava/lang/String;" );
   MultiPolygon_getValue = env->GetMethodID( MultiPolygonClass, "getValue", "()Ljava/lang/String;" );
   MultiPolygon_getPoint31XTile =env->GetMethodID( MultiPolygonClass, "getPoint31XTile", "(II)I" );
   MultiPolygon_getPoint31YTile =env->GetMethodID( MultiPolygonClass, "getPoint31YTile", "(II)I" );
   MultiPolygon_getBoundsCount =env->GetMethodID( MultiPolygonClass, "getBoundsCount", "()I" );
   MultiPolygon_getBoundPointsCount =env->GetMethodID( MultiPolygonClass, "getBoundPointsCount", "(I)I" );



   PathClass = globalRef(env->FindClass( "android/graphics/Path"));
   Path_init = env->GetMethodID( PathClass, "<init>", "()V" );
   Path_moveTo = env->GetMethodID( PathClass, "moveTo", "(FF)V" );
   Path_lineTo = env->GetMethodID( PathClass, "lineTo", "(FF)V" );

   CanvasClass = globalRef(env->FindClass( "android/graphics/Canvas"));
   Canvas_drawPath = env->GetMethodID( CanvasClass, "drawPath",
		   "(Landroid/graphics/Path;Landroid/graphics/Paint;)V" );
   Canvas_nativeCanvas = env->GetFieldID( CanvasClass, "mNativeCanvas","I" );


   RenderingContextClass = globalRef(env->GetObjectClass( rc));

   RenderingIconsClass = globalRef(env->FindClass( "net/osmand/plus/render/RenderingIcons"));
   RenderingIcons_getIcon = env->GetStaticMethodID(RenderingIconsClass, "getIcon","(Landroid/content/Context;Ljava/lang/String;)Landroid/graphics/Bitmap;");

   BinaryMapDataObjectClass = globalRef(env->FindClass( "net/osmand/binary/BinaryMapDataObject"));
   BinaryMapDataObject_getPointsLength = env->GetMethodID( BinaryMapDataObjectClass,"getPointsLength","()I");
   BinaryMapDataObject_getPoint31YTile = env->GetMethodID( BinaryMapDataObjectClass,"getPoint31YTile","(I)I");
   BinaryMapDataObject_getPoint31XTile = env->GetMethodID( BinaryMapDataObjectClass,"getPoint31XTile","(I)I");
   BinaryMapDataObject_getTypes = env->GetMethodID( BinaryMapDataObjectClass,"getTypes","()[I");
   BinaryMapDataObject_getTagValue = env->GetMethodID( BinaryMapDataObjectClass,"getTagValue",
		   "(I)Lnet/osmand/binary/BinaryMapIndexReader$TagValuePair;");

   TagValuePairClass = globalRef(env->FindClass( "net/osmand/binary/BinaryMapIndexReader$TagValuePair"));
   TagValuePair_tag = env->GetFieldID( TagValuePairClass, "tag", "Ljava/lang/String;");
   TagValuePair_value= env->GetFieldID( TagValuePairClass, "value", "Ljava/lang/String;");

   RenderingRuleStoragePropertiesClass =
		   globalRef(env->FindClass( "net/osmand/render/RenderingRuleStorageProperties"));
   RenderingRulePropertyClass = globalRef(env->FindClass( "net/osmand/render/RenderingRuleProperty"));


   RenderingRuleSearchRequestClass = globalRef(env->FindClass( "net/osmand/render/RenderingRuleSearchRequest"));
   RenderingRuleSearchRequest_setInitialTagValueZoom =
		   env->GetMethodID( RenderingRuleSearchRequestClass,"setInitialTagValueZoom",
				   "(Ljava/lang/String;Ljava/lang/String;I)V");
   RenderingRuleSearchRequest_ALL = env->GetFieldID( RenderingRuleSearchRequestClass, "ALL",
		   "Lnet/osmand/render/RenderingRuleStorageProperties;");
   RenderingRuleSearchRequest_getIntPropertyValue = env->GetMethodID( RenderingRuleSearchRequestClass,
   		   "getIntPropertyValue",  "(Lnet/osmand/render/RenderingRuleProperty;)I");
   RenderingRuleSearchRequest_getIntIntPropertyValue = env->GetMethodID( RenderingRuleSearchRequestClass,
      		   "getIntPropertyValue",  "(Lnet/osmand/render/RenderingRuleProperty;I)I");
   RenderingRuleSearchRequest_getFloatPropertyValue = env->GetMethodID( RenderingRuleSearchRequestClass,
   		   "getFloatPropertyValue",  "(Lnet/osmand/render/RenderingRuleProperty;)F");
   RenderingRuleSearchRequest_getStringPropertyValue = env->GetMethodID( RenderingRuleSearchRequestClass,
    	   "getStringPropertyValue",  "(Lnet/osmand/render/RenderingRuleProperty;)Ljava/lang/String;");
   RenderingRuleSearchRequest_setIntFilter = env->GetMethodID( RenderingRuleSearchRequestClass,
       	   "setIntFilter",  "(Lnet/osmand/render/RenderingRuleProperty;I)V");
   RenderingRuleSearchRequest_setStringFilter = env->GetMethodID( RenderingRuleSearchRequestClass,
           "setStringFilter", "(Lnet/osmand/render/RenderingRuleProperty;Ljava/lang/String;)V");
   RenderingRuleSearchRequest_setBooleanFilter = env->GetMethodID( RenderingRuleSearchRequestClass,
           "setBooleanFilter", "(Lnet/osmand/render/RenderingRuleProperty;Z)V");
   RenderingRuleSearchRequest_search = env->GetMethodID( RenderingRuleSearchRequestClass, "search",  "(I)Z");
   RenderingRuleSearchRequest_searchI = env->GetMethodID( RenderingRuleSearchRequestClass, "search",  "(IZ)Z");

}

void unloadLibrary()
{
   env->DeleteGlobalRef( MultiPolygonClass );
   env->DeleteGlobalRef( PathClass );
   env->DeleteGlobalRef( CanvasClass );
   env->DeleteGlobalRef( RenderingContextClass );
   env->DeleteGlobalRef( RenderingIconsClass );
   env->DeleteGlobalRef( TagValuePairClass);
   env->DeleteGlobalRef( RenderingRuleSearchRequestClass);
   env->DeleteGlobalRef( RenderingRulePropertyClass);
   env->DeleteGlobalRef( RenderingRuleStoragePropertiesClass);
   env->DeleteGlobalRef( BinaryMapDataObjectClass );

}


extern "C" JNIEXPORT jstring JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering( JNIEnv* ienv,
		jobject obj, jobject renderingContext, jobjectArray binaryMapDataObjects, jobject bmpObj,
		jboolean useEnglishNames, jobject renderingRuleSearchRequest, jint defaultColor) {
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Initializing rendering");
	size_t i = 0;
	if(!env) {
	   env = ienv;
	   initLibrary(renderingContext);
	}


	SkPaint* paint = new SkPaint;
	paint->setAntiAlias(true);

	SkBitmap* bmp = getNativeBitmap(bmpObj);
	SkCanvas* canvas = new SkCanvas(*bmp);

	sprintf(debugMessage, "Image w:%d h:%d rb: %d!", bmp->width(), bmp->height(), bmp->rowBytes());
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", debugMessage);
	sprintf(debugMessage, "Image h:%d sz:%d bperpix:%d  shiftperpix:%d!",
			bmp->height(), bmp->getSize(), bmp->bytesPerPixel(), bmp->shiftPerPixel());
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", debugMessage);
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Classes and methods are loaded");
	canvas->drawColor(defaultColor);
	const size_t size = env->GetArrayLength( binaryMapDataObjects);
    RenderingContext rc;
    
    copyRenderingContext(renderingContext, &rc);
    __android_log_print(ANDROID_LOG_WARN, "net.osmand", "Rendering image");
    
    for(; i < size; i++) 
    {
   	    jobject binaryMapDataObject = (jobject) env->GetObjectArrayElement( binaryMapDataObjects, i);
   	    jintArray types = (jintArray) env->CallObjectMethod( binaryMapDataObject, BinaryMapDataObject_getTypes);
   	    // check multipolygon?
   	    if (types != NULL) {
			jint sizeTypes = env->GetArrayLength( types);
			env->DeleteLocalRef( types);
			int j = 0;
			for (; j < sizeTypes; j++) {
				drawObject(&rc, binaryMapDataObject, canvas, renderingRuleSearchRequest, paint, j, 1, 0);
			}
		}
   	    
   	    env->DeleteLocalRef( binaryMapDataObject);
    }

    delete paint;
    delete canvas;

    __android_log_print(ANDROID_LOG_WARN, "net.osmand", "End Rendering image");

    sprintf(debugMessage, "Hello android %d", size);
  
    // get an object string  
    jstring result = env->NewStringUTF( debugMessage);
  
  	mergeRenderingContext(renderingContext, &rc);

//  unloadLibrary();
  	
	return result;
}

