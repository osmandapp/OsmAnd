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
// Forward declarations
void loadJniCommon(JNIEnv* env);
void loadJniRendering(JNIEnv* env);
void loadJniRenderingRules(JNIEnv* env);

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv* globalJniEnv;
	if(vm->GetEnv((void **)&globalJniEnv, JNI_VERSION_1_6))
		return JNI_ERR; /* JNI version not supported */
	globalJVM = vm;

	loadJniCommon(globalJniEnv);
	loadJniRendering(globalJniEnv);
	loadJniRenderingRules(globalJniEnv);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "JNI_OnLoad completed");

	return JNI_VERSION_1_6;
}

void throwNewException(JNIEnv* env, const char* msg)
{
	__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, msg);
	env->ThrowNew(env->FindClass("java/lang/Exception"), msg);
}

jclass findClass(JNIEnv* env, const char* className, bool mustHave/* = true*/)
{
	jclass javaClass = env->FindClass(className);
	if(!javaClass && mustHave)
		throwNewException(env, (std::string("Failed to find class ") + className).c_str());
	return (jclass)newGlobalRef(env, javaClass);
}

jobject newGlobalRef(JNIEnv* env, jobject o)
{
	return env->NewGlobalRef(o);
}

jfieldID getFid(JNIEnv* env, jclass cls, const char* fieldName, const char* sig)
{
	jfieldID jfield = env->GetFieldID(cls, fieldName, sig);
	if(!jfield)
		throwNewException(env, (std::string("Failed to find field ") + fieldName + std::string(" with signature ") + sig).c_str());
	return jfield;
}

std::string getStringField(JNIEnv* env, jobject o, jfieldID fid)
{
	jstring jstr = (jstring)env->GetObjectField(o, fid);
	if(!jstr)
	{
		throwNewException(env, "Failed to get object from field");
		return std::string();
	}
	const char* utfBytes = env->GetStringUTFChars(jstr, NULL);
	//TODO: I'm not quite sure that if real unicode will happen here, this will work as expected
	std::string result(utfBytes);
	env->ReleaseStringUTFChars(jstr, utfBytes);
	env->DeleteLocalRef(jstr);
	return result;
}

std::string getString(JNIEnv* env, jstring jstr)
{
	if(!jstr)
	{
		throwNewException(env, "NULL jstring passed in");
		return std::string();
	}
	const char* utfBytes = env->GetStringUTFChars(jstr, NULL);
	//TODO: I'm not quite sure that if real unicode will happen here, this will work as expected
	std::string result(utfBytes);
	env->ReleaseStringUTFChars(jstr, utfBytes);
	return result;
}

std::string getStringMethod(JNIEnv* env, jobject o, jmethodID fid)
{
	return getString(env, (jstring)env->CallObjectMethod(o, fid));
}

std::string getStringMethod(JNIEnv* env, jobject o, jmethodID fid, int i)
{
	return getString(env, (jstring)env->CallObjectMethod(o, fid, i));
}

jclass jclass_RenderingContext = NULL;
jfieldID jfield_RenderingContext_interrupted = NULL;
jclass jclass_RenderingIcons = NULL;
jmethodID jmethod_RenderingIcons_getIconRawData = NULL;
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

void loadJniCommon(JNIEnv* env)
{
	jclass_RenderingContext = findClass(env, "net/osmand/plus/render/OsmandRenderer$RenderingContext");
	jfield_RenderingContext_interrupted = getFid(env, jclass_RenderingContext, "interrupted", "Z");
	jfield_RenderingContext_leftX = getFid(env,  jclass_RenderingContext, "leftX", "F" );
	jfield_RenderingContext_topY = getFid(env,  jclass_RenderingContext, "topY", "F" );
	jfield_RenderingContext_width = getFid(env,  jclass_RenderingContext, "width", "I" );
	jfield_RenderingContext_height = getFid(env,  jclass_RenderingContext, "height", "I" );
	jfield_RenderingContext_zoom = getFid(env,  jclass_RenderingContext, "zoom", "I" );
	jfield_RenderingContext_rotate = getFid(env,  jclass_RenderingContext, "rotate", "F" );
	jfield_RenderingContext_tileDivisor = getFid(env,  jclass_RenderingContext, "tileDivisor", "F" );
	jfield_RenderingContext_pointCount = getFid(env,  jclass_RenderingContext, "pointCount", "I" );
	jfield_RenderingContext_pointInsideCount = getFid(env,  jclass_RenderingContext, "pointInsideCount", "I" );
	jfield_RenderingContext_visible = getFid(env,  jclass_RenderingContext, "visible", "I" );
	jfield_RenderingContext_allObjects = getFid(env,  jclass_RenderingContext, "allObjects", "I" );
	jfield_RenderingContext_cosRotateTileSize = getFid(env,  jclass_RenderingContext, "cosRotateTileSize", "F" );
	jfield_RenderingContext_sinRotateTileSize = getFid(env,  jclass_RenderingContext, "sinRotateTileSize", "F" );
	jfield_RenderingContext_density = getFid(env,  jclass_RenderingContext, "density", "F" );
	jfield_RenderingContext_highResMode = getFid(env,  jclass_RenderingContext, "highResMode", "Z" );
	jfield_RenderingContext_mapTextSize = getFid(env,  jclass_RenderingContext, "mapTextSize", "F" );
	jfield_RenderingContext_shadowRenderingMode = getFid(env,  jclass_RenderingContext, "shadowRenderingMode", "I" );
	jfield_RenderingContext_shadowLevelMin = getFid(env,  jclass_RenderingContext, "shadowLevelMin", "I" );
	jfield_RenderingContext_shadowLevelMax = getFid(env,  jclass_RenderingContext, "shadowLevelMax", "I" );
	jfield_RenderingContext_ctx = getFid(env,  jclass_RenderingContext, "ctx", "Landroid/content/Context;" );
	jfield_RenderingContext_textRenderingTime = getFid(env,  jclass_RenderingContext, "textRenderingTime", "I" );
	jfield_RenderingContext_lastRenderedKey = getFid(env,  jclass_RenderingContext, "lastRenderedKey", "I" );
	
	jclass_RenderingIcons = findClass(env, "net/osmand/plus/render/RenderingIcons");
	jmethod_RenderingIcons_getIconRawData = env->GetStaticMethodID(jclass_RenderingIcons,
		"getIconRawData",
		"(Landroid/content/Context;Ljava/lang/String;)[B");
}

void pullFromJavaRenderingContext(JNIEnv* env, jobject jrc, RenderingContext* rc)
{
	rc->env = env;
	rc->leftX = env->GetFloatField( jrc, jfield_RenderingContext_leftX );
	rc->topY = env->GetFloatField( jrc, jfield_RenderingContext_topY );
	rc->width = env->GetIntField( jrc, jfield_RenderingContext_width );
	rc->height = env->GetIntField( jrc, jfield_RenderingContext_height );

	rc->zoom = env->GetIntField( jrc, jfield_RenderingContext_zoom );
	rc->rotate = env->GetFloatField( jrc, jfield_RenderingContext_rotate );
	rc->tileDivisor = env->GetFloatField( jrc, jfield_RenderingContext_tileDivisor );

	rc->pointCount = env->GetIntField( jrc, jfield_RenderingContext_pointCount );
	rc->pointInsideCount = env->GetIntField( jrc, jfield_RenderingContext_pointInsideCount );
	rc->visible = env->GetIntField( jrc, jfield_RenderingContext_visible );
	rc->allObjects = env->GetIntField( jrc, jfield_RenderingContext_allObjects );

	rc->cosRotateTileSize = env->GetFloatField( jrc, jfield_RenderingContext_cosRotateTileSize );
	rc->sinRotateTileSize = env->GetFloatField( jrc, jfield_RenderingContext_sinRotateTileSize );
	rc->density = env->GetFloatField( jrc, jfield_RenderingContext_density );
	rc->highResMode = env->GetBooleanField( jrc, jfield_RenderingContext_highResMode );
	rc->mapTextSize = env->GetFloatField( jrc, jfield_RenderingContext_mapTextSize );

	rc->shadowRenderingMode = env->GetIntField( jrc, jfield_RenderingContext_shadowRenderingMode );
	rc->shadowLevelMin = env->GetIntField( jrc, jfield_RenderingContext_shadowLevelMin );
	rc->shadowLevelMax = env->GetIntField( jrc, jfield_RenderingContext_shadowLevelMax );
	rc->androidContext = env->GetObjectField(jrc, jfield_RenderingContext_ctx );
	rc->lastRenderedKey = 0;

	rc->javaRenderingContext = jrc;
}


void pushToJavaRenderingContext(JNIEnv* env, jobject jrc, RenderingContext* rc)
{
	env->SetIntField( jrc, jfield_RenderingContext_pointCount, rc->pointCount);
	env->SetIntField( jrc, jfield_RenderingContext_pointInsideCount, rc->pointInsideCount);
	env->SetIntField( jrc, jfield_RenderingContext_visible, rc->visible);
	env->SetIntField( jrc, jfield_RenderingContext_allObjects, rc->allObjects);
	env->SetIntField( jrc, jfield_RenderingContext_textRenderingTime, rc->textRendering.getElapsedTime());
	env->SetIntField( jrc, jfield_RenderingContext_lastRenderedKey, rc->lastRenderedKey);

	env->DeleteLocalRef(rc->androidContext);
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
	return env->GetBooleanField(javaRenderingContext, jfield_RenderingContext_interrupted);
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
SkBitmap* getCachedBitmap(RenderingContext* rc, const std::string& bitmapResource)
{
	JNIEnv* env = rc->env;
	if(bitmapResource.size() == 0)
		return NULL;

	// Try to find previously cached
	std::hash_map<std::string, SkBitmap*>::iterator itPreviouslyCachedBitmap = cachedBitmaps.find(bitmapResource);
	if (itPreviouslyCachedBitmap != cachedBitmaps.end())
		return itPreviouslyCachedBitmap->second;
	
	rc->nativeOperations.pause();

	jstring jstr = env->NewStringUTF(bitmapResource.c_str());
	jbyteArray javaIconRawData = (jbyteArray)env->CallStaticObjectMethod(jclass_RenderingIcons, jmethod_RenderingIcons_getIconRawData, rc->androidContext, jstr);
	if(!javaIconRawData)
		return NULL;

	jbyte* bitmapBuffer = env->GetByteArrayElements(javaIconRawData, NULL);
	size_t bufferLen = env->GetArrayLength(javaIconRawData);

	// Decode bitmap
	SkBitmap* iconBitmap = new SkBitmap();
	//TODO: JPEG is badly supported! At the moment it needs sdcard to be present (sic). Patch that
	if(!SkImageDecoder::DecodeMemory(bitmapBuffer, bufferLen, iconBitmap))
	{
		// Failed to decode
		delete iconBitmap;

		rc->nativeOperations.start();
		env->ReleaseByteArrayElements(javaIconRawData, bitmapBuffer, JNI_ABORT);
		env->DeleteLocalRef(javaIconRawData);
		env->DeleteLocalRef(jstr);

		throwNewException(env, (std::string("Failed to decode ") + bitmapResource).c_str());

		return NULL;
	}
	cachedBitmaps[bitmapResource] = iconBitmap;
	
	rc->nativeOperations.start();

	env->ReleaseByteArrayElements(javaIconRawData, bitmapBuffer, JNI_ABORT);
	env->DeleteLocalRef(javaIconRawData);
	env->DeleteLocalRef(jstr);
	
	return iconBitmap;
}

