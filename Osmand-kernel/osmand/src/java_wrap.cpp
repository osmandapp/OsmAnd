#ifndef _JAVA_WRAP_CPP
#define _JAVA_WRAP_CPP

#include "common.h"
#include "java_wrap.h"
#include "binaryRead.h"
#include "rendering.h"
#include <SkBitmap.h>
#include <SkCanvas.h>

extern "C" JNIEXPORT void JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_deleteSearchResult(JNIEnv* ienv,
		jobject obj, jint searchResult) {
	ResultPublisher* result = (ResultPublisher*) searchResult;
	if(result != NULL){
		// destructor will delete result
//		deleteObjects(result->result);
		delete result;
	}
}


extern "C" JNIEXPORT void JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_closeBinaryMapFile(JNIEnv* ienv,
		jobject path) {
	const char* utf = ienv->GetStringUTFChars((jstring) path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring) path, utf);
	closeBinaryMapFile(inputName);
}

extern "C" JNIEXPORT jboolean JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_initBinaryMapFile(JNIEnv* ienv,
		jobject obj, jobject path) {
	// Verify that the version of the library that we linked against is
	const char* utf = ienv->GetStringUTFChars((jstring) path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring) path, utf);
	return (initBinaryMapFile(inputName) != NULL);
}


extern "C" JNIEXPORT jint JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_searchNativeObjectsForRendering(JNIEnv* ienv,
		jobject obj, jint sleft, jint sright, jint stop, jint sbottom, jint zoom,
		jobject renderingRuleSearchRequest, bool skipDuplicates, jobject objInterrupted, jstring msgNothingFound) {
	RenderingRuleSearchRequest* req = initSearchRequest(ienv, renderingRuleSearchRequest);
	jclass clObjInterrupted = ienv->GetObjectClass(objInterrupted);
	jfieldID interruptedField = getFid(ienv, clObjInterrupted, "interrupted", "Z");
	ienv->DeleteLocalRef(clObjInterrupted);

	ResultJNIPublisher* j = new ResultJNIPublisher( objInterrupted, interruptedField, ienv);
	SearchQuery q(sleft, sright, stop, sbottom, req, j);
	q.zoom = zoom;

	ResultPublisher* res = searchObjectsForRendering(&q, req, skipDuplicates, getString(ienv, msgNothingFound));
	delete req;
	return (jint) res;
}

RenderingRuleSearchRequest* initSearchRequest(JNIEnv* env, jobject renderingRuleSearchRequest) {
	return new RenderingRuleSearchRequest(env, renderingRuleSearchRequest);
}

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
	RenderingContext rc;
	pullFromJavaRenderingContext(ienv, renderingContext, &rc);
	rc.useEnglishNames = useEnglishNames;
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

	jclass resultClass = findClass(ienv, "net/osmand/plus/render/NativeOsmandLibrary$RenderingGenerationResult");

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
        RenderingContext rc;
        pullFromJavaRenderingContext(ienv, renderingContext, &rc);
        rc.useEnglishNames = useEnglishNames;
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
