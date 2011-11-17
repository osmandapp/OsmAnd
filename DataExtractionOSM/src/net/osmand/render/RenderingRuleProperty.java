package net.osmand.render;


import net.osmand.LogUtil;

import org.apache.commons.logging.Log;


public class RenderingRuleProperty {
	private final static Log log = LogUtil.getLog(RenderingRuleProperty.class);
	
	private final static int INT_TYPE = 1;
	private final static int FLOAT_TYPE = 2;
	private final static int STRING_TYPE = 3;
	private final static int COLOR_TYPE = 4;
	private final static int BOOLEAN_TYPE = 5;
	
	public static final int TRUE_VALUE = 1;
	public static final int FALSE_VALUE = 0;
	
	// Fields C++
	protected final int type;
	protected final boolean input;
	protected final String attrName;
	
	protected int id = -1;

	// use for custom rendering rule properties
	protected String name;
	protected String description;
	protected String[] possibleValues;
	
	private RenderingRuleProperty(String attrName, int type, boolean input){
		this.attrName = attrName;
		this.type = type;
		this.input = input;
	}
	
	public boolean isInputProperty() {
		return input;
	}
	
	public boolean isOutputProperty() {
		return !input;
	}
	
	public void setId(int id) {
		if (this.id != -1) {
			throw new IllegalArgumentException();
		}
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public String getAttrName() {
		return attrName;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	protected void setName(String name) {
		this.name = name;
	}
	
	protected void setDescription(String description) {
		this.description = description;
	}
	
	protected void setPossibleValues(String[] possibleValues) {
		this.possibleValues = possibleValues;
	}
	
	public String[] getPossibleValues() {
		if (isBoolean()) {
			return new String[] { "true", "false" };
		}
		return possibleValues;
	}
	
	public boolean isBoolean() {
		return type == BOOLEAN_TYPE;
	}
	
	public boolean isFloat() {
		return type == FLOAT_TYPE;
	}
	
	public boolean isInt() {
		return type == INT_TYPE;
	}
	
	public boolean isColor() {
		return type == COLOR_TYPE;
	}
	
	public boolean isString() {
		return type == STRING_TYPE;
	}
	
	public boolean isIntParse(){
		return type == INT_TYPE  || type == STRING_TYPE || type == COLOR_TYPE || type == BOOLEAN_TYPE; 
	}
	
	public boolean accept(int ruleValue, int renderingProperty){
		if(!isIntParse() || !input){
			return false;
		}
		return ruleValue == renderingProperty;
	}
	
	public boolean accept(float ruleValue, float renderingProperty){
		if(type != FLOAT_TYPE || !input){
			return false;
		}
		return ruleValue == renderingProperty;
	}
	
	@Override
	public String toString() {
		return "#RenderingRuleProperty " + getAttrName();
	}
	
	
	public int parseIntValue(String value){
		if(type == INT_TYPE){
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				log.error("Rendering parse " + value);
			}
			return -1;
		} else if(type == BOOLEAN_TYPE){
			return Boolean.parseBoolean(value) ? TRUE_VALUE : FALSE_VALUE;
		} else if(type == STRING_TYPE){
			// requires dictionary to parse
			return -1;
		} else if(type == COLOR_TYPE){
			try {
				return parseColor(value);
			} catch (RuntimeException e) {
				log.error("Rendering parse " + e.getMessage());
			}
			return -1;
		} else {
			return -1;
		}
	}
	
	public float parseFloatValue(String value){
		if(type == FLOAT_TYPE){
			try {
				return Float.parseFloat(value);
			} catch (NumberFormatException e) {
				log.error("Rendering parse " + value);
			}
			return -1;
		} else {
			return -1;
		}
	}
	
	
	
	public static RenderingRuleProperty createOutputIntProperty(String name){
		return new RenderingRuleProperty(name, INT_TYPE, false);
	}
	
	public static RenderingRuleProperty createOutputBooleanProperty(String name){
		return new RenderingRuleProperty(name, BOOLEAN_TYPE, false);
	}
	
	public static RenderingRuleProperty createInputBooleanProperty(String name){
		return new RenderingRuleProperty(name, BOOLEAN_TYPE, true);
	}
	
	public static RenderingRuleProperty createOutputFloatProperty(String name){
		return new RenderingRuleProperty(name, FLOAT_TYPE, false);
	}
	
	public static RenderingRuleProperty createOutputStringProperty(String name){
		return new RenderingRuleProperty(name, STRING_TYPE, false);
	}
	
	public static RenderingRuleProperty createInputIntProperty(String name){
		return new RenderingRuleProperty(name, INT_TYPE, true);
	}
	
	public static RenderingRuleProperty createInputColorProperty(String name){
		return new RenderingRuleProperty(name, COLOR_TYPE, true);
	}
	
	public static RenderingRuleProperty createOutputColorProperty(String name){
		return new RenderingRuleProperty(name, COLOR_TYPE, false);
	}
	
	public static RenderingRuleProperty createInputStringProperty(String name){
		return new RenderingRuleProperty(name, STRING_TYPE, true);
	}
	
	public static RenderingRuleProperty createInputLessIntProperty(String name){
		return new RenderingRuleProperty(name, INT_TYPE, true) {
			@Override
			public boolean accept(int ruleValue, int renderingProperty) {
				if(!isIntParse() || !input){
					return false;
				}
				return ruleValue >= renderingProperty;
			}
		};
	}
	
	public static RenderingRuleProperty createInputGreaterIntProperty(String name){
		return new RenderingRuleProperty(name, INT_TYPE, true) {
			@Override
			public boolean accept(int ruleValue, int renderingProperty) {
				if(!isIntParse() || !input){
					return false;
				}
				return ruleValue <= renderingProperty;
			}
		};
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
                throw new IllegalArgumentException("Unknown color " + colorString); //$NON-NLS-1$
            }
            return (int)color;
        }
        throw new IllegalArgumentException("Unknown color " + colorString); //$NON-NLS-1$
    }
    
    public static String colorToString(int color) {
		if ((0xFF000000 & color) == 0xFF000000) {
			return "#" + Integer.toHexString(color & 0x00FFFFFF); //$NON-NLS-1$
		} else {
			return "#" + Integer.toHexString(color); //$NON-NLS-1$
		}
	}

}
