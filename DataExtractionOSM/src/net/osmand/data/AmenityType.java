package net.osmand.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import net.osmand.Messages;
import net.osmand.osm.MapRenderingTypes;


// http://wiki.openstreetmap.org/wiki/Amenity
// POI tags : amenity, leisure, shop, sport, tourism, historic; accessories (internet-access), natural ?
public enum AmenityType {
	// Some of those types are subtypes of Amenity tag 
	SUSTENANCE("amenity_type_sustenance", "amenity"), // restaurant, cafe ... //$NON-NLS-1$ //$NON-NLS-2$
	EDUCATION("amenity_type_education", "amenity"), // school, ... //$NON-NLS-1$ //$NON-NLS-2$
	TRANSPORTATION("amenity_type_transportation", "amenity"), // car_wash, parking, ... //$NON-NLS-1$ //$NON-NLS-2$
	FINANCE("amenity_type_finance", "amenity"), // bank, atm, ... //$NON-NLS-1$ //$NON-NLS-2$
	HEALTHCARE("amenity_type_healthcare", "amenity"), // hospital ... //$NON-NLS-1$ //$NON-NLS-2$
	ENTERTAINMENT("amenity_type_entertainment", "amenity"), // cinema, ... (+! sauna, brothel) //$NON-NLS-1$ //$NON-NLS-2$
	TOURISM("amenity_type_tourism", "tourism"), // [TAG] tourism hotel, sights, museum .. //$NON-NLS-1$ //$NON-NLS-2$ 
	HISTORIC("amenity_type_historic", "historic"), // [TAG] historic places, monuments (should we unify tourism/historic) //$NON-NLS-1$ //$NON-NLS-2$
	NATURAL("amenity_type_natural", "natural"), // [TAG] natural places, monuments (should we unify tourism/historic) //$NON-NLS-1$ //$NON-NLS-2$
	SHOP("amenity_type_shop", "shop"), // [TAG] amenity convenience (product), clothes... //$NON-NLS-1$ //$NON-NLS-2$
	LEISURE("amenity_type_leisure", "leisure"), // [TAG] leisure //$NON-NLS-1$ //$NON-NLS-2$
	SPORT("amenity_type_sport", "sport"), // [TAG] sport //$NON-NLS-1$ //$NON-NLS-2$
	BARRIER("amenity_type_barrier", "barrier"), // [TAG] barrier + traffic_calming //$NON-NLS-1$ //$NON-NLS-2$
	LANDUSE("amenity_type_landuse", "landuse"), // [TAG] landuse //$NON-NLS-1$ //$NON-NLS-2$
	MAN_MADE("amenity_type_manmade", "man_made"), // [TAG] man_made and others //$NON-NLS-1$ //$NON-NLS-2$
	OFFICE("amenity_type_office", "office"), // [TAG] office //$NON-NLS-1$ //$NON-NLS-2$
	EMERGENCY("amenity_type_emergency", "emergency"), // [TAG] emergency //$NON-NLS-1$ //$NON-NLS-2$
	MILITARY("amenity_type_military", "military"), // [TAG] military //$NON-NLS-1$ //$NON-NLS-2$
	ADMINISTRATIVE("amenity_type_administrative", "administrative"), // [TAG] administrative //$NON-NLS-1$ //$NON-NLS-2$
	OTHER("amenity_type_other", "amenity"), // grave-yard, police, post-office [+Internet_access] //$NON-NLS-1$ //$NON-NLS-2$
	;
	
	private final String name;
	private final String defaultTag;
	
	private AmenityType(String name, String defaultTag) {
		this.name = name;
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
	
	public static Collection<String> getSubCategories(AmenityType t, MapRenderingTypes renderingTypes){
		Map<AmenityType, Map<String, String>> amenityTypeNameToTagVal = renderingTypes.getAmenityTypeNameToTagVal();
		if(!amenityTypeNameToTagVal.containsKey(t)){
			return Collections.emptyList(); 
		}
		return amenityTypeNameToTagVal.get(t).keySet();
	}
	
	
	public static String toPublicString(AmenityType t){
//		return Algoritms.capitalizeFirstLetterAndLowercase(t.toString().replace('_', ' '));
		return Messages.getMessage(t.name);
	}
	

	
}