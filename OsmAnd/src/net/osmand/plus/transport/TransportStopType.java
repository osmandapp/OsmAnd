package net.osmand.plus.transport;

import net.osmand.plus.R;

public enum TransportStopType {
	BUS(R.drawable.mx_route_bus_ref, R.drawable.mx_route_bus_ref, R.drawable.mm_route_bus_ref, "routeBusColor","route_bus_ref"),
	FERRY(R.drawable.mx_route_ferry_ref, R.drawable.mx_route_ferry_ref, R.drawable.mm_route_ferry_ref, "routeFerryColor","route_ferry_ref"),
	FUNICULAR(R.drawable.mx_route_funicular_ref, R.drawable.mx_route_funicular_ref, R.drawable.mm_route_funicular_ref, "routeFunicularColor","route_funicular_ref"),
	LIGHT_RAIL(R.drawable.mx_route_light_rail_ref, R.drawable.mx_route_light_rail_ref, R.drawable.mm_route_light_rail_ref, "routeLightrailColor","route_light_rail_ref"),
	MONORAIL(R.drawable.mx_route_monorail_ref, R.drawable.mx_route_monorail_ref, R.drawable.mm_route_monorail_ref, "routeLightrailColor","route_monorail_ref"),
	RAILWAY(R.drawable.mx_route_railway_ref, R.drawable.mx_route_railway_ref, R.drawable.mm_route_railway_ref, "routeTrainColor","route_railway_ref"),
	SHARE_TAXI(R.drawable.mx_route_share_taxi_ref, R.drawable.mx_route_share_taxi_ref, R.drawable.mm_route_share_taxi_ref, "routeShareTaxiColor","route_share_taxi_ref"),
	TRAIN(R.drawable.mx_route_train_ref, R.drawable.mx_route_train_ref, R.drawable.mm_route_train_ref, "routeTrainColor","route_train_ref"),
	TRAM(R.drawable.mx_route_tram_ref, R.drawable.mx_railway_tram_stop, R.drawable.mx_route_tram_ref, "routeTramColor","route_tram_ref"),
	TROLLEYBUS(R.drawable.mx_route_trolleybus_ref, R.drawable.mx_route_trolleybus_ref, R.drawable.mm_route_trolleybus_ref, "routeTrolleybusColor","route_trolleybus_ref"),
	SUBWAY(R.drawable.mx_subway_station, R.drawable.mx_subway_station, R.drawable.mm_subway_station, "routeTrainColor","subway_station");

	final int resId;
	final int topResId;
	final int smallResId;
	final String renderAttr;
	final String nameDrawable;

	TransportStopType(int resId, int topResId, int smallResId, String renderAttr, String nameDrawable) {
		this.resId = resId;
		this.topResId = topResId;
		this.renderAttr = renderAttr;
		this.smallResId = smallResId;
		this.nameDrawable = nameDrawable;
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

	public String getNameDrawable() {
		return nameDrawable;
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
