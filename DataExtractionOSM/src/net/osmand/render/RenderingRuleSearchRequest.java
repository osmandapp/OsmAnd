package net.osmand.render;


public class RenderingRuleSearchRequest {

	private final RenderingRulesStorage storage;
	RenderingRuleProperty[] props;
	int[] values;
	float[] fvalues;

	int[] savedValues;
	float[] savedFvalues;
	
	boolean searchResult = false;
	
	
	public final RenderingRuleStorageProperties ALL;

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
		saveState();
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

	public void saveState() {
		savedValues = new int[values.length];
		savedFvalues = new float[fvalues.length];
		System.arraycopy(values, 0, savedValues, 0, values.length);
		System.arraycopy(fvalues, 0, savedFvalues, 0, fvalues.length);
	}

	public void clearState() {
		System.arraycopy(savedValues, 0, values, 0, values.length);
		System.arraycopy(savedFvalues, 0, fvalues, 0, fvalues.length);
	}
	
	public void clearValue(RenderingRuleProperty p) {
		if(p.isIntParse()){
			values[p.getId()] = savedValues[p.getId()];
		} else {
			fvalues[p.getId()] = savedFvalues[p.getId()];
		}
	}
	
	public void setInitialTagValueZoom(String tag, String val, int zoom){
		clearState();
		setIntFilter(ALL.R_MINZOOM, zoom);
		setIntFilter(ALL.R_MAXZOOM, zoom);
		setStringFilter(ALL.R_TAG, tag);
		setStringFilter(ALL.R_VALUE, val);
	}
	
	public void setTagValueZoomLayer(String tag, String val, int zoom, int layer){
		setIntFilter(ALL.R_MINZOOM, zoom);
		setIntFilter(ALL.R_MAXZOOM, zoom);
		setIntFilter(ALL.R_LAYER, layer);
		setStringFilter(ALL.R_TAG, tag);
		setStringFilter(ALL.R_VALUE, val);
	}
	
	public boolean isFound() {
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
		RenderingRule accept = storage.getRule(state, tagKey, valueKey);
		if (accept == null) {
			return false;
		}
		boolean match = visitRule(accept, loadOutput);
		return match;
	}

	private boolean visitRule(RenderingRule rule, boolean loadOutput) {
		RenderingRuleProperty[] properties = rule.getProperties();
		for (int i = 0; i < properties.length; i++) {
			RenderingRuleProperty rp = properties[i];
			if (rp.isInputProperty()) {
				boolean match;
				if (rp.isFloat()) {
					match = rp.accept(rule.getFloatProp(i), fvalues[rp.getId()]);
				} else {
					match = rp.accept(rule.getIntProp(i), values[rp.getId()]);
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
		for (int i = 0; i < properties.length; i++) {
			RenderingRuleProperty rp = properties[i];
			if (rp.isOutputProperty()) {
				searchResult = true;
				if (rp.isFloat()) {
					fvalues[rp.getId()] = rule.getFloatProp(i);
				} else {
					values[rp.getId()] = rule.getIntProp(i);
				}
			}
		}
		
		for (RenderingRule rr : rule.getIfElseChildren()) {
			boolean match = visitRule(rr, loadOutput);
			if (match) {
				break;
			}
		}
		
		for(RenderingRule rr : rule.getIfChildren()){
			visitRule(rr, loadOutput);
		}
		return true;
		
	}
	
	public boolean isSpecified(RenderingRuleProperty property){
		if(property.isFloat()){
			return fvalues[property.getId()] != 0;
		} else {
			int val = values[property.getId()];
			if(property.isColor()){
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
		if(val < 0){
			return null;
		}
		return storage.getStringValue(val);
	}
	
	public float getFloatPropertyValue(RenderingRuleProperty property) {
		return fvalues[property.getId()];
	}
	
	public String getColorStringPropertyValue(RenderingRuleProperty property) {
		return RenderingRuleProperty.colorToString(values[property.getId()]);
	}
	
	public int getIntPropertyValue(RenderingRuleProperty property) {
		return values[property.getId()];
	}
	
	public int getIntPropertyValue(RenderingRuleProperty property, int defValue) {
		int val = values[property.getId()];
		return val == -1 ? defValue : val;
	}

}