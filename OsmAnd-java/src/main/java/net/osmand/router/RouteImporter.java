package net.osmand.router;

import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.RouteSegment;
import net.osmand.gpx.GPXUtilities.RouteType;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.StringBundle;
import net.osmand.util.Algorithms;

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
	private List<WptPt> segmentRoutePoints;
	private boolean leftSide = false;

	private final List<RouteSegmentResult> route = new ArrayList<>();

	public RouteImporter(File file) {
		this.file = file;
	}

	public RouteImporter(GPXFile gpxFile, boolean leftSide) {
		this.gpxFile = gpxFile;
		this.leftSide = leftSide;
	}

	public RouteImporter(TrkSegment segment, List<WptPt> segmentRoutePoints) {
		this.segment = segment;
		this.segmentRoutePoints = segmentRoutePoints;
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
				gpxFile.pointsModifiedTime = gpxFile.modifiedTime;
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
			parseRoute(segment, segmentRoutePoints);
		} else if (gpxFile != null) {
			List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(true);
			for (int i = 0; i < segments.size(); i++) {
				TrkSegment segment = segments.get(i);
				parseRoute(segment, gpxFile.getRoutePoints(i));
			}
		}
	}

	private void parseRoute(TrkSegment segment, List<WptPt> segmentRoutePoints) {
		RouteRegion region = new RouteRegion();
		RouteDataResources resources = new RouteDataResources();

		collectLocations(resources, segment);
		collectRoutePointIndexes(resources, segmentRoutePoints);
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

	private void collectRoutePointIndexes(RouteDataResources resources, List<WptPt> segmentRoutePoints) {
		List<Integer> routePointIndexes = resources.getRoutePointIndexes();
		if (!Algorithms.isEmpty(segmentRoutePoints)) {
			for (WptPt routePoint : segmentRoutePoints) {
				routePointIndexes.add(routePoint.getTrkPtIndex());
			}
		}
	}

	private List<RouteSegmentResult> collectRouteSegments(RouteRegion region, RouteDataResources resources, TrkSegment segment) {
		List<RouteSegmentResult> route = new ArrayList<>();
		for (RouteSegment routeSegment : segment.routeSegments) {
			RouteDataObject object = new RouteDataObject(region);
			RouteSegmentResult segmentResult = new RouteSegmentResult(object, leftSide);
			try {
				segmentResult.readFromBundle(new RouteDataBundle(resources, routeSegment.toStringBundle()));
				route.add(segmentResult);
			} catch (IllegalStateException e) {
				log.error(e.getMessage());
				break;
			}
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
