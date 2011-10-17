package net.osmand.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RenderingRule {
	
	private List<RenderingRuleProperty> properties;
	private int[] intProperties;
	private float[] floatProperties;
	
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
			RenderingRuleProperty property = storage.getProperty(e.getKey());
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
			}
			i++;
		}
	}

}
