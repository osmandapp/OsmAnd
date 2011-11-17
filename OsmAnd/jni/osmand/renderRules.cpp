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
		return EMPTY_STRING;
	}
	return dictionary.at(i);
}

int RenderingRulesStorage::getDictionaryValue(std::string s) {
	return dictionaryMap[s];
}

void RenderingRulesStorage::initDictionary() {
	jobject listDictionary = globalEnv()->GetObjectField(javaStorage, RenderingRulesStorageClass_dictionary);
	uint sz = globalEnv()->CallIntMethod(listDictionary, List_size);
	uint i = 0;
	for (; i < sz; i++) {
		jstring st = (jstring) globalEnv()->CallObjectMethod(listDictionary, List_get, i);
//			if(st != NULL)
//			{
		const char* utf = globalEnv()->GetStringUTFChars(st, NULL);
		std::string d = std::string(utf);

		globalEnv()->ReleaseStringUTFChars(st, utf);
		globalEnv()->DeleteLocalRef(st);
		dictionary.push_back(d);
		dictionaryMap[d] = i;
//			}
	}
	globalEnv()->DeleteLocalRef(listDictionary);
}

void RenderingRulesStorage::initProperties() {
	jobject props = globalEnv()->GetObjectField(javaStorage, RenderingRulesStorage_PROPS);
	jobject listProps = globalEnv()->GetObjectField(props, RenderingRuleStorageProperties_rules);
	uint sz = globalEnv()->CallIntMethod(listProps, List_size);
	uint i = 0;
	for (; i < sz; i++) {
		jobject rulePrope = globalEnv()->CallObjectMethod(listProps, List_get, i);
		bool input = globalEnv()->GetIntField(rulePrope, RenderingRuleProperty_input);
		int type = globalEnv()->GetIntField(rulePrope, RenderingRuleProperty_type);
		std::string name = getStringField(rulePrope, RenderingRuleProperty_attrName);
		RenderingRuleProperty* prop = new RenderingRuleProperty(type, input, name, i);
		properties.push_back(*prop);
		propertyMap[name] = prop;
		globalEnv()->DeleteLocalRef(rulePrope);
	}
	globalEnv()->DeleteLocalRef(props);
	globalEnv()->DeleteLocalRef(listProps);

}

void RenderingRulesStorage::initRules() {
	for (int i = 1; i < SIZE_STATES; i++) {
		jobjectArray rules = (jobjectArray) globalEnv()->CallObjectMethod(javaStorage, RenderingRulesStorage_getRules,
				i);
		jsize len = globalEnv()->GetArrayLength(rules);
		for (jsize j = 0; j < len; j++) {
			jobject rRule = globalEnv()->GetObjectArrayElement(rules, j);
			RenderingRule* rule = createRenderingRule(rRule);
			globalEnv()->DeleteLocalRef(rRule);
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
		globalEnv()->DeleteLocalRef(rules);
	}
}

RenderingRule* RenderingRulesStorage::createRenderingRule(jobject rRule) {
	RenderingRule* rule = new RenderingRule;
	jobjectArray props = (jobjectArray) globalEnv()->GetObjectField(rRule, RenderingRule_properties);
	jintArray intProps = (jintArray) globalEnv()->GetObjectField(rRule, RenderingRule_intProperties);
	jfloatArray floatProps = (jfloatArray) globalEnv()->GetObjectField(rRule, RenderingRule_floatProperties);
	jobject ifChildren = globalEnv()->GetObjectField(rRule, RenderingRule_ifChildren);
	jobject ifElseChildren = globalEnv()->GetObjectField(rRule, RenderingRule_ifElseChildren);

	jsize sz = globalEnv()->GetArrayLength(props);

	if (floatProps != NULL) {
		jfloat* fe = globalEnv()->GetFloatArrayElements(floatProps, NULL);
		for (int j = 0; j < sz; j++) {
			rule->floatProperties.push_back(fe[j]);
		}
		globalEnv()->ReleaseFloatArrayElements(floatProps, fe, JNI_ABORT);
		globalEnv()->DeleteLocalRef(floatProps);
	} else {
		rule->floatProperties.assign(sz, 0);
	}

	if (intProps != NULL) {
		jint* ie = globalEnv()->GetIntArrayElements(intProps, NULL);
		for (int j = 0; j < sz; j++) {
			rule->intProperties.push_back(ie[j]);
		}
		globalEnv()->ReleaseIntArrayElements(intProps, ie, JNI_ABORT);
		globalEnv()->DeleteLocalRef(intProps);
	} else {
		rule->intProperties.assign(sz, -1);
	}

	for (jsize i = 0; i < sz; i++) {
		jobject prop = globalEnv()->GetObjectArrayElement(props, i);
		std::string attr = getStringField(prop, RenderingRuleProperty_attrName);
		RenderingRuleProperty* p = getProperty(attr.c_str());
		rule->properties.push_back(p);
		globalEnv()->DeleteLocalRef(prop);
	}
	globalEnv()->DeleteLocalRef(props);

	if (ifChildren != NULL) {
		sz = globalEnv()->CallIntMethod(ifChildren, List_size);
		for (jsize i = 0; i < sz; i++) {
			jobject o = globalEnv()->CallObjectMethod(ifChildren, List_get, i);
			rule->ifChildren.push_back(*createRenderingRule(o));
			globalEnv()->DeleteLocalRef(o);
		}
		globalEnv()->DeleteLocalRef(ifChildren);
	}

	if (ifElseChildren != NULL) {
		sz = globalEnv()->CallIntMethod(ifElseChildren, List_size);
		for (jsize i = 0; i < sz; i++) {
			jobject o = globalEnv()->CallObjectMethod(ifElseChildren, List_get, i);
			rule->ifElseChildren.push_back(*createRenderingRule(o));
			globalEnv()->DeleteLocalRef(o);
		}
		globalEnv()->DeleteLocalRef(ifElseChildren);
	}

	return rule;
}

// Global object
RenderingRulesStorage* defaultStorage = NULL;

void RenderingRuleSearchRequest::initObject(jobject rrs) {
	jsize sz;
	jobjectArray oa = (jobjectArray) globalEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_props);
	sz = globalEnv()->GetArrayLength(oa);
	std::vector<RenderingRuleProperty*> requestProps;
	for (jsize i = 0; i < sz; i++) {
		jobject prop = globalEnv()->GetObjectArrayElement(oa, i);
		std::string attr = getStringField(prop, RenderingRuleProperty_attrName);
		RenderingRuleProperty* p = storage->getProperty(attr.c_str());
		requestProps.push_back(p);
		globalEnv()->DeleteLocalRef(prop);
	}
	globalEnv()->DeleteLocalRef(oa);
	sz = storage->getPropertiesSize();
	{
		values = new int[sz];
		jintArray ia = (jintArray) globalEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_values);
		jint* ie = globalEnv()->GetIntArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			values[requestProps.at(i)->id] = ie[i];
		}
		globalEnv()->ReleaseIntArrayElements(ia, ie, JNI_ABORT);
		globalEnv()->DeleteLocalRef(ia);
	}

	{
		fvalues = new float[sz];
		jfloatArray ia = (jfloatArray) globalEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_fvalues);
		jfloat* ie = globalEnv()->GetFloatArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			fvalues[requestProps.at(i)->id] = ie[i];
		}
		globalEnv()->ReleaseFloatArrayElements(ia, ie, JNI_ABORT);
		globalEnv()->DeleteLocalRef(ia);
	}

	{
		savedValues = new int[sz];
		jintArray ia = (jintArray) globalEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_values);
		jint* ie = globalEnv()->GetIntArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			savedValues[requestProps.at(i)->id] = ie[i];
		}
		globalEnv()->ReleaseIntArrayElements(ia, ie, JNI_ABORT);
		globalEnv()->DeleteLocalRef(ia);
	}

	{
		savedFvalues = new float[sz];
		jfloatArray ia = (jfloatArray) globalEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_fvalues);
		jfloat* ie = globalEnv()->GetFloatArrayElements(ia, NULL);
		for (int i = 0; i < sz; i++) {
			savedFvalues[requestProps.at(i)->id] = ie[i];
		}
		globalEnv()->ReleaseFloatArrayElements(ia, ie, JNI_ABORT);
		globalEnv()->DeleteLocalRef(ia);
	}

}

RenderingRuleSearchRequest::RenderingRuleSearchRequest(jobject rrs) :
		renderingRuleSearch(rrs) {
	jobject storage = globalEnv()->GetObjectField(rrs, RenderingRuleSearchRequest_storage);
	if (defaultStorage == NULL || defaultStorage->javaStorage != storage) {
		// multi threadn will not work?
		if (defaultStorage != NULL) {
			delete defaultStorage;
		}
		defaultStorage = new RenderingRulesStorage(storage);
	}
	globalEnv()->DeleteLocalRef(storage);
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
		return EMPTY_STRING;
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

void loadJNIRenderingRules() {
	RenderingRuleClass = globalRef(globalEnv()->FindClass("net/osmand/render/RenderingRule"));
	RenderingRule_properties = globalEnv()->GetFieldID(RenderingRuleClass, "properties",
			"[Lnet/osmand/render/RenderingRuleProperty;");
	RenderingRule_intProperties = globalEnv()->GetFieldID(RenderingRuleClass, "intProperties", "[I");
	RenderingRule_floatProperties = globalEnv()->GetFieldID(RenderingRuleClass, "floatProperties", "[F");
	RenderingRule_ifElseChildren = globalEnv()->GetFieldID(RenderingRuleClass, "ifElseChildren", "Ljava/util/List;");
	RenderingRule_ifChildren = globalEnv()->GetFieldID(RenderingRuleClass, "ifChildren", "Ljava/util/List;");

	RenderingRuleStoragePropertiesClass = globalRef(
			globalEnv()->FindClass("net/osmand/render/RenderingRuleStorageProperties"));
	RenderingRuleStorageProperties_rules = globalEnv()->GetFieldID(RenderingRuleStoragePropertiesClass, "rules",
			"Ljava/util/List;");

	RenderingRulePropertyClass = globalRef(globalEnv()->FindClass("net/osmand/render/RenderingRuleProperty"));
	RenderingRuleProperty_type = globalEnv()->GetFieldID(RenderingRulePropertyClass, "type", "I");
	RenderingRuleProperty_input = globalEnv()->GetFieldID(RenderingRulePropertyClass, "input", "Z");
	RenderingRuleProperty_attrName = globalEnv()->GetFieldID(RenderingRulePropertyClass, "attrName",
			"Ljava/lang/String;");

	RenderingRulesStorageClass = globalRef(globalEnv()->FindClass("net/osmand/render/RenderingRulesStorage"));
	RenderingRulesStorageClass_dictionary = globalEnv()->GetFieldID(RenderingRulesStorageClass, "dictionary",
			"Ljava/util/List;");
	RenderingRulesStorage_PROPS = globalEnv()->GetFieldID(RenderingRulesStorageClass, "PROPS",
			"Lnet/osmand/render/RenderingRuleStorageProperties;");
	RenderingRulesStorage_getRules = globalEnv()->GetMethodID(RenderingRulesStorageClass, "getRules",
			"(I)[Lnet/osmand/render/RenderingRule;");

	ListClass = globalRef(globalEnv()->FindClass("java/util/List"));
	List_size = globalEnv()->GetMethodID(ListClass, "size", "()I");
	List_get = globalEnv()->GetMethodID(ListClass, "get", "(I)Ljava/lang/Object;");

	RenderingRuleSearchRequestClass = globalRef(globalEnv()->FindClass("net/osmand/render/RenderingRuleSearchRequest"));
	RenderingRuleSearchRequest_storage = globalEnv()->GetFieldID(RenderingRuleSearchRequestClass, "storage",
			"Lnet/osmand/render/RenderingRulesStorage;");
	RenderingRuleSearchRequest_props = globalEnv()->GetFieldID(RenderingRuleSearchRequestClass, "props",
			"[Lnet/osmand/render/RenderingRuleProperty;");
	RenderingRuleSearchRequest_values = globalEnv()->GetFieldID(RenderingRuleSearchRequestClass, "values", "[I");
	RenderingRuleSearchRequest_fvalues = globalEnv()->GetFieldID(RenderingRuleSearchRequestClass, "fvalues", "[F");
	RenderingRuleSearchRequest_savedValues = globalEnv()->GetFieldID(RenderingRuleSearchRequestClass, "savedValues",
			"[I");
	RenderingRuleSearchRequest_savedFvalues = globalEnv()->GetFieldID(RenderingRuleSearchRequestClass, "savedFvalues",
			"[F");

}

void unloadJniRenderRules() {
	globalEnv()->DeleteGlobalRef(RenderingRuleSearchRequestClass);
	globalEnv()->DeleteGlobalRef(RenderingRuleClass);
	globalEnv()->DeleteGlobalRef(RenderingRulePropertyClass);
	globalEnv()->DeleteGlobalRef(RenderingRuleStoragePropertiesClass);
	globalEnv()->DeleteGlobalRef(RenderingRulesStorageClass);
	globalEnv()->DeleteGlobalRef(ListClass);

}

#endif
