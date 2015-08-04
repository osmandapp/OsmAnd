package net.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.util.Algorithms;


public class Street extends MapObject {
	
	protected List<Building> buildings = new ArrayList<Building>(); 
	protected List<Street> intersectedStreets = null;
	protected final City city;

	public Street(City city) {
		this.city = city;
	}
	
	public void addBuilding(Building building){
		buildings.add(building);
	}
	
	public List<Street> getIntersectedStreets() {
		if(intersectedStreets == null){
			return Collections.emptyList();
		}
		return intersectedStreets;
	}
	
	public void addIntersectedStreet(Street s){
		if(intersectedStreets == null) {
			intersectedStreets = new ArrayList<Street>();
		}
		intersectedStreets.add(s);
	}
	
	public void addBuildingCheckById(Building building){
		for(Building b : buildings) {
			if(b.getId().longValue() == building.getId().longValue()){
				return;
			}
		}
		buildings.add(building);
	}
	
	public List<Building> getBuildings() {
		return buildings;
	}
	
	
	
	public City getCity() {
		return city;
	}
	
	public void sortBuildings(){
		Collections.sort(buildings, new Comparator<Building>(){
			@Override
			public int compare(Building o1, Building o2) {
				String s1 = o1.getName();
				String s2 = o2.getName();
				int i1 = Algorithms.extractFirstIntegerNumber(s1);
				int i2 = Algorithms.extractFirstIntegerNumber(s2);
				if(i1 == i2) {
					String t1 = Algorithms.extractIntegerSuffix(s1);
					String t2 = Algorithms.extractIntegerSuffix(s2);
					return t1.compareTo(t2);
				}
				return i1 - i2;
			}
		});
	}
	
	/// GENERATION
	
	
	public void mergeWith(Street street) {
		buildings.addAll(street.getBuildings());
		copyNames(street);
	}

	public String getNameWithoutCityPart(String lang) {
		String nm = getName(lang);
		int t = nm.lastIndexOf('(');
		if (t > 0) {
			return nm.substring(0, t);
		}
		return nm;
	}


}
