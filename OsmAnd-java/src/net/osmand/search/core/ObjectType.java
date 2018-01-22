package net.osmand.search.core;

public enum ObjectType {
	// ADDRESS
	CITY(true), VILLAGE(true), POSTCODE(true), STREET(true), HOUSE(true), STREET_INTERSECTION(true),
	// POI
	POI_TYPE(false), POI(true),
	// LOCATION
	LOCATION(true), PARTIAL_LOCATION(false),
	// UI OBJECTS
	FAVORITE(true), FAVORITE_GROUP(false), WPT(true), RECENT_OBJ(true),

	// ONLINE SEARCH
	ONLINE_SEARCH(true),
	
	REGION(true),

	SEARCH_STARTED(false),
	SEARCH_FINISHED(false),
	SEARCH_API_FINISHED(false),
	SEARCH_API_REGION_FINISHED(false),
	UNKNOWN_NAME_FILTER(false);

	private boolean hasLocation;
	private ObjectType(boolean location) {
		this.hasLocation = location;
	}
	public boolean hasLocation() {
		return hasLocation;
	}
	
	public static boolean isAddress(ObjectType t) {
		return t == CITY || t == VILLAGE || t == POSTCODE || t == STREET || t == HOUSE || t == STREET_INTERSECTION;
	}

	public static ObjectType getExclusiveSearchType(ObjectType t) {
		if (t == FAVORITE_GROUP) {
			return FAVORITE;
		}
		return null;
	}
}
