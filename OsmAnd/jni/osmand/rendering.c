
#include <jni.h>
#include <android/log.h>
#include <time.h>

#include <stdio.h>
#include <stdlib.h>


JNIEXPORT jstring JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_generateRendering( JNIEnv* env, 
		jobject obj, jobject renderingContext, jobjectArray binaryMapDataObjects, jobject bmp,
		jboolean useEnglishNames, jobject renderingRuleSearchRequest) {
	
	__android_log_print(ANDROID_LOG_INFO, "net.osmand", "Rendering cpp");
	// malloc room for the resulting string  
    // szResult = malloc(sizeof(szFormat) + 20);  
  
  
    // get an object string  
    jstring result = (*env)->NewStringUTF(env, "Hello android");  
  
    // cleanup  
    // free(szResult);  
  
	return result;
}
