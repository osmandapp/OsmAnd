//Predicate.java
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
import java.util.List;

/**
   This class works with "sweepline" algo. For the actual binary predicate between MBRs, this interface 
   should help. We will have an implementing class for "intersects","contains","meet".
   @author Prachuryya Barua
*/
public abstract class Predicate
{
  protected Pair p = new PairElmt();//this is always PairElmt

  public Predicate(){}

  //  public Predicate(Pair p)
  //  {
  //    if(p == null)
  //      throw new NullPointerException(" Argument null");
  //    this.p = p;
  //  }
  
  /**
   * Do the appropriate rtree operation.
   * @param event the <code>Element</code> at which the event took place.
   * @param from the index from which <code>others</code> start.
   * @param others the <code>Element[]</code> to compare with <code>event</code>
   * @param pairs a <code>List</code> value where the output pairs would put.
   * @param evtSide tells whether <code>event</code> is from left tree or right tree.
   * @param p a <code>Pair</code> value which specifies the kind of output required.
   */
  public abstract void relate(Element event, int from, Element[] others, List pairs, int evtSide);
  
  /**
   * This one is specifically for the case where one tree is longer then the other.
   * It may be noted that both the elements can be leaf as well.
   * @param nlElmt the non-leaf <code>Element</code> 
   * @param lfElmt the leaf <code>Element</code>
   * @param side The side of <code>nlElmt</code> element.
   * @return a <code>boolean</code> value
   */
  public abstract boolean relateMismatch(Element nlElmt, Element lfElmt, int side);
  /**
     Returns the <code>Pair</code> which tells the type of output required.
  */
  public Pair getPair()
  {
    return p;
  }
}
