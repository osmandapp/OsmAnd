package com.osmand.data;

import java.util.LinkedHashMap;
import java.util.Map;

import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class Amenity {
	// http://wiki.openstreetmap.org/wiki/Amenity
	public enum AmenityType {
		SUSTENANCE, // restaurant, cafe ...
		EDUCATION, // school, ...
		TRANSPORTATION, // car_wash, parking, ...
		FINANCE, // bank, atm, ...
		HEALTHCARE, // hospital ...
		ENTERTAINMENT, // cinema, ... (+! sauna, brothel)
		TOURISM, // hotel, sights, museum ..
		SHOP, // convenience (product), clothes...
		LEISURE, // sport
		OTHER, // grave-yard, police, post-office
	}
	private static Map<String, AmenityType> prebuiltMap = new LinkedHashMap<String, AmenityType>();
	static {
		prebuiltMap.put("restaurant", AmenityType.SUSTENANCE);
		prebuiltMap.put("food_court", AmenityType.SUSTENANCE);
		prebuiltMap.put("fast_food", AmenityType.SUSTENANCE);
		prebuiltMap.put("drinking_water", AmenityType.SUSTENANCE);
		prebuiltMap.put("bbq", AmenityType.SUSTENANCE);
		prebuiltMap.put("pub", AmenityType.SUSTENANCE);
		prebuiltMap.put("bar", AmenityType.SUSTENANCE);
		prebuiltMap.put("cafe", AmenityType.SUSTENANCE);
		prebuiltMap.put("biergarten", AmenityType.SUSTENANCE);
		
		prebuiltMap.put("kindergarten", AmenityType.EDUCATION);
		prebuiltMap.put("school", AmenityType.EDUCATION);
		prebuiltMap.put("college", AmenityType.EDUCATION);
		prebuiltMap.put("library", AmenityType.EDUCATION);
		prebuiltMap.put("university", AmenityType.EDUCATION);

		prebuiltMap.put("ferry_terminal", AmenityType.TRANSPORTATION);
		prebuiltMap.put("bicycle_parking", AmenityType.TRANSPORTATION);
		prebuiltMap.put("bicycle_rental", AmenityType.TRANSPORTATION);
		prebuiltMap.put("bus_station", AmenityType.TRANSPORTATION);
		prebuiltMap.put("car_rental", AmenityType.TRANSPORTATION);
		prebuiltMap.put("car_sharing", AmenityType.TRANSPORTATION);
		prebuiltMap.put("car_wash", AmenityType.TRANSPORTATION);
		prebuiltMap.put("grit_bin", AmenityType.TRANSPORTATION);
		prebuiltMap.put("parking", AmenityType.TRANSPORTATION);
		prebuiltMap.put("taxi", AmenityType.TRANSPORTATION);
		
		prebuiltMap.put("atm", AmenityType.FINANCE);
		prebuiltMap.put("bank", AmenityType.FINANCE);
		prebuiltMap.put("bureau_de_change", AmenityType.FINANCE);
		
		prebuiltMap.put("pharmacy", AmenityType.HEALTHCARE);
		prebuiltMap.put("hospital", AmenityType.HEALTHCARE);
		prebuiltMap.put("baby_hatch", AmenityType.HEALTHCARE);
		prebuiltMap.put("dentist", AmenityType.HEALTHCARE);
		prebuiltMap.put("doctors", AmenityType.HEALTHCARE);
		prebuiltMap.put("veterinary", AmenityType.HEALTHCARE);
		prebuiltMap.put("first_aid", AmenityType.HEALTHCARE);
		
		prebuiltMap.put("architect_office", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("arts_centre", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("cinema", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("community_centre", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("fountain", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("nightclub", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("stripclub", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("studio", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("theatre", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("sauna", AmenityType.ENTERTAINMENT);
		prebuiltMap.put("brothel", AmenityType.ENTERTAINMENT);
	}
	
	
	
	private final Node node;

	public Amenity(Node node){
		this.node = node;
	}
	
	public Node getNode() {
		return node;
	}
	
	public String getSubType(){
		if(node.getTag(OSMTagKey.AMENITY) != null){
			return node.getTag(OSMTagKey.AMENITY);
		} else if(node.getTag(OSMTagKey.SHOP) != null){
			return node.getTag(OSMTagKey.SHOP);
		} else if(node.getTag(OSMTagKey.TOURISM) != null){
			return node.getTag(OSMTagKey.TOURISM);
		} else if(node.getTag(OSMTagKey.LEISURE) != null){
			return node.getTag(OSMTagKey.LEISURE);
		} 
		return "";
	}
	
	public AmenityType getType(){
		if(node.getTag(OSMTagKey.SHOP) != null){
			return AmenityType.SHOP;
		} else if(node.getTag(OSMTagKey.TOURISM) != null){
			return AmenityType.TOURISM;
		} else if(node.getTag(OSMTagKey.LEISURE) != null){
			return AmenityType.LEISURE;
		} else if(prebuiltMap.containsKey(node.getTag(OSMTagKey.AMENITY))){
				return prebuiltMap.get(node.getTag(OSMTagKey.AMENITY));
		}
		return AmenityType.OTHER;
	}
	
	public static boolean isAmenity(Node n){
		if(n.getTag(OSMTagKey.AMENITY) != null){
			return true;
		} else if(n.getTag(OSMTagKey.SHOP) != null){
			return true;
		} else if(n.getTag(OSMTagKey.LEISURE) != null){
			return true;
		} else if(n.getTag(OSMTagKey.TOURISM) != null){
			return true;
		}
		return false;
	}

	public String getSimpleFormat(){
		String name = node.getTag(OSMTagKey.NAME);
		return getType().toString() +" : " + getSubType() + " " +(name == null ? node.getId() : name);
	}
	
	@Override
	public String toString() {
		return getSimpleFormat();
	}
	
	
	

}
