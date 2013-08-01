package net.osmand.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RenderingRule {
	
	private RenderingRuleProperty[] properties;
	private int[] intProperties;
	private float[] floatProperties;
	private List<RenderingRule> ifElseChildren;
	private List<RenderingRule> ifChildren;
	
	private final RenderingRulesStorage storage;
	
	public RenderingRule(Map<String, String> attributes, RenderingRulesStorage storage){
		this.storage = storage;
		process(attributes);
	}

	private void process(Map<String, String> attributes) {
		ArrayList<RenderingRuleProperty> props = new ArrayList<RenderingRuleProperty>(attributes.size());
		intProperties = new int[attributes.size()];
		int i = 0;
		Iterator<Entry<String, String>> it = attributes.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> e = it.next();
			RenderingRuleProperty property = storage.PROPS.get(e.getKey());
			if (property != null) {
				props.add(property);
				
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
		properties = props.toArray(new RenderingRuleProperty[props.size()]);
	}
	
	private int getPropertyIndex(String property){
		for (int i = 0; i < properties.length; i++) {
			RenderingRuleProperty prop = properties[i];
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
	
	public RenderingRuleProperty[] getProperties() {
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
	
	@Override
	public String toString() {
		StringBuilder bls = new StringBuilder();
		toString("", bls);
		return bls.toString();
	}
	
	public StringBuilder toString(String indent, StringBuilder bls ) {
		bls.append("RenderingRule [");
		for(RenderingRuleProperty p : getProperties()){
			bls.append(" ").append(p.getAttrName()).append("= ");
			if(p.isString()){
				bls.append("\"").append(getStringPropertyValue(p.getAttrName())).append("\"");
			} else if(p.isFloat()){
				bls.append(getFloatPropertyValue(p.getAttrName()));
			} else if(p.isColor()){
				bls.append(getColorPropertyValue(p.getAttrName()));
			} else if(p.isIntParse()){
				bls.append(getIntPropertyValue(p.getAttrName()));
			} 
		}
		bls.append("]");
		indent += "   ";
		for(RenderingRule rc : getIfElseChildren()){
			bls.append("\n").append(indent);
			rc.toString(indent, bls);
		}
		
		return bls;
	}

}
