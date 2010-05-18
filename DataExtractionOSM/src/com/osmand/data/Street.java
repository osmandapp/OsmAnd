package com.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.osmand.Algoritms;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.Way;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class Street extends MapObject<Entity> {
	
	private List<Building> buildings = new ArrayList<Building>(); 
	private List<Way> wayNodes = new ArrayList<Way>();
	private final City city;

	public Street(City city, String name){
		this.city = city;
		this.name = name;
	}
	
	public Street(City city) {
		this.city = city;
	}
	
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
		List<Node> nodes = new ArrayList<Node>();
		for(Way w : wayNodes){
			nodes.addAll(w.getNodes());
		}
		
		LatLon c = MapUtils.getWeightCenterForNodes(nodes);
		double dist = Double.POSITIVE_INFINITY;
		for(Node n : nodes){
			if (n != null) {
				double nd = MapUtils.getDistance(n, c);
				if (nd < dist) {
					entity = n;
					dist = nd;
				}
			}
		}
	}
	
	@Override
	public void setName(String name) {
		if(name.equals(getName())){
			return;
		}
		Street unregisterStreet = city.unregisterStreet(getName());
		assert unregisterStreet == this;
		super.setName(name);
		city.registerStreet(this);
	}
	
	
	public List<Way> getWayNodes() {
		return wayNodes;
	}

	public void doDataPreparation() {
		calculateCenter();
		Collections.sort(buildings, new Comparator<Building>(){

			@Override
			public int compare(Building o1, Building o2) {
				int i1 = Algoritms.extractFirstIntegerNumber(o1.getName());
				int i2 = Algoritms.extractFirstIntegerNumber(o2.getName());
				return i1 - i2;
			}
			
		});
	}

}
