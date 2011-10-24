#ifndef _OSMAND_COMMON
#define _OSMAND_COMMON

#include <jni.h>
#include <string>
std::string EMPTY_STRING;
JNIEnv* env;

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

std::string getStringMethod(jobject o, jmethodID fid)
{
	jstring st = (jstring) env->CallObjectMethod(o, fid);
	if (st == NULL) {
		return EMPTY_STRING;
	}
	const char* utf = env->GetStringUTFChars(st, NULL);
	std::string res(utf);
	env->ReleaseStringUTFChars(st, utf);
	env->DeleteLocalRef(st);
	return res;
}

std::string getStringMethod(jobject o, jmethodID fid, int i)
{
	jstring st = (jstring) env->CallObjectMethod(o, fid, i);
	if (st == NULL) {
		return EMPTY_STRING;
	}
	const char* utf = env->GetStringUTFChars(st, NULL);
	std::string res(utf);
	env->ReleaseStringUTFChars(st, utf);
	env->DeleteLocalRef(st);
	return res;
}


#endif
