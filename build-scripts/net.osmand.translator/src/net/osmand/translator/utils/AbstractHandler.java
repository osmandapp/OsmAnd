package net.osmand.translator.utils;

import static net.osmand.translator.utils.TranslationConstants.*;

import java.lang.reflect.Modifier;

import net.osmand.translator.TranslatorException;

import org.eclipse.jdt.core.dom.Type;

public abstract class AbstractHandler {
	
	protected static void applyType(Type type, StringBuffer buffer) {
        if (type.isPrimitiveType()) {       	
        	try {
				applyPrimitiveType(type, buffer);
			} catch (TranslatorException e) {
//				TODO
				System.out.println("Primitive type is undefined.");;
			}        	
        } else if (type.toString().equals("String")) {
        	buffer.append(NOT_PRIMITIVE_TYPE_STRING);
        }
	}
	
//	private static void buildCppField(StringBuffer buffer) {
//		
//	}
	
	protected static void applyModifiers(int mod, StringBuffer buffer) {		
		if (Modifier.isPublic(mod)) {
			buffer.append(PUBLIC_MODIFIER);
		} else if (Modifier.isProtected(mod)) {
			buffer.append(PROTECTED_MODIFIER);
		} else if (Modifier.isPrivate(mod)) {
			buffer.append(PRIVATE_MODIFIER);
		} 
		if (Modifier.isFinal(mod))  {
			buffer.append(FINAL_MODIFIER);
		}
		if (Modifier.isStatic(mod)) {
			buffer.append(STATIC_MODIFIER);
		}
//		if (Modifier.isVolatile(mod)) {}
//		if (Modifier.isTransient(mod)) {}
	}
	
	private static void applyPrimitiveType (Type type, StringBuffer buffer) throws TranslatorException {
		if (type.toString().equals("boolean")) {
    		buffer.append(PRIMITIVE_TYPE_BOOLEAN);
    	} else if (type.toString().equals("char")) {
    		buffer.append(PRIMITIVE_TYPE_CHAR);
    	} else if (type.toString().equals("short")) {
    		buffer.append(PRIMITIVE_TYPE_SHORT);
    	} else if (type.toString().equals("int")) {
    		buffer.append(PRIMITIVE_TYPE_INT);
    	} else if (type.toString().equals("float")) {
    		buffer.append(PRIMITIVE_TYPE_FLOAT);
    	} else if (type.toString().equals("double")) {
    		buffer.append(PRIMITIVE_TYPE_DOUBLE);
    	} else {
    		throw new TranslatorException();
    	}
	}
}
