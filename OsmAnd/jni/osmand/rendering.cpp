
#include <jni.h>
#include <android/log.h>
#include <time.h>
#include <stdio.h>
#include <stdlib.h>

#define SK_BUILD_FOR_ANDROID_NDK
#include <SkBitmap.h>
#include <SkCanvas.h>
#include <SkPaint.h>
#include <SkPath.h>

JNIEnv* env;
jclass MultiPolygonClass;

jclass PathClass;
jmethodID Path_init;
jmethodID Path_moveTo;
jmethodID Path_lineTo;

jclass CanvasClass;
jmethodID Canvas_drawPath;
jfieldID Canvas_nativeCanvas;

jclass PaintClass;
jmethodID PaintClass_setStrokeWidth;
jmethodID PaintClass_setStrokeCap;
jmethodID PaintClass_setPathEffect;
jmethodID PaintClass_setStyle;
jmethodID PaintClass_setColor;

jclass PaintStyleClass;
jobject PaintStyle_STROKE;
jobject PaintStyle_FILL_AND_STROKE;

jclass DashPathEffect;
jmethodID DashPathEffect_init;

jclass CapClass;
jobject CapClass_BUTT;
jmethodID CapClass_valueOf;

jclass MapRenderingTypesClass;
jmethodID MapRenderingTypes_getMainObjectType;
jmethodID MapRenderingTypes_getObjectSubType;
jmethodID MapRenderingTypes_getNegativeWayLayer;

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
jfieldID RenderingContextClass_interrupted;

char debugMessage[1024];


typedef struct RenderingContext {
		jobject originalRC;
		
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


 void calcPoint(jobject mapObject, jint ind, RenderingContext* rc)
 {
		rc -> pointCount ++;
		
		float tx = env->CallIntMethod( mapObject, BinaryMapDataObject_getPoint31XTile, ind)
						/ (rc -> tileDivisor);
		float ty = env->CallIntMethod( mapObject, BinaryMapDataObject_getPoint31YTile, ind)
				/ (rc -> tileDivisor);

		float dTileX = tx - rc -> leftX;
		float dTileY = ty - rc -> topY;
		rc -> calcX = rc -> cosRotateTileSize * dTileX - rc -> sinRotateTileSize * dTileY;
		rc -> calcY = rc -> sinRotateTileSize * dTileX + rc -> cosRotateTileSize * dTileY;
		
//		sprintf(debugMessage, "Coordinates %f %f", rc->calcX, rc->calcY);
//		__android_log_print(ANDROID_LOG_WARN, "net.osmand", debugMessage);
		if(rc -> calcX >= 0 && rc -> calcX < rc -> width && 
				rc -> calcY >= 0 && rc -> calcY < rc ->height){
			rc -> pointInsideCount++;
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

jobject getDashEffect(jstring dashes){
	int length = env->GetStringLength( dashes);
	const char* chars = env->GetStringUTFChars( dashes, NULL);
	int i = 0;
	char fval[10];
	int flength = 0;
	jfloat primFloats[20];
	int floatLen = 0;
	for(;i<=length;i++)
	{
		if(i == length)
		{
			if(flength > 0)	{ fval[flength] = 0;
			primFloats[floatLen++] = atof(fval); flength = 0;}
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
	env->ReleaseStringUTFChars( dashes, chars);
	jfloatArray floatArray = env->NewFloatArray( floatLen);
	env->SetFloatArrayRegion(floatArray, 0, floatLen, primFloats);
	jobject dashEffect = env->NewObject( DashPathEffect, DashPathEffect_init, floatArray, 0);
	env->DeleteLocalRef( floatArray);
	return dashEffect;
}

int updatePaint(jobject renderingRuleSearch, SkPaint* paint, int ind, int area,
		RenderingContext* rc) {
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
	if (!area) {
		// TODO not complete
		float stroke = getFloatPropertyValue(renderingRuleSearch, rStrokeW);
		if (!(stroke > 0)) {
			return 0;
		}

		int color = getIntPropertyValue(renderingRuleSearch, rColor);
		paint->setStyle(SkPaint::kStroke_Style);
		paint->setColor(color);
		paint->setStrokeWidth(stroke);
		jstring cap = getStringPropertyValue(renderingRuleSearch, rCap);
		jstring pathEff = getStringPropertyValue(renderingRuleSearch, rPathEff);

		if (cap != NULL && env->GetStringLength( cap) > 0) {
			jobject capObj = env->CallStaticObjectMethod( CapClass, CapClass_valueOf, cap);
			// TODO
			paint->setStrokeCap(SkPaint::kButt_Cap);
			env->DeleteLocalRef( capObj);
		} else {
			paint->setStrokeCap(SkPaint::kButt_Cap);
		}

		if (pathEff != NULL && env->GetStringLength(pathEff) > 0) {
			// TODO
			//jobject pathObj = getDashEffect(pathEff);
			//env->CallVoidMethod( paint, PaintClass_setPathEffect, pathObj);
			// env->DeleteLocalRef( pathObj );
		} else {
			paint-> setPathEffect(NULL);
		}

		env->DeleteLocalRef( cap);
		env->DeleteLocalRef( pathEff);

		return 1;
	}

	return 0;
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
//	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Search done");
	env->DeleteLocalRef( tag);
	env->DeleteLocalRef( value);
	if (!rendered || !updatePaint(renderingRuleSearch, paint, 0, 0, rc)) {
		return;
	}

	rc->visible++;
//	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "About to draw");
	SkPath path ;
	int i = 0;
	float px = 0;
	float py = 0;
	for (; i < length; i++) {
		calcPoint(binaryMapDataObject, i, rc);
		if (i == 0) {
			path.moveTo(rc->calcX, rc->calcY);
		} else {
//			cv->drawLine(px, py, rc->calcX, rc->calcY, *paint);
			path.lineTo(rc->calcX, rc->calcY);
		}
		px = rc->calcX;
		py = rc->calcY;
	}
	if (i > 0) {
		if (drawOnlyShadow) {
			//int shadowColor = render.getIntPropertyValue(render.ALL.R_SHADOW_COLOR);
			//int shadowRadius = render.getIntPropertyValue(render.ALL.R_SHADOW_RADIUS);
			//drawPolylineShadow(canvas, rc, path, shadowColor, shadowRadius);
		} else {
			cv->drawPath(path, *paint);
			//if (updatePaint(render, paint, 1, false, rc)) {
			//	canvas.drawPath(path, paint);
			//	if (updatePaint(render, paint, 2, false, rc)) {
			//		canvas.drawPath(path, paint);
			//	}
			//}
		}
	}

}

void drawObject(RenderingContext* rc, jobject binaryMapDataObject, SkCanvas* cv,
		jobject renderingRuleSearch, SkPaint* paint, int l, int renderText, int drawOnlyShadow) {
		rc -> allObjects++;
		if (env->IsInstanceOf( binaryMapDataObject, MultiPolygonClass)) {
			//if(!drawOnlyShadow){
			//	drawMultiPolygon(obj, render, canvas, rc);
			//}
			 return; 
		}
		
		jintArray types = (jintArray) env->CallObjectMethod( binaryMapDataObject, BinaryMapDataObject_getTypes);
		jint mainType;
		env->GetIntArrayRegion( types, l, 1, &mainType);
		int t = mainType & 3;
		env->DeleteLocalRef( types);
		
		jobject pair = env->CallObjectMethod( binaryMapDataObject, BinaryMapDataObject_getTagValue, l);
		if( t == 1 && !drawOnlyShadow) {
			// point

			// drawPoint(obj, render, canvas, rc, pair, renderText);
		} else if(t == 2) {
			// polyline
			int layer = env->CallStaticIntMethod( MapRenderingTypesClass,
						MapRenderingTypes_getNegativeWayLayer, mainType);
//			__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Draw polyline");

			drawPolyline(binaryMapDataObject, renderingRuleSearch, cv, paint, rc, pair, layer, drawOnlyShadow);
		} else if(t == 3 && !drawOnlyShadow) {
			// polygon

			// drawPolygon(obj, render, canvas, rc, pair);
		}
		
		env->DeleteLocalRef( pair);

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
	
	rc->originalRC = orc; 

}


void mergeRenderingContext(jobject orc, RenderingContext* rc) 
{
	env->SetIntField( orc, getFid(RenderingContextClass, "pointCount", "I" ) , rc->pointCount);
	env->SetIntField( orc, getFid(RenderingContextClass, "pointInsideCount", "I" ) , rc->pointInsideCount);
	env->SetIntField( orc, getFid(RenderingContextClass, "visible", "I" ) , rc->visible);
	env->SetIntField( orc, getFid(RenderingContextClass, "allObjects", "I" ) , rc->allObjects);

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

   PathClass = globalRef(env->FindClass( "android/graphics/Path"));
   Path_init = env->GetMethodID( PathClass, "<init>", "()V" );
   Path_moveTo = env->GetMethodID( PathClass, "moveTo", "(FF)V" );
   Path_lineTo = env->GetMethodID( PathClass, "lineTo", "(FF)V" );

   CanvasClass = globalRef(env->FindClass( "android/graphics/Canvas"));
   Canvas_drawPath = env->GetMethodID( CanvasClass, "drawPath",
		   "(Landroid/graphics/Path;Landroid/graphics/Paint;)V" );
   Canvas_nativeCanvas = env->GetFieldID( CanvasClass, "mNativeCanvas","I" );

   PaintClass = globalRef(env->FindClass( "android/graphics/Paint"));
   PaintClass_setColor = env->GetMethodID( PaintClass, "setColor", "(I)V" );
   PaintClass_setStrokeWidth = env->GetMethodID( PaintClass, "setStrokeWidth", "(F)V" );
   PaintClass_setStrokeCap = env->GetMethodID( PaintClass, "setStrokeCap", "(Landroid/graphics/Paint$Cap;)V" );
   PaintClass_setPathEffect = env->GetMethodID( PaintClass, "setPathEffect",
		   "(Landroid/graphics/PathEffect;)Landroid/graphics/PathEffect;" );
   PaintClass_setStyle = env->GetMethodID( PaintClass, "setStyle", "(Landroid/graphics/Paint$Style;)V" );

   DashPathEffect = globalRef(env->FindClass( "android/graphics/DashPathEffect"));
   DashPathEffect_init =env->GetMethodID( DashPathEffect, "<init>", "([FF)V" );

   CapClass = globalRef(env->FindClass( "android/graphics/Paint$Cap"));
   CapClass_valueOf = env->GetStaticMethodID( CapClass, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Paint$Cap;" );
   CapClass_BUTT = globalRef(env->GetStaticObjectField( CapClass,
   		   env-> GetStaticFieldID( CapClass, "BUTT","Landroid/graphics/Paint$Cap;")));

   PaintStyleClass = globalRef(env->FindClass( "android/graphics/Paint$Style"));
   PaintStyle_FILL_AND_STROKE = globalObj(env->GetStaticObjectField( PaintStyleClass,
		   env-> GetStaticFieldID( PaintStyleClass, "FILL_AND_STROKE","Landroid/graphics/Paint$Style;")));
   PaintStyle_STROKE = globalObj(env->GetStaticObjectField( PaintStyleClass,
		   env-> GetStaticFieldID( PaintStyleClass, "STROKE","Landroid/graphics/Paint$Style;")));

   RenderingContextClass = globalRef(env->GetObjectClass( rc));
   RenderingContextClass_interrupted = getFid( RenderingContextClass, "interrupted", "Z" );

   MapRenderingTypesClass = globalRef(env->FindClass( "net/osmand/osm/MapRenderingTypes"));
   MapRenderingTypes_getMainObjectType = env->GetStaticMethodID( MapRenderingTypesClass,"getMainObjectType","(I)I");
   MapRenderingTypes_getObjectSubType = env->GetStaticMethodID( MapRenderingTypesClass, "getObjectSubType","(I)I");
   MapRenderingTypes_getNegativeWayLayer = env->GetStaticMethodID( MapRenderingTypesClass,"getNegativeWayLayer","(I)I");

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
   env->DeleteGlobalRef( MapRenderingTypesClass );
   env->DeleteGlobalRef( PathClass );
   env->DeleteGlobalRef( CanvasClass );
   env->DeleteGlobalRef( PaintClass );
   env->DeleteGlobalRef( RenderingContextClass );
   env->DeleteGlobalRef( DashPathEffect );
   env->DeleteGlobalRef( CapClass );
   env->DeleteGlobalRef( CapClass_BUTT );
   env->DeleteGlobalRef( PaintStyleClass );
   env->DeleteGlobalRef( PaintStyle_FILL_AND_STROKE );
   env->DeleteGlobalRef( PaintStyle_STROKE );
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

//	SkBitmap* bmp = GraphicsJNI::getNativeBitmap(env, bmpObj);
	jclass bmpClass = env->GetObjectClass(bmpObj);
	SkBitmap* bmp = (SkBitmap*)env->CallIntMethod(bmpObj, env->GetMethodID(bmpClass, "ni", "()I"));
//	SkBitmap* bmp = new SkBitmap;
	SkPaint* paint = new SkPaint;
	paint->setAntiAlias(true);
	SkCanvas* canvas = new SkCanvas(*bmp);

	sprintf(debugMessage, "Image %d %d  %d!", bmp->width(), bmp->height(), bmp->rowBytes());
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", debugMessage);
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Classes and methods are loaded");
	canvas->drawColor(defaultColor);
	const size_t size = env->GetArrayLength( binaryMapDataObjects);
    RenderingContext rc;
    
    copyRenderingContext(renderingContext, &rc);
    // szResult = malloc(sizeof(szFormat) + 20);
    
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
  
    // cleanup  
    // free(szResult);
      
  	mergeRenderingContext(renderingContext, &rc);

//  unloadLibrary();
  	
  	
	return result;
}

