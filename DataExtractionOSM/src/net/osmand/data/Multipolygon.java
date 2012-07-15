package net.osmand.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Stack;

import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

/**
 * The idea of multipolygon:
 * - we treat each outer way as closed polygon
 * - multipolygon is always closed!
 * - each way we try to assign to existing way and form 
 *   so a more complex polygon
 * - number of outer ways, is number of polygons
 * 
 * @author Pavol Zibrita
 */
public class Multipolygon {

	protected List<Way> closedOuterWays;
	protected List<Way> outerWays;
	protected List<Way> closedInnerWays;
	protected List<Way> innerWays;
	
	protected IdentityHashMap<Way,List<Way>> outerInnerMapping;
	
	private void addNewPolygonPart(List<Way> polygons, List<Way> closedPolygons, Way newPoly) {
		if (isClosed(newPoly)) {
			closedPolygons.add(newPoly); //if closed, put directly to closed polygons
		} else if (polygons.isEmpty()) { 
			polygons.add(newPoly); //if open, and first, put to polygons..
		} else {
			// now we try to merge the ways to form bigger polygons
			Stack<Way> wayStack = new Stack<Way>();
			wayStack.push(newPoly);
			addAndMergePolygon(polygons, closedPolygons, wayStack);
		}
		//reset the mapping
		outerInnerMapping = null;
	}

	private boolean isClosed(Way newPoly) {
		List<Node> ns = newPoly.getNodes();
		return !ns.isEmpty() && ns.get(0).getId() == ns.get(ns.size()-1).getId();
	}

	private void addAndMergePolygon(List<Way> polygons, List<Way> closedPolygons, Stack<Way> workStack) {
		while (!workStack.isEmpty()) {
			Way changedWay = workStack.pop();
			List<Node> nodes = changedWay.getNodes();
			if (nodes.isEmpty()) {
				//don't bother with it!
				continue;
			}
			if (isClosed(changedWay)) {
				polygons.remove(changedWay);
				closedPolygons.add(changedWay);
				continue;
			}
			
			Node first = nodes.get(0);
			Node last = nodes.get(nodes.size()-1);
			for (Way anotherWay : polygons) {
				if (anotherWay == changedWay) {
					continue;
				}
				//try to find way, that matches the one ...
				if (anotherWay.getNodes().get(0).getId() == first.getId()) {
					Collections.reverse(changedWay.getNodes());
					anotherWay.getNodes().addAll(0,changedWay.getNodes());
					workStack.push(anotherWay);
					break;
				} else if (anotherWay.getNodes().get(0).getId() == last.getId()) {
					anotherWay.getNodes().addAll(0,changedWay.getNodes());
					workStack.push(anotherWay);
					break;
				} else if (anotherWay.getNodes().get(anotherWay.getNodes().size()-1).getId() == first.getId()) {
					anotherWay.getNodes().addAll(changedWay.getNodes());
					workStack.push(anotherWay);
					break;
				} else if (anotherWay.getNodes().get(anotherWay.getNodes().size()-1).getId() == last.getId()) {
					Collections.reverse(changedWay.getNodes());
					anotherWay.getNodes().addAll(changedWay.getNodes());
					workStack.push(anotherWay);
					break;
				}
			}
			//if we could not merge the new polygon, and it is not already there, add it!
			if (workStack.isEmpty() && !polygons.contains(changedWay)) {
				polygons.add(changedWay);
			}
		}
	}

	public boolean containsPoint(LatLon point) {
		return containsPoint(point.getLatitude(), point.getLongitude());
	}

	public boolean containsPoint(double latitude, double longitude) {
		return containsPointInPolygons(closedOuterWays, latitude, longitude) || containsPointInPolygons(outerWays, latitude, longitude);
	}

	private boolean containsPointInPolygons(List<Way> outerPolygons, double latitude, double longitude) {
		if (outerPolygons != null) {
			for (Way polygon : outerPolygons) {
				List<Way> inners = getOuterInnerMapping().get(polygon);
				if (polygonContainsPoint(latitude, longitude, polygon, inners)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean polygonContainsPoint(double latitude, double longitude,
			Way polygon, List<Way> inners) {
		int intersections = 0;
		intersections = countIntersections(latitude, longitude, polygon,
				intersections);
		if (inners != null) {
			for (Way w : inners) {
				intersections = countIntersections(latitude, longitude, w,
						intersections);
			}
		}
		return intersections % 2 == 1;
	}

	private int countIntersections(double latitude, double longitude,
			Way polygon, int intersections) {
		List<Node> polyNodes = polygon.getNodes();
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

	private IdentityHashMap<Way, List<Way>> getOuterInnerMapping() {
		if (outerInnerMapping == null) {
			outerInnerMapping = new IdentityHashMap<Way, List<Way>>();
			//compute the mapping
			if ((innerWays != null || closedInnerWays != null)
					&& countOuterPolygons() != 0) {
				fillOuterInnerMapping(closedOuterWays);
				fillOuterInnerMapping(outerWays);
			}
		}
		return outerInnerMapping;
	}

	private void fillOuterInnerMapping(List<Way> outerPolygons) {
		for (Way outer : outerPolygons) {
			List<Way> inners = new ArrayList<Way>();
			inners.addAll(findInnersFor(outer, innerWays));
			inners.addAll(findInnersFor(outer, closedInnerWays));
			outerInnerMapping.put(outer, inners);
		}
	}

	private Collection<Way> findInnersFor(Way outer, List<Way> inners) {
		List<Way> result = new ArrayList<Way>(inners.size());
		for (Way in : inners) {
			boolean inIsIn = true;
			for (Node n : in.getNodes()) {
				if (!polygonContainsPoint(n.getLatitude(), n.getLongitude(), outer, null)) {
					inIsIn = false;
					break;
				}
			}
			if (inIsIn) {
				result.add(in);
			}
		}
		return result;
	}

	private List<Way> getOuterWays() {
		if (outerWays == null) {
			outerWays = new ArrayList<Way>(1);
		}
		return outerWays;
	}

	private List<Way> getClosedOuterWays() {
		if (closedOuterWays == null) {
			closedOuterWays = new ArrayList<Way>(1);
		}
		return closedOuterWays;
	}

	
	private List<Way> getInnerWays() {
		if (innerWays == null) {
			innerWays = new ArrayList<Way>(1);
		}
		return innerWays;
	}
	
	private List<Way> getClosedInnerWays() {
		if (closedInnerWays == null) {
			closedInnerWays = new ArrayList<Way>(1);
		}
		return closedInnerWays;
	}

	public int countOuterPolygons()
	{
		return zeroSizeIfNull(outerWays) + zeroSizeIfNull(closedOuterWays);
	}

	public boolean hasOpenedPolygons()
	{
	    return zeroSizeIfNull(outerWays) != 0;
	}
	
	private int zeroSizeIfNull(List<Way> list) {
		return list != null ? list.size() : 0;
	}
	
	public void addInnerWay(Way es) {
		addNewPolygonPart(getInnerWays(), getClosedInnerWays(), new Way(es));
	}

	public void addOuterWay(Way es) {
		addNewPolygonPart(getOuterWays(), getClosedOuterWays(), new Way(es));
	}

	public void copyPolygonsFrom(Multipolygon multipolygon) {
		for (Way inner : multipolygon.getInnerWays()) {
			addInnerWay(inner);
		}
		for (Way outer : multipolygon.getOuterWays()) {
			addOuterWay(outer);
		}
		getClosedInnerWays().addAll(multipolygon.getClosedInnerWays());
		getClosedOuterWays().addAll(multipolygon.getClosedOuterWays());
	}

	public void addOuterWays(List<Way> ring) {
		for (Way outer : ring) {
			addOuterWay(outer);
		}
	}

	public LatLon getCenterPoint() {
		List<Node> points = new ArrayList<Node>();
		collectPoints(points, outerWays);
		collectPoints(points, closedOuterWays);
		collectPoints(points, innerWays);
		collectPoints(points, closedInnerWays);
		return MapUtils.getWeightCenterForNodes(points);
	}

	private void collectPoints(List<Node> points, List<Way> polygons) {
		if (polygons != null) {
			for(Way w : polygons){
				points.addAll(w.getNodes());
			}
		}
	}

}
