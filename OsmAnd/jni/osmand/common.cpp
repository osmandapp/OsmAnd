#ifndef _OSMAND_COMMON
#define _OSMAND_COMMON

#include <common.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <hash_map>
#include <SkPath.h>
#include <SkBitmap.h>

JNIEnv* globalE;
JNIEnv* globalEnv(){
	return globalE;
}

JNIEnv* setGlobalEnv(JNIEnv* e) {
	globalE = e;
	return e;
}

extern void loadJniCommon();
extern void loadJNIRenderingRules();
extern void loadJNIRendering();
extern void loadJniMapObjects();
extern void loadJniBinaryRead();

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    if(vm->GetEnv((void **)&globalE, JNI_VERSION_1_4)){
    	return JNI_ERR; /* JNI version not supported */
    }
    loadJniCommon();
    loadJNIRendering();
    loadJNIRenderingRules();
    loadJniMapObjects();
    loadJniBinaryRead();
	return JNI_VERSION_1_4;
}

//extern "C" JNIEXPORT jboolean JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_loadLibrary(JNIEnv* ienv) {}


jclass RenderingContextClass;
jfieldID RenderingContext_interrupted;

jclass RenderingIconsClass;
jmethodID RenderingIcons_getIcon;

jclass globalRef(jobject o)
{
	return  (jclass) globalEnv()->NewGlobalRef( o);
}


jfieldID getFid(jclass cls,const char* fieldName, const char* sig )
{
	return globalEnv()->GetFieldID( cls, fieldName, sig);
}

watcher::watcher() {
	elapsedTime = 0;
	enableFlag = true;
	run = false;
}
void watcher::enable() {
	enableFlag = true;
}
void watcher::disable() {
	pause();
	enableFlag = false;
}
void watcher::start() {
	if (!enableFlag) {
		return;
	}
	if (!run) {
		clock_gettime(CLOCK_MONOTONIC, &startInit);
//			gettimeofday(&startInit, NULL);
	}
	run = true;
}
void watcher::pause() {
	if (!run) {
		return;
	}
	clock_gettime(CLOCK_MONOTONIC, &endInit);
	// gettimeofday(&endInit, NULL);
	int sec = endInit.tv_sec - startInit.tv_sec;
	if (sec > 0) {
		elapsedTime += 1e9 * sec;
	}
	elapsedTime += endInit.tv_nsec - startInit.tv_nsec;
//		elapsedTime += (endInit.tv_sec * 1000 + endInit.tv_usec / 1000)
//					- (startInit.tv_sec * 1000 + startInit.tv_usec / 1000);
	run = false;
}
int watcher::getElapsedTime() {
	pause();
	return elapsedTime / 1e6;
}



std::string getStringField(jobject o, jfieldID fid)
{
	jstring st = (jstring) globalEnv()->GetObjectField(o, fid);
	if(st == NULL)
	{
		return std::string();
	}
	const char* utf = globalEnv()->GetStringUTFChars(st, NULL);
	std::string res(utf);
	globalEnv()->ReleaseStringUTFChars(st, utf);
	globalEnv()->DeleteLocalRef(st);
	return res;
}

std::string getString(jstring st) {
	if (st == NULL) {
		return EMPTY_STRING;
	}
	const char* utf = globalEnv()->GetStringUTFChars(st, NULL);
	std::string res(utf);
	globalEnv()->ReleaseStringUTFChars(st, utf);
	globalEnv()->DeleteLocalRef(st);
	return res;
}

std::string getStringMethod(jobject o, jmethodID fid)
{
	return getString((jstring) globalEnv()->CallObjectMethod(o, fid));
}

std::string getStringMethod(jobject o, jmethodID fid, int i)
{
	return getString((jstring) globalEnv()->CallObjectMethod(o, fid, i));
}

float getDensityValue(RenderingContext* rc, float val) {
	if (rc -> highResMode && rc -> density > 1) {
		return val * rc -> density * rc -> mapTextSize;
	} else {
		return val * rc -> mapTextSize;
	}
}

SkBitmap* getNativeBitmap(jobject bmpObj){
	if(bmpObj == NULL){
		return NULL;
	}
	jclass bmpClass = globalEnv()->GetObjectClass(bmpObj);
	SkBitmap* bmp = (SkBitmap*)globalEnv()->CallIntMethod(bmpObj, globalEnv()->GetMethodID(bmpClass, "ni", "()I"));
	globalEnv()->DeleteLocalRef(bmpClass);
	return bmp;
}

std::hash_map<std::string, SkBitmap*> cachedBitmaps;
SkBitmap* getCachedBitmap(RenderingContext* rc, std::string js)
{
	if (cachedBitmaps.find(js) != cachedBitmaps.end()) {
		return cachedBitmaps[js];
	}
	rc->nativeOperations.pause();
	jstring jstr = globalEnv()->NewStringUTF(js.c_str());
	jobject bmp = globalEnv()->CallStaticObjectMethod(RenderingIconsClass, RenderingIcons_getIcon, rc->androidContext, jstr);
	SkBitmap* res = getNativeBitmap(bmp);
	rc->nativeOperations.start();

	globalEnv()->DeleteLocalRef(bmp);
	globalEnv()->DeleteLocalRef(jstr);
	if(res != NULL){
		res = new SkBitmap(*res);
	}
	cachedBitmaps[js] = res;

	return res;
}


void loadJniCommon() {
	RenderingContextClass = globalRef(globalEnv()->FindClass("net/osmand/plus/render/OsmandRenderer$RenderingContext"));
	RenderingContext_interrupted = getFid(RenderingContextClass, "interrupted", "Z");

	RenderingIconsClass = globalRef(globalEnv()->FindClass("net/osmand/render/RenderingRule"));
	RenderingIconsClass = globalRef(globalEnv()->FindClass("net/osmand/plus/render/RenderingIcons"));
	RenderingIcons_getIcon = globalEnv()->GetStaticMethodID(RenderingIconsClass, "getIcon",
			"(Landroid/content/Context;Ljava/lang/String;)Landroid/graphics/Bitmap;");

}

void unloadJniCommon() {
	globalEnv()->DeleteGlobalRef(RenderingContextClass);
	globalEnv()->DeleteGlobalRef(RenderingIconsClass);
}





void copyRenderingContext(jobject orc, RenderingContext* rc)
{
	rc->leftX = globalEnv()->GetFloatField( orc, getFid( RenderingContextClass, "leftX", "F" ) );
	rc->topY = globalEnv()->GetFloatField( orc, getFid( RenderingContextClass, "topY", "F" ) );
	rc->width = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "width", "I" ) );
	rc->height = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "height", "I" ) );


	rc->zoom = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "zoom", "I" ) );
	rc->rotate = globalEnv()->GetFloatField( orc, getFid( RenderingContextClass, "rotate", "F" ) );
	rc->tileDivisor = globalEnv()->GetFloatField( orc, getFid( RenderingContextClass, "tileDivisor", "F" ) );

	rc->pointCount = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "pointCount", "I" ) );
	rc->pointInsideCount = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "pointInsideCount", "I" ) );
	rc->visible = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "visible", "I" ) );
	rc->allObjects = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "allObjects", "I" ) );

	rc->cosRotateTileSize = globalEnv()->GetFloatField( orc, getFid( RenderingContextClass, "cosRotateTileSize", "F" ) );
	rc->sinRotateTileSize = globalEnv()->GetFloatField( orc, getFid( RenderingContextClass, "sinRotateTileSize", "F" ) );
	rc->density = globalEnv()->GetFloatField( orc, getFid( RenderingContextClass, "density", "F" ) );
	rc->highResMode = globalEnv()->GetBooleanField( orc, getFid( RenderingContextClass, "highResMode", "Z" ) );
	rc->mapTextSize = globalEnv()->GetFloatField( orc, getFid( RenderingContextClass, "mapTextSize", "F" ) );


	rc->shadowRenderingMode = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "shadowRenderingMode", "I" ) );
	rc->shadowLevelMin = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "shadowLevelMin", "I" ) );
	rc->shadowLevelMax = globalEnv()->GetIntField( orc, getFid( RenderingContextClass, "shadowLevelMax", "I" ) );
	rc->androidContext = globalEnv()->GetObjectField(orc, getFid( RenderingContextClass, "ctx", "Landroid/content/Context;"));
	rc->lastRenderedKey = 0;

	rc->originalRC = orc;

}


void mergeRenderingContext(jobject orc, RenderingContext* rc)
{
	globalEnv()->SetIntField( orc, getFid(RenderingContextClass, "pointCount", "I" ) , rc->pointCount);
	globalEnv()->SetIntField( orc, getFid(RenderingContextClass, "pointInsideCount", "I" ) , rc->pointInsideCount);
	globalEnv()->SetIntField( orc, getFid(RenderingContextClass, "visible", "I" ) , rc->visible);
	globalEnv()->SetIntField( orc, getFid(RenderingContextClass, "allObjects", "I" ) , rc->allObjects);
	globalEnv()->SetIntField( orc, getFid(RenderingContextClass, "textRenderingTime", "I" ) , rc->textRendering.getElapsedTime());
	globalEnv()->SetIntField( orc, getFid(RenderingContextClass, "lastRenderedKey", "I" ) , rc->lastRenderedKey);

	globalEnv()->DeleteLocalRef(rc->androidContext);
}



#endif
