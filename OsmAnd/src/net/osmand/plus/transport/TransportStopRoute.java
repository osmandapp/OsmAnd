package net.osmand.plus.transport;

import android.util.Pair;

import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportStopRoute {
	public TransportStop refStop;
	public TransportStopType type;
	public String desc;
	public TransportRoute route;
	public TransportStop stop;
	private int stopIndex = -1;
	public int distance;
	public boolean showWholeRoute;
	private int cachedColor;
	private boolean cachedNight;
	private final Map<Pair<String, Boolean>, Integer> cachedRouteColors = new HashMap<>();

	public static TransportStopRoute getTransportStopRoute(TransportRoute rs, TransportStop s) {
		TransportStopType type = TransportStopType.findType(rs.getType());
		TransportStopRoute r = new TransportStopRoute();
		r.type = type;
		r.desc = rs.getName();
		r.route = rs;
		r.stop = s;
		r.refStop = s;
		r.initStopIndex();
		return r;
	}

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

	// TODO: [Hot fix] - Move changes to upper method later
	public String getDescription(OsmandApplication ctx) {
		String preferredMapLang = ctx.getSettings().MAP_PREFERRED_LOCALE.get();
		boolean transliterateNames = ctx.getSettings().MAP_TRANSLITERATE_NAMES.get();
		if (route != null) {
			return route.getName(preferredMapLang, transliterateNames);
		}
		return "";
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
			cachedColor = ctx.getColor(R.color.transport_route_line);
			cachedNight = nightMode;
			if (type != null) {
				RenderingRulesStorage rrs = ctx.getRendererRegistry().getCurrentSelectedRenderer();
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
				String color = route.getColor();
				String typeStr = color == null || color.isEmpty() ? type.getRendeAttr() : color;
				if (req.searchRenderingAttribute(typeStr)) {
					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_ATTR_COLOR_VALUE);
				}
			}
		}
		return cachedColor;
	}

	public int getRouteColor(OsmandApplication ctx, boolean nightMode) {
		Integer cachedColor = null;
		if (route != null) {
			Pair<String, Boolean> key = new Pair<>(route.getType(), nightMode);
			cachedColor = cachedRouteColors.get(key);
			if (cachedColor == null) {
				RenderingRulesStorage rrs = ctx.getRendererRegistry().getCurrentSelectedRenderer();
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
				req.setStringFilter(rrs.PROPS.R_TAG, "pt_line");
				req.setStringFilter(rrs.PROPS.R_VALUE, route.getType());
				if (req.searchRenderingAttribute("publicTransportLine")) {
					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_COLOR);
				}
				cachedRouteColors.put(key, cachedColor);
			}
		}
		if (cachedColor == null || cachedColor == 0) {
			cachedColor = getColor(ctx, nightMode);
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

	private void initStopIndex() {
		if (route == null || stop == null) {
			return;
		}
		List<TransportStop> stops = route.getForwardStops();
		for (int i = 0; i < stops.size(); i++) {
			TransportStop stop = stops.get(i);
			if (this.stop.getId().equals(stop.getId())) {
				stopIndex = i;
				break;
			}
		}
	}

	public int getStopIndex() {
		if (stopIndex == -1) {
			initStopIndex();
		}
		return stopIndex;
	}

	public void setStopIndex(int stopIndex) {
		if (route == null || stop == null) {
			return;
		}
		if (stopIndex == -1) {
			initStopIndex();
		} else if (stopIndex < route.getForwardStops().size()) {
			this.stopIndex = stopIndex;
		}
	}
}
