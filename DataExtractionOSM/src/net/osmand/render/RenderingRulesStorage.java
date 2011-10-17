package net.osmand.render;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.osmand.LogUtil;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.render.OsmandRenderingRulesParser.EffectAttributes;
import net.osmand.render.OsmandRenderingRulesParser.FilterState;
import net.osmand.render.OsmandRenderingRulesParser.RenderingRuleVisitor;
import net.osmand.render.OsmandRenderingRulesParser.SwitchState;
import net.osmand.render.OsmandRenderingRulesParser.TextAttributes;

public class RenderingRulesStorage {

	private final static Log log = LogUtil.getLog(RenderingRulesStorage.class);
	
	public final static int POINT_STATE = 1;
	public final static int LINE_STATE = 2;
	public final static int POLYGON_STATE = 3;
	public final static int TEXT_STATE = 4;
	public final static int ORDER_STATE = 5;
	
	List<String> dictionary = new ArrayList<String>();
	Map<String, Integer> dictionaryMap = new LinkedHashMap<String, Integer>();
	final Map<String, RenderingRuleProperty> properties;
	
	private int bgColor = 0;
	private int bgNightColor = 0;
	private String renderingName;
	private String depends;
	
	
	public RenderingRulesStorage(){
		properties = DefaultRenderingRuleProperties.createDefaultRenderingRuleProperties();
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
	
	public RenderingRuleProperty getProperty(String name){
		return properties.get(name);
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
	
	private class RenderingRulesHandler extends DefaultHandler {
		private final SAXParser parser;
		private int state;

		Stack<Object> stack = new Stack<Object>();
		
		
		public RenderingRulesHandler(SAXParser parser){
			this.parser = parser;
		}
		
		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			name = parser.isNamespaceAware() ? localName : name;
			if("filter".equals(name)){ //$NON-NLS-1$
				FilterState st = parseFilterAttributes(attributes);
				stack.push(st);
			} else if("order".equals(name)){ //$NON-NLS-1$
				state = ORDER_STATE;
			} else if("text".equals(name)){ //$NON-NLS-1$
				state = TEXT_STATE;
			} else if("point".equals(name)){ //$NON-NLS-1$
				state = POINT_STATE;
			} else if("line".equals(name)){ //$NON-NLS-1$
				state = LINE_STATE;
			} else if("polygon".equals(name)){ //$NON-NLS-1$
				state = POLYGON_STATE;
			} else if("switch".equals(name)){ //$NON-NLS-1$
				SwitchState st = new SwitchState();
				stack.push(st);
			} else if("case".equals(name)){ //$NON-NLS-1$
				FilterState st = parseFilterAttributes(attributes);
				((SwitchState)stack.peek()).filters.add(st);
			} else if("renderer".equals(name)){ //$NON-NLS-1$
				String dc = attributes.getValue("defaultColor");
				int defaultColor = 0;
				if(dc != null && dc.length() > 0){
					bgColor = RenderingRuleProperty.parseColor(dc);
				}
				String dnc = attributes.getValue("defaultNightColor");
				int defautNightColor = defaultColor;
				if(dnc != null && dnc.length() > 0){
					bgNightColor = RenderingRuleProperty.parseColor(dnc);
				}
				renderingName = attributes.getValue("name");
				depends = attributes.getValue("depends");
			} else {
				log.warn("Unknown tag" + name); //$NON-NLS-1$
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			name = parser.isNamespaceAware() ? localName : name;
			if ("filter".equals(name)) { //$NON-NLS-1$
				
			} else if("switch".equals(name)){ //$NON-NLS-1$
				stack.pop();
			}
		}
		
		
		
	}
}
