package net.osmand.data;

import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Street extends MapObject {

	protected List<Building> buildings = new ArrayList<Building>();
	protected Map<String, Building> buildingsByIdCache = null;
	protected List<Street> intersectedStreets = null;
	protected final City city;

	public Street(City city) {
		this.city = city;
	}

	public void addBuilding(Building building) {
		buildings.add(building);
	}

	public List<Street> getIntersectedStreets() {
		if (intersectedStreets == null) {
			return Collections.emptyList();
		}
		return intersectedStreets;
	}

	public void addIntersectedStreet(Street s) {
		if (intersectedStreets == null) {
			intersectedStreets = new ArrayList<Street>();
		}
		intersectedStreets.add(s);
	}

	public void addBuildingCheckById(Building building) {
		if (buildingsByIdCache == null) {
			buildingsByIdCache = new HashMap<String, Building>();
			for (Building b : buildings) {
				buildingsByIdCache.put(b.getId() + " " + b.getFullName(), b);
			}
		}
		String key = building.getId() + " " + building.getFullName();
		if (buildingsByIdCache.containsKey(key)) {
			return;
		}
		buildingsByIdCache.put(key, building);
		buildings.add(building);
	}

	public List<Building> getBuildings() {
		return buildings;
	}

	public City getCity() {
		return city;
	}

	public void sortBuildings() {
		Collections.sort(buildings, new Comparator<Building>() {
			@Override
			public int compare(Building o1, Building o2) {
				String s1 = o1.getName();
				String s2 = o2.getName();
				int i1 = Algorithms.extractFirstIntegerNumber(s1);
				int i2 = Algorithms.extractFirstIntegerNumber(s2);
				if (i1 == i2) {
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
		for (Building b : street.getBuildings()) {
			addBuildingCheckById(b);
		}
		copyNames(street);
	}

	public String getNameWithoutCityPart(String lang, boolean transliterate) {
		String nm = getName(lang, transliterate);
		int t = nm.lastIndexOf('(');
		if (t > 0) {
			return nm.substring(0, t);
		}
		return nm;
	}

	public JSONObject toJSON() {
		return toJSON(true);
	}

	public JSONObject toJSON(boolean includingBuildings) {
		JSONObject json = super.toJSON();
		if (buildings.size() > 0 && includingBuildings) {
			JSONArray buildingsArr = new JSONArray();
			for (Building b : buildings) {
				buildingsArr.put(b.toJSON());
			}
			json.put("buildings", buildingsArr);
		}
		if (intersectedStreets != null) {
			JSONArray intersectedStreetsArr = new JSONArray();
			for (Street s : intersectedStreets) {
				intersectedStreetsArr.put(s.toJSON());
			}
			json.put("intersectedStreets", intersectedStreetsArr);
		}

		return json;
	}

	public static Street parseJSON(City city, JSONObject json) throws IllegalArgumentException {
		Street s = new Street(city);
		MapObject.parseJSON(json, s);

		if (json.has("buildings")) {
			JSONArray buildingsArr = json.getJSONArray("buildings");
			s.buildings = new ArrayList<>();
			for (int i = 0; i < buildingsArr.length(); i++) {
				JSONObject buildingObj = buildingsArr.getJSONObject(i);
				Building building = Building.parseJSON(buildingObj);
				if (building != null) {
					s.buildings.add(building);
				}
			}
		}
		if (json.has("intersectedStreets")) {
			JSONArray streetsArr = json.getJSONArray("intersectedStreets");
			s.intersectedStreets = new ArrayList<>();
			for (int i = 0; i < streetsArr.length(); i++) {
				JSONObject streetObj = streetsArr.getJSONObject(i);
				Street street = parseJSON(city, streetObj);
				if (street != null) {
					s.intersectedStreets.add(street);
				}
			}
		}
		return s;
	}
}
