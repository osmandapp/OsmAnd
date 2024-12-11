package net.osmand.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OsmMapUtils;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapAlgorithms;


/**
 * A ring is a list of CONTIGUOUS ways that form a simple boundary or an area. <p />
 *
 * @author sander
 */
public class Ring implements Comparable<Ring> {
	/**
	 * This is a list of the ways added by the user
	 * The order can be changed with methods from this class
	 */
//	private final ArrayList<Way> ways;
	
	private static final int INDEX_RING_NODES_FAST_CHECK = 100;
	private static final int INDEX_SIZE = 100;
	private double[] indexedRingIntervals = null;
	private List<Node>[] indexedRingNodes = null;

	/**
	 * a concatenation of the ways to form the border
	 * this is NOT necessarily a CLOSED way
	 * The id is random, so this may never leave the Ring object
	 */
	private Way border;

	/**
	 * area can be asked a lot of times when comparing rings, cache it
	 */
	private double area = -1;


	/**
	 * Construct a Ring with a list of ways
	 *
	 * @param ways the ways that make up the Ring
	 */
	Ring(Way w) {
		border = w;
		indexForFastCheck();
	}

	@SuppressWarnings("unchecked")
	private void indexForFastCheck() {
		if(border.getNodes().size() > INDEX_RING_NODES_FAST_CHECK) {
			// calculate min/max lat
			double maxLat = Double.MIN_VALUE;
			double minLat = Double.MAX_VALUE;
			Node lastNode = null;
			for(Node n : border.getNodes()) {
				if(n == null) {
					continue;
				}
				lastNode = n;
				if(n.getLatitude() > maxLat) {
					maxLat = n.getLatitude();
				} else if(n.getLatitude() < minLat) {
					minLat = n.getLatitude();
				}
			}
			maxLat += 0.0001;
			minLat -= 0.0001;
			// create interval array [minLat, minLat+interval, ..., maxLat]
			double interval = (maxLat - minLat) / (INDEX_SIZE - 1);
			indexedRingIntervals = new double[INDEX_SIZE];
			indexedRingNodes = new List[INDEX_SIZE];
			for(int i = 0; i < INDEX_SIZE; i++) {
				indexedRingIntervals[i] = minLat + i * interval;		
				indexedRingNodes[i] = new ArrayList<Node>();
			}
			// split nodes by intervals
			Node prev = lastNode;
			for(int i = 0; i < border.getNodes().size(); i++) {
				Node current = border.getNodes().get(i);
				if(current == null) {
					continue;
				}
				int i1 = getIndexedLessOrEq(current.getLatitude());
				int i2 = getIndexedLessOrEq(prev.getLatitude());
				int min, max;
				if(i1 > i2) {
					min = i2;
					max = i1;
				} else {
					min = i1;
					max = i2;
				}
				for (int j = min; j <= max; j++) {
					indexedRingNodes[j].add(prev);
					indexedRingNodes[j].add(current);
				}
				prev = current;
			}
		}
		
	}

	private int getIndexedLessOrEq(double latitude) {
		int ind1 = Arrays.binarySearch(indexedRingIntervals, latitude);
		if(ind1 < 0) {
			ind1 = -(ind1 + 1);
		}
		return ind1;
	}

	/**
	 * check if this ring is closed by nature
	 *
	 * @return true if this ring is closed, false otherwise
	 */
	public boolean isClosed() {
		return border.getFirstNodeId() == border.getLastNodeId();
	}

	public List<Node> getBorder() {
		return border.getNodes();
	}


	/**
	 * check if this Ring contains the node
	 *
	 * @param n the Node to check
	 * @return yes if the node is inside the ring
	 */
	public boolean containsNode(Node n) {
		return containsPoint(n.getLatitude(), n.getLongitude());
	}

	/**
	 * check if this Ring contains the point
	 *
	 * @param latitude  lat of the point
	 * @param longitude lon of the point
	 * @return yes if the point is inside the ring
	 */
	public boolean containsPoint(double latitude, double longitude) {
		if(indexedRingIntervals != null) {
			int intersections = 0;
			int indx = getIndexedLessOrEq(latitude);
			if(indx == 0 || indx >= indexedRingNodes.length) {
				return false;
			}
			List<Node> lst = indexedRingNodes[indx];
			for (int k = 0; k < lst.size(); k += 2) {
				Node first = lst.get(k);
				Node last = lst.get(k + 1);
				if (OsmMapUtils.ray_intersect_lon(first, last, latitude, longitude) != -360.0d) {
					intersections++;
				}
			}
			return intersections  % 2 == 1;
		}
		return MapAlgorithms.containsPoint(getBorder(), latitude, longitude);
	}
	
	/**
	 * Check if this is in Ring r
	 * @param r the ring to check
	 * @return true if this Ring is inside Ring r (false if it is undetermined)
	 */
	public boolean speedIsIn(Ring r) {
		/*
		 * bi-directional check is needed because some concave rings can intersect
		 * and would only fail on one of the checks
		 */
		List<Node> points = this.getBorder();
		if(points.size() < 2) {
			return false;
		}
		double minlat = points.get(0).getLatitude();
		double maxlat = points.get(0).getLatitude();
		double minlon = points.get(0).getLongitude();
		double maxlon = points.get(0).getLongitude();
		// r should contain all nodes of this
		for (Node n : points) {
			minlat = Math.min(n.getLatitude(), minlat);
			maxlat = Math.max(n.getLatitude(), maxlat);
			minlon = Math.min(n.getLongitude(), minlon);
			maxlon = Math.max(n.getLongitude(), maxlon);
		}
		
		// r should contain all nodes of this
		if (!r.containsPoint(minlat, minlon)) {
			return false;
		}
		if (!r.containsPoint(maxlat, minlon)) {
			return false;
		}
		if (!r.containsPoint(minlat, maxlon)) {
			return false;
		}
		if (!r.containsPoint(maxlat, maxlon)) {
			return false;
		}
		// this should not contain a node from r
		for (Node n : r.getBorder()) {
			if(n.getLatitude() > minlat && n.getLatitude() < maxlat && 
					n.getLongitude() > minlon && n.getLongitude() < maxlon) {
				return false;
			}
		}

		return true;

	}

	/**
	 * Check if this is in Ring r
	 *
	 * @param r the ring to check
	 * @return true if this Ring is inside Ring r
	 */
	public boolean isIn(Ring r) {
		if(speedIsIn(r)) {
			return true;
		}
		/*
		 * bi-directional check is needed because some concave rings can intersect
		 * and would only fail on one of the checks
		 */
		List<Node> points = this.getBorder();

		// r should contain all nodes of this
		for (Node n : points) {
			if (!r.containsNode(n)) {
				return false;
			}
		}

		points = r.getBorder();

		// this should not contain a node from r
		for (Node n : points) {
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
	 *
	 * @param other the other Ring (which is complete) used to close this one
	 */
	public void closeWithOtherRing(Ring other) {
		List<Node> thisBorder = getBorder();
		List<Integer> thisSwitchPoints = new ArrayList<Integer>();

		boolean insideOther = other.containsNode(thisBorder.get(0));

		// Search the node pairs for which the ring goes inside or out the other
		for (int i = 0; i < thisBorder.size(); i++) {
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
			LatLon a = thisBorder.get(i - 1).getLatLon();
			LatLon b = thisBorder.get(i).getLatLon();
			otherSwitchPoints.add(getTheSegmentRingIntersectsSegment(a, b));
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
	 * from getBorder().get(i-1) to getBorder().get(i) intersects with
	 * the segment from parameters a to b. <p />
	 * <p>
	 * 0 if the segment from a to b doesn't intersect with the Ring.
	 */
	private int getTheSegmentRingIntersectsSegment(LatLon a, LatLon b) {
		List<Node> border = getBorder();
		for (int i = 1; i < border.size(); i++) {
			LatLon c = border.get(i - 1).getLatLon();
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
			area = OsmMapUtils.getArea(getBorder());
		}
		return area;
	}

	public LinearRing toLinearRing() {
		GeometryFactory geometryFactory = new GeometryFactory();
		CoordinateList coordinates = new CoordinateList();
		for (Node node : border.getNodes()) {
			coordinates.add(new Coordinate(node.getLatitude(), node.getLongitude()), true);
		}
		coordinates.closeRing();
		return geometryFactory.createLinearRing(coordinates.toCoordinateArray());
	}

	/**
	 * Use area size as comparable metric
	 */
	@Override
	public int compareTo(Ring r) {
		return Double.compare(getArea(), r.getArea());
	}
}
