package net.osmand.render;

import java.util.ArrayList;
import java.util.List;

public class RenderingRuleSearchRequest {

	private final RenderingRulesStorage storage;
	RenderingRuleProperty[] props;
	int[] values;
	float[] fvalues;

	int[] savedValues;
	float[] savedFvalues;
	
	private List<RenderingRule> searchedScope = new ArrayList<RenderingRule>();

	public RenderingRuleSearchRequest(RenderingRulesStorage storage) {
		this.storage = storage;
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
	
	public boolean isFound() {
		return searchedScope.size() > 0;
	}
	
	public boolean search(int state) {
		int tagKey = values[storage.PROPS.R_TAG.getId()];
		int valueKey = values[storage.PROPS.R_VALUE.getId()];
		boolean result = search(state, tagKey, valueKey);
		if (result) {
			return true;
		}
		result = search(state, tagKey, 0);
		if (result) {
			return true;
		}
		result = search(state, 0, 0);
		if (result) {
			return true;
		}
		return false;
	}

	private boolean search(int state, int tagKey, int valueKey) {
		searchedScope.clear();
		values[storage.PROPS.R_TAG.getId()] = tagKey;
		values[storage.PROPS.R_VALUE.getId()] = valueKey;
		RenderingRule accept = storage.getRule(state, tagKey, valueKey);
		if (accept == null) {
			return false;
		}
		boolean match = visitRule(accept);
		return match;
	}

	private boolean visitRule(RenderingRule rule) {
		boolean empty = true;
		int ind = 0;
		for(RenderingRuleProperty rp : rule.getProperties()){
			if(rp.isInputProperty()){
				boolean match;
				if(rp.isFloat()){
					match = rp.accept(rule.getFloatProp(ind), fvalues[rp.getId()]);
				} else {
					match = rp.accept(rule.getIntProp(ind), values[rp.getId()]);
				}
				if(!match){
					return false;
				}
			}
			ind ++;
		}
		// accept it
		ind = 0;
		for(RenderingRuleProperty rp : rule.getProperties()){
			if(rp.isOutputProperty()){
				empty = false;
				if(rp.isFloat()){
					fvalues[rp.getId()] = rule.getFloatProp(ind);
				} else {
					values[rp.getId()] = rule.getIntProp(ind);
				}
			}
			ind++;
		}
		if(!empty) {
			searchedScope.add(rule);
		}
		
		for (RenderingRule rr : rule.getIfElseChildren()) {
			boolean match = visitRule(rr);
			if (match) {
				break;
			}
		}
		
		for(RenderingRule rr : rule.getIfChildren()){
			visitRule(rr);
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

}