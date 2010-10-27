//Pair.java
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

import rtree.*;


/**
 * This keeps a pair of intersection between two trees.
 * Basically the purpose of <code>Pair</code> and <code>PairElmt</code> is that the caller can get
 * pairs of pointers or pairs elements from the join operations. 
 @author Prachuryya Barua
*/
public class Pair
{
  protected long left;
  protected long right;
  private Pair(long left, long right)
  {
    this.left = left;
    this.right = right;
  }

  public Pair(){}

  public long getLtPtr()
  {
    return left;
  }
  public long getRtPtr()
  {
    return right;
  }
  /**
     Returns an object <code>this</code> type.
  */
  public Pair paired(Element ltElmt, Element rtElmt)
  {
    return new Pair(ltElmt.getPtr(), rtElmt.getPtr());
  }
}
