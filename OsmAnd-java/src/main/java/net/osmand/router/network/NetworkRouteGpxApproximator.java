package net.osmand.router.network;

import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXFile;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPoint;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.router.network.NetworkRouteContext.NetworkRoutePoint;
import net.osmand.router.network.NetworkRouteContext.NetworkRouteSegment;
import net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkRouteGpxApproximator {

	private static final double GPX_MAX_DISTANCE_POINT_MATCH = 5;
	private static final double GPX_MAX_INTER_SKIP_DISTANCE = 10;
	private static final int GPX_SKIP_POINTS_GPX_MAX = 5;

	private final NetworkRouteSelector selector;
	public List<RouteSegmentResult> result = null;

	public NetworkRouteGpxApproximator(BinaryMapIndexReader[] files, boolean routing) {
		selector = new NetworkRouteSelector(files, new NetworkRouteSelectorFilter() {
			@Override
			public List<RouteKey> convert(RouteDataObject obj) {
				return Collections.singletonList(null);
			}
			
			@Override
			public List<RouteKey> convert(BinaryMapDataObject obj) {
				return Collections.singletonList(null);
			}
		}, null, routing);
	}

	public List<RouteSegmentResult> approximate(GPXFile gpxFile, RoutingContext rCtx) throws IOException {
		List<NetworkRouteSegment> loaded = loadDataByGPX(gpxFile);
		List<RouteSegmentResult> res = new ArrayList<>();
		for (NetworkRouteSegment segment : loaded) {
			res.add(new RouteSegmentResult(segment.robj, segment.start, segment.end));
		}
		new RouteResultPreparation().prepareResult(rCtx, res, true);
		return result = res;
	}

	private double getDistance(GpxRoutePoint start, GpxRoutePoint end) {
		return MapUtils.getDistance(start.lat, start.lon, end.lat, end.lon);
	}

	private NetworkRouteSegment getMatchingGpxSegments(GpxRoutePoint p1, GpxRoutePoint p2) {
		List<NetworkRouteSegment> segments = new ArrayList<>();
		for (NetworkRouteSegment segStart : p1.getObjects()) {
			for (NetworkRouteSegment segEnd : p2.getObjects()) {
				if (segEnd.getId() == segStart.getId() && segStart.start != segEnd.start) {
					segments.add(new NetworkRouteSegment(segStart, segStart.start, segEnd.start));
				}
			}
		}
		//fix https://www.openstreetmap.org/way/51203425
		NetworkRouteSegment res = null;
		if (!segments.isEmpty()) {
			double minLength = Double.MAX_VALUE;
			for (NetworkRouteSegment segment : segments) {
				double length = segment.robj.distance(segment.start, segment.end);
				if (length < minLength) {
					minLength = length;
					res = segment;
				}
			}
		}
		return res;
	}

	private List<NetworkRouteSegment> loadDataByGPX(GPXFile gpxFile) throws IOException {
		List<GpxRoutePoint> gpxRoutePoints = new ArrayList<>();
		List<NetworkRouteSegment> res = new ArrayList<>();
		List<NetworkRoutePoint> passedRoutePoints = new ArrayList<>();
		int totalDistance = 0;
		int unmatchedDistance = 0;
		int unmatchedPoints = 0;
		for (GPXUtilities.Track t : gpxFile.tracks) {
			for (GPXUtilities.TrkSegment ts : t.segments) {
				for (int i = 0; i < ts.points.size() - 1; i++) {
					GPXUtilities.WptPt ps = ts.points.get(i);
					NetworkRoutePoint nearesetPoint = selector.getNetworkRouteContext()
							.getClosestNetworkRoutePoint(MapUtils.get31TileNumberX(ps.lon), MapUtils.get31TileNumberY(ps.lat));
					GpxRoutePoint gpxRoutePoint = new GpxRoutePoint(ps.lat, ps.lon);
					if (MapUtils.squareRootDist31(MapUtils.get31TileNumberX(ps.lon), MapUtils.get31TileNumberY(ps.lat),
							nearesetPoint.x31, nearesetPoint.y31) < GPX_MAX_DISTANCE_POINT_MATCH) {
						gpxRoutePoint.routePoint = nearesetPoint;
					}
					if (!passedRoutePoints.contains(nearesetPoint)) {
						passedRoutePoints.add(nearesetPoint);
						gpxRoutePoints.add(gpxRoutePoint);
					}
				}
			}
		}
		for (int idx = 0; idx < gpxRoutePoints.size() - 1; idx++) {
			GpxRoutePoint start = gpxRoutePoints.get(idx);
			GpxRoutePoint nextPoint = gpxRoutePoints.get(idx + 1);
			totalDistance += getDistance(start, nextPoint);
			// 1. simple segment matching
			NetworkRouteSegment matchingGpxSegment = getMatchingGpxSegments(start, nextPoint);
			if (matchingGpxSegment != null) {
				res.add(matchingGpxSegment);
				continue;
			}
			// 2. skip extra gpx points
			int[] idxa = {idx};
			matchingGpxSegment = getGpxSegmentWithoutExtraGpxPoints(gpxRoutePoints, idxa, start);
			idx = idxa[0];
			if (matchingGpxSegment != null) {
				res.add(matchingGpxSegment);
				continue;
			}
			nextPoint = gpxRoutePoints.get(idx + 1);
			res.add(createStraightSegment(start, nextPoint));
			unmatchedDistance += getDistance(start, nextPoint);
			unmatchedPoints++;
		}
		int matchingGpxSegmentSize = res.size();
		System.out.printf(">> GPX approximation (%d route points matched, %d points unmatched) for %d m: %d m unmatched\n",
				matchingGpxSegmentSize, unmatchedPoints, totalDistance, unmatchedDistance);
		return res;
	}

	private NetworkRouteSegment createStraightSegment(GpxRoutePoint startPoint, GpxRoutePoint nextPoint) {
		BinaryMapRouteReaderAdapter.RouteRegion reg = new BinaryMapRouteReaderAdapter.RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);
		RouteDataObject rdo = new RouteDataObject(reg);
		rdo.pointsX = new int[]{MapUtils.get31TileNumberX(startPoint.lon), MapUtils.get31TileNumberX(nextPoint.lon)};
		rdo.pointsY = new int[]{MapUtils.get31TileNumberY(startPoint.lat), MapUtils.get31TileNumberY(nextPoint.lat)};
		rdo.types = new int[]{0};
		rdo.id = -1;
		return new NetworkRouteSegment(rdo, null, 0, 1);
	}

	private NetworkRouteSegment getGpxSegmentWithoutExtraGpxPoints(List<GpxRoutePoint> gpxRoutePoints, int[] idxa,
	                                                               GpxRoutePoint start) {
		NetworkRouteSegment matchingGpxSegment = null;
		int idx = idxa[0];
		int maxSkipGpxPoints = Math.min(gpxRoutePoints.size() - idx, GPX_SKIP_POINTS_GPX_MAX);
		for (int j = 2; j < maxSkipGpxPoints; j++) {
			GpxRoutePoint nextPoint = gpxRoutePoints.get(idx + j);
			matchingGpxSegment = getMatchingGpxSegments(start, nextPoint);
			if (matchingGpxSegment != null) {
				boolean notFarAway = true;
				// check that skipped points are not far away
				for (int t = 1; t < j; t++) {
					GpxRoutePoint gpxRoutePoint = gpxRoutePoints.get(idx + t);
					if (gpxRoutePoint.routePoint != null && getOrthogonalDistance(gpxRoutePoint,
							matchingGpxSegment) > GPX_MAX_INTER_SKIP_DISTANCE) {
						notFarAway = false;
						break;
					}
				}
				if (notFarAway) {
					idxa[0] = idx + j - 1;
					break;
				}
			}
		}
		return matchingGpxSegment;
	}

	private double getOrthogonalDistance(GpxRoutePoint gpxRoutePoint, NetworkRouteSegment matchingGpxSegment) {
		double minDistance = Double.MAX_VALUE;
		int px31 = gpxRoutePoint.routePoint.x31;
		int py31 = gpxRoutePoint.routePoint.y31;
		int step = matchingGpxSegment.start < matchingGpxSegment.end ? 1 : -1;
		for (int i = matchingGpxSegment.start; i < matchingGpxSegment.end; i += step) {
			int x1 = matchingGpxSegment.robj.pointsX[i];
			int y1 = matchingGpxSegment.robj.pointsY[i];
			int x2 = matchingGpxSegment.robj.pointsX[i + step];
			int y2 = matchingGpxSegment.robj.pointsY[i + step];
			QuadPoint pp = MapUtils.getProjectionPoint31(px31, py31, x1, y1, x2, y2);
			double distance = MapUtils.squareRootDist31(px31, py31, (int) pp.x, (int) pp.y);
			minDistance = Math.min(minDistance, distance);
		}
		return minDistance;
	}

	public static class GpxRoutePoint {
		int idx;
		double lat;
		double lon;
		NetworkRoutePoint routePoint = null;

		public GpxRoutePoint(double lat, double lon) {
			this.lat = lat;
			this.lon = lon;
		}

		public List<NetworkRouteSegment> getObjects() {
			if (routePoint == null) {
				return Collections.emptyList();
			}
			return routePoint.objects;
		}
	}
}
