//Point.java
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
/**
   A simple class used only in nearestNeighbour search.
   @author Prachuryya Barua
*/
public class Point implements Cloneable
{
  private int X=0,Y=0;
  public Point(int x,int y)
  {
    X = x;
    Y = y;
  }
  public int getX()
  {
    return X;
  }
  public int getY()
  {
    return Y;
  }
  @Override
public Object clone()
  {
    return new Point(X,Y);
  }
  @Override
public String toString()
  {
    String ret;
    ret = "\nThe Point:-";
    ret += "\n\tX: " + X;
    ret += "\n\tY: " + Y;
    return ret;
  }
}
