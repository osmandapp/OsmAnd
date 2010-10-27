//ReadNode.java
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
package rtree;

import java.io.*;

/***********************************************************************************************************
 * *                                                                                                       *
 *  This is a special purpose class that is alowed read only operartion on tree. Any write operation       *
 *  would give error.                                                                                      *
 *  This class was necessary to solve the following problem. Since none of the nodes are cloned when given *
 *  to the clients, they can become problematic when multiple reads on the same node is going on.          *
 *  Because of the new buffer policy we do not clone node, it is like a single node in the whole system.   *
 *  But the obvious problem is multiple read threads when reading nodes.                                   *
 *  No though problem when writing as the whole tree is locked. What we can do is that clients themselves  *
 *  tell the cache that they need a cloned node and that it willnot be used for write purpose.             *
 *  This class does exactly that.                                                                          *
 * @author Prachuryya Barua
 ***********************************************************************************************************/

class ReadNode extends Node
{
  ReadNode()
  {
  }
  public static ReadNode makeReadNode(Node node)
  {
    ReadNode rdNode = new ReadNode();
    try{
      //Integer intVal;
      rdNode.file = node.file;
      rdNode.dirty = node.dirty;
      rdNode.fileName = new String(node.fileName.toCharArray());
      rdNode.nodeIndex = node.nodeIndex;
      //rdNode.isNodeEmpty = node.isNodeEmpty;
      rdNode.sorted = node.sorted;
      rdNode.nodeMBR = new Rect(node.nodeMBR);//remove
      rdNode.elements = new Element[node.elements.length];
      if(node.elementType == LEAF_NODE){
        for(int i=0; i<node.totalElements; i++)
          rdNode.elements[i] = new LeafElement(new Rect(node.elements[i].getRect()), 
                                               node.elements[i].getPtr());
      }else{
        for(int i=0; i<node.totalElements; i++)
          rdNode.elements[i] = new NonLeafElement(new Rect(node.elements[i].getRect()), 
                                                  node.elements[i].getPtr());
      }
      //  for(int i=0; i<node.totalElements; i++){
      //          if(node.elementType == LEAF_NODE)
      //            rdNode.elements[i] = new LeafElement(new Rect(node.elements[i].getRect()), 
      //                                               node.elements[i].getPtr());
      //          else
      //            rdNode.elements[i] = new NonLeafElement(new Rect(node.elements[i].getRect()), 
      //                                                  node.elements[i].getPtr());
      //        }//if
      //  }//for
      rdNode.fileHdr = node.fileHdr;
      rdNode.totalElements = node.totalElements;
      rdNode.parent = node.parent;
      rdNode.elementSize = node.elementSize;
      rdNode.elementType = node.elementType;
      return rdNode;
    }
    catch(Exception e){
      e.printStackTrace();
      return null;
    }

  }
  /**
     This method delets the element with the given index from the node.
     It rewrites the node.
     This method now also being used to write the whole node to the file.
     @param index The element to delete. Give -1 if the whole node is to be flushed.
     @param force Whether to force IO. As this method is also used to write the whole node, this was 
     required.
     @return thengaa!
     XXX : This is till not correct.
  */
  public void deleteElement(int index, boolean force)
    throws IllegalValueException, NodeWriteException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  public void insertElement(Element elmt) 
    throws NodeWriteException, NodeFullException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  public void insertElement(Element[] elmts, boolean updateChldrn)
    throws NodeWriteException, NodeFullException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  public int getElementType()
  {
    return super.getElementType();
  }
  public long getNodeIndex()//for new nodes
  {
    return super.getNodeIndex();
  }
  Rect[] getAllRectangles()
    throws  IllegalValueException
  {
    return super.getAllRectangles();
  }
  public Element getLeastEnlargement(Element elmt)
    throws NodeEmptyException, IllegalValueException, NodeWriteException
  {
    return super.getLeastEnlargement(elmt);
  }
  boolean isInsertPossible()
  {
    return super.isInsertPossible();
  }

  public Node[] splitNode(Element elmtM1, long slotIndex)
    throws RTreeException, NodeWriteException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  public long getParent()
  {
    return super.getParent();
  }
  public int getElementIndex(long ptr/*Object ptr*/)
  {
    return super.getElementIndex(ptr);
  }
  /**
     Used to overwrite the old Element with the new one.
     It modifies the element in the disk as well as in the local variables.
  */
  public void modifyElement(int index,Element elmt)
    throws  IllegalValueException,IOException, NodeWriteException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  /**
     Overloaded
  */
  public void modifyElement(int index,long pointer)
    throws  IllegalValueException,IOException, NodeWriteException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  /**
     Overloaded
  */
  public void modifyElement(int index,Rect rect)
    throws  IllegalValueException,IOException, NodeWriteException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  /**
     This function runs a loop on the elements to calculate the total MBR.
     Therefore in case if you already have loop that runs through each of the entries,
     then it is better to calculate MBR in that loop without calling this method.
     @throws IllegalValueException When there are no elements in the node.
  */
  public Rect getNodeMBR()
    throws  IllegalValueException
  {
    return super.getNodeMBR();
  }
  /**
     No error echecking at all.
  */
  public void setParent(long /*int*/ prnt)
    throws IOException, NodeWriteException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  public int getTotalElements()
  {
    return super.getTotalElements();
  }
  /**
     Although it returns all the elements but the total elements will not be equal to the length of
     the returned array.Therefore <br><b>Never Use <code>.length</code> field With the Returned Array
     </b>. Instead use <code>getTotalElements()</code>.
     @return An element Array.
  */
  public Element[] getAllElements()
  {
    return super.getAllElements();
  }
  Element getElement(int index)
    throws  IllegalValueException
  {
    return super.getElement(index);
  }
  /**
     Adds the node to the free stack.
     Be very careful with this method because once called, this node may be
     given to any new node even when you have not destroyed its object.
     If the node is the only node then it updates the file header as well.
     </br><i><b>Once called, there is no turning back!</b></i>.
  */
  public void deleteNode()
    throws NodeWriteException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  /**
   * This method is added to sort the elements in this node to help sweepline algorithm.
   */
  void sweepSort()//check out for null elements
  {
    super.sweepSort();
  }//sweepSort
  /**
     This is a new methos that will help the phylosophy where one should write to tbe cache only when
     required.
     @return true if needed write and written or false (not dirty).
  */
  public boolean flush()
    throws NodeWriteException
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  void setDirty(boolean val)
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  public boolean isDirty()
  {
    throw new UnsupportedOperationException("operation not supported");
  }
  
}






