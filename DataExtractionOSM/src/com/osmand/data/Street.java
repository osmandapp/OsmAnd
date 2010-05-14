package com.osmand.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.Node;

public class Street {
	
	private final String name;
	private Map<Entity, LatLon> buildings = new HashMap<Entity, LatLon>();
	private List<Node> wayNodes = new ArrayList<Node>(); 

	public Street(String name){
		this.name = name;
	}
	
	public void registerBuilding(LatLon point, Entity e){
		buildings.put(e, point);
	}
	
	public Set<Entity> getBuildings() {
		return buildings.keySet();
	}
	
	public LatLon getLocationBuilding(Entity e){
		return buildings.get(e);
	}
	
	public String getName() {
		return name;
	}
	
	public List<Node> getWayNodes() {
		return wayNodes;
	}

}
