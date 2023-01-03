package net.osmand.router;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.RouteSegment;
import net.osmand.gpx.GPXUtilities.RouteType;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.StringBundle;
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

	public GPXFile exportRoute() {
		GPXFile gpx = new GPXFile(OSMAND_ROUTER_V2);
		Track track = new Track();
		track.name = name;
		gpx.tracks.add(track);
		track.segments.add(generateRouteSegment());
		if (points != null) {
			for (WptPt pt : points) {
				gpx.addPoint(pt);
			}
		}
		return gpx;
	}

	public static GPXFile exportRoute(String name, List<TrkSegment> trkSegments, List<WptPt> points, List<List<WptPt>> routePoints) {
		GPXFile gpx = new GPXFile(OSMAND_ROUTER_V2);
		Track track = new Track();
		track.name = name;
		gpx.tracks.add(track);
		track.segments.addAll(trkSegments);
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
			pt.lat = loc.getLatitude();
			pt.lon = loc.getLongitude();
			if (loc.hasSpeed()) {
				pt.speed = loc.getSpeed();
			}
			if (loc.hasAltitude()) {
				pt.ele = loc.getAltitude();
			}
			if (loc.hasAccuracy()) {
				pt.hdop = loc.getAccuracy();
			}
			trkSegment.points.add(pt);
		}

		List<RouteSegment> routeSegments = new ArrayList<>();
		for (StringBundle item : routeItems) {
			routeSegments.add(RouteSegment.fromStringBundle(item));
		}
		trkSegment.routeSegments = routeSegments;
		List<RouteType> routeTypes = new ArrayList<>();
		for (StringBundle item : typeList) {
			routeTypes.add(RouteType.fromStringBundle(item));
		}
		trkSegment.routeTypes = routeTypes;
		return trkSegment;
	}
}
