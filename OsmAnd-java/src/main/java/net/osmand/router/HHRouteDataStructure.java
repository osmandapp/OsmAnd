package net.osmand.router;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRouteRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.HHRoutingDB.NetworkDBPoint;
import net.osmand.router.HHRoutingDB.NetworkDBSegment;
import net.osmand.util.MapUtils;

public class HHRouteDataStructure {
	
	public static class HHRoutingConfig {
		float HEURISTIC_COEFFICIENT = 0; // A* - 1, Dijkstra - 0
		float DIJKSTRA_DIRECTION = 0; // 0 - 2 directions, 1 - positive, -1 - reverse
		
		Double INITIAL_DIRECTION = null;
		
		
		boolean ROUTE_LAST_MILE = false;
		boolean ROUTE_ALL_SEGMENTS = false;
		boolean ROUTE_ALL_ALT_SEGMENTS = false;
		boolean PRELOAD_SEGMENTS = false;
		
		boolean CALC_ALTERNATIVES = false;
		boolean USE_GC_MORE_OFTEN = false;
		// TODO 3.1 HHRoutePlanner Alternative routes - could use distributions like 50% route (2 alt), 25%/75% route (1 alt)
		double ALT_EXCLUDE_RAD_MULT = 0.3; // radius multiplier to exclude points
		double ALT_EXCLUDE_RAD_MULT_IN = 3; // skip some points to speed up calculation
		double ALT_NON_UNIQUENESS = 0.7; // 0.7 - 30% of points must be unique

		
		double MAX_COST;
		int MAX_DEPTH = -1; // max depth to go to
		int MAX_SETTLE_POINTS = -1; // max points to settle
		
		boolean USE_CH;
		boolean USE_CH_SHORTCUTS;

		boolean USE_MIDPOINT;
		int MIDPOINT_ERROR = 3;
		int MIDPOINT_MAX_DEPTH = 20 + MIDPOINT_ERROR;
		
		public static HHRoutingConfig dijkstra(int direction) {
			HHRoutingConfig df = new HHRoutingConfig();
			df.HEURISTIC_COEFFICIENT = 0;
			df.DIJKSTRA_DIRECTION = direction;
			return df;
		}
		
		public static HHRoutingConfig astar(int direction) {
			HHRoutingConfig df = new HHRoutingConfig();
			df.HEURISTIC_COEFFICIENT = 1;
			df.DIJKSTRA_DIRECTION = direction;
			return df;
		}
		
		public static HHRoutingConfig ch() {
			HHRoutingConfig df = new HHRoutingConfig();
			df.HEURISTIC_COEFFICIENT = 0;
			df.USE_CH = true;
			df.USE_CH_SHORTCUTS = true;
			df.DIJKSTRA_DIRECTION = 0;
			return df;
		}
		
		public static HHRoutingConfig midPoints(boolean astar, int dir) {
			HHRoutingConfig df = new HHRoutingConfig();
			df.HEURISTIC_COEFFICIENT = astar ? 1 : 0;
			df.USE_MIDPOINT = true;
			df.DIJKSTRA_DIRECTION = dir;
			return df;
		}
		
		public HHRoutingConfig preloadSegments() {
			this.PRELOAD_SEGMENTS = true;
			return this;
		}
		
		public HHRoutingConfig calcAlternative() {
			this.CALC_ALTERNATIVES = true;
			return this;
		}
		
		public HHRoutingConfig calcDetailed(int segments) {
			this.ROUTE_LAST_MILE = true;
			this.ROUTE_ALL_SEGMENTS = segments >= 1;
			this.ROUTE_ALL_ALT_SEGMENTS = segments >= 2;
			return this;
		}

		
		public HHRoutingConfig useShortcuts() {
			USE_CH_SHORTCUTS = true;
			return this;
		}
		
		public HHRoutingConfig gc() {
			USE_GC_MORE_OFTEN = true;
			return this;
		}
		
		public HHRoutingConfig maxCost(double cost) {
			MAX_COST = cost;
			return this;
		}
		
		public HHRoutingConfig maxDepth(int depth) {
			MAX_DEPTH = depth;
			return this;
		}
		
		public HHRoutingConfig maxSettlePoints(int maxPoints) {
			MAX_SETTLE_POINTS = maxPoints;
			return this;
		}

		@Override
		public String toString() {
			return toString(null, null);
		}
		
		public String toString(LatLon start, LatLon end) {
			return String.format("Routing %s -> %s (HC %d, dir %d)", start == null ? "?" : start.toString(),
					end == null ? "?" : end.toString(), (int) HEURISTIC_COEFFICIENT, (int) DIJKSTRA_DIRECTION);
		}
	}
	
	public static class HHRoutingContext<T extends NetworkDBPoint> {
		// faster when roads are in 1 global network but doesn't make sense for isolated islands
		static boolean USE_GLOBAL_QUEUE = false; 
		
		// Initial data structure
		RoutingContext rctx; 
		HHRoutingDB networkDB;
		BinaryMapIndexReader file;
		HHRouteRegion fileRegion;
		int routingProfile = 0;
		
		// Global network data structure 
		TLongObjectHashMap<T> pointsById;
		TLongObjectHashMap<T> pointsByFileId;
		TLongObjectHashMap<T> pointsByGeo;
		TIntObjectHashMap<List<T>> clusterInPoints;
		TIntObjectHashMap<List<T>> clusterOutPoints;

		DataTileManager<T> pointsRect = new DataTileManager<>(11); // 20km tile
		TLongObjectHashMap<RouteSegment> boundaries;
		
		// Route specific details
		RoutingStats stats;
		HHRoutingConfig config;
		int startX;
		int startY;
		int endY;
		int endX;
		
		// Route runtime vars
		List<T> queueAdded = new ArrayList<>();
		List<T> visited = new ArrayList<>();
		List<T> visitedRev = new ArrayList<>();
		
		Queue<NetworkDBPointCost<T>> queue = createQueue();
		Queue<NetworkDBPointCost<T>> queuePos = createQueue();
		Queue<NetworkDBPointCost<T>> queueRev = createQueue();

		

		private PriorityQueue<NetworkDBPointCost<T>> createQueue() {
			return new PriorityQueue<>(new Comparator<NetworkDBPointCost<T>>() {
				@Override
				public int compare(NetworkDBPointCost<T> o1, NetworkDBPointCost<T> o2) {
					return Double.compare(o1.cost, o2.cost);
				}
			});
		}
		
		public void clearVisited() {
			queue(false).clear();
			queue(true).clear();
			for (NetworkDBPoint p : queueAdded) {
				p.clearRouting();
			}
			queueAdded.clear();
			visited.clear();
			visitedRev.clear();
		}

		public void clearVisited(TLongObjectHashMap<T> stPoints, TLongObjectHashMap<T> endPoints) {
			queue(false).clear();
			queue(true).clear();
			Iterator<T> it = queueAdded.iterator();
			while (it.hasNext()) {
				NetworkDBPoint p = it.next();
				if (stPoints.containsKey(p.index)) {
					p.setDetailedParentRt(false, p.rt(false).rtDetailedRoute);
				} else if (endPoints.containsKey(p.index)) {
					p.setDetailedParentRt(true, p.rt(true).rtDetailedRoute);
				} else {
					p.clearRouting();
				}
				it.remove();
			}
			visited.clear();
			visitedRev.clear();
		}

		public void unloadAllConnections() {
			for (NetworkDBPoint p : pointsById.valueCollection()) {
				p.markSegmentsNotLoaded();
			}
		}

		public void setStartEnd(LatLon start, LatLon end) {
			if (start != null) {
				startY = MapUtils.get31TileNumberY(start.getLatitude());
				startX = MapUtils.get31TileNumberX(start.getLongitude());
			}
			if (end != null) {
				endY = MapUtils.get31TileNumberY(end.getLatitude());
				endX = MapUtils.get31TileNumberX(end.getLongitude());
			}
		}

		public Queue<NetworkDBPointCost<T>> queue(boolean rev) {
			return USE_GLOBAL_QUEUE ? queue : (rev ? queueRev : queuePos);
		}

		public TLongObjectHashMap<T> loadNetworkPoints(Class<T> pointClass) throws SQLException, IOException {
			if (networkDB != null) {
				return networkDB.loadNetworkPoints(pointClass);
			}
			if (file != null) {
				return file.initHHPoints(fileRegion, pointClass);
			}
			throw new IllegalStateException();
		}

		public int loadNetworkSegments(Collection<T> valueCollection) throws SQLException {
			if (networkDB != null) {
				return networkDB.loadNetworkSegments(valueCollection, routingProfile);
			}
			throw new UnsupportedOperationException();
		}

		public boolean loadGeometry(NetworkDBSegment segment, boolean reload) throws SQLException {
			if (networkDB != null && !networkDB.compactDB) {
				networkDB.loadGeometry(segment, routingProfile, reload);
				return true;
			}
			return false;
		}

		public int loadNetworkSegmentPoint(T point, boolean reverse) throws SQLException, IOException {
			if (networkDB != null) {
				return networkDB.loadNetworkSegmentPoint(pointsById, clusterInPoints, clusterOutPoints, routingProfile,
						point, reverse);
			}
			if (file != null) {
				return file.loadNetworkSegmentPoint(fileRegion, pointsById, pointsByFileId, clusterInPoints,
						clusterOutPoints, routingProfile, point, reverse);
			}
			throw new UnsupportedOperationException();
		}

		public String getRoutingProfile() {
			if (networkDB != null) {
				return networkDB.getRoutingProfile() + " [" + networkDB.getRoutingProfiles().get(routingProfile) + "] ";
			}
			if (fileRegion != null) {
				return fileRegion.profile + " [" + fileRegion.profileParams.get(routingProfile) + "] ";
			}
			return "";
		}
		
	}

	static class NetworkDBPointCost<T> {
		final T point;
		final double cost;
		final boolean rev;
		
		NetworkDBPointCost(T p, double cost, boolean rev) {
			point = p;
			this.cost = cost;
			this.rev = rev;
		}
	}
	
	public static class RoutingStats {
		int firstRouteVisitedVertices = 0;
		int visitedVertices = 0;
		int uniqueVisitedVertices = 0;
		int addedVertices = 0;

		double loadPointsTime = 0;
		int loadEdgesCnt;
		double loadEdgesTime = 0;
		double altRoutingTime;
		double routingTime = 0;
		double searchPointsTime = 0;
		double addQueueTime = 0;
		double pollQueueTime = 0;
		double prepTime = 0;
	}
	
	public static class HHNetworkRouteRes {
		public RoutingStats stats;
		public List<HHNetworkSegmentRes> segments = new ArrayList<>();
		public List<RouteSegmentResult> detailed = new ArrayList<>();
		public List<HHNetworkRouteRes> altRoutes = new ArrayList<>();
		public TLongHashSet uniquePoints = new TLongHashSet();
		public String error;
		
		public double getHHRoutingTime() {
			double d = 0;
			for (HHNetworkSegmentRes r : segments) {
				d += r.rtTimeHHSegments;
			}
			return d;
		}
		
		public double getHHRoutingDetailed() {
			double d = 0;
			for (HHNetworkSegmentRes r : segments) {
				d += r.rtTimeDetailed;
			}
			return d;
		}
		
	}
	
	public static class HHNetworkSegmentRes {
		public NetworkDBSegment segment;
		public List<RouteSegmentResult> list = null;
		public double rtTimeDetailed;
		public double rtTimeHHSegments;
		public HHNetworkSegmentRes(NetworkDBSegment s) {
			segment = s;
		}
	}
}
