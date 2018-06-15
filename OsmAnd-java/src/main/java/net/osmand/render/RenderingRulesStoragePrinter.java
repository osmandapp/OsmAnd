package net.osmand.render;

import gnu.trove.iterator.TIntObjectIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.PlatformUtil;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RenderingRulesStoragePrinter {
	
	public static void main(String[] args) throws XmlPullParserException, IOException {
		RenderingRulesStorage.STORE_ATTTRIBUTES = true;
//		InputStream is = RenderingRulesStorage.class.getResourceAsStream("default.render.xml");
		String defaultFile = "/Users/victorshcherb/osmand/repos/resources/rendering_styles/default.render.xml";
		if(args.length > 0) {
			defaultFile = args[0];
		}
		String outputPath = ".";
		if(args.length > 1) {
			outputPath = args[1];
		}
		String name = "Style";
		Map<String, String> renderingConstants = new LinkedHashMap<String, String>();
		InputStream is = new FileInputStream(defaultFile);
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
		is = new FileInputStream(defaultFile);
		RenderingRulesStorage storage = new RenderingRulesStorage("default", renderingConstants);
		final RenderingRulesStorageResolver resolver = new RenderingRulesStorageResolver() {
			@Override
			public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws XmlPullParserException, IOException {
				RenderingRulesStorage depends = new RenderingRulesStorage(name, null);
				depends.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream(name + ".render.xml"), ref);
				return depends;
			}
		};
		storage.parseRulesFromXmlInputStream(is, resolver);
		new RenderingRulesStoragePrinter().printJavaFile(outputPath, name, storage);
	
	}
	
	protected void printJavaFile(String path, String name, RenderingRulesStorage storage) throws IOException {
		PrintStream out = System.out;
		out = new PrintStream(new File(path, name + "RenderingRulesStorage.java"));
		out.println("\n\npackage net.osmand.render;\n\npublic class " + name + "RenderingRulesStorage {");
		String defindent = "\t";
		String indent = defindent;
		out.println("" + indent + defindent + "RenderingRulesStorage storage;");

		out.println("\tprivate java.util.Map<String, String> createMap(int... attrs) {\n"
				+ "\t	java.util.Map<String, String> mp = new java.util.HashMap<String, String>();\n"
				+ "\t		for(int i = 0; i< attrs.length; i+=2) {\n"
				+ "\t			mp.put(storage.getStringValue(attrs[i]), storage.getStringValue(attrs[i+1]));\n" + "\t		}\n"
				+ "\t	return mp;\n" + "\t}");

		out.println("\tprivate java.util.Map<String, String> createMap(String... attrs) {\n"
				+ "\t	java.util.Map<String, String> mp = new java.util.HashMap<String, String>();\n"
				+ "\t		for(int i = 0; i< attrs.length; i+=2) {\n" + "\t			mp.put(attrs[i], attrs[i+1]);\n" + "\t		}\n"
				+ "\t	return mp;\n" + "\t}");

		out.println("\n" + indent + "public void createStyle(RenderingRulesStorage storage) {");
		out.println("" + indent + defindent + "this.storage=storage;");
		out.println("" + indent + defindent + "storage.renderingName=" + javaString(storage.renderingName) + ";");
		out.println("" + indent + defindent + "storage.internalRenderingName="
				+ javaString(storage.internalRenderingName) + ";");
		// init dictionary must be first here
		out.println("" + indent + defindent + "initDictionary();");
		out.println("" + indent + defindent + "initProperties();");
		out.println("" + indent + defindent + "initConstants();");
		out.println("" + indent + defindent + "initAttributes();");
		out.println("" + indent + defindent + "initRules();");
		out.println("" + indent + "}");
		printJavaInitConstants(storage, out, indent, defindent);
		printJavaInitProperties(storage, out, indent, defindent);
		printJavaInitRules(storage, out, indent, defindent);
		printJavaInitAttributes(storage, out, indent, defindent);
		// PRINT last one in order to initialize storage properly
		printJavaInitDictionary(storage, out, indent, defindent);

		out.println("\n\n}");
	}
	
	
	private String javaString(String s) {
		return "\""+s+"\"";
	}

	private void printJavaInitDictionary(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
		out.println("\n" + indent + "public void initDictionary() {");
		int i = 0;
		for(String s : storage.dictionary) {
			out.println(""+indent + ti +"storage.getDictionaryValue("+ javaString(s) + ");  // " + i++);
		}
		out.println(""+indent +"}");
	}
	
	private void printJavaInitProperties(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
		out.println("\n" + indent + "public void initProperties() {");
		out.println("" + indent + ti + "RenderingRuleProperty prop = null;");
		for(RenderingRuleProperty p : storage.PROPS.customRules) {
			out.println("" + indent + ti + "prop = new RenderingRuleProperty("+javaString(p.attrName)+
					"," +p.type+", "+p.input+");");
			out.println("" + indent + ti + "prop.setDescription("+javaString(p.description)+");");
			out.println("" + indent + ti + "prop.setCategory("+javaString(p.category)+");");
			out.println("" + indent + ti + "prop.setName("+javaString(p.name)+");");
			if(p.possibleValues != null && !p.isBoolean()) {
				String mp = "";
				for (String s : p.possibleValues) {
					if (mp.length() > 0) {
						mp += ", ";
					}
					mp += javaString(s);
				}
				out.println("" + indent + ti + "prop.setPossibleValues(new String[]{"+mp+"});");
			}
			out.println("" + indent + ti + "storage.PROPS.registerRule(prop);");
		}
		out.println(""+indent +"}");
	}
	
	private void printJavaInitAttributes(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
		out.println("\n" + indent + "public void initAttributes() {");
		for (int i = 0; i < 15; i++) {
			out.println("" + indent + ti + "RenderingRule rule" + i + " = null;");
		}
		for (String s : storage.renderingAttributes.keySet()) {
			generateRenderingRule(storage, out, indent + ti, "rule", 0, storage.renderingAttributes.get(s));
			out.println("" + indent + ti + "storage.renderingAttributes.put(" + javaString(s) + ", rule0);");
		}
		out.println(""+indent +"}");
	}
	
	
	private void printJavaInitRules(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
		int javaFunctions = 0;
		boolean initNewSection = true;
		for (int rulesSection = 0; rulesSection < RenderingRulesStorage.LENGTH_RULES; rulesSection++) {
			initNewSection = true;
			if(storage.tagValueGlobalRules[rulesSection] == null) {
				continue;
			}
			TIntObjectIterator<RenderingRule> iterator = storage.tagValueGlobalRules[rulesSection].iterator();
			int rulesInSection = 0;
			while(iterator.hasNext()) {
				iterator.advance();
				if (initNewSection) {
					if (javaFunctions > 0) {
						out.println("" + indent + "}\n");
					}
					out.println("\n" + indent + "public void initRules"+javaFunctions+"() {");
					for (int k = 0; k < 15; k++) {
						out.println("" + indent + ti + "RenderingRule rule" + k + " = null;");
					}
					initNewSection = false;
					javaFunctions++;
				}
				if(rulesInSection > 50) {
					rulesInSection = 0;
					initNewSection = true;
				}
				rulesInSection += generateRenderingRule(storage, out, indent + ti, "rule", 0, iterator.value());
				out.println("" + indent + ti + "storage.tagValueGlobalRules["+rulesSection+"].put("+iterator.key()+", rule0);");
			}
		}
		if (javaFunctions > 0) {
			out.println("" + indent + "}\n");
		}
		out.println("\n" + indent + "public void initRules() {");
		for (int k = 0; k < RenderingRulesStorage.LENGTH_RULES; k++) {
			if(storage.tagValueGlobalRules[k] == null) {
				continue;
			}
			out.println("" + indent + ti + "storage.tagValueGlobalRules["+k+"] = new gnu.trove.map.hash.TIntObjectHashMap();");
		}
		for(int i = 0; i < javaFunctions; i++) {
			out.println("" + indent + ti + "initRules"+i+"();");
		}
		out.println(""+indent +"}");
	}
	
	private int generateRenderingRule(RenderingRulesStorage storage, PrintStream out, String indent, String name, int ind, RenderingRule key) {
		int cnt = 1;
		String mp = "";
		Iterator<Entry<String, String>> it = key.getAttributes().entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, String> e = it.next();
//			mp += javaString(e.getKey()) + ", " + javaString(e.getValue());
			int kk = storage.getDictionaryValue(e.getKey());
			int vv = storage.getDictionaryValue(e.getValue());
			mp += kk +", " +vv;
			if(it.hasNext()) {
				mp+=", ";
			}
		}
		if(mp.equals("")) {
			mp = "java.util.Collections.EMPTY_MAP";
		} else {
			mp = "createMap(" +mp +")";
		}
		out.println("" + indent + name + ind +" = new RenderingRule("+mp +", "+ key.isGroup() + ",  storage);");
		for (RenderingRule k : key.getIfChildren()) {
			generateRenderingRule(storage, out, indent + "\t", name, ind + 1, k);
			out.println("" + indent + name + ind + ".addIfChildren(" + name + (ind + 1) + ");");
			cnt++;
		}
		for (RenderingRule k : key.getIfElseChildren()) {
			generateRenderingRule(storage, out, indent + "\t", name, ind + 1, k);
			out.println("" + indent + name + ind + ".addIfElseChildren(" + name + (ind + 1) + ");");
			cnt++;
		}
		return cnt;
	}

	private void printJavaInitConstants(RenderingRulesStorage storage, PrintStream out, String indent, String ti) {
		out.println("\n" + indent + "public void initConstants() {");
		for (String s : storage.renderingConstants.keySet()) {
			out.println("" + indent + ti + "storage.renderingConstants.put(" + javaString(s) + ", "
					+ javaString(storage.renderingConstants.get(s)) + ");");
		}
		out.println(""+indent +"}");
	}

}
