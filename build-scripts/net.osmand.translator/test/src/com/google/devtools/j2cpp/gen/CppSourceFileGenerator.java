package com.google.devtools.j2cpp.gen;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.collect.Lists;
import com.google.devtools.j2objc.gen.SourceFileGenerator;
import com.google.devtools.j2objc.types.IOSMethod;
import com.google.devtools.j2objc.types.IOSParameter;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.NameTable;

public abstract class CppSourceFileGenerator extends SourceFileGenerator {

	  /**
	   * Create a new generator.
	   *
	   * @param sourceFileName the name of the source file being translated
	   * @param outputDirectory the top-level directory for output file(s)
	   */
	  protected CppSourceFileGenerator(String sourceFileName, String source,
	      CompilationUnit unit, boolean emitLineDirectives) {
	    super(sourceFileName, source, unit, emitLineDirectives);
	  }

	  /**
	   * Generate an output source file from the specified type declaration.
	   */
	  public void generate(AbstractTypeDeclaration node) {
	    if (node instanceof TypeDeclaration) {
	      generate((TypeDeclaration) node);
	    } else if (node instanceof EnumDeclaration) {
	      generate((EnumDeclaration) node);
	    } else if (node instanceof AnnotationTypeDeclaration) {
	      generate((AnnotationTypeDeclaration) node);
	    }
	  }

	  protected abstract void generate(TypeDeclaration node);

	  protected abstract void generate(EnumDeclaration node);

	  protected abstract void generate(AnnotationTypeDeclaration node);

	  public void save(CompilationUnit node) {
	    save(getOutputFileName(node));
	  }

	  /**
	   * Print a list of methods.
	   */
	  protected void printMethods(List<MethodDeclaration> methods) {
	    for (MethodDeclaration m : methods) {
	      syncLineNumbers(m.getName());  // avoid doc-comment
	      IMethodBinding binding = Types.getMethodBinding(m);
	      IOSMethod iosMethod = Types.getMappedMethod(binding);
	      if (iosMethod != null) {
	        print(mappedMethodDeclaration(m, iosMethod));
	      } else if (m.isConstructor()) {
	        print(constructorDeclaration(m));
	      } else if (Modifier.isStatic(m.getModifiers()) &&
	          NameTable.CLINIT_NAME.equals(m.getName().getIdentifier())) {
	        printStaticConstructorDeclaration(m);
	      } else if (!isMainMethod(m) && !isInterfaceConstantAccessor(binding)) {
	        printMethod(m);
	      }
	    }
	  }

	  /**
	   * Returns true if the specified method binding describes an accessor for
	   * an interface constant.
	   */
	  protected boolean isInterfaceConstantAccessor(IMethodBinding binding) {
	    return binding.getDeclaringClass().isInterface()
	        && !Modifier.isAbstract(binding.getModifiers());
	  }

	  /**
	   * Returns a list of those methods that define accessors to interface
	   * constants.  For most interfaces, the returned list will be empty.
	   */
	  protected List<MethodDeclaration> findInterfaceConstantAccessors(
	      List<MethodDeclaration> methods) {
	    List<MethodDeclaration> results = Lists.newArrayList();
	    for (MethodDeclaration m : methods) {
	      if (isInterfaceConstantAccessor(Types.getMethodBinding(m))) {
	        results.add(m);
	      }
	    }
	    return results;
	  }

	  protected void printMethod(MethodDeclaration m) {
	    print(methodDeclaration(m));
	  }

	  /**
	   * Create an C++ method or constructor declaration string for an
	   * inlined method.
	   */
	  protected String mappedMethodDeclaration(MethodDeclaration method, IOSMethod mappedMethod) {
	    StringBuffer sb = new StringBuffer();
	    boolean isStatic = (method.getModifiers() & Modifier.STATIC) > 0;

	    // Explicitly test hashCode() because of NSObject's hash return value.
	    String baseDeclaration;
	    if (mappedMethod.getName().equals("hash")) {
	      baseDeclaration = "- (NSUInteger)hash";
	    } else {
	      baseDeclaration = String.format("%s (%s)%s", isStatic ? "static" : "",
	          NameTable.javaRefToCpp(method.getReturnType2()), mappedMethod.getName());
	    }

	    sb.append(baseDeclaration);
	    Iterator<IOSParameter> iosParameters = mappedMethod.getParameters().iterator();
	    if (iosParameters.hasNext()) {
	      @SuppressWarnings("unchecked")
	      List<SingleVariableDeclaration> parameters = method.parameters();
	      IOSParameter first = iosParameters.next();
	      SingleVariableDeclaration var = parameters.get(first.getIndex());
	      addTypeAndName(first, var, sb);
	      if (iosParameters.hasNext()) {
	        sb.append(mappedMethod.isVarArgs() ? ", " : " ");
	        IOSParameter next = iosParameters.next();
	        sb.append(next.getParameterName());
	        var = parameters.get(next.getIndex());
	        addTypeAndName(next, var, sb);
	      }
	    }
	    return sb.toString();
	  }

	  private void addTypeAndName(IOSParameter iosParameter, SingleVariableDeclaration var,
	      StringBuffer sb) {
	    sb.append(":(");
	    sb.append(iosParameter.getType());
	    sb.append(')');
	    sb.append(var.getName().getIdentifier());
	  }

	  /**
	   * Create an C++ method declaration string.
	   */
	  protected String methodDeclaration(MethodDeclaration m) {
	    assert !m.isConstructor();
	    StringBuffer sb = new StringBuffer();
	    boolean isStatic = Modifier.isStatic(m.getModifiers());
	    IMethodBinding binding = Types.getMethodBinding(m);
	    String  methodName = NameTable.getName(binding);
	    String baseDeclaration = String.format("\t %s %s %s", isStatic ? "static " : "", NameTable.javaRefToCpp(m.getReturnType2()), methodName);
	    sb.append(baseDeclaration);
	    @SuppressWarnings("unchecked")
	    List<SingleVariableDeclaration> params = m.parameters(); // safe by definition
	    parametersDeclaration(Types.getOriginalMethodBinding(binding), params, baseDeclaration, sb);
	    return sb.toString();
	  }

	  /**
	   * Create an C++ constructor declaration string.
	   */
	  protected String constructorDeclaration(MethodDeclaration m) {
	    assert m.isConstructor();
	    StringBuffer sb = new StringBuffer();
	    String baseDeclaration = "- (id)init";
	    sb.append(baseDeclaration);
	    @SuppressWarnings("unchecked")
	    List<SingleVariableDeclaration> params = m.parameters(); // safe by definition
	    parametersDeclaration(Types.getMethodBinding(m), params, baseDeclaration, sb);
	    return sb.toString();
	  }

	  /**
	   * Print an C++ constructor declaration string.
	   */
	  protected abstract void printStaticConstructorDeclaration(MethodDeclaration m);

	  private void parametersDeclaration(IMethodBinding method, List<SingleVariableDeclaration> params,
	      String baseDeclaration, StringBuffer sb) throws AssertionError {
		  
	    if (!params.isEmpty()) {
	      ITypeBinding[] parameterTypes = method.getParameterTypes();
	      int nParams = params.size();
	      sb.append(" (");
	      for (int i = 0; i < nParams; i++) {
	        SingleVariableDeclaration param = params.get(i);
	        String fieldName = getParameterName(param);
	        ITypeBinding typeBinding = parameterTypes[i];
	        boolean isTypeVariable = typeBinding.isTypeVariable();
	        String type = isTypeVariable ? 
	        		NameTable.getParameterTypeName(NameTable.ID_TYPE, typeBinding) : 
	        		NameTable.getParameterTypeName(NameTable.javaTypeToCpp(param.getType(), true), typeBinding);
	        sb.append(" ").append(type).append(" ").append(fieldName);
	        if (i<nParams-1) {
	        	sb.append(",");
	        } else {
	        	sb.append(" )");
	        } 
	      }
	    }
//	    TODO
//	    if (method.isConstructor() && method.getDeclaringClass().isEnum()) {
//	      // If enum constant type, append name and ordinal.
//	      if (params.isEmpty()) {
//	        sb.append("WithNSString:(NSString *)name withInt:(int)ordinal");
//	      } else {
//	        sb.append('\n');
//	        String keyword = "withNSString";
//	        sb.append(pad(baseDeclaration.length() - keyword.length()));
//	        sb.append(keyword);
//	        sb.append(":(NSString *)name\n");
//	        keyword = "withInt";
//	        sb.append(pad(baseDeclaration.length() - keyword.length()));
//	        sb.append(keyword);
//	        sb.append(":(int)ordinal");
//	      }
//	    }
	  }

	  protected String getParameterName(SingleVariableDeclaration param) {
	    String name = NameTable.getName(param.getName());
	    if (NameTable.isReservedName(name)) {
	      name += "Arg";
	    }
	    return name;
	  }

	  private String parameterKeyword(Type type, ITypeBinding typeBinding) {
	    String typeName = NameTable.javaTypeToCpp(type, true);
	    return parameterKeyword(typeName, typeBinding);
	  }

	  /**
	   * Returns a parameter name, which consists of a prefix ("with") and
	   * a type name that doesn't conflict with core names.  For example,
	   * "Foo" returns "withFoo", "Long" returns "withLong", and "long"
	   * returns "withLongInt", so as not to conflict with the previous
	   * example.
	   *
	   * For array types, the name returned is the type of the array's
	   * element followed by "Array".
	   */
	  public static String parameterKeyword(String typeName, ITypeBinding typeBinding) {
	    return "with" + NameTable.capitalize(NameTable.getParameterTypeName(typeName, typeBinding));
	  }

	  /**
	   * Returns true if the specified method declaration is for a Java main
	   */
	  protected boolean isMainMethod(MethodDeclaration m) {
	    int modifiers = m.getModifiers();
	    if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
	      if (m.getName().getIdentifier().equals("main")) {
	        List<?> args = m.parameters();
	        if (args.size() == 1) {
	          SingleVariableDeclaration var = (SingleVariableDeclaration) args.get(0);

	          // Use original binding, since we can't tell if it's a String
	          // array after translation, since IOSObjectArray just holds objects.
	          ITypeBinding type = var.resolveBinding().getType();
	          ITypeBinding stringType = m.getAST().resolveWellKnownType("java.lang.String");
	          return type.isArray() && type.getComponentType().isEqualTo(stringType);
	        }
	      }
	    }
	    return false;
	  }

	  /**
	   * Returns a function declaration string from a specified class and method.
	   */
	  protected String makeFunctionDeclaration(AbstractTypeDeclaration cls,
	      MethodDeclaration method) {
	    StringBuffer sb = new StringBuffer();
	    Type returnType = method.getReturnType2();
	    ITypeBinding binding = Types.getTypeBinding(returnType);
	    if (binding.isEnum()) {
	      sb.append(NameTable.javaTypeToCpp(returnType, true));
	    } else {
	      sb.append(NameTable.javaRefToCpp(returnType));
	    }
	    sb.append(' ');
	    sb.append(NameTable.makeFunctionName(cls, method));
	    sb.append('(');
	    for (Iterator<?> iterator = method.parameters().iterator(); iterator.hasNext(); ) {
	      Object o = iterator.next();
	      if (o instanceof SingleVariableDeclaration) {
	        SingleVariableDeclaration param = (SingleVariableDeclaration) o;
	        String fieldType = NameTable.javaRefToCpp(param.getType());
	        String fieldName = param.getName().getIdentifier();
	        sb.append(String.format("%s %s", fieldType, fieldName));
	        if (iterator.hasNext()) {
	          sb.append(", ");
	        }
	      }
	    }
	    sb.append(')');
	    return sb.toString();
	  }

	  /**
	   * Returns true if a superclass also defines this variable.
	   */
	  protected boolean superDefinesVariable(VariableDeclarationFragment var) {
	    IVariableBinding varBinding = Types.getVariableBinding(var);
	    ITypeBinding declaringClassBinding = varBinding.getDeclaringClass();
	    TypeDeclaration declaringClass =
	        Types.getTypeDeclaration(declaringClassBinding, getUnit().types());
	    if (declaringClass == null) {
	      return false;
	    }
	    String name = var.getName().getIdentifier();
	    ITypeBinding type = varBinding.getType();
	    return superDefinesVariable(declaringClass, name, type);
	  }

	  private boolean superDefinesVariable(TypeDeclaration declaringClass, String name,
	      ITypeBinding type) {
	    ITypeBinding superClazzBinding = Types.getTypeBinding(declaringClass.getSuperclassType());
	    TypeDeclaration superClazz = Types.getTypeDeclaration(superClazzBinding, getUnit().types());
	    if (superClazz == null) {
	      return false;
	    }
	    for (FieldDeclaration field : superClazz.getFields()) {
	      @SuppressWarnings("unchecked")
	      List<VariableDeclarationFragment> vars = field.fragments(); // safe by definition
	      for (VariableDeclarationFragment var : vars) {
	        if (var.getName().getIdentifier().equals(name)) {
	          ITypeBinding varType = Types.getTypeBinding(var);
	          if (varType.isEqualTo(type)) {
	            return true;
	          }
	        }
	      }
	    }
	    return superDefinesVariable(superClazz, name, type);
	  }
	}
