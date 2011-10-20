//Element.java
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

/**baap of all elemetns
  @author Prachuryya Barua
 * 
 **/
public abstract class Element implements Cloneable, java.io.Serializable
{
  Rect Rectangle;
  
  Element(){};
  public Element( Rect Rectangle)
  {
    this.Rectangle = Rectangle;
  }
  private void overwriteRect( Rect Rectangle)
  {
    this.Rectangle = Rectangle;
  }
  public  Rect getRect()
  {
    return(Rectangle);
  }
  /**
     This can be a child node pointer or a record pointer.
  */
  //public abstract Object getPtr();//old
  public abstract long getPtr();
  /**
     Do not call this function, Node will call it.
  */
  void setRect( Rect mbr)
    throws IllegalValueException
  {
    if(mbr == null)
      throw new IllegalValueException("Element.adjustMBR: Rect is null");
    overwriteRect(mbr);
  }
  //abstract void setPtr(Object ptr) //old
  //throws IllegalValueException;
  public abstract void setPtr(long ptr);
  public abstract int getElementType(); 
  @Override
public String toString()
  {
    return Rectangle.toString();
  }
}
