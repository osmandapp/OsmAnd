package net.osmand.render;

import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import gnu.trove.map.hash.TIntObjectHashMap;

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

	private final static String SEQ_ATTR_KEY = "seq";
	private final static String SEQ_PLACEHOLDER = "#SEQ";
	
	// C++
	List<String> dictionary = new ArrayList<String>();
	Map<String, Integer> dictionaryMap = new LinkedHashMap<String, Integer>();
	
	public RenderingRuleStorageProperties PROPS = new RenderingRuleStorageProperties();

	@SuppressWarnings("unchecked")
	public TIntObjectHashMap<RenderingRule>[] tagValueGlobalRules = new TIntObjectHashMap[LENGTH_RULES];
	
	protected Map<String, RenderingRule> renderingAttributes = new LinkedHashMap<String, RenderingRule>();
	protected Map<String, RenderingRule> renderingAssociations = new LinkedHashMap<String, RenderingRule>();
	protected Map<String, String> renderingConstants = new LinkedHashMap<String, String>();
	
	protected String renderingName;
	protected String dependsName;
	protected String internalRenderingName;

	protected int internalVersion = 1;


	public String getDependsName() {
		return dependsName;
	}

	public interface RenderingRulesStorageResolver {
		RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws XmlPullParserException, IOException;
	}
	
	public RenderingRulesStorage(String name, Map<String, String> renderingConstants){
		getDictionaryValue("");
		this.renderingName = name;
		if (renderingConstants != null) {
			this.renderingConstants.putAll(renderingConstants);
		}
	}

	@SuppressWarnings("unchecked")
	public RenderingRulesStorage copy() {
		RenderingRulesStorage storage = new RenderingRulesStorage(renderingName, renderingConstants);
		storage.internalRenderingName = internalRenderingName;
		storage.internalVersion = internalVersion + 1;
		storage.dictionary = new ArrayList<>(dictionary);
		storage.dictionaryMap.putAll(dictionaryMap);
		storage.PROPS = new RenderingRuleStorageProperties(PROPS);
		storage.tagValueGlobalRules = new TIntObjectHashMap[tagValueGlobalRules.length];
		for (int i = 0; i < tagValueGlobalRules.length; i++) {
			TIntObjectHashMap<RenderingRule> rule = this.tagValueGlobalRules[i];
			if (rule != null) {
				TIntObjectHashMap<RenderingRule> newRule = new TIntObjectHashMap<>();
				newRule.putAll(rule);
				storage.tagValueGlobalRules[i] = newRule;
			}
		}
		storage.renderingAttributes.putAll(renderingAttributes);
		storage.renderingAssociations.putAll(renderingAssociations);
		return storage;
	}

	public int getInternalVersion() {
		return internalVersion;
	}

	public int getDictionaryValue(String val) {
		if (dictionaryMap.containsKey(val)) {
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

	public void parseRulesFromXmlInputStream(InputStream is, RenderingRulesStorageResolver resolver, boolean addon) throws XmlPullParserException,
			IOException {
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		RenderingRulesHandler handler = new RenderingRulesHandler(parser, resolver, addon);
		handler.parse(is);
		RenderingRulesStorage depends = handler.getDependsStorage();
		if (depends != null) {
			dependsName = depends.getName();
			mergeDependsOrAddon(depends);
		}
	}

	public void mergeDependsOrAddon(RenderingRulesStorage depends) {
		if (depends == null) {
			return;
		}
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
		it = depends.renderingAssociations.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, RenderingRule> e = it.next();
			if (renderingAssociations.containsKey(e.getKey())) {
				RenderingRule root = renderingAssociations.get(e.getKey());
				List<RenderingRule> list = e.getValue().getIfElseChildren();
				for (RenderingRule every : list) {
					root.addIfElseChildren(every);
				}
				e.getValue().addToBeginIfElseChildren(root);
			} else {
				renderingAssociations.put(e.getKey(), e.getValue());
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

	public static String colorToString(int color) {
		if ((0xFF000000 & color) == 0xFF000000) {
			return "#" + Integer.toHexString(color & 0x00FFFFFF); //$NON-NLS-1$
		} else {
			return "#" + Integer.toHexString(color); //$NON-NLS-1$
		}
	}

	private void registerGlobalRule(RenderingRule rr, int state, String tagS, String valueS, boolean addToBegin) throws XmlPullParserException {
		if (tagS == null || valueS == null) {
			throw new XmlPullParserException("Attribute tag should be specified for root filter " + rr.toString());
		}
		int key = getTagValueKey(tagS, valueS);
		RenderingRule insert = tagValueGlobalRules[state].get(key);
		if (insert != null) {
			// all root rules should have at least tag/value
			insert = createTagValueRootWrapperRule(key, insert);
			if (addToBegin) {
				insert.addToBeginIfElseChildren(rr);
			} else {
				insert.addIfElseChildren(rr);
			}
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
	
	private class XmlTreeSequence {
		XmlTreeSequence parent;
		String seqOrder;
		Map<String, String> attrsMap = new LinkedHashMap<String, String>();
		String name;
		List<XmlTreeSequence> children = new ArrayList<RenderingRulesStorage.XmlTreeSequence>();
		
		private void process(RenderingRulesHandler handler, int el) throws XmlPullParserException, IOException {
			Map<String, String> seqAttrsMap = new HashMap<String, String>(attrsMap);
			if (attrsMap.containsKey(SEQ_ATTR_KEY)) {
				attrsMap.remove(SEQ_ATTR_KEY);
			}
			
			for (Entry<String, String> attr: attrsMap.entrySet()) {
				if (attr.getValue().contains(SEQ_PLACEHOLDER)) {
					seqAttrsMap.put(attr.getKey(), attr.getValue().replace(SEQ_PLACEHOLDER, el+""));
				} else {
					seqAttrsMap.put(attr.getKey(), attr.getValue());
				}
			}
			handler.startElement(seqAttrsMap, name);
			for(XmlTreeSequence s : children) {
				s.process(handler, el);
			}
			handler.endElement(name);
		}
	}
	
	private class RenderingRulesHandler {
		private final XmlPullParser parser;
		private int state;
		Stack<RenderingRule> stack = new Stack<RenderingRule>();
		private final RenderingRulesStorageResolver resolver;
		private final boolean addon;
		private RenderingRulesStorage dependsStorage;
		
		public RenderingRulesHandler(XmlPullParser parser, RenderingRulesStorageResolver resolver,
									 boolean addon){
			this.parser = parser;
			this.resolver = resolver;
			this.addon = addon;
		}
		
		public void parse(InputStream is) throws XmlPullParserException, IOException {
			XmlPullParser parser = this.parser;
			Map<String, String> attrsMap = new LinkedHashMap<String, String>();
			parser.setInput(is, "UTF-8");
			int tok;
			XmlTreeSequence currentSeqElement = null;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					attrsMap.clear();
					parseAttributes(parser, attrsMap);
					String name = parser.getName();
					if (!Algorithms.isEmpty(parser.getAttributeValue("", SEQ_ATTR_KEY)) || currentSeqElement != null) {
						XmlTreeSequence seq = new XmlTreeSequence();
						seq.name = name;
						seq.attrsMap = new HashMap<String, String>(attrsMap);
						seq.parent = currentSeqElement;
						if (currentSeqElement == null) {
							seq.seqOrder = parser.getAttributeValue("", SEQ_ATTR_KEY);
						} else {
							currentSeqElement.children.add(seq);
							seq.seqOrder = currentSeqElement.seqOrder;
						}
						currentSeqElement = seq;
					} else {
						startElement(attrsMap, name);
					}
				} else if (tok == XmlPullParser.END_TAG) {
					if(currentSeqElement == null) {
						endElement(parser.getName());
					} else {
						XmlTreeSequence process = currentSeqElement;
						currentSeqElement = currentSeqElement.parent;
						if (currentSeqElement == null) {
							// Here we process sequence element
							int seqEnd = Integer.parseInt(process.seqOrder.substring(process.seqOrder.indexOf(':') + 1, process.seqOrder.length()));
							for(int i = 1; i < seqEnd; i++) {
								process.process(this, i);
							}
						}
					}
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
		
		public void startElement(Map<String, String> attrsMap, String name) throws XmlPullParserException, IOException {
			final boolean isCase = isCase(name);
			final boolean isSwitch = isSwitch(name);
			if(isCase || isSwitch){ //$NON-NLS-1$
				boolean top = stack.size() == 0 || isTopCase();
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
			} else if("text".equals(name)){ //$NON-NLS-1$
				state = TEXT_RULES;
			} else if("point".equals(name)){ //$NON-NLS-1$
				state = POINT_RULES;
			} else if("line".equals(name)){ //$NON-NLS-1$
				state = LINE_RULES;
			} else if("polygon".equals(name)){ //$NON-NLS-1$
				state = POLYGON_RULES;
			} else if("renderingAttribute".equals(name)){ //$NON-NLS-1$
				String attr = attrsMap.get("name");
				RenderingRule root = new RenderingRule(new HashMap<String, String>(), false, RenderingRulesStorage.this);
				if (renderingAttributes.containsKey(attr)) {
					renderingAttributes.get(attr).addIfElseChildren(root);
				} else {
					renderingAttributes.put(attr, root);
				}
				stack.push(root);
			} else if("renderingAssociation".equals(name)){ //$NON-NLS-1$
				String attr = attrsMap.get("name");
				RenderingRule root = new RenderingRule(new HashMap<String, String>(), false, RenderingRulesStorage.this);
				if (renderingAssociations.containsKey(attr)) {
					renderingAssociations.get(attr).addIfElseChildren(root);
				} else {
					renderingAssociations.put(attr, root);
				}
				stack.push(root);
			} else if("renderingProperty".equals(name)){ //$NON-NLS-1$
				String attr = attrsMap.get("attr");
				RenderingRuleProperty prop;
				String type = attrsMap.get("type");
				if("boolean".equalsIgnoreCase(type)){
					prop = RenderingRuleProperty.createInputBooleanProperty(attr);
				} else if("string".equalsIgnoreCase(type)){
					prop = RenderingRuleProperty.createInputStringProperty(attr);
				} else {
					prop = RenderingRuleProperty.createInputIntProperty(attr);
				}
				prop.setDescription(attrsMap.get("description"));
				prop.setDefaultValueDescription(attrsMap.get("defaultValueDescription"));
				prop.setCategory(attrsMap.get("category"));
				prop.setName(attrsMap.get("name"));
				if (attrsMap.get("possibleValues") != null) {
					prop.setPossibleValues(attrsMap.get("possibleValues").split(","));
				}
				PROPS.registerRule(prop);
			} else if("renderingConstant".equals(name)){ //$NON-NLS-1$
				if(!renderingConstants.containsKey(attrsMap.get("name"))){
					renderingConstants.put(attrsMap.get("name"), attrsMap.get("value"));
				}
			} else if ("renderingStyle".equals(name)) { //$NON-NLS-1$
				if (!addon) {
					String depends = attrsMap.get("depends");
					if (depends != null && depends.length() > 0) {
						this.dependsStorage = resolver.resolve(depends, resolver);
					}
					if (dependsStorage != null) {
						// copy dictionary
						dictionary = new ArrayList<String>(dependsStorage.dictionary);
						dictionaryMap = new LinkedHashMap<String, Integer>(dependsStorage.dictionaryMap);
						PROPS = new RenderingRuleStorageProperties(dependsStorage.PROPS);
					}
					internalRenderingName = attrsMap.get("name");
				}
			} else if ("renderer".equals(name)) { //$NON-NLS-1$
				throw new XmlPullParserException("Rendering style is deprecated and no longer supported.");
			} else {
				log.warn("Unknown tag : " + name); //$NON-NLS-1$
			}

			if (tagValueGlobalRules[state] == null) {
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
		
		private Map<String, String> parseAttributes(XmlPullParser parser, Map<String, String> m) {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				String name = parser.getAttributeName(i);
				String vl = parser.getAttributeValue(i);
				if (vl != null && vl.startsWith("$")) {
					String cv = vl.substring(1);
					if (!renderingConstants.containsKey(cv) && !renderingAttributes.containsKey(cv) && !renderingAssociations.containsKey(cv)) {
						throw new IllegalStateException("Rendering constant or attribute '" + cv + "' was not specified.");
					}
					if (renderingConstants.containsKey(cv)) {
						vl = renderingConstants.get(cv);
					}
				}
				m.put(name, vl);
			}
			return m;
		}


		public void endElement(String name) throws XmlPullParserException {
			if (isCase(name) || isSwitch(name)) {
				RenderingRule renderingRule = (RenderingRule) stack.pop();
				if (stack.size() == 0) {
					registerTopLevel(renderingRule, null, Collections.EMPTY_MAP, state, false);
				}
			} else if (isApply(name)) {
				stack.pop();
			} else if ("renderingAttribute".equals(name)) { //$NON-NLS-1$
				stack.pop();
			} else if ("renderingAssociation".equals(name)) { //$NON-NLS-1$
				stack.pop();
			}
		}
	}

	public void registerTopLevel(RenderingRule renderingRule,
								 List<RenderingRule> applyRules,
								 Map<String, String> attrs,
								 int state,
								 boolean addToBegin) throws XmlPullParserException {
		if (renderingRule.isGroup()
				&& (renderingRule.getIntPropertyValue(RenderingRuleStorageProperties.TAG) == -1
				|| renderingRule.getIntPropertyValue(RenderingRuleStorageProperties.VALUE) == -1)) {
			List<RenderingRule> caseChildren = renderingRule.getIfElseChildren();
			for (RenderingRule ch : caseChildren) {
				List<RenderingRule> apply = applyRules;
				if (!renderingRule.getIfChildren().isEmpty()) {
					apply = new ArrayList<>(renderingRule.getIfChildren());
					if (applyRules != null) {
						apply.addAll(applyRules);
					}
				}
				Map<String, String> cattrs = new HashMap<String, String>(attrs);
				cattrs.putAll(renderingRule.getAttributes());
				registerTopLevel(ch, apply, cattrs, state, addToBegin);
			}
		} else {
			HashMap<String, String> ns = new HashMap<String, String>(attrs);
			ns.putAll(renderingRule.getAttributes());
			String tg = ns.remove("tag");
			String vl = ns.remove("value");
			// reset rendering rule attributes
			renderingRule.init(ns);
			if (STORE_ATTRIBUTES) {
				renderingRule.storeAttributes(ns);
			}

			registerGlobalRule(renderingRule, state, tg, vl, addToBegin);
			if (applyRules != null) {
				for (RenderingRule apply : applyRules) {
					renderingRule.addIfChildren(apply);
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
		if (tagValueGlobalRules[state] != null){
			return tagValueGlobalRules[state].get((itag << SHIFT_TAG_VAL) | ivalue);
		}
		return null;
	}

	public RenderingRule getRule(int state, int key) {
		if (tagValueGlobalRules[state] != null) {
			return tagValueGlobalRules[state].get(key);
		}
		return null;
	}

	public RenderingRule getRenderingAttributeRule(String attribute) {
		return renderingAttributes.get(attribute);
	}
	
	public String[] getRenderingAttributeNames() {
		return renderingAttributes.keySet().toArray(new String[0]);
	}
	public RenderingRule[] getRenderingAttributeValues() {
		return renderingAttributes.values().toArray(new RenderingRule[0]);
	}

	public RenderingRule getRenderingAssociationRule(String association) {
		return renderingAssociations.get(association);
	}

	public String[] getRenderingAssociationNames() {
		return renderingAssociations.keySet().toArray(new String[0]);
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
		if (tagValueGlobalRules[state] != null) {
			for (int key : tagValueGlobalRules[state].keys()) {
				RenderingRule rr = tagValueGlobalRules[state].get(key);
				out.print("\n\n" + getTagString(key) + " : " + getValueString(key) + "\n ");
				printRenderingRule(" ", rr, out);
			}
		}
	}
	
	private static void printRenderingRule(String indent, RenderingRule rr, PrintStream out){
		out.print(rr.toString(indent, new StringBuilder()).toString());
	}

	public static void main(String[] args) throws XmlPullParserException, IOException {
		STORE_ATTRIBUTES = true;
		final File styleFile;
		final String styleName;
		InputStream defaultIS;
		if (args.length > 0) {
			styleFile = new File(args[0]);
			styleName = styleFile.getName().substring(0, styleFile.getName().indexOf('.'));
			defaultIS = RenderingRulesStorage.class.getResourceAsStream("default.render.xml");
		} else {
			File stylesDir = new File(System.getProperty("repo.dir") + "/resources/rendering_styles/");
			defaultIS = new FileInputStream(new File(stylesDir, "default.render.xml"));
//			styleName = "default";
			styleName = "test";
			styleFile = new File(stylesDir, styleName +".render.xml");
		}
		final Map<String, String> renderingConstants = new LinkedHashMap<String, String>();
		
		try {
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(defaultIS, "UTF-8");
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
			defaultIS.close();
		}
		RenderingRulesStorage storage = new RenderingRulesStorage(styleName, renderingConstants);
		final RenderingRulesStorageResolver resolver = new RenderingRulesStorageResolver() {
			@Override
			public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws XmlPullParserException, IOException {
				RenderingRulesStorage depends = new RenderingRulesStorage(name, renderingConstants);
//				depends.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream(name + ".render.xml"), ref);
				depends.parseRulesFromXmlInputStream(
						new FileInputStream(new File(styleFile.getParentFile(), name + ".render.xml")), ref, false);
				return depends;
			}
		};
		
		defaultIS = new FileInputStream(styleFile);
		storage.parseRulesFromXmlInputStream(defaultIS, resolver, false);
		if (styleFile.getParentFile() != null) {
			for (File file : styleFile.getParentFile().listFiles()) {
				if (file.isFile() && file.getName().endsWith("addon.render.xml")) {
					InputStream is3 = new FileInputStream(file);
					storage.parseRulesFromXmlInputStream(is3, resolver, true);
					is3.close();
				}
			}
		}
		
//		storage = new RenderingRulesStorage("", null);
//		new DefaultRenderingRulesStorage().createStyle(storage);
//		for (RenderingRuleProperty p :  storage.PROPS.getCustomRules()) {
//			System.out.println(p.getCategory() + " " + p.getName() + " " + p.getAttrName());
//		}
		printAllRules(storage);
		testSearch(storage);
	}

	protected static void testSearch(RenderingRulesStorage storage) {
		//		long tm = System.nanoTime();
		//		int count = 100000;
		//		for (int i = 0; i < count; i++) {
					RenderingRuleSearchRequest searchRequest = new RenderingRuleSearchRequest(storage);
					searchRequest.setStringFilter(storage.PROPS.R_TAG, "highway");
					searchRequest.setStringFilter(storage.PROPS.R_VALUE, "primary");
//					searchRequest.setStringFilter(storage.PROPS.R_ADDITIONAL, "contourtype=10m");
//					 searchRequest.setIntFilter(storage.PROPS.R_LAYER, 1);
					searchRequest.setIntFilter(storage.PROPS.R_MINZOOM, 15);
					searchRequest.setIntFilter(storage.PROPS.R_MAXZOOM, 15);
//						searchRequest.setBooleanFilter(storage.PROPS.R_NIGHT_MODE, true);
//					searchRequest.setStringFilter(storage.PROPS.get("contourColorScheme"), "yellow");
//					searchRequest.setStringFilter(storage.PROPS.get("contourLines"), "15");
					
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

	public static RenderingRulesStorage getTestStorageForStyle(String filePath) throws XmlPullParserException, IOException  {
		RenderingRulesStorage.STORE_ATTRIBUTES = true;
		Map<String, String> renderingConstants = new LinkedHashMap<String, String>();

		InputStream is = new FileInputStream(filePath);
		// buggy attributes
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
		is = new FileInputStream(filePath);
		RenderingRulesStorage storage = new RenderingRulesStorage("default", renderingConstants);
		final RenderingRulesStorageResolver resolver = new RenderingRulesStorageResolver() {
			@Override
			public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws XmlPullParserException, IOException {
				RenderingRulesStorage depends = new RenderingRulesStorage(name, null);
				depends.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream(name + ".render.xml"), ref, false);
				return depends;
			}
		};
		storage.parseRulesFromXmlInputStream(is, resolver, false);
		return storage;
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

	@Override
	public String toString() {
		return getName();
	}
}
