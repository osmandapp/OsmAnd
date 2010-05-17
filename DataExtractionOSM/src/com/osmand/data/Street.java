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
	
	private List<Building> buildings = new ArrayList<Building>(); 
	private List<Node> wayNodes = new ArrayList<Node>();

	public Street(String name){
		this.name = name;
	}
	
	public Street(){}
	
	public void registerBuilding(Entity e){
		Building building = new Building(e);
		building.setName(e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER));
		buildings.add(building);
	}
	
	public void registerBuilding(Building building){
		buildings.add(building);
	}
	
	public List<Building> getBuildings() {
		return buildings;
	}
	
	public LatLon getLocation(){
		if(entity == null){
			calculateCenter();
		}
		return entity == null ? null : entity.getLatLon();
	}
	
	protected void calculateCenter(){
		if(wayNodes.size() == 1){
			entity = wayNodes.get(0);
			return;
		}
		LatLon c = MapUtils.getWeightCenterForNodes(wayNodes);
		double dist = Double.POSITIVE_INFINITY;
		for(Node n : wayNodes){
			if (n != null) {
				double nd = MapUtils.getDistance(n, c);
				if (nd < dist) {
					entity = n;
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

}
