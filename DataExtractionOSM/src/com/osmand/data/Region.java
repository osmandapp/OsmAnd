package com.osmand.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.osmand.Algoritms;
import com.osmand.data.City.CityType;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;
import com.osmand.osm.io.OsmBaseStorage;

public class Region {
	private Entity entity;
	
	private DataTileManager<Amenity> amenities = new DataTileManager<Amenity>();
	
	private String name;
	
	private OsmBaseStorage storage;
	
	private static class CityComparator implements Comparator<City>{
		@Override
		public int compare(City o1, City o2) {
			return o1.getName().compareTo(o2.getName());
		}
	} 
	
	private DataTileManager<City> cityManager = new DataTileManager<City>(); 
	private Map<CityType, List<City>> cities = new HashMap<CityType, List<City>>();
	{
		cityManager.setZoom(10);
		for(CityType type : CityType.values()){
			cities.put(type, new ArrayList<City>());
		}
	}
	
	
	public Region(Entity entity){
		this.entity = entity;
	}
	
	
	public OsmBaseStorage getStorage() {
		return storage;
	}
	
	public void setStorage(OsmBaseStorage storage) {
		this.storage = storage;
	}
	
	public void setEntity(Entity e){
		this.entity = e;
	}
	
	public String getName(){
		if(name != null){
			return name;
		}
		return entity == null ? "" : entity.getTag(OSMTagKey.NAME);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Collection<City> getCitiesByType(CityType type){
		return cities.get(type);
	}
	
	public int getCitiesCount(CityType type) {
		if (type == null) {
			int am = 0;
			for (CityType t : cities.keySet()) {
				am += cities.get(t).size();
			}
			return am;
		} else if (!cities.containsKey(type)) {
			return 0;
		} else {
			return cities.get(type).size();
		}

	}
	
	public Collection<City> getCitiesByName(String name){
		return getCityByName(name, true, Integer.MAX_VALUE);
	}
	
	public Collection<City> getSuggestedCities(String name, int number){
		return getCityByName(name, false, number);
	}
	
	protected Collection<City> getCityByName(String name, boolean exactMatch, int number){
		List<City> l = new ArrayList<City>();
		for(CityType type : CityType.values()){
			for(City c : cities.get(type)){
				if( (exactMatch && c.getName().equalsIgnoreCase(name)) || 
					(!exactMatch && c.getName().toLowerCase().startsWith(name.toLowerCase())
							)){
						l.add(c);
						if(l.size() >= number){
							break;
					}
				}
			}
		}
		return l;
	}
	
	public City getClosestCity(LatLon point) {
		City closest = null;
		double relDist = Double.POSITIVE_INFINITY;
		for (City c : cityManager.getClosestObjects(point.getLatitude(), point.getLongitude())) {
			double rel = MapUtils.getDistance(c.getEntity(), point) / c.getType().getRadius();
			if (rel < relDist) {
				closest = c;
				relDist = rel;
				if(relDist < 0.2d){
					break;
				}
			}
		}
		return closest;
	}
	
	public List<Amenity> getClosestAmenities(double latitude, double longitude){
		return amenities.getClosestObjects(latitude, longitude);
	}
	
	public DataTileManager<Amenity> getAmenityManager(){
		return amenities;
	}
	
	public void registerAmenity(Amenity a){
		LatLon location = a.getLocation();
		amenities.registerObject(location.getLatitude(), location.getLongitude(), a);
	}

	
	public City registerCity(Node c){
		City city = new City(c);
		if(city.getType() != null && !Algoritms.isEmpty(city.getName())){
			cityManager.registerObject(c.getLatitude(), c.getLongitude(), city);
			cities.get(city.getType()).add(city);
			return city;
		}
		return null;
	}
	
	
	public void doDataPreparation(){
		CityComparator comp = new CityComparator();
		for(CityType t : cities.keySet()){
			Collections.sort(cities.get(t), comp);
			for(City c : cities.get(t)){
				c.doDataPreparation();
			}
		}
		
		
	}

}
