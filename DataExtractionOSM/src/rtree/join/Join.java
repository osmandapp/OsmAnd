//Join.java
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

import java.util.ArrayList;
import java.util.List;

/**
	 A class to join two RTrees.
   TODO: 
   Make another buffer just for this algorithm of 512 bytes (or should you?). 
   It is best to have this method's own cache. Read the document well before deciding on it.
   FIXME:
   1) This thing goes out of memory for large randomly generated trees.
   2) The result for point objects are not correct. The extra check done at Rect class is not done at sweep line algorithm.
   3) Better documentation
   
   @author Prachuryya Barua
*/
public class Join
{
  public static final int LEFT = 0;
  public static final int RIGHT = 1;
  private RTree ltTree = null;
  private RTree rtTree = null;
  private Pair p = null;
  SweepLine spLine = new SweepLine();//our sweep line always return a pair of pointers

  /**
     @param left The left tree.
     @param right The right tree.
  */
  public Join(RTree left, RTree right, Pair p, Predicate pred)
  {
    if(left == null || right == null)
      throw new IllegalArgumentException("Join : Argument null");
    this.ltTree = left;
    this.rtTree = right;
    this.p = p;
    spLine.setPredicate(pred);
    //this.pred = pred;
  }

  public void setPairType(Pair p)
  {
    if(p != null)
      this.p = p;
  }
  
  /**
     Will return all the record pointers of the left tree that intersects with the right tree.
     At the moment I assume that the heights of the trees are same.
     @return a <code>List</code> of <code>Pair</code>s of pointers that intersect.
  */
  public List relate()
    throws JoinException
  {
    try{
      //lock the files
      ltTree.getFileHdr().lockRead();
      rtTree.getFileHdr().lockRead();
      List vct = new ArrayList();
      long ltRoot = ltTree.getFileHdr().getRootIndex();
      long rtRoot = rtTree.getFileHdr().getRootIndex();

      /*We can't do anything when we do not have any mbrs in either of the trees*/
      if(ltRoot == Node.NOT_DEFINED || rtRoot == Node.NOT_DEFINED)
        return vct;
      
      Node ltRootNd = ltTree.getReadNode(ltRoot);
      Node rtRootNd = rtTree.getReadNode(rtRoot);
      
      relateRec(ltRootNd, rtRootNd, ltRootNd.getNodeMBR().intersection(rtRootNd.getNodeMBR()), vct);
      return vct;
    }catch(Exception e){
      e.printStackTrace();
      throw new JoinException("Join.intersectsInt : " + e.getMessage());
    }finally{
      ltTree.getFileHdr().lockRead();
      rtTree.getFileHdr().lockRead();
    }
  }
  /**
     @param ltNode
     @param rtNode
     @param ret 
     @param intsect The intersection between the two <code>Node</code>.
     @param ret A <code>List</code> that would be filled with the pairs that intersect.
  */
  private void relateRec(Node ltNode, Node rtNode, Rect intsect, List ret)
    throws Exception
  {
    if(ltNode == null || rtNode == null)
      throw new IllegalValueException("Join.intersectRec : Argument(s) null");
    Element[] ltElmts = ltNode.getAllElements();
    Element[] rtElmts = rtNode.getAllElements();
    
    if(ltNode.getElementType() == Node.NONLEAF_NODE && 
       rtNode.getElementType() == Node.LEAF_NODE){//both sides are of different types
      ret.addAll(joinMismatch(ltElmts, rtElmts, Join.LEFT));
    }else if(ltNode.getElementType() == Node.LEAF_NODE && 
             rtNode.getElementType() == Node.NONLEAF_NODE){//both sides are of different types
      ret.addAll(joinMismatch(rtElmts, ltElmts, Join.RIGHT));
    }else {//either both are leaf or both non-leaf
      //this is where I remove elemensts which do no intersect with the intersection rectangle
      ltElmts = filterRect(ltElmts, intsect);
      rtElmts = filterRect(rtElmts, intsect);
      List pairs = spLine.sortedIntersectionTest(ltElmts, rtElmts);//get the intersecting pairs
      for(int i=0; i<pairs.size(); i++){//for each pair
        PairElmt intPair = (PairElmt)pairs.get(i);//the intersecting pair at i
        if(intPair.getLtElmt() instanceof NonLeafElement && 
           intPair.getRtElmt() instanceof NonLeafElement){//both are non leaf elements
          Node newLtNode = ltTree.getReadNode(intPair.getLtPtr());
          Node newRtNode = rtTree.getReadNode(intPair.getRtPtr());
          relateRec(newLtNode, newRtNode, newLtNode.getNodeMBR().intersection(newRtNode.getNodeMBR()),
                    ret);
        }else if(intPair.getLtElmt() instanceof LeafElement && 
                 intPair.getRtElmt() instanceof LeafElement){//LeafElement
          ret.add(p.paired(intPair.getLtElmt(), intPair.getRtElmt()));
          //System.out.println("Join.intersectRec with size of ret - leaf " + ret.size());
        }
      }//for
    }
  }
  /**
     Joins two nodes of different types.
     @param nlElmts non-leaf elements
     @param lfElmts leaf elements
     @param side The side of <code>nlElmts</code>. (Join.LEFT or Join.RIGHT)
     @return Pair of joins between leaf and non leaf elements (after window query).
  */
  private List joinMismatch(Element[] nlElmts, Element[] lfElmts, int side)
    throws Exception
  {
    List ret = new ArrayList();
    for(int i=0; (i<nlElmts.length) && (nlElmts[i] != null); i++){//for each elmt in non-leaf
      for(int j=0; (j<lfElmts.length) &&(lfElmts[j] != null); j++){
        if(nlElmts[i].getRect().overlaps(lfElmts[j].getRect())){
          if(side == Join.LEFT)
            ret.addAll(windowQuery(ltTree.getReadNode(nlElmts[i].getPtr()),
                                   (LeafElement)lfElmts[j], side ));
          else//non leaf is the right tree
            ret.addAll(windowQuery(rtTree.getReadNode(nlElmts[i].getPtr()),
                                   (LeafElement)lfElmts[j], side ));
          
        }//if
      }//for leaf-elements - j
    }//for non-leaf-elements - i
    return ret;
  }

  /**
   * This method actually performs a simple window query on <code>nlNode</code> (a non-leaf Node).
   * Will return a <code>List</code> of <code>Pair</code>. The pair is made accordingly <code>side</code>.
   * @param side The side of <code>nlNode</code>.
   */
  private List windowQuery(Node nlNode, LeafElement lfElmt, int side)
    throws Exception
  {
    RTree nlTree = null;//the non leaf tree
    if(side == Join.LEFT) 
      nlTree = ltTree;
    else
      nlTree = rtTree;
    List list = new ArrayList();
    Element[] elmts = nlNode.getAllElements();
    int totElements = nlNode.getTotalElements();
    for(int i=0; i<totElements; i++){//for every element; we can use sweepline algorithm here
      //if(elmts[i].getRect().overlaps(lfElmt.getRect())){ //select elements that overlap
      if(spLine.getPredicate().relateMismatch(elmts[i], lfElmt, side)){//select elements that overlap
        if(elmts[i].getElementType() == Node.NONLEAF_NODE){//non leaf
          list.addAll(windowQuery( nlTree.getReadNode(elmts[i].getPtr()), lfElmt, side));
        }
        else{//if leaf element
          if(side == Join.LEFT)//here we add another condiation of the specified predicate
            list.add(p.paired(elmts[i], lfElmt));
          else
            list.add(p.paired(lfElmt, elmts[i]));
        }
      }
    }
    return list;
  }
  /**
     This method removes those elements from <code>elmts</code> that do not intersect <code>rect</code>.
     The original elements, <code>elmts</code>, are not modified.
  */
  private Element[] filterRect(Element[] elmts, Rect rect)
    throws Exception
  {
    Element[] ret = new Element[elmts.length];
    int j=0;
    for(int i=0; i<elmts.length && elmts[i] != null; i++)
      if(rect.overlaps(elmts[i].getRect()))
        //ret[j++] = (Element)elmts[i].clone();//i hope java does not remove columns
        ret[j++] = elmts[i];
    return ret;
  }
}

