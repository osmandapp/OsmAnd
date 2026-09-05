package net.osmand.plus.transport;

import androidx.annotation.DrawableRes;

import net.osmand.plus.R;

public enum TransportType {

	TRANSPORT_STOPS("transportStops", R.drawable.ic_action_transport_stop),
	BUS("showBusRoutes", R.drawable.mx_highway_bus_stop),
	TROLLEYBUS("showTrolleybusRoutes", R.drawable.mx_route_trolleybus_ref),
	SUBWAY("subwayMode", R.drawable.ic_action_transport_subway),
	SHARE_TAXI("showShareTaxiRoutes", R.drawable.mx_route_share_taxi_ref),
	TRAM("showTramRoutes", R.drawable.mx_railway_tram_stop),
	TRAIN("showTrainRoutes", R.drawable.mx_railway_station),
	LIGHT_RAIL("showLightRailRoutes", R.drawable.mx_route_light_rail_ref),
	FUNICULAR("showFunicularRoutes", R.drawable.mx_funicular),
	MONORAIL("showMonorailRoutes", R.drawable.mx_route_monorail_ref);

	TransportType(String attrName, int iconId) {
		this.attrName = attrName;
		this.iconId = iconId;
	}

	private final String attrName;
	@DrawableRes
	private final int iconId;

	public String getAttrName() {
		return attrName;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}
}
