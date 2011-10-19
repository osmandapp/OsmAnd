package net.osmand.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RenderingRule {
	
	private List<RenderingRuleProperty> properties;
	private int[] intProperties;
	private float[] floatProperties;
	private List<RenderingRule> ifElseChildren;
	private List<RenderingRule> ifChildren;
	
	private final RenderingRulesStorage storage;
	
	public RenderingRule(Map<String, String> attributes, RenderingRulesStorage storage){
		this.storage = storage;
		this.properties = new ArrayList<RenderingRuleProperty>();
		process(attributes);
	}

	private void process(Map<String, String> attributes) {
		properties = new ArrayList<RenderingRuleProperty>(attributes.size());
		intProperties = new int[attributes.size()];
		int i = 0;
		Iterator<Entry<String, String>> it = attributes.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> e = it.next();
			RenderingRuleProperty property = storage.PROPS.get(e.getKey());
			if (property != null) {
				properties.add(property);
				
				if (property.isString()) {
					intProperties[i] = storage.getDictionaryValue(e.getValue());
				} else if (property.isFloat()) {
					if (floatProperties == null) {
						// lazy creates
						floatProperties = new float[attributes.size()];
					}
					floatProperties[i] = property.parseFloatValue(e.getValue());
				} else {
					intProperties[i] = property.parseIntValue(e.getValue());
				}
				i++;
			}
			
		}
	}
	
	private int getPropertyIndex(String property){
		for (int i = 0; i < properties.size(); i++) {
			RenderingRuleProperty prop = properties.get(i);
			if (prop.getAttrName().equals(property)) {
				return i;
			}
		}
		return -1;
	}
	
	public String getStringPropertyValue(String property) {
		int i = getPropertyIndex(property);
		if(i >= 0){
			return storage.getStringValue(intProperties[i]);
		}
		
		return null;
	}
	
	public float getFloatPropertyValue(String property) {
		int i = getPropertyIndex(property);
		if(i >= 0){
			return floatProperties[i];
		}
		return 0;
	}
	
	public String getColorPropertyValue(String property) {
		int i = getPropertyIndex(property);
		if(i >= 0){
			return RenderingRuleProperty.colorToString(intProperties[i]);
		}
		return null;
	}
	
	public int getIntPropertyValue(String property) {
		int i = getPropertyIndex(property);
		if(i >= 0){
			return intProperties[i];
		}
		return -1;
	}
	
	protected int getIntProp(int ind){
		return intProperties[ind];
	}
	
	protected float getFloatProp(int ind){
		return floatProperties[ind];
	}
	
	public List<RenderingRuleProperty> getProperties() {
		return properties;
	}
	
	@SuppressWarnings("unchecked")
	public List<RenderingRule> getIfChildren() {
		return ifChildren != null ? ifChildren : Collections.EMPTY_LIST ;
	}
	
	@SuppressWarnings("unchecked")
	public List<RenderingRule> getIfElseChildren() {
		return ifElseChildren != null ? ifElseChildren : Collections.EMPTY_LIST ;
	}
	
	public void addIfChildren(RenderingRule rr){
		if(ifChildren == null){
			ifChildren = new ArrayList<RenderingRule>();
		}
		ifChildren.add(rr);
	}
	
	public void addIfElseChildren(RenderingRule rr){
		if(ifElseChildren == null){
			ifElseChildren = new ArrayList<RenderingRule>();
		}
		ifElseChildren.add(rr);
	}

}
