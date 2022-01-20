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
	GPX_TRACK(false), ROUTE(false), MAP_MARKER(true), INDEX_ITEM(false),

	// ONLINE SEARCH
	ONLINE_SEARCH(true),

	REGION(true),

	SEARCH_STARTED(false),
	FILTER_FINISHED(false),
	SEARCH_FINISHED(false),
	SEARCH_API_FINISHED(false),
	SEARCH_API_REGION_FINISHED(false),
	UNKNOWN_NAME_FILTER(false);

	private final boolean hasLocation;

	ObjectType(boolean location) {
		this.hasLocation = location;
	}

	public boolean hasLocation() {
		return hasLocation;
	}

	public static boolean isAddress(ObjectType t) {
		return t == CITY || t == VILLAGE || t == POSTCODE || t == STREET || t == HOUSE || t == STREET_INTERSECTION;
	}

	public static boolean isTopVisible(ObjectType t) {
		return t == POI_TYPE || t == FAVORITE || t == FAVORITE_GROUP || t == WPT || t == GPX_TRACK || t == LOCATION || t == PARTIAL_LOCATION || t == INDEX_ITEM;
	}

	public static ObjectType getExclusiveSearchType(ObjectType t) {
		if (t == FAVORITE_GROUP) {
			return FAVORITE;
		}
		return null;
	}

	public static int getTypeWeight(ObjectType t) {
		if (t == null) {
			return 1;
		}
		switch (t) {
			case HOUSE:
			case STREET_INTERSECTION:
				return 4;
			case STREET:
				return 3;
			case CITY:
			case VILLAGE:
			case POSTCODE:
				return 2;
			case POI:
				return 1;
			default:
				return 1;
		}
	}
}
