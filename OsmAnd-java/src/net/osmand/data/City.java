package net.osmand.data;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;

public class City extends MapObject {
	public enum CityType {
		// that's tricky way to play with that numbers (to avoid including suburbs in city & vice verse)
		// district special type and it is not registered as a city
		CITY(10000), TOWN(5000), VILLAGE(1300), HAMLET(1000), SUBURB(400), DISTRICT(400);

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
	private Map<String, Street> streets = new TreeMap<String, Street>(PlatformUtil.primaryCollator());
	private String isin = null;
	private String postcode = null;

	private static long POSTCODE_INTERNAL_ID = -1000;
	public static City createPostcode(String postcode){
		return new City(postcode, POSTCODE_INTERNAL_ID--);
	}

	public City(CityType type) {
		if(type == null) {
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

	public boolean isEmptyWithStreets() {
		return streets.isEmpty();
	}


	public Street unregisterStreet(String name) {
		return streets.remove(name.toLowerCase());
	}

	public void removeAllStreets() {
		streets.clear();
	}

	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	protected Street registerStreet(Street street, boolean en) {
		String name = en ? street.getEnName() : street.getName();
		name = name.toLowerCase();
		if (!Algorithms.isEmpty(name)) {
			if (!streets.containsKey(name)) {
				return streets.put(name, street);
			} else {
				// try to merge streets
				Street prev = streets.get(name);
				prev.mergeWith(street);
				return prev;
			}
		}
		return null;
	}

	public Street registerStreet(Street street) {
		return registerStreet(street, false);
	}

	public CityType getType() {
		return type;
	}

	public Collection<Street> getStreets() {
		return streets.values();
	}

	public Street getStreet(String name) {
		return streets.get(name.toLowerCase());
	}

	@Override
	public String toString() {
		if(isPostcode()) {
			return "Postcode : " + getName(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return "City [" + type + "] " + getName(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void setIsin(String isin) {
		this.isin = isin;
	}

}
