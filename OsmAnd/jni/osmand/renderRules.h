#ifndef _OSMAND_RENDER_RULES_H
#define _OSMAND_RENDER_RULES_H

#include <jni.h>
#include <string>

class RenderingRuleProperty
{
public:
	int type;
	bool input;
	std::string attrName;
	// order in
	int id;
	RenderingRuleProperty(int type, bool input, std::string& name, int id) :
			type(type), input(input), attrName(name), id(id) {
	}

	bool isFloat() {
		return type == FLOAT_TYPE;
	}


private:
	const static int INT_TYPE = 1;
	const static int FLOAT_TYPE = 2;
	const static int STRING_TYPE = 3;
	const static int COLOR_TYPE = 4;
	const static int BOOLEAN_TYPE = 5;

};

const static int TRUE_VALUE = 1;
const static int FALSE_VALUE = 0;


class RenderingRule
{
public:
	std::vector<RenderingRuleProperty*> properties;
	std::vector<int> intProperties;
	std::vector<float> floatProperties;
	std::vector<RenderingRule> ifElseChildren;
	std::vector<RenderingRule> ifChildren;

};


class RenderingRulesStorage
{
private:
	const static int SHIFT_TAG_VAL = 16;
	const static int SIZE_STATES = 7;
	std::hash_map<std::string, int> dictionaryMap;
	std::vector<std::string> dictionary;
	std::hash_map<int, RenderingRule>* tagValueGlobalRules;
	std::vector<RenderingRuleProperty> properties;
	std::hash_map<std::string,  RenderingRuleProperty*> propertyMap;


	RenderingRule* createRenderingRule(jobject rRule);
	void initDictionary();
	void initProperties();
	void initRules();

public:
    const static int POINT_RULES = 1;
    const static int LINE_RULES = 2;
    const static int POLYGON_RULES = 3;
    const static int TEXT_RULES = 4;
    const static int ORDER_RULES = 5;
	RenderingRulesStorage(jobject storage) :
			javaStorage(storage) {
		tagValueGlobalRules = new std::hash_map<int, RenderingRule >[SIZE_STATES];
		initDictionary();
		initProperties();
		initRules();
	}

	~RenderingRulesStorage() {
		delete[] tagValueGlobalRules;
		// proper
	}
	jobject javaStorage;

	int getPropertiesSize();

	RenderingRuleProperty* getProperty(int i);

	RenderingRule* getRule(int state, int itag, int ivalue);

	RenderingRuleProperty* getProperty(const char* st);

	std::string getDictionaryValue(int i);

	int getDictionaryValue(std::string s);

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


class RenderingRuleSearchRequest
{
private :
	jobject renderingRuleSearch;
	RenderingRulesStorage* storage;
	RenderingRulesStorageProperties* PROPS;
	int* values;
	float* fvalues;
	int* savedValues;
	float* savedFvalues;
	bool searchResult;

	bool searchInternal(int state, int tagKey, int valueKey, bool loadOutput);
	void initObject(jobject rrs);
	bool visitRule(RenderingRule* rule, bool loadOutput);
public:
	RenderingRuleSearchRequest(jobject rrs);

	int getIntPropertyValue(RenderingRuleProperty* prop);

	int getIntPropertyValue(RenderingRuleProperty* prop, int def);

	std::string getStringPropertyValue(RenderingRuleProperty* prop);

	float getFloatPropertyValue(RenderingRuleProperty* prop);

	void setStringFilter(RenderingRuleProperty* p, std::string filter);

	void setIntFilter(RenderingRuleProperty* p, int filter);

	void clearIntvalue(RenderingRuleProperty* p);

	void setBooleanFilter(RenderingRuleProperty* p, bool filter);

	RenderingRulesStorageProperties* props();

	bool searchRule(int state);

	bool search(int state, bool loadOutput);

	void clearState();

	void setInitialTagValueZoom(std::string tag, std::string value, int zoom);

	void setTagValueZoomLayer(std::string tag, std::string val, int zoom, int layer);

};


RenderingRuleSearchRequest* initSearchRequest(jobject renderingRuleSearchRequest);

void loadJNIRenderingRules();

void unloadJniRenderRules();

#endif
