package net.osmand.data;


import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

/**
 * A ring is a list of ways that form a simple boundary or an area. <p />
 * 
 * @author sander
 *
 */
public class Ring implements Comparable<Ring> {
	/**
	 * This is a list of the ways added by the user
	 * The order can be changed with methods from this class
	 */
	private final ArrayList<Way> ways;
	
	/**
	 * a concatenation of the ways to form the border
	 * this is NOT necessarily a CLOSED way
	 * The id is random, so this may never leave the Ring object
	 */
	private Way border;
	
	/**
	 * area can be asked a lot of times when comparing rings, chace it
	 */
	private double area = -1;
	
	

	/**
	 * Construct a Ring with a list of ways
	 * @param ways the ways that make up the Ring
	 */
	private Ring(List<Way> ways) {
		this.ways = new ArrayList<Way>(ways);
	}


	/**
	 * Get the ways added to the Ring.
	 * This is not closed
	 * The order is not fixed
	 * @return the ways added to the Ring
	 */
	public List<Way> getWays() {
		return new ArrayList<Way>(ways);
	}
	
	
	/**
	 * check if this ring is closed by nature
	 * @return true if this ring is closed, false otherwise
	 */
	public boolean isClosed() {
		mergeWays();
		return border.getFirstNodeId() == border.getLastNodeId();
	}
	
	/**
	 * get a single closed way that represents the border
	 * this method is CPU intensive
	 * @return a list of Nodes that represents the border
	 * if the border can't be created, an empty list will be returned
	 */
	public List<Node> getBorder() {
		mergeWays();
		List<Node> l = border.getNodes();
		if (border.getNodes().size() != 0 && !isClosed()) {
			l.add(border.getNodes().get(0));
		}
		return l;
	}

	/**
	 * Merge all ways from the into a single border way
	 * If the original ways are initialized with nodes, the border will be so too
	 * If the original ways aren't initialized with nodes, the border won't be either
	 * If only some original ways are initialized with nodes, the border will only have the nodes of the initialized ways
	 */
	private void mergeWays() {
		if (border != null) return;
		
		
		//make a copy of the ways
		List<Way> ways = new ArrayList<Way>(getWays());
		
		// do we have to include ways with uninitialized nodes?
		// Only if all ways have uninitialized nodes
		boolean unInitializedNodes = true;
		
		for (Way w : ways) {
			if (w.getNodes() != null && w.getNodes().size() != 0) {
				unInitializedNodes = false;
				break;
			}
		}
		
		List<Way> borderWays = new ArrayList<Way>();
		
		
		for (Way w : ways) {
			// if the way has no nodes initialized, and we should initialize them, continue
			if ((w.getNodes() == null || w.getNodes().size() == 0) &&
					!unInitializedNodes) continue;
			
			// merge the Way w with the first borderway suitable, repeat until nothing can be merged
			Way wayToMerge = w;
			Way newWay;
			do {
				newWay = null;
				for (Way borderWay : borderWays) {
					newWay = combineTwoWaysIfHasPoints(wayToMerge, borderWay);
					if(newWay != null) {
						wayToMerge = newWay;
						borderWays.remove(borderWay);
						break;
					}
				}
			} while (newWay != null);
			//no suitable borderWay has been found, add this way as one of the boundaries
			borderWays.add(wayToMerge);
		}
		
		if (borderWays.size() != 1) {
			border = new Way(nextRandId());
			return;
		}
		
		border = borderWays.get(0);
		
		return;
		
	}
	
	/**
	 * check if this Ring contains the node
	 * @param n the Node to check
	 * @return yes if the node is inside the ring
	 */
	public boolean containsNode(Node n) {
		return  containsPoint(n.getLatitude(), n.getLongitude());
	}
	
	/**
	 * check if this Ring contains the point
	 * @param latitude lat of the point
	 * @param longitude lon of the point
	 * @return yes if the point is inside the ring
	 */
	public boolean containsPoint(double latitude, double longitude){
		return  countIntersections(latitude, longitude) % 2 == 1;
	}
	
	/**
	 * count the intersections when going from lat, lon to outside the ring
	 * @param latitude the lat to start
	 * @param longitude the lon to start
	 * @param intersections the number of intersections to start with
	 * @return the number of intersections
	 */
	private int countIntersections(double latitude, double longitude) {
		int intersections = 0;
		
		List<Node> polyNodes = getBorder();
		if (polyNodes.size() == 0) return 0;
		for (int i = 0; i < polyNodes.size() - 1; i++) {
			if (MapAlgorithms.ray_intersect_lon(polyNodes.get(i),
					polyNodes.get(i + 1), latitude, longitude) != -360d) {
				intersections++;
			}
		}
		// special handling, also count first and last, might not be closed, but
		// we want this!
		if (MapAlgorithms.ray_intersect_lon(polyNodes.get(0),
				polyNodes.get(polyNodes.size() - 1), latitude, longitude) != -360d) {
			intersections++;
		}
		return intersections;
	}
	
	
	/**
	 * Check if this is in Ring r
	 * @param r the ring to check
	 * @return true if this Ring is inside Ring r
	 */
	public boolean isIn(Ring r) {
		/*
		 * bi-directional check is needed because some concave rings can intersect
		 * and would only fail on one of the checks
		 */
		List<Node> points = this.getBorder();
		
		// r should contain all nodes of this
		for(Node n : points) {
			if (!r.containsNode(n)) {
				return false;
			}
		}
		
		points = r.getBorder();
		
		// this should not contain a node from r
		for(Node n : points) {
			if (this.containsNode(n)) {
				return false;
			}
		}
		
		return true;
		
	}

	
	/**
	 * If this Ring is not complete 
	 * (some ways are not initialized 
	 * because they are not included in the OSM file) <p />
	 * 
	 * We are trying to close this Ring by using the other Ring.<p />
	 * 
	 * The other Ring must be complete, and the part of this Ring 
	 * inside the other Ring must also be complete.
	 * @param other the other Ring (which is complete) used to close this one
	 */
	public void closeWithOtherRing(Ring other) {
		List<Node> thisBorder = getBorder();
		List<Integer> thisSwitchPoints = new ArrayList<Integer>();
		
		boolean insideOther = other.containsNode(thisBorder.get(0));
		
		// Search the node pairs for which the ring goes inside or out the other
		for (int i = 0; i<thisBorder.size(); i++) {
			Node n = thisBorder.get(i);
			if (other.containsNode(n) != insideOther) {
				// we are getting out or in the boundary now.
				// toggle switch
				insideOther = !insideOther;
				
				thisSwitchPoints.add(i);
			}
		}
		
		List<Integer> otherSwitchPoints = new ArrayList<Integer>();
		
		// Search the according node pairs in the other ring
		for (int i : thisSwitchPoints) {
			LatLon a = thisBorder.get(i-1).getLatLon();
			LatLon b = thisBorder.get(i).getLatLon();
			otherSwitchPoints.add(crossRingBorder(a, b));
		}
		
		
		
		/*
		 * TODO:
		 * 
		 * * Split the other Ring into ways from splitPoint to splitPoint
		 * 
		 * * Split this ring into ways from splitPoint to splitPoint
		 * 
		 * * Filter out the parts of way from this that are inside the other Ring
		 * 		Use the insideOther var and the switchPoints list for this.
		 * 
		 * * For each two parts of way from this, search a part of way connecting the two. 
		 * 		If there are two, take the shortest.
		 */
	}
	
	/**
	 * Get the segment of the Ring that intersects a segment 
	 * going from point a to point b
	 * 
	 * @param a the begin point of the segment
 	 * @param b the end point of the segment
	 * @return an integer i which is the index so that the segment 
	 * 		from getBorder().get(i-1) to getBorder().get(i) intersects with 
	 * 		the segment from parameters a to b. <p />
	 * 
	 * 		0 if the segment from a to b doesn't intersect with the Ring. 
	 */
	public int crossRingBorder(LatLon a, LatLon b) {
		List<Node> border = getBorder();
		for (int i = 1; i<border.size(); i++) {
			LatLon c = border.get(i-1).getLatLon();
			LatLon d = border.get(i).getLatLon();
			if (MapAlgorithms.linesIntersect(
					a.getLatitude(), a.getLongitude(), 
					b.getLatitude(), b.getLongitude(), 
					c.getLatitude(), c.getLongitude(), 
					d.getLatitude(), d.getLongitude())) {
				return i;
			}
		}
		return 0;
		
	}
	
	public double getArea() {
		if (area == -1) {
			//cache the area
			area = MapAlgorithms.getArea(getBorder());
		}
		return area;
	}
	
	


	/**
	 * Use area size as comparable metric
	 */
	@Override
	public int compareTo(Ring r) {
		return Double.compare(getArea(), r.getArea());
	}
	
	/**
	 * Join the ways in connected strings for further processing
	 * @return A list with list of connected ways
	 */
	private static ArrayList<ArrayList<Way>> createMultiLines(List<Way> ways){
		// make a list of multiLines (connecter pieces of way)
		// One ArrayList<Way> is one multiLine
		ArrayList<ArrayList<Way>> multiLines = new ArrayList<ArrayList<Way>>();
		for (Way toAdd : ways) {
			/* 
			 * Check if the way has at least 2 nodes
			 * 
			 * TO LOG OR NOT TO LOG?
			 *
			 *		logging this creates a whole bunch of log lines for all ways
			 *		part of a multipolygon but not in the map
			 */
			if (toAdd.getNodeIds().size() < 2) {
				continue;
			}

			long toAddBeginPt = toAdd.getFirstNodeId();
			long toAddEndPt = toAdd.getLastNodeId();

			// the way has been added to this number of multiLines
			int addedTo = 0;

			// save the first and second changed multiLine
			ArrayList<Way> firstMultiLine = new ArrayList<Way> ();
			ArrayList<Way> secondMultiLine = new ArrayList<Way> ();


			// iterate over the multiLines, and add the way to the correct one
			for( ArrayList<Way> multiLine : multiLines) {

				// to check if this multiLine has been changed at the end of the loop
				int previousAddedTo = addedTo;

				// get the first and last way of a multiLine
				Way firstWay = multiLine.get(0);
				Way lastWay = multiLine.get(multiLine.size() - 1);
				// add the way to the correct multiLines (maybe two)
				if (toAddBeginPt == firstWay.getFirstNodeId() || 
						toAddBeginPt == firstWay.getLastNodeId() || 
						toAddEndPt == firstWay.getFirstNodeId() || 
						toAddEndPt == firstWay.getLastNodeId() ) {
					// add the way to the begining to respect order
					multiLine.add(0, toAdd);
					addedTo++;
				} else if (toAddBeginPt == lastWay.getFirstNodeId() || 
						toAddBeginPt == lastWay.getLastNodeId() || 
						toAddEndPt == lastWay.getFirstNodeId() || 
						toAddEndPt == lastWay.getLastNodeId()) {
					// add the way to the end
					multiLine.add(toAdd);
					addedTo++;
				}

				// save this multiLines if it has been changed
				if (previousAddedTo != addedTo) {

					if (addedTo == 1) {
						firstMultiLine = multiLine;
					} 

					if (addedTo == 2) {
						secondMultiLine = multiLine;
					} 

					// a Ring may never contain a fork
					// if there is a third multiline, don't process
					// hope there is a fourth one, sot these two will be processed later on
				}

			}

			// If the way is added to nothing, make a new multiLine
			if (addedTo == 0 ) {
				ArrayList<Way> multiLine = new ArrayList<Way>();
				multiLine.add(toAdd);
				multiLines.add(multiLine);
				continue;
			}

			//everything OK
			if (addedTo == 1) continue;


			// only the case addedTo == 2 remains
			// two multiLines have to be merged

			if (firstMultiLine.get(firstMultiLine.size() - 1) == secondMultiLine.get(0)) {
				// add the second to the first
				secondMultiLine.remove(0) ;
				for (Way w : secondMultiLine) {
					firstMultiLine.add(w);
				}
				multiLines.remove(secondMultiLine);
			} else if (secondMultiLine.get(secondMultiLine.size() - 1) == firstMultiLine.get(0)) {
				// just add the first to the second
				firstMultiLine.remove(0) ;
				for (Way w : firstMultiLine) {
					secondMultiLine.add(w);
				}
				multiLines.remove(firstMultiLine);
			} else if (firstMultiLine.get(0) == secondMultiLine.get(0)) {
				// add the first in reversed to the beginning of the  second
				firstMultiLine.remove(toAdd);
				for (Way w : firstMultiLine) {
					secondMultiLine.add(0,w);
				}
				multiLines.remove(firstMultiLine);
			} else {
				// add the first in reversed to the end of the second
				firstMultiLine.remove(toAdd);
				int index = secondMultiLine.size();
				for (Way w : firstMultiLine) {
					secondMultiLine.add(index ,w);
				}
				multiLines.remove(firstMultiLine);
			}


		}
		return multiLines;
	}
	
	/**
	 * Combine a list of ways into a list of rings
	 * 
	 * The ways must not have initialized nodes for this
	 * 
	 * @param ways the ways to group
	 * @return a list of Rings
	 */
	public static ArrayList<Ring> combineToRings(List<Way> ways){
		ArrayList<ArrayList<Way>> multiLines = createMultiLines(ways);
		
		ArrayList<Ring> result = new ArrayList<Ring> ();
		
		for (ArrayList<Way> multiLine : multiLines) {
			Ring r = new Ring(multiLine);
			result.add(r);
		}
		
		return result;
	}
	
	
	private static long initialValue = -1000;
	private final static long randomInterval = 5000;
	/**
	 * get a random long number
	 * @return
	 */
	private static long nextRandId() {
		// exclude duplicates in one session (!) and be quazirandom every run
		long val = initialValue - Math.round(Math.random()*randomInterval);
		initialValue = val;
		return val;
	}
	
	/**
	 * make a new Way with the nodes from two other ways
	 * @param w1 the first way
	 * @param w2 the second way
	 * @return null if it is not possible
	 */
	private static Way combineTwoWaysIfHasPoints(Way w1, Way w2) {
		boolean combine = true;
		boolean firstReverse = false;
		boolean secondReverse = false;
		if(w1.getFirstNodeId() == w2.getFirstNodeId()) {
			firstReverse = true;
			secondReverse = false;
		} else if(w1.getLastNodeId() == w2.getFirstNodeId()) {
			firstReverse = false;
			secondReverse = false;
		} else if(w1.getLastNodeId() == w2.getLastNodeId()) {
			firstReverse = false;
			secondReverse = true;
		} else if(w1.getFirstNodeId() == w2.getLastNodeId()) {
			firstReverse = true;
			secondReverse = true;
		} else {
			combine = false;
		}
		if (combine) {
			Way newWay = new Way(nextRandId());
			boolean nodePresent = w1.getNodes() != null || w1.getNodes().size() != 0;
			int w1size = nodePresent ? w1.getNodes().size() : w1.getNodeIds().size();
			for (int i = 0; i < w1size; i++) {
				int ind = firstReverse ? (w1size - 1 - i) : i;
				if (nodePresent) {
					newWay.addNode(w1.getNodes().get(ind));
				} else {
					newWay.addNode(w1.getNodeIds().get(ind));
				}
			}
			int w2size = nodePresent ? w2.getNodes().size() : w2.getNodeIds().size();
			for (int i = 1; i < w2size; i++) {
				int ind = secondReverse ? (w2size - 1 - i) : i;
				if (nodePresent) {
					newWay.addNode(w2.getNodes().get(ind));
				} else {
					newWay.addNode(w2.getNodeIds().get(ind));
				}
			}
			return newWay;
		}
		return null;
		
	}
	

}
