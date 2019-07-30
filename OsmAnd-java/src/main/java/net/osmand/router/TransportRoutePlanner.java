package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportSchedule;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;

public class TransportRoutePlanner {
	
	private static final boolean MEASURE_TIME = false;
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
			route.mergeForwardWays();
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
		
		public List<TransportRouteResultSegment> getSegments() {
			return segments;
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
		
		public RouteCalculationProgress calculationProgress;
		public TLongObjectHashMap<TransportRouteSegment> visitedSegments = new TLongObjectHashMap<TransportRouteSegment>();
		public TransportRoutingConfiguration cfg;
		
		
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
		
		
		
		
		public TransportRoutingContext(TransportRoutingConfiguration cfg, BinaryMapIndexReader... readers) {
			this.cfg = cfg;
			walkRadiusIn31 = (int) (cfg.walkRadius / MapUtils.getTileDistanceWidth(31));
			walkChangeRadiusIn31 = (int) (cfg.walkChangeRadius / MapUtils.getTileDistanceWidth(31));
			quadTree = new TLongObjectHashMap<List<TransportRouteSegment>>();
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
			
			TIntArrayList localFileRoutesToLoad = new TIntArrayList();
			for (BinaryMapIndexReader r : routeMap.keySet()) {
				sr.clearSearchResults();
				List<TransportStop> stops = r.searchTransportIndex(sr);
				
				localFileRoutesToLoad.clear();
				prepareRoutesToLoad(loadedTransportStops, stops, localFileRoutesToLoad);
				
				
				// load routes and create transport segments
				TIntObjectHashMap<TransportRoute> localFileRoutes = new TIntObjectHashMap<TransportRoute>();
				if (localFileRoutesToLoad.size() > 0) {
					localFileRoutesToLoad.sort();
					TIntArrayList referencesToLoad = new TIntArrayList();
					TIntObjectHashMap<TransportRoute> loadedRoutes = routeMap.get(r);
					TIntIterator it = localFileRoutesToLoad.iterator();
					int p = localFileRoutesToLoad.get(0) + 1; // different
					while (it.hasNext()) {
						int nxt = it.next();
						if (p != nxt) {
							if (loadedRoutes.contains(nxt)) {
								localFileRoutes.put(nxt, loadedRoutes.get(nxt));
							} else {
								referencesToLoad.add(nxt);
							}
						}
					}
					r.loadTransportRoutes(referencesToLoad.toArray(), localFileRoutes);
					loadedRoutes.putAll(localFileRoutes);
				}
				
				for (TransportStop stop : stops) {
					long stopId = stop.getId();
					TransportStop multifileStop = loadedTransportStops.get(stopId);
					int[] rrs = stop.getReferencesToRoutes();
					if (multifileStop == stop) {
						// clear up so it won't be used as it is multi file stop
						stop.setReferencesToRoutes(null);
					} else {
						// add other routes
						stop.setReferencesToRoutes(null);
					}
					if(rrs != null && !multifileStop.isDeleted()) {
						for(int i = 0; i < rrs.length; i++) {
							TransportRoute route = localFileRoutes.get(rrs[i]);
							if(route == null) {
								System.err.println(String.format("Something went wrong by loading route %d for stop %s", rrs[i], stop));
							} else if(multifileStop == stop ||
									(!multifileStop.hasRoute(route.getId()) && 
									!multifileStop.isRouteDeleted(route.getId()))){
								// duplicates won't be added
								multifileStop.addRouteId(route.getId());
								multifileStop.addRoute(route);
							}
							
						}
					}
				}
			}
			
			loadTransportSegments(loadedTransportStops.valueCollection(), lst);
			
			readTime += System.nanoTime() - nanoTime;
			return lst;
		}

		private List<TransportStop> prepareRoutesToLoad(TLongObjectHashMap<TransportStop> loadedTransportStops,
				List<TransportStop> stops, TIntArrayList resRoutesToLoad) {
			Iterator<TransportStop> it = stops.iterator();
			while (it.hasNext()) {
				TransportStop stop = it.next();
				long stopId = stop.getId();
				TransportStop multifileStop = loadedTransportStops.get(stopId);
				long[] routesIds = stop.getRoutesIds();
				long[] delRIds = stop.getDeletedRoutesIds();
				if (multifileStop == null) {
					loadedTransportStops.put(stopId, stop);
					if(!stop.isDeleted()) {
						resRoutesToLoad.addAll(stop.getReferencesToRoutes());
					}
				} else if(multifileStop.isDeleted()){
					// stop has noting to load, so not needed
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
								resRoutesToLoad.add(refs[i]);
							}
						}
					} else {
						if (stop.hasReferencesToRoutes()) {
							// old format
							resRoutesToLoad.addAll(stop.getReferencesToRoutes());
						} else {
							// stop has noting to load, so not needed
							it.remove();
						}
					}
				}
			}
			return stops;
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

}
