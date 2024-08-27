package net.osmand.render;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RenderingRule {
	
	private RenderingRuleProperty[] properties;
	private int[] intProperties;
	private RenderingRule[] attributesRef;
	private float[] floatProperties;
	private List<RenderingRule> ifElseChildren;
	private List<RenderingRule> ifChildren;
	private final boolean isGroup;

	private final RenderingRulesStorage storage;
	private Map<String, String> attributes;
	
	public RenderingRule(Map<String, String> attributes, boolean isGroup, RenderingRulesStorage storage){
		this.isGroup = isGroup;
		this.storage = storage;
		init(attributes);
	}
	
	public void storeAttributes(Map<String, String> attributes){
		this.attributes = new HashMap<String, String>(attributes);
	}
	
	public Map<String, String> getAttributes() {
		return attributes == null ? Collections.EMPTY_MAP : attributes;
	}

	public boolean hasAttributes(Map<String, String> attributes) {
		for (Entry<String, String> tagValue : attributes.entrySet()) {
			if (!tagValue.getValue().equals(this.attributes.get(tagValue.getKey()))) {
				return false;
			}
		}
		return true;
	}

	public void init(Map<String, String> attributes) {
		ArrayList<RenderingRuleProperty> props = new ArrayList<RenderingRuleProperty>(attributes.size());
		intProperties = new int[attributes.size()];
		floatProperties = new float[attributes.size()];
		attributesRef = null;
		int i = 0;
		Iterator<Entry<String, String>> it = attributes.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> e = it.next();
			RenderingRuleProperty property = storage.PROPS.get(e.getKey());
			if (property != null) {
				props.add(property);
				String vl = e.getValue();
				if(vl.startsWith("$")){
					if (attributesRef == null) {
						attributesRef = new RenderingRule[attributes.size()];
					}
					attributesRef[i] = storage.getRenderingAttributeRule(vl.substring(1));
					if (attributesRef[i] == null) {
						attributesRef[i] = storage.getRenderingAssociationRule(vl.substring(1));
					}
				} else if (property.isString()) {
					intProperties[i] = storage.getDictionaryValue(vl);
				} else {
					float floatVal = property.parseFloatValue(vl);
//					if (floatProperties == null && floatVal != 0) {
//						// lazy creates
//						floatProperties = new float[attributes.size()];
						floatProperties[i] = floatVal;
//					}
					intProperties[i] = property.parseIntValue(vl);
				}
				i++;
			}
		}
		properties = props.toArray(new RenderingRuleProperty[0]);
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
		if (i >= 0) {
			return floatProperties[i];
		}
		return 0;
	}
	
	public String getColorPropertyValue(String property) {
		int i = getPropertyIndex(property);
		if(i >= 0){
			return Algorithms.colorToString(intProperties[i]);
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
	
	protected RenderingRule getAttrProp(int ind) {
		if (attributesRef == null) {
			return null;
		}
		return attributesRef[ind];
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
	
	public void addToBeginIfElseChildren(RenderingRule rr){
		if(ifElseChildren == null){
			ifElseChildren = new ArrayList<RenderingRule>();
		}
		ifElseChildren.add(0, rr);
	}
	
	public boolean isGroup() {
		return isGroup;
	}

	public void removeIfChildren(RenderingRule rule) {
		if (ifChildren != null) {
			List<RenderingRule> children = new ArrayList<>(ifChildren);
			children.remove(rule);
			ifChildren = children;
		}
	}

	public void removeIfElseChildren(RenderingRule rule) {
		if (ifElseChildren != null) {
			List<RenderingRule> children = new ArrayList<>(ifElseChildren);
			children.remove(rule);
			ifElseChildren = children;
		}
	}

	@Override
	public String toString() {
		StringBuilder bls = new StringBuilder();
		toString("", bls);
		return bls.toString();
	}
	
	public StringBuilder toString(String indent, StringBuilder bls ) {
		if(isGroup){
			bls.append("switch test [");
		} else {
			bls.append(" test [");
		}
		printAttrs(bls, true);
		bls.append("]");
		
		bls.append(" set [");
		printAttrs(bls, false);
		bls.append("]");
		
		for(RenderingRule rc : getIfElseChildren()){
			String cindent = indent + "* case ";
			bls.append("\n").append(cindent);
			rc.toString(indent + "*    ", bls);
		}
		
		for(RenderingRule rc : getIfChildren()){
			String cindent = indent + "* apply " ;
			bls.append("\n").append(cindent);
			rc.toString(indent + "*    ", bls);
		}
		
		return bls;
	}

	protected void printAttrs(StringBuilder bls, boolean in) {
		for(RenderingRuleProperty p : getProperties()){
			if (p.isInputProperty() != in) {
				continue;
			}
			bls.append(" ").append(p.getAttrName()).append("= ");
			if (attributesRef != null && attributesRef[getPropertyIndex(p.getAttrName())] != null) {
				bls.append(attributesRef[getPropertyIndex(p.getAttrName())]);
			} else if (p.isString()) {
				bls.append("\"").append(getStringPropertyValue(p.getAttrName())).append("\"");
			} else if (p.isFloat()) {
				bls.append(getFloatPropertyValue(p.getAttrName()));
			} else if (p.isColor()) {
				bls.append(getColorPropertyValue(p.getAttrName()));
			} else if (p.isIntParse()) {
				bls.append(getIntPropertyValue(p.getAttrName()));
			}
		}
	}
}
