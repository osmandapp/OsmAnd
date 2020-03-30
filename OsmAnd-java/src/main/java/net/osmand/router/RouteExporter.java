package net.osmand.router;

import net.osmand.GPXUtilities.GPXExtensionsWriter;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.StringBundle;
import net.osmand.binary.StringBundleWriter;
import net.osmand.binary.StringBundleXmlWriter;

import org.xmlpull.v1.XmlSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouteExporter {

	public static final String OSMAND_ROUTER_V2 = "OsmAndRouterV2";

	private String name;
	private List<RouteSegmentResult> route;
	private List<Location> locations;
	private List<WptPt> points;

	public RouteExporter(String name, List<RouteSegmentResult> route, List<Location> locations, List<WptPt> points) {
		this.name = name;
		this.route = route;
		this.locations = locations;
		this.points = points;
	}

	public GPXFile exportRoute() {
		RouteDataResources resources = new RouteDataResources(locations);
		final RouteDataBundle bundle = new RouteDataBundle(resources);

		for (RouteSegmentResult sr : route) {
			sr.collectTypes(resources);
		}
		for (RouteSegmentResult sr : route) {
			sr.collectNames(resources);
		}

		List<StringBundle> routeItems = new ArrayList<>();
		for (RouteSegmentResult sr : route) {
			RouteDataBundle itemBundle = new RouteDataBundle(resources);
			sr.writeToBundle(itemBundle);
			routeItems.add(itemBundle);
		}
		bundle.putBundleList("route", "segment", routeItems);

		List<StringBundle> typeList = new ArrayList<>();
		Map<RouteTypeRule, Integer> rules = resources.getRules();
		for (RouteTypeRule rule : rules.keySet()) {
			RouteDataBundle typeBundle = new RouteDataBundle(resources);
			rule.writeToBundle(typeBundle);
			typeList.add(typeBundle);
		}
		bundle.putBundleList("types", "type", typeList);

		GPXFile gpx = new GPXFile(OSMAND_ROUTER_V2);
		Track track = new Track();
		track.name = name;
		gpx.tracks.add(track);
		TrkSegment trkSegment = new TrkSegment();
		track.segments.add(trkSegment);

		if (locations == null || locations.isEmpty()) {
			return gpx;
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

		if (points != null) {
			for (WptPt pt : points) {
				gpx.addPoint(pt);
			}
		}

		GPXExtensionsWriter extensionsWriter = new GPXExtensionsWriter() {
			@Override
			public void writeExtensions(XmlSerializer serializer) {
				StringBundleWriter bundleWriter = new StringBundleXmlWriter(bundle, serializer);
				bundleWriter.writeBundle();
			}
		};
		gpx.setExtensionsWriter(extensionsWriter);

		return gpx;
	}
}
