package net.osmand.router.network;

import static java.lang.Math.abs;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPoint;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.network.NetworkRouteContext.NetworkRoutePoint;
import net.osmand.router.network.NetworkRouteContext.NetworkRouteSegment;
import net.osmand.router.network.NetworkRouteSelector.NetworkRouteSegmentChain;
import net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NetworkRouteGpxApproximator {

	GPXFile gpxFile;
	
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

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public void setGpxFile(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public List<RouteSegmentResult> approximate() throws IOException {
		Map<RouteKey, GPXFile> res = new HashMap<>();
		List<NetworkRouteSegment> loaded = loadDataByGPX(gpxFile);
		List<NetworkRouteSegmentChain> chainList = selector.getNetworkRouteSegmentChains(null, res, loaded);
		gpxFile = res.values().iterator().next();
		result = convertToRoadSegmentResultList(chainList, loaded.get(0));
		return result;
	}

	List<RouteSegmentResult> convertToRoadSegmentResultList(List<NetworkRouteSegmentChain> chainList,
	                                                        NetworkRouteSegment start) {
		List<List<NetworkRouteSegment>> listOfSegmentLists = new ArrayList<>();
		for (NetworkRouteSegmentChain c : chainList) {
			List<NetworkRouteSegment> segmentList = new ArrayList<>();
			segmentList.add(c.start);
			if (c.connected != null) {
				segmentList.addAll(c.connected);
			}
			listOfSegmentLists.add(segmentList);
			break; // todo check all chains are needed
		}
		List<NetworkRouteSegment> segmentList = fitSegmentsOneByOne(listOfSegmentLists, start);
		List<RouteSegmentResult> res = new ArrayList<>();
		for (NetworkRouteSegment segment : segmentList) {
			res.add(new RouteSegmentResult(segment.robj, segment.start, segment.end));
		}
		return res;
	}

	private List<NetworkRouteSegment> fitSegmentsOneByOne(List<List<NetworkRouteSegment>> listOfSegmentLists, NetworkRouteSegment start) {
		List<NetworkRouteSegment> res = new ArrayList<>();
		while (listOfSegmentLists.size() > 0) {
			List<NetworkRouteSegment> list = findNextSegmentList(listOfSegmentLists, start);
			start = list.get(list.size() - 1);
			res.addAll(list);
		}
		return res;
	}

	private List<NetworkRouteSegment> findNextSegmentList(List<List<NetworkRouteSegment>> listOfSegmentLists,
	                                                      NetworkRouteSegment start) {
		List<NetworkRouteSegment> res = new ArrayList<>();
		double distanceFromFirst = Double.MAX_VALUE;
		List<NetworkRouteSegment> nearFromFirstList = null;
		double distanceFromLast = Double.MAX_VALUE;
		List<NetworkRouteSegment> nearFromLastList = null;
		for (List<NetworkRouteSegment> segmentList : listOfSegmentLists) {
			NetworkRouteSegment first = segmentList.get(0);
			NetworkRouteSegment last = segmentList.get(segmentList.size() - 1);
			double distance = getDistance(start, first);
			if (distance < distanceFromFirst) {
				distanceFromFirst = distance;
				nearFromFirstList = segmentList;
			}
			distance = getDistance(start, last);
			if (distance < distanceFromLast) {
				distanceFromLast = distance;
				nearFromLastList = segmentList;
			}
		}
		if (distanceFromLast < distanceFromFirst) {
			listOfSegmentLists.remove(nearFromLastList);
			nearFromLastList = reverseRoute(nearFromLastList);
			res = new ArrayList<>(nearFromLastList);
		} else {
			if (nearFromFirstList != null) {
				listOfSegmentLists.remove(nearFromFirstList);
				res = new ArrayList<>(nearFromFirstList);
			}
		}
		return res;
	}

	private List<NetworkRouteSegment> reverseRoute(List<NetworkRouteSegment> nearFromLastList) {
		List<NetworkRouteSegment> res = new ArrayList<>();
		for (NetworkRouteSegment segment : nearFromLastList) {
			res.add(segment.inverse());
		}
		Collections.reverse(res);
		return res;
	}

	private double getDistance(NetworkRouteSegment start, NetworkRouteSegment last) {
		return MapUtils.getSqrtDistance(start.getEndPointX(), start.getEndPointY(), last.getStartPointX(), last.getStartPointY());
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
		for (GPXUtilities.Track t : gpxFile.tracks) {
			for (GPXUtilities.TrkSegment ts : t.segments) {
				for (int i = 0; i < ts.points.size() - 1; i++) {
					GPXUtilities.WptPt ps = ts.points.get(i);
					NetworkRoutePoint nearesetPoint = selector.getNetworkRouteContext()
							.getClosestNetworkRoutePoint(MapUtils.get31TileNumberX(ps.lon), MapUtils.get31TileNumberY(ps.lat));
					GpxRoutePoint gpxRoutePoint = new GpxRoutePoint();
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
			// 1. simple segment matching
			NetworkRouteSegment matchingGpxSegment = getMatchingGpxSegments(start, nextPoint);
			if (matchingGpxSegment != null) {
				res.add(matchingGpxSegment);
				continue;
			}
			// 2. skip extra gpx points
			matchingGpxSegment = getGpxSegmentWithoutExtraGpxPoints(gpxRoutePoints, idx, start);
			if (matchingGpxSegment != null) {
				res.add(matchingGpxSegment);
			}
		}
		res = addAbsentPoint(res);
		return res;
	}

	private List<NetworkRouteSegment> addAbsentPoint(List<NetworkRouteSegment> segmentList) {
		List<NetworkRouteSegment> res = new ArrayList<>();
		for (NetworkRouteSegment segment : segmentList) {
			if (abs(segment.end - segment.start) > 1) {
				int step = Integer.signum(segment.end - segment.start);
				for (int idx = segment.start; idx != segment.end; idx += step) {
					res.add(new NetworkRouteSegment(segment, idx, idx + step));
				}
			} else {
				res.add(segment);
			}
		}
		return res;
	}

	private NetworkRouteSegment getGpxSegmentWithoutExtraGpxPoints(List<GpxRoutePoint> gpxRoutePoints, int idx,
	                                                               GpxRoutePoint start) {
		NetworkRouteSegment matchingGpxSegment = null;
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
		
		public List<NetworkRouteSegment> getObjects() {
			if(routePoint == null) {
				return Collections.emptyList();
			}
			return routePoint.objects;
		}
	}
}
