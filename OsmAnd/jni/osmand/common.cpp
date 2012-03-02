#include <android/log.h>

#include <string>
#include <vector>
#include <hash_map>
#include <SkPath.h>
#include <SkBitmap.h>
#include <SkImageDecoder.h>

#include "common.h"

const char* const LOG_TAG = "net.osmand:native";

JavaVM* globalJVM = NULL;
JNIEnv* globalJniEnv = NULL;

JNIEnv* getGlobalJniEnv()
{
	return globalJniEnv;
}

JNIEnv* setGlobalJniEnv(JNIEnv* e)
{
	if(!globalJVM)
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "No globalJVM set");
	globalJVM->GetEnv((void**)&globalJniEnv, JNI_VERSION_1_6);
	return globalJniEnv;
}

// Forward declarations
void loadJniCommon();
void loadJniRendering();
void loadJniRenderingRules();
void loadJniMapObjects();
void loadJniBinaryRead();
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
	if(vm->GetEnv((void **)&globalJniEnv, JNI_VERSION_1_6))
		return JNI_ERR; /* JNI version not supported */
	globalJVM = vm;

	loadJniCommon();
	loadJniRendering();
	loadJniRenderingRules();
	loadJniMapObjects();
	loadJniBinaryRead();

	__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "JNI_OnLoad completed");

	return JNI_VERSION_1_6;
}

void throwNewException(const char* msg)
{
	__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, msg);
	getGlobalJniEnv()->ThrowNew(getGlobalJniEnv()->FindClass("java/lang/Exception"), msg);
}

jclass findClass(const char* className, bool mustHave/* = true*/)
{
	jclass javaClass = getGlobalJniEnv()->FindClass(className);
	if(!javaClass && mustHave)
		throwNewException((std::string("Failed to find class ") + className).c_str());
	return (jclass)newGlobalRef(javaClass);
}

jobject newGlobalRef(jobject o)
{
	return getGlobalJniEnv()->NewGlobalRef(o);
}

jfieldID getFid(jclass cls, const char* fieldName, const char* sig)
{
	jfieldID jfield = getGlobalJniEnv()->GetFieldID(cls, fieldName, sig);
	if(!jfield)
		throwNewException((std::string("Failed to find field ") + fieldName + std::string(" with signature ") + sig).c_str());
	return jfield;
}

std::string getStringField(jobject o, jfieldID fid)
{
	jstring jstr = (jstring)getGlobalJniEnv()->GetObjectField(o, fid);
	if(!jstr)
	{
		throwNewException("Failed to get object from field");
		return std::string();
	}
	const char* utfBytes = getGlobalJniEnv()->GetStringUTFChars(jstr, NULL);
	//TODO: I'm not quite sure that if real unicode will happen here, this will work as expected
	std::string result(utfBytes);
	getGlobalJniEnv()->ReleaseStringUTFChars(jstr, utfBytes);
	getGlobalJniEnv()->DeleteLocalRef(jstr);
	return result;
}

std::string getString(jstring jstr)
{
	if(!jstr)
	{
		throwNewException("NULL jstring passed in");
		return std::string();
	}
	const char* utfBytes = getGlobalJniEnv()->GetStringUTFChars(jstr, NULL);
	//TODO: I'm not quite sure that if real unicode will happen here, this will work as expected
	std::string result(utfBytes);
	getGlobalJniEnv()->ReleaseStringUTFChars(jstr, utfBytes);
	return result;
}

std::string getStringMethod(jobject o, jmethodID fid)
{
	return getString((jstring)getGlobalJniEnv()->CallObjectMethod(o, fid));
}

std::string getStringMethod(jobject o, jmethodID fid, int i)
{
	return getString((jstring)getGlobalJniEnv()->CallObjectMethod(o, fid, i));
}

jclass jclass_RenderingContext = NULL;
jfieldID jfield_RenderingContext_interrupted = NULL;
jclass jclass_RenderingIcons = NULL;
jmethodID jmethod_RenderingIcons_getIconAsByteBuffer = NULL;
jfieldID jfield_RenderingContext_leftX = NULL;
jfieldID jfield_RenderingContext_topY = NULL;
jfieldID jfield_RenderingContext_width = NULL;
jfieldID jfield_RenderingContext_height = NULL;
jfieldID jfield_RenderingContext_zoom = NULL;
jfieldID jfield_RenderingContext_rotate = NULL;
jfieldID jfield_RenderingContext_tileDivisor = NULL;
jfieldID jfield_RenderingContext_pointCount = NULL;
jfieldID jfield_RenderingContext_pointInsideCount = NULL;
jfieldID jfield_RenderingContext_visible = NULL;
jfieldID jfield_RenderingContext_allObjects = NULL;
jfieldID jfield_RenderingContext_cosRotateTileSize = NULL;
jfieldID jfield_RenderingContext_sinRotateTileSize = NULL;
jfieldID jfield_RenderingContext_density = NULL;
jfieldID jfield_RenderingContext_highResMode = NULL;
jfieldID jfield_RenderingContext_mapTextSize = NULL;
jfieldID jfield_RenderingContext_shadowRenderingMode = NULL;
jfieldID jfield_RenderingContext_shadowLevelMin = NULL;
jfieldID jfield_RenderingContext_shadowLevelMax = NULL;
jfieldID jfield_RenderingContext_ctx = NULL;
jfieldID jfield_RenderingContext_textRenderingTime = NULL;
jfieldID jfield_RenderingContext_lastRenderedKey = NULL;

void loadJniCommon()
{
	jclass_RenderingContext = findClass("net/osmand/plus/render/OsmandRenderer$RenderingContext");
	jfield_RenderingContext_interrupted = getFid(jclass_RenderingContext, "interrupted", "Z");
	jfield_RenderingContext_leftX = getFid( jclass_RenderingContext, "leftX", "F" );
	jfield_RenderingContext_topY = getFid( jclass_RenderingContext, "topY", "F" );
	jfield_RenderingContext_width = getFid( jclass_RenderingContext, "width", "I" );
	jfield_RenderingContext_height = getFid( jclass_RenderingContext, "height", "I" );
	jfield_RenderingContext_zoom = getFid( jclass_RenderingContext, "zoom", "I" );
	jfield_RenderingContext_rotate = getFid( jclass_RenderingContext, "rotate", "F" );
	jfield_RenderingContext_tileDivisor = getFid( jclass_RenderingContext, "tileDivisor", "F" );
	jfield_RenderingContext_pointCount = getFid( jclass_RenderingContext, "pointCount", "I" );
	jfield_RenderingContext_pointInsideCount = getFid( jclass_RenderingContext, "pointInsideCount", "I" );
	jfield_RenderingContext_visible = getFid( jclass_RenderingContext, "visible", "I" );
	jfield_RenderingContext_allObjects = getFid( jclass_RenderingContext, "allObjects", "I" );
	jfield_RenderingContext_cosRotateTileSize = getFid( jclass_RenderingContext, "cosRotateTileSize", "F" );
	jfield_RenderingContext_sinRotateTileSize = getFid( jclass_RenderingContext, "sinRotateTileSize", "F" );
	jfield_RenderingContext_density = getFid( jclass_RenderingContext, "density", "F" );
	jfield_RenderingContext_highResMode = getFid( jclass_RenderingContext, "highResMode", "Z" );
	jfield_RenderingContext_mapTextSize = getFid( jclass_RenderingContext, "mapTextSize", "F" );
	jfield_RenderingContext_shadowRenderingMode = getFid( jclass_RenderingContext, "shadowRenderingMode", "I" );
	jfield_RenderingContext_shadowLevelMin = getFid( jclass_RenderingContext, "shadowLevelMin", "I" );
	jfield_RenderingContext_shadowLevelMax = getFid( jclass_RenderingContext, "shadowLevelMax", "I" );
	jfield_RenderingContext_ctx = getFid( jclass_RenderingContext, "ctx", "Landroid/content/Context;" );
	jfield_RenderingContext_textRenderingTime = getFid( jclass_RenderingContext, "textRenderingTime", "I" );
	jfield_RenderingContext_lastRenderedKey = getFid( jclass_RenderingContext, "lastRenderedKey", "I" );
	
	jclass_RenderingIcons = findClass("net/osmand/plus/render/RenderingIcons");
	jmethod_RenderingIcons_getIconAsByteBuffer = getGlobalJniEnv()->GetStaticMethodID(jclass_RenderingIcons,
		"getIconAsByteBuffer",
		"(Landroid/content/Context;Ljava/lang/String;)Ljava/nio/ByteBuffer;");
}

//TODO: Dead code
/*
void unloadJniCommon() {
	getGlobalJniEnv()->DeleteGlobalRef(jclass_RenderingIcons);
	getGlobalJniEnv()->DeleteGlobalRef(jclass_RenderingContext);
}
*/

void pullFromJavaRenderingContext(jobject jrc, RenderingContext* rc)
{
	rc->leftX = getGlobalJniEnv()->GetFloatField( jrc, jfield_RenderingContext_leftX );
	rc->topY = getGlobalJniEnv()->GetFloatField( jrc, jfield_RenderingContext_topY );
	rc->width = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_width );
	rc->height = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_height );

	rc->zoom = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_zoom );
	rc->rotate = getGlobalJniEnv()->GetFloatField( jrc, jfield_RenderingContext_rotate );
	rc->tileDivisor = getGlobalJniEnv()->GetFloatField( jrc, jfield_RenderingContext_tileDivisor );

	rc->pointCount = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_pointCount );
	rc->pointInsideCount = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_pointInsideCount );
	rc->visible = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_visible );
	rc->allObjects = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_allObjects );

	rc->cosRotateTileSize = getGlobalJniEnv()->GetFloatField( jrc, jfield_RenderingContext_cosRotateTileSize );
	rc->sinRotateTileSize = getGlobalJniEnv()->GetFloatField( jrc, jfield_RenderingContext_sinRotateTileSize );
	rc->density = getGlobalJniEnv()->GetFloatField( jrc, jfield_RenderingContext_density );
	rc->highResMode = getGlobalJniEnv()->GetBooleanField( jrc, jfield_RenderingContext_highResMode );
	rc->mapTextSize = getGlobalJniEnv()->GetFloatField( jrc, jfield_RenderingContext_mapTextSize );

	rc->shadowRenderingMode = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_shadowRenderingMode );
	rc->shadowLevelMin = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_shadowLevelMin );
	rc->shadowLevelMax = getGlobalJniEnv()->GetIntField( jrc, jfield_RenderingContext_shadowLevelMax );
	rc->androidContext = getGlobalJniEnv()->GetObjectField(jrc, jfield_RenderingContext_ctx );
	rc->lastRenderedKey = 0;

	rc->javaRenderingContext = jrc;
}


void pushToJavaRenderingContext(jobject jrc, RenderingContext* rc)
{
	getGlobalJniEnv()->SetIntField( jrc, jfield_RenderingContext_pointCount, rc->pointCount);
	getGlobalJniEnv()->SetIntField( jrc, jfield_RenderingContext_pointInsideCount, rc->pointInsideCount);
	getGlobalJniEnv()->SetIntField( jrc, jfield_RenderingContext_visible, rc->visible);
	getGlobalJniEnv()->SetIntField( jrc, jfield_RenderingContext_allObjects, rc->allObjects);
	getGlobalJniEnv()->SetIntField( jrc, jfield_RenderingContext_textRenderingTime, rc->textRendering.getElapsedTime());
	getGlobalJniEnv()->SetIntField( jrc, jfield_RenderingContext_lastRenderedKey, rc->lastRenderedKey);

	getGlobalJniEnv()->DeleteLocalRef(rc->androidContext);
}

TextDrawInfo::TextDrawInfo(std::string itext)
	: text(itext)
	, drawOnPath(false)
	, path(NULL)
	, pathRotate(0)
{
}

TextDrawInfo::~TextDrawInfo()
{
	if (path)
		delete path;
}

IconDrawInfo::IconDrawInfo()
	: bmp(NULL)
{
}

RenderingContext::RenderingContext()
	: javaRenderingContext(NULL)
	, androidContext(NULL)
{
}

RenderingContext::~RenderingContext()
{
	std::vector<TextDrawInfo*>::iterator itTextToDraw;
	for(itTextToDraw = textToDraw.begin(); itTextToDraw != textToDraw.end(); ++itTextToDraw)
		delete (*itTextToDraw);
}

bool RenderingContext::interrupted()
{
	return getGlobalJniEnv()->GetBooleanField(javaRenderingContext, jfield_RenderingContext_interrupted);
}

float getDensityValue(RenderingContext* rc, float val)
{
	if (rc->highResMode && rc->density > 1)
		return val * rc->density * rc->mapTextSize;
	else
		return val * rc->mapTextSize;
}

ElapsedTimer::ElapsedTimer()
	: elapsedTime(0)
	, enableFlag(true)
	, run(false)
{
}

void ElapsedTimer::enable()
{
	enableFlag = true;
}

void ElapsedTimer::disable()
{
	pause();
	enableFlag = false;
}

void ElapsedTimer::start()
{
	if (!enableFlag)
		return;
	if (!run)
		clock_gettime(CLOCK_MONOTONIC, &startInit);
	run = true;
}

void ElapsedTimer::pause()
{
	if (!run)
		return;

	clock_gettime(CLOCK_MONOTONIC, &endInit);
	int sec = endInit.tv_sec - startInit.tv_sec;
	if (sec > 0)
		elapsedTime += 1e9 * sec;
	elapsedTime += endInit.tv_nsec - startInit.tv_nsec;
	run = false;
}

int ElapsedTimer::getElapsedTime()
{
	pause();
	return elapsedTime / 1e6;
}

std::hash_map<std::string, SkBitmap*> cachedBitmaps;

SkBitmap* getCachedBitmap(RenderingContext* rc, std::string bitmapResource)
{
	// Try to find previously cached
	std::hash_map<std::string, SkBitmap*>::iterator itPreviouslyCachedBitmap = cachedBitmaps.find(bitmapResource);
	if (itPreviouslyCachedBitmap != cachedBitmaps.end())
		return itPreviouslyCachedBitmap->second;
	
	rc->nativeOperations.pause();

	jstring jstr = getGlobalJniEnv()->NewStringUTF(bitmapResource.c_str());
	jobject javaIconByteBuffer = getGlobalJniEnv()->CallStaticObjectMethod(jclass_RenderingIcons, jmethod_RenderingIcons_getIconAsByteBuffer, rc->androidContext, jstr);
	if(!javaIconByteBuffer)
		return NULL;

	// Decode bitmap
	SkBitmap* iconBitmap = new SkBitmap();
	void* bitmapBuffer = getGlobalJniEnv()->GetDirectBufferAddress(javaIconByteBuffer);
	size_t bufferLen = getGlobalJniEnv()->GetDirectBufferCapacity(javaIconByteBuffer);
	//TODO: JPEG is badly supported! At the moment it needs sdcard to be present (sic). Patch that
	if(!SkImageDecoder::DecodeMemory(bitmapBuffer, bufferLen, iconBitmap))
	{
		// Failed to decode
		delete iconBitmap;

		rc->nativeOperations.start();
		getGlobalJniEnv()->DeleteLocalRef(javaIconByteBuffer);
		getGlobalJniEnv()->DeleteLocalRef(jstr);

		throwNewException((std::string("Failed to decode ") + bitmapResource).c_str());

		return NULL;
	}
	cachedBitmaps[bitmapResource] = iconBitmap;
	
	rc->nativeOperations.start();

	getGlobalJniEnv()->DeleteLocalRef(javaIconByteBuffer);
	getGlobalJniEnv()->DeleteLocalRef(jstr);
	
	return iconBitmap;
}

