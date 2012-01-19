//ContainsPred.java
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
public class ContainsPred extends Predicate
{
  public ContainsPred(){}


  @Override
public void relate(Element event, int from, Element[] others, List pairs, int evtSide)
  {
    //System.out.println("SweepLine.internalLoop : before pairs size : " + pairs.size());
    if(evtSide == Join.LEFT){
      for(int i=from; 
          (i<others.length) && (others[i] != null) && 
            (others[i].getRect().getMaxX() < event.getRect().getMaxX());
          i++){//while others are still intersecting with the event
        if(event.getRect().getMinY() < others[i].getRect().getMinY() &&
           event.getRect().getMaxY() > others[i].getRect().getMaxY()){//check the y coordinate
          if(evtSide == Join.LEFT)
            pairs.add(p.paired(event, others[i]));
          else
            pairs.add(p.paired(others[i], event));
        }//if
      }//for
    }else{
      /*do nothing as, if left contains right then left must have appeared before at all cost
        .In other words, if the event took place at a right side then it is not supposed to contain anything.
        We need to concern overselves with events because of left tree only.
      */
    }
    //System.out.println("SweepLine.internalLoop : after pairs size : " + pairs.size());
  }

  /**
     @param side The side of <code>elmt1</code>.
  */
  @Override
public boolean relateMismatch(Element elmt1, Element elmt2, int side)
  {
    //System.out.println("ContainsPred.relateMismatch :");
    
    try{
      if(elmt1 instanceof NonLeafElement || elmt2 instanceof NonLeafElement)
        return elmt1.getRect().overlaps(elmt2.getRect());
      else{
        if(side == Join.LEFT)
          return elmt1.getRect().contains(elmt2.getRect());
        else
          return elmt2.getRect().contains(elmt1.getRect());
      }
    }catch(Exception e){
      e.printStackTrace();
      return false;
    }
  }
}
