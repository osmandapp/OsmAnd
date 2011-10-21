
#include <jni.h>
#include <android/log.h>
#include <time.h>

#include <stdio.h>
#include <stdlib.h>



JNIEnv* env;
jclass MultiPolygonClass;

jclass PathClass;
jmethodID Path_init;
jmethodID Path_moveTo;
jmethodID Path_lineTo;

jclass CanvasClass;
jmethodID Canvas_drawPath;

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



jfieldID getFid(jclass cls, char* fieldName, char* sig )
{
	return (*env)->GetFieldID(env, cls, fieldName, sig);
}


 void calcPoint(jobject mapObject, jint ind, RenderingContext* rc)
 {
		rc -> pointCount ++;
		
		float tx = (*env)->CallIntMethod(env, mapObject, BinaryMapDataObject_getPoint31XTile, ind)
						/ (rc -> tileDivisor);
		float ty = (*env)->CallIntMethod(env, mapObject, BinaryMapDataObject_getPoint31YTile, ind)
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

int getIntPropertyValue(jobject renderingRuleSearch, char* prop)
{
	jobject all = (*env)->GetObjectField(env, renderingRuleSearch, RenderingRuleSearchRequest_ALL);
	jfieldID fid = (*env)->GetFieldID(env, RenderingRuleStoragePropertiesClass, prop,
			"Lnet/osmand/render/RenderingRuleProperty;");
	jobject propObj = (*env)->GetObjectField(env, all, fid);
	int res = (*env)->CallIntMethod(env, renderingRuleSearch, RenderingRuleSearchRequest_getIntPropertyValue, propObj);
	(*env)->DeleteLocalRef(env, all);
	(*env)->DeleteLocalRef(env, propObj);
	return res;
}

jstring getStringPropertyValue(jobject renderingRuleSearch, char* prop)
{
	jobject all = (*env)->GetObjectField(env, renderingRuleSearch, RenderingRuleSearchRequest_ALL);
	jfieldID fid = (*env)->GetFieldID(env, RenderingRuleStoragePropertiesClass, prop,
			"Lnet/osmand/render/RenderingRuleProperty;");
	jobject propObj = (*env)->GetObjectField(env, all, fid);
	jstring res = (*env)->CallObjectMethod(env, renderingRuleSearch, RenderingRuleSearchRequest_getStringPropertyValue, propObj);
	(*env)->DeleteLocalRef(env, all);
	(*env)->DeleteLocalRef(env, propObj);
	return res;
}

void setIntPropertyFilter(jobject renderingRuleSearch, char* prop, int filter)
{
	jobject all = (*env)->GetObjectField(env, renderingRuleSearch, RenderingRuleSearchRequest_ALL);
	jfieldID fid = (*env)->GetFieldID(env, RenderingRuleStoragePropertiesClass, prop,
			"Lnet/osmand/render/RenderingRuleProperty;");
	jobject propObj = (*env)->GetObjectField(env, all, fid);
	(*env)->CallVoidMethod(env, renderingRuleSearch, RenderingRuleSearchRequest_setIntFilter, propObj, filter);
	(*env)->DeleteLocalRef(env, all);
	(*env)->DeleteLocalRef(env, propObj);
}


float getFloatPropertyValue(jobject renderingRuleSearch, char* prop)
{
	jobject all = (*env)->GetObjectField(env, renderingRuleSearch, RenderingRuleSearchRequest_ALL);
	jfieldID fid = (*env)->GetFieldID(env, RenderingRuleStoragePropertiesClass, prop,
			"Lnet/osmand/render/RenderingRuleProperty;");
	jobject propObj = (*env)->GetObjectField(env, all, fid);
	float res = (*env)->CallFloatMethod(env, renderingRuleSearch, RenderingRuleSearchRequest_getFloatPropertyValue, propObj);
	(*env)->DeleteLocalRef(env, all);
	(*env)->DeleteLocalRef(env, propObj);
	return res;
}

jobject getDashEffect(jstring dashes){
	int length = (*env)->GetStringLength(env, dashes);
	const char* chars = (*env)->GetStringUTFChars(env, dashes, NULL);
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
	(*env)->ReleaseStringUTFChars(env, dashes, chars);
	jobjectArray floatArray = (*env)->NewFloatArray(env, floatLen);
	(*env)->SetFloatArrayRegion(env, floatArray, 0, floatLen, primFloats);
	jobject dashEffect = (*env)->NewObject(env, DashPathEffect, DashPathEffect_init, floatArray, 0);
	(*env)->DeleteLocalRef(env, floatArray);
	return dashEffect;
}

int updatePaint(jobject renderingRuleSearch, jobject paint, int ind, int area,
		RenderingContext* rc) {
	char* rColor;
	char* rStrokeW;
	char* rCap;
	char* rPathEff;
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
		(*env)->CallVoidMethod(env, paint, PaintClass_setStyle,PaintStyle_STROKE);
		(*env)->CallVoidMethod(env, paint, PaintClass_setColor, color);
		(*env)->CallVoidMethod(env, paint, PaintClass_setStrokeWidth, stroke);
		jstring cap = getStringPropertyValue(renderingRuleSearch, rCap);
		jstring pathEff = getStringPropertyValue(renderingRuleSearch, rPathEff);

		if (cap != NULL && (*env)->GetStringLength(env, cap) > 0) {
			jobject capObj = (*env)->CallStaticObjectMethod(env, CapClass, CapClass_valueOf, cap);
			(*env)->CallVoidMethod(env, paint, PaintClass_setStrokeCap, capObj);
			(*env)->DeleteLocalRef(env, capObj);
		} else {
			(*env)->CallVoidMethod(env, paint, PaintClass_setStrokeCap, CapClass_BUTT);
		}

		if (pathEff != NULL && (*env)->GetStringLength(env, pathEff) > 0) {
			//jobject pathObj = getDashEffect(pathEff);
			//(*env)->CallVoidMethod(env, paint, PaintClass_setPathEffect, pathObj);
			// (*env)->DeleteLocalRef(env, pathObj );
		} else {
			(*env)->CallObjectMethod(env, paint, PaintClass_setPathEffect, NULL);
		}

		(*env)->DeleteLocalRef(env, cap);
		(*env)->DeleteLocalRef(env, pathEff);

		return 1;
	}

	return 0;
}

 void drawPolyline(jobject binaryMapDataObject,	jobject renderingRuleSearch, jobject cv, jobject paint,
 		RenderingContext* rc, jobject pair, int layer, int drawOnlyShadow)
 {

	if (renderingRuleSearch == NULL || pair == NULL) {
		return;
	}
	jint length = (*env)->CallIntMethod(env, binaryMapDataObject,
			BinaryMapDataObject_getPointsLength);
	if (length < 2) {
		return;
	}
	jstring tag = (*env)->GetObjectField(env, pair, TagValuePair_tag);
	jstring value = (*env)->GetObjectField(env, pair, TagValuePair_value);

//	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "About to search");
	(*env)->CallVoidMethod(env, renderingRuleSearch,
			RenderingRuleSearchRequest_setInitialTagValueZoom, tag, value,
			rc->zoom);
	setIntPropertyFilter(renderingRuleSearch, "R_LAYER", layer);
	// TODO oneway
	// int oneway = 0;
	//if(rc -> zoom >= 16 && "highway".equals(pair.tag) && MapRenderingTypes.isOneWayWay(obj.getHighwayAttributes())){
	//strcmp("highway") oneway = 1;
	//}

	int rendered = (*env)->CallBooleanMethod(env, renderingRuleSearch,
			RenderingRuleSearchRequest_search, 2);
//	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Search done");
	(*env)->DeleteLocalRef(env, tag);
	(*env)->DeleteLocalRef(env, value);
	if (!rendered || !updatePaint(renderingRuleSearch, paint, 0, 0, rc)) {
		return;
	}

	rc->visible++;
//	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "About to draw");

	jobject path = NULL;
	int i = 0;
	for (; i < length; i++) {
		calcPoint(binaryMapDataObject, i, rc);
		if (path == NULL) {
			path = (*env)->NewObject(env, PathClass, Path_init);
			(*env)->CallVoidMethod(env, path, Path_moveTo, rc->calcX,
					rc->calcY);
		} else {
			(*env)->CallVoidMethod(env, path, Path_lineTo, rc->calcX,
					rc->calcY);
		}
	}

	if (path) {
		if (drawOnlyShadow) {
			//int shadowColor = render.getIntPropertyValue(render.ALL.R_SHADOW_COLOR);
			//int shadowRadius = render.getIntPropertyValue(render.ALL.R_SHADOW_RADIUS);
			//drawPolylineShadow(canvas, rc, path, shadowColor, shadowRadius);
		} else {
			(*env)->CallVoidMethod(env, cv, Canvas_drawPath, path, paint);
			//if (updatePaint(render, paint, 1, false, rc)) {
			//	canvas.drawPath(path, paint);
			//	if (updatePaint(render, paint, 2, false, rc)) {
			//		canvas.drawPath(path, paint);
			//	}
			//}
		}
		(*env)->DeleteLocalRef(env, path);
	}

}

void drawObject(RenderingContext* rc, jobject binaryMapDataObject, jobject cv,
		jobject renderingRuleSearch, jobject paint, int l, int renderText, int drawOnlyShadow) {
		rc -> allObjects++;
		if ((*env)->IsInstanceOf(env, binaryMapDataObject, MultiPolygonClass)) {
			//if(!drawOnlyShadow){
			//	drawMultiPolygon(obj, render, canvas, rc);
			//}
			 return; 
		}
		
		jintArray types = (*env)->CallObjectMethod(env, binaryMapDataObject, BinaryMapDataObject_getTypes);
		jint mainType;
		(*env)->GetIntArrayRegion(env, types, l, 1, &mainType);	
		int t = mainType & 3;
		(*env)->DeleteLocalRef(env, types);
		
		jobject pair = (*env)->CallObjectMethod(env, binaryMapDataObject, BinaryMapDataObject_getTagValue, l);
		if( t == 1 && !drawOnlyShadow) {
			// point

			// drawPoint(obj, render, canvas, rc, pair, renderText);
		} else if(t == 2) {
			// polyline
			int layer = (*env)->CallStaticIntMethod(env, MapRenderingTypesClass,
						MapRenderingTypes_getNegativeWayLayer, mainType);
//			__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Draw polyline");
			drawPolyline(binaryMapDataObject, renderingRuleSearch, cv, paint, rc, pair, layer, drawOnlyShadow);
		} else if(t == 3 && !drawOnlyShadow) {
			// polygon

			// drawPolygon(obj, render, canvas, rc, pair);
		}
		
		(*env)->DeleteLocalRef(env, pair);

}



void copyRenderingContext(jobject orc, RenderingContext* rc) 
{
	rc->leftX = (*env)->GetFloatField(env, orc, getFid( RenderingContextClass, "leftX", "F" ) );
	rc->topY = (*env)->GetFloatField(env, orc, getFid( RenderingContextClass, "topY", "F" ) );
	rc->width = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "width", "I" ) );
	rc->height = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "height", "I" ) );
	
	
	rc->zoom = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "zoom", "I" ) );
	rc->rotate = (*env)->GetFloatField(env, orc, getFid( RenderingContextClass, "rotate", "F" ) );
	rc->tileDivisor = (*env)->GetFloatField(env, orc, getFid( RenderingContextClass, "tileDivisor", "F" ) );
	
	rc->pointCount = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "pointCount", "I" ) );
	rc->pointInsideCount = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "pointInsideCount", "I" ) );
	rc->visible = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "visible", "I" ) );
	rc->allObjects = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "allObjects", "I" ) );
	
	rc->cosRotateTileSize = (*env)->GetFloatField(env, orc, getFid( RenderingContextClass, "cosRotateTileSize", "F" ) );
	rc->sinRotateTileSize = (*env)->GetFloatField(env, orc, getFid( RenderingContextClass, "sinRotateTileSize", "F" ) );

	rc->shadowRenderingMode = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "shadowRenderingMode", "I" ) );
	rc->shadowLevelMin = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "shadowLevelMin", "I" ) );
	rc->shadowLevelMax = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "shadowLevelMax", "I" ) );
	
	rc->originalRC = orc; 

}


void mergeRenderingContext(jobject orc, RenderingContext* rc) 
{
	(*env)->SetIntField(env, orc, getFid(RenderingContextClass, "pointCount", "I" ) , rc->pointCount);
	(*env)->SetIntField(env, orc, getFid(RenderingContextClass, "pointInsideCount", "I" ) , rc->pointInsideCount);
	(*env)->SetIntField(env, orc, getFid(RenderingContextClass, "visible", "I" ) , rc->visible);
	(*env)->SetIntField(env, orc, getFid(RenderingContextClass, "allObjects", "I" ) , rc->allObjects);

}


void initLibrary(jobject rc)
{
   MultiPolygonClass = (*env)->FindClass(env, "net/osmand/osm/MultyPolygon");

   PathClass = (*env)->FindClass(env, "android/graphics/Path");
   Path_init = (*env)->GetMethodID(env, PathClass, "<init>", "()V" );
   Path_moveTo = (*env)->GetMethodID(env, PathClass, "moveTo", "(FF)V" );
   Path_lineTo = (*env)->GetMethodID(env, PathClass, "lineTo", "(FF)V" );

   CanvasClass = (*env)->FindClass(env, "android/graphics/Canvas");
   Canvas_drawPath = (*env)->GetMethodID(env, CanvasClass, "drawPath",
		   "(Landroid/graphics/Path;Landroid/graphics/Paint;)V" );

   PaintClass = (*env)->FindClass(env, "android/graphics/Paint");
   PaintClass_setColor = (*env)->GetMethodID(env, PaintClass, "setColor", "(I)V" );
   PaintClass_setStrokeWidth = (*env)->GetMethodID(env, PaintClass, "setStrokeWidth", "(F)V" );
   PaintClass_setStrokeCap = (*env)->GetMethodID(env, PaintClass, "setStrokeCap", "(Landroid/graphics/Paint$Cap;)V" );
   PaintClass_setPathEffect = (*env)->GetMethodID(env, PaintClass, "setPathEffect",
		   "(Landroid/graphics/PathEffect;)Landroid/graphics/PathEffect;" );
   PaintClass_setStyle = (*env)->GetMethodID(env, PaintClass, "setStyle", "(Landroid/graphics/Paint$Style;)V" );

   DashPathEffect = (*env)->FindClass(env, "android/graphics/DashPathEffect");
   DashPathEffect_init =(*env)->GetMethodID(env, DashPathEffect, "<init>", "([FF)V" );

   CapClass = (*env)->FindClass(env, "android/graphics/Paint$Cap");
   CapClass_valueOf = (*env)->GetStaticMethodID(env, CapClass, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Paint$Cap;" );
   CapClass_BUTT = (*env)->GetStaticObjectField(env, CapClass,
   		   (*env)-> GetStaticFieldID(env, CapClass, "BUTT","Landroid/graphics/Paint$Cap;"));

   PaintStyleClass = (*env)->FindClass(env, "android/graphics/Paint$Style");
   PaintStyle_FILL_AND_STROKE = (*env)->GetStaticObjectField(env, PaintStyleClass,
		   (*env)-> GetStaticFieldID(env, PaintStyleClass, "FILL_AND_STROKE","Landroid/graphics/Paint$Style;"));
   PaintStyle_STROKE = (*env)->GetStaticObjectField(env, PaintStyleClass,
		   (*env)-> GetStaticFieldID(env, PaintStyleClass, "STROKE","Landroid/graphics/Paint$Style;"));

   RenderingContextClass = (*env)->GetObjectClass(env, rc);
   RenderingContextClass_interrupted = getFid( RenderingContextClass, "interrupted", "Z" );

   MapRenderingTypesClass = (*env)->FindClass(env, "net/osmand/osm/MapRenderingTypes");
   MapRenderingTypes_getMainObjectType = (*env)->GetStaticMethodID(env, MapRenderingTypesClass,"getMainObjectType","(I)I");
   MapRenderingTypes_getObjectSubType = (*env)->GetStaticMethodID(env, MapRenderingTypesClass, "getObjectSubType","(I)I");
   MapRenderingTypes_getNegativeWayLayer = (*env)->GetStaticMethodID(env, MapRenderingTypesClass,"getNegativeWayLayer","(I)I");

   BinaryMapDataObjectClass = (*env)->FindClass(env, "net/osmand/binary/BinaryMapDataObject");
   BinaryMapDataObject_getPointsLength = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getPointsLength","()I");
   BinaryMapDataObject_getPoint31YTile = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getPoint31YTile","(I)I");
   BinaryMapDataObject_getPoint31XTile = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getPoint31XTile","(I)I");
   BinaryMapDataObject_getTypes = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getTypes","()[I");
   BinaryMapDataObject_getTagValue = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getTagValue",
		   "(I)Lnet/osmand/binary/BinaryMapIndexReader$TagValuePair;");

   TagValuePairClass = (*env)->FindClass(env, "net/osmand/binary/BinaryMapIndexReader$TagValuePair");
   TagValuePair_tag = (*env)->GetFieldID(env, TagValuePairClass, "tag", "Ljava/lang/String;");
   TagValuePair_value= (*env)->GetFieldID(env, TagValuePairClass, "value", "Ljava/lang/String;");

   RenderingRuleStoragePropertiesClass = (*env)->FindClass(env, "net/osmand/render/RenderingRuleStorageProperties");
   RenderingRulePropertyClass = (*env)->FindClass(env, "net/osmand/render/RenderingRuleProperty");


   RenderingRuleSearchRequestClass = (*env)->FindClass(env, "net/osmand/render/RenderingRuleSearchRequest");
   RenderingRuleSearchRequest_setInitialTagValueZoom =
		   (*env)->GetMethodID(env, RenderingRuleSearchRequestClass,"setInitialTagValueZoom",
				   "(Ljava/lang/String;Ljava/lang/String;I)V");
   RenderingRuleSearchRequest_ALL = (*env)->GetFieldID(env, RenderingRuleSearchRequestClass, "ALL",
		   "Lnet/osmand/render/RenderingRuleStorageProperties;");
   RenderingRuleSearchRequest_getIntPropertyValue = (*env)->GetMethodID(env, RenderingRuleSearchRequestClass,
   		   "getIntPropertyValue",  "(Lnet/osmand/render/RenderingRuleProperty;)I");
   RenderingRuleSearchRequest_getIntIntPropertyValue = (*env)->GetMethodID(env, RenderingRuleSearchRequestClass,
      		   "getIntPropertyValue",  "(Lnet/osmand/render/RenderingRuleProperty;I)I");
   RenderingRuleSearchRequest_getFloatPropertyValue = (*env)->GetMethodID(env, RenderingRuleSearchRequestClass,
   		   "getFloatPropertyValue",  "(Lnet/osmand/render/RenderingRuleProperty;)F");
   RenderingRuleSearchRequest_getStringPropertyValue = (*env)->GetMethodID(env, RenderingRuleSearchRequestClass,
    	   "getStringPropertyValue",  "(Lnet/osmand/render/RenderingRuleProperty;)Ljava/lang/String;");
   RenderingRuleSearchRequest_setIntFilter = (*env)->GetMethodID(env, RenderingRuleSearchRequestClass,
       	   "setIntFilter",  "(Lnet/osmand/render/RenderingRuleProperty;I)V");
   RenderingRuleSearchRequest_setStringFilter = (*env)->GetMethodID(env, RenderingRuleSearchRequestClass,
           "setStringFilter", "(Lnet/osmand/render/RenderingRuleProperty;Ljava/lang/String;)V");
   RenderingRuleSearchRequest_setBooleanFilter = (*env)->GetMethodID(env, RenderingRuleSearchRequestClass,
           "setBooleanFilter", "(Lnet/osmand/render/RenderingRuleProperty;Z)V");
   RenderingRuleSearchRequest_search = (*env)->GetMethodID(env, RenderingRuleSearchRequestClass, "search",  "(I)Z");
   RenderingRuleSearchRequest_searchI = (*env)->GetMethodID(env, RenderingRuleSearchRequestClass, "search",  "(IZ)Z");

}

void unloadLibrary()
{
   (*env)->DeleteLocalRef( env, MultiPolygonClass );
   (*env)->DeleteLocalRef( env, MapRenderingTypesClass );
   (*env)->DeleteLocalRef( env, PathClass );
   (*env)->DeleteLocalRef( env, CanvasClass );
   (*env)->DeleteLocalRef( env, PaintClass );
   (*env)->DeleteLocalRef( env, RenderingContextClass );
   (*env)->DeleteLocalRef( env, DashPathEffect );
   (*env)->DeleteLocalRef( env, CapClass );
   (*env)->DeleteLocalRef( env, CapClass_BUTT );
   (*env)->DeleteLocalRef( env, PaintStyleClass );
   (*env)->DeleteLocalRef( env, PaintStyle_FILL_AND_STROKE );
   (*env)->DeleteLocalRef( env, PaintStyle_STROKE );
   (*env)->DeleteLocalRef( env, TagValuePairClass);
   (*env)->DeleteLocalRef( env, RenderingRuleSearchRequestClass);
   (*env)->DeleteLocalRef( env, RenderingRulePropertyClass);
   (*env)->DeleteLocalRef( env, RenderingRuleStoragePropertiesClass);
   (*env)->DeleteLocalRef( env, BinaryMapDataObjectClass );

}



JNIEXPORT jstring JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering( JNIEnv* ienv, 
		jobject obj, jobject renderingContext, jobjectArray binaryMapDataObjects, jobject cv,
		jboolean useEnglishNames, jobject renderingRuleSearchRequest, jobject paint) {
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Initializing rendering");
	int i = 0;
	if(!env) {
	   env = ienv;
	   initLibrary(renderingContext);
	}
	env = ienv;
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Classes and methods are loaded");
	
	const size_t size = (*env)->GetArrayLength(env, binaryMapDataObjects);
    char szResult[1024];
    RenderingContext rc;
    
    copyRenderingContext(renderingContext, &rc);
    // szResult = malloc(sizeof(szFormat) + 20);
    
    __android_log_print(ANDROID_LOG_WARN, "net.osmand", "Rendering image");
    
    for(; i < size; i++) 
    {
   	    jobject binaryMapDataObject = (jobject) (*env)->GetObjectArrayElement(env, binaryMapDataObjects, i);
   	    jintArray types = (*env)->CallObjectMethod(env, binaryMapDataObject, BinaryMapDataObject_getTypes);
   	    // check multipolygon?
   	    if (types != NULL) {
			jint sizeTypes = (*env)->GetArrayLength(env, types);
			(*env)->DeleteLocalRef(env, types);
			int j = 0;
			for (; j < sizeTypes; j++) {
				drawObject(&rc, binaryMapDataObject, cv, renderingRuleSearchRequest, paint, j, 1, 0);
			}
		}
   	    
   	    (*env)->DeleteLocalRef(env, binaryMapDataObject);
    } 

    __android_log_print(ANDROID_LOG_WARN, "net.osmand", "End Rendering image");

    sprintf(szResult, "Hello android %d", size);
  
    // get an object string  
    jstring result = (*env)->NewStringUTF(env, szResult);  
  
    // cleanup  
    // free(szResult);
      
  	mergeRenderingContext(renderingContext, &rc);

//  unloadLibrary();
  	
  	
	return result;
}

