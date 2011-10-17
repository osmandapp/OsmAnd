package net.osmand.render;

import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultRenderingRuleProperties {

	private static final String TEXT_LENGTH = "textLength";
	private static final String REF = "ref";
	private static final String TEXT_SHIELD = "textShield";
	private static final String SHADOW_RADIUS = "shadowRadius";
	private static final String SHADOW_COLOR = "shadowColor";
	private static final String SHADER = "shader";
	private static final String CAP_3 = "cap_3";
	private static final String CAP_2 = "cap_2";
	private static final String CAP = "cap";
	private static final String PATH_EFFECT_3 = "pathEffect_3";
	private static final String PATH_EFFECT_2 = "pathEffect_2";
	private static final String PATH_EFFECT = "pathEffect";
	private static final String STROKE_WIDTH_3 = "strokeWidth_3";
	private static final String STROKE_WIDTH_2 = "strokeWidth_2";
	private static final String STROKE_WIDTH = "strokeWidth";
	private static final String COLOR_3 = "color_3";
	private static final String COLOR = "color";
	private static final String COLOR_2 = "color_2";
	private static final String TEXT_BOLD = "textBold";
	private static final String TEXT_ORDER = "textOrder";
	private static final String TEXT_MIN_DISTANCE = "textMinDistance";
	private static final String TEXT_ON_PATH = "textOnPath";
	private static final String ICON = "icon";
	private static final String LAYER = "layer";
	private static final String ORDER = "order";
	private static final String ORDER_TYPE = "order_type";
	public static final String TAG = "tag";
	public static final String VALUE = "value";
	public static final String MINZOOM = "minzoom";
	public static final String MAXZOOM = "maxzoom";
	public static final String NIGHT_MODE = "nightMode";
	public static final String TEXT_DY = "textDy";
	public static final String TEXT_SIZE = "textSize";
	public static final String TEXT_COLOR = "textColor";
	public static final String TEXT_HALO_RADIUS = "textHaloRadius";
	public static final String TEXT_WRAP_WIDTH = "textWrapWidth";

	public static Map<String, RenderingRuleProperty> createDefaultRenderingRuleProperties() {
		Map<String, RenderingRuleProperty> map = new LinkedHashMap<String, RenderingRuleProperty>();
		registerRule(map, RenderingRuleProperty.createInputStringProperty(TAG));
		registerRule(map, RenderingRuleProperty.createInputStringProperty(VALUE));
		registerRule(map, RenderingRuleProperty.createInputGreaterIntProperty(MINZOOM));
		registerRule(map, RenderingRuleProperty.createInputLessIntProperty(MAXZOOM));
		registerRule(map, RenderingRuleProperty.createInputBooleanProperty(NIGHT_MODE));
		registerRule(map, RenderingRuleProperty.createInputIntProperty(LAYER));
		
		// order - no sense to make it float
		registerRule(map, RenderingRuleProperty.createOutputIntProperty(ORDER));
		registerRule(map, RenderingRuleProperty.createInputStringProperty(ORDER_TYPE));
		
		// text properties
		registerRule(map, RenderingRuleProperty.createOutputIntProperty(TEXT_WRAP_WIDTH));
		registerRule(map, RenderingRuleProperty.createOutputIntProperty(TEXT_DY));
		registerRule(map, RenderingRuleProperty.createOutputIntProperty(TEXT_HALO_RADIUS));
		registerRule(map, RenderingRuleProperty.createOutputIntProperty(TEXT_SIZE));
		registerRule(map, RenderingRuleProperty.createOutputIntProperty(TEXT_ORDER));
		registerRule(map, RenderingRuleProperty.createOutputIntProperty(TEXT_MIN_DISTANCE));
		registerRule(map, RenderingRuleProperty.createOutputIntProperty(TEXT_LENGTH));
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(TEXT_SHIELD));
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(REF));
		
		
		
		registerRule(map, RenderingRuleProperty.createOutputColorProperty(TEXT_COLOR));
		
		registerRule(map, RenderingRuleProperty.createOutputBooleanProperty(TEXT_BOLD));
		registerRule(map, RenderingRuleProperty.createOutputBooleanProperty(TEXT_ON_PATH));
		
		// point
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(ICON));
		
		// polygon/way
		registerRule(map, RenderingRuleProperty.createOutputColorProperty(COLOR));
		registerRule(map, RenderingRuleProperty.createOutputColorProperty(COLOR_2));
		registerRule(map, RenderingRuleProperty.createOutputColorProperty(COLOR_3));
		registerRule(map, RenderingRuleProperty.createOutputFloatProperty(STROKE_WIDTH));
		registerRule(map, RenderingRuleProperty.createOutputFloatProperty(STROKE_WIDTH_2));
		registerRule(map, RenderingRuleProperty.createOutputFloatProperty(STROKE_WIDTH_3));
		
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(PATH_EFFECT));
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(PATH_EFFECT_2));
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(PATH_EFFECT_3));
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(CAP));
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(CAP_2));
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(CAP_3));
		registerRule(map, RenderingRuleProperty.createOutputStringProperty(SHADER));
		
		registerRule(map, RenderingRuleProperty.createOutputColorProperty(SHADOW_COLOR));
		registerRule(map, RenderingRuleProperty.createOutputIntProperty(SHADOW_RADIUS));
		

		return map;
	}

	private static void registerRule(Map<String, RenderingRuleProperty> map, RenderingRuleProperty p) {
		map.put(p.getAttrName(), p);
	}
}
