//Rect.java
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
   It is very easy to extend this program to n dimensions. Here every thing is 
   hard coaded for 2 dim. To convert to more than 2 dim. :-
   <br>make the point class to hold the required dimensions.(keep an array of 
   dimensions)
   <br>The rect class needs the four points directly, instead of that make it to
   accept Point as its argument in the constructor.
   <br>Then on deal with the point class directly instead of the four points 
   directly.
   <p>The methods of relational topology(equals,contains etc.) are from various
   papers by <b>D. Papadias</b> and mainly from Modeling Topological Spatial 
   Relation: Strategies for Query Processing.- <b>Egenhofer</b>
   <br>The methods are nothing but set theories.
   <p>In GIS there can be many types of combinations of the topological relations.
   The theories of these combinations are given in the above said papers. If 
   required this class can be improved for those conditions.
   @author Prachuryya Barua
*/
//TODO: Apply the isNull considerations.
//      Take the common procedures in overloaded methods to one method.
public class Rect implements java.io.Serializable
{
  private int minX=0,minY=0,maxX=0,maxY=0;
  boolean isNull = false;
  public Rect()
  {
    initNull();
  }
  
  public Rect(int topX,int topY,int botX, int botY)
    throws IllegalValueException
  {
    if((topX > botX) || (topY > botY)){
      System.out.println("\ttopX:"+topX+"\ttopY:"+topY+"\tbotX:"+botX+"\tbotY:"+botY);
      throw new IllegalValueException("rtree.Rect.Rect: wrong order of params.");
    }
    init(topX,topY,botX,botY);
  }
  Rect(Rect rect)
    throws IllegalValueException
  {
    if(rect == null)
      throw new IllegalValueException("rtree.Rect.Rect: Param is null.");
    if(rect.isNull()){
      initNull();
    }
    else
      init(rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY());
  }
  private void initNull()
  {
    minX = 0;
    minY = 0;
    maxX = -1;
    maxY = -1;
    isNull = true;
  }
  private void init(int minX, int minY, int maxX, int maxY)
  {
    this.minX = minX;
    this.minY = minY;
    this.maxX = maxX;
    this.maxY = maxY;
    isNull = false;
  }
  /*
    If you want to make the constructor take Point, then....
    Point minP,maxP;
    Rect(int minX,int minY,int botX, int botY)
    throws IllegalValueException
    {
    if((minX > maxX) || (minY > maxY))
    throw new IllegalValueException("rtree.Rect.Rect: "
    +"minP > maxP ?");
    minP = new Point(minX,minY);
    maxP = new Point(maxX,maxY);
    }
    Rect(Point minP,Point maxP)
    throws IllgalValueException
    {
    if((minP == null) || (maxP == null))
    throw new IllegalValueException("rtree.Rect.Rect: "
    +"Either of the point are null");
    if((minP.getX() > maxP.getX()) || (minP.getY() > maxP.getY()))
    throw new IllegalValueException("rtree.Rect.Rect: "
    +"minP > maxP?");
    this.minP = (Point)minP.clone();
    this.maxP = (Point)maxP.clone();
    }
  */
  public boolean isNull()
  {
    return isNull;
    //if(minX == 0 && minY == 0 && maxX == -1 && maxY == -1)
    //return true;
    //else
    //return false;
  }
  public static int sizeInBytes()
  {
    return(Node.INTEGER_SIZE*4);//depends upon the points
  }
  public int getArea()
  {
    if(isNull())
      return 0;
    return((maxX-minX)*(maxY-minY));
  }
  public int getWidth()
  {
    if(isNull())
      return 0;
    return(Math.abs(maxX - minX));
  }
  public int getHeight()
  {
    if(isNull())
      return 0;
    return(Math.abs(maxY - minY));
  }
  public int getMinX()
  {
    return(minX);
  }
  public int getMinY()
  {
    return(minY);
  }
  public int getMaxX()
  {
    return(maxX);
  }
  public int getMaxY()
  {
    return(maxY);
  }
  /**
     Include the given Rectangle.
  */
  public void expandToInclude(Rect rect)
  {
    if(rect == null || rect.isNull())
      return;
    
    if(this.isNull()){
      init(rect.getMinX(),rect.getMinY(), rect.getMaxX(), rect.getMaxY());
      return;
    }
    minX = Math.min(rect.getMinX(),minX);//minX
    minY = Math.min(rect.getMinY(),minY);//minY
    maxX = Math.max(rect.getMaxX(),maxX);//maxX
    maxY = Math.max(rect.getMaxY(),maxY);//maxY
  }
  /**
     return the minimum bounding rectangle of this rectangle and the passed
     rectangle.
  */
  public Rect getResultingMBR(Rect rectangle)
    throws IllegalValueException
  {
    if(rectangle == null)
      throw new IllegalValueException("rtree.Rect.getResultingMBR : Rect is null");
    if(rectangle.isNull())
      if(this.isNull())
        return new Rect();
      else
        return new Rect(this);
    else//rectangle is not null
      if(this.isNull())
        return rectangle;
    //if nobody is "isNull"
    int topX,topY,botX,botY;
    topX = Math.min(rectangle.getMinX(),minX);//minX
    topY = Math.min(rectangle.getMinY(),minY);//minY
    botX = Math.max(rectangle.getMaxX(),maxX);//maxX
    botY = Math.max(rectangle.getMaxY(),maxY);//maxY
    return(new Rect(topX,topY,botX,botY));
  }
  /**
     Overloaded type of the previous function - but static
  */
  public static Rect getResultingMBR(Rect source, Rect dest)
    throws IllegalValueException
  {
    if((dest == null) || (source == null))
      throw new IllegalValueException("rtree.Rect.getResultingMBR : Rect is null");
    return source.getResultingMBR(dest);
  }
  /**
     Another overloaded version - but static
  */
  public static Rect getResultingMBR(Rect[] rects)
    throws IllegalValueException
  {
    if(rects.length <= 0)
      throw new IllegalValueException("rtree.Rect.getResultingMBR : "+
                                      "Array of rectangles are empty.");
    Rect result = rects[0];
    for(int i=1; i<rects.length; ++i)
      result = getResultingMBR(rects[i],result);
    return result;
  }
  /**
     Another overloaded version - but static
  */
  public static Rect getResultingMBR(Rect[] rects, Rect rect)
    throws IllegalValueException
  {
    Rect result = getResultingMBR(rects);
    result = getResultingMBR(rect,result);
    return result;
  }
  //--------------------------Topological methods---------------------------
  /**
     Checks if this rectangle contains 'rect'.
     <br>Checks the two minimal conditions from 'contains' matrix(Egenhofer.)
     <br>Correct for Point as well.
  */
  public boolean contains(Rect rect)
    throws IllegalValueException
  { 
    if(rect == null)
      throw new IllegalValueException("rtree.Rect.contains: null argument");
    boolean ret = true;
    //m12 = true && m22 = false
    //X dim.
    if((minX >= rect.getMinX()) || (maxX <= rect.getMaxX()))
      return false;
    //Y dim.
    if((minY >= rect.getMinY()) || (maxY <= rect.getMaxY()))
      return false;
    return ret;
  }
  /**
     The difference betn. this method and <code>contains</code> is that this method returns true
     even if the <code>covers</code> condition if true. 
     @return true if this rectangle completely encloses 'rect'.
  */
    
  public boolean encloses(Rect rect)
    throws IllegalValueException
  {
    if(rect == null)
      throw new IllegalValueException("rtree.Rect.overlaps: null argument");
    boolean ret = true;
    //X dim.
    if((minX > rect.getMinX()) || (maxX < rect.getMaxX()))
      return false;
    //Y dim.
    if((minY > rect.getMinY()) || (maxY < rect.getMaxY()))
      return false;
    return ret;
  }

  /**
     Check if this rectangle is contained by 'rect'
  */
  public boolean containedBy(Rect rect)
    throws IllegalValueException
  { 
    if(rect == null)
      throw new IllegalValueException("rtree.Rect.containedBy:null argument");
    boolean ret = true;
    //m21 = true && m22 = false
    //X dim.
    if((rect.getMinX() >= minX) || (rect.getMaxX() <= maxX))
      return false;
    //Y dim.
    if((rect.getMinY() >= minY) || (rect.getMaxY() <= maxY))
      return false;
    return ret;
  }
  /**
     This method also gives whether one point is over another, if you do not
     want that much precision then comment the line.This will improve performance.
     @return true if both the rectangle overlap/intresect else false.
     <br><b>Note:-</b>This method is not precisely according to Egenhofer.
  */
  public boolean overlaps(Rect rect)
    throws IllegalValueException
  {
    if(rect == null)
      throw new IllegalValueException("Rect.overlaps: null argument");
    int rectMinX = rect.getMinX();
    int rectMinY = rect.getMinY();
    int rectMaxX = rect.getMaxX();
    int rectMaxY = rect.getMaxY();
    boolean ret = false;
    //if one point object is over another then.....
    if((minX == rectMinX) && (minY == rectMinY) && (maxX == rectMaxX) && (maxY == rectMaxY))
      return true;
    //if you do not want this much precision then comment above written line.
        
    //X dim.
    if((minX < rectMaxX) && (maxX > rectMinX))
      ret = true;
    else
      return false;
    //Y dim.
    if((minY < rectMaxY) && (maxY > rectMinY))
      ret = true;
    else
      return false;

    return ret;
  }//overlaps

  /**
     Saperate or not(Egenhofer).
     <br>To check if two rectangles intersect in any way then call '!disjoint()'.
  */
  public boolean disjoint(Rect rect)
    throws IllegalValueException
  { 
    if(rect == null)
      throw new IllegalValueException("rtree.Rect.disjoint: null argument");
    boolean ret = true;
    //m12 = false && m22 = false
    //X dim.
    if((minX <= rect.getMaxX()) && (maxX >= rect.getMinX()))
      ret = false;
    else
      return ret;
    //Y dim.
    if((minY <= rect.getMaxY()) && (maxY >= rect.getMinY()))
      ret = false;
    else 
      ret = true;
    return ret;

  }//disjoint - over
  /**
     Checks if both the rectangles meet or not.
  */
  public boolean meet(Rect rect)
    throws IllegalValueException
  { 
    if(rect == null)
      throw new IllegalValueException("rtree.Rect.meet: null argument");
    boolean ret = true;
    //m11 = false
    if(disjoint(rect))
      return false;
    //if both have common area then exit with false.
    if((minX < rect.getMaxX()) && (maxX > rect.getMinX()))
      if((minY < rect.getMaxY()) && (maxY > rect.getMinY()))
        return false;
    System.out.println("Raj!");
    //m22 = true
    if((minX == rect.getMaxX()) || (maxX == rect.getMinX()))
      return true;
    else
      ret = false;
    if((minY == rect.getMaxY()) || (maxY == rect.getMinY()))
      ret = true;
    return ret;

  }//meet
  /**
     Checks if this rectangle contains 'rect'. This method is incomplete. It can 
     be finished if required.(Depends on query requirements)
  
     <pre>
     ---------------
     |             |
     |      |------|
     |      |'rect'|
     |      |------|
     |             |
     ---------------
     </pre>
  */
  public boolean covers(Rect rect)
    throws IllegalValueException
  { 
    if(rect == null)
      throw new IllegalValueException("rtree.Rect.covers: null argument");
    boolean ret = true;
    //m12 = true
    if((minX > rect.getMaxX()) || (maxX < rect.getMinX()))
      return false;
    if((minY > rect.getMaxY()) || (maxY < rect.getMinY()))
      return false;
    //m21 = false
    if((minX < rect.getMinX()) && (maxX < rect.getMaxX()))
      return false;
    if((minY < rect.getMinY()) && (maxY < rect.getMaxY()))
      return false;
    //m22 = true
    if((minX != rect.getMinX()) && (maxX != rect.getMaxX()))
      return false;
    if((minY != rect.getMinY()) && (maxY != rect.getMaxY()))
      return false;
    return ret;
  }
  /**
     Checks if this rectangle is equal to 'rect'
  */
  public boolean equals(Rect rect)
    throws IllegalValueException
  { 
    if(rect == null)
      throw new IllegalValueException("rtree.Rect.equals: null argument");
    //m21=false && m23=false
    if((minX != rect.getMinX()) || (maxX != rect.getMaxX()))
      return false;
    if((minY != rect.getMinY()) || (maxY != rect.getMaxY()))
      return false;
    return true;
  }

  @Override
public String toString()
  {
    String ret;
    ret = "\nThe Rectangle:-";
    ret += "\n\tminX: " + minX;
    ret += "\n\tminY: " + minY;
    ret += "\n\tmaxX: " + maxX;
    ret += "\n\tmaxY: " + maxY;
    return ret;
  }
  /**
     Calculate the Euclidean distance between the point and the MBR.
     To calculate the distance(MINDIST) betn. a Point and Rectangle in 
     n dimension. In our case we consider only 2 dimensions.
     <br><b>Note:-</b> The distance is the square of the actual distance. To find the actual 
     distance, square root the returned value.
  */
  public static long minDist(Point p, Rect rect)
  {
    long minDist;
    int ri;//see Roussopoulos for notations
    int pX = p.getX();
    int pY = p.getY();
    int minX = rect.getMinX();
    int minY = rect.getMinY();
    int maxX = rect.getMaxX();
    int maxY = rect.getMaxY();
    //for X dim.
    if(pX < minX)
      ri = minX;
    else if(pX > maxX)
      ri = maxX;
    else
      ri = pX;
    Long temp = new Long(Math.abs(pX - ri));
    minDist = (new Double(Math.pow(temp.doubleValue(),2))).longValue();
    //for Y dim.
    if(pY < minY)
      ri = minY;
    else if(pY > maxY)
      ri = maxY;
    else
      ri = pY;
    temp = new Long(Math.abs(pY - ri));
    minDist += (new Double(Math.pow(temp.doubleValue(),2))).longValue();
    return minDist;
  }
  /**
     To find the minimum of the maximum distances from a point to a Rectangle
     <b>(Not implemeneted yet).</b>
     For further details see Roussopoulos.
     If we apply Cheung then it will not be used.
  */
  public static int minMaxDist(Point p, Rect rect)
  {
    return(0);
  }
  /**
   * Will return the intersection of the <code>this</code> and <code>rect</code>.
   * @param rect The other <code>Rect</code>
   */
  public Rect intersection(Rect rect)
  {
    if(rect == null)
      throw new IllegalArgumentException("Rect.instersection : Argument Rect is null");
    int x1;// = 0
    int y1;// = 0;
    int x2;// = -1;
    int y2;// = -1;
    //minX
    if(minX < rect.minX)
      x1 = rect.minX;
    else
      x1 = minX;
    //minY
    if(minY < rect.minY)
      y1 = rect.minY;
    else
      y1 = minY;
    //maxX
    if(maxX > rect.maxX)
      x2 = rect.maxX;
    else
      x2 = maxX;
    //maxY
    if(maxY > rect.maxY)
      y2 = rect.maxY;
    else
      y2 = maxY;
    try{
      if(x1 > x2 || y1 > y2)
        return new Rect();
      else
        return new Rect(x1, y1, x2, y2);
    }catch(Exception e){
      e.printStackTrace();
      return new Rect();
    }
  }
}
//TODO
/*
  New disjunctions of topological relations can be derived from the
  minimal subset algorithm(Egenhofer)
  <br> To do so first make a method for each of the matrix element. Then
  using the algo. find the conditions of the matrix to satisfy and call
  the desired methods.
  Although this is possible through the combinations of the eight methods
  directly but it would need a lot of processing.
*/

/*
  The diif. bet. this function and 'overlap' is that the latter returns
  false if the two rects have only a side(s) in common but no area.
  This method returns true if both the rects have either or both
  area and side(s) common.
  <br><b>returns true if ((meet=true)||(overlap=true)||(covers=true)||
  (equal=true)) or it can be said not disjoint.
  @return true if this rect. touches or overlaps with 'rect'
*/
/*
  public boolean intersect(Rect rect)
  throws IllegalValueException
  {
  if(rect == null)
  throw new IllegalValueException("rtree.Rect.overlaps: null argument");
  boolean ret = true;
  //X dim.
  if((minX > rect.getMaxX()) || (maxX < rect.getMinX()))
  return false;
  //Y dim.
  if((minY > rect.getMaxY()) || (maxY < rect.getMinY()))
  return false;
  return ret;
  }

*/

