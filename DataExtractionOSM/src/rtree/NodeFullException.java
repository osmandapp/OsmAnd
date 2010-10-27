//NodeFullException.java
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

import java.lang.Exception;

/**Exception when no. of elements increase, then the limited value, in a node.

@author Prachuryya Barua
*/
public class NodeFullException extends Exception
{
  public NodeFullException(String msg)
  {
    super(msg);
  }
}
