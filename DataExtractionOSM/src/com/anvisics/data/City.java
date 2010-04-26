package com.anvisics.data;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import com.anvisics.NodeUtil;
import com.anvisics.NodeUtil.LatLon;

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
		String place = NodeUtil.getTag(el, "place");
		for(CityType t : CityType.values()){
			if(t.name().equalsIgnoreCase(place)){
				type = t;
				break;
			}
		}
	}
	
	public Street registerBuilding(LatLon point, Entity e){
		String number = NodeUtil.getTag(e, "addr:housenumber");
		String street = NodeUtil.getTag(e, "addr:street");
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
		return NodeUtil.getTag(el, "name");
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
