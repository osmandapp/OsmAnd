package net.osmand.data;

import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

public class Boundary {
	
	private long boundaryId;
	private String name;
	private String adminLevel;
	
	
	// not necessary ready rings
	private List<Way> outerWays = new ArrayList<Way>();
	private List<Way> innerWays = new ArrayList<Way>();
	
	
	public boolean containsPoint(LatLon point) {
		return containsPoint(point.getLatitude(), point.getLongitude());
	}
	
	public boolean containsPoint(double latitude, double longitude) {
		int intersections = 0;
		for(Way w : outerWays){
			for(int i=0; i<w.getNodes().size() - 1; i++){
				if(ray_intersect(w.getNodes().get(i), w.getNodes().get(i+1), latitude, longitude)){
					intersections ++;
				}
			}
		}
		for(Way w : innerWays){
			for(int i=0; i<w.getNodes().size() - 1; i++){
				if(ray_intersect(w.getNodes().get(i), w.getNodes().get(i+1), latitude, longitude)){
					intersections ++;
				}
			}
		}

		return intersections % 2 == 1;
	}
	
	// Try to intersect with ray from left to right
	private boolean ray_intersect(Node node, Node node2, double latitude, double longitude) {
		// a node below 
		Node a = node.getLatitude() < node2.getLatitude() ? node : node2;
		// b node above
		Node b = a == node2 ? node : node2;
		if(latitude == a.getLatitude() || latitude == b.getLatitude()){
			latitude += 0.00000001d;
		}
		if(latitude < a.getLatitude() || latitude > b.getLatitude()){
			return false;
		} else {
			if(longitude > Math.max(a.getLongitude(), b.getLongitude())) {
				return true;
			} else if(longitude < Math.min(a.getLongitude(), b.getLongitude())){
				return false;
			} else {
				if(a.getLongitude() == b.getLongitude()) {
					// the node on the boundary !!!
					return true;
				}
				// that tested on all cases (left/right)
			    double mR = (b.getLatitude() - a.getLatitude()) / (b.getLongitude() - a.getLongitude());
				double mB = (latitude - a.getLatitude()) / (longitude - a.getLongitude());
				if(mB <= mR){
					return true;
				} else {
					return false;
				}
			}
		}
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

	public String getAdminLevel() {
		return adminLevel;
	}

	public void setAdminLevel(String adminLevel) {
		this.adminLevel = adminLevel;
	}
	
	
}
