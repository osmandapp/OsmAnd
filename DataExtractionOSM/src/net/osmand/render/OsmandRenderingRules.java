package net.osmand.render;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.osmand.LogUtil;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OsmandRenderingRules {

	
	private final static Log log = LogUtil.getLog(OsmandRenderingRules.class);
	
	public static class EffectAttributes {
		public int color = 0;
		public float strokeWidth = 0;
		public String pathEffect = null;
		public int shadowColor = 0;
		public float shadowRadius = 0;
		public String cap = null;
		
	}
	
	public static class TextAttributes {
		public int textColor = 0;
		public float textSize = 0;
		public boolean textBold = false;
		public String textShield = null;
		public int textMinDistance = 0;
		public boolean textOnPath = false;
		public int textWrapWidth = 0;
		public float textHaloRadius = 0;
		public int textDy = 0;
		public String ref = null;
	}
	
	protected static class SwitchState {
		List<FilterState> filters = new ArrayList<FilterState>();
	}
	
	public static class FilterState {
		public int minzoom = -1;
		public int maxzoom = -1;
		public String tag = null;
		public String val = null;
		public int layer = 0;
		public int textLength = 0;
		
		public String shader = null;
		
		// point
		public String icon = null;
		
		public EffectAttributes main = new EffectAttributes();
		public TextAttributes text = null;
		public List<EffectAttributes> effectAttributes = new ArrayList<EffectAttributes>(3);
		
		protected EffectAttributes getEffectAttributes(int i){
			while(i >= effectAttributes.size()){
				effectAttributes.add(new EffectAttributes());
			}
			return effectAttributes.get(i);
		}
		
		
	}
	
	
	private final static int POINT_STATE = 1;
	private final static int POLYGON_STATE = 2;
	private final static int LINE_STATE = 3;
	private final static int TEXT_STATE = 4;
	public void parseRenderingRules(InputStream is) throws IOException, SAXException {
		try {
			final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(is, new RenderingRulesHandler(saxParser));
		} catch (ParserConfigurationException e) {
			throw new SAXException(e);
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
			} else {
//				System.err.println("Unknown tag " + name);
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			name = parser.isNamespaceAware() ? localName : name;
			if ("filter".equals(name)) { //$NON-NLS-1$
				List<FilterState> list = popAndAggregateState();
				for (FilterState pop : list) {
					if (pop.tag != null && pop.minzoom != -1) {
						String gen = generateAttributes(pop);
						if (gen != null) {
							String res = "";
							if (pop.maxzoom != -1) {
								res += " zoom : " +pop.minzoom + "-" + pop.maxzoom;
							} else {
								res += " zoom : " +pop.minzoom;
							}
							res += " tag="+pop.tag;
							res += " val="+pop.val;
							if(pop.layer != 0){
								res += " layer="+pop.layer;
							}
							
							res += gen;
							System.out.println(res);
						}
					}
				}
			} else if("switch".equals(name)){
				stack.pop();
			}
		}
		
		private String generateAttributes(FilterState s){
			String res = "";
			if(s.shader != null){
				res+=" shader=" + s.shader;
			}
			if(s.main.color != 0){
				res +=" color="+colorToString(s.main.color);
			}
			if(s.icon != null){
				res+= " icon="+s.icon;
			}
			if(s.main.strokeWidth != 0){
				res+= " strokeWidth="+s.main.strokeWidth;
			}
			if(s.main.pathEffect != null){
				res+= " pathEffect="+s.main.pathEffect;
			}
			if(state == POLYGON_STATE){
				return null;
//				if(s.shader != null){
//					return " shader=" + s.shader;
//				}
//				return " color=" + colorToString(s.main.color);
//			} else if(state == POINT_STATE){
//				return " icon=" + s.icon;
			} else if(state == LINE_STATE){
				return res;
			} else {
				return null;
			}
		}
		
		public List<FilterState> popAndAggregateState() {
			FilterState pop = (FilterState) stack.pop();
			List<FilterState> res = null;
			for (int i = stack.size() - 1; i >= 0; i--) {
				Object o = stack.get(i);
				if(o instanceof FilterState){
					if(res == null){
						mergeStateInto((FilterState) o, pop);
					} else {
						for(FilterState f : res){
							mergeStateInto((FilterState) o, f);
						}
					}
				} else {
					List<FilterState> filters = ((SwitchState)o).filters;
					if(res == null){
						res =new ArrayList<FilterState>();
						res.add(pop);
					}
					int l = res.size();
					for (int t = 0; t < filters.size() - 1; t++) {
						for (int j = 0; j < l; j++) {
							FilterState n = new FilterState();
							mergeStateInto(res.get(j), n);
							res.add(n);
						}
					}
					for (int j = 0; j < res.size(); j++) {
						mergeStateInto(filters.get(j % filters.size()), res.get(j));
					}
				}
				
			}
			if(res == null){
				return Collections.singletonList(pop);
			} else {
				return res;
			}
		}
		
		public void mergeStateInto(FilterState toMerge, FilterState mergeInto){
			if(toMerge.maxzoom != -1 && mergeInto.maxzoom == -1){
				mergeInto.maxzoom = toMerge.maxzoom;
			}
			
			if(toMerge.minzoom != -1 && mergeInto.minzoom == -1){
				mergeInto.minzoom = toMerge.minzoom;
			}
			if(toMerge.icon != null && mergeInto.icon == null){
				mergeInto.icon = toMerge.icon;
			}
			if(toMerge.tag != null && mergeInto.tag == null){
				mergeInto.tag = toMerge.tag;
			}
			if(toMerge.layer != 0 && mergeInto.layer == 0){
				mergeInto.layer = toMerge.layer;
			}
			if(toMerge.textLength != 0 && mergeInto.textLength == 0){
				mergeInto.textLength = toMerge.textLength;
			}
			if(toMerge.val != null && mergeInto.val == null){
				mergeInto.val = toMerge.val;
			}
			if(toMerge.text != null){
				if(mergeInto.text == null){
					mergeInto.text = new TextAttributes();
				}
				if(toMerge.text.textColor != 0 && mergeInto.text.textColor == 0){
					mergeInto.text.textColor = toMerge.text.textColor;
				}
				if(toMerge.text.textSize != 0 && mergeInto.text.textSize == 0){
					mergeInto.text.textSize = toMerge.text.textSize;
				}
				if(toMerge.text.textBold && !mergeInto.text.textBold){
					mergeInto.text.textBold = toMerge.text.textBold;
				}
				if(toMerge.text.textShield != null && mergeInto.text.textShield == null){
					mergeInto.text.textShield = toMerge.text.textShield;
				}
				
				if(toMerge.text.textMinDistance != 0 && mergeInto.text.textMinDistance == 0){
					mergeInto.text.textMinDistance = toMerge.text.textMinDistance;
				}
				if(toMerge.text.textDy != 0 && mergeInto.text.textDy == 0){
					mergeInto.text.textDy = toMerge.text.textDy;
				}
				if(toMerge.text.textHaloRadius != 0 && mergeInto.text.textHaloRadius == 0){
					mergeInto.text.textHaloRadius = toMerge.text.textHaloRadius;
				}
				if(toMerge.text.textWrapWidth != 0 && mergeInto.text.textWrapWidth == 0){
					mergeInto.text.textWrapWidth = toMerge.text.textWrapWidth;
				}
				if(toMerge.text.textOnPath && !mergeInto.text.textOnPath){
					mergeInto.text.textOnPath = toMerge.text.textOnPath;
				}
			}
			
			mergeStateInto(toMerge.main, mergeInto.main);
			while(mergeInto.effectAttributes.size() < toMerge.effectAttributes.size()){
				mergeInto.effectAttributes.add(new EffectAttributes());
			}
			for(int i=0; i<toMerge.effectAttributes.size(); i++){
				mergeStateInto(toMerge.effectAttributes.get(i), mergeInto.effectAttributes.get(i));
			}
		}
		
		public void mergeStateInto(EffectAttributes toMerge, EffectAttributes mergeInto){
			if(toMerge.color != 0 && mergeInto.color == 0){
				mergeInto.color = toMerge.color;
			}
			
			if(toMerge.strokeWidth != 0 && mergeInto.strokeWidth == 0){
				mergeInto.strokeWidth = toMerge.strokeWidth;
			}
			
			if(toMerge.pathEffect != null && mergeInto.pathEffect == null){
				mergeInto.pathEffect = toMerge.pathEffect;
			}
			if(toMerge.shadowRadius != 0 && mergeInto.shadowRadius == 0){
				mergeInto.shadowRadius = toMerge.shadowRadius;
			}
			
			if(toMerge.shadowColor != 0 && mergeInto.shadowColor == 0){
				mergeInto.shadowColor = toMerge.shadowColor;
			}
			if(toMerge.cap != null && mergeInto.cap == null){
				mergeInto.cap = toMerge.cap;
			}
		}
		
		public FilterState parseFilterAttributes(Attributes attributes){
			FilterState state = new FilterState();
			if(this.state == TEXT_STATE){
				state.text = new TextAttributes();
			}
			for(int i=0; i<attributes.getLength(); i++){
				String name = attributes.getLocalName(i);
				String val = attributes.getValue(i);
				if(name.equals("tag")){
					state.tag = val;
				} else if(name.equals("value")){
					state.val = val;
				} else if(name.equals("minzoom")){
					state.minzoom = Integer.parseInt(val);
				} else if(name.equals("maxzoom")){
					state.maxzoom = Integer.parseInt(val);
				} else if(name.equals("maxzoom")){
					state.maxzoom = Integer.parseInt(val);
				} else if(name.equals("layer")){
					state.layer = Integer.parseInt(val);
				} else if(name.equals("icon")){
					state.icon = val;
				} else if(name.equals("color")){
					state.main.color = parseColor(val);
				} else if(name.startsWith("color_")){
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(6)));
					ef.color = parseColor(val);
				} else if(name.equals("shader")){
					state.shader = val;
				} else if(name.equals("strokeWidth")){
					state.main.strokeWidth = Float.parseFloat(val);
				} else if(name.startsWith("strokeWidth_")){
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(12)));
					ef.strokeWidth = Float.parseFloat(val);
				} else if(name.equals("pathEffect")){
					state.main.pathEffect = val;
				} else if(name.startsWith("pathEffect_")){
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(11)));
					ef.pathEffect = val;
				} else if(name.equals("shadowRadius")){
					state.main.shadowRadius = Float.parseFloat(val);
				} else if(name.startsWith("shadowRadius_")){
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(14)));
					ef.shadowRadius = Float.parseFloat(val);
				} else if(name.equals("shadowColor")){
					state.main.shadowColor = parseColor(val);
				} else if(name.startsWith("shadowColor_")){
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(12)));
					ef.shadowColor = parseColor(val);
				} else if(name.equals("cap")){
					state.main.cap = val;
				} else if(name.startsWith("cap_")){
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(4)));
					ef.cap = val;
				} else if(name.equals("ref")){
					state.text.ref = val;
				} else if(name.equals("textSize")){
					state.text.textSize = Float.parseFloat(val);
				} else if(name.equals("textBold")){
					state.text.textBold = Boolean.parseBoolean(val);
				} else if(name.equals("textColor")){
					state.text.textColor = parseColor(val);
				} else if(name.equals("textLength")){
					state.textLength = Integer.parseInt(val);
				} else if(name.equals("textShield")){
					state.text.textShield = val;
				} else if(name.equals("textMinDistance")){
					state.text.textMinDistance = Integer.parseInt(val);
				} else if(name.equals("textOnPath")){
					state.text.textOnPath = Boolean.parseBoolean(val);
				} else if(name.equals("textWrapWidth")){
					state.text.textWrapWidth = Integer.parseInt(val);
				} else if(name.equals("textDy")){
					state.text.textDy = Integer.parseInt(val);
				} else if(name.equals("textHaloRadius")){
					state.text.textHaloRadius = Float.parseFloat(val);
				} else {
					log.warn("Unknown attribute " + name);
				}
			}
			return state;
		}
	}

	/**
     * Parse the color string, and return the corresponding color-int.
     * If the string cannot be parsed, throws an IllegalArgumentException
     * exception. Supported formats are:
     * #RRGGBB
     * #AARRGGBB
     * 'red', 'blue', 'green', 'black', 'white', 'gray', 'cyan', 'magenta',
     * 'yellow', 'lightgray', 'darkgray'
     */
    public static int parseColor(String colorString) {
        if (colorString.charAt(0) == '#') {
            // Use a long to avoid rollovers on #ffXXXXXX
            long color = Long.parseLong(colorString.substring(1), 16);
            if (colorString.length() == 7) {
                // Set the alpha value
                color |= 0x00000000ff000000;
            } else if (colorString.length() != 9) {
                throw new IllegalArgumentException("Unknown color" + colorString);
            }
            return (int)color;
        }
        throw new IllegalArgumentException("Unknown color" + colorString);
    }
    
    public static String colorToString(int color) {
		if ((0xFF000000 & color) == 0xFF000000) {
			return "#" + Integer.toHexString(color & 0x00FFFFFF);
		} else {
			return "#" + Integer.toHexString(color);
		}
	}
	
	public static void main(String[] args) throws IOException, SAXException {
		OsmandRenderingRules parser = new OsmandRenderingRules();
		parser.parseRenderingRules(OsmandRenderingRules.class.getResourceAsStream("default.render.xml"));
	}
}
