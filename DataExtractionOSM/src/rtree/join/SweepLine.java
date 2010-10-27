//SweepLine.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import rtree.*;
import java.util.Comparator;

/**
   @author Prachuryya Barua
*/
public class SweepLine
{
  //Pair p = new Pair();
  IntersectPred intPred = new IntersectPred();
  Predicate pred = null;
  public SweepLine()
  {
  }

  public SweepLine(Predicate pred)
  {
    //this.p = p;
    this.pred = pred;
    //intPred = new IntersectPred();
  }

  public void setPredicate(Predicate pred)
  {
    this.pred = pred;
  }
  public Predicate getPredicate()
  {
    return pred;
  }
  public void sort(Rect[] rects)
  {
    Arrays.sort(rects, new CompRectX());
  }
  public void sort(Element[] elmts)
  {
    Arrays.sort(elmts, new CompElmtX());
  }

  /**
   * This method will return all the pointers of <code>elmts</code> which intersect with
   * <code>rect</code>. It is expected that <code>elmts</code> are sorted acording to <code>minX</code>
   * @return either a <code>List</code> of eithers pointers or ehole elements depending upon <code>p</code>.
   */
  public List intersects(Rect rect, Element[] elmts)
  {
    if(elmts == null || elmts.length < 1)
      return null;
    Element dummy;
    if(elmts[0] instanceof LeafElement)
      dummy = new LeafElement(rect, 128);
    else
      dummy = new NonLeafElement(rect, 128);
    Element[] dummyArr = new Element[1];
    dummyArr[0] = dummy;
    return sortedIntersectionTest(dummyArr, elmts);
  }
  /**
   * This method applies the sweep line algorithm to the two given set of elements.
   * It is not necessary to have the two arrays sorted (by minX).
   */
  public List intersects(Element[] ltElmts,Element[] rtElmts)
  {
    sort(ltElmts);
    sort(rtElmts);
    return sortedIntersectionTest(ltElmts, rtElmts);
  }
  
  /**
     Does the sweep sort on the two sets. Assumes that both the set of elements are of the same type.
     @param ltElmts The sorted elements(by minX) of the left node.
     @param rtElmts The sorted elements(by minX) of the right node.
     @return A <code>List</code> of <code>Pair</code> of elements
  */
  public List sortedIntersectionTest(Element[] ltElmts, Element[] rtElmts)
  {
    int i = 0;//loop cntr for left
    int j = 0;//loop cntr for right
    List pairs = new ArrayList();
    while((i < ltElmts.length) && (ltElmts[i] != null) && 
          (j < rtElmts.length) && (rtElmts[j] != null)){
      if(ltElmts[i].getRect().getMinX() < rtElmts[j].getRect().getMinX()){//event at left

        if(ltElmts[i] instanceof NonLeafElement){
          intPred.relate(ltElmts[i], j, rtElmts, pairs, Join.LEFT);
        }else{
          pred.relate(ltElmts[i], j, rtElmts, pairs, Join.LEFT);
        }
        //System.out.println("SweepLine.sortedIntersectionTest : total pairs " + pairs.size());
        i++;
      }else{
        if(rtElmts[j] instanceof NonLeafElement){
          intPred.relate(rtElmts[j], i, ltElmts, pairs, Join.RIGHT);
        }else{
          pred.relate(rtElmts[j], i, ltElmts, pairs, Join.RIGHT);
        }
        //System.out.println("SweepLine.sortedIntersectionTest : total pairs " + pairs.size());
        j++;
      }
    }
    return pairs;
  }
  
  /**
     @param evtSide tells whether <code>event</code> is from left tree or right tree.
  
     private void internalLoop(Element event, int from, Element[] others, List pairs, int evtSide)
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
  */
}

/**
   An internal Comparable class to sort Rect.
*/
class CompRectX implements Comparator
{
  public int compare(Object o1, Object o2)
  {
    if(o1 instanceof Rect && o2 instanceof Rect){
      Rect r1 = (Rect)o1;
      Rect r2 = (Rect)o2;
      if(r1.getMinX() <= r2.getMinX())
        return -1;
      else if(r1.getMinX() == r2.getMinX())
        return 0;
      else
        return 1;
    }
    else
      throw new ClassCastException("Rect.compareTo : wrong object(s) passed");
      
  }
  public boolean equals(Object o)
  {
    return true;
  }
}
