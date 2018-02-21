package net.osmand.plus.transport;

import net.osmand.plus.R;

public enum TransportStopType {
	BUS(R.drawable.mx_route_bus_ref, R.drawable.mx_route_bus_ref, "routeBusColor", R.drawable.mm_route_bus_ref),
	FERRY(R.drawable.mx_route_ferry_ref, R.drawable.mx_route_ferry_ref, "routeFerryColor", R.drawable.mm_route_ferry_ref),
	FUNICULAR(R.drawable.mx_route_funicular_ref, R.drawable.mx_route_funicular_ref, "routeFunicularColor", R.drawable.mm_route_funicular_ref),
	LIGHT_RAIL(R.drawable.mx_route_light_rail_ref, R.drawable.mx_route_light_rail_ref, "routeLightrailColor", R.drawable.mm_route_light_rail_ref),
	MONORAIL(R.drawable.mx_route_monorail_ref, R.drawable.mx_route_monorail_ref, "routeLightrailColor", R.drawable.mm_route_monorail_ref),
	RAILWAY(R.drawable.mx_route_railway_ref, R.drawable.mx_route_railway_ref, "routeTrainColor", R.drawable.mm_route_railway_ref),
	SHARE_TAXI(R.drawable.mx_route_share_taxi_ref, R.drawable.mx_route_share_taxi_ref, "routeShareTaxiColor", R.drawable.mm_route_share_taxi_ref),
	TRAIN(R.drawable.mx_route_train_ref, R.drawable.mx_route_train_ref, "routeTrainColor", R.drawable.mm_route_train_ref),
	TRAM(R.drawable.mx_route_tram_ref, R.drawable.mx_railway_tram_stop, "routeTramColor", R.drawable.mx_route_tram_ref),
	TROLLEYBUS(R.drawable.mx_route_trolleybus_ref, R.drawable.mx_route_trolleybus_ref, "routeTrolleybusColor", R.drawable.mm_route_trolleybus_ref),
	SUBWAY(R.drawable.mx_subway_station, R.drawable.mx_subway_station, "routeTrainColor", R.drawable.mm_subway_station);

	final int resId;
	final int topResId;
	final int smallResId;
	final String renderAttr;

	TransportStopType(int resId, int topResId, String renderAttr, int smallResId) {
		this.resId = resId;
		this.topResId = topResId;
		this.renderAttr = renderAttr;
		this.smallResId = smallResId;
	}

	public int getResourceId() {
		return resId;
	}

	public int getTopResourceId() {
		return topResId;
	}

	public int getSmallResId() {
		return smallResId;
	}

	public String getRendeAttr() {
		return renderAttr;
	}

	public boolean isTopType() {
		return this == TRAM || this == SUBWAY;
	}

	public static TransportStopType findType(String typeName) {
		String tName = typeName.toUpperCase();
		for (TransportStopType t : values()) {
			if (t.name().equals(tName)) {
				return t;
			}
		}
		return null;
	}

}
