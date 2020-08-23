package net.osmand.router;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.RouteSegment;
import net.osmand.GPXUtilities.RouteType;
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

	private List<RouteSegmentResult> route = new ArrayList<>();
	private RouteRegion region = new RouteRegion();
	private RouteDataResources resources = new RouteDataResources();

	public RouteImporter(File file) {
		this.file = file;
	}

	public RouteImporter(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public List<RouteSegmentResult> importRoute() {
		if (gpxFile != null) {
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
		collectLocations();
		collectSegments();
		collectTypes();
		for (RouteSegmentResult segment : route) {
			segment.fillNames(resources);
		}
	}

	private void collectLocations() {
		List<Location> locations = resources.getLocations();
		double lastElevation = HEIGHT_UNDEFINED;
		if (gpxFile.tracks.size() > 0 && gpxFile.tracks.get(0).segments.size() > 0 && gpxFile.tracks.get(0).segments.get(0).points.size() > 0) {
			for (WptPt point : gpxFile.tracks.get(0).segments.get(0).points) {
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

	private void collectSegments() {
		for (RouteSegment segment : gpxFile.routeSegments) {
			RouteDataObject object = new RouteDataObject(region);
			RouteSegmentResult segmentResult = new RouteSegmentResult(object);
			segmentResult.readFromBundle(new RouteDataBundle(resources, segment.toStringBundle()));
			route.add(segmentResult);
		}
	}

	private void collectTypes() {
		int i = 0;
		for (RouteType routeType : gpxFile.routeTypes) {
			StringBundle bundle = routeType.toStringBundle();
			String t = bundle.getString("t", null);
			String v = bundle.getString("v", null);
			region.initRouteEncodingRule(i++, t, v);
		}
	}
}
