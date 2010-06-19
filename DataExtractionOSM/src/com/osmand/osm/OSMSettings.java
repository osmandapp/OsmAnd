package com.osmand.osm;

public class OSMSettings {
	
	public enum OSMTagKey {
		NAME("name"), //$NON-NLS-1$
		NAME_EN("name:en"), //$NON-NLS-1$
		// ways
		HIGHWAY("highway"), //$NON-NLS-1$
		BUILDING("building"), //$NON-NLS-1$
		// transport
		ROUTE("route"), //$NON-NLS-1$
		OPERATOR("operator"), //$NON-NLS-1$
		REF("ref"), //$NON-NLS-1$
		
		// address
		PLACE("place"), //$NON-NLS-1$
		ADDR_HOUSE_NUMBER("addr:housenumber"), //$NON-NLS-1$
		ADDR_STREET("addr:street"), //$NON-NLS-1$
		ADDR_POSTCODE("addr:postcode"), //$NON-NLS-1$
		
		// POI
		AMENITY("amenity"), //$NON-NLS-1$
		SHOP("shop"), //$NON-NLS-1$
		LEISURE("leisure"),  //$NON-NLS-1$
		TOURISM("tourism"), //$NON-NLS-1$
		SPORT("sport"),  //$NON-NLS-1$
		HISTORIC("historic"), //$NON-NLS-1$
		NATURAL("natural"), //$NON-NLS-1$
		INTERNET_ACCESS("internet_access"), //$NON-NLS-1$
		
		OPENING_HOURS("opening_hours"), //$NON-NLS-1$
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
			String[] cars = new String[]{"trunk", "motorway", "primary", "secondary", "tertiary", "service", "residential", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
										"trunk_link", "motorway_link", "primary_link", "secondary_link", "residential_link",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
										"tertiary_link", "track" }; //$NON-NLS-1$ //$NON-NLS-2$
			for(String c : cars){
				if(c.equals(tagHighway)){
					return true;
				}
			}
		}
		return false;
	}
}
