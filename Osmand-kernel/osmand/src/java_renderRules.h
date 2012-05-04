#ifndef _JAVA_RENDER_RULES_H
#define _JAVA_RENDER_RULES_H

#include <jni.h>
#include "java_wrap.h"
#include "common.h"
#include "renderRules.h"


jclass ListClass;
jmethodID List_size;
jmethodID List_get;

jclass RenderingRuleClass;
jfieldID RenderingRule_properties;
jfieldID RenderingRule_intProperties;
jfieldID RenderingRule_floatProperties;
jfieldID RenderingRule_ifElseChildren;
jfieldID RenderingRule_ifChildren;

jclass RenderingRuleStoragePropertiesClass;
jfieldID RenderingRuleStorageProperties_rules;

jclass RenderingRulePropertyClass;
jfieldID RenderingRuleProperty_type;
jfieldID RenderingRuleProperty_input;
jfieldID RenderingRuleProperty_attrName;

jclass RenderingRulesStorageClass;
jfieldID RenderingRulesStorageClass_dictionary;
jfieldID RenderingRulesStorage_PROPS;
jmethodID RenderingRulesStorage_getRules;

jclass RenderingRuleSearchRequestClass;
jfieldID RenderingRuleSearchRequest_storage;
jfieldID RenderingRuleSearchRequest_props;
jfieldID RenderingRuleSearchRequest_values;
jfieldID RenderingRuleSearchRequest_fvalues;
jfieldID RenderingRuleSearchRequest_savedValues;
jfieldID RenderingRuleSearchRequest_savedFvalues;

RenderingRule createRenderingRule(JNIEnv* env, jobject rRule, RenderingRulesStorage* st) {
	RenderingRule rule;
	jobjectArray props = (jobjectArray) env->GetObjectField(rRule, RenderingRule_properties);
	jintArray intProps = (jintArray) env->GetObjectField(rRule, RenderingRule_intProperties);
	jfloatArray floatProps = (jfloatArray) env->GetObjectField(rRule, RenderingRule_floatProperties);
	jobject ifChildren = env->GetObjectField(rRule, RenderingRule_ifChildren);
	jobject ifElseChildren = env->GetObjectField(rRule, RenderingRule_ifElseChildren);

	jsize sz = env->GetArrayLength(props);

	if (floatProps != NULL) {
		jfloat* fe = env->GetFloatArrayElements(floatProps, NULL);
		for (int j = 0; j < sz; j++) {
			rule.floatProperties.push_back(fe[j]);
		}
		env->ReleaseFloatArrayElements(floatProps, fe, JNI_ABORT);
		env->DeleteLocalRef(floatProps);
	} else {
		rule.floatProperties.assign(sz, 0);
	}

	if (intProps != NULL) {
		jint* ie = env->GetIntArrayElements(intProps, NULL);
		for (int j = 0; j < sz; j++) {
			rule.intProperties.push_back(ie[j]);
		}
		env->ReleaseIntArrayElements(intProps, ie, JNI_ABORT);
		env->DeleteLocalRef(intProps);
	} else {
		rule.intProperties.assign(sz, -1);
	}

	for (jsize i = 0; i < sz; i++) {
		jobject prop = env->GetObjectArrayElement(props, i);
		std::string attr = getStringField(env, prop, RenderingRuleProperty_attrName);
		RenderingRuleProperty* p = st->getProperty(attr.c_str());
		rule.properties.push_back(p);
		env->DeleteLocalRef(prop);
	}
	env->DeleteLocalRef(props);

	if (ifChildren != NULL) {
		sz = env->CallIntMethod(ifChildren, List_size);
		for (jsize i = 0; i < sz; i++) {
			jobject o = env->CallObjectMethod(ifChildren, List_get, i);
			rule.ifChildren.push_back(createRenderingRule(env, o, st));
			env->DeleteLocalRef(o);
		}
		env->DeleteLocalRef(ifChildren);
	}

	if (ifElseChildren != NULL) {
		sz = env->CallIntMethod(ifElseChildren, List_size);
		for (jsize i = 0; i < sz; i++) {
			jobject o = env->CallObjectMethod(ifElseChildren, List_get, i);
			rule.ifElseChildren.push_back(createRenderingRule(env, o, st));
			env->DeleteLocalRef(o);
		}
		env->DeleteLocalRef(ifElseChildren);
	}

	return rule;
}


void initDictionary(JNIEnv* env, RenderingRulesStorage* storage, jobject javaStorage) {
	jobject listDictionary = env->GetObjectField(javaStorage, RenderingRulesStorageClass_dictionary);
	uint sz = env->CallIntMethod(listDictionary, List_size);
	uint i = 0;
	for (; i < sz; i++) {
		jstring st = (jstring) env->CallObjectMethod(listDictionary, List_get, i);
//			if(st != NULL)
//			{
		const char* utf = env->GetStringUTFChars(st, NULL);
		std::string d = std::string(utf);

		env->ReleaseStringUTFChars(st, utf);
		env->DeleteLocalRef(st);
		storage->dictionary.push_back(d);
		storage->dictionaryMap[d] = i;
//			}
	}
	env->DeleteLocalRef(listDictionary);
}

void initProperties(JNIEnv* env, RenderingRulesStorage* st, jobject javaStorage) {
	jobject props = env->GetObjectField(javaStorage, RenderingRulesStorage_PROPS);
	jobject listProps = env->GetObjectField(props, RenderingRuleStorageProperties_rules);
	uint sz = env->CallIntMethod(listProps, List_size);
	uint i = 0;
	for (; i < sz; i++) {
		jobject rulePrope = env->CallObjectMethod(listProps, List_get, i);
		bool input = (env->GetBooleanField(rulePrope, RenderingRuleProperty_input) == JNI_TRUE);
		int type = env->GetIntField(rulePrope, RenderingRuleProperty_type);
		std::string name = getStringField(env, rulePrope, RenderingRuleProperty_attrName);
		RenderingRuleProperty* prop = new RenderingRuleProperty(type, input, name, i);
		st->properties.push_back(*prop);
		st->propertyMap[name] = prop;
		env->DeleteLocalRef(rulePrope);
	}
	env->DeleteLocalRef(props);
	env->DeleteLocalRef(listProps);

}

void initRules(JNIEnv* env, RenderingRulesStorage* st, jobject javaStorage) {
	for (int i = 1; i < st->SIZE_STATES; i++) {
		jobjectArray rules = (jobjectArray) env->CallObjectMethod(javaStorage, RenderingRulesStorage_getRules,
				i);
		jsize len = env->GetArrayLength(rules);
		for (jsize j = 0; j < len; j++) {
			jobject rRule = env->GetObjectArrayElement(rules, j);
			RenderingRule rule = createRenderingRule(env, rRule, st);
			env->DeleteLocalRef(rRule);

			jsize psz = rule.properties.size();
			int tag = -1;
			int value = -1;
			for (int p = 0; p < psz; p++) {
				if (rule.properties.at(p)->attrName == "tag") {
					tag = rule.intProperties.at(p);
				} else if (rule.properties.at(p)->attrName == "value") {
					value = rule.intProperties.at(p);
				}
			}
			if (tag != -1 && value != -1) {
				int key = (tag << st->SHIFT_TAG_VAL) + value;
				st->tagValueGlobalRules[i][key] = rule;
			}
		}
		env->DeleteLocalRef(rules);
	}
}

RenderingRulesStorage* createRenderingRulesStorage(JNIEnv* env, jobject storage) {
	RenderingRulesStorage* res = new RenderingRulesStorage(storage);
	initDictionary(env, res, storage);
	initProperties(env, res, storage);
	initRules(env, res, storage);
	return res;
}


void initRenderingRuleSearchRequest(JNIEnv* env, RenderingRuleSearchRequest* r, jobject rrs) {
	jsize sz;
	jobjectArray oa = (jobjectArray) env->GetObjectField(rrs, RenderingRuleSearchRequest_props);
	sz = env->GetArrayLength(oa);
	std::vector<RenderingRuleProperty*> requestProps;
	int* values;
	float* fvalues;
	int* savedValues;
	float* savedFvalues;

	for (jsize i = 0; i < sz; i++) {
		jobject prop = env->GetObjectArrayElement(oa, i);
		std::string attr = getStringField(env, prop, RenderingRuleProperty_attrName);
		RenderingRuleProperty* p = r->storage->getProperty(attr.c_str());
		requestProps.push_back(p);
		env->DeleteLocalRef(prop);
	}
	env->DeleteLocalRef(oa);
	sz = r->storage->getPropertiesSize();
	{
		values = new int[sz];
		jintArray ia = (jintArray) env->GetObjectField(rrs, RenderingRuleSearchRequest_values);
		jint* ie = env->GetIntArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			values[requestProps.at(i)->id] = ie[i];
		}
		env->ReleaseIntArrayElements(ia, ie, JNI_ABORT);
		env->DeleteLocalRef(ia);
	}

	{
		fvalues = new float[sz];
		jfloatArray ia = (jfloatArray) env->GetObjectField(rrs, RenderingRuleSearchRequest_fvalues);
		jfloat* ie = env->GetFloatArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			fvalues[requestProps.at(i)->id] = ie[i];
		}
		env->ReleaseFloatArrayElements(ia, ie, JNI_ABORT);
		env->DeleteLocalRef(ia);
	}

	{
		savedValues = new int[sz];
		jintArray ia = (jintArray) env->GetObjectField(rrs, RenderingRuleSearchRequest_values);
		jint* ie = env->GetIntArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			savedValues[requestProps.at(i)->id] = ie[i];
		}
		env->ReleaseIntArrayElements(ia, ie, JNI_ABORT);
		env->DeleteLocalRef(ia);
	}

	{
		savedFvalues = new float[sz];
		jfloatArray ia = (jfloatArray) env->GetObjectField(rrs, RenderingRuleSearchRequest_fvalues);
		jfloat* ie = env->GetFloatArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			savedFvalues[requestProps.at(i)->id] = ie[i];
		}
		env->ReleaseFloatArrayElements(ia, ie, JNI_ABORT);
		env->DeleteLocalRef(ia);
	}
	r->externalInitialize(values, fvalues, savedValues, savedFvalues);
}


void loadJniRenderingRules(JNIEnv* env) {
	RenderingRuleClass = findClass(env, "net/osmand/render/RenderingRule");
	RenderingRule_properties = env->GetFieldID(RenderingRuleClass, "properties",
			"[Lnet/osmand/render/RenderingRuleProperty;");
	RenderingRule_intProperties = env->GetFieldID(RenderingRuleClass, "intProperties", "[I");
	RenderingRule_floatProperties = env->GetFieldID(RenderingRuleClass, "floatProperties", "[F");
	RenderingRule_ifElseChildren = env->GetFieldID(RenderingRuleClass, "ifElseChildren", "Ljava/util/List;");
	RenderingRule_ifChildren = env->GetFieldID(RenderingRuleClass, "ifChildren", "Ljava/util/List;");

	RenderingRuleStoragePropertiesClass = findClass(env, "net/osmand/render/RenderingRuleStorageProperties");
	RenderingRuleStorageProperties_rules = env->GetFieldID(RenderingRuleStoragePropertiesClass, "rules",
			"Ljava/util/List;");

	RenderingRulePropertyClass = findClass(env, "net/osmand/render/RenderingRuleProperty");
	RenderingRuleProperty_type = env->GetFieldID(RenderingRulePropertyClass, "type", "I");
	RenderingRuleProperty_input = env->GetFieldID(RenderingRulePropertyClass, "input", "Z");
	RenderingRuleProperty_attrName = env->GetFieldID(RenderingRulePropertyClass, "attrName",
			"Ljava/lang/String;");

	RenderingRulesStorageClass = findClass(env, "net/osmand/render/RenderingRulesStorage");
	RenderingRulesStorageClass_dictionary = env->GetFieldID(RenderingRulesStorageClass, "dictionary",
			"Ljava/util/List;");
	RenderingRulesStorage_PROPS = env->GetFieldID(RenderingRulesStorageClass, "PROPS",
			"Lnet/osmand/render/RenderingRuleStorageProperties;");
	RenderingRulesStorage_getRules = env->GetMethodID(RenderingRulesStorageClass, "getRules",
			"(I)[Lnet/osmand/render/RenderingRule;");

	ListClass = findClass(env, "java/util/List");
	List_size = env->GetMethodID(ListClass, "size", "()I");
	List_get = env->GetMethodID(ListClass, "get", "(I)Ljava/lang/Object;");

	RenderingRuleSearchRequestClass = findClass(env, "net/osmand/render/RenderingRuleSearchRequest");
	RenderingRuleSearchRequest_storage = env->GetFieldID(RenderingRuleSearchRequestClass, "storage",
			"Lnet/osmand/render/RenderingRulesStorage;");
	RenderingRuleSearchRequest_props = env->GetFieldID(RenderingRuleSearchRequestClass, "props",
			"[Lnet/osmand/render/RenderingRuleProperty;");
	RenderingRuleSearchRequest_values = env->GetFieldID(RenderingRuleSearchRequestClass, "values", "[I");
	RenderingRuleSearchRequest_fvalues = env->GetFieldID(RenderingRuleSearchRequestClass, "fvalues", "[F");
	RenderingRuleSearchRequest_savedValues = env->GetFieldID(RenderingRuleSearchRequestClass, "savedValues",
			"[I");
	RenderingRuleSearchRequest_savedFvalues = env->GetFieldID(RenderingRuleSearchRequestClass, "savedFvalues",
			"[F");

}

void unloadJniRenderRules(JNIEnv* env) {
	env->DeleteGlobalRef(RenderingRuleSearchRequestClass);
	env->DeleteGlobalRef(RenderingRuleClass);
	env->DeleteGlobalRef(RenderingRulePropertyClass);
	env->DeleteGlobalRef(RenderingRuleStoragePropertiesClass);
	env->DeleteGlobalRef(RenderingRulesStorageClass);
	env->DeleteGlobalRef(ListClass);

}

#endif
