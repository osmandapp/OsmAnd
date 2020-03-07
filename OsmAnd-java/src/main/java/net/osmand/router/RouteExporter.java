package net.osmand.router;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXExtensionsWriter;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.StringBundle;
import net.osmand.binary.StringBundleWriter;
import net.osmand.binary.StringBundleXmlWriter;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RouteExporter {

	private static final String OSMAND_ROUTER = "OsmAndRouter";

	private File file;
	private List<RouteSegmentResult> route;
	private List<Location> locations;

	public RouteExporter(File file, List<RouteSegmentResult> route, List<Location> locations) {
		this.file = file;
		this.route = route;
		this.locations = locations;
	}

	public Exception export() {
		RouteDataResources resources = new RouteDataResources();
		final RouteDataBundle bundle = new RouteDataBundle(resources);

		for (RouteSegmentResult sr : route) {
			sr.collectResources(resources);
		}

		List<StringBundle> routeItems = new ArrayList<>();
		for (RouteSegmentResult sr : route) {
			RouteDataBundle itemBundle = new RouteDataBundle(resources);
			sr.writeToBundle(itemBundle);
			routeItems.add(itemBundle);
		}
		bundle.putBundleList("route", "segment", routeItems);

		List<StringBundle> segments = new ArrayList<>();
		for (RouteSegmentResult segment : resources.getRouteSegments()) {
			RouteDataBundle segmentBundle = new RouteDataBundle(resources);
			segment.writeToBundle(segmentBundle);
			segments.add(segmentBundle);
		}
		bundle.putBundleList("segments", "segment", segments);

		List<StringBundle> dataObjects = new ArrayList<>();
		for (RouteDataObject dataObject : resources.getRouteDataObjects()) {
			RouteDataBundle objectBundle = new RouteDataBundle(resources);
			dataObject.writeToBundle(objectBundle);
			dataObjects.add(objectBundle);
		}
		bundle.putBundleList("objects", "object", dataObjects);

		List<StringBundle> regions = new ArrayList<>();
		for (RouteRegion routeRegion : resources.getRouteRegions()) {
			RouteDataBundle regionBundle = new RouteDataBundle(resources);
			routeRegion.writeToBundle(regionBundle);
			regions.add(regionBundle);
		}
		bundle.putBundleList("regions", "region", regions);

		GPXFile gpx = new GPXFile("OsmAnd");
		gpx.author = OSMAND_ROUTER;
		Track track = new Track();
		track.name = new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date());
		gpx.tracks.add(track);
		TrkSegment trkSegment = new TrkSegment();
		track.segments.add(trkSegment);
		for (int i = 0; i< locations.size(); i++) {
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

		GPXExtensionsWriter extensionsWriter = new GPXExtensionsWriter() {
			@Override
			public void writeExtensions(XmlSerializer serializer) {
				StringBundleWriter bundleWriter = new StringBundleXmlWriter(bundle, serializer);
				bundleWriter.writeBundle();
			}
		};
		gpx.setExtensionsWriter(extensionsWriter);

		Exception result = GPXUtilities.writeGpxFile(file, gpx);
		return result;
	}
}
