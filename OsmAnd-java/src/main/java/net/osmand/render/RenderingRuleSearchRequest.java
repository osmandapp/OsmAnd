package net.osmand.render;

import static net.osmand.render.RenderingRuleProperty.BOOLEAN_TYPE;
import static net.osmand.render.RenderingRuleProperty.COLOR_TYPE;
import static net.osmand.render.RenderingRuleProperty.FLOAT_TYPE;
import static net.osmand.render.RenderingRuleProperty.INT_TYPE;
import static net.osmand.render.RenderingRuleProperty.STRING_TYPE;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class RenderingRuleSearchRequest {

	public final RenderingRuleStorageProperties ALL;

	private BinaryMapDataObject object;
	private final RenderingRuleProperty[] props;
	private final RenderingRulesStorage storage;

	private final int[] values;
	private final float[] fvalues;

	private int[] savedValues;
	private float[] savedFvalues;

	private boolean searchResult = false;
	private Map<String, String> classProperties = new HashMap<>();

	public RenderingRuleSearchRequest(RenderingRulesStorage storage) {
		this.storage = storage;
		this.ALL = storage.PROPS;
		props = storage.PROPS.getPoperties();
		values = new int[props.length];
		for (int i = 0; i < props.length; i++) {
			if (!props[i].isColor()) {
				values[i] = -1;
			}
		}
		fvalues = new float[props.length];
		setBooleanFilter(storage.PROPS.R_TEST, true);
		saveState();
	}

	public RenderingRuleSearchRequest(RenderingRuleSearchRequest searchRequest) {
		this.storage = searchRequest.storage;
		this.props = searchRequest.props;
		this.values = new int[searchRequest.values.length];
		this.fvalues = new float[searchRequest.fvalues.length];
		this.object = searchRequest.object;
		this.searchResult = searchRequest.searchResult;
		this.ALL = searchRequest.ALL;
		System.arraycopy(searchRequest.values, 0, values, 0, searchRequest.values.length);
		System.arraycopy(searchRequest.fvalues, 0, fvalues, 0, searchRequest.fvalues.length);
		saveState();
	}

	public static RenderingRuleSearchRequest initWithCustomProperties(RenderingRulesStorage renderingRules, int zoom,
	                                                                  Map<String, String> customProperties) {
		RenderingRuleSearchRequest searchRequest = new RenderingRuleSearchRequest(renderingRules);

		for (RenderingRuleProperty customProp : renderingRules.PROPS.getCustomRules()) {
			String custom = customProperties != null ? customProperties.get(customProp.getAttrName()) : null;
			if (customProp.isBoolean()) {
				searchRequest.setBooleanFilter(customProp, custom != null ? "true".equals(custom) : false);
			} else {
				searchRequest.setStringFilter(customProp, custom != null ? custom : "");
			}
		}

		if (zoom > 0) {
			searchRequest.setIntFilter(renderingRules.PROPS.R_MINZOOM, zoom);
			searchRequest.setIntFilter(renderingRules.PROPS.R_MAXZOOM, zoom);
		}

		searchRequest.saveState();

		return searchRequest;
	}

	RenderingRulesStorage getStorage() {
		return storage;
	}

	public BinaryMapDataObject getObject() {
		return object;
	}

	public void setStringFilter(RenderingRuleProperty p, String filter) {
		assert p.isInputProperty();
		values[p.getId()] = storage.getDictionaryValue(filter);
	}

	public void setIntFilter(RenderingRuleProperty p, int filter) {
		assert p.isInputProperty();
		values[p.getId()] = filter;
	}
	
	public void setBooleanFilter(RenderingRuleProperty p, boolean filter) {
		assert p.isInputProperty();
		values[p.getId()] = filter ? RenderingRuleProperty.TRUE_VALUE : RenderingRuleProperty.FALSE_VALUE;
	}

	public void setFloatFilter(RenderingRuleProperty p, float filter) {
		assert p.isInputProperty();
		fvalues[p.getId()] = filter;
	}

	public void saveState() {
		savedValues = new int[values.length];
		savedFvalues = new float[fvalues.length];
		System.arraycopy(values, 0, savedValues, 0, values.length);
		System.arraycopy(fvalues, 0, savedFvalues, 0, fvalues.length);
	}

	public void clearState() {
		System.arraycopy(savedValues, 0, values, 0, values.length);
		System.arraycopy(savedFvalues, 0, fvalues, 0, fvalues.length);
		object = null;
	}
	
	public void clearValue(RenderingRuleProperty p) {
		if (!p.isIntParse()) {
			fvalues[p.getId()] = savedFvalues[p.getId()];
		}
		values[p.getId()] = savedValues[p.getId()];
	}

	public void setInitialTagValueZoom(String tag, String val, int zoom, BinaryMapDataObject obj) {
		clearState();
		object = obj;
		setIntFilter(ALL.R_MINZOOM, zoom);
		setIntFilter(ALL.R_MAXZOOM, zoom);
		setStringFilter(ALL.R_TAG, tag);
		setStringFilter(ALL.R_VALUE, val);
	}

	public void setTagValueZoomLayer(String tag, String val, int zoom, int layer, BinaryMapDataObject obj) {
		object = obj;
		setIntFilter(ALL.R_MINZOOM, zoom);
		setIntFilter(ALL.R_MAXZOOM, zoom);
		setIntFilter(ALL.R_LAYER, layer);
		setStringFilter(ALL.R_TAG, tag);
		setStringFilter(ALL.R_VALUE, val);
	}
	
	public boolean isFound() {
		return searchResult;
	}
	
	public boolean searchRenderingAttribute(String attribute) {
		searchResult = false;
		RenderingRule rule = storage.getRenderingAttributeRule(attribute);
		if (rule == null) {
			return false;
		}
		searchResult = visitRule(rule, true);
		return searchResult;
	}

	public boolean search(int state) {
		return search(state, true);
	}
	
	public boolean search(int state, boolean loadOutput) {
		searchResult = false;
		int tagKey = values[storage.PROPS.R_TAG.getId()];
		int valueKey = values[storage.PROPS.R_VALUE.getId()];
		boolean result = searchInternal(state, tagKey, valueKey, loadOutput);
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

	private boolean searchInternal(int state, int tagKey, int valueKey, boolean loadOutput) {
		values[storage.PROPS.R_TAG.getId()] = tagKey;
		values[storage.PROPS.R_VALUE.getId()] = valueKey;
		values[storage.PROPS.R_DISABLE.getId()] = 0;
		RenderingRule accept = storage.getRule(state, tagKey, valueKey);
		if (accept == null) {
			return false;
		}
		boolean match = visitRule(accept, loadOutput);
		if (match && values[storage.PROPS.R_DISABLE.getId()] != 0) {
			return false;
		}
		return match;
	}

	private boolean visitRule(RenderingRule rule, boolean loadOutput) {
		boolean input = checkInputProperties(rule);
		if (!input) {
			return false;
		}
		if (!loadOutput && !rule.isGroup()) {
			return true;
		}
		// accept it
		if (!rule.isGroup()) {
			loadOutputProperties(rule, true);
		}
		boolean match = false;
		for (RenderingRule rr : rule.getIfElseChildren()) {
			match = visitRule(rr, loadOutput);
			if (match) {
				break;
			}
		}
		boolean fit = (match || !rule.isGroup());
		if (fit && loadOutput) {
			if (rule.isGroup()) {
				loadOutputProperties(rule, false);
			}

			for (RenderingRule rr : rule.getIfChildren()) {
				visitRule(rr, loadOutput);
			}
		}
		return fit;
	}
	
	public void loadOutputProperties(RenderingRule rule, boolean override) {
		RenderingRuleProperty[] properties = rule.getProperties();
		for (int i = 0; i < properties.length; i++) {
			RenderingRuleProperty rp = properties[i];
			if (rp.isOutputProperty()) {
				if (!isSpecified(rp) || override) {
					RenderingRule rr = rule.getAttrProp(i);
					if (rr != null) {
						visitRule(rr, true);
						if (isSpecified(storage.PROPS.R_ATTR_COLOR_VALUE)) {
							values[rp.getId()] = getIntPropertyValue(storage.PROPS.R_ATTR_COLOR_VALUE);
						} else if (isSpecified(storage.PROPS.R_ATTR_INT_VALUE)) {
							values[rp.getId()] = getIntPropertyValue(storage.PROPS.R_ATTR_INT_VALUE);
							fvalues[rp.getId()] = getFloatPropertyValue(storage.PROPS.R_ATTR_INT_VALUE);
						} else if (isSpecified(storage.PROPS.R_ATTR_BOOL_VALUE)) {
							values[rp.getId()] = getIntPropertyValue(storage.PROPS.R_ATTR_BOOL_VALUE);
						}
					} else if (rp.isFloat()) {
						fvalues[rp.getId()] = rule.getFloatProp(i);
						values[rp.getId()] = rule.getIntProp(i);
					} else {
						values[rp.getId()] = rule.getIntProp(i);
					}
				}
			}
		}
	}

	protected boolean checkInputProperties(RenderingRule rule) {
		RenderingRuleProperty[] properties = rule.getProperties();
		for (int i = 0; i < properties.length; i++) {
			RenderingRuleProperty rp = properties[i];
			if (rp.isInputProperty()) {
				boolean match;
				if (rp.isFloat()) {
					match = rp.accept(rule.getFloatProp(i), fvalues[rp.getId()], this);
				} else {
					match = rp.accept(rule.getIntProp(i), values[rp.getId()], this);
				}
				if (!match) {
					return false;
				}
			} else if (rp == storage.PROPS.R_DISABLE) {
				// quick disable return even without load output
				values[rp.getId()] = rule.getIntProp(i);
			}
		}
		return true;
	}

	public boolean isSpecified(RenderingRuleProperty property) {
		if (property.isFloat()) {
			return fvalues[property.getId()] != 0 || values[property.getId()] != -1;
		} else {
			int val = values[property.getId()];
			if (property.isColor()) {
				return val != 0;
			} else {
				return val != -1;
			}
		}
	}
	
	public RenderingRuleProperty[] getProperties() {
		return props;
	}
	
	public String getStringPropertyValue(RenderingRuleProperty property) {
		int val = values[property.getId()];
		if (val < 0) {
			return null;
		}
		return storage.getStringValue(val);
	}
	
	public float getFloatPropertyValue(RenderingRuleProperty property) {
		return fvalues[property.getId()];
	}
	
	public float getFloatPropertyValue(RenderingRuleProperty property, float defVal) {
		float f = fvalues[property.getId()];
		if (f == 0) {
			return defVal;
		}
		return f;
	}
	
	public String getColorStringPropertyValue(RenderingRuleProperty property) {
		return Algorithms.colorToString(values[property.getId()]);
	}
	
	public int getIntPropertyValue(RenderingRuleProperty property) {
		return values[property.getId()];
	}
	
	public boolean getBoolPropertyValue(RenderingRuleProperty property) {
		int val = values[property.getId()];
		return val != -1 && val != 0;
	}
	
	public int getIntPropertyValue(RenderingRuleProperty property, int defValue) {
		int val = values[property.getId()];
		return val == -1 ? defValue : val;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("object ").append(object).append("\n");

		for (int i = 0; i < props.length; i++) {
			RenderingRuleProperty property = props[i];
			if (isSpecified(property)) {
				builder.append(i).append(" ").append(property.getAttrName()).append(" ");

				if (property.type == INT_TYPE) {
					builder.append(getIntPropertyValue(property));
				} else if (property.type == FLOAT_TYPE) {
					builder.append(getFloatPropertyValue(property));
				} else if (property.type == STRING_TYPE) {
					builder.append(getStringPropertyValue(property));
				} else if (property.type == COLOR_TYPE) {
					builder.append(getColorStringPropertyValue(property));
				} else if (property.type == BOOLEAN_TYPE) {
					builder.append(getBoolPropertyValue(property));
				}
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	public String searchIconByTags(Map<String, String> transformedTags) {
		return searchTopOrderedPropertyByTags(transformedTags, RenderingRulesStorage.POINT_RULES,
				storage.PROPS.R_ICON, storage.PROPS.R_ICON_ORDER);
	}

	private String searchTopOrderedPropertyByTags(Map<String, String> transformedTags, int rulesNumber,
	                                              RenderingRuleProperty mainStringProperty,
	                                              RenderingRuleProperty orderIntProperty) {

		Map<String, Integer> resultOrderMap = new HashMap<>();

		for (Map.Entry<String, String> entry : transformedTags.entrySet()) {
			final String tag = entry.getKey();
			final String value = entry.getValue();
			clearState();
			setStringFilter(storage.PROPS.R_TAG, tag);
			setStringFilter(storage.PROPS.R_VALUE, value);
			clearValue(storage.PROPS.R_ADDITIONAL); // parent - no additional

			int order = 0;
			String result = null;
			search(rulesNumber);

			if (isSpecified(mainStringProperty)) {
				result = getStringPropertyValue(mainStringProperty);
				order = orderIntProperty != null ? getIntPropertyValue(orderIntProperty) : 0;
			}

			// Cycle tags to visit "additional" rules. XXX: think how to optimize.
			for (Map.Entry<String, String> additional : transformedTags.entrySet()) {
				final String aTag = additional.getKey();
				final String aValue = additional.getValue();

				if (aTag.equals(tag) && aValue.equals(value)) {
					continue;
				}

				setStringFilter(storage.PROPS.R_ADDITIONAL, aTag + "=" + aValue);
				search(rulesNumber);

				if (isSpecified(mainStringProperty)) {
					String childResult = getStringPropertyValue(mainStringProperty);
					if (childResult != null && (result == null || !result.equals(childResult))) {
						order = orderIntProperty != null ? getIntPropertyValue(orderIntProperty) : 0;
						result = childResult;
						break; // enough
					}
				}
			}

			if (result != null) {
				resultOrderMap.put(result, order);
			}
		}

		if (!resultOrderMap.isEmpty()) {
			Map.Entry<String, Integer> bestResult =
					Collections.min(resultOrderMap.entrySet(), Comparator.comparingInt(Map.Entry::getValue));
			return bestResult.getKey();
		}

		return null;
	}

	public void setClassProperty(String tag, String value) {
		classProperties.put(tag, value);
	}
}
