package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;


import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.IncompleteTransportRoute;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportSchedule;
import net.osmand.data.TransportStop;
import net.osmand.data.TransportStopExit;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;

public class TransportRoutePlanner {
	
	private static final boolean MEASURE_TIME = false;
	private static final int MISSING_STOP_SEARCH_RADIUS = 15000;
	public static final long GEOMETRY_WAY_ID = -1;
	public static final long STOPS_WAY_ID = -2;

	public List<TransportRouteResult> buildRoute(TransportRoutingContext ctx, LatLon start, LatLon end) throws IOException, InterruptedException {
		ctx.startCalcTime = System.currentTimeMillis();
		List<TransportRouteSegment> startStops = ctx.getTransportStops(start);
		List<TransportRouteSegment> endStops = ctx.getTransportStops(end);

		TLongObjectHashMap<TransportRouteSegment> endSegments = new TLongObjectHashMap<TransportRouteSegment>();
		for(TransportRouteSegment s : endStops) {
			endSegments.put(s.getId(), s);
		}
		if(startStops.size() == 0) {
			return Collections.emptyList();
		}
		PriorityQueue<TransportRouteSegment> queue = new PriorityQueue<TransportRouteSegment>(startStops.size(), new SegmentsComparator(ctx));
		for(TransportRouteSegment r : startStops){
			r.walkDist = (float) MapUtils.getDistance(r.getLocation(), start);
			r.distFromStart = r.walkDist / ctx.cfg.walkSpeed;
			queue.add(r);
		}
		
		double finishTime = ctx.cfg.maxRouteTime;
		double maxTravelTimeCmpToWalk = MapUtils.getDistance(start, end) / ctx.cfg.walkSpeed - ctx.cfg.changeTime / 2;
		List<TransportRouteSegment> results = new ArrayList<TransportRouteSegment>();
		initProgressBar(ctx, start, end);
		while (!queue.isEmpty()) {
			long beginMs = MEASURE_TIME ? System.currentTimeMillis() : 0;
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				return null;
			}
			TransportRouteSegment segment = queue.poll();
			TransportRouteSegment ex = ctx.visitedSegments.get(segment.getId());
			if(ex != null) {
				if(ex.distFromStart > segment.distFromStart) {
					System.err.println(String.format("%.1f (%s) > %.1f (%s)", ex.distFromStart, ex, segment.distFromStart, segment));
				}
				continue;
			}
			ctx.visitedRoutesCount++;
			ctx.visitedSegments.put(segment.getId(), segment);
			
			if (segment.getDepth() > ctx.cfg.maxNumberOfChanges + 1) {
				continue;
			}
			if (segment.distFromStart > finishTime + ctx.cfg.finishTimeSeconds || 
					segment.distFromStart > maxTravelTimeCmpToWalk) {
				break;
			}
			long segmentId = segment.getId();
			TransportRouteSegment finish = null;
			double minDist = 0;
			double travelDist = 0;
			double travelTime = 0;
			final float routeTravelSpeed = ctx.cfg.getSpeedByRouteType(segment.road.getType());
			if(routeTravelSpeed == 0) {
				continue;
			}
			TransportStop prevStop = segment.getStop(segment.segStart);
			List<TransportRouteSegment> sgms = new ArrayList<TransportRouteSegment>();
			for (int ind = 1 + segment.segStart; ind < segment.getLength(); ind++) {
				if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
					return null;
				}
				segmentId ++;
				ctx.visitedSegments.put(segmentId, segment);
				TransportStop stop = segment.getStop(ind);
				// could be geometry size
				double segmentDist = MapUtils.getDistance(prevStop.getLocation(), stop.getLocation());
				travelDist += segmentDist;
				if(ctx.cfg.useSchedule) {
					TransportSchedule sc = segment.road.getSchedule();
					int interval = sc.avgStopIntervals.get(ind - 1);
					travelTime += interval * 10;
				} else {
					travelTime += ctx.cfg.stopTime + segmentDist / routeTravelSpeed;
				}
				if(segment.distFromStart + travelTime > finishTime + ctx.cfg.finishTimeSeconds) {
					break;
				}
				sgms.clear();
				sgms = ctx.getTransportStops(stop.x31, stop.y31, true, sgms);
				ctx.visitedStops++;
				for (TransportRouteSegment sgm : sgms) {
					if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
						return null;
					}
					if (segment.wasVisited(sgm)) {
						continue;
					}
					TransportRouteSegment nextSegment = new TransportRouteSegment(sgm);
					nextSegment.parentRoute = segment;
					nextSegment.parentStop = ind;
					nextSegment.walkDist = MapUtils.getDistance(nextSegment.getLocation(), stop.getLocation());
					nextSegment.parentTravelTime = travelTime;
					nextSegment.parentTravelDist = travelDist;
					double walkTime = nextSegment.walkDist / ctx.cfg.walkSpeed
							+ ctx.cfg.getChangeTime() + ctx.cfg.getBoardingTime();
					nextSegment.distFromStart = segment.distFromStart + travelTime + walkTime;
					if(ctx.cfg.useSchedule) {
						int tm = (sgm.departureTime - ctx.cfg.scheduleTimeOfDay) * 10;
						if(tm >= nextSegment.distFromStart) {
							nextSegment.distFromStart = tm;
							queue.add(nextSegment);
						}
					} else {
						queue.add(nextSegment);
					}
				}
				TransportRouteSegment finalSegment = endSegments.get(segmentId);
				double distToEnd = MapUtils.getDistance(stop.getLocation(), end);
				if (finalSegment != null && distToEnd < ctx.cfg.walkRadius) {
					if (finish == null || minDist > distToEnd) {
						minDist = distToEnd;
						finish = new TransportRouteSegment(finalSegment);
						finish.parentRoute = segment;
						finish.parentStop = ind;
						finish.walkDist = distToEnd;
						finish.parentTravelTime = travelTime;
						finish.parentTravelDist = travelDist;

						double walkTime = distToEnd / ctx.cfg.walkSpeed;
						finish.distFromStart = segment.distFromStart + travelTime + walkTime;

					}
				}
				prevStop = stop;
			}
			if (finish != null) {
				if (finishTime > finish.distFromStart) {
					finishTime = finish.distFromStart;
				}
				if(finish.distFromStart < finishTime + ctx.cfg.finishTimeSeconds && 
						(finish.distFromStart < maxTravelTimeCmpToWalk || results.size() == 0)) {
					results.add(finish);
				}
			}
			
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				throw new InterruptedException("Route calculation interrupted");
			}
			if (MEASURE_TIME) {
				long time = System.currentTimeMillis() - beginMs;
				if (time > 10) {
					System.out.println(String.format("%d ms ref - %s id - %d", time, segment.road.getRef(),
							segment.road.getId()));
				}
			}
			updateCalculationProgress(ctx, queue);
			
		}
		return prepareResults(ctx, results);
	}
	
	private void initProgressBar(TransportRoutingContext ctx, LatLon start, LatLon end) {
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.distanceFromEnd = 0;
			ctx.calculationProgress.reverseSegmentQueueSize = 0;
			ctx.calculationProgress.directSegmentQueueSize = 0;
			float speed = (float) ctx.cfg.defaultTravelSpeed + 1; // assume
			ctx.calculationProgress.totalEstimatedDistance = (float) (MapUtils.getDistance(start, end) / speed);
		}
	}

	private void updateCalculationProgress(TransportRoutingContext ctx, PriorityQueue<TransportRouteSegment> queue) {
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.directSegmentQueueSize = queue.size();
			if (queue.size() > 0) {
				TransportRouteSegment peek = queue.peek();
				ctx.calculationProgress.distanceFromBegin = (float) Math.max(peek.distFromStart,
						ctx.calculationProgress.distanceFromBegin);
			}
		}		
	}


	private List<TransportRouteResult> prepareResults(TransportRoutingContext ctx, List<TransportRouteSegment> results) {
		Collections.sort(results, new SegmentsComparator(ctx));
		List<TransportRouteResult> lst = new ArrayList<TransportRouteResult>();
		System.out.println(String.format("Calculated %.1f seconds, found %d results, visited %d routes / %d stops, loaded %d tiles (%d ms read, %d ms total), loaded ways %d (%d wrong)",
				(System.currentTimeMillis() - ctx.startCalcTime) / 1000.0, results.size(), 
				ctx.visitedRoutesCount, ctx.visitedStops, 
				ctx.quadTree.size(), ctx.readTime / (1000 * 1000), ctx.loadTime / (1000 * 1000),
				ctx.loadedWays, ctx.wrongLoadedWays));
		for(TransportRouteSegment res : results) {
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				return null;
			}
			TransportRouteResult route = new TransportRouteResult(ctx);
			route.routeTime = res.distFromStart;
			route.finishWalkDist = res.walkDist;
			TransportRouteSegment p = res;
			while (p != null) {
				if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
					return null;
				}
				if (p.parentRoute != null) {
					TransportRouteResultSegment sg = new TransportRouteResultSegment();
					sg.route = p.parentRoute.road;
					sg.start = p.parentRoute.segStart;
					sg.end = p.parentStop;
					sg.walkDist = p.parentRoute.walkDist;
					sg.walkTime = sg.walkDist / ctx.cfg.walkSpeed;
					sg.depTime = p.departureTime;
					sg.travelDistApproximate = p.parentTravelDist;
					sg.travelTime = p.parentTravelTime;
					route.segments.add(0, sg);
				}
				p = p.parentRoute;
			}
			// test if faster routes fully included
			boolean include = false;
			for(TransportRouteResult s : lst) {
				if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
					return null;
				}
				if(includeRoute(s, route)) {
					include = true;
					break;
				}
			}
			if(!include) {
				lst.add(route);
				System.out.println(route.toString());
			} else {
//				System.err.println(route.toString());
			}
		}
		return lst;
	}

	private boolean includeRoute(TransportRouteResult fastRoute, TransportRouteResult testRoute) {
		if(testRoute.segments.size() < fastRoute.segments.size()) {
			return false;
		}
		int j = 0;
		for(int i = 0; i < fastRoute.segments.size(); i++, j++) {
			TransportRouteResultSegment fs = fastRoute.segments.get(i);
			while(j < testRoute.segments.size()) {
				TransportRouteResultSegment ts = testRoute.segments.get(j);
				if(fs.route.getId().longValue() != ts.route.getId().longValue()) {
					j++;	
				} else {
					break;
				}
			}
			if(j >= testRoute.segments.size()) {
				return false;
			}
		}
		
		return true;
	}

	private static class SegmentsComparator implements Comparator<TransportRouteSegment> {

		public SegmentsComparator(TransportRoutingContext ctx) {
		}

		@Override
		public int compare(TransportRouteSegment o1, TransportRouteSegment o2) {
			return Double.compare(o1.distFromStart, o2.distFromStart);
		}
	}
	
	
	public static class TransportRouteResultSegment {
		
		private static final boolean DISPLAY_FULL_SEGMENT_ROUTE = false;
		private static final int DISPLAY_SEGMENT_IND = 0;
		public TransportRoute route;
		public double walkTime;
		public double travelDistApproximate;
		public double travelTime;
		public int start;
		public int end;
		public double walkDist ;
		public int depTime;
		
		public TransportRouteResultSegment() {
		}
		
		public int getArrivalTime() {
			if(route.getSchedule() != null && depTime != -1) {
				int tm = depTime;
				TIntArrayList intervals = route.getSchedule().avgStopIntervals;
				for(int i = start; i <= end; i++) {
					if(i == end) {
						return tm;
					}
					if(intervals.size() > i) {
						tm += intervals.get(i); 
					} else {
						break;
					}
				}
			}
			return -1;
		}
		
		public double getTravelTime() {
			return travelTime;
		}
		
		public TransportStop getStart() {
			return route.getForwardStops().get(start);
		}
		
		public TransportStop getEnd() {
			return route.getForwardStops().get(end);
		}

		public List<TransportStop> getTravelStops() {
			return route.getForwardStops().subList(start, end + 1);
		}

		public QuadRect getSegmentRect() {
			double left = 0, right = 0;
			double top = 0, bottom = 0;
			for (Node n : getNodes()) {
				if (left == 0 && right == 0) {
					left = n.getLongitude();
					right = n.getLongitude();
					top = n.getLatitude();
					bottom = n.getLatitude();
				} else {
					left = Math.min(left, n.getLongitude());
					right = Math.max(right, n.getLongitude());
					top = Math.max(top, n.getLatitude());
					bottom = Math.min(bottom, n.getLatitude());
				}
			}
			return left == 0 && right == 0 ? null : new QuadRect(left, top, right, bottom);
		}

		public List<Node> getNodes() {
			List<Node> nodes = new ArrayList<>();
			List<Way> ways = getGeometry();
			for (Way way : ways) {
				nodes.addAll(way.getNodes());
			}
			return nodes;
		}

		public List<Way> getGeometry() {
			List<Way> list = new ArrayList<>();
			route.mergeForwardWays(); //TODO merge ways of all Route parts
			if (DISPLAY_FULL_SEGMENT_ROUTE) {
				System.out.println("TOTAL SEGMENTS: " + route.getForwardWays().size());
				if (route.getForwardWays().size() > DISPLAY_SEGMENT_IND) {
					return Collections.singletonList(route.getForwardWays().get(DISPLAY_SEGMENT_IND));
				}
				return route.getForwardWays();				
			}
			List<Way> fw = route.getForwardWays();
			double minStart = 150;
			double minEnd = 150;
			LatLon str = getStart().getLocation();
			LatLon en = getEnd().getLocation();
			int endInd = -1;
			List<Node> res = new ArrayList<>();
			for (int i = 0;  i < fw.size() ; i++) {
				List<Node> nodes = fw.get(i).getNodes();
				for (int j = 0; j < nodes.size(); j++) {
					Node n = nodes.get(j);
					if (MapUtils.getDistance(str, n.getLatitude(), n.getLongitude()) < minStart) {
						minStart = MapUtils.getDistance(str, n.getLatitude(), n.getLongitude());
						res.clear();
					}
					res.add(n);
					if (MapUtils.getDistance(en, n.getLatitude(), n.getLongitude()) < minEnd) {
						endInd = res.size();
						minEnd = MapUtils.getDistance(en, n.getLatitude(), n.getLongitude());
					} 
				}
			}
			Way way;
			if (res.isEmpty() || endInd == -1) {
				way = new Way(STOPS_WAY_ID);
				for (int i = start; i <= end; i++) {
					LatLon l = getStop(i).getLocation();
					Node n = new Node(l.getLatitude(), l.getLongitude(), -1);
					way.addNode(n);
				}
			} else {
				way = new Way(GEOMETRY_WAY_ID);
				for(int k = 0; k < res.size() && k < endInd; k++) {
					way.addNode(res.get(k));
				}
			}
			list.add(way);
			return list;
		}
		
		public double getTravelDist() {
			double d = 0;
			for (int k = start; k < end; k++) {
				d += MapUtils.getDistance(route.getForwardStops().get(k).getLocation(),
						route.getForwardStops().get(k + 1).getLocation());
			}
			return d;
		}

		public TransportStop getStop(int i) {
			return route.getForwardStops().get(i);
		}
	}
	
	public static class TransportRouteResult {
		
		List<TransportRouteResultSegment> segments  = new ArrayList<TransportRouteResultSegment>(4);
		double finishWalkDist;
		double routeTime;
		private final TransportRoutingConfiguration cfg;
		
		public TransportRouteResult(TransportRoutingContext ctx) {
			cfg = ctx.cfg;
		}

		public TransportRouteResult(TransportRoutingConfiguration cfg) {
			this.cfg = cfg;
		}

		public List<TransportRouteResultSegment> getSegments() {
			return segments;
		}

		public void setFinishWalkDist(double finishWalkDist) {
			this.finishWalkDist = finishWalkDist;
		}

		public void setRouteTime(double routeTime) {
			this.routeTime = routeTime;
		}

		public void addSegment(TransportRouteResultSegment seg) {
			segments.add(seg);
		}

		public double getWalkDist() {
			double d = finishWalkDist;
			for (TransportRouteResultSegment s : segments) {
				d += s.walkDist;
			}
			return d;
		}

		public double getFinishWalkDist() {
			return finishWalkDist;
		}

		public double getWalkSpeed() {
			return  cfg.walkSpeed;
		}

		public double getRouteTime() {
			return routeTime;
		}
		
		public int getStops() {
			int stops = 0;
			for(TransportRouteResultSegment s : segments) {
				stops += (s.end - s.start);
			}
			return stops;
		}

		public boolean isRouteStop(TransportStop stop) {
			for(TransportRouteResultSegment s : segments) {
				if (s.getTravelStops().contains(stop)) {
					return true;
				}
			}
			return false;
		}

		public TransportRouteResultSegment getRouteStopSegment(TransportStop stop) {
			for(TransportRouteResultSegment s : segments) {
				if (s.getTravelStops().contains(stop)) {
					return s;
				}
			}
			return null;
		}

		public double getTravelDist() {
			double d = 0;
			for (TransportRouteResultSegment s : segments) {
				d += s.getTravelDist();
			}
			return d;
		}
		
		public double getTravelTime() {
			double t = 0;
			for (TransportRouteResultSegment s : segments) {
				if (cfg.useSchedule) {
					TransportSchedule sts = s.route.getSchedule();
					for (int k = s.start; k < s.end; k++) {
						t += sts.getAvgStopIntervals()[k] * 10;
					}
				} else {
					t += cfg.getBoardingTime();
					t += s.getTravelTime();
				}
			}
			return t;
		}
		
		public double getWalkTime() {
			return getWalkDist() / cfg.walkSpeed;
		}

		public double getChangeTime() {
			return cfg.getChangeTime();
		}

		public double getBoardingTime() {
			return cfg.getBoardingTime();
		}

		public int getChanges() {
			return segments.size() - 1;
		}
		
		@Override
		public String toString() {
			StringBuilder bld = new StringBuilder();
			bld.append(String.format("Route %d stops, %d changes, %.2f min: %.2f m (%.1f min) to walk, %.2f m (%.1f min) to travel\n",
					getStops(), getChanges(), routeTime / 60, getWalkDist(), getWalkTime() / 60.0, 
					getTravelDist(), getTravelTime() / 60.0));
			for(int i = 0; i < segments.size(); i++) {
				TransportRouteResultSegment s = segments.get(i);
				String time = "";
				String arriveTime = "";
				if(s.depTime != -1) {
					time = String.format("at %s", formatTransporTime(s.depTime));
				}
				int aTime = s.getArrivalTime();
				if(aTime != -1) {
					arriveTime = String.format("and arrive at %s", formatTransporTime(aTime));
				}
				bld.append(String.format(" %d. %s: walk %.1f m to '%s' and travel %s to '%s' by %s %d stops %s\n",
						i + 1, s.route.getRef(), s.walkDist, s.getStart().getName(), 
						 time, s.getEnd().getName(),s.route.getName(),  (s.end - s.start), arriveTime));
			}
			bld.append(String.format(" F. Walk %.1f m to reach your destination", finishWalkDist));
			return bld.toString();
		}
	}
	
	public static String formatTransporTime(int i) {
		int h = i / 60 / 6;
		int mh = i - h * 60 * 6;
		int m = mh / 6;
		int s = (mh - m * 6) * 10;
		String tm = String.format("%02d:%02d:%02d ", h, m, s);
		return tm;
	}
	
	public static class TransportRouteSegment {
		
		final int segStart;
		final TransportRoute road;
		final int departureTime;
		private static final int SHIFT = 10; // assume less than 1024 stops
		private static final int SHIFT_DEPTIME = 14; // assume less than 1024 stops
		
		TransportRouteSegment parentRoute = null;
		int parentStop; // last stop to exit for parent route
		double parentTravelTime; // travel time for parent route
		double parentTravelDist; // travel distance for parent route (inaccurate) 
		// walk distance to start route location (or finish in case last segment)
		double walkDist = 0;
		
		// main field accumulated all time spent from beginning of journey
		double distFromStart = 0;
		
		
		
		
		public TransportRouteSegment(TransportRoute road, int stopIndex) {
			this.road = road;
			this.segStart = (short) stopIndex;
			this.departureTime = -1;
		}
		
		public TransportRouteSegment(TransportRoute road, int stopIndex, int depTime) {
			this.road = road;
			this.segStart = (short) stopIndex;
			this.departureTime = depTime;
		}
		
		public TransportRouteSegment(TransportRouteSegment c) {
			this.road = c.road;
			this.segStart = c.segStart;
			this.departureTime = c.departureTime;
		}
		
		
		public boolean wasVisited(TransportRouteSegment rrs) {
			if (rrs.road.getId().longValue() == road.getId().longValue() && 
					rrs.departureTime == departureTime) {
				return true;
			}
			if(parentRoute != null) {
				return parentRoute.wasVisited(rrs);
			}
			return false;
		}


		public TransportStop getStop(int i) {
			return road.getForwardStops().get(i);
		}


		public LatLon getLocation() {
			return road.getForwardStops().get(segStart).getLocation();
		}


		public int getLength() {
			return road.getForwardStops().size();
		}
		
		
		public long getId() {
			long l = road.getId();
			
			l = l << SHIFT_DEPTIME;
			if(departureTime >= (1 << SHIFT_DEPTIME)) {
				throw new IllegalStateException("too long dep time" + departureTime);
			}
			l += (departureTime + 1);

			l = l << SHIFT;
			if (segStart >= (1 << SHIFT)) {
				throw new IllegalStateException("too many stops " + road.getId() + " " + segStart);
			}
			l += segStart;
			
			if(l < 0 ) {
				throw new IllegalStateException("too long id " + road.getId());
			}
			return l  ;
		}

		
		public int getDepth() {
			if(parentRoute != null) {
				return parentRoute.getDepth() + 1;
			}
			return 1;
		}
		
		@Override
		public String toString() {
			return String.format("Route: %s, stop: %s %s", road.getName(), road.getForwardStops().get(segStart).getName(),
					departureTime == -1 ? "" : formatTransporTime(departureTime) );
		}

	}
	
	public static class TransportRoutingContext {
		public NativeLibrary library;
		public RouteCalculationProgress calculationProgress;
		public TLongObjectHashMap<TransportRouteSegment> visitedSegments = new TLongObjectHashMap<TransportRouteSegment>();
		public TransportRoutingConfiguration cfg;
		public TLongObjectHashMap<TransportRoute> combinedRoutesCache = new TLongObjectHashMap<TransportRoute>();
		public Map<TransportStop, List<TransportRoute>> missingStopsCache = new HashMap<TransportStop, List<TransportRoute>>();
		
		public TLongObjectHashMap<List<TransportRouteSegment>> quadTree;
		public final Map<BinaryMapIndexReader, TIntObjectHashMap<TransportRoute>> routeMap = 
				new LinkedHashMap<BinaryMapIndexReader, TIntObjectHashMap<TransportRoute>>();
		
		
		// stats
		public long startCalcTime;
		public int visitedRoutesCount;
		public int visitedStops;
		public int wrongLoadedWays;
		public int loadedWays;
		public long loadTime;
		public long readTime;
		
		
		
		private final int walkRadiusIn31;
		private final int walkChangeRadiusIn31;
		
		
		
		public TransportRoutingContext(TransportRoutingConfiguration cfg, NativeLibrary library, BinaryMapIndexReader... readers) {
			this.cfg = cfg;
			walkRadiusIn31 = (int) (cfg.walkRadius / MapUtils.getTileDistanceWidth(31));
			walkChangeRadiusIn31 = (int) (cfg.walkChangeRadius / MapUtils.getTileDistanceWidth(31));
			quadTree = new TLongObjectHashMap<List<TransportRouteSegment>>();
			this.library = library;
			for (BinaryMapIndexReader r : readers) {
				routeMap.put(r, new TIntObjectHashMap<TransportRoute>());
			}
		}
		
		public List<TransportRouteSegment> getTransportStops(LatLon loc) throws IOException {
			int y = MapUtils.get31TileNumberY(loc.getLatitude());
			int x = MapUtils.get31TileNumberX(loc.getLongitude());
			return getTransportStops(x, y, false, new ArrayList<TransportRouteSegment>());
		}
		
		public List<TransportRouteSegment> getTransportStops(int x, int y, boolean change, List<TransportRouteSegment> res) throws IOException {
			return loadNativeTransportStops(x, y, change, res);
		}

		private List<TransportRouteSegment> loadNativeTransportStops(int sx, int sy, boolean change, List<TransportRouteSegment> res) throws IOException {
			long nanoTime = System.nanoTime();
			int d = change ? walkChangeRadiusIn31 : walkRadiusIn31;
			int lx = (sx - d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
			int rx = (sx + d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
			int ty = (sy - d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
			int by = (sy + d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
			for(int x = lx; x <= rx; x++) {
				for(int y = ty; y <= by; y++) {
					long tileId = (((long)x) << (cfg.ZOOM_TO_LOAD_TILES + 1)) + y;
					List<TransportRouteSegment> list = quadTree.get(tileId);
					if(list == null) {
						list = loadTile(x, y);
						quadTree.put(tileId, list);
					}
					for(TransportRouteSegment r : list) {
						TransportStop st = r.getStop(r.segStart);
						if (Math.abs(st.x31 - sx) > walkRadiusIn31 || Math.abs(st.y31 - sy) > walkRadiusIn31) {
							wrongLoadedWays++;
						} else {
							loadedWays++;
							res.add(r);
						}
					}
				}
			}
			loadTime += System.nanoTime() - nanoTime;
			return res;
		}


		private List<TransportRouteSegment> loadTile(int x, int y) throws IOException {
			long nanoTime = System.nanoTime();
			List<TransportRouteSegment> lst = new ArrayList<TransportRouteSegment>();
			int pz = (31 - cfg.ZOOM_TO_LOAD_TILES);
			SearchRequest<TransportStop> sr = BinaryMapIndexReader.buildSearchTransportRequest(x << pz, (x + 1) << pz,
					y << pz, (y + 1) << pz, -1, null);
			
			// could be global ?
			TLongObjectHashMap<TransportStop> loadedTransportStops = new TLongObjectHashMap<TransportStop>();
			TIntObjectHashMap<TransportRoute> localFileRoutes = new TIntObjectHashMap<>(); //reference, route
			for (BinaryMapIndexReader r : routeMap.keySet()) {
				sr.clearSearchResults();
				List<TransportStop> stops = r.searchTransportIndex(sr);

				localFileRoutes.clear();
				mergeTransportStops(r, loadedTransportStops, stops, localFileRoutes, routeMap.get(r));
					
				for (TransportStop stop : stops) {
					// skip missing stops
					if (stop.isMissingStop()) {
						continue;
					}
					long stopId = stop.getId();
					TransportStop multifileStop = loadedTransportStops.get(stopId);
					int[] rrs = stop.getReferencesToRoutes();
					// TODO what is this?
					if (multifileStop == stop) {
						// clear up so it won't be used as it is multi file stop
						stop.setReferencesToRoutes(null);
					} else {
						// add other routes
						stop.setReferencesToRoutes(null);
					}
					if (rrs != null && !multifileStop.isDeleted()) {
						for (int rr : rrs) {
							TransportRoute route = localFileRoutes.get(rr);
							if (route == null) {
								System.err.println(
										String.format("Something went wrong by loading combined route %d for stop %s", 
												rr, stop));
							} else {								
								TransportRoute combinedRoute = getCombinedRoute(route);
								if (multifileStop == stop || (!multifileStop.hasRoute(combinedRoute.getId()) &&
												!multifileStop.isRouteDeleted(combinedRoute.getId()))) {
									// duplicates won't be added
									multifileStop.addRouteId(combinedRoute.getId());
									multifileStop.addRoute(combinedRoute); 
								}
							}
						}
					}
				}
			}
			//there should go stops with complete routes:
			loadTransportSegments(loadedTransportStops.valueCollection(), lst);
			
			readTime += System.nanoTime() - nanoTime;
			return lst;
		}

		
		public static List<TransportStop> mergeTransportStops(BinaryMapIndexReader reader,
															  TLongObjectHashMap<TransportStop> loadedTransportStops,
															  List<TransportStop> stops,
															  TIntObjectHashMap<TransportRoute> localFileRoutes,
															  TIntObjectHashMap<TransportRoute> loadedRoutes 
															  ) throws IOException {
			TIntArrayList routesToLoad = new TIntArrayList();
			TIntArrayList localRoutesToLoad = new TIntArrayList();
			Iterator<TransportStop> it = stops.iterator();
			while (it.hasNext()) {
				TransportStop stop = it.next();
				long stopId = stop.getId();
				localRoutesToLoad.clear();
				TransportStop multifileStop = loadedTransportStops.get(stopId);
				long[] routesIds = stop.getRoutesIds();
				long[] delRIds = stop.getDeletedRoutesIds();
				if (multifileStop == null) {
					loadedTransportStops.put(stopId, stop);
					multifileStop = stop;
					if(!stop.isDeleted()) {
						localRoutesToLoad.addAll(stop.getReferencesToRoutes());
					}
				} else if (multifileStop.isDeleted()){
					// stop has nothing to load, so not needed
					it.remove();
				} else {
					if (delRIds != null) {
						for (long deletedRouteId : delRIds) {
							multifileStop.addDeletedRouteId(deletedRouteId);
						}
					}
					if (routesIds != null && routesIds.length > 0) {
						int[] refs = stop.getReferencesToRoutes();
						for (int i = 0; i < routesIds.length; i++) {
							long routeId = routesIds[i];
							if (!multifileStop.hasRoute(routeId) && !multifileStop.isRouteDeleted(routeId)) {
								localRoutesToLoad.add(refs[i]);
							}
						}
					} else {
						if (stop.hasReferencesToRoutes()) {
							// old format
							localRoutesToLoad.addAll(stop.getReferencesToRoutes());
						} else {
							// stop has noting to load, so not needed
							it.remove();
						}
					}
				}
				routesToLoad.addAll(localRoutesToLoad);
				multifileStop.putReferencesToRoutes(reader.getFile().getName(), localRoutesToLoad.toArray()); //add valid stop and references to routes 
			}

			// load/combine routes
			if (routesToLoad.size() > 0) {
				routesToLoad.sort();
				TIntArrayList referencesToLoad = new TIntArrayList();
				TIntIterator itr = routesToLoad.iterator();
				int p = routesToLoad.get(0) + 1; // different
				while (itr.hasNext()) {
					int nxt = itr.next();
					if (p != nxt) {
						if (localFileRoutes != null && loadedRoutes != null && loadedRoutes.contains(nxt)) { //check if 
							localFileRoutes.put(nxt, loadedRoutes.get(nxt));
						} else {
							referencesToLoad.add(nxt);
						}
					}
				}
				if (localFileRoutes != null && loadedRoutes != null) {
					reader.loadTransportRoutes(referencesToLoad.toArray(), localFileRoutes);
					loadedRoutes.putAll(localFileRoutes);
				}
			}

			return stops;
		}
		
		private TransportRoute getCombinedRoute(TransportRoute route) throws IOException {
			if (!route.isIncomplete()) {
				return route;
			}
			TransportRoute c = combinedRoutesCache.get(route.getId());
			if (c == null) {
				c = combineRoute(route);
				combinedRoutesCache.put(route.getId(), c);
			}
			return c;
		} 

		private TransportRoute combineRoute(TransportRoute route) throws IOException {
			// 1. Get all available route parts;
			List<TransportRoute> incompleteRoutes = findIncompleteRouteParts(route);
			if (incompleteRoutes == null) {
				return route;
			}
			// here could be multiple overlays between same points
			// It's better to remove them especially identical segments
			List<Way> allWays = getAllWays(incompleteRoutes);
			
			
			// 2. Get array of segments (each array size > 1):
			LinkedList<List<TransportStop>> stopSegments = parseRoutePartsToSegments(incompleteRoutes);
			
			// 3. Merge segments and remove excess missingStops (when they are closer then MISSING_STOP_SEARCH_RADIUS):
			//    + Check for missingStops. If they present in the middle/there more then one segment - we have a hole in the  map data
			List<List<TransportStop>> mergedSegments = combineSegmentsOfSameRoute(stopSegments);
			
			// 4. Now we need to properly sort segments, proper sorting is minimizing distance between stops
			// So it is salesman problem, we have this solution at TspAnt, but if we know last or first segment we can solve it straightforward
			List<TransportStop> firstSegment = null;
			List<TransportStop> lastSegment = null;
			for(List<TransportStop> l : mergedSegments) {
				if(!l.get(0).isMissingStop()) {
					firstSegment = l;
				} 
				if(!l.get(l.size() - 1).isMissingStop()) {
					lastSegment = l;
				}
			}
			List<List<TransportStop>> sortedSegments = new ArrayList<List<TransportStop>>(); 
			if(firstSegment != null) {
				sortedSegments.add(firstSegment);
				while(!mergedSegments.isEmpty()) {
					List<TransportStop> last = sortedSegments.get(sortedSegments.size() - 1);
					List<TransportStop> add = findAndDeleteMinDistance(last.get(last.size() - 1).getLocation(), mergedSegments, true);
					sortedSegments.add(add);
				}
				
			} else if(lastSegment != null) {
				sortedSegments.add(lastSegment);
				while(!mergedSegments.isEmpty()) {
					List<TransportStop> first = sortedSegments.get(0);
					List<TransportStop> add = findAndDeleteMinDistance(first.get(0).getLocation(), mergedSegments, false);
					sortedSegments.add(0, add);
				}
			} else {
				sortedSegments = mergedSegments;
			}
			List<TransportStop> finalList = new ArrayList<TransportStop>();
			for(List<TransportStop> s : sortedSegments) {
				finalList.addAll(s);
			}
			// 5. Create combined TransportRoute and return it
			return new TransportRoute(route, finalList, allWays);
		}

		private List<TransportStop> findAndDeleteMinDistance(LatLon location, List<List<TransportStop>> mergedSegments,
				boolean attachToBegin) {
			int ind = attachToBegin ? 0 : mergedSegments.get(0).size() - 1;
			double minDist = MapUtils.getDistance(mergedSegments.get(0).get(ind).getLocation(), location);
			int minInd = 0;
			for(int i = 1; i < mergedSegments.size(); i++) {
				ind = attachToBegin ? 0 : mergedSegments.get(i).size() - 1;
				double dist = MapUtils.getDistance(mergedSegments.get(i).get(ind).getLocation(), location);
				if(dist < minDist) {
					minInd = i;
				}
			}
			return mergedSegments.remove(minInd);
		}

		private List<Way> getAllWays(List<TransportRoute> parts) {
			List<Way> w = new ArrayList<Way>();
			for (TransportRoute t : parts) {
				w.addAll(t.getForwardWays());
			}
			return w;
		}
		
		
		
		private List<List<TransportStop>> combineSegmentsOfSameRoute(LinkedList<List<TransportStop>> segments) {
			List<List<TransportStop>> resultSegments = new ArrayList<List<TransportStop>>();
			while (!segments.isEmpty()) {
				List<TransportStop> firstSegment = segments.poll();
				boolean merged = true;
				while (merged) {
					merged = false;
					Iterator<List<TransportStop>> it = segments.iterator();
					while (it.hasNext()) {
						List<TransportStop> segmentToMerge = it.next();
						merged = tryToMerge(firstSegment, segmentToMerge);
						if (merged) {
							it.remove();
							break;
						}
					}
				}
				resultSegments.add(firstSegment);
			}
			return resultSegments;
		}	
		
		private boolean tryToMerge(List<TransportStop> firstSegment, List<TransportStop> segmentToMerge) {
			if(firstSegment.size() < 2 || segmentToMerge.size() < 2) {
				return false;
			}
			// 1st we check that segments overlap by stop
			int commonStopFirst = 0;
			int commonStopSecond = 0;
			for(;commonStopFirst < firstSegment.size(); commonStopFirst++) {
				for(; commonStopSecond < segmentToMerge.size(); commonStopSecond++ ) {
					long lid1 = firstSegment.get(commonStopFirst).getId();
					long lid2 = segmentToMerge.get(commonStopSecond).getId();
					if(lid1 > 0 && lid2 == lid1) {
						break;
					}
				}
			}
			if(commonStopFirst < firstSegment.size()) {
				// we've found common stop so we can merge based on stops
				// merge last part first
				if(firstSegment.size() - commonStopFirst < segmentToMerge.size() - commonStopSecond) {
					while(firstSegment.size() > commonStopFirst) {
						firstSegment.remove(firstSegment.size() - 1);
					}
					for(int i = commonStopSecond; i < segmentToMerge.size(); i++) {
						firstSegment.add(segmentToMerge.get(i));
					}
				}
				// merge first part
				if(commonStopFirst < commonStopSecond) {
					for(int i = 0; i < commonStopFirst; i++) {
						firstSegment.remove(0);
					}
					for(int i = commonStopSecond; i >= 0; i--) {
						firstSegment.add(0, segmentToMerge.get(i));
					}	
				}
				return true;
				
			}
			// no common stops, so try to connect to the end or beginning
			// beginning
			boolean merged = false;
			if (MapUtils.getDistance(firstSegment.get(0).getLocation(),
					segmentToMerge.get(segmentToMerge.size() - 1).getLocation()) < MISSING_STOP_SEARCH_RADIUS) {
				firstSegment.remove(0);
				for(int i = segmentToMerge.size() - 2; i >= 0; i--) {
					firstSegment.add(0, segmentToMerge.get(i));
				}
				merged = true;
			} else if(MapUtils.getDistance(firstSegment.get(firstSegment.size() - 1).getLocation(),
					segmentToMerge.get(0).getLocation()) < MISSING_STOP_SEARCH_RADIUS) {
				firstSegment.remove(firstSegment.size() - 1);
				for(int i = 1; i < segmentToMerge.size(); i++) {
					firstSegment.add(segmentToMerge.get(i));
				}
				merged = true;
			}
			return merged;
		}

		
		
		private LinkedList<List<TransportStop>> parseRoutePartsToSegments(List<TransportRoute> routeParts) {
			LinkedList<List<TransportStop>> segs = new LinkedList<List<TransportStop>>();
			// here we assume that missing stops come in pairs <A, B, C, MISSING, MISSING, D, E...>
			// TODO check generation that are doubles
			for (TransportRoute part : routeParts) {
				List<TransportStop> newSeg = new ArrayList<TransportStop>();
				for (TransportStop s : part.getForwardStops()) {
					newSeg.add(s);
					if (s.isMissingStop()) {
						if (newSeg.size() > 1) {
							segs.add(newSeg);
							newSeg = new ArrayList<TransportStop>();
						}
					}
				}
				if (newSeg.size() > 1) {
					segs.add(newSeg);
				}
			}
			return segs;
		}
		
		private List<TransportRoute> findIncompleteRouteParts(TransportRoute baseRoute) throws IOException {
			List<TransportRoute> allRoutes = null;
			// TODO completely irrelevant always reiteration over all maps (especially not in bbox of the route probabl)
			for (BinaryMapIndexReader bmir : routeMap.keySet()) {
				IncompleteTransportRoute ptr = bmir.getIncompleteTransportRoutes().get(baseRoute.getId());
				if (ptr != null) {
					TIntArrayList lst = new TIntArrayList();
					while(ptr != null) {
						lst.add(ptr.getRouteOffset());
						ptr = ptr.getNextLinkedRoute();
					}
					if(lst.size() > 0) {
						if(allRoutes == null) {
							allRoutes = new ArrayList<TransportRoute>();
						}
						allRoutes.addAll(bmir.getTransportRoutes(lst.toArray()).valueCollection());
					}
				}
			}
			return allRoutes;
		}
		
		private void loadTransportSegments(Collection<TransportStop> stops, List<TransportRouteSegment> lst) throws IOException {
			for(TransportStop s : stops) {
				if (s.isDeleted() || s.getRoutes() == null) {
					continue;
				}
				for (TransportRoute route : s.getRoutes()) {
					int stopIndex = -1;
					double dist = TransportRoute.SAME_STOP;
					for (int k = 0; k < route.getForwardStops().size(); k++) {
						TransportStop st = route.getForwardStops().get(k);
						if(st.getId().longValue() == s.getId().longValue() ) {
							stopIndex = k;
							break;
						}
						double d = MapUtils.getDistance(st.getLocation(), s.getLocation());
						if (d < dist) {
							stopIndex = k;
							dist = d;
						}
					}
					if (stopIndex != -1) {
						if (cfg.useSchedule) {
							loadScheduleRouteSegment(lst, route, stopIndex);
						} else {
							TransportRouteSegment segment = new TransportRouteSegment(route, stopIndex);
							lst.add(segment);
						}
					} else {
						// MapUtils.getDistance(s.getLocation(), route.getForwardStops().get(158).getLocation());
						System.err.println(String.format("Routing error: missing stop '%s' in route '%s' id: %d",
								s.toString(), route.getRef(), route.getId() / 2));
					}
				}
			}
		}

		private void loadScheduleRouteSegment(List<TransportRouteSegment> lst, TransportRoute route, int stopIndex) {
			if(route.getSchedule() != null) {
				TIntArrayList ti = route.getSchedule().tripIntervals;
				int cnt = ti.size();
				int t = 0;
				// improve by using exact data
				int stopTravelTime = 0;
				TIntArrayList avgStopIntervals = route.getSchedule().avgStopIntervals;
				for (int i = 0; i < stopIndex; i++) {
					if (avgStopIntervals.size() > i) {
						stopTravelTime += avgStopIntervals.getQuick(i);
					}
				}
				for(int i = 0; i < cnt; i++) {
					t += ti.getQuick(i);
					int startTime = t + stopTravelTime;
					if(startTime >= cfg.scheduleTimeOfDay && startTime <= cfg.scheduleTimeOfDay + cfg.scheduleMaxTime ) {
						TransportRouteSegment segment = new TransportRouteSegment(route, stopIndex, startTime);
						lst.add(segment);
					}
				}
			}
		}
	}

	public static List<TransportRouteResult> convertToTransportRoutingResult(NativeTransportRoutingResult[] res,
	                                                                             TransportRoutingConfiguration cfg) {
		//cache for converted TransportRoutes:
		TLongObjectHashMap<TransportRoute> convertedRoutesCache = new TLongObjectHashMap<>();
		TLongObjectHashMap<TransportStop> convertedStopsCache = new TLongObjectHashMap<>();

		if (res.length == 0) {
			return new ArrayList<TransportRouteResult>();
		}
		List<TransportRouteResult> convertedRes = new ArrayList<TransportRouteResult>();
		for (NativeTransportRoutingResult ntrr : res) {
			TransportRouteResult trr = new TransportRouteResult(cfg);
			trr.setFinishWalkDist(ntrr.finishWalkDist);
			trr.setRouteTime(ntrr.routeTime);

			for (NativeTransportRouteResultSegment ntrs : ntrr.segments) {
				TransportRouteResultSegment trs = new TransportRouteResultSegment();
				trs.route = convertTransportRoute(ntrs.route, convertedRoutesCache, convertedStopsCache);
				trs.walkTime = ntrs.walkTime;
				trs.travelDistApproximate = ntrs.travelDistApproximate;
				trs.travelTime = ntrs.travelTime;
				trs.start = ntrs.start;
				trs.end = ntrs.end;
				trs.walkDist = ntrs.walkDist;
				trs.depTime = ntrs.depTime;

				trr.addSegment(trs);
			}
			convertedRes.add(trr);
		}
		convertedStopsCache.clear();
		convertedRoutesCache.clear();
		return convertedRes;
	}

	private static TransportRoute convertTransportRoute(NativeTransportRoute nr,
	                                                    TLongObjectHashMap<TransportRoute> convertedRoutesCache,
	                                                    TLongObjectHashMap<TransportStop> convertedStopsCache) {
		TransportRoute r = new TransportRoute();
		r.setId(nr.id);
		r.setLocation(nr.routeLat, nr.routeLon);
		r.setName(nr.name);
		r.setEnName(nr.enName);
		if (nr.namesLng.length > 0 && nr.namesLng.length == nr.namesNames.length) {
			for (int i = 0; i < nr.namesLng.length; i++) {
				r.setName(nr.namesLng[i], nr.namesNames[i]);
			}
		}
		r.setFileOffset(nr.fileOffset);
		r.setForwardStops(convertTransportStops(nr.forwardStops, convertedStopsCache));
		r.setRef(nr.ref);
		r.setOperator(nr.routeOperator);
		r.setType(nr.type);
		r.setDist(nr.dist);
		r.setColor(nr.color);

		if (nr.intervals != null && nr.intervals.length > 0 && nr.avgStopIntervals !=null
				&& nr.avgStopIntervals.length > 0 && nr.avgWaitIntervals != null && nr.avgWaitIntervals.length > 0) {
			r.setSchedule(new TransportSchedule(new TIntArrayList(nr.intervals),
					new TIntArrayList(nr.avgStopIntervals), new TIntArrayList(nr.avgWaitIntervals)));
		}

		for (int i = 0; i < nr.waysIds.length; i++) {
			List<Node> wnodes = new ArrayList<>();
			for (int j = 0; j < nr.waysNodesLats[i].length; j++) {
				wnodes.add(new Node(nr.waysNodesLats[i][j], nr.waysNodesLons[i][j], -1));
			}
			r.addWay(new Way(nr.waysIds[i], wnodes));
		}

		if (convertedRoutesCache.get(r.getId()) == null) {
			convertedRoutesCache.put(r.getId(), r);
		}
		return r;
	}

	private static List<TransportStop> convertTransportStops(NativeTransportStop[] nstops,
	                                                         TLongObjectHashMap<TransportStop> convertedStopsCache) {
		List<TransportStop> stops = new ArrayList<>();
		for (NativeTransportStop ns : nstops) {
			if (convertedStopsCache != null && convertedStopsCache.get(ns.id) != null) {
				stops.add(convertedStopsCache.get(ns.id));
				continue;
			}
			TransportStop s = new TransportStop();
			s.setId(ns.id);
			s.setLocation(ns.stopLat, ns.stopLon);
			s.setName(ns.name);
			s.setEnName(ns.enName);
			if (ns.namesLng.length > 0 && ns.namesLng.length == ns.namesNames.length) {
				for (int i = 0; i < ns.namesLng.length; i++) {
					s.setName(ns.namesLng[i], ns.namesNames[i]);
				}
			}
			s.setFileOffset(ns.fileOffset);
			s.setReferencesToRoutes(ns.referencesToRoutes);
			s.setDeletedRoutesIds(ns.deletedRoutesIds);
			s.setRoutesIds(ns.routesIds);
			s.distance = ns.distance;
			s.x31 = ns.x31;
			s.y31 = ns.y31;

			if (ns.pTStopExit_refs != null && ns.pTStopExit_refs.length > 0) {
				for (int i = 0; i < ns.pTStopExit_refs.length; i++) {
					s.addExit(new TransportStopExit(ns.pTStopExit_x31s[i],
							ns.pTStopExit_y31s[i], ns.pTStopExit_refs[i]));
				}
			}

			if (ns.referenceToRoutesKeys != null && ns.referenceToRoutesKeys.length > 0) {
				for (int i = 0; i < ns.referenceToRoutesKeys.length; i++) {
					s.putReferencesToRoutes(ns.referenceToRoutesKeys[i], ns.referenceToRoutesVals[i]);
				}
			}
			if (convertedStopsCache == null) {
				convertedStopsCache = new TLongObjectHashMap<>();
			}
			if (convertedStopsCache.get(s.getId()) == null) {
				convertedStopsCache.put(s.getId(), s);
			}
			stops.add(s);
		}
		return stops;
	}
}
