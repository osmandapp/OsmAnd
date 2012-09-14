package net.osmand.translator.utils;

public class TranslationConstants {

	public static final String FINAL_MODIFIER = "const ";
	public static final String STATIC_MODIFIER = "static ";
	public static final String PRIVATE_MODIFIER = "private ";
	public static final String PROTECTED_MODIFIER = "protected ";
	public static final String PUBLIC_MODIFIER = "public ";
	public static final String VOLATILE_MODIFIER = "volatile ";
	public static final String TRANSIENT_MODIFIER = "transient ";
	
	
	public static final String PRIMITIVE_TYPE_BOOLEAN = "bool ";
	public static final String PRIMITIVE_TYPE_CHAR = "wchar_t ";
	public static final String PRIMITIVE_TYPE_SHORT = "signed short ";
	public static final String PRIMITIVE_TYPE_INT = "signed long ";
	public static final String PRIMITIVE_TYPE_FLOAT = "float ";
	public static final String PRIMITIVE_TYPE_DOUBLE = "double ";
	
	public static final String NOT_PRIMITIVE_TYPE_STRING = "string ";
	
	public static final String ARRAY_TYPE = "[] ";
	
	
	public void initMethods(){
		register("Math", "sin",  "sin");
		register("Math", "cos",  "cos");
		register("Math", "toRadians",  "toRadians");
		
		registerType("boolean", "bool",  true);
		registerType("float", "float",  true);
		/// TODO
		registerType("String", "String",  true);
		
		registerType("LatLon", "LatLon",  true);
		registerType("net.osmand.plus.Node", "Node",  false);
	}


	private void registerType(String javaType, String cppType, boolean primitive) {
		// TODO Auto-generated method stub
		
	}


	private void register(String javaClass, String javaMethod, String cppReplacement) {
		// TODO Auto-generated method stub
		
	}
	
}
