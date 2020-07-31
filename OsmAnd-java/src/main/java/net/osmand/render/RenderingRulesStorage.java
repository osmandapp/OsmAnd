package net.osmand.render;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.management.modelmbean.XMLParseException;

import net.osmand.PlatformUtil;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RenderingRulesStorage {

	private final static Log log = PlatformUtil.getLog(RenderingRulesStorage.class);
	static boolean STORE_ATTRIBUTES = false;
	
	// keep sync !
	// keep sync ! not change values
	public final static int MULTY_POLYGON_TYPE = 0;
	public final static int POINT_RULES = 1;
	public final static int LINE_RULES = 2;
	public final static int POLYGON_RULES = 3;
	public final static int TEXT_RULES = 4;
	public final static int ORDER_RULES = 5;
	public final static int LENGTH_RULES = 6;
	
	private final static int SHIFT_TAG_VAL = 16;

	
	// C++
	List<String> dictionary = new ArrayList<String>();
	Map<String, Integer> dictionaryMap = new LinkedHashMap<String, Integer>();
	
	public RenderingRuleStorageProperties PROPS = new RenderingRuleStorageProperties();

	@SuppressWarnings("unchecked")
	public TIntObjectHashMap<RenderingRule>[] tagValueGlobalRules = new TIntObjectHashMap[LENGTH_RULES];
	
	protected Map<String, RenderingRule> renderingAttributes = new LinkedHashMap<String, RenderingRule>();
	protected Map<String, String> renderingConstants = new LinkedHashMap<String, String>();
	
	protected String renderingName;
	protected String internalRenderingName;
	
	
	public static interface RenderingRulesStorageResolver {
		
		RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws XmlPullParserException, IOException;
	}
	
	public RenderingRulesStorage(String name, Map<String, String> renderingConstants){
		getDictionaryValue("");
		this.renderingName = name;
		if(renderingConstants != null) {
			this.renderingConstants.putAll(renderingConstants);
		}
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
	
	public String getInternalRenderingName() {
		return internalRenderingName;
	}
	
	
	public void parseRulesFromXmlInputStream(InputStream is, RenderingRulesStorageResolver resolver) throws XmlPullParserException,
			IOException {
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		RenderingRulesHandler handler = new RenderingRulesHandler(parser, resolver);
		handler.parse(is);
		RenderingRulesStorage depends = handler.getDependsStorage();
		if (depends != null) {
			// merge results
			// dictionary and props are already merged
			Iterator<Entry<String, RenderingRule>> it = depends.renderingAttributes.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, RenderingRule> e = it.next();
				if (renderingAttributes.containsKey(e.getKey())) {
					RenderingRule root = renderingAttributes.get(e.getKey());
					List<RenderingRule> list = e.getValue().getIfElseChildren();
					for (RenderingRule every : list) {
						root.addIfElseChildren(every);
					}
					e.getValue().addToBeginIfElseChildren(root);
				} else {
					renderingAttributes.put(e.getKey(), e.getValue());
				}
			}

			for (int i = 0; i < LENGTH_RULES; i++) {
				if (depends.tagValueGlobalRules[i] == null || depends.tagValueGlobalRules[i].isEmpty()) {
					continue;
				}
				if (tagValueGlobalRules[i] != null) {
					int[] keys = depends.tagValueGlobalRules[i].keys();
					for (int j = 0; j < keys.length; j++) {
						RenderingRule rule = tagValueGlobalRules[i].get(keys[j]);
						RenderingRule dependsRule = depends.tagValueGlobalRules[i].get(keys[j]);
						if (dependsRule != null) {
							if (rule != null) {
								RenderingRule toInsert = createTagValueRootWrapperRule(keys[j], rule);
								toInsert.addIfElseChildren(dependsRule);
								tagValueGlobalRules[i].put(keys[j], toInsert);
							} else {
								tagValueGlobalRules[i].put(keys[j], dependsRule);
							}
						}
					}
				} else {
					tagValueGlobalRules[i] = depends.tagValueGlobalRules[i];
				}
			}

		}
	}

	public static String colorToString(int color) {
		if ((0xFF000000 & color) == 0xFF000000) {
			return "#" + Integer.toHexString(color & 0x00FFFFFF); //$NON-NLS-1$
		} else {
			return "#" + Integer.toHexString(color); //$NON-NLS-1$
		}
	}
	
	private void registerGlobalRule(RenderingRule rr, int state, String tagS, String valueS) throws XmlPullParserException {
		if(tagS == null || valueS == null){
			throw new XmlPullParserException("Attribute tag should be specified for root filter " + rr.toString());
		}
		int tag = getDictionaryValue(tagS);
		int value = getDictionaryValue(valueS);
		int key = (tag << SHIFT_TAG_VAL) + value;
		RenderingRule insert = tagValueGlobalRules[state].get(key);
		if (insert != null) {
			// all root rules should have at least tag/value
			insert = createTagValueRootWrapperRule(key, insert);
			insert.addIfElseChildren(rr);
		} else {
			insert = rr;
		}
		tagValueGlobalRules[state].put(key, insert);			
	}
	

	private RenderingRule createTagValueRootWrapperRule(int tagValueKey, RenderingRule previous) {
		if (previous.getProperties().length > 0) {
			Map<String, String> m = new HashMap<String, String>();
			RenderingRule toInsert = new RenderingRule(m, true, RenderingRulesStorage.this);
			toInsert.addIfElseChildren(previous);
			return toInsert;
		} else {
			return previous;
		}
	}
	
	private class SequencedRule {
		String name;
		Map<String, String> attrs = new HashMap<String, String>();
		
		List<SequencedRule> ifElseChildren = new LinkedList<SequencedRule>();
		List<SequencedRule> ifChildren = new LinkedList<SequencedRule>();
		
		public SequencedRule(String name, Map<String, String> attrs) {
			this.name = name;
			this.attrs = attrs;
		}
	
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Map<String, String> getAttrs() {
			return attrs;
		}

		public void setAttrs(Map<String, String> attrs) {
			this.attrs = attrs;
		}

		public List<SequencedRule> getIfElseChildren() {
			return ifElseChildren;
		}

		public void addIfElseChild(SequencedRule ifElseChild) {
			ifElseChildren.add(ifElseChild);
		}

		public List<SequencedRule> getIfChildren() {
			return ifChildren;
		}

		public void addIfChildren(SequencedRule ifChild) {
			ifChildren.add(ifChild);
		}
	}
	
	private class RenderingRulesHandler {
		private final XmlPullParser parser;
		private int state;

		Stack<RenderingRule> stack = new Stack<RenderingRule>();
		
		Map<String, String> attrsMap = new LinkedHashMap<String, String>();
		private final RenderingRulesStorageResolver resolver;
		private RenderingRulesStorage dependsStorage;
		
		private final static String SEQ_ATTR_KEY = "seq";
		private final static String SEQ_PARAM_PH = "#SEQ";

		boolean isSequencedRule = false;
		List<String> sequenceKeys = new ArrayList<String>();
		
		public RenderingRulesHandler(XmlPullParser parser, RenderingRulesStorageResolver resolver){
			this.parser = parser;
			this.resolver = resolver;
		}
		
		public void parse(InputStream is) throws XmlPullParserException, IOException {
			parser.setInput(is, "UTF-8");
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					if (isSwitch(parser.getName()) && parser.getAttributeCount() > 0) {
						for (int i = 0; i < parser.getAttributeCount(); i++) {
							if (parser.getAttributeName(i).equals(SEQ_ATTR_KEY)) {
								processSequence(parser.getAttributeValue(i));
							}
						}
					} else {
						startElement(parser.getName());
					}					
				} else if (tok == XmlPullParser.END_TAG) {
					endElement(parser.getName());
				}
			}
			
		}
		
		public RenderingRulesStorage getDependsStorage() {
			return dependsStorage;
		}
		
		private boolean isTopCase() {
			for(int i = 0; i < stack.size(); i++) {
				if(!stack.get(i).isGroup()) {
					return false;
				}
			}
			return true;
		}
		
		private void processSequence(String seq) throws XmlPullParserException, IOException {
			int seqStart = 0;
			int seqEnd = 0;
			Stack<SequencedRule> seqStack = new Stack<RenderingRulesStorage.SequencedRule>();
			List<SequencedRule> seqSwitches;
			try {
				seqStart = Integer.parseInt(seq.substring(0, seq.indexOf(':')));
				seqEnd = Integer.parseInt(seq.substring(seq.indexOf(':') + 1, seq.length()));
			} catch (NumberFormatException nfe) {
				seqStart = 0;
				seqEnd = 0;
				throw new XmlPullParserException("Broken sequence tag, pass section");  
			}
			SequencedRule ssw = new SequencedRule("switch", parseAttributes(new HashMap<String, String>()));
			seqStack.add(ssw);
			int sTok;
			String sName;
			Map<String, String> seqAttrsMap = new HashMap<String, String>();
			
			//parse sequenced block from .style.xml:
			while((sTok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (sTok == XmlPullParser.START_TAG) {
					sName = parser.getName();
					if (isCase(sName) || isSwitch(sName)) {
						seqAttrsMap.clear();
						SequencedRule sRule = new SequencedRule(sName, parseAttributes(seqAttrsMap));
						if (seqStack.size() > 0) {
							seqStack.peek().addIfElseChild(sRule);
						} 
						seqStack.push(sRule);
					} else if (isApply(sName)) {
						seqAttrsMap.clear();
						SequencedRule sRule = new SequencedRule(sName, parseAttributes(seqAttrsMap));
						if (seqStack.size() > 0) {
							seqStack.peek().addIfChildren(sRule);

						} else {
							throw new XmlPullParserException("Apply (groupFilter) without parent");							
						}
						seqStack.push(sRule);
					} else {
						throw new XmlPullParserException("Unsupported tag in sequence"); //TODO for now
					}
			
				} else if (sTok == XmlPullParser.END_TAG) {
					if (seqStack.size() == 1 && seqStack.peek().getAttrs().containsKey(SEQ_ATTR_KEY)) {
						seqStack.pop();
						break;
					}
					if (isSwitch(parser.getName()) || isCase(parser.getName()) || isApply(parser.getName())) {
						seqStack.pop();
					}
				}
			}
			
			//iterate sequence and write rules to storage:
			if (ssw.getIfElseChildren().size() > 0) {				
				for (int n = seqStart; n <= seqEnd; n++) {
					writeSeqRules(ssw , n);
				}
			} else {
				throw new XmlPullParserException("No cases in switch!");
			}
		}
		
		public void writeSeqRules(SequencedRule sRule, int seqPos) throws XmlPullParserException {
			final boolean isSwitch = isSwitch(sRule.getName());
			attrsMap = new HashMap<String, String> (sRule.attrs);
			//iterate values in attributes
			sequenceKeys.clear();
			for (Entry<String, String> attr : attrsMap.entrySet()) {
				if (attr.getValue().contains(SEQ_ATTR_KEY)) {
					sequenceKeys.add(attr.getKey());
				}
			}
			
			if (!sequenceKeys.isEmpty()) {
				for (String key : sequenceKeys) {
					attrsMap.put(key, attrsMap.get(key).replace(SEQ_PARAM_PH, seqPos + ""));
				}
			}
			
			if (isSwitch || isCase(sRule.getName())) {
				if (attrsMap.containsKey(SEQ_ATTR_KEY)) {					
					attrsMap.remove(SEQ_ATTR_KEY);
				}
				boolean top = stack.size() == 0 || isTopCase();
				RenderingRule renderingRule = new RenderingRule(attrsMap, isSwitch, RenderingRulesStorage.this);
				if (top || STORE_ATTRIBUTES) {					
					renderingRule.storeAttributes(attrsMap);
				}
				if (stack.size() > 0 && stack.peek() instanceof RenderingRule) {
					RenderingRule parent = ((RenderingRule) stack.peek());
					parent.addIfElseChildren(renderingRule);
				}
				stack.push(renderingRule);
				
				for (SequencedRule sr : sRule.ifElseChildren) {
					writeSeqRules(sr, seqPos);
				}
				for (SequencedRule sr : sRule.ifChildren) {
					writeSeqRules(sr, seqPos);
				}
				stack.pop();
			} else if (isApply(sRule.getName())) {

				RenderingRule renderingRule = new RenderingRule(attrsMap, false, RenderingRulesStorage.this);
				if(STORE_ATTRIBUTES) {
					renderingRule.storeAttributes(attrsMap);
				}
				if (stack.size() > 0 && stack.peek() instanceof RenderingRule) {
					((RenderingRule) stack.peek()).addIfChildren(renderingRule);
				} else {
					throw new XmlPullParserException("Apply (groupFilter) without parent");
				}
				stack.push(renderingRule);
				for (SequencedRule sr : sRule.ifElseChildren) {
					writeSeqRules(sr, seqPos);
				}
				for (SequencedRule sr : sRule.ifChildren) {
					writeSeqRules(sr, seqPos);
				}
				stack.pop();
			}
		}
		
		public void startElement(String name) throws XmlPullParserException, IOException {

			boolean stateChanged = false;
			final boolean isCase = isCase(name);
			final boolean isSwitch = isSwitch(name);
			if(isCase || isSwitch){ //$NON-NLS-1$
				attrsMap.clear();
				boolean top = stack.size() == 0 || isTopCase();
				parseAttributes(attrsMap);
				RenderingRule renderingRule = new RenderingRule(attrsMap, isSwitch, RenderingRulesStorage.this);
				if(top || STORE_ATTRIBUTES){
					renderingRule.storeAttributes(attrsMap);
				}
				if (stack.size() > 0 && stack.peek() instanceof RenderingRule) {
					RenderingRule parent = ((RenderingRule) stack.peek());
					parent.addIfElseChildren(renderingRule);
				}
				stack.push(renderingRule);
			} else if(isApply(name)){ //$NON-NLS-1$
				attrsMap.clear();
				parseAttributes(attrsMap);
				RenderingRule renderingRule = new RenderingRule(attrsMap, false, RenderingRulesStorage.this);
				if(STORE_ATTRIBUTES) {
					renderingRule.storeAttributes(attrsMap);
				}
				if (stack.size() > 0 && stack.peek() instanceof RenderingRule) {
					((RenderingRule) stack.peek()).addIfChildren(renderingRule);
				} else {
					throw new XmlPullParserException("Apply (groupFilter) without parent");
				}
				stack.push(renderingRule);
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
			} else if("renderingAttribute".equals(name)){ //$NON-NLS-1$
				String attr = parser.getAttributeValue("", "name");
				RenderingRule root = new RenderingRule(new HashMap<String, String>(), false, RenderingRulesStorage.this);
				renderingAttributes.put(attr, root);
				stack.push(root);
			} else if("renderingProperty".equals(name)){ //$NON-NLS-1$
				String attr = parser.getAttributeValue("", "attr");
				RenderingRuleProperty prop;
				String type = parser.getAttributeValue("", "type");
				if("boolean".equalsIgnoreCase(type)){
					prop = RenderingRuleProperty.createInputBooleanProperty(attr);
				} else if("string".equalsIgnoreCase(type)){
					prop = RenderingRuleProperty.createInputStringProperty(attr);
				} else {
					prop = RenderingRuleProperty.createInputIntProperty(attr);
				}
				prop.setDescription(parser.getAttributeValue("", "description"));
				prop.setDefaultValueDescription(parser.getAttributeValue("", "defaultValueDescription"));
				prop.setCategory(parser.getAttributeValue("", "category"));
				prop.setName(parser.getAttributeValue("", "name"));
				if(parser.getAttributeValue("", "possibleValues") != null){
					prop.setPossibleValues(parser.getAttributeValue("", "possibleValues").split(","));
				}
				PROPS.registerRule(prop);
			} else if("renderingConstant".equals(name)){ //$NON-NLS-1$
				if(!renderingConstants.containsKey(parser.getAttributeValue("", "name"))){
					renderingConstants.put(parser.getAttributeValue("", "name"), parser.getAttributeValue("", "value"));
				}
			} else if("renderingStyle".equals(name)){ //$NON-NLS-1$
				String depends = parser.getAttributeValue("", "depends");
				if(depends != null && depends.length()> 0){
					this.dependsStorage = resolver.resolve(depends, resolver);
				}
				if(dependsStorage != null){
					// copy dictionary
					dictionary = new ArrayList<String>(dependsStorage.dictionary);
					dictionaryMap = new LinkedHashMap<String, Integer>(dependsStorage.dictionaryMap);
					PROPS = new RenderingRuleStorageProperties(dependsStorage.PROPS);
					
				}
				internalRenderingName = parser.getAttributeValue("", "name");
				
			} else if("renderer".equals(name)){ //$NON-NLS-1$
				throw new XmlPullParserException("Rendering style is deprecated and no longer supported.");
			} else {
				log.warn("Unknown tag : " + name); //$NON-NLS-1$
			}
			
			if(stateChanged){
				tagValueGlobalRules[state] = new TIntObjectHashMap<RenderingRule>();
			}
			
		}

		protected boolean isCase(String name) {
			return "filter".equals(name) || "case".equals(name);
		}

		protected boolean isApply(String name) {
			return "groupFilter".equals(name) || "apply".equals(name) || "apply_if".equals(name);
		}

		protected boolean isSwitch(String name) {
			return "group".equals(name) || "switch".equals(name);
		}
		
		private Map<String, String> parseAttributes(Map<String, String> m) {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				String name = parser.getAttributeName(i);
				String vl = parser.getAttributeValue(i);
				if (vl != null && vl.startsWith("$")) {
					String cv = vl.substring(1);
					if (!renderingConstants.containsKey(cv) &&
							!renderingAttributes.containsKey(cv)) {
						throw new IllegalStateException("Rendering constant or attribute '" + cv + "' was not specified.");
					}
					if(renderingConstants.containsKey(cv)){
						vl = renderingConstants.get(cv);
					}
				}
				m.put(name, vl);
			}
			return m;
		}


		public void endElement(String name) throws XmlPullParserException  {
			if (isCase(name) || isSwitch(name)) { 
				RenderingRule renderingRule = (RenderingRule) stack.pop();
				if(stack.size() == 0) {
					registerTopLevel(renderingRule, null, Collections.EMPTY_MAP);
				}
			} else if(isApply(name)){ 
				stack.pop();
			} else if("renderingAttribute".equals(name)){ //$NON-NLS-1$
				stack.pop();
			}
		}

		protected void registerTopLevel(RenderingRule renderingRule, List<RenderingRule> applyRules, Map<String, String> attrs) throws XmlPullParserException {
			if(renderingRule.isGroup() && (renderingRule.getIntPropertyValue(RenderingRuleStorageProperties.TAG) == -1 ||
					renderingRule.getIntPropertyValue(RenderingRuleStorageProperties.VALUE) == -1)){
				List<RenderingRule> caseChildren = renderingRule.getIfElseChildren();
				for(RenderingRule ch : caseChildren) {
					List<RenderingRule> apply = applyRules;
					if(!renderingRule.getIfChildren().isEmpty()) {
						apply = new ArrayList<RenderingRule>();
						apply.addAll(renderingRule.getIfChildren());
						if(applyRules != null) {
							apply.addAll(applyRules);
						}
					}
					Map<String, String> cattrs = new HashMap<String, String>(attrs);
					cattrs.putAll(renderingRule.getAttributes());
					registerTopLevel(ch, apply, cattrs);
				}
			} else {
				String tg = null;
				String vl = null;
				HashMap<String, String> ns = new HashMap<String, String>(attrs);
				ns.putAll(renderingRule.getAttributes());
				tg = ns.remove("tag");
				vl = ns.remove("value");
				// reset rendering rule attributes
				renderingRule.init(ns);
				if(STORE_ATTRIBUTES) {
					renderingRule.storeAttributes(ns);
				}
				
				registerGlobalRule(renderingRule, state, tg, vl);
				if (applyRules != null) {
					for (RenderingRule apply : applyRules) {
						renderingRule.addIfChildren(apply);
					}
				}
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
	
	public RenderingRule getRenderingAttributeRule(String attribute){
		return renderingAttributes.get(attribute);
	}
	
	public String[] getRenderingAttributeNames() {
		return renderingAttributes.keySet().toArray(new String[renderingAttributes.size()]);
	}
	public RenderingRule[] getRenderingAttributeValues() {
		return renderingAttributes.values().toArray(new RenderingRule[renderingAttributes.size()]);
	}
	
	public RenderingRule[] getRules(int state){
		if(state >= tagValueGlobalRules.length ||  tagValueGlobalRules[state] == null) {
			return new RenderingRule[0];
		}
		return tagValueGlobalRules[state].values(new RenderingRule[tagValueGlobalRules[state].size()]);
	}
	
	public int getRuleTagValueKey(int state, int ind){
		return tagValueGlobalRules[state].keys()[ind];
	}
	
	
	public void printDebug(int state, PrintStream out){
		for(int key : tagValueGlobalRules[state].keys()) {
			RenderingRule rr = tagValueGlobalRules[state].get(key);
			out.print("\n\n"+getTagString(key) + " : " + getValueString(key) + "\n ");
			printRenderingRule(" ", rr, out);
		}
	}
	
	private static void printRenderingRule(String indent, RenderingRule rr, PrintStream out){
		out.print(rr.toString(indent, new StringBuilder()).toString());
	}
	
	
	public static void main(String[] args) throws XmlPullParserException, IOException {
		STORE_ATTRIBUTES = true;
//		InputStream is = RenderingRulesStorage.class.getResourceAsStream("default.render.xml");
		final String loc = "/home/madwasp79/Osmand/resources/rendering_styles/";
		String defaultFile = loc + "default.render.xml";
		if(args.length > 0) {
			defaultFile = args[0];
		}
		final Map<String, String> renderingConstants = new LinkedHashMap<String, String>();
		InputStream is = new FileInputStream(loc + "default.render.xml");
		try {
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(is, "UTF-8");
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String tagName = parser.getName();
					if (tagName.equals("renderingConstant")) {
						if (!renderingConstants.containsKey(parser.getAttributeValue("", "name"))) {
							renderingConstants.put(parser.getAttributeValue("", "name"), 
									parser.getAttributeValue("", "value"));
						}
					}
				}
			}
		} finally {
			is.close();
		}
		RenderingRulesStorage storage = new RenderingRulesStorage("default", renderingConstants);
		final RenderingRulesStorageResolver resolver = new RenderingRulesStorageResolver() {
			@Override
			public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws XmlPullParserException, IOException {
				RenderingRulesStorage depends = new RenderingRulesStorage(name, renderingConstants);
//				depends.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream(name + ".render.xml"), ref);
				depends.parseRulesFromXmlInputStream(new FileInputStream(loc + name + ".render.xml"), ref);
				return depends;
			}
		};
		is = new FileInputStream(defaultFile);
		storage.parseRulesFromXmlInputStream(is, resolver);
		
//		storage = new RenderingRulesStorage("", null);
//		new DefaultRenderingRulesStorage().createStyle(storage);
		for (RenderingRuleProperty p :  storage.PROPS.getCustomRules()) {
			System.out.println(p.getCategory() + " " + p.getName() + " " + p.getAttrName());
		}
//		printAllRules(storage);
//		testSearch(storage);
		
	}
	
	
	
	protected static void testSearch(RenderingRulesStorage storage) {
		//		long tm = System.nanoTime();
		//		int count = 100000;
		//		for (int i = 0; i < count; i++) {
					RenderingRuleSearchRequest searchRequest = new RenderingRuleSearchRequest(storage);
					searchRequest.setStringFilter(storage.PROPS.R_TAG, "highway");
					searchRequest.setStringFilter(storage.PROPS.R_VALUE, "residential");
//					searchRequest.setStringFilter(storage.PROPS.R_ADDITIONAL, "leaf_type=broadleaved");
//					 searchRequest.setIntFilter(storage.PROPS.R_LAYER, 1);
					searchRequest.setIntFilter(storage.PROPS.R_MINZOOM, 13);
					searchRequest.setIntFilter(storage.PROPS.R_MAXZOOM, 13);
//						searchRequest.setBooleanFilter(storage.PROPS.R_NIGHT_MODE, true);
//					for (RenderingRuleProperty customProp : storage.PROPS.getCustomRules()) {
//						if (customProp.isBoolean()) {
//							searchRequest.setBooleanFilter(customProp, false);
//						} else {
//							searchRequest.setStringFilter(customProp, "");
//						}
//					}
//					searchRequest.setBooleanFilter(storage.PROPS.get("noPolygons"), true);
					boolean res = searchRequest.search(LINE_RULES);
					System.out.println("Result " + res);
					printResult(searchRequest,  System.out);
		//		}
		//		System.out.println((System.nanoTime()- tm)/ (1e6f * count) );
	}

	protected static void printAllRules(RenderingRulesStorage storage) {
		System.out.println("\n\n--------- POINTS ----- ");
		storage.printDebug(POINT_RULES, System.out);
		System.out.println("\n\n--------- POLYGON ----- ");
		storage.printDebug(POLYGON_RULES, System.out);
		System.out.println("\n\n--------- LINES ----- ");
		storage.printDebug(LINE_RULES, System.out);
		System.out.println("\n\n--------- ORDER ----- ");
		storage.printDebug(ORDER_RULES, System.out);
		System.out.println("\n\n--------- TEXT ----- ");
		storage.printDebug(TEXT_RULES, System.out);
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
