package net.osmand.search.example.core;

public enum ObjectType {
	CITY(true), VILLAGE(true), POSTCODE(true), STREET(true), HOUSE(true),
	STREET_INTERSECTION(true), POI_TYPE(false), POI(true), LOCATION(true), PARTIAL_LOCATION(false), FAVORITE(true),
	REGION(true), RECENT_OBJ(true), WPT(true), UNKNOWN_NAME_FILTER(false);
	private boolean hasLocation;
	private ObjectType(boolean location) {
		this.hasLocation = location;
	}
	public boolean hasLocation() {
		return hasLocation;
	}
	
}
