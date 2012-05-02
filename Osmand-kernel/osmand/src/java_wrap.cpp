#ifndef _JAVA_WRAP_CPP
#define _JAVA_WRAP_CPP

#include "common.h"
#include "java_wrap.h"
#include "binaryRead.h"

extern "C" JNIEXPORT void JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_deleteSearchResult(JNIEnv* ienv,
		jobject obj, jint searchResult) {
	SearchResult* result = (SearchResult*) searchResult;
	if(result != NULL){
		deleteObjects(result->result);
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

	SearchQuery q(sleft, sright, stop, sbottom, req, objInterrupted, interruptedField, ienv);
	q.zoom = zoom;

	SearchResult* res = searchObjectsForRendering(&q, req, skipDuplicates, getString(ienv, msgNothingFound));
	delete req;
	return (jint) res;
}

#endif
