//ABL.java
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
/**Active Branch List
   This class will consist of the Elements and their MINDIST from the point of query.
   When the array of this object is returned by the 'nearestSearch' method,
   kindly type cast 'Elemen't to 'LeafElement' when necessary.
   This library should be considered as an open source library. Formal GNU licensing I will include later.
*/
public class ABL implements Cloneable
{
  /**
     Please type cast it to LeafElement when used as a returned value of 
     the 'nearestSearch' method.
  */
  public Element element;
  /**By Definition - The distance of a point P in Euclidean space (E(n))
     from a rectangle R in the same space, denoted by MINDIST(P,R).<br>
     In English - This is the minimum distance between the query point P
     and the MBR of the object.
     <b>Note:-</b> The distance(minDist) is the square of the actual distance.
     To get the actual distance, call <b>Math.sqrt(minDist)</b> (cast minDist to
     Double).
  */
  public long minDist;//MINDIST(P,this)
  public ABL(Element element,long minDist)
  {
    this.element = element;
    this.minDist = minDist;
  }
  //Uses Two-Way-Merge-Sort (Recursive)
  //Sorts an ABL array based on minDist. Make sure there are no null values.
  public void mergeSort(ABL[] arrABL)
  {
    twoWayMerge(arrABL,0,arrABL.length-1);
  }
  private void twoWayMerge(ABL[] arrABL,int start,int finish)
  {
    try{
      int size = finish - start+1;
      if(size <= 2){
        if(size < 2)
          return;
        ABL temp;
        if(arrABL[start].minDist > arrABL[finish].minDist){
          temp = arrABL[start];
          arrABL[start] = arrABL[finish];
          arrABL[finish] = temp;
        }
        return;
      }
      Double middle = new Double(start+finish);
      middle = new Double(Math.ceil(middle.doubleValue()/2));
      twoWayMerge(arrABL,start,middle.intValue());
      twoWayMerge(arrABL,middle.intValue(),finish);
      simpleMerge(arrABL,start,middle.intValue(),finish);
    }
    catch(Exception e){
      System.out.println("rtree.ABL.twoWayMerge: most probably a null value in array");
    }
  }
    
  //simple merge
  private void simpleMerge(ABL[] arrABL,int first,int second,int third)
    throws Exception
  {
    int i = first;
    int j = second;
    int l = 0;
    ABL[] temp = new ABL[third-first+1];
    while((i < second) && (j <= third)){//loop till one lasts
      if(arrABL[i].minDist <= arrABL[j].minDist)
        temp[l++] = arrABL[i++];
      else
        temp[l++] = arrABL[j++];
    }
    //copy the rest
    if(i >= second)//give second section
      while(j <= third)
        temp[l++] = arrABL[j++];
    else
      while(i < second)//give first section
        temp[l++] = arrABL[i++];
    System.arraycopy(temp,0,arrABL,first,temp.length);
  }
  public Object clone()
  {
    return new ABL(element,minDist);
  }
    
}//class ABL


