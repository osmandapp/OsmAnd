package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;


import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

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
	
	private static final int MIN_DIST_STOP_TO_GEOMETRY = 150;
	public static final long GEOMETRY_WAY_ID = -1;
	public static final long STOPS_WAY_ID = -2;

	public List<TransportRouteResult> buildRoute(TransportRoutingContext ctx, LatLon start, LatLon end) throws IOException, InterruptedException {
		ctx.startCalcTime = System.currentTimeMillis();
		double totalDistance = MapUtils.getDistance(start, end);
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
		ctx.finishTimeSeconds = ctx.cfg.finishTimeSeconds;
		if (totalDistance > ctx.cfg.maxRouteDistance && ctx.cfg.maxRouteIncreaseSpeed > 0)  {
			int increaseTime = (int) ((totalDistance - ctx.cfg.maxRouteDistance) 
					* 3.6 / ctx.cfg.maxRouteIncreaseSpeed);
			finishTime += increaseTime;
			ctx.finishTimeSeconds += increaseTime / 6;
		}
		double maxTravelTimeCmpToWalk = totalDistance / ctx.cfg.walkSpeed - ctx.cfg.changeTime / 2;
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
			
			if (segment.distFromStart > finishTime + ctx.finishTimeSeconds ||
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
				if(segment.distFromStart + travelTime > finishTime + ctx.finishTimeSeconds) {
					break;
				}
				sgms.clear();
				if (segment.getDepth() < ctx.cfg.maxNumberOfChanges + 1) {
					sgms = ctx.getTransportStops(stop.x31, stop.y31, true, sgms);
					ctx.visitedStops++;
					for (TransportRouteSegment sgm : sgms) {
						if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
							return null;
						}
						if (segment.wasVisited(sgm)) {
							continue;
						}
						if (ctx.visitedSegments.containsKey(sgm.getId())) {
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
						if (ctx.cfg.useSchedule) {
							int tm = (sgm.departureTime - ctx.cfg.scheduleTimeOfDay) * 10;
							if (tm >= nextSegment.distFromStart) {
								nextSegment.distFromStart = tm;
								queue.add(nextSegment);
							}
						} else {
							queue.add(nextSegment);
						}
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
				if(finish.distFromStart < finishTime + ctx.finishTimeSeconds && 
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
		System.out.println(String.format(Locale.US, "Calculated %.1f seconds, found %d results, visited %d routes / %d stops, loaded %d tiles (%d ms read, %d ms total), loaded ways %d (%d wrong)",
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
		
		private static class SearchNodeInd { 
			int ind = -1;
			Way way = null;
			double dist = MIN_DIST_STOP_TO_GEOMETRY;
		}

		public List<Way> getGeometry() {
			route.mergeForwardWays();
			if (DISPLAY_FULL_SEGMENT_ROUTE) {
				System.out.println("TOTAL SEGMENTS: " + route.getForwardWays().size());
				if (route.getForwardWays().size() > DISPLAY_SEGMENT_IND && DISPLAY_SEGMENT_IND != -1) {
					return Collections.singletonList(route.getForwardWays().get(DISPLAY_SEGMENT_IND));
				}
				return route.getForwardWays();				
			}
			List<Way> ways = route.getForwardWays();
			
			final LatLon startLoc = getStart().getLocation();
			final LatLon endLoc = getEnd().getLocation();
			SearchNodeInd startInd = new SearchNodeInd();
			SearchNodeInd endInd = new SearchNodeInd();
			for (int i = 0;  i < ways.size() ; i++) {
				List<Node> nodes = ways.get(i).getNodes();
				for (int j = 0; j < nodes.size(); j++) {
					Node n = nodes.get(j);
					if (MapUtils.getDistance(startLoc, n.getLatitude(), n.getLongitude()) < startInd.dist) {
						startInd.dist = MapUtils.getDistance(startLoc, n.getLatitude(), n.getLongitude());
						startInd.ind = j;
						startInd.way = ways.get(i);
					}
					if (MapUtils.getDistance(endLoc, n.getLatitude(), n.getLongitude()) < endInd.dist) {
						endInd.dist = MapUtils.getDistance(endLoc, n.getLatitude(), n.getLongitude());
						endInd.ind = j;
						endInd.way = ways.get(i);
					} 
				}
			}
			boolean validOneWay = startInd.way != null && startInd.way == endInd.way && startInd.ind <= endInd.ind;
			if (validOneWay) {
				Way way = new Way(GEOMETRY_WAY_ID);
				for (int k = startInd.ind; k <= endInd.ind; k++) {
					way.addNode(startInd.way.getNodes().get(k));
				}
				return Collections.singletonList(way);
			}
			boolean validContinuation = startInd.way != null && endInd.way != null &&
					startInd.way != endInd.way;
			if (validContinuation) {
				Node ln = startInd.way.getLastNode();
				Node fn = endInd.way.getFirstNode();
				// HERE we need to check other ways for continuation
				if (ln != null && fn != null && MapUtils.getDistance(ln.getLatLon(), fn.getLatLon()) < TransportStopsRouteReader.MISSING_STOP_SEARCH_RADIUS) {
					validContinuation = true;
				} else {
					validContinuation = false;
				}
			}
			if (validContinuation) {
				List<Way> two = new ArrayList<Way>();
				Way way = new Way(GEOMETRY_WAY_ID);
				for (int k = startInd.ind; k < startInd.way.getNodes().size(); k++) {
					way.addNode(startInd.way.getNodes().get(k));
				}
				two.add(way);
				way = new Way(GEOMETRY_WAY_ID);
				for (int k = 0; k <= endInd.ind; k++) {
					way.addNode(endInd.way.getNodes().get(k));
				}
				two.add(way);
				return two;
			}
			Way way = new Way(STOPS_WAY_ID);
			for (int i = start; i <= end; i++) {
				LatLon l = getStop(i).getLocation();
				Node n = new Node(l.getLatitude(), l.getLongitude(), -1);
				way.addNode(n);
			}
			return Collections.singletonList(way);
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

	public static String formatTransportTime(int i) {
		int h = i / 60 / 6;
		int mh = i - h * 60 * 6;
		int m = mh / 6;
		int s = (mh - m * 6) * 10;
		return String.format(Locale.US, "%02d:%02d:%02d ", h, m, s);
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
					departureTime == -1 ? "" : formatTransportTime(departureTime) );
		}
	}

	public static List<TransportRouteResult> convertToTransportRoutingResult(NativeTransportRoutingResult[] res,
			TransportRoutingConfiguration cfg) {
		// cache for converted TransportRoutes:
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

		if (nr.intervals != null && nr.intervals.length > 0 && nr.avgStopIntervals != null
				&& nr.avgStopIntervals.length > 0 && nr.avgWaitIntervals != null && nr.avgWaitIntervals.length > 0) {
			r.setSchedule(new TransportSchedule(new TIntArrayList(nr.intervals), new TIntArrayList(nr.avgStopIntervals),
					new TIntArrayList(nr.avgWaitIntervals)));
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
			// convert to long as C++ doesn't support int
			if (ns.referencesToRoutes != null) {
				long[] r = new long[ns.referencesToRoutes.length];
				for (int k = 0; k < r.length; k++) {
					r[k] = ns.referencesToRoutes[k];
				}
				s.setReferencesToRoutes(r);
			}
			s.setDeletedRoutesIds(ns.deletedRoutesIds);
			s.setRoutesIds(ns.routesIds);
			s.distance = ns.distance;
			s.x31 = ns.x31;
			s.y31 = ns.y31;

			if (ns.pTStopExit_refs != null && ns.pTStopExit_refs.length > 0) {
				for (int i = 0; i < ns.pTStopExit_refs.length; i++) {
					s.addExit(
							new TransportStopExit(ns.pTStopExit_x31s[i], ns.pTStopExit_y31s[i], ns.pTStopExit_refs[i]));
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
