package com.osmand.data;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class City {
	
	public enum CityType {
		 CITY(10000), TOWN(5000), VILLAGE(1000), HAMLET(300), SUBURB(300);
		 
		 private double radius;
		 private CityType(double radius){
			 this.radius = radius;
		 }
		 
		 public double getRadius() {
			return radius;
		}
	}
	
	private final Node el;
	private CityType type = null;
	private Map<String, Street> streets = new TreeMap<String, Street>(); 

	public City(Node el){
		this.el = el;
		String place = el.getTag(OSMTagKey.PLACE);
		for(CityType t : CityType.values()){
			if(t.name().equalsIgnoreCase(place)){
				type = t;
				break;
			}
		}
	}
	
	public Street registerBuilding(LatLon point, Entity e){
		String number = e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
		String street = e.getTag(OSMTagKey.ADDR_STREET);
		if( street != null && number != null){
			if(!streets.containsKey(street)){
				streets.put(street, new Street(street));
			}
			streets.get(street).registerBuilding(point, e);
			return streets.get(street);
		}
		return null;
	}
	
	
	public String getName(){
		return el.getTag(OSMTagKey.NAME);
	}
	
	public CityType getType(){
		return type;
	}
	
	public Node getNode(){
		return  el;
	}
	
	public Collection<Street> getStreets(){
		return streets.values();
	}
	
	@Override
	public String toString() {
		return "City [" +type+"] " + getName();
	}

}
