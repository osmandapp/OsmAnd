//ContainedByPred.java
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
public class ContainedByPred extends Predicate
{
  public ContainedByPred(){}

  public void relate(Element event, int from, Element[] others, List pairs, int evtSide)
  {
    //System.out.println("SweepLine.internalLoop : before pairs size : " + pairs.size());
    if(evtSide == Join.RIGHT){
      for(int i=from; 
          (i<others.length) && (others[i] != null) && 
            (others[i].getRect().getMaxX() < event.getRect().getMaxX());
          i++){//while others are still intersecting with the event
        if(event.getRect().getMinX() < others[i].getRect().getMinX() &&//check x as well
           event.getRect().getMinY() < others[i].getRect().getMinY() &&//check the y coordinate
           event.getRect().getMaxY() > others[i].getRect().getMaxY()){
          if(evtSide == Join.LEFT)
            pairs.add(p.paired(event, others[i]));
          else
            pairs.add(p.paired(others[i], event));
        }//if
      }//for
      //System.out.println("SweepLine.internalLoop : after pairs size : " + pairs.size());
    }else{
      //do nothing and see the comments in ContainsPredicate
    }
  }

  /**
     @param side The side of <code>elmt1</code>.
  */
  public boolean relateMismatch(Element elmt1, Element elmt2, int side)
  {
    try{
      if(elmt1 instanceof NonLeafElement || elmt2 instanceof NonLeafElement)
        return elmt1.getRect().overlaps(elmt2.getRect());
      else{
        if(side == Join.LEFT)
          return elmt1.getRect().containedBy(elmt2.getRect());
        else
          return elmt2.getRect().containedBy(elmt1.getRect());
      }
    }catch(Exception e){
      e.printStackTrace();
      return false;
    }
  }
}
