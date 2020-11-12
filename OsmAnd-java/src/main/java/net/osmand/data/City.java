package net.osmand.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class City extends MapObject {
	public enum CityType {
		// that's tricky way to play with that numbers (to avoid including suburbs in city & vice verse)
		// district special type and it is not registered as a city
		CITY(10000), TOWN(4000), VILLAGE(1300), HAMLET(1000), SUBURB(400), BOROUGH(400), DISTRICT(400), NEIGHBOURHOOD(300);

		private double radius;

		CityType(double radius) {
			this.radius = radius;
		}

		public double getRadius() {
			return radius;
		}
		
		public boolean storedAsSeparateAdminEntity() {
			return this != DISTRICT && this != NEIGHBOURHOOD && this != BOROUGH;
		}

		public static String valueToString(CityType t) {
			return t.toString().toLowerCase();
		}

		public static CityType valueFromString(String place) {
			if (place == null) {
				return null;
			}
			if ("township".equals(place)) {
				return CityType.TOWN;
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
	private List<Street> listOfStreets = new ArrayList<Street>();
	private String postcode = null;
	private City closestCity = null;
	
	private static long POSTCODE_INTERNAL_ID = -1000;
	public static City createPostcode(String postcode){
		return new City(postcode, POSTCODE_INTERNAL_ID--);
	}

	public City(CityType type) {
		if (type == null) {
			throw new NullPointerException();
		}
		this.type = type;
	}
	
	public City(String postcode, long id) {
		this.type = null;
		this.name = this.enName = postcode;
		this.id = id;
	}

	public String getIsInValue() {
		return isin;
	}
	
	public boolean isPostcode(){
		return type == null;
	}
	
	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}
	
	public City getClosestCity() {
		return closestCity;
	}
	
	public void setClosestCity(City closestCity) {
		this.closestCity = closestCity;
	}


	public void registerStreet(Street street) {
		listOfStreets.add(street);
	}
	
	public void unregisterStreet(Street candidate) {
		listOfStreets.remove(candidate);
	}

	public CityType getType() {
		return type;
	}

	public List<Street> getStreets() {
		return listOfStreets;
	}

	@Override
	public String toString() {
		if (isPostcode()) {
			return "Postcode : " + getName() + " " + getLocation(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return "City [" + type + "] " + getName() + " " + getLocation(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public Street getStreetByName(String name) {
		for (Street s : listOfStreets) {
			if (s.getName().equalsIgnoreCase(name)) {
				return s;
			}
		}
		return null;
	}
	

	// GENERATION
	// Be attentive ! Working with street names ignoring case
	private String isin = null;
	
		
	public void setIsin(String isin) {
		this.isin = isin;
	}

	public Map<Street, Street> mergeWith(City city) {
		Map<Street, Street> m = new LinkedHashMap<>();
		for (Street street : city.listOfStreets) {
			if (listOfStreets.contains(street)) {
				listOfStreets.get(listOfStreets.indexOf(street)).mergeWith(street);
			} else {
				Street s = new Street(this);
				s.copyNames(street);
				s.setLocation(street.getLocation().getLatitude(), street.getLocation().getLongitude());
				s.setId(street.getId());
				s.buildings.addAll(street.getBuildings());
				m.put(street, s);
				listOfStreets.add(s);
			}
		}
		copyNames(city);
		return m;
	}

	public JSONObject toJSON() {
		return toJSON(true);
	}

	public JSONObject toJSON(boolean includingBuildings) {
		JSONObject json = super.toJSON();
		if (type != null) {
			json.put("type", type.name());
		}
		if (postcode != null) {
			json.put("postcode", postcode);
		}
		JSONArray listOfStreetsArr = new JSONArray();
		for (Street s : listOfStreets) {
			listOfStreetsArr.put(s.toJSON(includingBuildings));
		}
		json.put("listOfStreets", listOfStreetsArr);

		return json;
	}

	public static City parseJSON(JSONObject json) throws IllegalArgumentException {
		CityType type;
		if (json.has("type")) {
			type = CityType.valueOf(json.getString("type"));
		} else {
			throw new IllegalArgumentException();
		}
		City c = new City(type);
		MapObject.parseJSON(json, c);

		if (json.has("postcode")) {
			c.postcode = json.getString("postcode");
		}
		if (json.has("listOfStreets")) {
			JSONArray streetsArr = json.getJSONArray("listOfStreets");
			c.listOfStreets = new ArrayList<>();
			for (int i = 0; i < streetsArr.length(); i++) {
				JSONObject streetObj = streetsArr.getJSONObject(i);
				Street street = Street.parseJSON(c, streetObj);
				if (street != null) {
					c.listOfStreets.add(street);
				}
			}
		}
		return c;
	}
}
