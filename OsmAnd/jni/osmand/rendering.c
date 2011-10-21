
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
jclass PaintClass_setStrokeWidth;
jclass PaintClass_setColor;


jclass MapRenderingTypesClass;
jmethodID MapRenderingTypes_getMainObjectType;
jmethodID MapRenderingTypes_getObjectSubType;
jmethodID MapRenderingTypes_getNegativeWayLayer;

jclass BinaryMapDataObjectClass;
jmethodID BinaryMapDataObject_getPointsLength;
jmethodID BinaryMapDataObject_getPoint31YTile;
jmethodID BinaryMapDataObject_getPoint31XTile;
jmethodID BinaryMapDataObject_getTypes;
jmethodID BinaryMapDataObject_getTagValue;

jclass RenderingContextClass;
jfieldID RenderingContextClass_interrupted;


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
		
		float tx = (*env)->CallFloatMethod(env, mapObject, BinaryMapDataObject_getPoint31XTile,  ind )
						/ (rc -> tileDivisor);
		float ty = (*env)->CallFloatMethod(env, mapObject, BinaryMapDataObject_getPoint31YTile, ind )
				/ (rc -> tileDivisor);
		
		float dTileX = tx - rc -> leftX;
		float dTileY = ty - rc -> topY;
		rc -> calcX = rc -> cosRotateTileSize * dTileX - rc -> sinRotateTileSize * dTileY;
		rc -> calcY = rc -> sinRotateTileSize * dTileX + rc ->cosRotateTileSize * dTileY;
		
		if(rc -> calcX >= 0 && rc -> calcX < rc -> width && 
				rc -> calcY >= 0 && rc -> calcY < rc ->height){
			rc -> pointInsideCount++;
		}
}

 void drawPolyline(jobject binaryMapDataObject,	jobject renderingRuleSearch, jobject cv, jobject paint,
 		RenderingContext* rc, jobject pair, int layer, int drawOnlyShadow)
 {
 	rc -> visible++;
 	jint length = (*env)->CallIntMethod(env, binaryMapDataObject, BinaryMapDataObject_getPointsLength);
 	jobject path;
 	int i = 0;
 	for(; i< length ; i++)
 	{
 		calcPoint(binaryMapDataObject, i, rc);
 		if (!path) {
 			path = (*env)->NewObject( env, PathClass, Path_init);
 			(*env)->CallObjectMethod(env, path, Path_moveTo, rc->calcX, rc->calcY);
 		} else {
 			(*env)->CallObjectMethod(env, path, Path_lineTo, rc->calcX, rc->calcY);
 		}
 	}

 	if (path) {
 		if (drawOnlyShadow) {
 			//int shadowColor = render.getIntPropertyValue(render.ALL.R_SHADOW_COLOR);
 			//int shadowRadius = render.getIntPropertyValue(render.ALL.R_SHADOW_RADIUS);
 			//drawPolylineShadow(canvas, rc, path, shadowColor, shadowRadius);
 		} else {
 			(*env)->CallObjectMethod(env, paint, PaintClass_setColor, (jint) -3355444);
 			(*env)->CallObjectMethod(env, paint, PaintClass_setStrokeWidth, 3.5f);
 			(*env)->CallObjectMethod(env, cv, Canvas_drawPath, path, paint);
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
		
		jint type = (*env)->CallStaticIntMethod(env, MapRenderingTypesClass, MapRenderingTypes_getMainObjectType, mainType);
		jint subtype = (*env)->CallStaticIntMethod(env, MapRenderingTypesClass, MapRenderingTypes_getObjectSubType, mainType);


		jobject pair = (*env)->CallObjectMethod(env, binaryMapDataObject, BinaryMapDataObject_getTagValue, mainType);
		if( t == 1 && !drawOnlyShadow) {
			// point

			// drawPoint(obj, render, canvas, rc, pair, renderText);
		} else if(t == 2) {
			// polyline
			int layer = (*env)->CallStaticIntMethod(env, MapRenderingTypesClass,
						MapRenderingTypes_getNegativeWayLayer, mainType);
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
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "1");
   PathClass = (*env)->FindClass(env, "android/graphics/Path");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "2");
   Path_init = (*env)->GetMethodID(env, PathClass, "<init>", "()V" );
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "3");
   Path_moveTo = (*env)->GetMethodID(env, PathClass, "moveTo", "(F,F)" );
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "4");
   Path_lineTo = (*env)->GetMethodID(env, PathClass, "lineTo", "(F,F)" );
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "5");

   CanvasClass = (*env)->FindClass(env, "android/graphics/Canvas");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "6");
   Canvas_drawPath = (*env)->GetMethodID(env, CanvasClass, "drawPath",
		   "(Landroid/graphics/Path;,Landroid/graphics/Paint;)V" );
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "7");

   PaintClass = (*env)->FindClass(env, "android/graphics/Paint");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "8");
   PaintClass_setColor = (*env)->GetMethodID(env, PaintClass, "setColor", "(I)V" );
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "9");
   PaintClass_setStrokeWidth = (*env)->GetMethodID(env, PaintClass, "setStrokeWidth", "(F)V" );
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "10");

   RenderingContextClass = (*env)->GetObjectClass(env, rc);
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "11");
   RenderingContextClass_interrupted = getFid( RenderingContextClass, "interrupted", "Z" );
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "12");

   MapRenderingTypesClass = (*env)->FindClass(env, "net/osmand/osm/MapRenderingTypes");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "13");
   MapRenderingTypes_getMainObjectType = (*env)->GetStaticMethodID(env, MapRenderingTypesClass,"getMainObjectType","(I)I");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "14");
   MapRenderingTypes_getObjectSubType = (*env)->GetStaticMethodID(env, MapRenderingTypesClass, "getObjectSubType","(I)I");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "15");
   MapRenderingTypes_getNegativeWayLayer = (*env)->GetMethodID(env, MapRenderingTypesClass,"getNegativeWayLayer","(I)I");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "16");

   BinaryMapDataObjectClass = (*env)->FindClass(env, "net/osmand/binary/BinaryMapDataObject");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "17");
   BinaryMapDataObject_getPointsLength = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getPointsLength","()I");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "18");
   BinaryMapDataObject_getPoint31YTile = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getPoint31YTile","(I)I");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "19");
   BinaryMapDataObject_getPoint31XTile = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getPoint31XTile","(I)I");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "20");
   BinaryMapDataObject_getTypes = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getTypes","()[I");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "21");
   BinaryMapDataObject_getTagValue = (*env)->GetMethodID(env, BinaryMapDataObjectClass,"getTagValue",
		   "(I)Lnet/osmand/binary/BinaryMapIndexReader$TagValuePair;");
   __android_log_print(ANDROID_LOG_WARN, "net.osmand", "22");


}

void unloadLibrary()
{
   (*env)->DeleteLocalRef( env, MultiPolygonClass );
   (*env)->DeleteLocalRef( env, MapRenderingTypesClass );
   (*env)->DeleteLocalRef( env, PathClass );
   (*env)->DeleteLocalRef( env, CanvasClass );
   (*env)->DeleteLocalRef( env, PaintClass );
   (*env)->DeleteLocalRef( env, RenderingContextClass );
   (*env)->DeleteLocalRef( env, BinaryMapDataObjectClass );

}



JNIEXPORT jstring JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering( JNIEnv* ienv, 
		jobject obj, jobject renderingContext, jobjectArray binaryMapDataObjects, jobject cv,
		jboolean useEnglishNames, jobject renderingRuleSearchRequest, jobject paint) {
	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Initializing rendering");
	int i = 0;
	//if(!env) {
	   env = ienv;
	   initLibrary(renderingContext);
	//}
	//env = ienv;
	
	const size_t size = (*env)->GetArrayLength(env, binaryMapDataObjects);
    char szResult[1024];
    RenderingContext rc;
    
    copyRenderingContext(renderingContext, &rc);
    // szResult = malloc(sizeof(szFormat) + 20);
    
    __android_log_print(ANDROID_LOG_WARN, "net.osmand", "Rendering image");
    
    for(; i < size; i++) 
    {
   	    jobject binaryMapDataObject = (jobject) (*env)->GetObjectArrayElement(env, binaryMapDataObjects, i);
   	    
   	    // drawObject(&rc, binaryMapDataObject, cv, renderingRuleSearchRequest, paint, 0, 1, 0);
   	    
   	    (*env)->DeleteLocalRef(env, binaryMapDataObject);
    } 

    __android_log_print(ANDROID_LOG_WARN, "net.osmand", "End Rendering image");

    sprintf(szResult, "Hello android %d", size);
  
    // get an object string  
    jstring result = (*env)->NewStringUTF(env, szResult);  
  
    // cleanup  
    // free(szResult);
      
  	mergeRenderingContext(renderingContext, &rc);

  	unloadLibrary();
  	
  	
	return result;
}

