#include <jni.h>
#include <android/log.h>
#include <vector>


extern JNIEnv* env;

jclass RenderingRuleStoragePropertiesClass;
jclass RenderingRulePropertyClass;

jclass RenderingRuleSearchRequestClass;
jfieldID RenderingRuleSearchRequest_ALL;
jmethodID RenderingRuleSearchRequest_setInitialTagValueZoom;
jmethodID RenderingRuleSearchRequest_getIntPropertyValue;
jmethodID RenderingRuleSearchRequest_getFloatPropertyValue;
jmethodID RenderingRuleSearchRequest_getIntIntPropertyValue;
jmethodID RenderingRuleSearchRequest_getStringPropertyValue;
jmethodID RenderingRuleSearchRequest_setIntFilter;
jmethodID RenderingRuleSearchRequest_setStringFilter;
jmethodID RenderingRuleSearchRequest_setBooleanFilter;

jmethodID RenderingRuleSearchRequest_search;
jmethodID RenderingRuleSearchRequest_searchI;

class RenderingRuleSearchRequest
{
public :
	int getIntPropertyValue(const char* prop)
	{
		jobject all = env->GetObjectField( renderingRuleSearch, RenderingRuleSearchRequest_ALL);
		jfieldID fid = env->GetFieldID( RenderingRuleStoragePropertiesClass, prop,
				"Lnet/osmand/render/RenderingRuleProperty;");
		jobject propObj = env->GetObjectField( all, fid);
		int res = env->CallIntMethod( renderingRuleSearch, RenderingRuleSearchRequest_getIntPropertyValue, propObj);
		env->DeleteLocalRef( all);
		env->DeleteLocalRef( propObj);
		return res;
	}

	jstring getStringPropertyValue(const char* prop)
	{
		jobject all = env->GetObjectField( renderingRuleSearch, RenderingRuleSearchRequest_ALL);
		jfieldID fid = env->GetFieldID( RenderingRuleStoragePropertiesClass, prop,
				"Lnet/osmand/render/RenderingRuleProperty;");
		jobject propObj = env->GetObjectField( all, fid);
		jstring res = (jstring) env->CallObjectMethod( renderingRuleSearch, RenderingRuleSearchRequest_getStringPropertyValue, propObj);
		env->DeleteLocalRef( all);
		env->DeleteLocalRef( propObj);
		return res;
	}

	void setIntPropertyFilter(const char* prop, int filter)
	{
		jobject all = env->GetObjectField( renderingRuleSearch, RenderingRuleSearchRequest_ALL);
		jfieldID fid = env->GetFieldID( RenderingRuleStoragePropertiesClass, prop,
				"Lnet/osmand/render/RenderingRuleProperty;");
		jobject propObj = env->GetObjectField( all, fid);
		env->CallVoidMethod( renderingRuleSearch, RenderingRuleSearchRequest_setIntFilter, propObj, filter);
		env->DeleteLocalRef( all);
		env->DeleteLocalRef( propObj);
	}


	float getFloatPropertyValue(const char* prop)
	{
		jobject all = env->GetObjectField( renderingRuleSearch, RenderingRuleSearchRequest_ALL);
		jfieldID fid = env->GetFieldID( RenderingRuleStoragePropertiesClass, prop,
				"Lnet/osmand/render/RenderingRuleProperty;");
		jobject propObj = env->GetObjectField( all, fid);
		float res = env->CallFloatMethod( renderingRuleSearch, RenderingRuleSearchRequest_getFloatPropertyValue, propObj);
		env->DeleteLocalRef( all);
		env->DeleteLocalRef( propObj);
		return res;
	}


	int searchRule(int type)
	{
		return env->CallBooleanMethod(renderingRuleSearch, RenderingRuleSearchRequest_search, type);
	}

	void setInitialTagValueZoom(jstring tag, jstring value, int zoom)
	{
		env->CallVoidMethod(renderingRuleSearch, RenderingRuleSearchRequest_setInitialTagValueZoom, tag, value, zoom);
	}

	RenderingRuleSearchRequest(jobject rrs) :
			renderingRuleSearch(rrs) {
	}
private :
	jobject renderingRuleSearch;

};


jclass globalRef(jobject o)
{
	return  (jclass) env->NewGlobalRef( o);
}

RenderingRuleSearchRequest* initSearchRequest(jobject renderingRuleSearchRequest)
{
	return new RenderingRuleSearchRequest(renderingRuleSearchRequest);
}

void initRenderingRules(JNIEnv* ienv, jobject renderingRuleSearchRequest)
{
	env = ienv;
	RenderingRuleStoragePropertiesClass = globalRef(env->FindClass("net/osmand/render/RenderingRuleStorageProperties"));
	RenderingRulePropertyClass = globalRef(env->FindClass("net/osmand/render/RenderingRuleProperty"));

	RenderingRuleSearchRequestClass = globalRef(env->FindClass("net/osmand/render/RenderingRuleSearchRequest"));
	RenderingRuleSearchRequest_setInitialTagValueZoom = env->GetMethodID(RenderingRuleSearchRequestClass,
			"setInitialTagValueZoom", "(Ljava/lang/String;Ljava/lang/String;I)V");
	RenderingRuleSearchRequest_ALL = env->GetFieldID(RenderingRuleSearchRequestClass, "ALL",
			"Lnet/osmand/render/RenderingRuleStorageProperties;");
	RenderingRuleSearchRequest_getIntPropertyValue = env->GetMethodID(RenderingRuleSearchRequestClass,
			"getIntPropertyValue", "(Lnet/osmand/render/RenderingRuleProperty;)I");
	RenderingRuleSearchRequest_getIntIntPropertyValue = env->GetMethodID(RenderingRuleSearchRequestClass,
			"getIntPropertyValue", "(Lnet/osmand/render/RenderingRuleProperty;I)I");
	RenderingRuleSearchRequest_getFloatPropertyValue = env->GetMethodID(RenderingRuleSearchRequestClass,
			"getFloatPropertyValue", "(Lnet/osmand/render/RenderingRuleProperty;)F");
	RenderingRuleSearchRequest_getStringPropertyValue = env->GetMethodID(RenderingRuleSearchRequestClass,
			"getStringPropertyValue", "(Lnet/osmand/render/RenderingRuleProperty;)Ljava/lang/String;");
	RenderingRuleSearchRequest_setIntFilter = env->GetMethodID(RenderingRuleSearchRequestClass, "setIntFilter",
			"(Lnet/osmand/render/RenderingRuleProperty;I)V");
	RenderingRuleSearchRequest_setStringFilter = env->GetMethodID(RenderingRuleSearchRequestClass, "setStringFilter",
			"(Lnet/osmand/render/RenderingRuleProperty;Ljava/lang/String;)V");
	RenderingRuleSearchRequest_setBooleanFilter = env->GetMethodID(RenderingRuleSearchRequestClass, "setBooleanFilter",
			"(Lnet/osmand/render/RenderingRuleProperty;Z)V");
	RenderingRuleSearchRequest_search = env->GetMethodID(RenderingRuleSearchRequestClass, "search", "(I)Z");
	RenderingRuleSearchRequest_searchI = env->GetMethodID(RenderingRuleSearchRequestClass, "search", "(IZ)Z");

}

void unloadRenderingRules() {
	env->DeleteGlobalRef(RenderingRuleSearchRequestClass);
	env->DeleteGlobalRef(RenderingRulePropertyClass);
	env->DeleteGlobalRef(RenderingRuleStoragePropertiesClass);

}
