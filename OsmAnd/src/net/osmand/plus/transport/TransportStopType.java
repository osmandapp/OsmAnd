package net.osmand.plus.transport;

import net.osmand.plus.R;

public enum TransportStopType {
	BUS(R.drawable.mx_route_bus_ref, R.drawable.mx_route_bus_ref, "routeBusColor"),
	FERRY(R.drawable.mx_route_ferry_ref, R.drawable.mx_route_ferry_ref, "routeFerryColor"),
	FUNICULAR(R.drawable.mx_route_funicular_ref, R.drawable.mx_route_funicular_ref, "routeFunicularColor"),
	LIGHT_RAIL(R.drawable.mx_route_light_rail_ref, R.drawable.mx_route_light_rail_ref, "routeLightrailColor"),
	MONORAIL(R.drawable.mx_route_monorail_ref, R.drawable.mx_route_monorail_ref, "routeLightrailColor"),
	RAILWAY(R.drawable.mx_route_railway_ref, R.drawable.mx_route_railway_ref, "routeTrainColor"),
	SHARE_TAXI(R.drawable.mx_route_share_taxi_ref, R.drawable.mx_route_share_taxi_ref, "routeShareTaxiColor"),
	TRAIN(R.drawable.mx_route_train_ref, R.drawable.mx_route_train_ref, "routeTrainColor"),
	TRAM(R.drawable.mx_route_tram_ref, R.drawable.mx_railway_tram_stop, "routeTramColor"),
	TROLLEYBUS(R.drawable.mx_route_trolleybus_ref, R.drawable.mx_route_trolleybus_ref, "routeTrolleybusColor"),
	SUBWAY(R.drawable.mx_subway_station, R.drawable.mx_subway_station, "routeTrainColor");

	final int resId;
	final int topResId;
	final String renderAttr;

	TransportStopType(int resId, int topResId, String renderAttr) {
		this.resId = resId;
		this.topResId = topResId;
		this.renderAttr = renderAttr;
	}

	public int getResourceId() {
		return resId;
	}

	public int getTopResourceId() {
		return topResId;
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
