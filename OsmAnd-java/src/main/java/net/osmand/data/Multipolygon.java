package net.osmand.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OsmMapUtils;
import net.osmand.util.Algorithms;

public class Multipolygon {
	private List<Ring> innerRings, outerRings;
	private Map<Ring, Set<Ring>> containedInnerInOuter = new LinkedHashMap<Ring, Set<Ring>>();

	private float maxLat = -90;
	private float minLat = 90;
	private float maxLon = -180;
	private float minLon = 180;
	private long id;


	public Multipolygon(List<Ring> outer, List<Ring> inner, long id) {
		outerRings = outer;
		innerRings = inner;
		this.id = id;
		updateRings();
	}

	public Multipolygon(Ring outer, List<Ring> inner, long id, boolean checkedIsIn) {
		outerRings = new ArrayList<Ring>();
		outerRings.add(outer);
		innerRings = inner;
		this.id = id;
		updateRings(checkedIsIn);
	}

	public MultiPolygon toMultiPolygon() {
		GeometryFactory geometryFactory = new GeometryFactory();
		MultiPolygon emptyMultiPolygon = geometryFactory.createMultiPolygon(new Polygon[0]);
		List<Polygon> polygons = new ArrayList<>();
		for (Ring outerRing : outerRings) {
			if (!outerRing.isClosed()) {
				return emptyMultiPolygon;
			}
			List<LinearRing> innerLinearRings = new ArrayList<>();
			Set<Ring> innerRings = containedInnerInOuter.get(outerRing);
			if (!Algorithms.isEmpty(innerRings)) {
				for (Ring innerRing : innerRings) {
					if (!innerRing.isClosed()) {
						return emptyMultiPolygon;
					}
					innerLinearRings.add(innerRing.toLinearRing());
				}
			}
			polygons.add(geometryFactory.createPolygon(outerRing.toLinearRing(), innerLinearRings.toArray(new LinearRing[0])));
		}
		return geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
	}

	public long getId() {
		return id;
	}

	private void updateRings() {
		updateRings(false);
	}

	private void updateRings(boolean checkedIsIn) {
		maxLat = -90;
		minLat = 90;
		maxLon = -180;
		minLon = 180;
		for (Ring r : outerRings) {
			for (Node n : r.getBorder()) {
				maxLat = (float) Math.max(maxLat, n.getLatitude());
				minLat = (float) Math.min(minLat, n.getLatitude());
				maxLon = (float) Math.max(maxLon, n.getLongitude());
				minLon = (float) Math.min(minLon, n.getLongitude());
			}
		}
		// keep sorted
		Collections.sort(outerRings);
		for (Ring inner : innerRings) {
			HashSet<Ring> outContainingRings = new HashSet<Ring>();
			if (checkedIsIn && outerRings.size() == 1) {
				outContainingRings.add(outerRings.get(0));
			} else {
				for (Ring out : outerRings) {
					if (inner.isIn(out)) {
						outContainingRings.add(out);
					}
				}				
			}
			containedInnerInOuter.put(inner, outContainingRings);
		}
		// keep sorted
		Collections.sort(innerRings);
	}

	/**
	 * check if this multipolygon contains a point
	 *
	 * @param latitude  lat to check
	 * @param longitude lon to check
	 * @return true if this multipolygon is correct and contains the point
	 */
	public boolean containsPoint(double latitude, double longitude) {
		// fast check
		if (maxLat + 0.3 < latitude || minLat - 0.3 > latitude ||
				maxLon + 0.3 < longitude || minLon - 0.3 > longitude) {
			return false;
		}

		Ring containedInOuter = null;
		// use a sortedset to get the smallest outer containing the point
		for (Ring outer : outerRings) {
			if (outer.containsPoint(latitude, longitude)) {
				containedInOuter = outer;
				break;
			}
		}

		if (containedInOuter == null) {
			return false;
		}

		//use a sortedSet to get the smallest inner Ring
		Ring containedInInner = null;
		for (Ring inner : innerRings) {
			if (inner.containsPoint(latitude, longitude)) {
				containedInInner = inner;
				break;
			}
		}

		if (containedInInner == null) return true;
		if (outerRings.size() == 1) {
			// return immediately false
			return false;
		}

		// if it is both, in an inner and in an outer, check if the inner is indeed the smallest one
		Set<Ring> s = containedInnerInOuter.get(containedInInner);
		if (s == null) {
			throw new IllegalStateException();
		}
		return !s.contains(containedInOuter);
	}

	/**
	 * check if this multipolygon contains a point
	 *
	 * @param point point to check
	 * @return true if this multipolygon is correct and contains the point
	 */
	public boolean containsPoint(LatLon point) {
		return containsPoint(point.getLatitude(), point.getLongitude());
	}

	public int countOuterPolygons() {
		return zeroSizeIfNull(outerRings);
	}

	private int zeroSizeIfNull(Collection<?> l) {
		return l != null ? l.size() : 0;
	}

	/**
	 * Get the weighted center of all nodes in this multiPolygon <br />
	 * This only works when the ways have initialized nodes
	 *
	 * @return the weighted center
	 */
	public LatLon getCenterPoint() {
		List<Node> points = new ArrayList<Node>();
		for (Ring w : outerRings) {
			points.addAll(w.getBorder());
		}
		if (points.isEmpty()) {
			for (Ring w : innerRings) {
				points.addAll(w.getBorder());
			}
		}

		return OsmMapUtils.getWeightCenterForNodes(points);
	}

	public void mergeWith(Multipolygon multipolygon) {
		innerRings.addAll(multipolygon.innerRings);
		outerRings.addAll(multipolygon.outerRings);
		updateRings();
	}

	public boolean hasOpenedPolygons() {
		return !areRingsComplete();
	}

	public boolean areRingsComplete() {
		List<Ring> l = outerRings;
		for (Ring r : l) {
			if (!r.isClosed()) {
				return false;
			}
		}
		l = innerRings;
		for (Ring r : l) {
			if (!r.isClosed()) {
				return false;
			}
		}
		return true;
	}
	
	public QuadRect getLatLonBbox() {
		if(minLat == 90) {
			return new QuadRect();
		}
		return new QuadRect(minLon, maxLat, maxLon, minLat);
	}
	

	public List<Ring> getInnerRings() {
		return innerRings;
	}

	public List<Ring> getOuterRings() {
		return outerRings;
	}

	
}
