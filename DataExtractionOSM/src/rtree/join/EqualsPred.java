//EqualsPred.java
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
public class EqualsPred extends Predicate
{
  public EqualsPred(){}
  //  EqualsPred(Pair p)
  //  {
  //    super(p);
  //  }
  @Override
public void relate(Element event, int from, Element[] others, List pairs, int evtSide)
  {
    //System.out.println("SweepLine.internalLoop : before pairs size : " + pairs.size());
    for(int i=from; 
        (i<others.length) && (others[i] != null) && 
          (others[i].getRect().getMinX() <= event.getRect().getMaxX());
        i++){//while others are still intersecting with the event
      if(event.getRect().getMinY() < others[i].getRect().getMaxY() &&
         event.getRect().getMaxY() > others[i].getRect().getMinY()){//check the y coordinate
        if(evtSide == Join.LEFT)
          pairs.add(p.paired(event, others[i]));
        else
          pairs.add(p.paired(others[i], event));
      }//if
    }//for
    //System.out.println("SweepLine.internalLoop : after pairs size : " + pairs.size());
  }
  //  public boolean relateMismatch(Element nlElmt, Element lfElmt)
  //  {
  //    try{
  //      if(nlElmt instanceof NonLeafElement || lfElmt instanceof NonLeafElement)
  //        return nlElmt.getRect().overlaps(lfElmt.getRect());
  //      else
  //        return nlElmt.getRect().equals(lfElmt.getRect());
  //    }catch(Exception e){
  //      e.printStackTrace();
  //      return false;
  //    }
  //}
  @Override
public boolean relateMismatch(Element elmt1, Element elmt2, int side)
  {
    try{
      if(elmt1 instanceof NonLeafElement || elmt2 instanceof NonLeafElement)
        return elmt1.getRect().overlaps(elmt2.getRect());
      else{
        return elmt1.getRect().equals(elmt2.getRect());
      }
    }catch(Exception e){
      e.printStackTrace();
      return false;
    }
  }
}
