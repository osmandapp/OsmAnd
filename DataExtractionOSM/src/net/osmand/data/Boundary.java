package net.osmand.data;

import java.util.ArrayList;
import java.util.Collection;
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
	
	private List<Boundary> subboundaries = new ArrayList<Boundary>();
	
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

	public String getAdminLevel() {
		return adminLevel;
	}

	public void setAdminLevel(String adminLevel) {
		this.adminLevel = adminLevel;
	}
	
	public List<Boundary> getSubboundaries() {
		return subboundaries;
	}
	
	public void addSubBoundary(Boundary subBoundary) {
		if (subBoundary != null) {
			subboundaries.add(subBoundary);
		}
	}

	public void addSubBoundaries(Collection<Boundary> subBoundaries) {
		subboundaries.addAll(subBoundaries);
	}

}
