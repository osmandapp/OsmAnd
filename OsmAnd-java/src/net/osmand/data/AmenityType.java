package net.osmand.data;

// http://wiki.openstreetmap.org/wiki/Amenity
// POI tags : amenity, leisure, shop, sport, tourism, historic; accessories (internet-access), natural ?
public enum AmenityType {
	// Some of those types are subtypes of Amenity tag 
	SUSTENANCE("amenity"), // restaurant, cafe ... //$NON-NLS-1$ //$NON-NLS-2$
	EDUCATION("amenity"), // school, ... //$NON-NLS-1$ //$NON-NLS-2$
	TRANSPORTATION("amenity"), // car_wash, parking, ... //$NON-NLS-1$ //$NON-NLS-2$
	FINANCE("amenity"), // bank, atm, ... //$NON-NLS-1$ //$NON-NLS-2$
	HEALTHCARE("amenity"), // hospital ... //$NON-NLS-1$ //$NON-NLS-2$
	ENTERTAINMENT("amenity"), // cinema, ... (+! sauna, brothel) //$NON-NLS-1$ //$NON-NLS-2$
	TOURISM("tourism"), // [TAG] tourism hotel, sights, museum .. //$NON-NLS-1$ //$NON-NLS-2$ 
	HISTORIC("historic"), // [TAG] historic places, monuments (should we unify tourism/historic) //$NON-NLS-1$ //$NON-NLS-2$
	NATURAL("natural"), // [TAG] natural places, monuments (should we unify tourism/historic) //$NON-NLS-1$ //$NON-NLS-2$
	SHOP("shop"), // [TAG] amenity convenience (product), clothes... //$NON-NLS-1$ //$NON-NLS-2$
	LEISURE("leisure"), // [TAG] leisure //$NON-NLS-1$ //$NON-NLS-2$
	SPORT("sport"), // [TAG] sport //$NON-NLS-1$ //$NON-NLS-2$
	BARRIER("barrier"), // [TAG] barrier + traffic_calming //$NON-NLS-1$ //$NON-NLS-2$
	LANDUSE("landuse"), // [TAG] landuse //$NON-NLS-1$ //$NON-NLS-2$
	MAN_MADE("man_made"), // [TAG] man_made and others //$NON-NLS-1$ //$NON-NLS-2$
	OFFICE("office"), // [TAG] office //$NON-NLS-1$ //$NON-NLS-2$
	EMERGENCY("emergency"), // [TAG] emergency //$NON-NLS-1$ //$NON-NLS-2$
	MILITARY("military"), // [TAG] military //$NON-NLS-1$ //$NON-NLS-2$
	ADMINISTRATIVE("administrative"), // [TAG] administrative //$NON-NLS-1$ //$NON-NLS-2$
	GEOCACHE("geocache"),  //$NON-NLS-1$
	OSMWIKI("osmwiki"),  //$NON-NLS-1$
	USER_DEFINED("user_defined"),  //$NON-NLS-1$
	OTHER("amenity"), // grave-yard, police, post-office [+Internet_access] //$NON-NLS-1$ //$NON-NLS-2$
	;
	
	private final String defaultTag;
	
	private AmenityType(String defaultTag) {
		this.defaultTag = defaultTag;	
	}
	
	public static AmenityType fromString(String s){
		try {
			return AmenityType.valueOf(s.toUpperCase());
		} catch (IllegalArgumentException e) {
			return AmenityType.OTHER;
		}
	}
	
	public String getDefaultTag() {
		return defaultTag;
	}
	
	public static String valueToString(AmenityType t){
		return t.toString().toLowerCase();
	}
	
	public static AmenityType[] getCategories(){
		return AmenityType.values();
	}
	
	
}