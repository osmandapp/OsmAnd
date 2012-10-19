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

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.eclipse.jdt.core.dom.AST;

import java.util.List;
import java.util.regex.Pattern;

/**
 * An immutable description of an iOS method parameter, containing its name
 * and type.  It also stores its indices in the method and its mapped Java
 * method -- this supports reordering parameters when needed.
 *
 * @see IOSMethod
 * @author Tom Ball
 */
public class IOSParameter {
    private final String name;
    private final String type;
    private final int index;
    private final boolean isVarArgs;

    static final Splitter IOS_PARAMETER_SPLITTER =
      Splitter.on(Pattern.compile("(:|\\(|\\)|\\{|\\})")).omitEmptyStrings().trimResults();

    public IOSParameter(String s, int index, AST ast) {
      if (s.equals("...")) {
        name = "values";
        type = null;
        this.index = 1;
        isVarArgs = true;
      } else {
        List<String> argParts = Lists.newArrayList(IOS_PARAMETER_SPLITTER.split(s));
        assert argParts.size() == 3 : "bad iOS method description: " + s;

        name = argParts.get(0);
        type = argParts.get(1);
        this.index = index;
        isVarArgs = false;
      }
    }

    public String getParameterName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public int getIndex() {
      return index;
    }

    public boolean isVarArgs() {
      return isVarArgs;
    }
  }
