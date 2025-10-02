package net.osmand.router;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

import com.google.protobuf.CodedInputStream;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRouteRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.util.MapUtils;

public class HHRouteDataStructure {
	
	public static class HHRoutingConfig {
		public final static int CALCULATE_ALL_DETAILED = 3;
		public static int STATS_VERBOSE_LEVEL = 1; // 0 less verbose
		float HEURISTIC_COEFFICIENT = 0; // A* - 1, Dijkstra - 0
		float DIJKSTRA_DIRECTION = 0; // 0 - 2 directions, 1 - positive, -1 - reverse
		public HHRoutingContext<NetworkDBPoint> cacheCtx;
		
		// tweaks for route recalculations
		int FULL_DIJKSTRA_NETWORK_RECALC = 10;
		int MAX_START_END_REITERATIONS = 50;  
		double MAX_INC_COST_CF = 1.25;
		int MAX_COUNT_REITERATION = 30; // 3 is enough for 90%, 30 is for 10% (100-750km with 1.5m months live updates)
		Double INITIAL_DIRECTION = null;

		boolean STRICT_BEST_GROUP_MAPS = false; // derived from RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS

		boolean ROUTE_LAST_MILE = false;
		boolean ROUTE_ALL_SEGMENTS = false;
		boolean ROUTE_ALL_ALT_SEGMENTS = false;
		boolean PRELOAD_SEGMENTS = false;
		
		boolean CACHE_CALCULATION_CONTEXT = false;
		boolean CALC_ALTERNATIVES = false;
		boolean USE_GC_MORE_OFTEN = false;
		
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
		
		public HHRoutingConfig cacheContext(HHRoutingContext<NetworkDBPoint> toCache) {
			this.CACHE_CALCULATION_CONTEXT = true;
			this.cacheCtx = toCache;
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

		public HHRoutingConfig applyCalculateMissingMaps(boolean calculateMissingMaps) {
			STRICT_BEST_GROUP_MAPS = calculateMissingMaps;
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

	public static class HHRouteRegionPointsCtx<T extends NetworkDBPoint> {
		final HHRoutingDB networkDB;
		final BinaryMapIndexReader file;
		final HHRouteRegion fileRegion;
		public final short id;
		int routingProfile = 0;
		TLongObjectHashMap<T> pntsByFileId = new TLongObjectHashMap<T>();
		
		public HHRouteRegionPointsCtx(short id, HHRoutingDB networkDB) {
			this.id = id;
			this.fileRegion = null;
			this.file = null;
			this.networkDB = networkDB;
		}
		
		public HHRouteRegionPointsCtx(short id, HHRouteRegion fileRegion, BinaryMapIndexReader file, int routingProfile) {
			this.id = id;
			this.fileRegion = fileRegion;
			this.file = file;
			this.networkDB = null;
			if (routingProfile >= 0) {
				this.routingProfile = routingProfile;
			}
		}
		
		public int getRoutingProfile() {
			return routingProfile;
		}
		
		public HHRouteRegion getFileRegion() {
			return fileRegion;
		}
		
		
		public T getPoint(int pntFileId) {
			return pntsByFileId.get(pntFileId);
		}
		
		
	}
	
	public static class HHRoutingContext<T extends NetworkDBPoint> {
		// faster when roads are in 1 global network but doesn't make sense for isolated islands
		static boolean USE_GLOBAL_QUEUE = false; 
		
		// Initial data structure
		RoutingContext rctx; 
		List<HHRouteRegionPointsCtx<T>> regions = new ArrayList<>();
		TreeMap<String, String> filterRoutingParameters = new TreeMap<>();
		
		TLongObjectHashMap<T> pointsById; 
		TLongObjectHashMap<T> pointsByGeo;
		TIntObjectHashMap<List<T>> clusterInPoints;
		TIntObjectHashMap<List<T>> clusterOutPoints;

		DataTileManager<T> pointsRect = new DataTileManager<>(11); // 20km tile
		TLongObjectHashMap<RouteSegment> boundaries;
		boolean initialized = false;
		
		// Route specific details
		RoutingStats stats = new RoutingStats();
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
		
		public void clearAll(TLongObjectHashMap<T> stPoints, TLongObjectHashMap<T> endPoints) {
			clearVisited();
			if (stPoints != null) {
				for (NetworkDBPoint p : stPoints.valueCollection()) {
					p.clearRouting();
				}
			}
			if (endPoints != null) {
				for (NetworkDBPoint p : endPoints.valueCollection()) {
					p.clearRouting();
				}
			}
		}
		
		public void clearSegments() {
			for (T p : pointsById.valueCollection()) {
				p.markSegmentsNotLoaded();
			}
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

		public List<T> getIncomingPoints(T point) {
			return clusterInPoints.get(point.clusterId);
		}
		
		public List<T> getOutgoingPoints(T point) {
			return clusterOutPoints.get(point.dualPoint.clusterId);
		}

		public void clearVisited(TLongObjectHashMap<T> stPoints, TLongObjectHashMap<T> endPoints) {
			queue(false).clear();
			queue(true).clear();
			for (NetworkDBPoint p : queueAdded) {
				FinalRouteSegment pos = p.rt(false).rtDetailedRoute;
				FinalRouteSegment rev = p.rt(true).rtDetailedRoute;
				p.clearRouting();
				if (pos != null && stPoints.containsKey(p.index)) {
					p.setDistanceToEnd(false, distanceToEnd(false, p));
					p.setDetailedParentRt(false, pos);
				}
				if (rev != null && endPoints.containsKey(p.index)) {
					p.setDistanceToEnd(true, distanceToEnd(true, p));
					p.setDetailedParentRt(true, rev);
				}
			}
			queueAdded.clear();
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
			TLongObjectHashMap<T> points = new TLongObjectHashMap<>();
			for (HHRouteRegionPointsCtx<T> r : regions) {
				TLongObjectHashMap<T> pnts = null;
				if (r.networkDB != null) {
					pnts = r.networkDB.loadNetworkPoints(r.id, pointClass);
				}
				if (r.file != null) {
					pnts = r.file.initHHPoints(r.fileRegion, r.id, pointClass);
				}
				if (pnts != null) {
					TLongObjectIterator<T> it = pnts.iterator();
					while (it.hasNext()) {
						it.advance();
						T pnt = it.value();
						if (!pnt.incomplete || !points.contains(it.key())) {
							points.put(it.key(), pnt);
						}
					}
				}
			}
			return points;
		}

		public int loadNetworkSegments(Collection<T> valueCollection) throws SQLException {
			int loaded = 0;
			for (HHRouteRegionPointsCtx<T> r : regions) {
				if (r.networkDB != null) {
					loaded += r.networkDB.loadNetworkSegments(valueCollection, r.routingProfile);
				} else {
					throw new UnsupportedOperationException();
				}
			}
			return loaded;
		}

		public boolean loadGeometry(NetworkDBSegment segment, boolean reload) throws SQLException {
			if (!segment.getGeometry().isEmpty() && !reload) {
				return true;
			}
			for (HHRouteRegionPointsCtx<T> r : regions) {
				if (r.networkDB != null && !r.networkDB.compactDB) {
					if (r.networkDB.loadGeometry(segment, r.routingProfile, reload)) {
						return true;
					}
				}
			}
			return false;
		}

		public int loadNetworkSegmentPoint(T point, boolean reverse) throws SQLException, IOException {
			short mapId = point.mapId;
			HHRouteRegionPointsCtx<T> r = regions.get(mapId);
			if (r.networkDB != null) {
				return r.networkDB.loadNetworkSegmentPoint(this, r, point, reverse);
			}
			if (r.file != null) {
				return r.file.loadNetworkSegmentPoint(this, r, point, reverse);
			}
			throw new UnsupportedOperationException();
		}

		public String getRoutingInfo() {
			StringBuilder b = new StringBuilder();
			for (HHRouteRegionPointsCtx<T> r : regions) {
				if(b.length() > 0) {
					b.append(", ");
				}
				if (r.networkDB != null) {
					b.append(String.format("db %s [%s]", r.networkDB.getRoutingProfile(),
							r.networkDB.getRoutingProfiles().get(r.routingProfile)));
				} else if (r.fileRegion != null) {
					b.append(String.format("%s %s [%s]", r.file.getFile().getName(), r.fileRegion.profile,
							r.fileRegion.profileParams.get(r.routingProfile)));
				} else {
					b.append("unknown");
				}
			} 
			return b.toString();
		}
		
		public double distanceToEnd(boolean reverse,  NetworkDBPoint nextPoint) {
			if (config.HEURISTIC_COEFFICIENT > 0) {
				double distanceToEnd = nextPoint.rt(reverse).rtDistanceToEnd;
				if (distanceToEnd == 0) {
					double dist = HHRoutePlanner.squareRootDist31(reverse ? startX : endX, reverse ? startY : endY, 
							nextPoint.midX(), nextPoint.midY());
					distanceToEnd = config.HEURISTIC_COEFFICIENT * dist / rctx.getRouter().getMaxSpeed();
					nextPoint.setDistanceToEnd(reverse, distanceToEnd);
				}
				return distanceToEnd;
			}
			return 0;
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
	
	public static class HHNetworkRouteRes extends RouteCalcResult {

		public RoutingStats stats;
		public List<HHNetworkSegmentRes> segments = new ArrayList<>();
		public List<HHNetworkRouteRes> altRoutes = new ArrayList<>();
		public TLongHashSet uniquePoints = new TLongHashSet();
		
		public HHNetworkRouteRes() {
			super(new ArrayList<RouteSegmentResult>());
		}
		
		public HHNetworkRouteRes(String error) {
			super(error);
		}
		
		
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

		public void append(HHNetworkRouteRes res) {
			if (res == null || res.error != null) {
				this.error = "Can't build a route with intermediate point";
			} else {
				detailed.addAll(res.detailed);
				segments.addAll(res.segments);
				altRoutes.clear();
				uniquePoints.clear();
			}
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
	
	public static <T extends NetworkDBPoint> void setSegments(HHRoutingContext<T> ctx, T point,
			byte[] in, byte[] out) {
		point.connectedSet(true, HHRouteDataStructure.parseSegments(in, ctx.pointsById,
				ctx.getIncomingPoints(point), point, false));
		point.connectedSet(false, HHRouteDataStructure.parseSegments(out, ctx.pointsById,
				ctx.getOutgoingPoints(point), point, true));		
	}
	
	private static List<NetworkDBSegment> parseSegments(byte[] bytes, TLongObjectHashMap<? extends NetworkDBPoint> pntsById,
			List<? extends NetworkDBPoint> lst, NetworkDBPoint pnt, boolean out)  {
		try {
			List<NetworkDBSegment> l = new ArrayList<>();
			if (bytes == null || bytes.length == 0 || pnt.incomplete) {
				return l;
			}
			ByteArrayInputStream str = new ByteArrayInputStream(bytes);
			for (int i = 0; i < lst.size(); i++) {
				int d = CodedInputStream.readRawVarint32(str);
				if (d <= 0) {
					continue;
				}
				double dist = d / 10.0;
				NetworkDBPoint start = out ? pnt : lst.get(i);
				NetworkDBPoint end = out ? lst.get(i) : pnt;
				NetworkDBSegment seg = new NetworkDBSegment(start, end, dist, out, false);
				l.add(seg);
			}
			if (str.available() > 0) {
				System.err.println("Error reading file: " + pnt + " " + out);
			}

			return l;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
	}
	

	static class NetworkDBSegment {
		final boolean direction;
		final NetworkDBPoint start;
		final NetworkDBPoint end;
		final boolean shortcut;
		double dist;
		List<LatLon> geom;
		
		public NetworkDBSegment(NetworkDBPoint start, NetworkDBPoint end, double dist, boolean direction, boolean shortcut) {
			this.direction = direction;
			this.start = start;
			this.end = end;
			this.shortcut = shortcut;
			this.dist = dist;
		}
		
		public List<LatLon> getGeometry() {
			if (geom == null) {
				geom = new ArrayList<LatLon>();
			}
			return geom;
		}
		
		@Override
		public String toString() {
			return String.format("Segment %s -> %s [%.2f] %s", start, end, dist, shortcut ? "sh" : "bs");
		}
		
	}
	
	
	static class NetworkDBPointRouteInfo {
		NetworkDBPoint rtRouteToPoint;
		boolean rtVisited;
		double rtDistanceFromStart;
		int rtDepth = -1; // possibly not needed (used 1)
		double rtDistanceToEnd; // possibly not needed (used 1)
		double rtCost;
		FinalRouteSegment rtDetailedRoute;
		
		public int getDepth(boolean rev) {
			if (rtDepth > 0) {
				return rtDepth;
			}
			if (rtRouteToPoint != null) {
				rtDepth = rtRouteToPoint.rt(rev).getDepth(rev) + 1; 
				return rtDepth ;
			}
			return 0;
		}
		
		public void setDetailedParentRt(FinalRouteSegment r) {
			double segmentDist = r.getDistanceFromStart();
			rtRouteToPoint = null;
			rtCost = rtDistanceToEnd + segmentDist;
			rtDetailedRoute = r;
			rtDistanceFromStart = segmentDist;
		}
		
		public void setCostParentRt(boolean rev, double cost, NetworkDBPoint point, double segmentDist) {
			rtCost = cost;
			rtRouteToPoint = point;
			rtDistanceFromStart = (point == null ? 0 : point.rt(rev).rtDistanceFromStart) + segmentDist;
		}
	}
	
	
	
	public static class NetworkDBPoint {
		public List<TagValuePair> tagValues = null;
		public NetworkDBPoint dualPoint;
		public int index;
		public int clusterId;
		public int fileId;
		public short mapId;
		public boolean incomplete;
		
		public long roadId;
		public short start;
		public short end;
		public int startX;
		public int startY;
		public int endX;
		public int endY;
		
		boolean rtExclude;
		NetworkDBPointRouteInfo rtRev;
		NetworkDBPointRouteInfo rtPos;
		
		List<NetworkDBSegment> connected = new ArrayList<NetworkDBSegment>();
		List<NetworkDBSegment> connectedReverse = new ArrayList<NetworkDBSegment>();
		
		public int midX() {
			return startX / 2 + endX / 2 ;
		}
		
		public int midY() {
			return startY / 2 + endY/ 2 ;
		}
		
		public NetworkDBPointRouteInfo rt(boolean rev) {
			if (rev) {
				if (rtRev == null) {
					rtRev = new NetworkDBPointRouteInfo();
				}
				return rtRev;
			} else {
				if (rtPos == null) {
					rtPos = new NetworkDBPointRouteInfo();
				}
				return rtPos;
			}
		}
		
		public List<NetworkDBSegment> connected(boolean rev) {
			return rev ? connectedReverse : connected;
		}
		
		public void setDistanceToEnd(boolean rev, double segmentDist) {
			rt(rev).rtDistanceToEnd = segmentDist;
		}
		

		public void markVisited(boolean rev) {
			rt(rev).rtVisited = true;
		}
		
		public void connectedSet(boolean rev, List<NetworkDBSegment> l) {
			if (rev) {
				connectedReverse = l;
			} else {
				connected = l;
			}
		}
		
		public void setCostParentRt(boolean reverse, double cost, NetworkDBPoint point, double segmentDist) {
			rt(reverse).setCostParentRt(reverse, cost, point, segmentDist);
		}
		
		public void setDetailedParentRt(boolean reverse, FinalRouteSegment r) {
			rt(reverse).setDetailedParentRt(r);
		}
		
		public void markSegmentsNotLoaded() {
			connected = null;
			connectedReverse = null;
		}
		
		@Override
		public String toString() {
			return String.format("Point %d (%d %d-%d)", index, roadId / 64, start, end);
		}
		
		public LatLon getPoint() {
			return new LatLon(MapUtils.get31LatitudeY(this.startY / 2 + this.endY / 2),
					MapUtils.get31LongitudeX(this.startX / 2 + this.endX / 2));
		}
		
		public NetworkDBSegment getSegment(NetworkDBPoint target, boolean dir) {
			List<NetworkDBSegment> l = (dir ? connected : connectedReverse);
			if (l == null) {
				return null;
			}
			for (NetworkDBSegment s : l) {
				if (dir && s.end == target) {
					return s;
				} else if (!dir && s.start == target) {
					return s;
				}
			}
			return null;
		}

		public void clearRouting() {
			rtExclude = false;
			rtPos = null;
			rtRev = null;
		}

		public int chInd() {
			return 0;
		}

		public int midPntDepth() {
			return 0;
		}

		public long getGeoPntId() {
			return HHRoutePlanner.calculateRoutePointInternalId(roadId, start, end);
		}
		
	}

	static class NetworkDBPointMid extends NetworkDBPoint  {
		
		int rtMidPointDepth = 0;
		
		public int midPntDepth() {
			return rtMidPointDepth;
		}
	}

	static class NetworkDBPointCh extends NetworkDBPoint {
		int chInd;
		
		public int chInd() {
			return chInd;
		}
	}

	
}
