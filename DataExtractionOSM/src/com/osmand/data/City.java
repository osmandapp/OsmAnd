package com.osmand.data;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.osmand.Algoritms;
import com.osmand.osm.Entity;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class City extends MapObject<Node> {
	
	public enum CityType {
		CITY(10000), TOWN(5000), VILLAGE(1000), HAMLET(300), SUBURB(300);

		private double radius;

		private CityType(double radius) {
			this.radius = radius;
		}

		public double getRadius() {
			return radius;
		}

		public static String valueToString(CityType t) {
			return t.toString().toLowerCase();
		}
		
		public static CityType valueFromString(String place) {
			if (place == null) {
				return null;
			}
			for (CityType t : CityType.values()) {
				if (t.name().equalsIgnoreCase(place)) {
					return t;
				}
			}
			return null;
		}
	}
	
	private CityType type = null;
	private Map<String, Street> streets = new TreeMap<String, Street>(); 

	public City(Node el){
		super(el);
		type = CityType.valueFromString(el.getTag(OSMTagKey.PLACE));
	}
	
	public City(CityType type){
		this.type = type;
	}
	
	public Street registerStreet(String street){
		if(!streets.containsKey(street)){
			streets.put(street, new Street(street));
		}
		return streets.get(street); 
	}
	
	public Street registerStreet(Street street){
		if(!Algoritms.isEmpty(street.getName())){
			return streets.put(street.getName(), street);
		}
		return null;
	}
	
	public Street registerBuilding(Entity e){
		String number = e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
		String street = e.getTag(OSMTagKey.ADDR_STREET);
		if( street != null && number != null){
			registerStreet(street).registerBuilding(e);
			return streets.get(street);
		}
		return null;
	}
	
	
	public CityType getType(){
		return type;
	}
	
	public Collection<Street> getStreets(){
		return streets.values();
	}
	
	@Override
	public String toString() {
		return "City [" +type+"] " + getName();
	}
	
	public void doDataPreparation(){
		for(Street s : getStreets()){
			s.doDataPreparation();
		}
	}

}
