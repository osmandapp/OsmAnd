package net.osmand.search.core.spatial;

/** POI subtypes that ranking and deduplication both have to name. Constants only, no behaviour. */
public class SpatialSearchTags {

	public static final String PUBLIC_TRANSPORT_PLATFORM = "public_transport_platform";
	public static final String PUBLIC_TRANSPORT_STOP = "public_transport_stop_position";
	public static final String SUBWAY_ENTRANCE = "subway_entrance";
	public static final String ENTRANCE = "entrance";
	public static final String ELEVATOR = "elevator";
	public static final String TICKET_VALIDATOR = "ticket_validator";
	public static final String LEVEL_CROSSING = "level_crossing";
	public static final String MOTORWAY_JUNCTION = "motorway_junction";
	public static final String BUS_STOP = "bus_stop";
	public static final String TRAM_STOP = "tram_stop";
	public static final String RAILWAY_HALT = "railway_halt";
	public static final String BRIDGE = "bridge";
	public static final String TUNNEL = "tunnel";
	public static final String VIADUCT = "viaduct";
	public static final String FORD = "ford";

	private SpatialSearchTags() {
	}
}
