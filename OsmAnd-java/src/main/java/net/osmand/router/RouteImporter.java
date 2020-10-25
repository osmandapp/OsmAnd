package net.osmand.router;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.RouteSegment;
import net.osmand.GPXUtilities.RouteType;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.StringBundle;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.binary.RouteDataObject.HEIGHT_UNDEFINED;

public class RouteImporter {

	public final static Log log = PlatformUtil.getLog(RouteImporter.class);

	private File file;
	private GPXFile gpxFile;
	private TrkSegment segment;

	private final List<RouteSegmentResult> route = new ArrayList<>();

	public RouteImporter(File file) {
		this.file = file;
	}

	public RouteImporter(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public RouteImporter(TrkSegment segment) {
		this.segment = segment;
	}

	public List<RouteSegmentResult> importRoute() {
		if (gpxFile != null || segment != null) {
			parseRoute();
		} else if (file != null) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				gpxFile = GPXUtilities.loadGPXFile(fis);
				parseRoute();
				gpxFile.path = file.getAbsolutePath();
				gpxFile.modifiedTime = file.lastModified();
			} catch (IOException e) {
				log.error("Error importing route " + file.getAbsolutePath(), e);
				return null;
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
				} catch (IOException ignore) {
					// ignore
				}
			}
		}
		return route;
	}

	private void parseRoute() {
		if (segment != null) {
			parseRoute(segment);
		} else if (gpxFile != null) {
			List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(true);
			for (TrkSegment s : segments) {
				parseRoute(s);
			}
		}
	}

	private void parseRoute(TrkSegment segment) {
		RouteRegion region = new RouteRegion();
		RouteDataResources resources = new RouteDataResources();

		collectLocations(resources, segment);
		List<RouteSegmentResult> route = collectRouteSegments(region, resources, segment);
		collectRouteTypes(region, segment);
		for (RouteSegmentResult routeSegment : route) {
			routeSegment.fillNames(resources);
		}
		this.route.addAll(route);
	}

	private void collectLocations(RouteDataResources resources, TrkSegment segment) {
		List<Location> locations = resources.getLocations();
		double lastElevation = HEIGHT_UNDEFINED;
		if (segment.hasRoute()) {
			for (WptPt point : segment.points) {
				Location loc = new Location("", point.getLatitude(), point.getLongitude());
				if (!Double.isNaN(point.ele)) {
					loc.setAltitude(point.ele);
					lastElevation = point.ele;
				} else if (lastElevation != HEIGHT_UNDEFINED) {
					loc.setAltitude(lastElevation);
				}
				locations.add(loc);
			}
		}
	}

	private List<RouteSegmentResult> collectRouteSegments(RouteRegion region, RouteDataResources resources, TrkSegment segment) {
		List<RouteSegmentResult> route = new ArrayList<>();
		for (RouteSegment routeSegment : segment.routeSegments) {
			RouteDataObject object = new RouteDataObject(region);
			RouteSegmentResult segmentResult = new RouteSegmentResult(object);
			segmentResult.readFromBundle(new RouteDataBundle(resources, routeSegment.toStringBundle()));
			route.add(segmentResult);
		}
		return route;
	}

	private void collectRouteTypes(RouteRegion region, TrkSegment segment) {
		int i = 0;
		for (RouteType routeType : segment.routeTypes) {
			StringBundle bundle = routeType.toStringBundle();
			String t = bundle.getString("t", null);
			String v = bundle.getString("v", null);
			region.initRouteEncodingRule(i++, t, v);
		}
	}
}
