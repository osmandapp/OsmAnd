package net.osmand.router;

import net.osmand.Location;
import net.osmand.shared.routing.RouteTypeRule;
import net.osmand.shared.routing.RouteDataBundle;
import net.osmand.shared.util.StringBundle;
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
import net.osmand.shared.routing.RouteDataResources;
import net.osmand.shared.routing.RouteSegmentResult;

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
		RouteDataResources resources = new RouteDataResources(Location.toShared(locations), routePointIndexes);
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
			typeBundle.putString("t", rule.getTag());
			if (rule.getValue() != null) {
				typeBundle.putString("v", rule.getValue());
			}
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
			if (loc.getSpeed() > 0) {
				pt.setSpeed(loc.getSpeed());
			}
			trkSegment.getPoints().add(pt);
		}

		List<RouteSegment> routeSegments = new ArrayList<>();
		for (StringBundle item : routeItems) {
			routeSegments.add(RouteSegment.Companion.fromStringBundle(item));
		}
		trkSegment.setRouteSegments(routeSegments);
		List<RouteType> routeTypes = new ArrayList<>();
		for (StringBundle item : typeList) {
			routeTypes.add(RouteType.Companion.fromStringBundle(item));
		}
		trkSegment.setRouteTypes(routeTypes);
		return trkSegment;
	}

}
