//SdTree.java
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
package rtree.seeded;

import java.io.FileNotFoundException;
import java.io.IOException;

import rtree.Element;
import rtree.IllegalValueException;
import rtree.Node;
import rtree.NodeFullException;
import rtree.NodeReadException;
import rtree.NodeWriteException;
import rtree.NonLeafElement;
import rtree.RTree;
import rtree.RTreeException;
import rtree.Rect;

/**
   This is a seeded class is good only for joining with the seeding class and not for window 
   queries.
   How to use:
   1) Call the constructor.
   2) For each element to be inserted call <code>growLeaf</code>
   3) Positively call cleanup
   4) If you want you can create another rtree object from this file then flush the seed tree. Remember if
   this tree is used for query purpose and not going to be used again, there is no need to flush.
   @author Prachuryya Barua
*/
public class SdTree extends RTree
{
  private String seedName = null;
  private RTree sdingTree = null;
  private int slotLvl = 1;

  /**
     @param fileName The rtree filr name of this seed tree.
     @param sdingTree The <code>RTree</code> from which to start seeding.
  */
  public SdTree(String fileName, RTree sdingTree)
    throws RTreeException
  {
    super(fileName);//this method should do its own locking
    try{
      System.out.println("SdTree:: sedding height is " + sdingTree.getHeight());
      fileHdr.lockWrite();
      try{
        fileHdr.setBufferPolicy(false);
        if(sdingTree == null)
          throw new IllegalArgumentException("SdTree: Seeding tree is null");
        this.sdingTree = sdingTree;
        setSlot();
        if(slotLvl >= 1)
          seed();
        //growLeaf();
      }catch(Exception e){throw new RTreeException(e.getMessage());}
    }finally{
      fileHdr.unlock();
    }
  }
  private void setSlot()
  {
    int ht = sdingTree.getHeight();
    switch(ht){
    case 0 : slotLvl = Node.NOT_DEFINED;
      break;
    case 1 : slotLvl = Node.NOT_DEFINED;
      break;
    case 2 : slotLvl = Node.NOT_DEFINED;
      break;
    case 3 : slotLvl = 1;
      break;
    case 4 : slotLvl = 2;
      break;
    }
  }
  /**
     Start seeding - take the root node, copy it to this tree, keep on copying until the slot level.
     This method overwrites the root irrespective of its existance or nonexistence.
  */
  private void seed()
    throws RTreeException
  {
    try{
      long sdingRoot = sdingTree.getFileHdr().getRootIndex();
      //somehow remove all the nodes of this tree from the cache and since we have a write lock
      //nobody can get this tree's nodes on to the buufer if we don't
      
      Node sdingNode = sdingTree.getReadNode(sdingRoot);
      seedRec(sdingNode, chdNodes.getNode(fileHdr.getFile(), fileName, Node.NOT_DEFINED,//sd
                                          sdingNode.getElementType(), fileHdr), 0);
    }catch(Exception e){
      e.printStackTrace();
      throw new RTreeException(e.getMessage());
    }
  }
  /**
     @param sdingNode The seeding node from which to copy (source).
     @param level The height at which this node falls in the tree.
  */
  private void seedRec(Node sdingNode, Node sdNode, int level)//sd
    throws Exception
  {
    if(sdingNode.getElementType() == Node.LEAF_NODE)
      throw new IllegalArgumentException("SdTree.seedRec : Cannot seed a leaf node");

    //make the child nodes before hand so that we know their indices in file
    Node[] chNodes = null;//sd
    if(level != slotLvl)//we do not need to alocate new nodes if we are at slot level
      chNodes = new Node[sdingNode.getTotalElements()];//sd
      
    Element[] elmts = sdingNode.getAllElements();
    Element[] newElmts = null;//elements for non-slot levels

    if(level != slotLvl)
      newElmts = new Element[sdingNode.getTotalElements()];//non slots have multiple elements
    else{
      newElmts = new Element[1];//slot has only one element
      newElmts[0] = new NonLeafElement(new Rect(), Node.NOT_DEFINED);//element for slot level
    }
    for(int i=0; i<sdingNode.getTotalElements(); i++){//for each element in the seeding node

      if(level != slotLvl){
        newElmts[i] = (NonLeafElement)((NonLeafElement)elmts[i]).clone();//we do not seed leaf elements
        chNodes[i] = chdNodes.getNode(fileHdr.getFile(), fileName, sdNode.getNodeIndex(),//sd
                                      sdingNode.getElementType(), fileHdr);
        newElmts[i].setPtr(chNodes[i].getNodeIndex());//update the child pointers for new node
        seedRec(sdingTree.getReadNode(elmts[i].getPtr()), chNodes[i], level+1);
      }else{//this is the slot level
        /*What we do here is that we put only one element into the slot node instead of all the elements.
          This would result in a single element in the node which represents all the seding node elements
          with a null pointer.
        */
        newElmts[0].getRect().expandToInclude(elmts[i].getRect());
      }//else
    }//for
    sdNode.insertElement(newElmts, false);//copy the non-slot elements now //sd
  }

  public void growLeaf(Element elmt)
    throws RTreeException
  {
    if(slotLvl == Node.NOT_DEFINED){
      try{
        insert(elmt);
      }catch(Exception e){
        throw new RTreeException(e.getMessage());
      }
    }else{
      fileHdr.lockWrite();
      try{
        long root = fileHdr.getRootIndex();
        //Long slotIndex = null;
        LongWraper slotIndex = new LongWraper();
        Node node = this.chooseLeaf(elmt, slotIndex);//sd
        
        if(slotIndex == null)
          throw new NullPointerException();
        long nodeParent = node.getParent();
        Node[] newNodes = new Node[2];
        try{
          node.insertElement(elmt);//if another insert is possible
          newNodes[0] = node;
          newNodes[1] = null;
        }catch(NodeFullException e){ //if another insert is not possible
          newNodes = node.splitNode(elmt, slotIndex.val);
        }
        Node newRoot = adjustTree(newNodes, slotIndex.val);
        //if we got a new root node then we have to set the slot's child to point to this new root
        if(newRoot != null){
          Node slot = chdNodes.getNode(fileHdr.getFile(), fileName, newRoot.getParent(), fileHdr);//sd
          slot.modifyElement(0, newRoot.getNodeIndex());
        }
      }catch(Exception e){
        e.printStackTrace();
        throw new RTreeException(e.getMessage());
      }finally{
        fileHdr.unlock();
      }
    }
  }
  /**
     This method is a copy of <code>RTree.chooseLeaf</code> with minor modifications.
     In fact there are number of changes , most important is that this method will just not get the new 
     Node, but also change the parent's (slot node) child pointer.
     Remeber that if there are no leaf node associated with a slot selected, this method creates one 
     returns this new Node after doing the process described above.
     but if there is a leaf node present then that node is returned just as in simple rtrees.
  */
  private Node chooseLeaf(Element elmt, LongWraper slotIndex)//sd
    throws RTreeException, IOException
  {
    /*TODO : we may also have to traverse non seed node, i.e grown nodes.*/
    try{
      //get the root node
      long root = fileHdr.getRootIndex();
      int level = 0;
      Node sltNode = chdNodes.getNode(fileHdr.getFile(), fileName, root, fileHdr);//sd
      //repeat till you reach a slot node
      while(sltNode.getElementType() != Node.LEAF_NODE){//(level != slotLvl){
        //get the best fitting rect from the node
        Element nextElmt = sltNode.getLeastEnlargement(elmt);
        if(level == slotLvl){
          slotIndex.val = sltNode.getNodeIndex();
          if(nextElmt.getPtr() == Node.NOT_DEFINED){//the first leaf node for this slot node
            Node rtNode = chdNodes.getNode(fileHdr.getFile(), fileName, sltNode.getNodeIndex(),//sd
                                           Node.LEAF_NODE, fileHdr);

            sltNode.modifyElement(0, rtNode.getNodeIndex());
            nextElmt.setPtr(rtNode.getNodeIndex());
            return rtNode;
          }
        }
        //if are here then we are not at a slot that has no childs

        sltNode = chdNodes.getNode(fileHdr.getFile(), fileName, nextElmt.getPtr(), fileHdr);//sd
        level++;
      }//while
      //if we are here then we reached a proper leaf node rather than a slot node
      return sltNode;
    }catch(Exception e){
      e.printStackTrace();
      throw new RTreeException(e.getMessage());
    }
  }

  /**
     This method will adjust the slot's only elements's child pointer.
  */
  private void adjustSlot(Node node, long childIndex)//sd
    throws RTreeException
  {
    try{
      node.modifyElement(0, childIndex);
    }catch(Exception e){
      e.printStackTrace();
      throw new RTreeException(e.getMessage());
    }  
  }

  /**
   * The clean up pahse is the last method that should be called after all the data have been grown.
   * This method basically adjusts all the slot nodes after all the insertions are made
   */
  public void cleanUp()
    throws RTreeException
  {
    try{
      fileHdr.lockWrite();
      if(slotLvl == Node.NOT_DEFINED)
        return;
      long root = fileHdr.getRootIndex();
      Node node = chdNodes.getNode(fileHdr.getFile(), fileName, root, fileHdr);//sd
      cleanUpRec(node, 0);
    }catch(Exception e){
      e.printStackTrace();
      throw new RTreeException(e.getMessage());
    }finally{
      fileHdr.unlock();
    }
  }

  /**
   * This method adjusts all the seed node MBRs to the grown subtrees. It also delets the slot node and
   * makes the root node of the underneath substree as the slot node.
   */
  private Rect cleanUpRec(Node node, int level)//sd
    throws NodeWriteException, FileNotFoundException, IllegalValueException, IOException, NodeReadException,
    RTreeException
  {
    Element[] elmts = node.getAllElements();
    if(level == slotLvl){//if level is the slot 
      if(elmts[0].getPtr() == Node.NOT_DEFINED){//this slot was never supplied a child node
        node.deleteNode();
        return new Rect();//a null rect 
      }else{//a slot that does have child node
        //remove this slot node and make the parent element point to the child of this slot node
        
        Node parentNode = chdNodes.getNode(fileHdr.getFile(), fileName, node.getParent(), fileHdr);//sd
        int index = parentNode.getElementIndex(node.getNodeIndex());
        parentNode.modifyElement(index, elmts[0].getPtr());
        
        Node subRoot = chdNodes.getNode(fileHdr.getFile(), fileName, elmts[0].getPtr(), fileHdr);//sd
        
        subRoot.setParent(node.getParent());
        node.deleteNode();
        
        return(subRoot.getNodeMBR());
      }//else
    }else{//it is not slot node but a seed node
      //remebeer we may have a situation where we do not get any Rect from down below this node...we delete 
      //this node as well.
      Rect rect = new Rect();
      for(int i=node.getTotalElements()-1; i>-1; i--){//for each element in this seed node
        Node chNode = chdNodes.getNode(fileHdr.getFile(), fileName, elmts[i].getPtr(), fileHdr);//sd
        Rect chRect = cleanUpRec(chNode, level + 1);
        rect.expandToInclude(chRect);//get child node's rect
        if(chRect.isNull()){//situation where child node does not have grown subtrees underneath
          node.deleteElement(i, false);
        }else{//we do have a child Rect
          node.modifyElement(i, chRect);
        }//else
      }//for
      if(rect.isNull()){//situation where there are no grown subtrees underneath this node
        node.deleteNode();
      }
      return rect;      
    }//else
  }
  class LongWraper
  {
    long val = Node.NOT_DEFINED;
  }
}
//(79 residential_1 conby 121 main_area ** select * from main_area where strName like 'andheri'**)
