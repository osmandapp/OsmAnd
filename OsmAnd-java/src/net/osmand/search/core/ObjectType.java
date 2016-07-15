package net.osmand.search.core;

public enum ObjectType {
	// ADDRESS
	CITY(true), VILLAGE(true), POSTCODE(true), STREET(true), HOUSE(true),STREET_INTERSECTION(true),
	// POI
	POI_TYPE(false), POI(true),
	// LOCATION
	LOCATION(true), PARTIAL_LOCATION(false),
	// UI OBJECTS
	FAVORITE(true), WPT(true), RECENT_OBJ(true), 
	REGION(true), 
	SEARCH_API_FINISHED(false), UNKNOWN_NAME_FILTER(false);
	private boolean hasLocation;
	private ObjectType(boolean location) {
		this.hasLocation = location;
	}
	public boolean hasLocation() {
		return hasLocation;
	}
	
}
