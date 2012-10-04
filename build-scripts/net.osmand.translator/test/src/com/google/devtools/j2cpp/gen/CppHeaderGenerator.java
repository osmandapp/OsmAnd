package com.google.devtools.j2cpp.gen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.devtools.j2objc.J2ObjC;
import com.google.devtools.j2objc.Options;
import com.google.devtools.j2objc.types.HeaderImportCollector;
import com.google.devtools.j2objc.types.IOSMethod;
import com.google.devtools.j2objc.types.ImportCollector;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.devtools.j2objc.util.NameTable;
import com.google.devtools.j2objc.util.UnicodeUtils;

public class CppHeaderGenerator extends CppSourceFileGenerator{

	  /**
	   * Generate an C++ header file for each type declared in a specified
	   * compilation unit.
	   */
	  public static void generate(String fileName, String source, CompilationUnit unit) {
	    CppHeaderGenerator headerGenerator = new CppHeaderGenerator(fileName, source, unit);
	    headerGenerator.generate(unit);
	  }

	  private CppHeaderGenerator(String fileName, String source, CompilationUnit unit) {
	    super(fileName, source, unit, false);
	  }

	  @Override
	  protected String getSuffix() {
	    return ".h";
	  }

	  public void generate(CompilationUnit unit) {
	    println(J2ObjC.getFileHeader(getSourceFileName()));

	    @SuppressWarnings("unchecked")
	    List<AbstractTypeDeclaration> types = unit.types(); // safe by definition
	    Set<ITypeBinding> moreForwardTypes = sortTypes(types);
	    //TODO
	    printImportsAndForwardReferences(unit, moreForwardTypes);

	    for (AbstractTypeDeclaration type : types) {
	      newline();
	      generate(type);
	    }
	    save(unit);
	  }

	  @Override
	  public void generate(TypeDeclaration node) {
	    String typeName = NameTable.getFullName(node);
	    String superName = NameTable.getSuperClassName(node);

	    //DONE if extends superclass other than Object
	    //TODO if implements interfaces
	    if ((superName != null) && (!superName.equals("NSObject"))) {
	    	printf(": public %s", superName);
	    }
	    
	    printConstantDefines(node);
	    printf("class %s", typeName);
	    println(" {");
	    printInstanceVariables(node.getFields());
	    
	    MethodDeclaration[] allMethods = node.getMethods();
	    List<MethodDeclaration> publicMethods = new ArrayList<MethodDeclaration>(); 
	    List<MethodDeclaration> protectedMethods = new ArrayList<MethodDeclaration>(); 
	    List<MethodDeclaration> privateMethods = new ArrayList<MethodDeclaration>(); 
	    
	    for (MethodDeclaration m : allMethods) {
	    	if (Modifier.isPublic(m.getModifiers())) {
	    		publicMethods.add(m);
	    	} else if (Modifier.isProtected(m.getModifiers())) {
	    		protectedMethods.add(m);
	    	} else if (Modifier.isPrivate(m.getModifiers())) {
	    		privateMethods.add(m);
	    	} 
	    }
	    if (publicMethods.size() > 0) {
	    	println("\npublic: ");
	    	printMethods(publicMethods);
	    }
	    if (protectedMethods.size() > 0) {
	    	println("\nprotected: ");
	    	printMethods(protectedMethods);
	    }
	    if (privateMethods.size() > 0) {
	    	println("\nprivate: ");
	    	printMethods(privateMethods);
	    }
	    println("};\n");
	  }

	  @Override
	  protected void generate(AnnotationTypeDeclaration node) {
	    String typeName = NameTable.getFullName(node);
	    printf("@protocol %s < NSObject >\n@end\n", typeName);
	  }

	  @Override
	  protected void generate(EnumDeclaration node) {
	    printConstantDefines(node);
	    String typeName = NameTable.getFullName(node);
	    @SuppressWarnings("unchecked")
	    List<EnumConstantDeclaration> constants = node.enumConstants();

	    // C doesn't allow empty enum declarations.  Java does, so we skip the
	    // C enum declaration and generate the type declaration.
	    if (!constants.isEmpty()) {
	      println("typedef enum {");

	      // Strip enum type suffix.
	      String bareTypeName = typeName.endsWith("Enum") ?
	          typeName.substring(0, typeName.length() - 4) : typeName;

	      // Print C enum typedef.
	      indent();
	      int ordinal = 0;
	      for (EnumConstantDeclaration constant : constants) {
	        printIndent();
	        printf("%s_%s = %d,\n", bareTypeName, constant.getName().getIdentifier(), ordinal++);
	      }
	      unindent();
	      printf("} %s;\n\n", bareTypeName);
	    }

	    List<FieldDeclaration> fields = Lists.newArrayList();
	    List<MethodDeclaration> methods = Lists.newArrayList();
	    for (Object decl : node.bodyDeclarations()) {
	      if (decl instanceof FieldDeclaration) {
	        fields.add((FieldDeclaration) decl);
	      } else if (decl instanceof MethodDeclaration) {
	        methods.add((MethodDeclaration) decl);
	      }
	    }

	    // Print enum type.
	    printf("@interface %s : JavaLangEnum < NSCopying", typeName);
	    ITypeBinding enumType = Types.getTypeBinding(node);
	    for (ITypeBinding intrface : enumType.getInterfaces()) {
	      if (!intrface.getName().equals(("Cloneable"))) { // Cloneable handled below.
	        printf(", %s", NameTable.getFullName(intrface));
	      }
	    }
	    println(" > {");
	    FieldDeclaration[] fieldDeclarations = fields.toArray(new FieldDeclaration[0]);
	    printInstanceVariables(fieldDeclarations);
	    println("}");
//	    printProperties(fieldDeclarations);
	    for (EnumConstantDeclaration constant : constants) {
	      printf("+ (%s *)%s;\n", typeName, NameTable.getName(constant.getName()));
	    }
	    println("+ (IOSObjectArray *)values;");
	    printf("+ (%s *)valueOfWithNSString:(NSString *)name;\n", typeName);
	    println("- (id)copyWithZone:(NSZone *)zone;");
	    printMethods(methods);
	    println("@end");
	  }

	  @Override
	  protected String methodDeclaration(MethodDeclaration m) {
	    return super.methodDeclaration(m) + ";\n";
	  }

	  @Override
	  protected String mappedMethodDeclaration(MethodDeclaration method, IOSMethod mappedMethod) {
	    return super.mappedMethodDeclaration(method, mappedMethod) + ";\n";
	  }

	  @Override
	  protected String constructorDeclaration(MethodDeclaration m) {
	    return super.constructorDeclaration(m) + ";\n";
	  }

	  @Override
	  protected void printStaticConstructorDeclaration(MethodDeclaration m) {
	    // Don't do anything.
	  }

	  @Override
	  protected void printMethod(MethodDeclaration m) {
	    IMethodBinding binding = Types.getMethodBinding(m);
	    if (!binding.isSynthetic()) {
	      super.printMethod(m);
	    }
	  }

	  private void printImportsAndForwardReferences(CompilationUnit unit, Set<ITypeBinding> forwards) {
	    HeaderImportCollector collector = new HeaderImportCollector();
	    collector.collect(unit, getSourceFileName());
	    Set<ImportCollector.Import> imports = collector.getImports();
	    Set<ImportCollector.Import> superTypes = collector.getSuperTypes();

	    // Print forward declarations.
	    Set<String> forwardStmts = Sets.newTreeSet();
	    for (ImportCollector.Import imp : imports) {
	      forwardStmts.add(createForwardDeclaration(imp.getTypeName(), imp.isInterface()));
	    }
	    for (ITypeBinding forward : forwards) {
	      forwardStmts.add(
	          createForwardDeclaration(NameTable.getFullName(forward), forward.isInterface()));
	    }
	    if (!forwardStmts.isEmpty()) {
	      for (String stmt : forwardStmts) {
	        println(stmt);
	      }
	      newline();
	    }

	    // Print native imports.
	    int endOfImportText = unit.types().isEmpty() ? unit.getLength()
	        : ((ASTNode) unit.types().get(0)).getStartPosition();
	    @SuppressWarnings("unchecked")
	    List<Comment> comments = unit.getCommentList(); // safe by definition
	    for (Comment c : comments) {
	      int start = c.getStartPosition();
	      if (start >= endOfImportText) {
	        break;
	      }
	      if (c instanceof BlockComment) {
	        String nativeImport = extractNativeCode(start, c.getLength());
	        if (nativeImport != null) {  // if it has a JSNI section
	          println(nativeImport.trim());
	        }
	      }
	    }

	    // Print collected imports.
//	    println("#import \"JreEmulation.h\"");
	    if (!superTypes.isEmpty()) {
	      Set<String> importStmts = Sets.newTreeSet();
	      for (ImportCollector.Import imp : superTypes) {
	        importStmts.add(createImport(imp));
	      }
	      for (String stmt : importStmts) {
	        println(stmt);
	      }
	    }
	  }

	  protected String createForwardDeclaration(String typeName, boolean isInterface) {
	    return String.format("@%s %s;", isInterface ? "protocol" : "class", typeName);
	  }

	  protected String createImport(ImportCollector.Import imp) {
	    return String.format("#include \"%s.h\"", imp.getImportFileName());
	  }

	  /**
	   * If an inner type is defined before any of its super types are, put the
	   * super types first.  Otherwise, keep the order as is, to make debugging
	   * easier.  For field and method references to a following type, add a
	   * forward type.
	   *
	   * @return the set of forward types to declare.
	   */
	  static Set<ITypeBinding> sortTypes(List<AbstractTypeDeclaration> types) {
	    final List<AbstractTypeDeclaration> typesCopy =
	        new ArrayList<AbstractTypeDeclaration>(types);

	    final Map<String, AbstractTypeDeclaration> index = Maps.newHashMap();
	    for (AbstractTypeDeclaration type : typesCopy) {
	      index.put(Types.getTypeBinding(type).getBinaryName(), type);
	    }

	    final Multimap<String, String> references = HashMultimap.create();
	    final Multimap<String, String> superTypes = HashMultimap.create();

	    // Collect all references to other types, but track super types
	    // separately from other references.
	    for (AbstractTypeDeclaration type : typesCopy) {
	      final String typeName = Types.getTypeBinding(type).getBinaryName();

	      ErrorReportingASTVisitor collector = new ErrorReportingASTVisitor() {
	        protected void addSuperType(Type type) {
	          ITypeBinding binding = type == null ? null : Types.getTypeBinding(type);
	          if (binding != null && isMember(binding)) {
	            superTypes.put(typeName, binding.getBinaryName());
	          }
	        }

	        private void addReference(Type type) {
	          ITypeBinding binding = type == null ? null : Types.getTypeBinding(type);
	          if (binding != null && isMember(binding)) {
	            references.put(typeName, binding.getBinaryName());
	          }
	        }

	        // Only collect references to types members.
	        private boolean isMember(ITypeBinding binding) {
	          return index.containsKey(binding.getBinaryName());
	        }

	        @Override
	        public boolean visit(FieldDeclaration node) {
	          addReference(node.getType());
	          return true;
	        }

	        @Override
	        public boolean visit(MethodDeclaration node) {
	          addReference(node.getReturnType2());
	          @SuppressWarnings("unchecked")
	          List<SingleVariableDeclaration> params = node.parameters();
	          for (SingleVariableDeclaration param : params) {
	            addReference(param.getType());
	          }
	          return true;
	        }

	        @Override
	        public boolean visit(TypeDeclaration node) {
	          ITypeBinding binding = Types.getTypeBinding(node);
	          if (binding.isEqualTo(Types.getNSObject())) {
	            return false;
	          }
	          addSuperType(node.getSuperclassType());
	          for (Iterator<?> iterator = node.superInterfaceTypes().iterator(); iterator.hasNext();) {
	            Object o = iterator.next();
	            if (o instanceof Type) {
	              addSuperType((Type) o);
	            } else {
	              throw new AssertionError("unknown AST type: " + o.getClass());
	            }
	          }
	          return true;
	        }
	      };
	      collector.run(type);
	    }

	    // Do a topological sort on the types declared in this unit, with an edge
	    // in the graph denoting a type inheritance. Super types will end up
	    // higher up in the sort.

	    types.clear();
	    LinkedHashSet<AbstractTypeDeclaration> rootTypes = Sets.newLinkedHashSet();
	    for (AbstractTypeDeclaration type : typesCopy) {
	      String name = Types.getTypeBinding(type).getBinaryName();
	      if (!superTypes.containsValue(name)) {
	        rootTypes.add(type);
	      }
	    }

	    while (!rootTypes.isEmpty()) {
	      AbstractTypeDeclaration type =
	          (AbstractTypeDeclaration) rootTypes.toArray()[rootTypes.size() - 1];
	      rootTypes.remove(type);
	      types.add(0, type);

	      ITypeBinding binding = Types.getTypeBinding(type);
	      String typeName = binding.getBinaryName();
	      // Copy the values to avoid a ConcurrentModificationException.
	      List<String> values = Lists.newArrayList(superTypes.get(typeName));
	      for (String superTypeName : values) {
	        superTypes.remove(typeName, superTypeName);
	        if (!superTypes.containsValue(superTypeName)) {
	          AbstractTypeDeclaration superType = index.get(superTypeName);
	          rootTypes.add(superType);
	        }
	      }
	    }

	    assert types.size() == typesCopy.size();

	    // For all other references, if the referred to type is declared
	    // after the reference, add a forward reference.
	    final Set<ITypeBinding> moreForwardTypes = Sets.newHashSet();
	    for (Map.Entry<String, String> entry : references.entries()) {
	      AbstractTypeDeclaration referrer = index.get(entry.getKey());
	      AbstractTypeDeclaration referred = index.get(entry.getValue());
	      if (types.indexOf(referred) > types.indexOf(referrer)) {
	        // Referred to type occurs after the reference; add a forward decl
	        moreForwardTypes.add(Types.getTypeBinding(referred));
	      }
	    }

	    return moreForwardTypes;
	  }

	  private void printInstanceVariables(FieldDeclaration[] fields) {
	    indent();
	    String lastAccess = "@protected";
	    for (FieldDeclaration field : fields) {
	      if ((field.getModifiers() & Modifier.STATIC) == 0) {
	        @SuppressWarnings("unchecked")
	        List<VariableDeclarationFragment> vars = field.fragments(); // safe by definition
	        assert !vars.isEmpty();
	        VariableDeclarationFragment var = vars.get(0);
	        if (var.getName().getIdentifier().startsWith("this$") && superDefinesVariable(var)) {
	          // Don't print, as it shadows an inner field in a super class.
	          continue;
	        }
	        String access = accessScope(field.getModifiers());
	        if (!access.equals(lastAccess)) {
	          print(' ');
	          println(access);
	          lastAccess = access;
	        }
	        printIndent();
	        if (Types.isWeakReference(Types.getVariableBinding(var)) && Options.useARC()) {
	          print("__weak ");
	        }
	        ITypeBinding varType = Types.getTypeBinding(vars.get(0));
	        String objcType = NameTable.javaRefToObjC(varType);
	        boolean needsAsterisk = !varType.isPrimitive() && !objcType.matches("id|id<.*>|Class");
	        if (needsAsterisk && objcType.endsWith(" *")) {
	          // Strip pointer from type, as it will be added when appending fragment.
	          // This is necessary to create "Foo *one, *two;" declarations.
	          objcType = objcType.substring(0, objcType.length() - 2);
	        }
	        print(objcType);
	        print(' ');
	        for (Iterator<?> it = field.fragments().iterator(); it.hasNext(); ) {
	          VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
	          if (needsAsterisk) {
	            print('*');
	          }
	          String name = NameTable.getName(f.getName());
	          print(NameTable.javaFieldToObjC(name));
	          if (it.hasNext()) {
	            print(", ");
	          }
	        }
	        println(";");
	      }
	    }
	    unindent();
	  }

	  private void printProperties(FieldDeclaration[] fields) {
	    int nPrinted = 0;
	    for (FieldDeclaration field : fields) {
	      if ((field.getModifiers() & Modifier.STATIC) == 0) {
	        ITypeBinding type = Types.getTypeBinding(field.getType());
	        @SuppressWarnings("unchecked")
	        List<VariableDeclarationFragment> vars = field.fragments(); // safe by definition
	        for (VariableDeclarationFragment var : vars) {
	          if (var.getName().getIdentifier().startsWith("this$") && superDefinesVariable(var)) {
	            // Don't print, as it shadows an inner field in a super class.
	            continue;
	          }
	          print("@property (nonatomic, ");
	          IVariableBinding varBinding = Types.getVariableBinding(var);
	          if (type.isPrimitive()) {
	            print("assign");
	          } else if (Types.isWeakReference(varBinding) ||
	              (varBinding.getName().startsWith("this$") &&
	                  Types.hasWeakAnnotation(varBinding.getDeclaringClass()))) {
	            print(Options.useARC() ? "weak" : "assign");
	          } else if (type.isEqualTo(Types.getNSString())) {
	            print("copy");
	          } else {
	            print(Options.useARC() ? "strong" : "retain");
	          }
	          String typeString = NameTable.javaRefToObjC(type);
	          if (!typeString.endsWith("*")) {
	            typeString += " ";
	          }
	          println(String.format(") %s%s;", typeString, NameTable.getName(var.getName())));
	          nPrinted++;
	        }
	      }
	    }
	    if (nPrinted > 0) {
	      newline();
	    }
	  }

	  private void printConstantDefines(AbstractTypeDeclaration node) {
	    ITypeBinding type = Types.getTypeBinding(node);
	    boolean hadConstant = false;
	    for (IVariableBinding field : type.getDeclaredFields()) {
	      if (Types.isPrimitiveConstant(field)) {
	        printf("#define %s ", NameTable.getPrimitiveConstantName(field));
	        Object value = field.getConstantValue();
	        assert value != null;
	        if (value instanceof Boolean) {
	          println(((Boolean) value).booleanValue() ? "TRUE" : "FALSE");
	        } else if (value instanceof Character) {
	          char c = ((Character) value).charValue();
	          String convertedChar = UnicodeUtils.escapeCharacter(c);
	          if (convertedChar != null) {
	            if (convertedChar.equals("'") || convertedChar.equals("\\")) {
	              printf("'\\%s'\n", convertedChar);
	            } else {
	              printf("'%s'\n", convertedChar);
	            }
	          } else {
	            // The Java char constant is likely not a valid C character; just
	            // print it as an int.
	            printf("0x%4x\n", c & 0xffff);
	          }
	        } else if (value instanceof Long) {
	          long l = ((Long) value).longValue();
	          if (l == Long.MIN_VALUE) {
	            println("-0x7fffffffffffffffLL - 1");
	          } else {
	            println(value.toString());
	          }
	        } else if (value instanceof Integer) {
	          long l = ((Integer) value).intValue();
	          if (l == Integer.MIN_VALUE) {
	            println("-0x7fffffff - 1");
	          } else {
	            println(value.toString());
	          }
	        } else if (value instanceof Float) {
	          float f = ((Float) value).floatValue();
	          if (Float.isNaN(f)) {
	            println("NAN");
	          } else if (f == Float.POSITIVE_INFINITY) {
	            println("INFINITY");
	          } else if (f == Float.NEGATIVE_INFINITY) {
	            // FP representations are symmetrical.
	            println("-INFINITY");
	          } else if (f == Float.MAX_VALUE) {
	            println("__FLT_MAX__");
	          } else if (f == Float.MIN_VALUE) {
	            println("__FLT_MIN__");
	          } else {
	            println(value.toString());
	          }
	        } else if (value instanceof Double) {
	          double d = ((Double) value).doubleValue();
	          if (Double.isNaN(d)) {
	            println("NAN");
	          } else if (d == Double.POSITIVE_INFINITY) {
	            println("INFINITY");
	          } else if (d == Double.NEGATIVE_INFINITY) {
	            // FP representations are symmetrical.
	            println("-INFINITY");
	          } else if (d == Double.MAX_VALUE) {
	            println("__DBL_MAX__");
	          } else if (d == Double.MIN_VALUE) {
	            println("__DBL_MIN__");
	          } else {
	            println(value.toString());
	          }
	        } else {
	          println(value.toString());
	        }
	        hadConstant = true;
	      }
	    }
	    if (hadConstant) {
	      newline();
	    }
	  }

	  private String accessScope(int modifiers) {
	    if (Options.inlineFieldAccess()) {
	      // Need direct access to fields possibly from inner classes that are
	      // promoted to top level classes, so must make all fields public.
	      return "@public";
	    }

	    if ((modifiers & Modifier.PUBLIC) > 0) {
	      return "@public";
	    }
	    if ((modifiers & Modifier.PROTECTED) > 0) {
	      return "@protected";
	    }
	    if ((modifiers & Modifier.PRIVATE) > 0) {
	      return "@private";
	    }
	    return "@package";
	  }
	}
