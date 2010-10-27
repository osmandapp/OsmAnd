//SdNode.java
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

import rtree.*;
import java.io.*;
import rtree.Node;
import rtree.IllegalValueException;
import rtree.NodeWriteException;
import java.io.IOException;

/**
   The seed node.
   @author Prachuryya Barua
*/
public class SdNode extends Node
{
  /**
     For a new node.
  */
  public SdNode(RandomAccessFile file,String fileName, long prnt,int elmtType, FileHdr flHdr)
    throws IOException, NodeWriteException
  {
    super(file, fileName, prnt, elmtType, flHdr);
  }

  public SdNode(RandomAccessFile file, String fileName, long ndIndex, FileHdr flHdr)
    throws FileNotFoundException,IOException, NodeReadException, NodeWriteException
  {
    super(file, fileName, ndIndex, flHdr);
  }

  SdNode(Node node)
  {
    
  }
  public int getElementIndex(long param1)
  {
    return super.getElementIndex(param1);
  }
  public void insertElement(Element[] elmts )
    throws NodeWriteException, NodeFullException
  {
    super.insertElement(elmts, false);
  }
  public void insertElement(Element elmt)
    throws NodeWriteException, NodeFullException
  {
    super.insertElement(elmt);
  }
  public Element getLeastEnlargement(Element elmt)
    throws NodeEmptyException, IllegalValueException, NodeWriteException
  {
    return super.getLeastEnlargement(elmt);
  }
  
  /**
   * Overriden so that this package can use it.
   * @param param1 <description>
   * @return <description>
   * @exception RTreeException <description>
   */
  public Node[] splitNode(Element param1, long slotIndex) throws RTreeException, NodeWriteException
  {
    Node[] nodes = super.splitNode(param1, slotIndex);
    return nodes;
  }
  
  /**
   * Overriden so that this package can use it.
   */
  public void modifyElement(int index,long pointer) 
    throws IllegalValueException, IOException, NodeWriteException
  {
    super.modifyElement(index, pointer);
  }
  
  public void deleteNode() throws NodeWriteException
  {
    super.deleteNode();
  }
  
  public void modifyElement(int param1, Rect param2)
    throws IllegalValueException, IOException, NodeWriteException
  {
    super.modifyElement(param1, param2);
  }

  public void deleteElement(int param1) throws IllegalValueException, NodeWriteException
  {
    super.deleteElement(param1, false);
  }

  public void setParent(long param1) throws IOException, NodeWriteException
  {
    super.setParent(param1);
  }

}

