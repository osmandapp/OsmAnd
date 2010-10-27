//PairEmlt.java
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
 * This class inherits Pair but represents the whole Element that intersect and not just the pointers.
 * Basically the purpose of <code>Pair</code> and <code>PairElmt</code> is that the caller can get
 * pairs of pointers or pairs elements from the join operations. 
 @author Prachuryya Barua
*/
public class PairElmt extends Pair
{
  private Element ltElmt;
  private Element rtElmt;

  public PairElmt(){}

  private PairElmt(Element ltElmt, Element rtElmt)
  {
    this.ltElmt = ltElmt;
    this.rtElmt = rtElmt;
  }
  
  public long getLtPtr()
  {
    return ltElmt.getPtr();
  }
  public long getRtPtr()
  {
    return rtElmt.getPtr();
  }
  public Element getLtElmt()
  {
    return ltElmt;
  }

  public Element getRtElmt()
  {
    return rtElmt;
  }
  public Pair paired(Element ltElmt, Element rtElmt)
  {
    return new PairElmt(ltElmt, rtElmt);
  }

}
