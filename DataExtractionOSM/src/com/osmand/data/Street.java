package com.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class Street extends MapObject<Entity> {
	
	private final String name;
	private List<Building> buildings = new ArrayList<Building>(); 
	private List<Node> wayNodes = new ArrayList<Node>();
	private Node center = null;

	public Street(String name){
		this.name = name;
	}
	
	public void registerBuilding(Entity e){
		Building building = new Building(e);
		building.setName(e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER));
		buildings.add(building);
	}
	
	public List<Building> getBuildings() {
		return buildings;
	}
	
	public String getName() {
		return name;
	}

	
	public LatLon getLocation(){
		if(center == null){
			calculateCenter();
		}
		return center == null ? null : center.getLatLon();
	}
	
	protected void calculateCenter(){
		if(wayNodes.size() == 1){
			center = wayNodes.get(0);
			return;
		}
		LatLon c = MapUtils.getWeightCenterForNodes(wayNodes);
		double dist = Double.POSITIVE_INFINITY;
		for(Node n : wayNodes){
			if (n != null) {
				double nd = MapUtils.getDistance(n, c);
				if (nd < dist) {
					center = n;
					dist = nd;
				}
			}
		}
	}
	
	
	public List<Node> getWayNodes() {
		return wayNodes;
	}

	public void doDataPreparation() {
		calculateCenter();
		Collections.sort(buildings);
	}

	@Override
	public Entity getEntity() {
		return center;
	}

}
