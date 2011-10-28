#ifndef _OSMAND_COMMON
#define _OSMAND_COMMON

#include <common.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <hash_map>
#include <SkPath.h>
#include <SkBitmap.h>

JNIEnv* env;

JNIEnv* globalEnv(){
	return env;
}

JNIEnv* setGlobalEnv(JNIEnv* e) {
	env = e;
	return e;
}

extern void loadJniCommon();
extern void loadJniBinaryRead();
extern void loadJNIRenderingRules();
extern void loadJniMapObjects();

//extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
//    if(vm->GetEnv((void **)&env, JNI_VERSION_1_2)){
//    	return JNI_ERR; /* JNI version not supported */
//    }
//
//	return JNI_VERSION_1_2;
//}

extern "C" JNIEXPORT jboolean JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_loadLibrary(JNIEnv* ienv) {
//	env = ienv;
	loadJniCommon();
    loadJNIRenderingRules();
    loadJniMapObjects();
    loadJniBinaryRead();
    return true;
}



jclass RenderingContextClass;
jfieldID RenderingContext_interrupted;

jclass RenderingIconsClass;
jmethodID RenderingIcons_getIcon;

jclass globalRef(jobject o)
{
	return  (jclass) env->NewGlobalRef( o);
}


jfieldID getFid(jclass cls,const char* fieldName, const char* sig )
{
	return env->GetFieldID( cls, fieldName, sig);
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
	jstring st = (jstring) env->GetObjectField(o, fid);
	if(st == NULL)
	{
		return std::string();
	}
	const char* utf = env->GetStringUTFChars(st, NULL);
	std::string res(utf);
	env->ReleaseStringUTFChars(st, utf);
	env->DeleteLocalRef(st);
	return res;
}

std::string getString(jstring st) {
	if (st == NULL) {
		return EMPTY_STRING;
	}
	const char* utf = env->GetStringUTFChars(st, NULL);
	std::string res(utf);
	env->ReleaseStringUTFChars(st, utf);
	env->DeleteLocalRef(st);
	return res;
}

std::string getStringMethod(jobject o, jmethodID fid)
{
	return getString((jstring) env->CallObjectMethod(o, fid));
}

std::string getStringMethod(jobject o, jmethodID fid, int i)
{
	return getString((jstring) env->CallObjectMethod(o, fid, i));
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
	jclass bmpClass = env->GetObjectClass(bmpObj);
	SkBitmap* bmp = (SkBitmap*)env->CallIntMethod(bmpObj, env->GetMethodID(bmpClass, "ni", "()I"));
	env->DeleteLocalRef(bmpClass);
	return bmp;
}

std::hash_map<std::string, SkBitmap*> cachedBitmaps;
SkBitmap* getCachedBitmap(RenderingContext* rc, std::string js)
{
	if (cachedBitmaps.find(js) != cachedBitmaps.end()) {
		return cachedBitmaps[js];
	}
	rc->nativeOperations.pause();
	jstring jstr = env->NewStringUTF(js.c_str());
	jobject bmp = env->CallStaticObjectMethod(RenderingIconsClass, RenderingIcons_getIcon, rc->androidContext, jstr);
	SkBitmap* res = getNativeBitmap(bmp);
	rc->nativeOperations.start();

	env->DeleteLocalRef(bmp);
	env->DeleteLocalRef(jstr);
	if(res != NULL){
		res = new SkBitmap(*res);
	}
	cachedBitmaps[js] = res;

	return res;
}


void loadJniCommon() {
	RenderingContextClass = globalRef(env->FindClass("net/osmand/plus/render/OsmandRenderer$RenderingContext"));
	RenderingContext_interrupted = getFid(RenderingContextClass, "interrupted", "Z");

	RenderingIconsClass = globalRef(env->FindClass("net/osmand/render/RenderingRule"));
	RenderingIconsClass = globalRef(env->FindClass("net/osmand/plus/render/RenderingIcons"));
	RenderingIcons_getIcon = env->GetStaticMethodID(RenderingIconsClass, "getIcon",
			"(Landroid/content/Context;Ljava/lang/String;)Landroid/graphics/Bitmap;");

}

void unloadJniCommon() {
	env->DeleteGlobalRef(RenderingContextClass);
	env->DeleteGlobalRef(RenderingIconsClass);
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



#endif
