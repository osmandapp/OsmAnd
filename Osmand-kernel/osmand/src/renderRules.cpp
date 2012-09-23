#include <iterator>
#include <string>
#include <vector>
#include <stack>
#include <expat.h>
#include "osmand_log.h"
#include "common.h"
#include "renderRules.h"


/**
 * Parse the color string, and return the corresponding color-int.
 * Supported formats are:
 * #RRGGBB
 * #AARRGGBB
 */
int parseColor(string colorString) {
	if (colorString[0] == '#') {
		// Use a long to avoid rollovers on #ffXXXXXX
		char** end;
		long color = strtol(colorString.c_str() + 1, NULL, 16);
		if (colorString.size() == 7) {
			// Set the alpha value
			color |= 0x00000000ff000000;
		} else if (colorString.size() != 9) {
			osmand_log_print(LOG_ERROR, "Unknown color %s", colorString.c_str());
		}
		return (int) color;
	}
	osmand_log_print(LOG_ERROR, "Unknown color %s", colorString.c_str());
	return -1;
}

string colorToString(int color) {
	char c[9];
	if ((0xFF000000 & color) == 0xFF000000) {
		sprintf(c, "%x", (color & 0x00FFFFFF));
	} else {
		sprintf(c, "%x", color);
	}
	return string(c);
}


RenderingRule::RenderingRule(map<string, string>& attrs, RenderingRulesStorage* storage) {
	storage->childRules.push_back(this);
	properties.reserve(attrs.size());
	intProperties.assign(attrs.size(), -1);
	map<string, string>::iterator it = attrs.begin();
	int i = 0;
	for (; it != attrs.end(); it++) {
		RenderingRuleProperty* property = storage->PROPS.getProperty(it->first.c_str());
		if (property == NULL) {
			osmand_log_print(LOG_ERROR, "Property %s was not found in registry", it->first.c_str());
			return ;
		}
		properties.push_back(property);

		if (property->isString()) {
			intProperties[i] = storage->getDictionaryValue(it->second);
		} else if (property->isFloat()) {
			if (floatProperties.size() == 0) {
				// lazy creates
				floatProperties.assign(attrs.size(), - 1);
			}
			floatProperties[i] = property->parseFloatValue(it->second);
		} else {
			intProperties[i] = property->parseIntValue(it->second);
		}
		i++;
	}
}

string RenderingRule::getStringPropertyValue(string property, RenderingRulesStorage* storage) {
	int i = getPropertyIndex(property);
	if (i >= 0) {
		return storage->getStringValue(intProperties[i]);
	}
	return "";
}

float RenderingRule::getFloatPropertyValue(string property) {
	int i = getPropertyIndex(property);
	if (i >= 0) {
		return floatProperties[i];
	}
	return 0;
}

string RenderingRule::getColorPropertyValue(string property) {
	int i = getPropertyIndex(property);
	if (i >= 0) {
		return colorToString(intProperties[i]);
	}
	return "";
}


int RenderingRule::getIntPropertyValue(string property) {
	int i = getPropertyIndex(property);
	if (i >= 0) {
		return intProperties[i];
	}
	return -1;
}

RenderingRule* RenderingRulesStorage::getRule(int state, int itag, int ivalue) {
	UNORDERED(map)<int, RenderingRule*>::iterator it = (tagValueGlobalRules[state]).find(
			(itag << SHIFT_TAG_VAL) | ivalue);
	if (it == tagValueGlobalRules[state].end()) {
		return NULL;
	}
	return (*it).second;
}


void RenderingRulesStorage::registerGlobalRule(RenderingRule* rr, int state) {
	int tag = rr->getIntPropertyValue(this->PROPS.R_TAG->attrName);
	if (tag == -1) {
		osmand_log_print(LOG_ERROR, "Attribute tag should be specified for root filter ");
	}
	int value = rr->getIntPropertyValue(this->PROPS.R_VALUE->attrName);
	if (value == -1) {
		// attrsMap.toString()
		osmand_log_print(LOG_ERROR, "Attribute tag should be specified for root filter ");
	}
	int key = (tag << SHIFT_TAG_VAL) + value;
	RenderingRule* toInsert = rr;
	RenderingRule* previous = tagValueGlobalRules[state][key];

	if (previous != NULL) {
		// all root rules should have at least tag/value
		toInsert = createTagValueRootWrapperRule(key, previous);
		toInsert->ifElseChildren.push_back(rr);
	}
	tagValueGlobalRules[state][key] = toInsert;
}

RenderingRule* RenderingRulesStorage::createTagValueRootWrapperRule(int tagValueKey, RenderingRule* previous) {
	if (previous->properties.size() > 2) {
		map<string, string> m;
		m["tag"] = getTagString(tagValueKey);
		m["value"] = getValueString(tagValueKey);
		RenderingRule* toInsert = new RenderingRule(m, this);
		toInsert->ifElseChildren.push_back(previous);
		return toInsert;
	} else {
		return previous;
	}
}

struct GroupRules {
	RenderingRule* singleRule;
	map<string, string> groupAttributes;
	vector<RenderingRule*> children;
	vector<GroupRules> childrenGroups;

	GroupRules(RenderingRule* singleRule = NULL) : singleRule(singleRule) {
	}

	inline bool isGroup(){
		return singleRule == NULL;
	}

	void addGroupFilter(RenderingRule* rr) {
		for (vector<RenderingRule*>::iterator ch = children.begin(); ch != children.end(); ch++) {
			(*ch)->ifChildren.push_back(rr);
		}
		for (vector<GroupRules>::iterator gch = childrenGroups.begin(); gch != childrenGroups.end(); gch++) {
			gch->addGroupFilter(rr);
		}
	}

	void registerGlobalRules(RenderingRulesStorage* storage, int state) {
		for (vector<RenderingRule*>::iterator ch = children.begin(); ch != children.end(); ch++) {
			storage->registerGlobalRule(*ch, state);
		}
		for (vector<GroupRules>::iterator gch = childrenGroups.begin(); gch != childrenGroups.end(); gch++) {
			gch->registerGlobalRules(storage, state);
		}
	}
};

class RenderingRulesHandler {
	friend class RenderingRulesStorage;
	int state;
	stack<GroupRules> st;
	RenderingRulesStorageResolver* resolver;
	RenderingRulesStorage* dependsStorage;
	RenderingRulesStorage* storage;


	RenderingRulesHandler(RenderingRulesStorageResolver* resolver, RenderingRulesStorage* storage) :
			storage(storage), resolver(resolver), dependsStorage(NULL) {
	}

	RenderingRulesStorage* getDependsStorage() {
		return dependsStorage;
	}

	static map<string, string>& parseAttributes(const char **atts, map<string, string>& m,
			RenderingRulesStorage* st) {
		while (*atts != NULL) {
			string vl = string(atts[1]);
			if(vl.size() > 1 && vl[0] == '$') {
				vl = st->renderingConstants[vl.substr(1, vl.size() - 1)];
			}
			m[string(atts[0])] = string(atts[1]);
			atts += 2;
		}
		return m;
	}

	static void startElementHandler(void *data, const char *tag, const char **atts) {
		RenderingRulesHandler* t = (RenderingRulesHandler*) data;
		int len = strlen(tag);
		string name(tag);
		if (len == 6 && "filter" == name) {
			map<string, string> attrsMap;
			if (t->st.size() > 0 && t->st.top().isGroup()) {
				attrsMap.insert(t->st.top().groupAttributes.begin(), t->st.top().groupAttributes.end());
			}
			parseAttributes(atts, attrsMap, t->storage);
			RenderingRule* renderingRule = new RenderingRule(attrsMap,t->storage);
			if (t->st.size() > 0 && t->st.top().isGroup()) {
				t->st.top().children.push_back(renderingRule);
			} else if (t->st.size() > 0 && !t->st.top().isGroup()) {
				RenderingRule* parent = t->st.top().singleRule;
				t->st.top().singleRule->ifElseChildren.push_back(renderingRule);
			} else {
				t->storage->registerGlobalRule(renderingRule, t->state);

			}
			GroupRules gr(renderingRule);
			t->st.push(gr);
		} else if ("groupFilter" == name) { //$NON-NLS-1$
			map<string, string> attrsMap;
			parseAttributes(atts, attrsMap, t->storage);
			RenderingRule* renderingRule = new RenderingRule(attrsMap,t->storage);
			if (t->st.size() > 0 && t->st.top().isGroup()) {
				GroupRules parent = ((GroupRules) t->st.top());
				t->st.top().addGroupFilter(renderingRule);
			} else if (t->st.size() > 0 && !t->st.top().isGroup()) {
				t->st.top().singleRule->ifChildren.push_back(renderingRule);
			} else {
				osmand_log_print(LOG_ERROR, "Group filter without parent");
			}
			t->st.push(GroupRules(renderingRule));
		} else if ("group" == name) { //$NON-NLS-1$
			GroupRules groupRules;
			if (t->st.size() > 0 && t->st.top().isGroup()) {
				groupRules.groupAttributes.insert(t->st.top().groupAttributes.begin(), t->st.top().groupAttributes.end());
			}
			parseAttributes(atts, groupRules.groupAttributes, t->storage);
			t->st.push(groupRules);
		} else if ("order" == name) { //$NON-NLS-1$
			t->state = RenderingRulesStorage::ORDER_RULES;
		} else if ("text" == name) { //$NON-NLS-1$
			t->state = RenderingRulesStorage::TEXT_RULES;
		} else if ("point" == name) { //$NON-NLS-1$
			t->state = RenderingRulesStorage::POINT_RULES;
		} else if ("line" == name) { //$NON-NLS-1$
			t->state = RenderingRulesStorage::LINE_RULES;
		} else if ("polygon" == name) { //$NON-NLS-1$
			t->state = RenderingRulesStorage::POLYGON_RULES;
		} else if ("renderingConstant" == name) { //$NON-NLS-1$
			map<string, string> attrsMap;
			parseAttributes(atts, attrsMap, t->storage);
			t->storage->renderingConstants[attrsMap["name"]] = attrsMap["value"];
		} else if ("renderingAttribute" == name) { //$NON-NLS-1$
			map<string, string> attrsMap;
			parseAttributes(atts, attrsMap, t->storage);
			string attr = attrsMap["name"];
			map<string, string> empty;
			RenderingRule* root = new RenderingRule(empty,t->storage);
			t->storage->renderingAttributes[attr] = root;
			t->st.push(GroupRules(root));
		} else if ("renderingProperty" == name) {
			map<string, string> attrsMap;
			parseAttributes(atts, attrsMap, t->storage);
			string attr = attrsMap["attr"];
			RenderingRuleProperty* prop;
			string type = attrsMap["type"];
			if ("boolean" == type) {
				prop = RenderingRuleProperty::createInputBooleanProperty(attr);
			} else if ("string" == type) {
				prop = RenderingRuleProperty::createInputStringProperty(attr);
			} else {
				prop = RenderingRuleProperty::createInputIntProperty(attr);
			}
			prop->description = attrsMap["description"];
			prop->name = attrsMap["name"];
			string possible = attrsMap["possibleValues"];
			if (possible != "") {
				int n;
				int p = 0;
				while ((n = possible.find(',', p)) != string::npos) {
					prop->possibleValues.push_back(possible.substr(p, n));
					p = n + 1;
				}
				prop->possibleValues.push_back(possible.substr(p));
			}
			t->storage->PROPS.registerRule(prop);
		} else if ("renderingStyle" == name) {
			map<string, string> attrsMap;
			parseAttributes(atts, attrsMap, t->storage);
			string depends = attrsMap["depends"];
			if (depends.size() > 0 && t->resolver != NULL) {
				t->dependsStorage = t->resolver->resolve(depends, t->resolver);
			}
			if (t->dependsStorage != NULL) {
				// copy dictionary
				t->storage->dictionary = t->dependsStorage->dictionary;
				t->storage->dictionaryMap = t->dependsStorage->dictionaryMap;
				t->storage->PROPS.merge(t->dependsStorage->PROPS);
			} else if (depends.size() > 0) {
				osmand_log_print(LOG_ERROR, "!Dependent rendering style was not resolved : %s", depends.c_str());
			}
			//renderingName = attrsMap["name"];
		} else {
			osmand_log_print(LOG_WARN, "Unknown tag : %s", name.c_str());
		}

	}


	static void endElementHandler(void *data, const char *tag) {
		int len = strlen(tag);
		RenderingRulesHandler* t = (RenderingRulesHandler*) data;
		string name(tag);
		if ("filter" == name) { //$NON-NLS-1$
			t->st.pop();
		} else if ("group" == name) { //$NON-NLS-1$
			GroupRules group = t->st.top();
			t->st.pop();
			if (t->st.size() == 0) {
				group.registerGlobalRules(t->storage,t->state);
			} else if(t->st.top().isGroup()){
				t->st.top().childrenGroups.push_back(group);
			}
		} else if ("groupFilter" == name) { //$NON-NLS-1$
			t->st.pop();
		} else if ("renderingAttribute" == name) { //$NON-NLS-1$
			t->st.pop();
		}
	}

};


void RenderingRule::printDebugRenderingRule(string indent, RenderingRulesStorage * st) {
	indent += "   ";
	printf("\n%s", indent.c_str());
	vector<RenderingRuleProperty*>::iterator pp = properties.begin();
	for (; pp != properties.end(); pp++) {
		printf(" %s=", (*pp)->attrName.c_str());
		if ((*pp)->isString()) {
			printf("\"%s\"", getStringPropertyValue((*pp)->attrName, st).c_str());
		} else if ((*pp)->isFloat()) {
			printf("%f", getFloatPropertyValue((*pp)->attrName));
		} else if ((*pp)->isColor()) {
			printf("%s", getColorPropertyValue((*pp)->attrName).c_str());
		} else if ((*pp)->isIntParse()) {
			printf("%d", getIntPropertyValue((*pp)->attrName));
		}
	}
	vector<RenderingRule*>::iterator it = ifElseChildren.begin();
	for (; it != ifElseChildren.end(); it++) {
		(*it)->printDebugRenderingRule(indent, st);
	}
}
void RenderingRulesStorage::printDebug(int state) {
	UNORDERED(map)<int, RenderingRule*>::iterator it = tagValueGlobalRules[state].begin();
	for (; it != tagValueGlobalRules[state].end(); it++) {
		printf("\n\n%s : %s", getTagString(it->first).c_str(), getValueString(it->first).c_str());
		it->second->printDebugRenderingRule(string(""), this);
	}
}

void RenderingRulesStorage::parseRulesFromXmlInputStream(const char* filename, RenderingRulesStorageResolver* resolver) {
	XML_Parser parser = XML_ParserCreate(NULL);
	RenderingRulesHandler* handler = new RenderingRulesHandler(resolver, this);
	XML_SetUserData(parser, handler);
	XML_SetElementHandler(parser, RenderingRulesHandler::startElementHandler, RenderingRulesHandler::endElementHandler);
	FILE *file = fopen(filename, "r");
	if (file == NULL) {
		osmand_log_print(LOG_ERROR, "File can not be open %s", filename);
		XML_ParserFree(parser);
		delete handler;
		return;
	}
	char buffer[512];
	bool done = false;
	while (!done) {
		fgets(buffer, sizeof(buffer), file);
		int len = strlen(buffer);
		if (feof(file) != 0) {
			done = true;
		}
		if (XML_Parse(parser, buffer, len, done) == XML_STATUS_ERROR) {
			fclose(file);
			XML_ParserFree(parser);
			delete handler;
			return;
		}
	}

	RenderingRulesStorage* depends = handler->getDependsStorage();
	if (depends != NULL) {
		// merge results
		// dictionary and props are already merged
		map<std::string,  RenderingRule*>::iterator it = depends->renderingAttributes.begin();
		for(;it != depends->renderingAttributes.end(); it++) {
			map<std::string,  RenderingRule*>::iterator o = renderingAttributes.find(it->first);
			if (o != renderingAttributes.end()) {
				std::vector<RenderingRule*>::iterator list = it->second->ifElseChildren.begin();
				for (;list != it->second->ifElseChildren.end(); list++) {
					o->second->ifElseChildren.push_back(*list);
				}
			} else {
				renderingAttributes[it->first] = it->second;
			}
		}

		for (int i = 0; i < SIZE_STATES; i++) {
			if (depends->tagValueGlobalRules[i].empty()) {
				continue;
			}
			UNORDERED(map)<int, RenderingRule*>::iterator it = depends->tagValueGlobalRules[i].begin();
			for (; it != depends->tagValueGlobalRules[i].end(); it++) {
				UNORDERED(map)<int, RenderingRule*>::iterator o = tagValueGlobalRules[i].find(it->first);
				RenderingRule* toInsert = it->second;
				if (o != tagValueGlobalRules[i].end()) {
					toInsert = createTagValueRootWrapperRule(it->first, o->second);
					toInsert->ifElseChildren.push_back(it->second);
				}
				tagValueGlobalRules[i][it->first] = toInsert;
			}
		}

	}
	XML_ParserFree(parser);
	delete handler;
	fclose(file);
}


RenderingRuleSearchRequest::RenderingRuleSearchRequest(RenderingRulesStorage* storage)  {
	this->storage = storage;
	PROPS = &(this->storage->PROPS);
	this->values.resize(PROPS->properties.size(), 0);
	this->fvalues.resize(PROPS->properties.size(), 0);
	UNORDERED(map)<string, RenderingRuleProperty*>::iterator it = PROPS->properties.begin();
	for (; it != PROPS->properties.end(); it++) {
		if (!it->second->isColor()) {
			values[it->second->id] = -1;
		}
	}
	saveState();
}

void RenderingRuleSearchRequest::saveState() {
	this->savedFvalues = fvalues;
	this->savedValues = values;
}

RenderingRuleSearchRequest::~RenderingRuleSearchRequest() {
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

void RenderingRuleSearchRequest::externalInitialize(vector<int>& vs, vector<float>& fvs, vector<int>& sVs, vector<float>& sFvs){
	this->values = vs;
	this->fvalues = fvs;
	this->savedValues = sVs;
	this->savedFvalues = sFvs;

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

bool RenderingRuleSearchRequest::searchRenderingAttribute(string attribute) {
	searchResult = false;
	RenderingRule* rule = storage->getRenderingAttributeRule(attribute);
	if (rule == NULL) {
		return false;
	}
	searchResult = visitRule(rule, true);
	return searchResult;
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
				if (obj == NULL) {
					match = true;
				} else {
					std::string val = storage->getDictionaryValue(rule->intProperties[i]);
					int i = val.find('=');
					if (i >= 0) {
						match = obj->containsAdditional(val.substr(0, i), val.substr(i + 1));
					} else {
						match = false;
					}
				}
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
		bool match = visitRule(rule->ifElseChildren.at(j), loadOutput);
		if (match) {
			break;
		}
	}
	for (j = 0; j < rule->ifChildren.size(); j++) {
		visitRule(rule->ifChildren.at(j), loadOutput);
	}
	return true;

}

void RenderingRuleSearchRequest::clearState() {
	obj = NULL;
	values = savedValues;
	fvalues = savedFvalues;
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

bool RenderingRuleSearchRequest::isSpecified(RenderingRuleProperty* p) {
	if (p->isFloat()) {
		return fvalues[p->id] != 0;
	} else {
		int val = values[p->id];
		if (p->isColor()) {
			return val != 0;
		} else {
			return val != -1;
		}
	}
}

void RenderingRuleSearchRequest::printDebugResult() {
	if (searchResult) {
		printf("\n Found : ");
		UNORDERED(map)<string, RenderingRuleProperty*>::iterator it = PROPS->properties.begin();
		for (; it != PROPS->properties.end(); ++it) {
			RenderingRuleProperty* rp = it->second;
			if (!rp->input && isSpecified(rp)) {
				printf(" %s=", rp->attrName.c_str());
				if (rp->isString()) {
					printf("\"%s\"", getStringPropertyValue(rp).c_str());
				} else if (rp->isFloat()) {
					printf("%f", getFloatPropertyValue(rp));
				} else if (rp->isColor()) {
					printf("%s", colorToString(getIntPropertyValue(rp)).c_str());
				} else if (rp->isIntParse()) {
					printf("%d", getIntPropertyValue(rp));
				}
			}
		}
		printf("\n");
	} else {
		printf("\nNot found\n");
	}

}
