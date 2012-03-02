#ifndef _OSMAND_RENDER_RULES
#define _OSMAND_RENDER_RULES

#include <jni.h>
#include <android/log.h>
#include <iterator>
#include <string>
#include <vector>
#include <hash_map>
#include "renderRules.h"
#include "common.h"

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

int RenderingRulesStorage::getPropertiesSize() {
	return properties.size();
}

RenderingRuleProperty* RenderingRulesStorage::getProperty(int i) {
	return &properties.at(i);
}

RenderingRule* RenderingRulesStorage::getRule(int state, int itag, int ivalue) {
	std::hash_map<int, RenderingRule>::iterator it = (tagValueGlobalRules[state]).find(
			(itag << SHIFT_TAG_VAL) | ivalue);
	if (it == tagValueGlobalRules[state].end()) {
		return NULL;
	}
	return &(*it).second;
}

RenderingRuleProperty* RenderingRulesStorage::getProperty(const char* st) {
	std::hash_map<std::string, RenderingRuleProperty*>::iterator i = propertyMap.find(st);
	if (i == propertyMap.end()) {
		return NULL;
	}
	return (*i).second;
}

std::string RenderingRulesStorage::getDictionaryValue(int i) {
	if (i < 0) {
		return std::string();
	}
	return dictionary.at(i);
}

int RenderingRulesStorage::getDictionaryValue(std::string s) {
	return dictionaryMap[s];
}

void RenderingRulesStorage::initDictionary() {
	jobject listDictionary = getGlobalJniEnv()->GetObjectField(javaStorage, RenderingRulesStorageClass_dictionary);
	uint sz = getGlobalJniEnv()->CallIntMethod(listDictionary, List_size);
	uint i = 0;
	for (; i < sz; i++) {
		jstring st = (jstring) getGlobalJniEnv()->CallObjectMethod(listDictionary, List_get, i);
//			if(st != NULL)
//			{
		const char* utf = getGlobalJniEnv()->GetStringUTFChars(st, NULL);
		std::string d = std::string(utf);

		getGlobalJniEnv()->ReleaseStringUTFChars(st, utf);
		getGlobalJniEnv()->DeleteLocalRef(st);
		dictionary.push_back(d);
		dictionaryMap[d] = i;
//			}
	}
	getGlobalJniEnv()->DeleteLocalRef(listDictionary);
}

void RenderingRulesStorage::initProperties() {
	jobject props = getGlobalJniEnv()->GetObjectField(javaStorage, RenderingRulesStorage_PROPS);
	jobject listProps = getGlobalJniEnv()->GetObjectField(props, RenderingRuleStorageProperties_rules);
	uint sz = getGlobalJniEnv()->CallIntMethod(listProps, List_size);
	uint i = 0;
	for (; i < sz; i++) {
		jobject rulePrope = getGlobalJniEnv()->CallObjectMethod(listProps, List_get, i);
		bool input = (getGlobalJniEnv()->GetBooleanField(rulePrope, RenderingRuleProperty_input) == JNI_TRUE);
		int type = getGlobalJniEnv()->GetIntField(rulePrope, RenderingRuleProperty_type);
		std::string name = getStringField(rulePrope, RenderingRuleProperty_attrName);
		RenderingRuleProperty* prop = new RenderingRuleProperty(type, input, name, i);
		properties.push_back(*prop);
		propertyMap[name] = prop;
		getGlobalJniEnv()->DeleteLocalRef(rulePrope);
	}
	getGlobalJniEnv()->DeleteLocalRef(props);
	getGlobalJniEnv()->DeleteLocalRef(listProps);

}

void RenderingRulesStorage::initRules() {
	for (int i = 1; i < SIZE_STATES; i++) {
		jobjectArray rules = (jobjectArray) getGlobalJniEnv()->CallObjectMethod(javaStorage, RenderingRulesStorage_getRules,
				i);
		jsize len = getGlobalJniEnv()->GetArrayLength(rules);
		for (jsize j = 0; j < len; j++) {
			jobject rRule = getGlobalJniEnv()->GetObjectArrayElement(rules, j);
			RenderingRule* rule = createRenderingRule(rRule);
			getGlobalJniEnv()->DeleteLocalRef(rRule);
			if (rule != NULL) {

				jsize psz = rule->properties.size();
				int tag = -1;
				int value = -1;
				for (int p = 0; p < psz; p++) {
					if (rule->properties.at(p)->attrName == "tag") {
						tag = rule->intProperties.at(p);
					} else if (rule->properties.at(p)->attrName == "value") {
						value = rule->intProperties.at(p);
					}
				}
				if (tag != -1 && value != -1) {
					int key = (tag << SHIFT_TAG_VAL) + value;
					tagValueGlobalRules[i][key] = *rule;
				}
			}
		}
		getGlobalJniEnv()->DeleteLocalRef(rules);
	}
}

RenderingRule* RenderingRulesStorage::createRenderingRule(jobject rRule) {
	RenderingRule* rule = new RenderingRule;
	jobjectArray props = (jobjectArray) getGlobalJniEnv()->GetObjectField(rRule, RenderingRule_properties);
	jintArray intProps = (jintArray) getGlobalJniEnv()->GetObjectField(rRule, RenderingRule_intProperties);
	jfloatArray floatProps = (jfloatArray) getGlobalJniEnv()->GetObjectField(rRule, RenderingRule_floatProperties);
	jobject ifChildren = getGlobalJniEnv()->GetObjectField(rRule, RenderingRule_ifChildren);
	jobject ifElseChildren = getGlobalJniEnv()->GetObjectField(rRule, RenderingRule_ifElseChildren);

	jsize sz = getGlobalJniEnv()->GetArrayLength(props);

	if (floatProps != NULL) {
		jfloat* fe = getGlobalJniEnv()->GetFloatArrayElements(floatProps, NULL);
		for (int j = 0; j < sz; j++) {
			rule->floatProperties.push_back(fe[j]);
		}
		getGlobalJniEnv()->ReleaseFloatArrayElements(floatProps, fe, JNI_ABORT);
		getGlobalJniEnv()->DeleteLocalRef(floatProps);
	} else {
		rule->floatProperties.assign(sz, 0);
	}

	if (intProps != NULL) {
		jint* ie = getGlobalJniEnv()->GetIntArrayElements(intProps, NULL);
		for (int j = 0; j < sz; j++) {
			rule->intProperties.push_back(ie[j]);
		}
		getGlobalJniEnv()->ReleaseIntArrayElements(intProps, ie, JNI_ABORT);
		getGlobalJniEnv()->DeleteLocalRef(intProps);
	} else {
		rule->intProperties.assign(sz, -1);
	}

	for (jsize i = 0; i < sz; i++) {
		jobject prop = getGlobalJniEnv()->GetObjectArrayElement(props, i);
		std::string attr = getStringField(prop, RenderingRuleProperty_attrName);
		RenderingRuleProperty* p = getProperty(attr.c_str());
		rule->properties.push_back(p);
		getGlobalJniEnv()->DeleteLocalRef(prop);
	}
	getGlobalJniEnv()->DeleteLocalRef(props);

	if (ifChildren != NULL) {
		sz = getGlobalJniEnv()->CallIntMethod(ifChildren, List_size);
		for (jsize i = 0; i < sz; i++) {
			jobject o = getGlobalJniEnv()->CallObjectMethod(ifChildren, List_get, i);
			rule->ifChildren.push_back(*createRenderingRule(o));
			getGlobalJniEnv()->DeleteLocalRef(o);
		}
		getGlobalJniEnv()->DeleteLocalRef(ifChildren);
	}

	if (ifElseChildren != NULL) {
		sz = getGlobalJniEnv()->CallIntMethod(ifElseChildren, List_size);
		for (jsize i = 0; i < sz; i++) {
			jobject o = getGlobalJniEnv()->CallObjectMethod(ifElseChildren, List_get, i);
			rule->ifElseChildren.push_back(*createRenderingRule(o));
			getGlobalJniEnv()->DeleteLocalRef(o);
		}
		getGlobalJniEnv()->DeleteLocalRef(ifElseChildren);
	}

	return rule;
}

// Global object
RenderingRulesStorage* defaultStorage = NULL;

void RenderingRuleSearchRequest::initObject(jobject rrs) {
	jsize sz;
	jobjectArray oa = (jobjectArray) getGlobalJniEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_props);
	sz = getGlobalJniEnv()->GetArrayLength(oa);
	std::vector<RenderingRuleProperty*> requestProps;
	for (jsize i = 0; i < sz; i++) {
		jobject prop = getGlobalJniEnv()->GetObjectArrayElement(oa, i);
		std::string attr = getStringField(prop, RenderingRuleProperty_attrName);
		RenderingRuleProperty* p = storage->getProperty(attr.c_str());
		requestProps.push_back(p);
		getGlobalJniEnv()->DeleteLocalRef(prop);
	}
	getGlobalJniEnv()->DeleteLocalRef(oa);
	sz = storage->getPropertiesSize();
	{
		values = new int[sz];
		jintArray ia = (jintArray) getGlobalJniEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_values);
		jint* ie = getGlobalJniEnv()->GetIntArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			values[requestProps.at(i)->id] = ie[i];
		}
		getGlobalJniEnv()->ReleaseIntArrayElements(ia, ie, JNI_ABORT);
		getGlobalJniEnv()->DeleteLocalRef(ia);
	}

	{
		fvalues = new float[sz];
		jfloatArray ia = (jfloatArray) getGlobalJniEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_fvalues);
		jfloat* ie = getGlobalJniEnv()->GetFloatArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			fvalues[requestProps.at(i)->id] = ie[i];
		}
		getGlobalJniEnv()->ReleaseFloatArrayElements(ia, ie, JNI_ABORT);
		getGlobalJniEnv()->DeleteLocalRef(ia);
	}

	{
		savedValues = new int[sz];
		jintArray ia = (jintArray) getGlobalJniEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_values);
		jint* ie = getGlobalJniEnv()->GetIntArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			savedValues[requestProps.at(i)->id] = ie[i];
		}
		getGlobalJniEnv()->ReleaseIntArrayElements(ia, ie, JNI_ABORT);
		getGlobalJniEnv()->DeleteLocalRef(ia);
	}

	{
		savedFvalues = new float[sz];
		jfloatArray ia = (jfloatArray) getGlobalJniEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_fvalues);
		jfloat* ie = getGlobalJniEnv()->GetFloatArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			savedFvalues[requestProps.at(i)->id] = ie[i];
		}
		getGlobalJniEnv()->ReleaseFloatArrayElements(ia, ie, JNI_ABORT);
		getGlobalJniEnv()->DeleteLocalRef(ia);
	}

}

extern "C" JNIEXPORT void JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_initRenderingRulesStorage(JNIEnv* ienv,
		jobject obj, jobject storage) {
	setGlobalJniEnv(ienv);
	if (defaultStorage == NULL || defaultStorage->javaStorage != storage) {
		// multi thread will not work?
		if (defaultStorage != NULL) {
			delete defaultStorage;
		}
		defaultStorage = new RenderingRulesStorage(storage);
	}
}


RenderingRuleSearchRequest::RenderingRuleSearchRequest(jobject rrs) :
		renderingRuleSearch(rrs) {
	jobject storage = getGlobalJniEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_storage);
	if (defaultStorage == NULL || defaultStorage->javaStorage != storage) {
		// multi thread will not work?
		if (defaultStorage != NULL) {
			delete defaultStorage;
		}
		defaultStorage = new RenderingRulesStorage(storage);
	}
	getGlobalJniEnv()->DeleteLocalRef(storage);
	this->storage = defaultStorage;
	PROPS = new RenderingRulesStorageProperties(this->storage);
	initObject(rrs);
	clearState();
}

RenderingRuleSearchRequest::~RenderingRuleSearchRequest() {
	delete PROPS;
	delete[] fvalues;
	delete[] values;
	delete[] savedFvalues;
	delete[] savedValues;
}

int RenderingRuleSearchRequest::getIntPropertyValue(RenderingRuleProperty* prop) {
	if (prop == NULL) {
		return 0;
	}
	return values[prop->id];
}

int RenderingRuleSearchRequest::getIntPropertyValue(RenderingRuleProperty* prop, int def) {
	if (prop == NULL || values[prop->id] == -1) {
		return def;
	}
	return values[prop->id];
}

std::string RenderingRuleSearchRequest::getStringPropertyValue(RenderingRuleProperty* prop) {
	if (prop == NULL) {
		return std::string();
	}
	int s = values[prop->id];
	return storage->getDictionaryValue(s);
}

float RenderingRuleSearchRequest::getFloatPropertyValue(RenderingRuleProperty* prop) {
	if (prop == NULL) {
		return 0;
	}
	return fvalues[prop->id];
}

void RenderingRuleSearchRequest::setStringFilter(RenderingRuleProperty* p, std::string filter) {
	if (p != NULL) {
		// assert p->input;
		values[p->id] = storage->getDictionaryValue(filter);
	}
}
void RenderingRuleSearchRequest::setIntFilter(RenderingRuleProperty* p, int filter) {
	if (p != NULL) {
		// assert p->input;
		values[p->id] = filter;
	}
}

void RenderingRuleSearchRequest::clearIntvalue(RenderingRuleProperty* p) {
	if (p != NULL) {
		// assert !p->input;
		values[p->id] = -1;
	}
}

void RenderingRuleSearchRequest::setBooleanFilter(RenderingRuleProperty* p, bool filter) {
	if (p != NULL) {
		// assert p->input;
		values[p->id] = filter ? TRUE_VALUE : FALSE_VALUE;
	}
}

RenderingRulesStorageProperties* RenderingRuleSearchRequest::props() {
	return PROPS;
}

bool RenderingRuleSearchRequest::searchRule(int state) {
	return search(state, true);
}

bool RenderingRuleSearchRequest::search(int state, bool loadOutput) {
	searchResult = false;
	int tagKey = values[PROPS->R_TAG->id];
	int valueKey = values[PROPS->R_VALUE->id];
	bool result = searchInternal(state, tagKey, valueKey, loadOutput);
	if (result) {
		searchResult = true;
		return true;
	}
	result = searchInternal(state, tagKey, 0, loadOutput);
	if (result) {
		searchResult = true;
		return true;
	}
	result = searchInternal(state, 0, 0, loadOutput);
	if (result) {
		searchResult = true;
		return true;
	}
	return false;
}

bool RenderingRuleSearchRequest::searchInternal(int state, int tagKey, int valueKey, bool loadOutput) {
	values[PROPS->R_TAG->id] = tagKey;
	values[PROPS->R_VALUE->id] = valueKey;
	RenderingRule* accept = storage->getRule(state, tagKey, valueKey);
	if (accept == NULL) {
		return false;
	}
	bool match = visitRule(accept, loadOutput);
	return match;
}

bool RenderingRuleSearchRequest::visitRule(RenderingRule* rule, bool loadOutput) {
	std::vector<RenderingRuleProperty*> properties = rule->properties;
	int propLen = rule->properties.size();
	for (int i = 0; i < propLen; i++) {
		RenderingRuleProperty* rp = properties[i];
		if (rp != NULL && rp->input) {
			bool match;
			if (rp->isFloat()) {
				match = rule->floatProperties[i] == fvalues[rp->id];
			} else if (rp == PROPS->R_MINZOOM) {
				match = rule->intProperties[i] <= values[rp->id];
			} else if (rp == PROPS->R_MAXZOOM) {
				match = rule->intProperties[i] >= values[rp->id];
			} else {
				match = rule->intProperties[i] == values[rp->id];
			}
			if (!match) {
				return false;
			}
		}
	}
	if (!loadOutput) {
		return true;
	}
	// accept it
	for (int i = 0; i < propLen; i++) {
		RenderingRuleProperty* rp = properties[i];
		if (rp != NULL && !rp->input) {
			searchResult = true;
			if (rp->isFloat()) {
				fvalues[rp->id] = rule->floatProperties[i];
			} else {
				values[rp->id] = rule->intProperties[i];
			}
		}
	}
	size_t j;
	for (j = 0; j < rule->ifElseChildren.size(); j++) {
		bool match = visitRule(&rule->ifElseChildren.at(j), loadOutput);
		if (match) {
			break;
		}
	}
	for (j = 0; j < rule->ifChildren.size(); j++) {
		visitRule(&rule->ifChildren.at(j), loadOutput);
	}
	return true;

}

void RenderingRuleSearchRequest::clearState() {
	memcpy(values, savedValues, storage->getPropertiesSize() * sizeof(int));
	memcpy(fvalues, savedFvalues, storage->getPropertiesSize() * sizeof(float));
}

void RenderingRuleSearchRequest::setInitialTagValueZoom(std::string tag, std::string value, int zoom) {
	clearState();
	setIntFilter(PROPS->R_MINZOOM, zoom);
	setIntFilter(PROPS->R_MAXZOOM, zoom);
	setStringFilter(PROPS->R_TAG, tag);
	setStringFilter(PROPS->R_VALUE, value);
}

void RenderingRuleSearchRequest::setTagValueZoomLayer(std::string tag, std::string val, int zoom, int layer) {
	setIntFilter(PROPS->R_MINZOOM, zoom);
	setIntFilter(PROPS->R_MAXZOOM, zoom);
	setIntFilter(PROPS->R_LAYER, layer);
	setStringFilter(PROPS->R_TAG, tag);
	setStringFilter(PROPS->R_VALUE, val);
}

RenderingRuleSearchRequest* initSearchRequest(jobject renderingRuleSearchRequest) {
	return new RenderingRuleSearchRequest(renderingRuleSearchRequest);
}

void loadJniRenderingRules() {
	RenderingRuleClass = findClass("net/osmand/render/RenderingRule");
	RenderingRule_properties = getGlobalJniEnv()->GetFieldID(RenderingRuleClass, "properties",
			"[Lnet/osmand/render/RenderingRuleProperty;");
	RenderingRule_intProperties = getGlobalJniEnv()->GetFieldID(RenderingRuleClass, "intProperties", "[I");
	RenderingRule_floatProperties = getGlobalJniEnv()->GetFieldID(RenderingRuleClass, "floatProperties", "[F");
	RenderingRule_ifElseChildren = getGlobalJniEnv()->GetFieldID(RenderingRuleClass, "ifElseChildren", "Ljava/util/List;");
	RenderingRule_ifChildren = getGlobalJniEnv()->GetFieldID(RenderingRuleClass, "ifChildren", "Ljava/util/List;");

	RenderingRuleStoragePropertiesClass = findClass("net/osmand/render/RenderingRuleStorageProperties");
	RenderingRuleStorageProperties_rules = getGlobalJniEnv()->GetFieldID(RenderingRuleStoragePropertiesClass, "rules",
			"Ljava/util/List;");

	RenderingRulePropertyClass = findClass("net/osmand/render/RenderingRuleProperty");
	RenderingRuleProperty_type = getGlobalJniEnv()->GetFieldID(RenderingRulePropertyClass, "type", "I");
	RenderingRuleProperty_input = getGlobalJniEnv()->GetFieldID(RenderingRulePropertyClass, "input", "Z");
	RenderingRuleProperty_attrName = getGlobalJniEnv()->GetFieldID(RenderingRulePropertyClass, "attrName",
			"Ljava/lang/String;");

	RenderingRulesStorageClass = findClass("net/osmand/render/RenderingRulesStorage");
	RenderingRulesStorageClass_dictionary = getGlobalJniEnv()->GetFieldID(RenderingRulesStorageClass, "dictionary",
			"Ljava/util/List;");
	RenderingRulesStorage_PROPS = getGlobalJniEnv()->GetFieldID(RenderingRulesStorageClass, "PROPS",
			"Lnet/osmand/render/RenderingRuleStorageProperties;");
	RenderingRulesStorage_getRules = getGlobalJniEnv()->GetMethodID(RenderingRulesStorageClass, "getRules",
			"(I)[Lnet/osmand/render/RenderingRule;");

	ListClass = findClass("java/util/List");
	List_size = getGlobalJniEnv()->GetMethodID(ListClass, "size", "()I");
	List_get = getGlobalJniEnv()->GetMethodID(ListClass, "get", "(I)Ljava/lang/Object;");

	RenderingRuleSearchRequestClass = findClass("net/osmand/render/RenderingRuleSearchRequest");
	RenderingRuleSearchRequest_storage = getGlobalJniEnv()->GetFieldID(RenderingRuleSearchRequestClass, "storage",
			"Lnet/osmand/render/RenderingRulesStorage;");
	RenderingRuleSearchRequest_props = getGlobalJniEnv()->GetFieldID(RenderingRuleSearchRequestClass, "props",
			"[Lnet/osmand/render/RenderingRuleProperty;");
	RenderingRuleSearchRequest_values = getGlobalJniEnv()->GetFieldID(RenderingRuleSearchRequestClass, "values", "[I");
	RenderingRuleSearchRequest_fvalues = getGlobalJniEnv()->GetFieldID(RenderingRuleSearchRequestClass, "fvalues", "[F");
	RenderingRuleSearchRequest_savedValues = getGlobalJniEnv()->GetFieldID(RenderingRuleSearchRequestClass, "savedValues",
			"[I");
	RenderingRuleSearchRequest_savedFvalues = getGlobalJniEnv()->GetFieldID(RenderingRuleSearchRequestClass, "savedFvalues",
			"[F");

}

void unloadJniRenderRules() {
	getGlobalJniEnv()->DeleteGlobalRef(RenderingRuleSearchRequestClass);
	getGlobalJniEnv()->DeleteGlobalRef(RenderingRuleClass);
	getGlobalJniEnv()->DeleteGlobalRef(RenderingRulePropertyClass);
	getGlobalJniEnv()->DeleteGlobalRef(RenderingRuleStoragePropertiesClass);
	getGlobalJniEnv()->DeleteGlobalRef(RenderingRulesStorageClass);
	getGlobalJniEnv()->DeleteGlobalRef(ListClass);

}

#endif
