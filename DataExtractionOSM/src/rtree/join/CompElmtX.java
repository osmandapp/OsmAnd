//CompElmtX.java
//  
//This library is free software; you can redistribute it and/or
//modify it under the terms of the GNU Lesser General Public
//License as published by the Free Software Foundation; either
//version 2.1 of the License, or (at your option) any later version.
//  
//This library is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//Lesser General Public License for more details.
package rtree.join;

import java.util.Comparator;
import rtree.*;

/**
   An internal Comparable class to sort Element.
   @author Prachuryya Barua
*/
public class CompElmtX implements Comparator
{
  @Override
public int compare(Object o1, Object o2)
  {
    if(o1 instanceof Element && o2 instanceof Element){
      Rect r1 = ((Element)o1).getRect();
      Rect r2 = ((Element)o2).getRect();
      return r1.getMinX() - r2.getMinX();
      //if(r1.getMinX() <= r2.getMinX())
      //return -1;
      //else if(r1.getMinX() == r2.getMinX())
      //return 0;
      //else
      //return 1;
    }
    //  else if(o1 == null && o2 != null){
    //    return -1;
    //  }else if(o1 != null && o2 == null){
    //    return 1;
    //  }else if(o1 == null && o2 == null){
    //    return Integer.MAX_VALUE;
    //}
    else{
      throw new ClassCastException("Rect.compareTo : wrong object(s) passed : "
                                   +o1.getClass().getName() + " o2 " + o2.getClass().getName());
    }
  }
  @Override
public boolean equals(Object o)
  {
    return true;
  }
}
