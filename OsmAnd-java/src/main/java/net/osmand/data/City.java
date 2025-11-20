package net.osmand.data;

import org.json.JSONArray;
import org.json.JSONObject;

import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.util.Algorithms;

import java.util.*;


public class City extends MapObject {
	public enum CityType {
		// that's tricky way to play with that numbers (to avoid including suburbs in city & vice verse)
		CITY(10000, 100000), // 0. City
		TOWN(4000, 20000), // 1. Town
		VILLAGE(1300, 1000), // 2. Village 
		HAMLET(1000, 100), // 3. Hamlet - Small village
		SUBURB(1500, 5000), // 4. Mostly district of the city (introduced to avoid duplicate streets in city) - 
						   // however BOROUGH, DISTRICT, NEIGHBOURHOOD could be used as well for that purpose
						   // Main difference stores own streets to search and list by it  
		// 5.2 stored in city / villages sections written as city type
		BOUNDARY(0, 0), // 5. boundary no streets
		// 5.3 stored in city / villages sections written as city type
		POSTCODE(500, 1000), // 6. write this could be activated after 5.2 release
		
		// not stored entities but registered to uniquely identify streets as SUBURB
		BOROUGH(2000, 2500),  
		DISTRICT(1000, 10000),
		NEIGHBOURHOOD(500, 500),
		CENSUS(2000, 2500),
		;
		
		private final double radius;
		private final int population;
		
		CityType(double radius, int population) {
			this.radius = radius;
			this.population = population;
		}
		
		public double getRadius() {
			return radius;
		}
		
		public int getPopulation() {
			return population;
		}
		
		public boolean storedAsSeparateAdminEntity() {
			if (this == CITY || this == TOWN || this == VILLAGE || 
					this == HAMLET || this == SUBURB) {
				return true;
			}
			return false;
//			return this != DISTRICT && this != NEIGHBOURHOOD && this != BOROUGH 
//					&& this != BOUNDARY && this != POSTCODE;
		}

		public static String valueToString(CityType t) {
			return t.toString().toLowerCase();
		}

		public static CityType valueFromEntity(Entity e) {
			String place = e.getTag(OSMTagKey.PLACE);
			if ("locality".equals(place) && "townland".equals(e.getTag(OSMTagKey.LOCALITY))) {
				// Irish townlands are very similar to suburb 
				// however they could be separate polygons not inside town or city  
				return CityType.SUBURB;
			}
			return valueFromString(place);
		}
		
		// to be used only by amenity
		public static CityType valueFromString(String place) {
			if (place == null) {
				return null;
			}
			if ("township".equals(place)) {
				return CityType.TOWN;
			}
			for (CityType t : CityType.values()) {
				if (t.name().equalsIgnoreCase(place) 
						&& t != BOUNDARY && t != POSTCODE) {
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
	private int[] bbox31 = null;
	
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
		this.type = CityType.POSTCODE;
		this.name = this.enName = postcode;
		this.id = id;
	}

	public boolean isInCityByName(String name) {
		if(isin == null) {
			return false;
		}
		return isin.contains(name.toLowerCase());
	}
	
	public int[] getBbox31() {
		return bbox31;
	}
	
	public void setBbox31(int[] bbox31) {
		this.bbox31 = bbox31;
	}
	
	public boolean isPostcode(){
		return type == CityType.POSTCODE;
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
	

	// Be attentive ! Working with street names ignoring case
	private Set<String> isin = null;
	
	
	public Set<String> getIsin() {
		return isin;
	}
		
	public void setIsin(String val) {
		this.isin = new TreeSet<String>();
		String[] vls = val.toLowerCase().split(",");
		for (String v1 : vls) {
			String[] v2s = v1.trim().split(";");
			for (String v2 : v2s) {
				v2 = v2.trim();
				if (!Algorithms.isEmpty(v2)) {
					this.isin.add(v2);
				}
			}
		}
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
		if (bbox31 != null) {
			json.put("bbox31", Arrays.toString(bbox31));
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
		if (json.has("bbox31")) {
			Object bboxValue = json.get("bbox31");
			int[] parsedBbox = null;
			if (bboxValue instanceof JSONArray bboxArray) {
				if (bboxArray.length() >= 4) {
					int[] buffer = new int[4];
					boolean valid = true;
					for (int i = 0; i < buffer.length; i++) {
						Object value = bboxArray.get(i);
						if (value instanceof Number) {
							buffer[i] = ((Number) value).intValue();
						} else {
							valid = false;
							break;
						}
					}
					if (valid) {
						parsedBbox = buffer;
					}
				}
			} else if (bboxValue instanceof String) {
				String bboxString = ((String) bboxValue).trim();
				if (!bboxString.isEmpty()) {
					String normalized = bboxString;
					if (normalized.startsWith("[")) {
						normalized = normalized.substring(1);
					}
					if (normalized.endsWith("]")) {
						normalized = normalized.substring(0, normalized.length() - 1);
					}
					String[] parts = normalized.split(",");
					if (parts.length >= 4) {
						int[] buffer = new int[4];
						boolean valid = true;
						for (int i = 0; i < buffer.length; i++) {
							String part = parts[i].trim();
							if (Algorithms.isEmpty(part)) {
								valid = false;
								break;
							}
							try {
								buffer[i] = Integer.parseInt(part);
							} catch (NumberFormatException ex) {
								valid = false;
								break;
							}
						}
						if (valid) {
							parsedBbox = buffer;
						}
					}
				}
			}
			if (parsedBbox != null) {
				c.bbox31 = parsedBbox;
			}
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
