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

public class OsmandRenderingRulesParser {

	
	private final static Log log = LogUtil.getLog(OsmandRenderingRulesParser.class);
	
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
		public float order = 0;
		public int orderType = -1;
		
		public String shader = null;
		
		// point
		public String icon = null;
		
		public EffectAttributes main = new EffectAttributes();
		public TextAttributes text = null;
		public List<EffectAttributes> effectAttributes = new ArrayList<EffectAttributes>(3);
		
		protected EffectAttributes getEffectAttributes(int i) {
			i -= 2;
			while (i >= effectAttributes.size()) {
				effectAttributes.add(new EffectAttributes());
			}
			return effectAttributes.get(i);
		}
		
		
	}
	
	public interface RenderingRuleVisitor {
		
		/**
		 * @param state - one of the point, polygon, line, text state
		 * @param filter
		 */
		public void visitRule(int state, FilterState  filter);
		
		public void rendering(String name, String depends, int defaultColor);
	}
	
	
	public final static int POINT_STATE = 1;
	public final static int POLYGON_STATE = 2;
	public final static int LINE_STATE = 3;
	public final static int TEXT_STATE = 4;
	public final static int ORDER_STATE = 5;
	
	public void parseRenderingRules(InputStream is, RenderingRuleVisitor visitor) throws IOException, SAXException {
		try {
			final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(is, new RenderingRulesHandler(saxParser, visitor));
		} catch (ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}
	
	private class RenderingRulesHandler extends DefaultHandler {
		private final SAXParser parser;
		private final RenderingRuleVisitor visitor;
		private int state;

		Stack<Object> stack = new Stack<Object>();
		
		
		public RenderingRulesHandler(SAXParser parser, RenderingRuleVisitor visitor){
			this.parser = parser;
			this.visitor = visitor;
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
					defaultColor = parseColor(dc);
				}
				visitor.rendering(attributes.getValue("name"), attributes.getValue("depends"), defaultColor); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				log.warn("Unknown tag" + name); //$NON-NLS-1$
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			name = parser.isNamespaceAware() ? localName : name;
			if ("filter".equals(name)) { //$NON-NLS-1$
				List<FilterState> list = popAndAggregateState();
				for (FilterState pop : list) {
					if (pop.tag != null && (pop.minzoom != -1 || state == ORDER_STATE)) {
						visitor.visitRule(state, pop);
					}
				}
			} else if("switch".equals(name)){ //$NON-NLS-1$
				stack.pop();
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
					if (res == null) {
						res = new ArrayList<FilterState>();
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
						mergeStateInto(filters.get(j / l), res.get(j));
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
			if(toMerge.orderType != -1 && mergeInto.orderType == -1){
				mergeInto.orderType = toMerge.orderType;
			}
			if(toMerge.layer != 0 && mergeInto.layer == 0){
				mergeInto.layer = toMerge.layer;
			}
			if(toMerge.order != 0 && mergeInto.order == 0){
				mergeInto.order = toMerge.order;
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
				
				if(toMerge.text.ref != null && mergeInto.text.ref == null){
					mergeInto.text.ref = toMerge.text.ref;
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
				if(name.equals("tag")){ //$NON-NLS-1$
					state.tag = val;
				} else if(name.equals("value")){ //$NON-NLS-1$
					state.val = val;
				} else if(name.equals("minzoom")){ //$NON-NLS-1$
					state.minzoom = Integer.parseInt(val);
				} else if(name.equals("maxzoom")){ //$NON-NLS-1$
					state.maxzoom = Integer.parseInt(val);
				} else if(name.equals("maxzoom")){ //$NON-NLS-1$
					state.maxzoom = Integer.parseInt(val);
				} else if(name.equals("layer")){ //$NON-NLS-1$
					state.layer = Integer.parseInt(val);
				} else if(name.equals("orderType")){ //$NON-NLS-1$
					int i1 = val.equals("polygon") ? 3 : (val.equals("line") ? 2 : 1);  //$NON-NLS-1$ //$NON-NLS-2$
					state.orderType = i1;
				} else if(name.equals("order")){ //$NON-NLS-1$
					state.order = Float.parseFloat(val);
				} else if(name.equals("icon")){ //$NON-NLS-1$
					state.icon = val;
				} else if(name.equals("color")){ //$NON-NLS-1$
					state.main.color = parseColor(val);
				} else if(name.startsWith("color_")){ //$NON-NLS-1$
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(6)));
					ef.color = parseColor(val);
				} else if(name.equals("shader")){ //$NON-NLS-1$
					state.shader = val;
				} else if(name.equals("strokeWidth")){ //$NON-NLS-1$
					state.main.strokeWidth = Float.parseFloat(val);
				} else if(name.startsWith("strokeWidth_")){ //$NON-NLS-1$
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(12)));
					ef.strokeWidth = Float.parseFloat(val);
				} else if(name.equals("pathEffect")){ //$NON-NLS-1$
					state.main.pathEffect = val;
				} else if(name.startsWith("pathEffect_")){ //$NON-NLS-1$
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(11)));
					ef.pathEffect = val;
				} else if(name.equals("shadowRadius")){ //$NON-NLS-1$
					state.main.shadowRadius = Float.parseFloat(val);
				} else if(name.startsWith("shadowRadius_")){ //$NON-NLS-1$
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(14)));
					ef.shadowRadius = Float.parseFloat(val);
				} else if(name.equals("shadowColor")){ //$NON-NLS-1$
					state.main.shadowColor = parseColor(val);
				} else if(name.startsWith("shadowColor_")){ //$NON-NLS-1$
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(12)));
					ef.shadowColor = parseColor(val);
				} else if(name.equals("cap")){ //$NON-NLS-1$
					state.main.cap = val;
				} else if(name.startsWith("cap_")){ //$NON-NLS-1$
					EffectAttributes ef = state.getEffectAttributes(Integer.parseInt(name.substring(4)));
					ef.cap = val;
				} else if(name.equals("ref")){ //$NON-NLS-1$
					state.text.ref = val;
				} else if(name.equals("textSize")){ //$NON-NLS-1$
					state.text.textSize = Float.parseFloat(val);
				} else if(name.equals("textBold")){ //$NON-NLS-1$
					state.text.textBold = Boolean.parseBoolean(val);
				} else if(name.equals("textColor")){ //$NON-NLS-1$
					state.text.textColor = parseColor(val);
				} else if(name.equals("textLength")){ //$NON-NLS-1$
					state.textLength = Integer.parseInt(val);
				} else if(name.equals("textShield")){ //$NON-NLS-1$
					state.text.textShield = val;
				} else if(name.equals("textMinDistance")){ //$NON-NLS-1$
					state.text.textMinDistance = Integer.parseInt(val);
				} else if(name.equals("textOnPath")){ //$NON-NLS-1$
					state.text.textOnPath = Boolean.parseBoolean(val);
				} else if(name.equals("textWrapWidth")){ //$NON-NLS-1$
					state.text.textWrapWidth = Integer.parseInt(val);
				} else if(name.equals("textDy")){ //$NON-NLS-1$
					state.text.textDy = Integer.parseInt(val);
				} else if(name.equals("textHaloRadius")){ //$NON-NLS-1$
					state.text.textHaloRadius = Float.parseFloat(val);
				} else {
					log.warn("Unknown attribute " + name); //$NON-NLS-1$
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
                throw new IllegalArgumentException("Unknown color" + colorString); //$NON-NLS-1$
            }
            return (int)color;
        }
        throw new IllegalArgumentException("Unknown color" + colorString); //$NON-NLS-1$
    }
    
    
    // TEST purpose 
	public static void main(String[] args) throws IOException, SAXException {
		OsmandRenderingRulesParser parser = new OsmandRenderingRulesParser();
		parser.parseRenderingRules(OsmandRenderingRulesParser.class.getResourceAsStream("default.render.xml"),  //$NON-NLS-1$
				new RenderingRuleVisitor() {

			@Override
			public void rendering(String name, String depends, int defColor) {
				System.out.println("Renderer " + name); //$NON-NLS-1$
			}

			@Override
			public void visitRule(int state, FilterState filter) {
				String gen = generateAttributes(state, filter);
				if (gen != null) {
					String res = ""; //$NON-NLS-1$
					if (filter.maxzoom != -1) {
						res += " zoom : " +filter.minzoom + "-" + filter.maxzoom; //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						res += " zoom : " +filter.minzoom; //$NON-NLS-1$
					}
					res += " tag="+filter.tag; //$NON-NLS-1$
					res += " val="+filter.val; //$NON-NLS-1$
					if(filter.layer != 0){
						res += " layer="+filter.layer; //$NON-NLS-1$
					}
					
					res += gen;
					System.out.println(res);
				}
			}

		});
	}

    public static String colorToString(int color) {
		if ((0xFF000000 & color) == 0xFF000000) {
			return "#" + Integer.toHexString(color & 0x00FFFFFF); //$NON-NLS-1$
		} else {
			return "#" + Integer.toHexString(color); //$NON-NLS-1$
		}
	}
	
	private static String generateAttributes(int state, FilterState s){
		String res = ""; //$NON-NLS-1$
		if(s.shader != null){
			res+=" shader=" + s.shader; //$NON-NLS-1$
		}
		
		if(s.icon != null){
			res+= " icon="+s.icon; //$NON-NLS-1$
		}
		
		if(s.order != 0){
			res+= " order="+s.order; //$NON-NLS-1$
		}
		
		if(s.orderType != 0){
			res+= " orderType="+s.orderType; //$NON-NLS-1$
		}
		
		res = generateAttributes(s.main, res, ""); //$NON-NLS-1$
		int p = 2;
		for(EffectAttributes ef : s.effectAttributes){
			res = generateAttributes(ef, res, "_"+(p++)); //$NON-NLS-1$
		}
		if(s.text != null){
			if(s.text.textSize != 0){
				res+= " textSize="+s.text.textSize; //$NON-NLS-1$
			}
			if(s.text.ref != null){
				res+= " ref="+s.text.ref; //$NON-NLS-1$
			}
			if(s.text.textColor != 0){
				res+= " textColor="+colorToString(s.text.textColor); //$NON-NLS-1$
			}
			if(s.text.textShield != null){
				res+= " textShield="+s.text.textShield; //$NON-NLS-1$
			}
			
			
		}
		if(state == POLYGON_STATE){
//			return res;
		} else if(state == LINE_STATE){
//			return res;
		} else if(state == POINT_STATE){
//			return res;
		} else if(state == TEXT_STATE){
//			return res;
		} else if(state == ORDER_STATE){
			return res;
		}
		return null;
	}


	private static String generateAttributes(EffectAttributes s, String res, String prefix) {
		if(s.color != 0){
			res +=" color"+prefix+"="+colorToString(s.color); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if(s.strokeWidth != 0){
			res+= " strokeWidth"+prefix+"="+s.strokeWidth; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if(s.pathEffect != null){
			res+= " pathEffect"+prefix+"="+s.pathEffect; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return res;
	}
}
