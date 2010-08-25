#include <string.h>
#include <jni.h>


jstring
Java_net_osmand_NativeLibrary_stringFromJNI( JNIEnv* env,
                                                  jobject thiz )
{
    return (*env)->NewStringUTF(env, "Hello from JNI !");
}
