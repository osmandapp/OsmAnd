package com.osmand.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;

public class Street {
	
	private final String name;
	private Map<Entity, LatLon> buildings = new HashMap<Entity, LatLon>(); 

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

}
