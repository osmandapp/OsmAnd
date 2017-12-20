package net.osmand.plus;

import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;

import java.util.List;

public class TransportStopRoute {
	public TransportStop refStop;
	public TransportStopController.TransportStopType type;
	public String desc;
	public TransportRoute route;
	public TransportStop stop;
	public int distance;
	public boolean showWholeRoute;

	public String getDescription(OsmandApplication ctx, boolean useDistance) {
		if (useDistance && distance > 0) {
			String nm = OsmAndFormatter.getFormattedDistance(distance, ctx);
			if (refStop != null && !refStop.getName().equals(stop.getName())) {
				nm = refStop.getName() + ", " + nm;
			}
			return desc + " (" + nm + ")";
		}
		return desc;
	}

	public int calculateZoom(int startPosition, RotatedTileBox currentRotatedTileBox) {
		RotatedTileBox cp = currentRotatedTileBox.copy();
		boolean notContains = true;
		while (cp.getZoom() > 12 && notContains) {
			notContains = false;
			List<TransportStop> sts = route.getForwardStops();
			for (int i = startPosition; i < sts.size(); i++) {
				TransportStop st = sts.get(startPosition);
				if (!cp.containsLatLon(st.getLocation())) {
					notContains = true;
					break;
				}
			}
			cp.setZoom(cp.getZoom() - 1);
		}
		return cp.getZoom();
	}

	public int getColor(boolean nightMode) {
		int color;
		switch (type) {
			case BUS:
				color = R.color.route_bus_color;
				break;
			case SHARE_TAXI:
				color = R.color.route_share_taxi_color;
				break;
			case TROLLEYBUS:
				color = R.color.route_trolleybus_color;
				break;
			case TRAM:
				color = R.color.route_tram_color;
				break;
			case TRAIN:
				color = nightMode ? R.color.route_train_color_dark : R.color.route_train_color_light;
				break;
			case LIGHT_RAIL:
				color = R.color.route_lightrail_color;
				break;
			case FUNICULAR:
				color = R.color.route_funicular_color;
				break;
			case FERRY:
				color = nightMode ? R.color.route_ferry_color_dark : R.color.route_ferry_color_light;
				break;
			default:
				color = R.color.nav_track;
				break;
		}
		return color;
	}
}
