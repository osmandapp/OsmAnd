//LeafElement.java
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
//package rtree;
import java.util.*;

/**
   This class represents database objects. 
   It consist of a MBR and the PID of the object.   
   @author Prachuryya Barua
*/
public class LeafElement extends Element
{
  /**
     for the time being it is 'int'.
     Changed to long
  */
  long recordPtr;
  
  public LeafElement( Rect minBndRect,long recordPtr)
  {
    super(minBndRect);
    this.recordPtr = recordPtr;
  }
   
  /**
     if possible make this function abstract and static in the parent class
     depends upon the size of the pointer
  */
  public static int sizeInBytes()
  {
    return( Rect.sizeInBytes() + Node.LONG_SIZE);
  }
    
  /**
     The <tt>"Id"</tt> of the MBR in database.
     @return an Long type  object
  */
  //  public Object getPtr()
  //  {
  //    return(new Long(recordPtr));
  //  }
  @Override
public long getPtr()
  {
    return recordPtr;
  }
  @Override
public int getElementType()
  {
    return  Node.LEAF_NODE;
  }
  //  void setPtr(Object ptr)
  //    throws IllegalValueException
  //  {
  //    if(!(ptr instanceof Long))
  //      throw new IllegalValueException("rtree.LeafElement.setPtr: pointer shoild be Long");
  //    recordPtr = ((Long)ptr).longValue();
  //  }
  @Override
public void setPtr(long ptr)
  {
    recordPtr = ptr;
  }
  @Override
public String toString()
  {
    return (super.toString()+"\n\trecPointer: "+recordPtr);
  }
  @Override
public Object clone()
  {
    try{
      return new LeafElement(new Rect(Rectangle.getMinX(), Rectangle.getMinY(),
                                      Rectangle.getMaxX(), Rectangle.getMaxY()),recordPtr);
    }
    catch(Exception e){
      e.printStackTrace();
      return null;
    }
  }
  
  /**
     This is a utility method that extracts record pointers(IDs) from a <code>LeafElement</code> vector.
     This method can be called after calling amy of the spatial calls to RTree.
  */
  public synchronized static List extractPtrs(List elements)
    throws IllegalArgumentException
  {
    if(elements == null)
      throw new IllegalArgumentException("RTree.LeafElement: Argument null");
    List result = new ArrayList();
    try{
      for (Iterator i = elements.iterator(); i.hasNext();)
        //result.addElement(new Long(((LeafElement)i.next()).getPtr()));
        result.add(new Integer((int)((LeafElement)i.next()).getPtr()));//temp
      //System.out.println("LeafElement.Extractptrs : time " + (System.currentTimeMillis() - time));
      return result;
      
    }catch(ClassCastException e){
      throw new IllegalArgumentException("RTree.LeafElement: Type of vector is not LeafElement");
    }
  } 
  
}
