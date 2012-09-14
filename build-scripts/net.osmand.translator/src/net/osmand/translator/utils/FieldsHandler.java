package net.osmand.translator.utils;

import net.osmand.translator.visitor.FieldVisitor;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldsHandler extends AbstractHandler{

	public static void printFieldsInfo(CompilationUnit parse) {
	    FieldVisitor fVisitor = new FieldVisitor();
	    parse.accept(fVisitor);
	    for (FieldDeclaration field : fVisitor.getFields()) {
	    	VariableDeclarationFragment fragment = (VariableDeclarationFragment)field.fragments().get(0);
	    	IVariableBinding binding = fragment.resolveBinding();
			Expression initializer = fragment.getInitializer();
	    	StringBuffer buffer = new StringBuffer();
//	    	modifiers
	    	applyModifiers(field.getModifiers(), buffer);
//			type
	    	applyType(field.getType(), buffer);
//			name
			buffer.append(binding.getJavaElement().getElementName());
//			array brackets
			if (binding.getType().isArray()) {
				buffer.append("[]");
			}
//			value
			if (initializer != null) {
				buffer.append(" = ").append(initializer);
			}
//			end of string
			buffer.append(";");
			System.out.println(buffer);
//			System.out.println();
	     }
	}
	  
}
