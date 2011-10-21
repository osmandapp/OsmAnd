
#include <jni.h>
#include <android/log.h>
#include <time.h>

#include <stdio.h>
#include <stdlib.h>



JNIEnv* env;
jclass MultiPolygonClass;
jclass MapRenderingTypesClass;
jmethodID MapRenderingTypes_getMainObjectType;
jmethodID MapRenderingTypes_getObjectSubType;

typedef struct RenderingContext {
		// TODO check interrupted
		jobject originalRC;
		jfieldID interrupted;
		
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


jmethodID getMid(jobject obj, char* methodName, char* sig )
{
	jclass cls = (*env)->GetObjectClass(env, obj);
	return (*env)->GetMethodID(env, cls, methodName, sig);
}

jfieldID getFid(jobject obj, char* fieldName, char* sig )
{
	jclass cls = (*env)->GetObjectClass(env, obj);
	return (*env)->GetFieldID(env, cls, fieldName, sig);
}


 void calcPoint(jobject mapObject, jint ind, RenderingContext* rc)
 {
		rc -> pointCount ++;
		
		float tx = (*env)->CallFloatMethod(env, mapObject, getMid(mapObject, "getPoint31XTile", "(I)[F" ), 
						ind ) / (rc -> tileDivisor);
		float ty = (*env)->CallFloatMethod(env, mapObject, getMid(mapObject, "getPoint31YTile", "(I)[F" ), 
						ind ) / (rc -> tileDivisor);
		
		float dTileX = tx - rc -> leftX;
		float dTileY = ty - rc -> topY;
		rc -> calcX = rc -> cosRotateTileSize * dTileX - rc -> sinRotateTileSize * dTileY;
		rc -> calcY = rc -> sinRotateTileSize * dTileX + rc ->cosRotateTileSize * dTileY;
		
		if(rc -> calcX >= 0 && rc -> calcX < rc -> width && 
				rc -> calcY >= 0 && rc -> calcY < rc ->height){
			rc -> pointInsideCount++;
		}
} 

void drawObject(RenderingContext* rc, jobject binaryMapDataObject, jobject cv,
		jobject renderingRuleSearchRequest, int l) {
		
		rc -> allObjects++;
		
		
		if ((*env)->IsInstanceOf(env, binaryMapDataObject, MultiPolygonClass)) {
			//if(!drawOnlyShadow){
			//	drawMultiPolygon(obj, render, canvas, rc);
			//}
			 return; 
		}
		
		jintArray types = (*env)->CallObjectMethod(env, binaryMapDataObject, 
							         getMid(binaryMapDataObject, "getTypes", "()[I" ) );
		jint mainType;
		(*env)->GetIntArrayRegion(env, types, l, 1, &mainType);	
		int t = mainType & 3;
		(*env)->DeleteLocalRef(env, types);
		
		jint type = (*env)->CallStaticIntMethod(env, MapRenderingTypesClass, MapRenderingTypes_getMainObjectType, mainType);
		jint subtype = (*env)->CallStaticIntMethod(env, MapRenderingTypesClass, MapRenderingTypes_getObjectSubType, mainType);
		// TODO
		//TagValuePair pair = obj.getMapIndex().decodeType(type, subtype);
		if( t == 1) { 
			// point
		
		} else if(t == 2) {
			// polyline
		
		} else if(t == 3) {
			// polygon

		}
		
	/*	
			TagValuePair pair = obj.getMapIndex().decodeType(type, subtype);
			if (t == MapRenderingTypes.POINT_TYPE && !drawOnlyShadow) {
				drawPoint(obj, render, canvas, rc, pair, renderText);
			} else if (t == MapRenderingTypes.POLYLINE_TYPE) {
				int layer = MapRenderingTypes.getNegativeWayLayer(mainType);
				drawPolyline(obj, render, canvas, rc, pair, layer, drawOnlyShadow);
			} else if (t == MapRenderingTypes.POLYGON_TYPE && !drawOnlyShadow) {
				drawPolygon(obj, render, canvas, rc, pair);
			} else {
				if (t == MapRenderingTypes.MULTY_POLYGON_TYPE && !(obj instanceof MultyPolygon)) {
					// log this situation
					return;
				}
			}
		} */

}

void copyRenderingContext(jobject orc, RenderingContext* rc) 
{
	rc->leftX = (*env)->GetFloatField(env, orc, getFid( orc, "leftX", "F" ) ); 
	rc->topY = (*env)->GetFloatField(env, orc, getFid( orc, "topY", "F" ) );
	rc->width = (*env)->GetIntField(env, orc, getFid( orc, "width", "I" ) );
	rc->height = (*env)->GetIntField(env, orc, getFid( orc, "height", "I" ) );
	
	
	rc->zoom = (*env)->GetIntField(env, orc, getFid( orc, "zoom", "I" ) );
	rc->rotate = (*env)->GetFloatField(env, orc, getFid( orc, "rotate", "F" ) ); 
	rc->tileDivisor = (*env)->GetFloatField(env, orc, getFid( orc, "tileDivisor", "F" ) );
	
	rc->pointCount = (*env)->GetIntField(env, orc, getFid( orc, "pointCount", "I" ) );
	rc->pointInsideCount = (*env)->GetIntField(env, orc, getFid( orc, "pointInsideCount", "I" ) );
	rc->visible = (*env)->GetIntField(env, orc, getFid( orc, "visible", "I" ) );
	rc->allObjects = (*env)->GetIntField(env, orc, getFid( orc, "allObjects", "I" ) );
	
	rc->shadowRenderingMode = (*env)->GetIntField(env, orc, getFid( orc, "shadowRenderingMode", "I" ) );
	rc->shadowLevelMin = (*env)->GetIntField(env, orc, getFid( orc, "shadowLevelMin", "I" ) );
	rc->shadowLevelMax = (*env)->GetIntField(env, orc, getFid( orc, "shadowLevelMax", "I" ) );
	
	rc->interrupted = getFid( orc, "interrupted", "B" );
	rc->originalRC = orc; 

}


void mergeRenderingContext(jobject orc, RenderingContext* rc) 
{

	(*env)->SetIntField(env, orc, getFid(orc, "pointCount", "I" ) , rc->pointCount);
	(*env)->SetIntField(env, orc, getFid(orc, "pointInsideCount", "I" ) , rc->pointInsideCount);
	(*env)->SetIntField(env, orc, getFid(orc, "visible", "I" ) , rc->visible);
	(*env)->SetIntField(env, orc, getFid(orc, "allObjects", "I" ) , rc->allObjects);

}


void initLibrary() 
{
   MultiPolygonClass = (*env)->FindClass(env, "net/osmand/osm/MultyPolygon");
   MapRenderingTypesClass = (*env)->FindClass(env, "net/osmand/osm/MapRenderingTypes");
   MapRenderingTypes_getMainObjectType = (*env)->GetStaticMethodID(env, MapRenderingTypesClass,"getMainObjectType","(I)I");
   MapRenderingTypes_getObjectSubType = (*env)->GetStaticMethodID(env, MapRenderingTypesClass, "getObjectSubType","(I)I");
}



JNIEXPORT jstring JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering( JNIEnv* ienv, 
		jobject obj, jobject renderingContext, jobjectArray binaryMapDataObjects, jobject cv,
		jboolean useEnglishNames, jobject renderingRuleSearchRequest) {
	int i = 0;
	if(!env) {
	   env = ienv;
	   initLibrary();
	} 
	env = ienv;
	
	const size_t size = (*env)->GetArrayLength(env,binaryMapDataObjects);
	
    char szResult[1024];
    RenderingContext rc;
    
    
    
    // copyRenderingContext(renderingContext, &rc);
    // szResult = malloc(sizeof(szFormat) + 20);
    
    
	__android_log_print(ANDROID_LOG_INFO, "net.osmand", "Rendering cpp");    
    
    for(; i < size; i++) 
    {
   	    jobject binaryMapDataObject = (jobject) (*env)->GetObjectArrayElement(env, binaryMapDataObjects, i);
   	    
   	    drawObject(&rc, binaryMapDataObject, cv, renderingRuleSearchRequest, 0);
   	    
   	    (*env)->DeleteLocalRef(env, binaryMapDataObject);
    } 

    sprintf(szResult, "Hello android %d", size);
  
    // get an object string  
    jstring result = (*env)->NewStringUTF(env, szResult);  
  
    // cleanup  
    // free(szResult);
      
  	// mergeRenderingContext(renderingContext, &rc);
  	
  	
	return result;
}

