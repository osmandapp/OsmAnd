package com.osmand.data;

import java.text.Collator;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.osmand.Algoritms;
import com.osmand.osm.Entity;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class City extends MapObject {
	
	public enum CityType {
		// that's tricky way to play with that numbers (to avoid including suburbs in city & vice verse)
		CITY(10000), TOWN(5000), VILLAGE(1300), HAMLET(1000), SUBURB(300);

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
	// Be attentive ! Working with street names ignoring case
	private Map<String, Street> streets = new TreeMap<String, Street>(Collator.getInstance()); 
	private SortedSet<String> postcodes = new TreeSet<String>();
	
	public City(Node el){
		super(el);
		type = CityType.valueFromString(el.getTag(OSMTagKey.PLACE));
	}
	
	public City(CityType type){
		this.type = type;
	}
	
	public boolean isEmptyWithStreets(){
		return streets.isEmpty();
	}
	
	public Street registerStreet(String street){
		if(!streets.containsKey(street.toLowerCase())){
			streets.put(street.toLowerCase(), new Street(this, street));
		}
		return streets.get(street.toLowerCase()); 
	}
	
	public Street unregisterStreet(String name){
		return streets.remove(name.toLowerCase()); 
	}
	
	public void removeAllStreets(){
		streets.clear();
	}
	
	public Street registerStreet(Street street, boolean en){
		String name = en ? street.getEnName(): street.getName();
		name = name.toLowerCase();
		if(!Algoritms.isEmpty(name)){
			if(!streets.containsKey(name)){
				return streets.put(name, street);
			} else {
				// try to merge streets
				Street prev = streets.get(name);
				prev.getWayNodes().addAll(street.getWayNodes());
				prev.getBuildings().addAll(street.getBuildings());
				return prev;
			}
		}
		return null;
	}
	
	public Street registerStreet(Street street){
		return registerStreet(street, false);
	}
	
	public Street registerBuilding(Entity e){
		String number = e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
		String street = e.getTag(OSMTagKey.ADDR_STREET);
		if( street != null && number != null){
			Building building = registerStreet(street).registerBuilding(e);
			if (building.getPostcode() != null) {
				postcodes.add(building.getPostcode());
			}
			return streets.get(street.toLowerCase());
		}
		return null;
	}
	
	public CityType getType(){
		return type;
	}
	
	public SortedSet<String> getPostcodes() {
		return postcodes;
	}
	
	public Collection<Street> getStreets(){
		return streets.values();
	}
	
	public Street getStreet(String name){
		return streets.get(name.toLowerCase());
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
