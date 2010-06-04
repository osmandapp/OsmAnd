package com.osmand.osm;

public class OSMSettings {
	
	public enum OSMTagKey {
		NAME("name"),
		NAME_EN("name:en"),
		// ways
		HIGHWAY("highway"),
		BUILDING("building"),
		
		// address
		PLACE("place"),
		ADDR_HOUSE_NUMBER("addr:housenumber"),
		ADDR_STREET("addr:street"),
		
		// POI
		AMENITY("amenity"),
		SHOP("shop"),
		LEISURE("leisure"), 
		TOURISM("tourism"),
		OPENING_HOURS("opening_hours"),
		;
		
		private final String value;
		private OSMTagKey(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	public enum OSMHighwayTypes {
		TRUNK, MOTORWAY, PRIMARY, SECONDARY, RESIDENTIAL, TERTIARY, SERVICE, TRACK,
		
		// TODO is link needed?
		TRUNK_LINK, MOTORWAY_LINK, PRIMARY_LINK, SECONDARY_LINK, RESIDENTIAL_LINK, TERTIARY_LINK, SERVICE_LINK, TRACK_LINK, 
		
	}
	
	
	public static boolean wayForCar(String tagHighway){
		if(tagHighway != null){
			String[] cars = new String[]{"trunk", "motorway", "primary", "secondary", "tertiary", "service", "residential",
										"trunk_link", "motorway_link", "primary_link", "secondary_link", "residential_link", 
										"tertiary_link", "track" };
			for(String c : cars){
				if(c.equals(tagHighway)){
					return true;
				}
			}
		}
		return false;
	}
}
