//MeetPred.java
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
   The "intersects" binary predicate. This is only for the sweepline algo.
   @author Prachuryya Barua
*/
public class MeetPred extends Predicate
{
  public MeetPred(){}

  public void relate(Element event, int from, Element[] others, List pairs, int evtSide)
  {
    (new IntersectPred()).relate(event, from, others, pairs, evtSide);
  }
  
  public boolean relateMismatch(Element nlElmt, Element lfElmt, int side)
  {
    try{
      //if(nlElmt instanceof NonLeafElement)
      return nlElmt.getRect().overlaps(lfElmt.getRect());
      //else
      //return nlElmt.getRect().meet(lfElmt.getRect());
    }catch(Exception e){
      e.printStackTrace();
      return false;
    }
  }
}
