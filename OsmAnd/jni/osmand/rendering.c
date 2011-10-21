
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
jmethodID PaintClass_setStyle;
jmethodID PaintClass_setColor;

jclass PaintStyleClass;
jobject PaintStyle_STROKE;
jobject PaintStyle_FILL_AND_STROKE;


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
		
		sprintf(debugMessage, "Coordinates %f %f", rc->calcX, rc->calcY);
		__android_log_print(ANDROID_LOG_WARN, "net.osmand", debugMessage);
		if(rc -> calcX >= 0 && rc -> calcX < rc -> width && 
				rc -> calcY >= 0 && rc -> calcY < rc ->height){
			rc -> pointInsideCount++;
		}
}

 void drawPolyline(jobject binaryMapDataObject,	jobject renderingRuleSearch, jobject cv, jobject paint,
 		RenderingContext* rc, jobject pair, int layer, int drawOnlyShadow)
 {
 	rc -> visible++;
 	__android_log_print(ANDROID_LOG_WARN, "net.osmand", "Draw polyline");
 	jint length = (*env)->CallIntMethod(env, binaryMapDataObject, BinaryMapDataObject_getPointsLength);
 	jobject path = NULL;
 	int i = 0;
 	for(; i< length ; i++)
 	{
 		calcPoint(binaryMapDataObject, i, rc);
 		if (path == NULL) {
 			path = (*env)->NewObject( env, PathClass, Path_init);
 			(*env)->CallVoidMethod(env, path, Path_moveTo, rc->calcX, rc->calcY);
 		} else {
 			(*env)->CallVoidMethod(env, path, Path_lineTo, rc->calcX, rc->calcY);
 		}
 	}

 	if (path) {
 		if (drawOnlyShadow) {
 			//int shadowColor = render.getIntPropertyValue(render.ALL.R_SHADOW_COLOR);
 			//int shadowRadius = render.getIntPropertyValue(render.ALL.R_SHADOW_RADIUS);
 			//drawPolylineShadow(canvas, rc, path, shadowColor, shadowRadius);
 		} else {
 			(*env)->CallVoidMethod(env, paint, PaintClass_setColor, (jint) 0xffdddddd);
 			(*env)->CallVoidMethod(env, paint, PaintClass_setStyle, PaintStyle_STROKE);
 			(*env)->CallVoidMethod(env, paint, PaintClass_setStrokeWidth, 3.5f);
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
	
	rc->cosRotateTileSize = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "cosRotateTileSize", "F" ) );
	rc->sinRotateTileSize = (*env)->GetIntField(env, orc, getFid( RenderingContextClass, "sinRotateTileSize", "F" ) );

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
   PaintClass_setStyle = (*env)->GetMethodID(env, PaintClass, "setStyle", "(Landroid/graphics/Paint$Style;)V" );


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

}

void unloadLibrary()
{
   (*env)->DeleteLocalRef( env, MultiPolygonClass );
   (*env)->DeleteLocalRef( env, MapRenderingTypesClass );
   (*env)->DeleteLocalRef( env, PathClass );
   (*env)->DeleteLocalRef( env, CanvasClass );
   (*env)->DeleteLocalRef( env, PaintClass );
   (*env)->DeleteLocalRef( env, RenderingContextClass );
   (*env)->DeleteLocalRef( env, PaintStyleClass );
   (*env)->DeleteLocalRef( env, PaintStyle_FILL_AND_STROKE );
   (*env)->DeleteLocalRef( env, PaintStyle_STROKE );

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
   	    
   	     drawObject(&rc, binaryMapDataObject, cv, renderingRuleSearchRequest, paint, 0, 1, 0);
   	    
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

