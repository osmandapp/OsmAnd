package net.osmand.translator.cpp.model;

import java.util.List;

public class CppClass {
	
	private List<String> imports;
	
	private List<CppField> fields;
	
	private List<CppMethod> methods;
	
	private List<CppClass> classes;
	
	private boolean innerClass = false;
	
	private boolean staticClass = false;
	
	public CppClass() {
	}
}
