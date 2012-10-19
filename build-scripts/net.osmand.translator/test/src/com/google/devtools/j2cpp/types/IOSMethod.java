/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2cpp.types;

import com.google.common.collect.Lists;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

/**
 * An immutable description of an iOS method, containing its name, type and
 * parameters.  These descriptions are used when mapping Java method
 * use to equivalent iOS methods.
 *
 * @see JavaMethod
 * @author Tom Ball
 */
public class IOSMethod {
  private final String name;
  private final String declaringClass;
  private final IOSMethodBinding binding;
  private final List<IOSParameter> parameters;
  private boolean varArgs = false;

  public IOSMethod(String s, IMethodBinding binding, AST ast) {
    this(s, binding, binding.getReturnType(), ast);
  }

  public IOSMethod(String s, IMethodBinding binding, ITypeBinding returnType, AST ast) {
    if (s.endsWith(";")) {
      s = s.substring(0, s.length() -1 );
    }
    int i = s.indexOf(' ');
    IOSTypeBinding clazz = Types.resolveIOSType(s.substring(0, i));
    assert clazz != null;
    declaringClass = clazz.getName();
    s = s.substring(i + 1);

    parameters = Lists.newArrayList();
    i = s.indexOf(':');
    if (i > 0) {  // if there are parameters
      name = s.substring(0, i);
      String[] argDefs = splitParameterString(s);
      for (i = 0; i < argDefs.length; i++) {
        IOSParameter param = new IOSParameter(argDefs[i], i, ast);
        parameters.add(param);
        if (param.isVarArgs()) {
          varArgs = true;
          break;
        }
      }
    } else {
      name = s;
    }
    this.binding = new IOSMethodBinding(name, binding, clazz, returnType, varArgs);
  }

  public String getName() {
    return name;
  }

  public String getDeclaringClass() {
    return declaringClass;
  }

  public List<IOSParameter> getParameters() {
    return parameters;
  }

  public IOSMethodBinding resolveBinding() {
    return binding;
  }

  public boolean isVarArgs() {
    return varArgs;
  }

  private String[] splitParameterString(String s) {
    List<String> result = Lists.newArrayList();
    String[] parts = s.split(" ");
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      // If a type has spaces in it (ie, foo *), combine the parts.
      if (part.contains("(")) {
        while (!part.contains(")")) {
          assert i + 1 < parts.length;
          part += ' ' + parts[++i];
        }
      }
      result.add(part);
    }
    return result.toArray(new String[0]);
  }
}
