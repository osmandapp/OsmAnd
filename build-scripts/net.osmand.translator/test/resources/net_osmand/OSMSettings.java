package net.osmand.osm;

public class OSMSettings {
	
	public enum OSMTagKey {
		NAME("name"), //$NON-NLS-1$
		NAME_EN("name:en"), //$NON-NLS-1$
		
		// ways
		HIGHWAY("highway"), //$NON-NLS-1$
		BUILDING("building"), //$NON-NLS-1$
		BOUNDARY("boundary"), //$NON-NLS-1$
		POSTAL_CODE("postal_code"), //$NON-NLS-1$
		RAILWAY("railway"), //$NON-NLS-1$
		ONEWAY("oneway"), //$NON-NLS-1$
		LAYER("layer"), //$NON-NLS-1$
		BRIDGE("bridge"), //$NON-NLS-1$
		TUNNEL("tunnel"), //$NON-NLS-1$
		TOLL("toll"), //$NON-NLS-1$
		JUNCTION("junction"), //$NON-NLS-1$
		
		
		// transport
		ROUTE("route"), //$NON-NLS-1$
		ROUTE_MASTER("route_master"), //$NON-NLS-1$
		OPERATOR("operator"), //$NON-NLS-1$
		REF("ref"), //$NON-NLS-1$
		RCN_REF("rcn_ref"), //$NON-NLS-1$
		RWN_REF("rwn_ref"), //$NON-NLS-1$
		
		// address
		PLACE("place"), //$NON-NLS-1$
		ADDR_HOUSE_NUMBER("addr:housenumber"), //$NON-NLS-1$
		ADDR_STREET("addr:street"), //$NON-NLS-1$
		ADDR_STREET2("addr:street2"), //$NON-NLS-1$
		ADDR_CITY("addr:city"), //$NON-NLS-1$
		ADDR_POSTCODE("addr:postcode"), //$NON-NLS-1$
		ADDR_INTERPOLATION("addr:interpolation"), //$NON-NLS-1$
		ADDRESS_TYPE("address:type"), //$NON-NLS-1$
		ADDRESS_HOUSE("address:house"), //$NON-NLS-1$
		TYPE("type"), //$NON-NLS-1$
		IS_IN("is_in"), //$NON-NLS-1$
		
		// POI
		AMENITY("amenity"), //$NON-NLS-1$
		SHOP("shop"), //$NON-NLS-1$
		LANDUSE("landuse"),  //$NON-NLS-1$
		OFFICE("office"),  //$NON-NLS-1$
		EMERGENCY("emergency"),  //$NON-NLS-1$
		MILITARY("military"),  //$NON-NLS-1$
		ADMINISTRATIVE("administrative"),  //$NON-NLS-1$
		MAN_MADE("man_made"),  //$NON-NLS-1$
		BARRIER("barrier"),  //$NON-NLS-1$
		LEISURE("leisure"),  //$NON-NLS-1$
		TOURISM("tourism"), //$NON-NLS-1$
		SPORT("sport"),  //$NON-NLS-1$
		HISTORIC("historic"), //$NON-NLS-1$
		NATURAL("natural"), //$NON-NLS-1$
		INTERNET_ACCESS("internet_access"), //$NON-NLS-1$
		
		
		CONTACT_WEBSITE("contact:website"), //$NON-NLS-1$
		CONTACT_PHONE("contact:phone"), //$NON-NLS-1$
		
		OPENING_HOURS("opening_hours"),  //$NON-NLS-1$
		PHONE("phone"), //$NON-NLS-1$
		DESCRIPTION("description"), //$NON-NLS-1$
		WEBSITE("website"), //$NON-NLS-1$
		URL("url"), //$NON-NLS-1$
		WIKIPEDIA("wikipedia"), //$NON-NLS-1$
		
		ADMIN_LEVEL("admin_level") //$NON-NLS-1$
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
										"tertiary_link", "track", "unclassified" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			for(String c : cars){
				if(c.equals(tagHighway)){
					return true;
				}
			}
		}
		return false;
	}
}
