package net.osmand.router;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.StringBundle;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.RouteSegment;
import net.osmand.shared.gpx.GpxUtilities.RouteType;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouteExporter {

	public static final String OSMAND_ROUTER_V2 = "OsmAndRouterV2";

	private final String name;
	private final List<RouteSegmentResult> route;
	private final List<Location> locations;
	private final List<Integer> routePointIndexes;
	private final List<WptPt> points;

	public RouteExporter(String name, List<RouteSegmentResult> route, List<Location> locations,
	                     List<Integer> routePointIndexes, List<WptPt> points) {
		this.name = name;
		this.route = route;
		this.locations = locations;
		this.routePointIndexes = routePointIndexes == null ? new ArrayList<Integer>() : routePointIndexes;
		this.points = points;
	}

	public GpxFile exportRoute() {
		GpxFile gpx = new GpxFile(OSMAND_ROUTER_V2);
		Track track = new Track();
		track.setName(name);
		gpx.getTracks().add(track);
		track.getSegments().add(generateRouteSegment());
		if (points != null) {
			for (WptPt pt : points) {
				gpx.addPoint(pt);
			}
		}
		return gpx;
	}

	public static GpxFile exportRoute(String name, List<TrkSegment> trkSegments, List<WptPt> points, List<List<WptPt>> routePoints) {
		GpxFile gpx = new GpxFile(OSMAND_ROUTER_V2);
		Track track = new Track();
		track.setName(name);
		gpx.getTracks().add(track);
		track.getSegments().addAll(trkSegments);
		if (points != null) {
			for (WptPt pt : points) {
				gpx.addPoint(pt);
			}
		}
		if (routePoints != null) {
			for (List<WptPt> wptPts : routePoints) {
				gpx.addRoutePoints(wptPts, true);
			}
		}
		return gpx;
	}

	public TrkSegment generateRouteSegment() {
		RouteDataResources resources = new RouteDataResources(locations, routePointIndexes);
		List<StringBundle> routeItems = new ArrayList<>();
		if (!Algorithms.isEmpty(route)) {
			for (RouteSegmentResult sr : route) {
				sr.collectTypes(resources);
			}
			for (RouteSegmentResult sr : route) {
				sr.collectNames(resources);
			}

			for (RouteSegmentResult sr : route) {
				RouteDataBundle itemBundle = new RouteDataBundle(resources);
				sr.writeToBundle(itemBundle);
				routeItems.add(itemBundle);
			}
		}
		List<StringBundle> typeList = new ArrayList<>();
		Map<RouteTypeRule, Integer> rules = resources.getRules();
		for (RouteTypeRule rule : rules.keySet()) {
			RouteDataBundle typeBundle = new RouteDataBundle(resources);
			rule.writeToBundle(typeBundle);
			typeList.add(typeBundle);
		}

		TrkSegment trkSegment = new TrkSegment();
		if (locations == null || locations.isEmpty()) {
			return trkSegment;
		}
		for (int i = 0; i < locations.size(); i++) {
			Location loc = locations.get(i);
			WptPt pt = new WptPt();
			pt.setLat(loc.getLatitude());
			pt.setLon(loc.getLongitude());
			if (loc.hasSpeed()) {
				pt.setSpeed(loc.getSpeed());
			}
			if (loc.hasAltitude()) {
				pt.setEle(loc.getAltitude());
			}
			if (loc.hasAccuracy()) {
				pt.setHdop(loc.getAccuracy());
			}
			if (loc.getTime() > 0) {
				pt.setTime(loc.getTime());
			}
			trkSegment.getPoints().add(pt);
		}

		List<RouteSegment> routeSegments = new ArrayList<>();
		for (StringBundle item : routeItems) {
			net.osmand.shared.util.StringBundle itemInStrings = kStringBundleJustStrings(item);
			routeSegments.add(RouteSegment.Companion.fromStringBundle(itemInStrings));
		}
		trkSegment.setRouteSegments(routeSegments);
		List<RouteType> routeTypes = new ArrayList<>();
		for (StringBundle item : typeList) {
			net.osmand.shared.util.StringBundle itemInStrings = kStringBundleJustStrings(item);
			routeTypes.add(RouteType.Companion.fromStringBundle(itemInStrings));
		}
		trkSegment.setRouteTypes(routeTypes);
		return trkSegment;
	}

	private net.osmand.shared.util.StringBundle kStringBundleJustStrings(StringBundle in) {
		net.osmand.shared.util.StringBundle out = new net.osmand.shared.util.StringBundle();
		in.getMap().forEach((key, item) -> {
			String asString = in.getString(key, null);
			if (asString != null) {
				out.putString(key, asString);
			}
		});
		return out;
	}
}
