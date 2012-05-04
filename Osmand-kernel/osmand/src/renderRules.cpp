#ifndef _OSMAND_RENDER_RULES
#define _OSMAND_RENDER_RULES

#include "osmand_log.h"
#include <iterator>
#include <string>
#include <vector>
#include "common.h"
#include "renderRules.h"



int RenderingRulesStorage::getPropertiesSize() {
	return properties.size();
}

RenderingRuleProperty* RenderingRulesStorage::getProperty(int i) {
	return &properties.at(i);
}

RenderingRule* RenderingRulesStorage::getRule(int state, int itag, int ivalue) {
	HMAP::hash_map<int, RenderingRule>::iterator it = (tagValueGlobalRules[state]).find(
			(itag << SHIFT_TAG_VAL) | ivalue);
	if (it == tagValueGlobalRules[state].end()) {
		return NULL;
	}
	return &(*it).second;
}

RenderingRuleProperty* RenderingRulesStorage::getProperty(const char* st) {
	HMAP::hash_map<std::string, RenderingRuleProperty*>::iterator i = propertyMap.find(st);
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


RenderingRuleSearchRequest::RenderingRuleSearchRequest(RenderingRulesStorage* storage)  {
	this->storage = storage;
	PROPS = new RenderingRulesStorageProperties(this->storage);
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

void RenderingRuleSearchRequest::externalInitialize(int* vs, float* fvs, int* sVs, float* sFvs){
	this->values = vs;
	this->fvalues = fvs;
	this->savedFvalues = sFvs;
	this->savedValues = sVs;

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
			} else if (rp == PROPS->R_ADDITIONAL) {
				if(obj == NULL){
					return true;
				}
				std::string val = storage->getDictionaryValue(rule->intProperties[i]);
				int i = val.find('=');
				if(i >= 0) {
					return obj->containsAdditional(val.substr(0, i), val.substr(i+1));
				}
				return false;
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
	obj = NULL;
	memcpy(values, savedValues, storage->getPropertiesSize() * sizeof(int));
	memcpy(fvalues, savedFvalues, storage->getPropertiesSize() * sizeof(float));
}

void RenderingRuleSearchRequest::setInitialTagValueZoom(std::string tag, std::string value, int zoom, MapDataObject* obj) {
	clearState();
	this->obj = obj;
	setIntFilter(PROPS->R_MINZOOM, zoom);
	setIntFilter(PROPS->R_MAXZOOM, zoom);
	setStringFilter(PROPS->R_TAG, tag);
	setStringFilter(PROPS->R_VALUE, value);
}

void RenderingRuleSearchRequest::setTagValueZoomLayer(std::string tag, std::string val, int zoom, int layer, MapDataObject* obj) {
	this->obj = obj;
	setIntFilter(PROPS->R_MINZOOM, zoom);
	setIntFilter(PROPS->R_MAXZOOM, zoom);
	setIntFilter(PROPS->R_LAYER, layer);
	setStringFilter(PROPS->R_TAG, tag);
	setStringFilter(PROPS->R_VALUE, val);
}


#endif
