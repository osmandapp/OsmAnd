package net.osmand.plus.transport;

import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import java.util.List;

public class TransportStopRoute {
	public TransportStop refStop;
	public TransportStopType type;
	public String desc;
	public TransportRoute route;
	public TransportStop stop;
	public int distance;
	public boolean showWholeRoute;
	private int cachedColor;
	private boolean cachedNight;

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

	public int getColor(OsmandApplication ctx, boolean nightMode) {
		if (cachedColor == 0 || cachedNight != nightMode) {
			cachedColor = ctx.getResources().getColor(R.color.transport_route_line);
			cachedNight = nightMode;
			if (type != null) {
				RenderingRulesStorage rrs = ctx.getRendererRegistry().getCurrentSelectedRenderer();
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
				String typeStr = type.getRendeAttr();
				if (req.searchRenderingAttribute(typeStr)) {
					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_ATTR_COLOR_VALUE);
				}
			}
		}

		return cachedColor;
	}

	public int getTypeStrRes() {
		if (type != null) {
			switch (type) {
				case BUS:
					return R.string.poi_route_bus_ref;
				case TRAM:
					return R.string.poi_route_tram_ref;
				case FERRY:
					return R.string.poi_route_ferry_ref;
				case TRAIN:
					return R.string.poi_route_train_ref;
				case SHARE_TAXI:
					return R.string.poi_route_share_taxi_ref;
				case FUNICULAR:
					return R.string.poi_route_funicular_ref;
				case LIGHT_RAIL:
					return R.string.poi_route_light_rail_ref;
				case MONORAIL:
					return R.string.poi_route_monorail_ref;
				case TROLLEYBUS:
					return R.string.poi_route_trolleybus_ref;
				case RAILWAY:
					return R.string.poi_route_railway_ref;
				case SUBWAY:
					return R.string.poi_route_subway_ref;
				default:
					return R.string.poi_filter_public_transport;
			}
		} else {
			return R.string.poi_filter_public_transport;
		}
	}

}
