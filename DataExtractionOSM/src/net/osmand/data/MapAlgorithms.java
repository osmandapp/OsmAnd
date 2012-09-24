package net.osmand.data;

import gnu.trove.list.TLongList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

public class MapAlgorithms {
	
	public static void simplifyDouglasPeucker(List<Node> n, int zoom, int epsilon, List<Node> result){
		if(zoom > 31){
			zoom = 31;
		}
		ArrayList<Integer> l = new ArrayList<Integer>();
		int first = 0;
		int nsize = n.size(); 
		while(first < nsize){
			if(n.get(first) != null){
				break;
			}
			first++;
		}
		int last = nsize - 1;
		while (last >= 0) {
			if (n.get(last) != null) {
				break;
			}
			last--;
		}
		if (last - first < 1) {
			return;
		}
		// check for possible cycle
		boolean checkCycle = true;
		boolean cycle = false;
		while (checkCycle && last > first) {
			checkCycle = false;

			double x1 = MapUtils.getTileNumberX(zoom, n.get(first).getLongitude());
			double y1 = MapUtils.getTileNumberY(zoom, n.get(first).getLatitude());
			double x2 = MapUtils.getTileNumberX(zoom, n.get(last).getLongitude());
			double y2 = MapUtils.getTileNumberY(zoom, n.get(last).getLatitude());
			if (Math.abs(x1 - x2) + Math.abs(y1 - y2) < 0.001) {
				last--;
				cycle = true;
				checkCycle = true;
			}
		}
		if(last - first < 1){
			return;
		}
		simplifyDouglasPeucker(n, zoom, epsilon, l, first, last);
		result.add(n.get(first));
		int lsize = l.size();
		for (int i = 0; i < lsize; i++) {
			result.add(n.get(l.get(i)));
		}
		if (cycle) {
			result.add(n.get(first));
		}
	}

	private static void simplifyDouglasPeucker(List<Node> n, int zoom, int epsilon, List<Integer> ints, int start, int end){
		double dmax = -1;
		int index = -1;
		for (int i = start + 1; i <= end - 1; i++) {
			if(n.get(i) == null){
				continue;
			}
			double d = orthogonalDistance(zoom, n.get(start), n.get(end), n.get(i));// calculate distance from line
			if (d > dmax) {
				dmax = d;
				index = i;
			}
		}
		if(dmax >= epsilon){
			simplifyDouglasPeucker(n, zoom, epsilon, ints, start, index);
			simplifyDouglasPeucker(n, zoom, epsilon, ints, index, end);
		} else {
			ints.add(end);
		}
	}
	
	private static double orthogonalDistance(int zoom, Node nodeLineStart, Node nodeLineEnd, Node node) {
		double x1 = MapUtils.getTileNumberX(zoom, nodeLineStart.getLongitude());
		double y1 = MapUtils.getTileNumberY(zoom, nodeLineStart.getLatitude());
		double x2 = MapUtils.getTileNumberX(zoom, nodeLineEnd.getLongitude());
		double y2 = MapUtils.getTileNumberY(zoom, nodeLineEnd.getLatitude());
		double x = MapUtils.getTileNumberX(zoom, node.getLongitude());
		double y = MapUtils.getTileNumberY(zoom, node.getLatitude());
		double A = x - x1;
		double B = y - y1;
		double C = x2 - x1;
		double D = y2 - y1;
		return Math.abs(A * D - C * B) / Math.sqrt(C * C + D * D);
	}
	
	public static boolean isClockwiseWay(Way w){
		return isClockwiseWay(Collections.singletonList(w));
	}
	
	public static boolean isClockwiseWay(List<Way> ways){
		if(ways.isEmpty()){
			return true;
		}
		LatLon latLon = ways.get(0).getLatLon();
		double lat = latLon.getLatitude();
		double lon = 180;
		double firstLon = -360;
		boolean firstDirectionUp = false;
		double previousLon = -360;
		
		double clockwiseSum = 0;
		
		Node prev = null;
		boolean firstWay = true;
		for(Way w : ways){
			List<Node> ns = w.getNodes();
			int startInd = 0;
			int nssize = ns.size();
			if(firstWay && nssize > 0){
				prev = ns.get(0);
				startInd = 1;
				firstWay = false;
			}
			for(int i = startInd; i < nssize;i++) {
				Node next = ns.get(i);
				double rlon = ray_intersect_lon(prev, next, lat, lon);
				if(rlon != - 360d){
					boolean skipSameSide = (prev.getLatitude() <= lat) == (next.getLatitude() <= lat);
					if(skipSameSide){
						continue;
					}
					boolean directionUp = prev.getLatitude() <= lat;
					if(firstLon == - 360){
						firstDirectionUp = directionUp;
						firstLon = rlon;
					} else {
						boolean clockwise = (!directionUp) == (previousLon < rlon);
						if(clockwise){
							clockwiseSum += Math.abs(previousLon - rlon);
						} else {
							clockwiseSum -= Math.abs(previousLon - rlon);
						}
					}
					previousLon = rlon;
				}
				prev = next;
			}
		}
		
		if(firstLon != -360){
			boolean clockwise = (!firstDirectionUp) == (previousLon < firstLon);
			if(clockwise){
				clockwiseSum += Math.abs(previousLon - firstLon);
			} else {
				clockwiseSum -= Math.abs(previousLon - firstLon);
			}
		}
		
		return clockwiseSum >= 0;
	}
	
	public static boolean isClockwiseWay(TLongList c) {
		if (c.size() == 0) {
			return true;
		}

		// calculate middle Y
		long mask = 0xffffffffl;
		long middleY = 0;
		for (int i = 0; i < c.size(); i++) {
			middleY =  middleY +  (long)(c.get(i) & mask);
		}
		middleY = middleY /(long) c.size();

		double clockwiseSum = 0;

		boolean firstDirectionUp = false;
		int previousX = Integer.MIN_VALUE;
		int firstX = Integer.MIN_VALUE;

		int prevX = (int) (c.get(0) >> 32);
		int prevY = (int) (c.get(0) & mask);

		for (int i = 1; i < c.size(); i++) {
			int x = (int) (c.get(i) >> 32);
			int y = (int) (c.get(i) & mask);
			int rX = ray_intersect_x(prevX, prevY, x, y, (int) middleY);
			if (rX != Integer.MIN_VALUE) {
				boolean skipSameSide = (y <= middleY) == (prevY <= middleY);
				if (skipSameSide) {
					continue;
				}
				boolean directionUp = prevY >= middleY;
				if (firstX == Integer.MIN_VALUE) {
					firstDirectionUp = directionUp;
					firstX = rX;
				} else {
					boolean clockwise = (!directionUp) == (previousX < rX);
					if (clockwise) {
						clockwiseSum += Math.abs(previousX - rX);
					} else {
						clockwiseSum -= Math.abs(previousX - rX);
					}
				}
				previousX = rX;
			}
			prevX = x;
			prevY = y;
		}
		if (firstX != Integer.MIN_VALUE) {
			boolean clockwise = (!firstDirectionUp) == (previousX < firstX);
			if (clockwise) {
				clockwiseSum += Math.abs(previousX - firstX);
			} else {
				clockwiseSum -= Math.abs(previousX - firstX);
			}
		}

		return clockwiseSum >= 0;
	}
	
	// try to intersect from left to right
	public static double ray_intersect_lon(Node node, Node node2, double latitude, double longitude) {
		// a node below
		Node a = node.getLatitude() < node2.getLatitude() ? node : node2;
		// b node above
		Node b = a == node2 ? node : node2;
		if (latitude == a.getLatitude() || latitude == b.getLatitude()) {
			latitude += 0.00000001d;
		}
		if (latitude < a.getLatitude() || latitude > b.getLatitude()) {
			return -360d;
		} else {
			if (longitude < Math.min(a.getLongitude(), b.getLongitude())) {
				return -360d;
			} else {
				if (a.getLongitude() == b.getLongitude() && longitude == a.getLongitude()) {
					// the node on the boundary !!!
					return longitude;
				}
				// that tested on all cases (left/right)
				double lon = b.getLongitude() -
					(b.getLatitude() - latitude) * (b.getLongitude() - a.getLongitude()) / (b.getLatitude() - a.getLatitude());
				if (lon <= longitude) {
					return lon;
				} else {
					return -360d;
				}
			}
		}
	}
	
	public static int ray_intersect_x(int prevX, int prevY, int x, int y, int middleY) {
		// prev node above line
		// x,y node below line
		if (prevY > y) {
			int tx = x;
			int ty = y;
			x = prevX;
			y = prevY;
			prevX = tx;
			prevY = ty;
		}
		if (y == middleY || prevY == middleY) {
			middleY -= 1;
		}
		if (prevY > middleY || y < middleY) {
			return Integer.MIN_VALUE;
		} else {
			if (y == prevY) {
				// the node on the boundary !!!
				return x;
			}
			// that tested on all cases (left/right)
			double rx = x + ((double) middleY - y) * ((double) x - prevX) / (((double) y - prevY));
			return (int) rx;
		}
	}
	
	

	private static long combine2Points(int x, int y) {
		return (((long) x ) <<32) | ((long)y );
	}
	/**
	 * outx,outy are the coordinates out of the box 
	 * inx,iny are the coordinates from the box (NOT IMPORTANT in/out, just one should be in second out)
	 * @return -1 if there is no instersection or x<<32 | y
	 */
	public static long calculateIntersection(int inx, int iny, int outx, int outy, int leftX, int rightX, int bottomY, int topY) {
		int by = -1;
		int bx = -1;
		// firstly try to search if the line goes in
		if (outy < topY && iny >= topY) {
			int tx = (int) (outx + ((double) (inx - outx) * (topY - outy)) / (iny - outy));
			if (leftX <= tx && tx <= rightX) {
				bx = tx;
				by = topY;
				return combine2Points(bx, by);
			}
		}
		if (outy > bottomY && iny <= bottomY) {
			int tx = (int) (outx + ((double) (inx - outx) * (outy - bottomY)) / (outy - iny));
			if (leftX <= tx && tx <= rightX) {
				bx = tx;
				by = bottomY;
				return combine2Points(bx, by);
			}
		}
		if (outx < leftX && inx >= leftX) {
			int ty = (int) (outy + ((double) (iny - outy) * (leftX - outx)) / (inx - outx));
			if (ty >= topY && ty <= bottomY) {
				by = ty;
				bx = leftX;
				return combine2Points(bx, by);
			}

		}
		if (outx > rightX && inx <= rightX) {
			int ty = (int) (outy + ((double) (iny - outy) * (outx - rightX)) / (outx - inx));
			if (ty >= topY && ty <= bottomY) {
				by = ty;
				bx = rightX;
				return combine2Points(bx, by);
			}

		}

		// try to search if point goes out
		if (outy > topY && iny <= topY) {
			int tx = (int) (outx + ((double) (inx - outx) * (topY - outy)) / (iny - outy));
			if (leftX <= tx && tx <= rightX) {
				bx = tx;
				by = topY;
				return combine2Points(bx, by);
			}
		}
		if (outy < bottomY && iny >= bottomY) {
			int tx = (int) (outx + ((double) (inx - outx) * (outy - bottomY)) / (outy - iny));
			if (leftX <= tx && tx <= rightX) {
				bx = tx;
				by = bottomY;
				return combine2Points(bx, by);
			}
		}
		if (outx > leftX && inx <= leftX) {
			int ty = (int) (outy + ((double) (iny - outy) * (leftX - outx)) / (inx - outx));
			if (ty >= topY && ty <= bottomY) {
				by = ty;
				bx = leftX;
				return combine2Points(bx, by);
			}

		}
		if (outx < rightX && inx >= rightX) {
			int ty = (int) (outy + ((double) (iny - outy) * (outx - rightX)) / (outx - inx));
			if (ty >= topY && ty <= bottomY) {
				by = ty;
				bx = rightX;
				return combine2Points(bx, by);
			}

		}

		if (outx == rightX || outx == leftX || outy == topY || outy == bottomY) {
			bx = outx;
			by = outy;
			//return (((long) bx) << 32) | ((long) by);
		}
		return -1l;
	}
	
	/**
	 * return true if the line segment [a,b] intersects [c,d]
	 * @param a point 1
	 * @param b point 2
	 * @param c point 3
	 * @param d point 4
	 * @return true if the line segment [a,b] intersects [c,d]
	 */

	public static boolean linesIntersect(LatLon a, LatLon b, LatLon c, LatLon d){

		return linesIntersect(
				a.getLatitude(), a.getLongitude(),
				b.getLatitude(), b.getLongitude(),
				c.getLatitude(), c.getLongitude(),
				d.getLatitude(), d.getLongitude());

	}

  	/**
  	 * Return true if two line segments intersect inside the segment
  	 * 
  	 * source: http://www.java-gaming.org/index.php?topic=22590.0
  	 * @param x1 line 1 point 1 latitude
  	 * @param y1 line 1 point 1 longitude
  	 * @param x2 line 1 point 2 latitude
  	 * @param y2 line 1 point 2 longitude
  	 * @param x3 line 2 point 1 latitude
  	 * @param y3 line 2 point 1 longitude
  	 * @param x4 line 2 point 2 latitude
  	 * @param y4 line 2 point 2 longitude
  	 * @return
  	 */

  	public static boolean linesIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4){

  	     // Return false if either of the lines have zero length
  	     if (x1 == x2 && y1 == y2 ||
  	           x3 == x4 && y3 == y4){
  	        return false;
  	     }

  	     // Fastest method, based on Franklin Antonio's "Faster Line Segment Intersection" topic "in Graphics Gems III" book (http://www.graphicsgems.org/)

  	     double ax = x2-x1;
  	     double ay = y2-y1;
  	     double bx = x3-x4;
  	     double by = y3-y4;
  	     double cx = x1-x3;
  	     double cy = y1-y3;

  	 
  	     double alphaNumerator = by*cx - bx*cy;
  	     double commonDenominator = ay*bx - ax*by;
  	     if (commonDenominator > 0){
  	        if (alphaNumerator < 0 || alphaNumerator > commonDenominator){
  	           return false;
  	        }
        }else if (commonDenominator < 0){
  	        if (alphaNumerator > 0 || alphaNumerator < commonDenominator){
  	           return false;
  	        }
  	     }

  	     double betaNumerator = ax*cy - ay*cx;
  	     if (commonDenominator > 0){
  	        if (betaNumerator < 0 || betaNumerator > commonDenominator){
  	           return false;
  	        }
  	     }else if (commonDenominator < 0){
  	        if (betaNumerator > 0 || betaNumerator < commonDenominator){
  	          return false;
  	        }
  	     }

  	     if (commonDenominator == 0){
  	        // This code wasn't in Franklin Antonio's method. It was added by Keith Woodward.
  	        // The lines are parallel.
  	        // Check if they're collinear.
  	        double y3LessY1 = y3-y1;
  	        double collinearityTestForP3 = x1*(y2-y3) + x2*(y3LessY1) + x3*(y1-y2);   // see http://mathworld.wolfram.com/Collinear.html
  	       // If p3 is collinear with p1 and p2 then p4 will also be collinear, since p1-p2 is parallel with p3-p4

  	        if (collinearityTestForP3 == 0){
  	           // The lines are collinear. Now check if they overlap.
  	           if (x1 >= x3 && x1 <= x4 || x1 <= x3 && x1 >= x4 ||
  	                 x2 >= x3 && x2 <= x4 || x2 <= x3 && x2 >= x4 ||
  	                 x3 >= x1 && x3 <= x2 || x3 <= x1 && x3 >= x2){
  	              if (y1 >= y3 && y1 <= y4 || y1 <= y3 && y1 >= y4 ||
  	                    y2 >= y3 && y2 <= y4 || y2 <= y3 && y2 >= y4 ||
  	                    y3 >= y1 && y3 <= y2 || y3 <= y1 && y3 >= y2){
  	                 return true;
  	              }
  	           }
  	        }
  	        return false;
  	     }
  	     return true;
  	}
  	
  	
  	/**
  	 * Get the area (in m²) of a closed way, represented as a list of nodes
  	 * @param nodes the list of nodes
  	 * @return the area of it
  	 */
  	public static double getArea(List<Node> nodes) {
  		// x = longitude 
  		// y = latitude
  		// calculate the reference point (lower left corner of the bbox)
  		// start with an arbitrary value, bigger than any lat or lon
  		double refX = 500, refY = 500;
  		for (Node n : nodes){
  			if (n.getLatitude() < refY) refY = n.getLatitude(); 
  			if (n.getLongitude() < refX) refX = n.getLongitude();
  		}
  		
  		List<Double> xVal = new ArrayList<Double>();
  		List<Double> yVal = new ArrayList<Double>();
  		
  		for (Node n : nodes) {
  			// distance from bottom line to x coordinate of node
  			double xDist = MapUtils.getDistance(refY, refX, refY, n.getLongitude());
  			// distance from left line to y coordinate of node
  			double yDist = MapUtils.getDistance(refY, refX, n.getLatitude(), refX);
  			
  			xVal.add(xDist);
  			yVal.add(yDist);
  		}
  		
  		double area = 0;
  		
  		for(int i = 1; i<xVal.size(); i++) {
  			area += xVal.get(i-1)*yVal.get(i) - xVal.get(i)*yVal.get(i-1);
  		}
  		
  		return Math.abs(area) / 2;
  	}
}