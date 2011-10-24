package net.osmand.render;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.osmand.LogUtil;
import net.osmand.osm.MapRenderingTypes;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RenderingRulesStorage {

	private final static Log log = LogUtil.getLog(RenderingRulesStorage.class);
	
	// keep sync !
	public final static int POINT_RULES = MapRenderingTypes.POINT_TYPE;
	public final static int LINE_RULES = MapRenderingTypes.POLYLINE_TYPE;
	public final static int POLYGON_RULES = MapRenderingTypes.POLYGON_TYPE;
	public final static int TEXT_RULES = 4;
	public final static int ORDER_RULES = 5;
	private final static int LENGTH_RULES = 6;
	
	private final static int SHIFT_TAG_VAL = 16;
	// C++
	List<String> dictionary = new ArrayList<String>();
	Map<String, Integer> dictionaryMap = new LinkedHashMap<String, Integer>();
	
	public final RenderingRuleStorageProperties PROPS = new RenderingRuleStorageProperties();

	@SuppressWarnings("unchecked")
	protected TIntObjectHashMap<RenderingRule>[] tagValueGlobalRules = new TIntObjectHashMap[LENGTH_RULES];
	
	private int bgColor = 0;
	private int bgNightColor = 0;
	private String renderingName;
	private String depends;
	private RenderingRulesStorage dependsStorage;
	
	
	public RenderingRulesStorage(){
		// register empty string as 0
		getDictionaryValue("");
	}
	
	public int getDictionaryValue(String val) {
		if(dictionaryMap.containsKey(val)){
			return dictionaryMap.get(val);
		}
		int nextInd = dictionaryMap.size();
		dictionaryMap.put(val, nextInd);
		dictionary.add(val);
		return nextInd;

	}
	
	public String getStringValue(int i){
		return dictionary.get(i);
	}
	
	
	public String getName() {
		return renderingName;
	}
	
	public int getBgColor() {
		return bgColor;
	}
	
	public int getBgNightColor() {
		return bgNightColor;
	}
	
	public int getBgColor(boolean nightMode){
		return nightMode ? bgNightColor : bgColor;
	}
	
	public String getDepends() {
		return depends;
	}
	
	public RenderingRulesStorage getDependsStorage() {
		return dependsStorage;
	}
	
	public void setDependsStorage(RenderingRulesStorage dependsStorage) {
		this.dependsStorage = dependsStorage;
	}
	
	
	public void parseRulesFromXmlInputStream(InputStream is) throws SAXException, IOException {
		try {
			final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(is, new RenderingRulesHandler(saxParser));
		} catch (ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}

	public static String colorToString(int color) {
		if ((0xFF000000 & color) == 0xFF000000) {
			return "#" + Integer.toHexString(color & 0x00FFFFFF); //$NON-NLS-1$
		} else {
			return "#" + Integer.toHexString(color); //$NON-NLS-1$
		}
	}
	
	@SuppressWarnings("unchecked")
	private void registerGlobalRule(RenderingRule rr, int state, Map<String, String> attrsMap) throws SAXException {
		int tag = rr.getIntPropertyValue(RenderingRuleStorageProperties.TAG);
		if(tag == -1){
			throw new SAXException("Attribute tag should be specified for root filter " + attrsMap.toString());
		}
		int value = rr.getIntPropertyValue(RenderingRuleStorageProperties.VALUE);
		if(value == -1){
			throw new SAXException("Attribute tag should be specified for root filter " + attrsMap.toString());
		}
		int key = (tag << SHIFT_TAG_VAL) + value;
		RenderingRule toInsert = rr;
		RenderingRule previous = tagValueGlobalRules[state].get(key);
		if(previous != null){
			// all root rules should have at least tag/value
			if(previous.getProperties().length > 2){
				Map<String, String> m = new HashMap<String, String>();
				m.put("tag", dictionary.get(tag));
				m.put("value", dictionary.get(value));
				toInsert = new RenderingRule(m, RenderingRulesStorage.this);
				toInsert.addIfElseChildren(previous);
			} else {
				toInsert = previous; 
			}
			toInsert.addIfElseChildren(rr);
		}
		tagValueGlobalRules[state].put(key, toInsert);			
	}
	
	private class GroupRules {
		Map<String, String> groupAttributes = new LinkedHashMap<String, String>();
		List<RenderingRule> children = new ArrayList<RenderingRule>();
		List<GroupRules> childrenGroups = new ArrayList<GroupRules>();
		
		private void addGroupFilter(RenderingRule rr) {
			for (RenderingRule ch : children) {
				ch.addIfChildren(rr);
			}
			for(GroupRules gch : childrenGroups){
				gch.addGroupFilter(rr);
			}
		}

		public void registerGlobalRules(int state) throws SAXException {
			for (RenderingRule ch : children) {
				registerGlobalRule(ch, state, groupAttributes);
			}
			for(GroupRules gch : childrenGroups){
				gch.registerGlobalRules(state);
			}
			
		}
	}
	
	private class RenderingRulesHandler extends DefaultHandler {
		private final SAXParser parser;
		private int state;

		Stack<Object> stack = new Stack<Object>();
		
		Map<String, String> attrsMap = new LinkedHashMap<String, String>();
		
		
		public RenderingRulesHandler(SAXParser parser){
			this.parser = parser;
		}
		
		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			name = parser.isNamespaceAware() ? localName : name;
			boolean stateChanged = false;
			if("filter".equals(name)){ //$NON-NLS-1$
				attrsMap.clear();
				if (stack.size() > 0 && stack.peek() instanceof GroupRules) {
					GroupRules  parent = ((GroupRules) stack.peek());
					attrsMap.putAll(parent.groupAttributes);
				}
				parseAttributes(attributes, attrsMap);
				RenderingRule renderingRule = new RenderingRule(attrsMap, RenderingRulesStorage.this);
				
				if (stack.size() > 0 && stack.peek() instanceof GroupRules) {
					GroupRules parent = ((GroupRules) stack.peek());
					parent.children.add(renderingRule);
				} else if (stack.size() > 0 && stack.peek() instanceof RenderingRule) {
					RenderingRule parent = ((RenderingRule) stack.peek());
					parent.addIfElseChildren(renderingRule);
				} else {
					registerGlobalRule(renderingRule, state, attrsMap);
				}
				stack.push(renderingRule);
			} else if("groupFilter".equals(name)){ //$NON-NLS-1$
				attrsMap.clear();
				parseAttributes(attributes, attrsMap);
				RenderingRule renderingRule = new RenderingRule(attrsMap, RenderingRulesStorage.this);
				if (stack.size() > 0 && stack.peek() instanceof GroupRules) {
					GroupRules parent = ((GroupRules) stack.peek());
					parent.addGroupFilter(renderingRule);
				} else if (stack.size() > 0 && stack.peek() instanceof RenderingRule) {
					((RenderingRule) stack.peek()).addIfChildren(renderingRule);
				} else {
					throw new SAXException("Group filter without parent");
				}
				stack.push(renderingRule);
			} else if("group".equals(name)){ //$NON-NLS-1$
				GroupRules groupRules = new GroupRules();
				if (stack.size() > 0 && stack.peek() instanceof GroupRules) {
					GroupRules parent = ((GroupRules) stack.peek());
					groupRules.groupAttributes.putAll(parent.groupAttributes);
					parent.childrenGroups.add(groupRules);
				}
				parseAttributes(attributes, groupRules.groupAttributes);
				stack.push(groupRules);
			} else if("order".equals(name)){ //$NON-NLS-1$
				state = ORDER_RULES;
				stateChanged = true;
			} else if("text".equals(name)){ //$NON-NLS-1$
				state = TEXT_RULES;
				stateChanged = true;
			} else if("point".equals(name)){ //$NON-NLS-1$
				state = POINT_RULES;
				stateChanged = true;
			} else if("line".equals(name)){ //$NON-NLS-1$
				state = LINE_RULES;
				stateChanged = true;
			} else if("polygon".equals(name)){ //$NON-NLS-1$
				state = POLYGON_RULES;
				stateChanged = true;
			} else if("renderingProperty".equals(name)){ //$NON-NLS-1$
				String attr = attributes.getValue("attr");
				RenderingRuleProperty prop;
				String type = attributes.getValue("type");
				if("boolean".equalsIgnoreCase(type)){
					prop = RenderingRuleProperty.createInputBooleanProperty(attr);
				} else if("string".equalsIgnoreCase(type)){
					prop = RenderingRuleProperty.createInputStringProperty(attr);
				} else {
					prop = RenderingRuleProperty.createInputIntProperty(attr);
				}
				prop.setDescription(attributes.getValue("description"));
				prop.setName(attributes.getValue("name"));
				if(attributes.getValue("possibleValues") != null){
					prop.setPossibleValues(attributes.getValue("possibleValues").split(","));
				}
				PROPS.registerRule(prop);
			} else if("renderingStyle".equals(name)){ //$NON-NLS-1$
				String dc = attributes.getValue("defaultColor");
				int defaultColor = 0;
				if(dc != null && dc.length() > 0){
					bgColor = RenderingRuleProperty.parseColor(dc);
				}
				String dnc = attributes.getValue("defaultNightColor");
				bgNightColor = defaultColor;
				if(dnc != null && dnc.length() > 0){
					bgNightColor = RenderingRuleProperty.parseColor(dnc);
				}
				renderingName = attributes.getValue("name");
				depends = attributes.getValue("depends");
			} else if("renderer".equals(name)){ //$NON-NLS-1$
				throw new SAXException("Rendering style is deprecated and no longer supported.");
			} else {
				log.warn("Unknown tag : " + name); //$NON-NLS-1$
			}
			
			if(stateChanged){
				tagValueGlobalRules[state] = new TIntObjectHashMap<RenderingRule>();
			}
			
		}
		
		private Map<String, String> parseAttributes(Attributes attributes, Map<String, String> m) {
			for (int i = 0; i < attributes.getLength(); i++) {
				String name = parser.isNamespaceAware() ? attributes.getLocalName(i) : attributes.getQName(i);
				m.put(name, attributes.getValue(i));
			}
			return m;
		}


		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			name = parser.isNamespaceAware() ? localName : name;
			if ("filter".equals(name)) { //$NON-NLS-1$
				stack.pop();
			} else if("group".equals(name)){ //$NON-NLS-1$
				GroupRules group = (GroupRules) stack.pop();
				if (stack.size() == 0) {
					group.registerGlobalRules(state);
				}
			} else if("groupFilter".equals(name)){ //$NON-NLS-1$
				stack.pop();
			}
		}
		
	}
	
	public int getTagValueKey(String tag, String value){
		int itag = getDictionaryValue(tag);
		int ivalue = getDictionaryValue(value);
		return (itag << SHIFT_TAG_VAL) | ivalue; 
	}
	
	public String getValueString(int tagValueKey){
		return getStringValue(tagValueKey & ((1 << SHIFT_TAG_VAL) - 1)); 
	}
	
	public String getTagString(int tagValueKey){
		return getStringValue(tagValueKey >> SHIFT_TAG_VAL); 
	}
	
	protected RenderingRule getRule(int state, int itag, int ivalue){
		if(tagValueGlobalRules[state] != null){
			return tagValueGlobalRules[state].get((itag << SHIFT_TAG_VAL) | ivalue);
		}
		return null;
	}
	
	public RenderingRule[] getRules(int state){
		if(state >= tagValueGlobalRules.length ||  tagValueGlobalRules[state] == null) {
			return new RenderingRule[0];
		}
		return tagValueGlobalRules[state].values(new RenderingRule[tagValueGlobalRules[state].size()]);
	}
	
	
	public void printDebug(int state, PrintStream out){
		for(int key : tagValueGlobalRules[state].keys()) {
			RenderingRule rr = tagValueGlobalRules[state].get(key);
			out.print("\n\n"+getTagString(key) + " : " + getValueString(key));
			printRenderingRule("", rr, out);
		}
	}
	
	private static void printRenderingRule(String indent, RenderingRule rr, PrintStream out){
		indent += "   ";
		out.print("\n"+indent);
		for(RenderingRuleProperty p : rr.getProperties()){
			out.print(" " + p.getAttrName() + "= ");
			if(p.isString()){
				out.print("\"" + rr.getStringPropertyValue(p.getAttrName()) + "\"");
			} else if(p.isFloat()){
				out.print(rr.getFloatPropertyValue(p.getAttrName()));
			} else if(p.isColor()){
				out.print(rr.getColorPropertyValue(p.getAttrName()));
			} else if(p.isIntParse()){
				out.print(rr.getIntPropertyValue(p.getAttrName()));
			} 
		}
		for(RenderingRule rc : rr.getIfElseChildren()){
			printRenderingRule(indent, rc, out);
		}
		
	}
	
	
	public static void main(String[] args) throws SAXException, IOException {
		RenderingRulesStorage storage = new RenderingRulesStorage();
		storage.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream("new_default.render.xml"));
//		storage.printDebug(LINE_RULES, System.out);
		long tm = System.nanoTime();
		int count = 100000;
		for (int i = 0; i < count; i++) {
			RenderingRuleSearchRequest searchRequest = new RenderingRuleSearchRequest(storage);
			searchRequest.setStringFilter(storage.PROPS.R_TAG, "highway");
			searchRequest.setStringFilter(storage.PROPS.R_VALUE, "motorway");
			// searchRequest.setIntFilter(storage.PROPS.R_LAYER, 1);
			searchRequest.setIntFilter(storage.PROPS.R_MINZOOM, 14);
			searchRequest.setIntFilter(storage.PROPS.R_MAXZOOM, 14);
			// searchRequest.setStringFilter(storage.PROPS.R_ORDER_TYPE, "line");
			// searchRequest.setBooleanFilter(storage.PROPS.R_NIGHT_MODE, true);
			// searchRequest.setBooleanFilter(storage.PROPS.get("hmRendered"), true);

			searchRequest.search(ORDER_RULES);
			printResult(searchRequest, new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					
				}
			}));
		}
		System.out.println((System.nanoTime()- tm)/ (1e6f * count) );
	}

	private static void printResult(RenderingRuleSearchRequest searchRequest, PrintStream out) {
		if(searchRequest.isFound()){
			out.print(" Found : ");
			for (RenderingRuleProperty rp : searchRequest.getProperties()) {
				if(rp.isOutputProperty() && searchRequest.isSpecified(rp)){
					out.print(" " + rp.getAttrName() + "= ");
					if(rp.isString()){
						out.print("\"" + searchRequest.getStringPropertyValue(rp) + "\"");
					} else if(rp.isFloat()){
						out.print(searchRequest.getFloatPropertyValue(rp));
					} else if(rp.isColor()){
						out.print(searchRequest.getColorStringPropertyValue(rp));
					} else if(rp.isIntParse()){
						out.print(searchRequest.getIntPropertyValue(rp));
					}
				}
			}
		} else {
			out.println("Not found");
		}
		
	}
}
