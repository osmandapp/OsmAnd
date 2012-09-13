package net.osmand.translator.cpp.model;

import static net.osmand.translator.utils.TranslationConstants.FINAL_MODIFIER;
import static net.osmand.translator.utils.TranslationConstants.STATIC_MODIFIER;

public class CppField {
	
	private FieldAccessModifiers accessModifier;

	private boolean statick;
	
	private boolean constant;
	
	private String type;
	
	private String name;
	
	private String value;
	
	
	public CppField() {
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(accessModifier.getValue());
		if (statick) {
			buffer.append(STATIC_MODIFIER);
		}
		if (constant) {
			buffer.append(FINAL_MODIFIER);
		}
		buffer.append(type).append(name);
		if (value != null) {
			buffer.append(" = ").append(value).append(";");			
		}
		return buffer.toString();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public FieldAccessModifiers getAccessModifier() {
		return accessModifier;
	}

	public void setAccessModifier(FieldAccessModifiers accessModifier) {
		this.accessModifier = accessModifier;
	}

	public boolean isStatick() {
		return statick;
	}

	public void setStatick(boolean statick) {
		this.statick = statick;
	}

	public boolean isConstant() {
		return constant;
	}

	public void setConstant(boolean constant) {
		this.constant = constant;
	}
	
}
