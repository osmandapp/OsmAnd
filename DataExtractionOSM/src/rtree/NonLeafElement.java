//NonLeafElement.java
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

/**
   Anybody outside the package need not be concerned with this class.
   Element that will be in a non leaf node
   @author Prachuryya Barua
*/
public class NonLeafElement extends Element
{
  /**will contain file pointer pointing to the child node*/
  long nodePtr;
  public NonLeafElement( Rect nodeRect,long nodePtr)
  {
    super(nodeRect);
    this.nodePtr = nodePtr;
  }
  /**if possible make this function abstract and static in the parent class
     depends upon the size of the pointer
  */
  public static int sizeInBytes()
  {
    return( Rect.sizeInBytes() + Node.LONG_SIZE);
  }
  /**
     This can be the chile node pointer. 
  */
  //  public Object getPtr()//this is an integer object
  //  {
  //    return(new Integer(nodePtr));
  //  }
  public long getPtr()//this is an integer object
  {
    return nodePtr;
  }
  
  public int getElementType()
  {
    return Node.NONLEAF_NODE;
  }
  //  void setPtr(Object ptr)
  //    throws IllegalValueException
  //  {
  //    if(!(ptr instanceof Integer))
  //      throw new IllegalValueException("rtree.NonLeafElement.setPtr: pointer shoild be Integer");
  //    nodePtr = ((Integer)ptr).intValue();
  //  }
  public void setPtr(long ptr)
  {
    nodePtr = ptr;
  }
  public String toString()
  {
    return (super.toString()+"\n\tnodePointer: "+nodePtr);
  }
  /**
     A merge-sort routine for the Packing algo.
     @param rect the array to sort
     @param on if 0 then sort on X else on Y(i.e for 1)
  */
  public static void sort( Element[] elmts,int on)
  {
    twoWayMerge(elmts,0,elmts.length-1,on);
  }

  static void twoWayMerge(Element[] elmts,int start,int finish,int on)
  {
    try{
      int size = finish - start+1;
      if(size <= 2){//last two elements - simple swap
        if(size < 2)
          return;
        int midValI = getMid(elmts[start],on);
        int midValJ = getMid(elmts[finish],on);
        Element temp;
        if(midValI > midValJ){
          temp = elmts[start];
          elmts[start] = elmts[finish];
          elmts[finish] = temp;
        }
        return;
      }
      Double middle = new Double(start+finish);
      middle = new Double(Math.ceil(middle.doubleValue()/2));
      twoWayMerge(elmts,start,middle.intValue(),on);
      twoWayMerge(elmts,middle.intValue(),finish,on);
      simpleMerge(elmts,start,middle.intValue(),finish,on);
    }
    catch(Exception e){
      System.out.println("rtree.Element.twoWayMerge: probably index out of bound");
      //e.printStackTrace();
    }
  }
  //simple merge
  private static void simpleMerge( Element[] elmts,int first,int second,int third,int on)
    throws Exception
  {
    int i = first;
    int j = second;
    int l = 0;
    int midValI;
    int midValJ;
    Element[] temp = new Element[third-first+1];
    while((i < second) && (j <= third)){//loop till one lasts
      //get the mid values in the given dimension
      midValI = getMid(elmts[i],on);
      midValJ = getMid(elmts[j],on);
      if(midValI <= midValJ)
        temp[l++] = elmts[i++];
      else
        temp[l++] = elmts[j++];
    }
    //copy the rest
    if(i >= second)//give second section
      while(j <= third)
        temp[l++] = elmts[j++];
    else
      while(i < second)//give first section
        temp[l++] = elmts[i++];
    System.arraycopy(temp,0,elmts,first,temp.length);
  }
  /**
     A private class to calculate the mid value in given dimension
  */
  private static int getMid( Element elmt,int on)
  {
    if(on == 0)
      return ((elmt.getRect().getMaxX() 
               + elmt.getRect().getMinX())/2);
    else
      return ((elmt.getRect().getMaxY() 
               + elmt.getRect().getMinY())/2);
  }
  public Object clone()
  {
    try{
      return new NonLeafElement(new Rect(Rectangle.getMinX(), Rectangle.getMinY(), 
                                         Rectangle.getMaxX(), Rectangle.getMaxY()), nodePtr);
    }
    catch(Exception e){
      e.printStackTrace();
      return null;
    }
  }
}
