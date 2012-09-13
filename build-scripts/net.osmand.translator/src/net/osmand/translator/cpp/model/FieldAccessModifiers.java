package net.osmand.translator.cpp.model;

public enum FieldAccessModifiers {

	
	PUBLIC ("public"), PROTECTED ("protected "), PRIVATE ("private "), DEFAULT ("");
	
	private final String value;
	
	private FieldAccessModifiers(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
}
