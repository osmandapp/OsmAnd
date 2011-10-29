package net.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

public class Boundary {
	
	private long boundaryId;
	private String name;
	private int adminLevel;
	
	
	// not necessary ready rings
	private List<Way> outerWays = new ArrayList<Way>();
	private List<Way> innerWays = new ArrayList<Way>();
	private final boolean closedWay;
	
	public Boundary(boolean closedWay){
		this.closedWay = closedWay;
	}
	
	public boolean isClosedWay() {
		return closedWay;
	}
	
	public boolean computeIsClosedWay()
	{
		//now we try to merge the ways until we have only one
		int oldSize = 0;
		while (getOuterWays().size() != oldSize) {
			oldSize = getOuterWays().size();
			mergeOuterWays();
		}
		//there is one way and last element is equal to the first...
		return getOuterWays().size() == 1 && getOuterWays().get(0).getNodes().get(0).getId() == getOuterWays().get(0).getNodes().get(getOuterWays().get(0).getNodes().size()-1).getId();
	}
	
	
	private void mergeOuterWays() {
		Way way = getOuterWays().get(0);
		List<Node> nodes = way.getNodes();
		if (!nodes.isEmpty()) {
			int nodesSize = nodes.size();
			Node first = nodes.get(0);
			Node last = nodes.get(nodesSize-1);
			int size = getOuterWays().size();
			for (int i = size-1; i >= 1; i--) {
				//try to find way, that matches the one ...
				Way anotherWay = getOuterWays().get(i);
				if (anotherWay.getNodes().isEmpty()) {
					//remove empty one...
					getOuterWays().remove(i);
				} else {
					if (anotherWay.getNodes().get(0).getId() == first.getId()) {
						//reverese this way and add it to the actual
						Collections.reverse(anotherWay.getNodes());
						way.getNodes().addAll(0,anotherWay.getNodes());
						getOuterWays().remove(i);
					} else if (anotherWay.getNodes().get(0).getId() == last.getId()) {
						way.getNodes().addAll(anotherWay.getNodes());
						getOuterWays().remove(i);
					} else if (anotherWay.getNodes().get(anotherWay.getNodes().size()-1).getId() == first.getId()) {
						//add at begging
						way.getNodes().addAll(0,anotherWay.getNodes());
						getOuterWays().remove(i);
					} else if (anotherWay.getNodes().get(anotherWay.getNodes().size()-1).getId() == last.getId()) {
						Collections.reverse(anotherWay.getNodes());
						way.getNodes().addAll(anotherWay.getNodes());
						getOuterWays().remove(i);
					}
				}
			}
		} else {
			//remove way with no nodes!
			getOuterWays().remove(0);
		}
	}

	public boolean containsPoint(LatLon point) {
		return containsPoint(point.getLatitude(), point.getLongitude());
	}
	
	public boolean containsPoint(double latitude, double longitude) {
		int intersections = 0;
		for(Way w : outerWays){
			for(int i=0; i<w.getNodes().size() - 1; i++){
				if(MapAlgorithms.ray_intersect_lon(w.getNodes().get(i), w.getNodes().get(i+1), latitude, longitude) != -360d){
					intersections ++;
				}
			}
		}
		for(Way w : innerWays){
			for(int i=0; i<w.getNodes().size() - 1; i++){
				if(MapAlgorithms.ray_intersect_lon(w.getNodes().get(i), w.getNodes().get(i+1), latitude, longitude) != -360d){
					intersections ++;
				}
			}
		}

		return intersections % 2 == 1;
	}
	
	public LatLon getCenterPoint(){
		List<Node> points = new ArrayList<Node>();
		for(Way w : outerWays){
			points.addAll(w.getNodes());
		}
		for(Way w : innerWays){
			points.addAll(w.getNodes());
		}
		return MapUtils.getWeightCenterForNodes(points);
	}
	
	
	public List<Way> getOuterWays() {
		return outerWays;
	}
	
	public List<Way> getInnerWays() {
		return innerWays;
	}
	
	public long getBoundaryId() {
		return boundaryId;
	}
	
	public void setBoundaryId(long boundaryId) {
		this.boundaryId = boundaryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAdminLevel() {
		return adminLevel;
	}

	public void setAdminLevel(int adminLevel) {
		this.adminLevel = adminLevel;
	}
	

}
