#include <jni.h>
#include <android/log.h>
#include <iterator>
#include <string>
#include <vector>
#include <hash_map>


extern JNIEnv* env;

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


class RenderingRuleProperty
{
public :
	int type;
	bool input;
	std::string attrName;
	// order in
	int id ;
	RenderingRuleProperty(int type, bool input, std::string& name, int id) :
		type(type), input(input), attrName(name), id(id)
	{}

private :
   const static int INT_TYPE = 1;
   const static int FLOAT_TYPE = 2;
   const static int STRING_TYPE = 3;
   const static int COLOR_TYPE = 4;
   const static int BOOLEAN_TYPE = 5;

};


class RenderingRule
{
public:
	std::vector<RenderingRuleProperty*> properties;
	std::vector<int> intProperties;
	std::vector<float> floatProperties;
	std::vector<RenderingRule> ifElseChildren;
	std::vector<RenderingRule> ifChildren;

};

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

class RenderingRulesStorage
{
private:
	const static int SHIFT_TAG_VAL = 16;
	std::hash_map<std::string, int> dictionaryMap;
	std::vector<std::string> dictionary;
	std::hash_map<int, RenderingRule>* tagValueGlobalRules;
	std::vector<RenderingRuleProperty> properties;
	std::hash_map<std::string,  RenderingRuleProperty*> propertyMap;

public:
	RenderingRulesStorage(jobject storage) :
			javaStorage(storage) {
		tagValueGlobalRules = new std::hash_map<int, RenderingRule >[7];
		initDictionary();
		initProperties();
		initRules();
	}

	~RenderingRulesStorage() {
		delete[] tagValueGlobalRules;
		// proper
	}
	jobject javaStorage;

	RenderingRuleProperty* getProperty(const char* st)
	{
		std::hash_map<std::string,  RenderingRuleProperty*>::iterator i = propertyMap.find(st);
		if(i == propertyMap.end())
		{
			return NULL;
		}
		return (*i).second;
	}

private:
	void initDictionary() {
		jobject listDictionary = env->GetObjectField(javaStorage, RenderingRulesStorageClass_dictionary);
		uint sz = env->CallIntMethod(listDictionary, List_size);
		uint i = 0;
		for (; i < sz; i++) {
			jstring st = (jstring) env->CallObjectMethod(listDictionary, List_get, i);
			const char* utf = env->GetStringUTFChars(st, NULL);
			env->ReleaseStringUTFChars(st, utf);
			std::string d = std::string(utf);
			env->DeleteLocalRef(st);
			dictionary.push_back(d);
			dictionaryMap[d] = i;
		}
		env->DeleteLocalRef(listDictionary);
	}

	void initProperties() {
		jobject props = env->GetObjectField(javaStorage, RenderingRulesStorage_PROPS);
		jobject listProps = env->GetObjectField(props, RenderingRuleStorageProperties_rules);
		uint sz = env->CallIntMethod(listProps, List_size);
		uint i = 0;
		for (; i < sz; i++) {
			jobject rulePrope = env->CallObjectMethod(listProps, List_get, i);
			bool input = env->GetIntField(rulePrope, RenderingRuleProperty_input);
			int type = env->GetIntField(rulePrope, RenderingRuleProperty_type);
			std::string name = getStringField(rulePrope, RenderingRuleProperty_attrName);
			RenderingRuleProperty* prop = new RenderingRuleProperty(type, input, name, i);
			properties.push_back(*prop);
			propertyMap[name] = prop;
			env->DeleteLocalRef(rulePrope);
		}
		env->DeleteLocalRef(props);
		env->DeleteLocalRef(listProps);

	}

	void initRules() {
		for (int i = 1; i <= 7; i++)
		{
			jobjectArray rules = (jobjectArray) env->CallObjectMethod(javaStorage, RenderingRulesStorage_getRules);
			jsize len = env->GetArrayLength(rules);
			for (jsize j = 0; j < len; j++) {
				jobject rRule = env->GetObjectArrayElement(rules, j);
				RenderingRule* rule = createRenderingRule(rRule);
				env->DeleteLocalRef(rRule);
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
						(tagValueGlobalRules[i])[key] = *rule;
					}
				}
				env->DeleteLocalRef(rRule);
			}
			env->DeleteLocalRef(rules);
		}
	}

	RenderingRule* createRenderingRule(jobject rRule)
	{
		RenderingRule* rule = new RenderingRule;
		jobjectArray props =  (jobjectArray)env->GetObjectField(rRule, RenderingRule_properties);
		jintArray intProps =  (jintArray)env->GetObjectField(rRule, RenderingRule_intProperties);
		jfloatArray floatProps =  (jfloatArray) env->GetObjectField(rRule, RenderingRule_floatProperties);
		jobject ifChildren =  env->GetObjectField(rRule, RenderingRule_ifChildren);
		jobject ifElseChildren =  env->GetObjectField(rRule, RenderingRule_ifElseChildren);


		jsize sz = env->GetArrayLength(props);

		jfloat* fe = env->GetFloatArrayElements(floatProps, NULL);
		rule->floatProperties.insert(fe, sz, 0);
		env->ReleaseFloatArrayElements(floatProps, fe, JNI_ABORT);

		jint* ie = env->GetIntArrayElements(intProps, NULL);
		rule->intProperties.insert(ie, sz, 0);
		env->ReleaseIntArrayElements(intProps, ie, JNI_ABORT);

		for(jsize i = 0; i<sz;i++)
		{
			jobject prop = env->GetObjectArrayElement(props, i);
			std::string attr = getStringField(prop, RenderingRuleProperty_attrName);
			RenderingRuleProperty* p =  getProperty(attr.c_str());
			rule->properties.push_back(p);
			env->DeleteLocalRef(prop);
		}

		sz = env->CallIntMethod(ifChildren, List_size);
		for(jsize i = 0; i<sz;i++)
		{
			jobject o = env->CallObjectMethod(ifChildren, List_get, i);
			rule->ifChildren.push_back(*createRenderingRule(o));
			env->DeleteLocalRef(o);
		}

		sz = env->CallIntMethod(ifElseChildren, List_size);
		for (jsize i = 0; i < sz; i++) {
			jobject o = env->CallObjectMethod(ifElseChildren, List_get, i);
			rule->ifElseChildren.push_back(*createRenderingRule(o));
			env->DeleteLocalRef(o);
		}

		env->DeleteLocalRef(props);
		env->DeleteLocalRef(intProps);
		env->DeleteLocalRef(floatProps);
		env->DeleteLocalRef(ifChildren);
		env->DeleteLocalRef(ifElseChildren);
		return rule;
	}


};

class RenderingRulesStorageProperties
{
public:
	RenderingRuleProperty* R_TEXT_LENGTH;
	RenderingRuleProperty* R_REF;
	RenderingRuleProperty* R_TEXT_SHIELD;
	RenderingRuleProperty* R_SHADOW_RADIUS;
	RenderingRuleProperty* R_SHADOW_COLOR;
	RenderingRuleProperty* R_SHADER;
	RenderingRuleProperty* R_CAP_3;
	RenderingRuleProperty* R_CAP_2;
	RenderingRuleProperty* R_CAP;
	RenderingRuleProperty* R_PATH_EFFECT_3;
	RenderingRuleProperty* R_PATH_EFFECT_2;
	RenderingRuleProperty* R_PATH_EFFECT;
	RenderingRuleProperty* R_STROKE_WIDTH_3;
	RenderingRuleProperty* R_STROKE_WIDTH_2;
	RenderingRuleProperty* R_STROKE_WIDTH;
	RenderingRuleProperty* R_COLOR_3;
	RenderingRuleProperty* R_COLOR;
	RenderingRuleProperty* R_COLOR_2;
	RenderingRuleProperty* R_TEXT_BOLD;
	RenderingRuleProperty* R_TEXT_ORDER;
	RenderingRuleProperty* R_TEXT_MIN_DISTANCE;
	RenderingRuleProperty* R_TEXT_ON_PATH;
	RenderingRuleProperty* R_ICON;
	RenderingRuleProperty* R_LAYER;
	RenderingRuleProperty* R_ORDER;
	RenderingRuleProperty* R_ORDER_TYPE;
	RenderingRuleProperty* R_TAG;
	RenderingRuleProperty* R_VALUE;
	RenderingRuleProperty* R_MINZOOM;
	RenderingRuleProperty* R_SHADOW_LEVEL;
	RenderingRuleProperty* R_MAXZOOM;
	RenderingRuleProperty* R_NIGHT_MODE;
	RenderingRuleProperty* R_TEXT_DY;
	RenderingRuleProperty* R_TEXT_SIZE;
	RenderingRuleProperty* R_TEXT_COLOR;
	RenderingRuleProperty* R_TEXT_HALO_RADIUS;
	RenderingRuleProperty* R_TEXT_WRAP_WIDTH;

	RenderingRulesStorageProperties(RenderingRulesStorage* storage)
	{
		R_TEXT_LENGTH = storage->getProperty("textLength");
		R_REF = storage->getProperty("ref");
		R_TEXT_SHIELD = storage->getProperty("textShield");
		R_SHADOW_RADIUS = storage->getProperty("shadowRadius");
		R_SHADOW_COLOR = storage->getProperty("shadowColor");
		R_SHADER = storage->getProperty("shader");
		R_CAP_3 = storage->getProperty("cap_3");
		R_CAP_2 = storage->getProperty("cap_2");
		R_CAP = storage->getProperty("cap");
		R_PATH_EFFECT_3 = storage->getProperty("pathEffect_3");
		R_PATH_EFFECT_2 = storage->getProperty("pathEffect_2");
		R_PATH_EFFECT = storage->getProperty("pathEffect");
		R_STROKE_WIDTH_3 = storage->getProperty("strokeWidth_3");
		R_STROKE_WIDTH_2 = storage->getProperty("strokeWidth_2");
		R_STROKE_WIDTH = storage->getProperty("strokeWidth");
		R_COLOR_3 = storage->getProperty("color_3");
		R_COLOR = storage->getProperty("color");
		R_COLOR_2 = storage->getProperty("color_2");
		R_TEXT_BOLD = storage->getProperty("textBold");
		R_TEXT_ORDER = storage->getProperty("textOrder");
		R_TEXT_MIN_DISTANCE = storage->getProperty("textMinDistance");
		R_TEXT_ON_PATH = storage->getProperty("textOnPath");
		R_ICON = storage->getProperty("icon");
		R_LAYER = storage->getProperty("layer");
		R_ORDER = storage->getProperty("order");
		R_ORDER_TYPE = storage->getProperty("orderType");
		R_TAG = storage->getProperty("tag");
		R_VALUE = storage->getProperty("value");
		R_MINZOOM = storage->getProperty("minzoom");
		R_MAXZOOM = storage->getProperty("maxzoom");
		R_NIGHT_MODE = storage->getProperty("nightMode");
		R_TEXT_DY = storage->getProperty("textDy");
		R_TEXT_SIZE = storage->getProperty("textSize");
		R_TEXT_COLOR = storage->getProperty("textColor");
		R_TEXT_HALO_RADIUS = storage->getProperty("textHaloRadius");
		R_TEXT_WRAP_WIDTH = storage->getProperty("textWrapWidth");
		R_SHADOW_LEVEL = storage->getProperty("shadowLevel");

	}

};



// Global object
RenderingRulesStorage* defaultStorage = NULL;

class RenderingRuleSearchRequest
{
private :
	jobject renderingRuleSearch;
	RenderingRulesStorage* storage;
	RenderingRulesStorageProperties* PROPS;
	std::vector<RenderingRuleProperty*> requestProps;
	std::vector<int> values;
	std::vector<float> fvalues;
	std::vector<int> savedValues;
	std::vector<float> savedFvalues;
	bool searchResult = false;

	void initObject(jobject rrs)
	{
		jsize sz;
		jobjectArray oa = (jobjectArray ) env->GetObjectField(rrs, RenderingRuleSearchRequest_props);
		sz = env->GetArrayLength(oa);
		for(jsize i=0; i<sz; i++)
		{
			jobject prop = env->GetObjectArrayElement(oa, i);
			std::string attr = getStringField(prop, RenderingRuleProperty_attrName);
			RenderingRuleProperty* p =  storage -> getProperty(attr.c_str());
			requestProps.push_back(p);
			env->DeleteLocalRef(prop);
		}
		env->DeleteLocalRef(oa);
		{
			jintArray ia = (jintArray) env->GetObjectField(rrs, RenderingRuleSearchRequest_values);
			jint* ie = env->GetIntArrayElements(ia, NULL);
			sz = env->GetArrayLength(ia);
			values.insert(ie, sz, 0);
			env->ReleaseIntArrayElements(ia, ie, JNI_ABORT);
			env->DeleteLocalRef(ia);
		}

		{
			jfloatArray fa = (jfloatArray) env->GetObjectField(rrs, RenderingRuleSearchRequest_fvalues);
			jfloat* fe = env->GetFloatArrayElements(fa, NULL);
			sz = env->GetArrayLength(fa);
			fvalues.insert(fe, sz, 0);
			env->ReleaseFloatArrayElements(fa, fe, JNI_ABORT);
			env->DeleteLocalRef(fa);
		}
		{
			jintArray ia = (jintArray) env->GetObjectField(rrs, RenderingRuleSearchRequest_savedValues);
			jint* ie = env->GetIntArrayElements(ia, NULL);
			sz = env->GetArrayLength(ia);
			savedValues.insert(ie, sz, 0);
			env->ReleaseIntArrayElements(ia, ie, JNI_ABORT);
			env->DeleteLocalRef(ia);
		}

		{
			jfloatArray fa = (jfloatArray) env->GetObjectField(rrs, RenderingRuleSearchRequest_savedFvalues);
			jfloat* fe = env->GetFloatArrayElements(fa, NULL);
			sz = env->GetArrayLength(fa);
			savedFvalues.insert(fe, sz, 0);
			env->ReleaseFloatArrayElements(fa, fe, JNI_ABORT);
			env->DeleteLocalRef(fa);
		}

	}

public:
	RenderingRuleSearchRequest(jobject rrs) : renderingRuleSearch(rrs) {
		jobject storage = env->GetObjectField(rrs, RenderingRuleSearchRequest_storage);
		if(defaultStorage == NULL || defaultStorage->javaStorage != storage){
			// multi threadn will not work?
			if(defaultStorage != NULL){
				delete defaultStorage;
			}
			defaultStorage = new RenderingRulesStorage(storage);
		}
		env->DeleteLocalRef(storage);
		this->storage = defaultStorage;
		PROPS = new RenderingRulesStorageProperties(this->storage);
		initObject(rrs);
	}

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

	RenderingRulesStorageProperties* props(){
		return PROPS;
	}

	int searchRule(int type)
	{
		return env->CallBooleanMethod(renderingRuleSearch, RenderingRuleSearchRequest_search, type);
	}

	void setInitialTagValueZoom(jstring tag, jstring value, int zoom)
	{
		env->CallVoidMethod(renderingRuleSearch, RenderingRuleSearchRequest_setInitialTagValueZoom, tag, value, zoom);
	}


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
	RenderingRuleClass = globalRef(env->FindClass("net/osmand/render/RenderingRule"));
	RenderingRule_properties = env->GetFieldID(RenderingRuleClass, "properties", "[Lnet/osmand/render/RenderingRuleProperty;");
	RenderingRule_intProperties = env->GetFieldID(RenderingRuleClass, "intProperties", "[I");
	RenderingRule_floatProperties = env->GetFieldID(RenderingRuleClass, "floatProperties", "[I");
	RenderingRule_ifElseChildren = env->GetFieldID(RenderingRuleClass, "ifElseChildren", "Ljava/util/List;");
	RenderingRule_ifChildren = env->GetFieldID(RenderingRuleClass, "ifChildren", "Ljava/util/List;");

	RenderingRuleStoragePropertiesClass = globalRef(env->FindClass("net/osmand/render/RenderingRuleStorageProperties"));
	RenderingRuleStorageProperties_rules = env->GetFieldID(RenderingRuleStoragePropertiesClass, "rules", "Ljava/util/List;");

	RenderingRulePropertyClass = globalRef(env->FindClass("net/osmand/render/RenderingRuleProperty"));
	RenderingRuleProperty_type = env->GetFieldID(RenderingRulePropertyClass, "type", "I");
	RenderingRuleProperty_input = env->GetFieldID(RenderingRulePropertyClass, "input", "Z");
	RenderingRuleProperty_attrName = env->GetFieldID(RenderingRulePropertyClass, "attrName", "Ljava/lang/String;");

	RenderingRulesStorageClass = globalRef(env->FindClass("net/osmand/render/RenderingRulesStorage"));
	RenderingRulesStorageClass_dictionary = env->GetFieldID(RenderingRulesStorageClass, "dictionary", "Ljava/util/List;");
	RenderingRulesStorage_PROPS = env->GetFieldID(RenderingRulesStorageClass, "PROPS",
			"Lnet/osmand/render/RenderingRuleStorageProperties;");
	RenderingRulesStorage_getRules = env->GetMethodID(RenderingRulesStorageClass, "getRules",
			"(I)[Lnet/osmand/render/RenderingRule;");

	ListClass = globalRef(env->FindClass("java/util/List"));
	List_size = env->GetMethodID(ListClass, "size", "()I");
	List_get = env->GetMethodID(ListClass, "get", "(I)Ljava/lang/Object;");

	RenderingRuleSearchRequestClass = globalRef(env->FindClass("net/osmand/render/RenderingRuleSearchRequest"));
	RenderingRuleSearchRequest_setInitialTagValueZoom = env->GetMethodID(RenderingRuleSearchRequestClass,
			"setInitialTagValueZoom", "(Ljava/lang/String;Ljava/lang/String;I)V");
	RenderingRuleSearchRequest_storage = env->GetFieldID(RenderingRuleSearchRequestClass,
				"storage", "Lnet/osmand/render/RenderingRulesStorage;");
	RenderingRuleSearchRequest_props = env->GetFieldID(RenderingRuleSearchRequestClass,
					"props", "[Lnet/osmand/render/RenderingRuleProperty;");
	RenderingRuleSearchRequest_values = env->GetFieldID(RenderingRuleSearchRequestClass, "values", "[I");
	RenderingRuleSearchRequest_fvalues = env->GetFieldID(RenderingRuleSearchRequestClass, "fvalues", "[F");
	RenderingRuleSearchRequest_savedValues = env->GetFieldID(RenderingRuleSearchRequestClass, "savedValues", "[I");
	RenderingRuleSearchRequest_savedFvalues = env->GetFieldID(RenderingRuleSearchRequestClass, "savedFvalues", "[F");

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
	env->DeleteGlobalRef(RenderingRuleClass);
	env->DeleteGlobalRef(RenderingRulePropertyClass);
	env->DeleteGlobalRef(RenderingRuleStoragePropertiesClass);
	env->DeleteGlobalRef(RenderingRulesStorageClass);
	env->DeleteGlobalRef(ListClass);

}



