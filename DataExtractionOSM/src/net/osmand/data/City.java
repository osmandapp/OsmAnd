package net.osmand.data;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.Algoritms;
import net.osmand.osm.Entity;
import net.osmand.osm.Node;
import net.osmand.osm.OSMSettings.OSMTagKey;


public class City extends MapObject {
	
	public enum CityType {
		// that's tricky way to play with that numbers (to avoid including suburbs in city & vice verse)
		CITY(10000), TOWN(5000), VILLAGE(1300), HAMLET(1000), SUBURB(400);

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
	private final Map<String, Street> streets; 
	private String isin = null;
	
	public City(Node el){
		super(el);
		Collator instance = Collator.getInstance();
		instance.setStrength(Collator.PRIMARY);
		streets = new TreeMap<String, Street>(instance);
		type = CityType.valueFromString(el.getTag(OSMTagKey.PLACE));
		isin = el.getTag(OSMTagKey.IS_IN);
		isin = isin != null ? isin.toLowerCase() : null;
	}
	
	public City(CityType type){
		Collator instance = Collator.getInstance();
		instance.setStrength(Collator.PRIMARY);
		streets = new TreeMap<String, Street>(instance);
		this.type = type;
	}
	
	public String getIsInValue() {
		return isin;
	}
	
	public boolean isEmptyWithStreets(){
		return streets.isEmpty();
	}
	
	public Street registerStreet(String street){
		if(!streets.containsKey(street)){
			streets.put(street, new Street(this, street));
		}
		return streets.get(street); 
	}
	
	public Street unregisterStreet(String name){
		return streets.remove(name); 
	}
	
	public void removeAllStreets(){
		streets.clear();
	}
	
	public Street registerStreet(Street street, boolean en){
		String name = en ? street.getEnName(): street.getName();
		if(!Algoritms.isEmpty(name)){
			if(!streets.containsKey(name)){
				return streets.put(name, street);
			} else {
				// try to merge streets
				Street prev = streets.get(name);
				if(!street.getWayNodes().isEmpty()){
					prev.getWayNodes().addAll(street.getWayNodes());
				}
				prev.getBuildings().addAll(street.getBuildings());
				return prev;
			}
		}
		return null;
	}
	
	public Street registerStreet(Street street){
		return registerStreet(street, false);
	}
	
	public Building registerBuilding(Entity e){
		String number = e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
		String street = e.getTag(OSMTagKey.ADDR_STREET);
		if( street != null && number != null){
			return registerStreet(street).registerBuilding(e);
		}
		return null;
	}
	
	public CityType getType(){
		return type;
	}
	
	public Collection<Street> getStreets(){
		return streets.values();
	}
	
	public Street getStreet(String name){
		return streets.get(name);
	}
	
	@Override
	public String toString() {
		return "City [" +type+"] " + getName(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void doDataPreparation(){
		for(Street s : new ArrayList<Street>(getStreets())){
			s.doDataPreparation();
		}
	}

}
