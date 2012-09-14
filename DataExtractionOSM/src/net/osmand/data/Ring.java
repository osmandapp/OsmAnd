package net.osmand.data;

import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

/**
 * A ring is a list of ways that form a simple boundary or an area.
 * 
 * 
 * 
 * @author sander
 *
 */
public class Ring implements Comparable<Ring>{
	/**
	 * This is a list of the ways added by the user
	 * The order can be changed with methods from this class
	 */
	private ArrayList<Way> ways;
	/**
	 * This is the closure of the ways added by the user
	 * So simple two-node ways are added to close the ring
	 * This is a cache from what can calculated with the ways
	 */
	private ArrayList<Way> closedWays;
	/**
	 * This is a single way, consisting of all the nodes 
	 * from ways in the closedWays
	 * this is a cache from what can be calculated with the closedWays
	 */
	private Way closedBorder;

	/**
	 * Construct a Ring with a list of ways
	 * @param ways the ways that make up the Ring
	 */
	public Ring(List<Way> ways) {
		this.ways = new ArrayList<Way>();
		this.ways.addAll(ways);
	}

	/**
	 * Construct an empty Ring
	 */
	public Ring() {
		this.ways = new ArrayList<Way>();
	}

	/**
	 * Get the ways added to the Ring.
	 * This is not closed
	 * The order is not fixed
	 * @return the ways added to the Ring
	 */
	public List<Way> getWays() {
		return ways;
	}

	/**
	 * Add a way to the Ring
	 * @param w the way to add
	 */
	public void addWay(Way w) {
		// Reset the cache
		closedWays = null;
		closedBorder = null;
		// Add the way
		ways.add(w);
	}

	/**
	 * Get the closed ways that make up the Ring
	 * This method will sort the ways, so it is CPU intensive
	 * If extra ways are added to the ring, these ways will have random negative ids
	 * @return the closed ways
	 */
	public List<Way> getClosedWays() {
		// Add ways to close the ring
		closeWays();
		return closedWays;
	}
	
	/**
	 * check if this ring is closed by nature
	 * @return true if this ring is closed, false otherwise
	 */
	public boolean isClosed() {
		List<Way> cl = getClosedWays();
		List<Way> ways = getWays();
		for (Way w : cl) {
			if (!ways.contains(w)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * get a single closed way that represents the border
	 * this method is CPU intensive. <p/ >
	 * The returned border will have a random negative id
	 * @return a closed way that represents the border
	 */
	public Way getBorder() {
		mergeWays();
		return closedBorder;
	}

	/**
	 * Merge all ways from the closedways into a single way
	 * If the original ways are initialized with nodes, the new one will be so too
	 */
	private void mergeWays() {
		if (closedBorder != null) return;
		
		closeWays();

		closedBorder = new Way(Math.round(Math.random()*(-9000000000000000000L)));
		
		Long previousConnection = getMultiLineEndNodes(closedWays)[0];
		
		for (Way w : closedWays) {
			boolean firstNode = true;
			TLongArrayList nodeIds = w.getNodeIds();
			List<Node> nodes = w.getNodes();
			
			if (w.getFirstNodeId() == previousConnection) {
				
				for (int i = 0; i< nodeIds.size(); i++) {
					// don't need to add the first node, that one was added by the previous way
					if (!firstNode) {
						if(nodes == null || i>=nodes.size()) {
							closedBorder.addNode(nodeIds.get(i));
						} else {
							closedBorder.addNode(nodes.get(i));
						}
						
					}
					firstNode = false;
				}
				
				previousConnection = w.getLastNodeId();
			} else {
				
				// add the nodes in reverse order
				for (int i = nodeIds.size() - 1; i >= 0; i--) {
					// don't need to add the first node, that one was added by the previous way
					if (!firstNode) {
						if(nodes == null || i>=nodes.size()) {
							closedBorder.addNode(nodeIds.get(i));
						} else {
							closedBorder.addNode(nodes.get(i));
						}
					}
					firstNode = false;
				}
				
				previousConnection = w.getFirstNodeId();
				
			}
		}
		
	}

	/**
	 * Check if there exists a cache, if so, return it
	 * If there isn't a cache, sort the ways to form connected strings <p />
	 * 
	 * If a Ring contains a gap, one way (without initialized nodes and id=0) is added to the list
	 */
	private void closeWays(){
		// If the ways have been closed, return the cache
		if (closedWays != null) return;
		if (ways.size() == 0) {
			closedWays = new ArrayList<Way>();
			return;
		}
		ArrayList<ArrayList<Way>> multiLines = createMultiLines(ways);
		
		// TODO try to close rings which consist out of multiple segments.
		// This is a data fault, but it could be solved a bit by OsmAnd
		if (multiLines.size() != 1) return;
		
		ArrayList<Way> multiLine = multiLines.get(0);
		
		closedWays = multiLine;
		
		
		long[] endNodes = getMultiLineEndNodes(multiLine);
		
		
		if (endNodes[0] != endNodes[1]) {
			int lastML = multiLine.size() - 1;
			
			Way w = new Way(Math.round(Math.random()*(-9000000000000000000L)));
			closedWays.add(w);
			
			if(multiLine.get(0).getNodes() == null || 
					multiLine.get(lastML).getNodes() == null) {
				
				w.addNode(endNodes[0]);
				w.addNode(endNodes[1]);
			} else {
				Node n1 = null, n2 = null;
				if (multiLine.get(0).getFirstNodeId() == endNodes[0]) {
					n1 = multiLine.get(0).getNodes().get(0);
				} else {
					int index = multiLine.get(0).getNodes().size() - 1;
					n1 = multiLine.get(0).getNodes().get(index);
				}
				
				if (multiLine.get(lastML).getFirstNodeId() == endNodes[0]) {
					n2 = multiLine.get(lastML).getNodes().get(0);
				} else {
					int index = multiLine.get(lastML).getNodes().size() - 1;
					n2 = multiLine.get(lastML).getNodes().get(index);
				}
				
				w.addNode(n1);
				w.addNode(n2);
		 	}
		}
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
			 * FIXME TO LOG OR NOT TO LOG?
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
					// hope there is a fourth one, so these two will be processed later on
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
	 * Get the end nodes of a multiLine
	 * The ways in the multiLine don't have to be initialized for this.
	 * 
	 * @param multiLine the multiLine to get the end nodes of
	 * @return an array of size two with the end nodes on both sides. <br />
	 *  * The first node is the end node of the first way in the multiLine. <br />
	 *  * The second node is the end node of the last way in the multiLine. 
	 */
	private long[] getMultiLineEndNodes(ArrayList<Way> multiLine) {
		
		// special case, the multiLine contains only a single way, return the end nodes of the way
		if (multiLine.size() == 1){
			return new long[] {multiLine.get(0).getFirstNodeId(), multiLine.get(0).getLastNodeId()};
		}
		
		if (multiLine.size() == 2) {
			// ring of two elements, arbitrary choice of the end nodes
			if(multiLine.get(0).getFirstNodeId() == multiLine.get(1).getFirstNodeId() && 
					multiLine.get(0).getLastNodeId() == multiLine.get(1).getLastNodeId()) {
				return new long[] {multiLine.get(0).getFirstNodeId(), multiLine.get(0).getFirstNodeId()};
			} else if(multiLine.get(0).getFirstNodeId() == multiLine.get(1).getLastNodeId() && 
					multiLine.get(0).getLastNodeId() == multiLine.get(1).getFirstNodeId()) {
				return new long[] {multiLine.get(0).getFirstNodeId(), multiLine.get(0).getFirstNodeId()};
			}
		}
		
		// For all other multiLine lenghts, or for non-closed multiLines with two elements, proceed
		
		long n1 = 0, n2 = 0;
		
		if (multiLine.get(0).getFirstNodeId() == multiLine.get(1).getFirstNodeId() ||
				multiLine.get(0).getFirstNodeId() == multiLine.get(1).getLastNodeId()) {
			n1 = multiLine.get(0).getLastNodeId();
		} else if (multiLine.get(0).getLastNodeId() == multiLine.get(1).getFirstNodeId() ||
				multiLine.get(0).getLastNodeId() == multiLine.get(1).getLastNodeId()) {
			n1 = multiLine.get(0).getFirstNodeId();
		}
		
		int lastIdx = multiLine.size()-1;
		
		if (multiLine.get(lastIdx).getFirstNodeId() == multiLine.get(1).getFirstNodeId() ||
				multiLine.get(lastIdx).getFirstNodeId() == multiLine.get(1).getLastNodeId()) {
			n2 = multiLine.get(lastIdx).getLastNodeId();
		} else if (multiLine.get(lastIdx).getLastNodeId() == multiLine.get(lastIdx - 1).getFirstNodeId() ||
				multiLine.get(lastIdx).getLastNodeId() == multiLine.get(lastIdx - 1).getLastNodeId()) {
			n2 = multiLine.get(lastIdx).getFirstNodeId();
		}
		
		return new long[] {n1, n2};
	}
	
	/**
	 * Combine a list of ways to a list of rings
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
		
		mergeWays();
		List<Node> polyNodes = closedBorder.getNodes();
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
	 * collect the points of all ways added by the user <br />
	 * automatically added ways because of closing the Ring won't be added <br />
	 * Only ways with initialized points can be handled.
	 * @return a List with nodes
	 */
	public List<Node> collectPoints() {
		
		ArrayList<Node> collected = new ArrayList<Node>();
		
		for (Way w : ways) {
			collected.addAll(w.getNodes());
		}
		
		return collected;
		
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
		List<Node> points = this.collectPoints();
		
		// r should contain all nodes of this
		for(Node n : points) {
			if (!r.containsNode(n)) {
				return false;
			}
		}
		
		points = r.collectPoints();
		
		// this should not contain a node from r
		for(Node n : points) {
			if (this.containsNode(n)) {
				return false;
			}
		}
		
		return true;
		
	}

	
	@Override
	/**
	 * @return -1 if this Ring is inside r <br />
	 * 1 if r is inside this Ring <br />
	 * 0 otherwise (Rings are next to each other, Rings intersect or Rings are malformed)
	 */
	public int compareTo(Ring r) {
		if (this.isIn(r)) return -1;
		if (r.isIn(this)) return 1;
		return 0;
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
	public Ring closeWithOtherRing(Ring other) {
		Way thisBorder = getBorder();
		Way otherBorder = other.getBorder();
		
		boolean insideOther = other.containsNode(thisBorder.getNodes().get(0));
		boolean firstNodeInOther = insideOther;
		
		List<Way> wayPartsInsideOther = new ArrayList<Way>();
		Way wayPart = new Way(Math.round(Math.random()*(-9000000000000000000L)));
		wayPartsInsideOther.add(wayPart);
		
		
		// Search the node pairs for which the ring goes inside or out the other
		for (int i = 0; i<thisBorder.getNodes().size(); i++) {
			Node n = thisBorder.getNodes().get(i);
			
			if (other.containsNode(n) != insideOther) {
				
				// we are getting out or in the boundary now.
				// toggle switch
				insideOther = !insideOther;
				
				if(!insideOther) {
					// we just went out the boundary, add one more node
					wayPart.addNode(n);
				} else {
					// we just went in the boundary, create new way with previous node
					wayPart = new Way(Math.round(Math.random()*(-9000000000000000000L)));
					wayPart.addNode(thisBorder.getNodes().get(i-1));

					wayPartsInsideOther.add(wayPart);
				}
			}
			
			if(insideOther){
				wayPart.addNode(n);
			}
		}
		
		Way finalBorder = new Way(Math.round(Math.random()*(-9000000000000000000L)));
		
		
		if (firstNodeInOther) {
			if(!insideOther) return null; // How can we do this?
			Way firstWay = wayPartsInsideOther.get(0);
			Way lastWay = wayPartsInsideOther.get(wayPartsInsideOther.size() - 1);
			if(firstWay == lastWay) return this; // Nothing to do
			lastWay.removeNodeByIndex(lastWay.getNodes().size() - 1);
			for (Node n : firstWay.getNodes()) {
				lastWay.addNode(n);
			}
			wayPartsInsideOther.remove(firstWay);
		}
		
		/*
		 * Status: 
		 * 
		 * We have now a list of ways of which the first and the last node are inside the other
		 * Ring, and of which all other nodes are inside the other.
		 * 
		 * We must now connect these ways with the shortest (=less nodes) possible parts of other ring.
		 */
		
		for (int i = 0; i< wayPartsInsideOther.size(); i++) {
			// add nodes of this to the final border
			for (Node n : wayPartsInsideOther.get(i).getNodes()) {
				finalBorder.addNode(n);
			}
			
			int firstWaySize = wayPartsInsideOther.get(i).getNodes().size();
			// This node is inside the other
			Node firstWayNode1 = wayPartsInsideOther.get(i).getNodes().get(firstWaySize - 2);
			// This node is outside the other
			Node firstWayNode2 = wayPartsInsideOther.get(i).getNodes().get(firstWaySize - 1);
			LatLon a = firstWayNode1.getLatLon();
			LatLon b = firstWayNode2.getLatLon();
			int firstIntersectionIndex = crossRingBorder(a, b);
			
			// This node is outside the border
			Node secondWayNode1 = wayPartsInsideOther.get((i+1) %  wayPartsInsideOther.size()).getNodes().get(0);
			// This node is inside the border
			Node secondWayNode2 = wayPartsInsideOther.get((i+1) %  wayPartsInsideOther.size()).getNodes().get(1);
			
			a = secondWayNode1.getLatLon();
			b = secondWayNode2.getLatLon();
			int secondIntersectionIndex = crossRingBorder(a, b);
			
			boolean reversed = firstIntersectionIndex >= secondIntersectionIndex; // used reversed logic
			
			int highestInters = Math.max(firstIntersectionIndex, secondIntersectionIndex);
			int lowestInters = Math.min(firstIntersectionIndex, secondIntersectionIndex);
			
			int maxInters = other.getBorder().getNodes().size();
			
			if (highestInters - lowestInters < maxInters/2) {
				// the shortest is going from lowest to highest
				if (!reversed) {
					// add some nodes from the other to the final border
					for (int j = lowestInters + 1; j< highestInters+1; j++) {
						finalBorder.addNode(otherBorder.getNodes().get(j));
					}
				} else {
					// add some nodes from the other to the final border in reversed order
					for (int j = highestInters; j> lowestInters; j--) {
						finalBorder.addNode(otherBorder.getNodes().get(j));
					}
				}
			} else {
				// the shortest is going from highest to lowest, with a wrap
				if(!reversed) {
					// add two parts of nodes to this border
					for (int j = highestInters + 1; j< maxInters; j++) {
						finalBorder.addNode(otherBorder.getNodes().get(j));
					}
					for (int j = 0; j< lowestInters + 1; j++) {
						finalBorder.addNode(otherBorder.getNodes().get(j));
					}
				} else {
					// add two parts of nodes to this border
					for (int j = lowestInters; j >= 0; j--) {
						finalBorder.addNode(otherBorder.getNodes().get(j));
					}
					for (int j = maxInters; j>highestInters; j--) {
						finalBorder.addNode(otherBorder.getNodes().get(j));
					}
				}
			}
			
		}
		
		List<Way> borders = new ArrayList<Way>();
		borders.add(finalBorder);
		
		return new Ring(borders);
	}
	
	/**
	 * Get the segment of the Ring that intersects a segment 
	 * going from point a to point b
	 * 
	 * @param a the begin point of the segment
 	 * @param b the end point of the segment
	 * @return an integer i which is the index so that the segment 
	 * 		from getBorder().get(i) to getBorder().get(i+1) intersects with 
	 * 		the segment from parameters a to b. <p />
	 * 
	 * 		0 if the segment from a to b doesn't intersect with the Ring. 
	 */
	public int crossRingBorder(LatLon a, LatLon b) {
		Way border = getBorder();
		for (int i = 0; i<=border.getNodes().size(); i++) {
			LatLon c = border.getNodes().get(i).getLatLon();
			LatLon d = border.getNodes().get(i+1).getLatLon();
			if (MapUtils.linesIntersect(a,b,c,d)) {
				return i;
			}
		}
		return 0;
		
	}
	
	
}
