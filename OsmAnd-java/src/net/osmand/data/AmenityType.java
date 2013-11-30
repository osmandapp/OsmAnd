package net.osmand.data;

import java.util.LinkedHashMap;
import java.util.Map;

// http://wiki.openstreetmap.org/wiki/Amenity
// POI tags : amenity, leisure, shop, sport, tourism, historic; accessories (internet-access), natural ?
public class AmenityType {
	// Some of those types are subtypes of Amenity tag 

	static Map<String, AmenityType> amenityTypes = new LinkedHashMap<String, AmenityType>();
	private static AmenityType reg(String name, String defaultTag) {
		name = name.toLowerCase();
		if(amenityTypes.containsKey(name)) {
			return amenityTypes.get(name);
		}
		AmenityType t = new AmenityType(name, defaultTag, amenityTypes.size());
		amenityTypes.put(t.name, t);
		return t;
	}
	public static AmenityType EMERGENCY = reg("emergency", "emergency"); // [TAG] emergency services //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType HEALTHCARE = reg("healthcare", "amenity"); // hospitals, doctors, ... //$NON-NLS-1$ //$NON-NLS-2$

	public static AmenityType TRANSPORTATION = reg("transportation", "amenity"); // trffic-stuff, parking, public transportation, ... //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType BARRIER = reg("barrier", "barrier"); // [TAG] barrier + traffic_calming //$NON-NLS-1$ //$NON-NLS-2$

	public static AmenityType TOURISM = reg("tourism", "tourism"); // [TAG] tourism hotel, sights, museum .. //$NON-NLS-1$ //$NON-NLS-2$ 
	public static AmenityType ENTERTAINMENT = reg("entertainment", "amenity"); // cinema, ...  = reg("", +! sauna, brothel) //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType HISTORIC = reg("historic", "historic"); // [TAG] historic places, battlefields, ... //$NON-NLS-1$ //$NON-NLS-2$

	public static AmenityType SPORT = reg("sport", "sport"); // [TAG] sport //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType LEISURE = reg("leisure", "leisure"); // [TAG] leisure //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType GEOCACHE = reg("geocache", "geocache");  //$NON-NLS-1$

	public static AmenityType OTHER = reg("other", "amenity"); // grave-yard, post-office, [+Internet_access] //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType FINANCE = reg("finance", "amenity"); // bank, atm, ... //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType OFFICE = reg("office", "office"); // [TAG] office //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType ADMINISTRATIVE = reg("administrative", "administrative"); // [TAG] administrative //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType EDUCATION = reg("education", "amenity"); // school, ... //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType MAN_MADE = reg("man_made", "man_made"); // [TAG] man_made and others //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType SEAMARK = reg("seamark", "seamark"); // [TAG] seamark //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType SUSTENANCE = reg("sustenance", "amenity"); // restaurant, cafe, ... //$NON-NLS-1$ //$NON-NLS-2$

	public static AmenityType SHOP = reg("shop", "shop"); // [TAG] amenity convenience  = reg("", product); clothes,... //$NON-NLS-1$ //$NON-NLS-2$

	public static AmenityType NATURAL = reg("natural", "natural"); // [TAG] natural places, peaks, caves, trees,... //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType LANDUSE = reg("landuse", "landuse"); // [TAG] landuse //$NON-NLS-1$ //$NON-NLS-2$
	public static AmenityType MILITARY = reg("military", "military"); // [TAG] military //$NON-NLS-1$ //$NON-NLS-2$

	public static AmenityType OSMWIKI = reg("osmwiki", "osmwiki");  //$NON-NLS-1$
	public static AmenityType USER_DEFINED = reg("user_defined", "user_defined");  //$NON-NLS-1$
	
	
	private final String defaultTag;
	private final String name;
	private final int ordinal;
	
	private AmenityType(String name, String defaultTag, int ordinal) {
		this.name = name;
		this.defaultTag = defaultTag;
		this.ordinal = ordinal;	
	}
	
	public static AmenityType findOrCreateTypeNoReg(String s) {
		AmenityType type = null;
		for (AmenityType t : amenityTypes.values()) {
			if (t.name.equalsIgnoreCase(s)) {
				type = t;
				break;
			}
		}
		if(type == null) {
			type = new AmenityType(s, s, -1);
		}
		return type;
	}
	
	public static boolean isRegisteredType(AmenityType type) {
		//return amenityTypes.containsKey(type.name);
		return type.ordinal >= 0;
	}
	
	public static AmenityType getAndRegisterType(String name) {
		return reg(name, name);
	}
	
	public String getDefaultTag() {
		return defaultTag;
	}
	
	public String getCategoryName() {
		return name;
	}
	
	
	public static int getCategoriesSize() {
		return amenityTypes.size();
	}
	public static AmenityType[] getCategories(){
		return amenityTypes.values().toArray(new AmenityType[amenityTypes.size()]);
	}

	public int ordinal() {
		return ordinal;
	}

	public static String valueToString(AmenityType a) {
		return a.getCategoryName();
	}
	
	@Override
	public String toString() {
		return valueToString(this);
	}
	
	
}