package net.osmand.router.network;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
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
		}, routing);
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public void setGpxFile(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public List<Entity> approximate() throws IOException {
		Map<RouteKey, GPXFile> res = new HashMap<>();
		List<NetworkRouteSegment> loaded = loadDataByGPX(gpxFile);
		List<NetworkRouteSegmentChain> chainList = selector.getNetworkRouteSegmentChains(null, res, loaded);
		gpxFile = res.values().iterator().next();
		result = convertToRoadSegmentResultList(chainList);
		return convertToEntities(chainList);
	}

	List<RouteSegmentResult> convertToRoadSegmentResultList(List<NetworkRouteSegmentChain> chainList) {
		List<RouteSegmentResult> result = new ArrayList<>();
		for (NetworkRouteSegmentChain c : chainList) {
			List<NetworkRouteSegment> segmentList = new ArrayList<>();
			segmentList.add(c.start);
			if (c.connected != null) {
				segmentList.addAll(c.connected);
			}
			for (NetworkRouteSegment segment : segmentList) {
				result.add(new RouteSegmentResult(segment.robj, segment.start, segment.end));
			}
		}
		return result;
	}

	private NetworkRouteSegment getMatchingGpxSegments(GpxRoutePoint p1, GpxRoutePoint p2) {
		for (NetworkRouteSegment segStart : p1.getObjects()) {
			for (NetworkRouteSegment segEnd : p2.getObjects()) {
				if (segEnd.getId() == segStart.getId() && segStart.start != segEnd.start) {
					return new NetworkRouteSegment(segStart, segStart.start, segEnd.start);
				}
			}
		}
		return null;
	}

	private List<NetworkRouteSegment> loadDataByGPX(GPXFile gpxFile) throws IOException {
		List<GpxRoutePoint> gpxRoutePoints = new ArrayList<>();
		List<NetworkRouteSegment> res = new ArrayList<>();
		for (GPXUtilities.Track t : gpxFile.tracks) {
			for (GPXUtilities.TrkSegment ts : t.segments) {
				for (int i = 0; i < ts.points.size() - 1; i++) {
					GPXUtilities.WptPt ps = ts.points.get(i);
					NetworkRoutePoint nearesetPoint = selector.rCtx.getClosestNetworkRoutePoint(MapUtils.get31TileNumberX(ps.lon), MapUtils.get31TileNumberY(ps.lat));
					GpxRoutePoint gpxRoutePoint = new GpxRoutePoint();
					if (MapUtils.squareRootDist31(MapUtils.get31TileNumberX(ps.lon), MapUtils.get31TileNumberY(ps.lat), 
							nearesetPoint.x31, nearesetPoint.y31) < GPX_MAX_DISTANCE_POINT_MATCH) {
						gpxRoutePoint.routePoint = nearesetPoint;
					}
					gpxRoutePoints.add(gpxRoutePoint);
				}
			}
		}
		for (int i = 0; i < gpxRoutePoints.size() - 1; i++) {
			GpxRoutePoint start = gpxRoutePoints.get(i);
			GpxRoutePoint nextPoint = gpxRoutePoints.get(i + 1);
			NetworkRouteSegment matchingGpxSegment = getMatchingGpxSegments(start, nextPoint);
			boolean matched = false;
			if (matchingGpxSegment != null) {
				// 1. simple segment matching
				res.add(matchingGpxSegment);
				matched = true;
			}
			if (!matched) {
				// 2. skip gpx points
				for (int j = 2; j < GPX_SKIP_POINTS_GPX_MAX; j++) {
					nextPoint = gpxRoutePoints.get(i + j);
					matchingGpxSegment = getMatchingGpxSegments(start, nextPoint);
					if (matchingGpxSegment != null) {
						boolean notFarAway = true;
						// check that skipped points are not far away
						for (int t = 1; t < j; t++) {
							if (getOrthogonalDistance(gpxRoutePoints.get(i + t),
									matchingGpxSegment) > GPX_MAX_INTER_SKIP_DISTANCE) {
								notFarAway = false;
								break;
							}
						}
						if (notFarAway) {
							res.add(matchingGpxSegment);
							matched = true;
							break;
						}
					}
				}
			}
			if (!matched) {
				// TODO add straight line if needed
			}
		}
		return res;
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

	private List<Entity> convertToEntities(List<NetworkRouteSegmentChain> res) {
		List<Entity> entityList = new ArrayList<>();
		for (NetworkRouteSegmentChain chain : res) {
			if (chain.connected == null) {
				continue;
			}
			for (NetworkRouteSegment segment : chain.connected) {
				boolean plus = segment.start < segment.end;
				int ind = segment.start;
				Way way = new Way(-1);
				while (true) {
					if (segment.robj != null) {
						LatLon l = new LatLon(MapUtils.get31LatitudeY(segment.robj.getPoint31YTile(ind)),
								MapUtils.get31LongitudeX(segment.robj.getPoint31XTile(ind)));
						Node n = new Node(l.getLatitude(), l.getLongitude(), -1);
						way.addNode(n);
					}
					if (ind == segment.end) {
						break;
					}
					ind += plus ? 1 : -1;
				}
				if (way.getNodes().size() > 0) {
					entityList.add(way);
				}
			}
		}
		return entityList;
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
