#ifndef _JAVA_WRAP_CPP
#define _JAVA_WRAP_CPP

#include <dlfcn.h>
#include <SkBitmap.h>
#include <SkCanvas.h>
#include <SkImageDecoder.h>
#include "java_renderRules.h"
#include "common.h"
#include "java_wrap.h"
#include "binaryRead.h"
#include "rendering.h"


JavaVM* globalJVM = NULL;
void loadJniRenderingContext(JNIEnv* env);
void loadJniRenderingRules(JNIEnv* env);
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv* globalJniEnv;
	if(vm->GetEnv((void **)&globalJniEnv, JNI_VERSION_1_6))
		return JNI_ERR; /* JNI version not supported */
	globalJVM = vm;
	loadJniRenderingContext(globalJniEnv);
	loadJniRenderingRules(globalJniEnv);
	osmand_log_print(LOG_INFO, "JNI_OnLoad completed");

	return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL Java_net_osmand_NativeLibrary_deleteSearchResult(JNIEnv* ienv,
		jobject obj, jint searchResult) {
	ResultPublisher* result = (ResultPublisher*) searchResult;
	if(result != NULL){
		delete result;
	}
}


extern "C" JNIEXPORT void JNICALL Java_net_osmand_NativeLibrary_closeBinaryMapFile(JNIEnv* ienv,
		jobject path) {
	const char* utf = ienv->GetStringUTFChars((jstring) path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring) path, utf);
	closeBinaryMapFile(inputName);
}

extern "C" JNIEXPORT jboolean JNICALL Java_net_osmand_NativeLibrary_initBinaryMapFile(JNIEnv* ienv,
		jobject obj, jobject path) {
	// Verify that the version of the library that we linked against is
	const char* utf = ienv->GetStringUTFChars((jstring) path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring) path, utf);
	BinaryMapFile* fl = initBinaryMapFile(inputName);
	if(fl == NULL) {
		osmand_log_print(LOG_WARN, "File %s was not initialized", inputName.c_str());
	} else {
		osmand_log_print(LOG_INFO, "File %s is initialized.", fl->inputName.c_str());
	}
	return fl != NULL;
}




// Global object
HMAP::hash_map<void*, RenderingRulesStorage*> cachedStorages;

RenderingRulesStorage* getStorage(JNIEnv* env, jobject storage) {
	if (cachedStorages.find(storage) == cachedStorages.end()) {
		cachedStorages[storage] = createRenderingRulesStorage(env, storage);
	}
	return cachedStorages[storage];
}


extern "C" JNIEXPORT void JNICALL Java_net_osmand_NativeLibrary_initRenderingRulesStorage(JNIEnv* ienv,
		jobject obj, jobject storage) {
	getStorage(ienv, storage);
}

RenderingRuleSearchRequest* initSearchRequest(JNIEnv* env, jobject renderingRuleSearchRequest) {
	jobject storage = env->GetObjectField(renderingRuleSearchRequest, RenderingRuleSearchRequest_storage);
	RenderingRulesStorage* st = getStorage(env, storage);
	env->DeleteLocalRef(storage);
	RenderingRuleSearchRequest* res = new RenderingRuleSearchRequest(st);
	initRenderingRuleSearchRequest(env, res, renderingRuleSearchRequest);
	return res;
}


extern "C" JNIEXPORT jint JNICALL Java_net_osmand_NativeLibrary_searchNativeObjectsForRendering(JNIEnv* ienv,
		jobject obj, jint sleft, jint sright, jint stop, jint sbottom, jint zoom,
		jobject renderingRuleSearchRequest, bool skipDuplicates, jobject objInterrupted, jstring msgNothingFound) {
	RenderingRuleSearchRequest* req = initSearchRequest(ienv, renderingRuleSearchRequest);
	jfieldID interruptedField = 0;
	if(objInterrupted != NULL) {
		jclass clObjInterrupted = ienv->GetObjectClass(objInterrupted);
		interruptedField = getFid(ienv, clObjInterrupted, "interrupted", "Z");
		ienv->DeleteLocalRef(clObjInterrupted);
	}

	ResultJNIPublisher* j = new ResultJNIPublisher( objInterrupted, interruptedField, ienv);
	SearchQuery q(sleft, sright, stop, sbottom, req, j);
	q.zoom = zoom;


	ResultPublisher* res = searchObjectsForRendering(&q, skipDuplicates, getString(ienv, msgNothingFound));
	delete req;
	return (jint) j;
}


//////////////////////////////////////////
///////////// JNI RENDERING //////////////

#ifdef ANDROID_BUILD
#include <android/bitmap.h>

extern "C" JNIEXPORT jobject JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering_1Direct( JNIEnv* ienv, jobject obj,
    jobject renderingContext, jint searchResult,
    jobject targetBitmap, jboolean useEnglishNames, jobject renderingRuleSearchRequest, jint defaultColor) {

	// libJniGraphics interface
	typedef int (*PTR_AndroidBitmap_getInfo)(JNIEnv*, jobject, AndroidBitmapInfo*);
	typedef int (*PTR_AndroidBitmap_lockPixels)(JNIEnv*, jobject, void**);
	typedef int (*PTR_AndroidBitmap_unlockPixels)(JNIEnv*, jobject);
	static PTR_AndroidBitmap_getInfo dl_AndroidBitmap_getInfo = 0;
	static PTR_AndroidBitmap_lockPixels dl_AndroidBitmap_lockPixels = 0;
	static PTR_AndroidBitmap_unlockPixels dl_AndroidBitmap_unlockPixels = 0;
	static void* module_libjnigraphics = 0;

	if(!module_libjnigraphics)
	{
		module_libjnigraphics = dlopen("jnigraphics", /*RTLD_NOLOAD*/0x0004);
		if(!module_libjnigraphics) {
			osmand_log_print(LOG_WARN, "jnigraphics was not found in loaded libraries");
			module_libjnigraphics = dlopen("jnigraphics", RTLD_NOW);
		}
		if(!module_libjnigraphics) {
			osmand_log_print(LOG_WARN, "jnigraphics was not loaded in default location");
			module_libjnigraphics = dlopen("/system/lib/libjnigraphics.so", RTLD_NOW);
		}
		if(!module_libjnigraphics)
		{
			osmand_log_print(LOG_ERROR, "Failed to load jnigraphics via dlopen, will going to crash");
			return NULL;
		}
		dl_AndroidBitmap_getInfo = (PTR_AndroidBitmap_getInfo)dlsym(module_libjnigraphics, "AndroidBitmap_getInfo");
		dl_AndroidBitmap_lockPixels = (PTR_AndroidBitmap_lockPixels)dlsym(module_libjnigraphics, "AndroidBitmap_lockPixels");
		dl_AndroidBitmap_unlockPixels = (PTR_AndroidBitmap_unlockPixels)dlsym(module_libjnigraphics, "AndroidBitmap_unlockPixels");
	}

	// Gain information about bitmap
	AndroidBitmapInfo bitmapInfo;
	if(dl_AndroidBitmap_getInfo(ienv, targetBitmap, &bitmapInfo) != ANDROID_BITMAP_RESUT_SUCCESS)
	osmand_log_print(LOG_ERROR, "Failed to execute AndroidBitmap_getInfo");

	osmand_log_print(LOG_INFO, "Creating SkBitmap in native w:%d h:%d s:%d f:%d!", bitmapInfo.width, bitmapInfo.height, bitmapInfo.stride, bitmapInfo.format);

	SkBitmap* bitmap = new SkBitmap();
	if(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
		int rowBytes = bitmapInfo.stride;
		osmand_log_print(LOG_INFO, "Row bytes for RGBA_8888 is %d", rowBytes);
		bitmap->setConfig(SkBitmap::kARGB_8888_Config, bitmapInfo.width, bitmapInfo.height, rowBytes);
	} else if(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565) {
		int rowBytes = bitmapInfo.stride;
		osmand_log_print(LOG_INFO, "Row bytes for RGB_565 is %d", rowBytes);
		bitmap->setConfig(SkBitmap::kRGB_565_Config, bitmapInfo.width, bitmapInfo.height, rowBytes);
	} else {
		osmand_log_print(LOG_ERROR, "Unknown target bitmap format");
	}

	void* lockedBitmapData = NULL;
	if(dl_AndroidBitmap_lockPixels(ienv, targetBitmap, &lockedBitmapData) != ANDROID_BITMAP_RESUT_SUCCESS || !lockedBitmapData) {
		osmand_log_print(LOG_ERROR, "Failed to execute AndroidBitmap_lockPixels");
	}
	osmand_log_print(LOG_INFO, "Locked %d bytes at %p", bitmap->getSize(), lockedBitmapData);

	bitmap->setPixels(lockedBitmapData);

	osmand_log_print(LOG_INFO, "Initializing rendering");
	ElapsedTimer initObjects;
	initObjects.start();

	RenderingRuleSearchRequest* req = initSearchRequest(ienv, renderingRuleSearchRequest);
	JNIRenderingContext rc;
	pullFromJavaRenderingContext(ienv, renderingContext, &rc);
	rc.setUseEnglishNames(useEnglishNames);
	ResultPublisher* result = ((ResultPublisher*) searchResult);
	//    std::vector <BaseMapDataObject* > mapDataObjects = marshalObjects(binaryMapDataObjects);

	osmand_log_print(LOG_INFO, "Rendering image");
	initObjects.pause();

	// Main part do rendering
	rc.nativeOperations.start();
	SkCanvas* canvas = new SkCanvas(*bitmap);
	canvas->drawColor(defaultColor);
	if(result != NULL) {
		doRendering(result->result, canvas, req, &rc);
	}

	rc.nativeOperations.pause();

	pushToJavaRenderingContext(ienv, renderingContext, &rc);
	osmand_log_print(LOG_INFO, "End Rendering image");
	if(dl_AndroidBitmap_unlockPixels(ienv, targetBitmap) != ANDROID_BITMAP_RESUT_SUCCESS) {
		osmand_log_print(LOG_ERROR, "Failed to execute AndroidBitmap_unlockPixels");
	}

	// delete  variables
	delete canvas;
	delete req;
	delete bitmap;
	//    deleteObjects(mapDataObjects);

	jclass resultClass = findClass(ienv, "net/osmand/NativeLibrary$8RenderingGenerationResult");

	jmethodID resultClassCtorId = ienv->GetMethodID(resultClass, "<init>", "(Ljava/nio/ByteBuffer;)V");

#ifdef DEBUG_NAT_OPERATIONS
	osmand_log_print(LOG_INFO, LOG_TAG,"Native ok (init %d, native op %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#else
	osmand_log_print(LOG_INFO, "Native ok (init %d, rendering %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#endif

	/* Construct a result object */
	jobject resultObject = ienv->NewObject(resultClass, resultClassCtorId, NULL);

	return resultObject;
}
#endif

#endif


void* bitmapData = NULL;
size_t bitmapDataSize = 0;
extern "C" JNIEXPORT jobject JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering_1Indirect( JNIEnv* ienv, jobject obj,
    jobject renderingContext, jint searchResult,
    jint requestedBitmapWidth, jint requestedBitmapHeight, jint rowBytes, jboolean isTransparent,
    jboolean useEnglishNames, jobject renderingRuleSearchRequest, jint defaultColor) {

        osmand_log_print(LOG_INFO,  "Creating SkBitmap in native w:%d h:%d!", requestedBitmapWidth, requestedBitmapHeight);

        SkBitmap* bitmap = new SkBitmap();
        if(isTransparent == JNI_TRUE)
            bitmap->setConfig(SkBitmap::kARGB_8888_Config, requestedBitmapWidth, requestedBitmapHeight, rowBytes);
        else
            bitmap->setConfig(SkBitmap::kRGB_565_Config, requestedBitmapWidth, requestedBitmapHeight, rowBytes);

        if(bitmapData != NULL && bitmapDataSize != bitmap->getSize()) {
            free(bitmapData);
            bitmapData = NULL;
            bitmapDataSize = 0;
        }
        if(bitmapData == NULL && bitmapDataSize == 0) {
            bitmapDataSize = bitmap->getSize();
            bitmapData = malloc(bitmapDataSize);

            osmand_log_print(LOG_INFO,  "Allocated %d bytes at %p", bitmapDataSize, bitmapData);
        }

        bitmap->setPixels(bitmapData);


        osmand_log_print(LOG_INFO,  "Initializing rendering");
        ElapsedTimer initObjects;
        initObjects.start();

        RenderingRuleSearchRequest* req = initSearchRequest(ienv, renderingRuleSearchRequest);
        JNIRenderingContext rc;
        pullFromJavaRenderingContext(ienv, renderingContext, &rc);
        rc.setUseEnglishNames(useEnglishNames);
        ResultPublisher* result = ((ResultPublisher*) searchResult);
        //    std::vector <BaseMapDataObject* > mapDataObjects = marshalObjects(binaryMapDataObjects);

        osmand_log_print(LOG_INFO,  "Rendering image");
        initObjects.pause();
        // Main part do rendering

        SkCanvas* canvas = new SkCanvas(*bitmap);
        canvas->drawColor(defaultColor);
        if(result != NULL) {
            doRendering(result->result, canvas, req, &rc);
        }
        pushToJavaRenderingContext(ienv, renderingContext, &rc);
        osmand_log_print(LOG_INFO,  "End Rendering image");

        // delete  variables
        delete canvas;
        delete req;
        delete bitmap;
        jclass resultClass = findClass(ienv, "net/osmand/plus/render/NativeOsmandLibrary$RenderingGenerationResult");

        jmethodID resultClassCtorId = ienv->GetMethodID(resultClass, "<init>", "(Ljava/nio/ByteBuffer;)V");

#ifdef DEBUG_NAT_OPERATIONS
        osmand_log_print(LOG_INFO,  "Native ok (init %d, native op %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#else
        osmand_log_print(LOG_INFO,  "Native ok (init %d, rendering %d) ", initObjects.getElapsedTime(), rc.nativeOperations.getElapsedTime());
#endif

        // Allocate ctor paramters
        jobject bitmapBuffer = ienv->NewDirectByteBuffer(bitmapData, bitmap->getSize());

        /* Construct a result object */
        jobject resultObject = ienv->NewObject(resultClass, resultClassCtorId, bitmapBuffer);

        return resultObject;
}


///////////////////////////////////////////////
//////////  JNI Rendering Context //////////////

jclass jclass_JUnidecode;
jmethodID jmethod_JUnidecode_unidecode;
jclass jclass_RenderingContext = NULL;
jfieldID jfield_RenderingContext_interrupted = NULL;
jfieldID jfield_RenderingContext_leftX = NULL;
jfieldID jfield_RenderingContext_topY = NULL;
jfieldID jfield_RenderingContext_width = NULL;
jfieldID jfield_RenderingContext_height = NULL;
jfieldID jfield_RenderingContext_zoom = NULL;
jfieldID jfield_RenderingContext_rotate = NULL;
jfieldID jfield_RenderingContext_pointCount = NULL;
jfieldID jfield_RenderingContext_pointInsideCount = NULL;
jfieldID jfield_RenderingContext_visible = NULL;
jfieldID jfield_RenderingContext_allObjects = NULL;
jfieldID jfield_RenderingContext_density = NULL;
jfieldID jfield_RenderingContext_shadowRenderingMode = NULL;
jfieldID jfield_RenderingContext_textRenderingTime = NULL;
jfieldID jfield_RenderingContext_lastRenderedKey = NULL;

jmethodID jmethod_RenderingContext_getIconRawData = NULL;

void loadJniRenderingContext(JNIEnv* env)
{
	jclass_RenderingContext = findClass(env, "net/osmand/RenderingContext");
	jfield_RenderingContext_interrupted = getFid(env, jclass_RenderingContext, "interrupted", "Z");
	jfield_RenderingContext_leftX = getFid(env,  jclass_RenderingContext, "leftX", "F" );
	jfield_RenderingContext_topY = getFid(env,  jclass_RenderingContext, "topY", "F" );
	jfield_RenderingContext_width = getFid(env,  jclass_RenderingContext, "width", "I" );
	jfield_RenderingContext_height = getFid(env,  jclass_RenderingContext, "height", "I" );
	jfield_RenderingContext_zoom = getFid(env,  jclass_RenderingContext, "zoom", "I" );
	jfield_RenderingContext_rotate = getFid(env,  jclass_RenderingContext, "rotate", "F" );
	jfield_RenderingContext_pointCount = getFid(env,  jclass_RenderingContext, "pointCount", "I" );
	jfield_RenderingContext_pointInsideCount = getFid(env,  jclass_RenderingContext, "pointInsideCount", "I" );
	jfield_RenderingContext_visible = getFid(env,  jclass_RenderingContext, "visible", "I" );
	jfield_RenderingContext_allObjects = getFid(env,  jclass_RenderingContext, "allObjects", "I" );
	jfield_RenderingContext_density = getFid(env,  jclass_RenderingContext, "density", "F" );
	jfield_RenderingContext_shadowRenderingMode = getFid(env,  jclass_RenderingContext, "shadowRenderingMode", "I" );
	jfield_RenderingContext_textRenderingTime = getFid(env,  jclass_RenderingContext, "textRenderingTime", "I" );
	jfield_RenderingContext_lastRenderedKey = getFid(env,  jclass_RenderingContext, "lastRenderedKey", "I" );
	jmethod_RenderingContext_getIconRawData = env->GetMethodID(jclass_RenderingContext,
				"getIconRawData", "(Ljava/lang/String;)[B");


	jclass_JUnidecode = findClass(env, "net/sf/junidecode/Junidecode");
    jmethod_JUnidecode_unidecode = env->GetStaticMethodID(jclass_JUnidecode, "unidecode", "(Ljava/lang/String;)Ljava/lang/String;");
}

void pullFromJavaRenderingContext(JNIEnv* env, jobject jrc, JNIRenderingContext* rc)
{
	rc->env = env;
	rc->setLocation(env->GetFloatField( jrc, jfield_RenderingContext_leftX ), env->GetFloatField( jrc, jfield_RenderingContext_topY ));
	rc->setDimension(env->GetIntField( jrc, jfield_RenderingContext_width ), env->GetIntField( jrc, jfield_RenderingContext_height ));

	rc->setZoom(env->GetIntField( jrc, jfield_RenderingContext_zoom ));
	rc->setRotate(env->GetFloatField( jrc, jfield_RenderingContext_rotate ));
	rc->setDensityScale(env->GetFloatField( jrc, jfield_RenderingContext_density ));
	rc->setShadowRenderingMode(env->GetIntField( jrc, jfield_RenderingContext_shadowRenderingMode ));
	rc->javaRenderingContext = jrc;
}


void pushToJavaRenderingContext(JNIEnv* env, jobject jrc, JNIRenderingContext* rc)
{
	env->SetIntField( jrc, jfield_RenderingContext_pointCount, (jint) rc->pointCount);
	env->SetIntField( jrc, jfield_RenderingContext_pointInsideCount, (jint)rc->pointInsideCount);
	env->SetIntField( jrc, jfield_RenderingContext_visible, (jint)rc->visible);
	env->SetIntField( jrc, jfield_RenderingContext_allObjects, rc->allObjects);
	env->SetIntField( jrc, jfield_RenderingContext_textRenderingTime, rc->textRendering.getElapsedTime());
	env->SetIntField( jrc, jfield_RenderingContext_lastRenderedKey, rc->lastRenderedKey);
}

bool JNIRenderingContext::interrupted()
{
	return env->GetBooleanField(javaRenderingContext, jfield_RenderingContext_interrupted);
}

SkBitmap* JNIRenderingContext::getCachedBitmap(const std::string& bitmapResource) {
	JNIEnv* env = this->env;
	jstring jstr = env->NewStringUTF(bitmapResource.c_str());
	jbyteArray javaIconRawData = (jbyteArray)env->CallObjectMethod(this->javaRenderingContext, jmethod_RenderingContext_getIconRawData, jstr);
	env->DeleteLocalRef(jstr);
	if(!javaIconRawData)
		return NULL;

	jbyte* bitmapBuffer = env->GetByteArrayElements(javaIconRawData, NULL);
	jint bufferLen = env->GetArrayLength(javaIconRawData);

	// Decode bitmap
	SkBitmap* iconBitmap = new SkBitmap();
	//TODO: JPEG is badly supported! At the moment it needs sdcard to be present (sic). Patch that
	if(!SkImageDecoder::DecodeMemory(bitmapBuffer, bufferLen, iconBitmap))
	{
		// Failed to decode
		delete iconBitmap;

		this->nativeOperations.start();
		env->ReleaseByteArrayElements(javaIconRawData, bitmapBuffer, JNI_ABORT);
		env->DeleteLocalRef(javaIconRawData);
		env->DeleteLocalRef(jstr);

		throwNewException(env, (std::string("Failed to decode ") + bitmapResource).c_str());

		return NULL;
	}

	env->ReleaseByteArrayElements(javaIconRawData, bitmapBuffer, JNI_ABORT);
	env->DeleteLocalRef(javaIconRawData);

	return iconBitmap;
}

std::string JNIRenderingContext::getTranslatedString(const std::string& name) {
	if (this->isUsingEnglishNames()) {
		jstring n = this->env->NewStringUTF(name.c_str());
		std::string res = getString(this->env,
				(jstring) this->env->CallStaticObjectMethod(jclass_JUnidecode, jmethod_JUnidecode_unidecode, n));
		this->env->DeleteLocalRef(n);
		return res;
	}
	return name;
}

