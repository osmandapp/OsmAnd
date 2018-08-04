package net.osmand.router;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.util.MapUtils;

public class TransportRoutePlanner {
	

	public static void main(String[] args) throws IOException {
		File fl = new File(System.getProperty("maps.dir"), "Netherlands_noord-holland_europe_2.obf");
		RandomAccessFile raf = new RandomAccessFile(fl, "r");
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf, fl);
		
		LatLon start = new LatLon(52.28094, 4.853248);
//		LatLon end = new LatLon(52.320988, 4.87256);
		LatLon end = new LatLon(52.349308, 4.9017425);
		
		TransportRoutingConfiguration cfg = new TransportRoutingConfiguration();
		cfg.maxNumberOfChanges = 3;
		cfg.walkRadius = 1500;
//		cfg.walkChangeRadius = 500;
		TransportRoutingContext ctx = new TransportRoutingContext(cfg, reader);
		TransportRoutePlanner planner = new TransportRoutePlanner();
		planner.buildRoute(ctx, start, end);
		
	}

	private List<TransportRouteResult> buildRoute(TransportRoutingContext ctx, LatLon start, LatLon end) throws IOException {
		ctx.startCalcTime = System.currentTimeMillis();
		List<TransportRouteSegment> startStops = ctx.getTransportStops(start);
		List<TransportRouteSegment> endStops = ctx.getTransportStops(end);
		
		TLongObjectHashMap<TransportRouteSegment> endSegments = new TLongObjectHashMap<TransportRouteSegment>();
		for(TransportRouteSegment s : endStops) {
			endSegments.put(s.getId(), s);
		}
		PriorityQueue<TransportRouteSegment> queue = new PriorityQueue<TransportRouteSegment>(new SegmentsComparator(ctx));
		for(TransportRouteSegment r : startStops){
			r.walkDist = (float) MapUtils.getDistance(r.getLocation(), start);
			r.distFromStart = r.walkDist / ctx.cfg.walkSpeed;
			queue.add(r);
		}
		double finishTime = ctx.cfg.maxRouteTime;
		List<TransportRouteSegment> results = new ArrayList<TransportRouteSegment>();
		
		while (!queue.isEmpty()) {
			TransportRouteSegment segment = queue.poll();
			TransportRouteSegment ex = ctx.visitedSegments.get(segment.getId());
			if(ex != null) {
				if(ex.distFromStart > segment.distFromStart) {
					System.err.println(String.format("%.1f (%s) > %.1f (%s)", ex.distFromStart, ex, segment.distFromStart, segment));
				}
				continue;
			}
			ctx.visitedSegmentsCount++;
			ctx.visitedSegments.put(segment.getId(), segment);
			if (segment.getDepth() > ctx.cfg.maxNumberOfChanges) {
				continue;
			}
			if (segment.distFromStart > finishTime + ctx.cfg.finishTimeSeconds) {
				break;
			}
			long segmentId = segment.getId();
			TransportRouteSegment finish = null;
			double minDist = 0;
			double travelDist = 0;
			double travelTime = 0;
			TransportStop prevStop = segment.getStop(segment.segStart);
			for (int ind = 1 + segment.segStart; ind < segment.getLength(); ind++) {
				segmentId ++; 
				ctx.visitedSegments.put(segmentId, segment);
				TransportStop stop = segment.getStop(ind);
				// could be geometry size
				double segmentDist = MapUtils.getDistance(prevStop.getLocation(), stop.getLocation());
				travelDist += segmentDist;
				travelTime += ctx.cfg.stopTime + segmentDist / ctx.cfg.travelSpeed;
				List<TransportRouteSegment> sgms = ctx.getTransportStops(stop.x31, stop.y31, true);
				for (TransportRouteSegment sgm : sgms) {
					if (segment.wasVisited(sgm)) {
						continue;
					}
					TransportRouteSegment rrs = new TransportRouteSegment(sgm);
					rrs.parentRoute = segment;
					rrs.parentStop = ind;
					rrs.walkDist = MapUtils.getDistance(rrs.getLocation(), stop.getLocation());
					rrs.parentTravelTime = travelTime;
					rrs.parentTravelDist = travelDist;

					double walkTime = rrs.walkDist / ctx.cfg.walkSpeed + ctx.cfg.changeTime;
					rrs.distFromStart = segment.distFromStart + travelTime + walkTime;

					queue.add(rrs);
				}
				TransportRouteSegment f = endSegments.get(segmentId);
				double distToEnd = MapUtils.getDistance(stop.getLocation(), end);
				if (f != null && distToEnd < ctx.cfg.walkRadius) {
					if (finish == null || minDist > distToEnd) {
						minDist = distToEnd;
						finish = new TransportRouteSegment(f);
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
				if(finish.distFromStart < finishTime + ctx.cfg.finishTimeSeconds) {
					results.add(finish);
				}
			}
		}
		
		return prepareResults(ctx, results);
	}

	private List<TransportRouteResult> prepareResults(TransportRoutingContext ctx, List<TransportRouteSegment> results) {
		Collections.sort(results, new SegmentsComparator(ctx));
		List<TransportRouteResult> lst = new ArrayList<TransportRouteResult>();
		System.out.println("FIX !!! " + ctx.wrongLoadedWays + " " + ctx.loadedWays + " " + 
					(ctx.loadTime / (1000 * 1000)) + " ms");
		System.out.println(String.format("Calculated %.1f seconds, found %d results, visited %d segments, loaded %d tiles ",
				(System.currentTimeMillis() - ctx.startCalcTime) / 1000.0, results.size(), ctx.visitedSegmentsCount, 
				ctx.loadedTiles.size()));
		for(TransportRouteSegment res : results) {
			TransportRouteResult route = new TransportRouteResult(ctx);
			route.routeTime = res.distFromStart;
			route.finishWalkDist = res.walkDist;
			TransportRouteSegment p = res;
			while (p != null) {
				if (p.parentRoute != null) {
					TransportRouteResultSegment sg = new TransportRouteResultSegment(p.parentRoute.road, 
							p.parentRoute.segStart, p.parentStop, p.parentRoute.walkDist);
					route.segments.add(0, sg);
				}
				p = p.parentRoute;
			}
			// test if faster routes fully included
			boolean include = false;
			for(TransportRouteResult s : lst) {
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
		public final TransportRoute route;
		public final int start;
		public final int end;
		public final double walkDist ;
		
		
		public TransportRouteResultSegment(TransportRoute route, int start, int end, double walkDist) {
			this.route = route;
			this.start = start;
			this.end = end;
			this.walkDist = walkDist;
		}
		
		public TransportStop getStart() {
			return route.getForwardStops().get(start);
		}
		
		public TransportStop getEnd() {
			return route.getForwardStops().get(end);
		}
		
		public double getTravelDist() {
			double d = 0;
			for (int k = start; k < end; k++) {
				d += MapUtils.getDistance(route.getForwardStops().get(k).getLocation(),
						route.getForwardStops().get(k + 1).getLocation());
			}
			return d;
		}
	}
	
	public static class TransportRouteResult {
		
		List<TransportRouteResultSegment> segments  = new ArrayList<TransportRouteResultSegment>(4);
		double finishWalkDist;
		double routeTime;
		private final double walkSpeed;
		private final double travelSpeed;
		
		public TransportRouteResult(TransportRoutingContext ctx) {
			walkSpeed = ctx.cfg.walkSpeed;
			travelSpeed = ctx.cfg.travelSpeed;
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
		
		public int getStops() {
			int stops = 0;
			for(TransportRouteResultSegment s : segments) {
				stops += (s.end - s.start);
			}
			return stops;
		}
		
		public double getTravelDist() {
			double d = 0;
			for (TransportRouteResultSegment s : segments) {
				d += s.getTravelDist();
			}
			return d;
		}
		
		public int getChanges() {
			return segments.size() - 1;
		}
		
		@Override
		public String toString() {
			StringBuilder bld = new StringBuilder();
			bld.append(String.format("Route %d stops, %d changes, %.2f min: %.2f m (%.1f min) to walk, %.2f m (%.1f min) to travel\n",
					getStops(), getChanges(), routeTime / 60, getWalkDist(), getWalkDist() / walkSpeed / 60.0, 
					getTravelDist(), getTravelDist() / travelSpeed / 60.0));
			for(int i = 0; i < segments.size(); i++) {
				TransportRouteResultSegment s = segments.get(i);
				bld.append(String.format(" %d. %s: walk %.1f m to '%s' and travel to '%s' by %s %d stops \n",
						i + 1, s.route.getRef(), s.walkDist, s.getStart().getName(), s.getEnd().getName(), s.route.getName(),  (s.end - s.start)));
			}
			bld.append(String.format(" F. Walk %.1f m to reach your destination", finishWalkDist));
			return bld.toString();
		}
		
		
	}
	
	public static class TransportRouteSegment {
		
		final int segStart;
		final TransportRoute road;
		private static final int SHIFT = 10; // assume less than 1024 stops
		
		TransportRouteSegment parentRoute = null;
		int parentStop;
		double parentTravelTime; // travel time
		double parentTravelDist; // inaccurate
		// walk distance to start route location (or finish in case last segment)
		double walkDist = 0;
		
		// main field accumulated all time spent from beginning of journey
		double distFromStart = 0;
		
		
		public TransportRouteSegment(TransportRoute road, int stopIndex) {
			this.road = road;
			this.segStart = (short) stopIndex;
		}
		
		public TransportRouteSegment(TransportRouteSegment c) {
			this.road = c.road;
			this.segStart = c.segStart;
		}
		
		
		public boolean wasVisited(TransportRouteSegment rrs) {
			if (rrs.road.getId().longValue() == road.getId().longValue()) {
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
			long l = road.getId() << SHIFT;
			if(l < 0 ) {
				throw new IllegalStateException("too long id " + road.getId());
			}
			if(segStart >= (1 << SHIFT)) {
				throw new IllegalStateException("too many stops " + road.getId() + " " + segStart);
			}
			return l  + segStart;
		}

		
		public int getDepth() {
			if(parentRoute != null) {
				return parentRoute.getDepth() + 1;
			}
			return 1;
		}
		
		@Override
		public String toString() {
			return String.format("Route: %s, stop: %s", road.getName(), road.getForwardStops().get(segStart).getName());
		}

	}
	
	public static class TransportRoutingContext {
		
		public long startCalcTime;
		public int visitedSegmentsCount;
		public int wrongLoadedWays;
		public int loadedWays;
		public long loadTime;
		
		public RouteCalculationProgress calculationProgress;
		public TLongObjectHashMap<TransportRouteSegment> visitedSegments = new TLongObjectHashMap<TransportRouteSegment>();
		
		public TLongObjectHashMap<TransportStop> loadedTransportStops = new TLongObjectHashMap<TransportStop>();
		private TIntArrayList loadedTiles = new TIntArrayList();
		public TransportRoutingConfiguration cfg;
		
		
		
		public QuadTree<TransportRouteSegment> quadTree ;
		public final Map<BinaryMapIndexReader, TIntObjectHashMap<TransportRoute>> map = 
				new LinkedHashMap<BinaryMapIndexReader, TIntObjectHashMap<TransportRoute>>();
		
		private final int walkRadiusIn31;
		private final int walkChangeRadiusIn31;
		
		
		
		
		public TransportRoutingContext(TransportRoutingConfiguration cfg, BinaryMapIndexReader... readers) {
			this.cfg = cfg;
			walkRadiusIn31 = (int) (cfg.walkRadius / MapUtils.getTileDistanceWidth(31));
			walkChangeRadiusIn31 = (int) (cfg.walkChangeRadius / MapUtils.getTileDistanceWidth(31));
			quadTree = 
					new QuadTree<TransportRouteSegment>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE), cfg.ZOOM_TO_LOAD_TILES, 0.55f);
			for (BinaryMapIndexReader r : readers) {
				map.put(r, new TIntObjectHashMap<TransportRoute>());
			}
		}
		
		public List<TransportRouteSegment> getTransportStops(LatLon loc) throws IOException {
			int y = MapUtils.get31TileNumberY(loc.getLatitude());
			int x = MapUtils.get31TileNumberX(loc.getLongitude());
			return getTransportStops(x, y, false);
		}
		
		public List<TransportRouteSegment> getTransportStops(int x, int y, boolean change) throws IOException {
			return loadNativeTransportStops(x, y, change);
		}

		private List<TransportRouteSegment> loadNativeTransportStops(int sx, int sy, boolean change) throws IOException {
			long nanoTime = System.nanoTime();
			List<TransportRouteSegment> allstops = new LinkedList<TransportRouteSegment>();
			int d = change ? walkChangeRadiusIn31 : walkRadiusIn31;
			int lx = (sx - d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
			int rx = (sx + d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
			int ty = (sy - d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
			int by = (sy + d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
			for(int x = lx; x <= rx; x++) {
				for(int y = ty; y <= by; y++) {
					loadTile(x, y);
				}
			}
			quadTree.queryInBox(new QuadRect(sx - walkRadiusIn31, sy - walkRadiusIn31, sx + walkRadiusIn31, 
					sy + walkRadiusIn31), allstops);
			Iterator<TransportRouteSegment> it = allstops.iterator();
			while(it.hasNext()) {
				TransportRouteSegment r = it.next();
				TransportStop st = r.getStop(r.segStart);
				if (Math.abs(st.x31 - sx) > walkRadiusIn31 || Math.abs(st.y31 - sy) > walkRadiusIn31) {
					wrongLoadedWays++;
					it.remove();
				} else {
					loadedWays++;
				}
			}
			loadTime += System.nanoTime() - nanoTime;
			return allstops;
		}


		private void loadTile(int x, int y) throws IOException {
			int tileId = x << (cfg.ZOOM_TO_LOAD_TILES + 1) + y;
			if(loadedTiles.contains(tileId)) {
				return;
			}
			int pz = (31 - cfg.ZOOM_TO_LOAD_TILES);
			SearchRequest<TransportStop> sr = BinaryMapIndexReader.buildSearchTransportRequest(x << pz, (x + 1) << pz, 
					y << pz, (y + 1) << pz, -1, null);
			TIntArrayList allPoints = new TIntArrayList();
			TIntArrayList allPointsUnique = new TIntArrayList();
			for(BinaryMapIndexReader r : map.keySet()) {
				sr.clearSearchResults();
				List<TransportStop> stops = r.searchTransportIndex(sr);
				for(TransportStop s : stops) {
					if(!loadedTransportStops.contains(s.getId())) {
						loadedTransportStops.put(s.getId(), s);
						allPoints.addAll(s.getReferencesToRoutes());
						
					}
				}
				makeUnique(allPoints, allPointsUnique);
				if(allPointsUnique.size() > 0) {
					loadTransportSegments(allPointsUnique, r, stops);
				}
			}			
			loadedTiles.add(tileId);
		}

		private void loadTransportSegments(TIntArrayList allPointsUnique, BinaryMapIndexReader r,
				List<TransportStop> stops) throws IOException {
			TIntObjectHashMap<TransportRoute> routes = r.getTransportRoutes(allPointsUnique.toArray());
			map.get(r).putAll(routes);
			for(TransportStop s : stops) {
				for(int ref : s.getReferencesToRoutes()) {
					TransportRoute route = routes.get(ref);
					if(route != null) {
						int stopIndex = -1;
						for(int k = 0; k < route.getForwardStops().size(); k++) {
							TransportStop st = route.getForwardStops().get(k);
							if(st.x31 == s.x31 && s.y31 == st.y31) {
								stopIndex = k;
								break;
							}
						}
						if(stopIndex != -1) {
							TransportRouteSegment segment = new TransportRouteSegment(route, stopIndex);
							quadTree.insert(segment, s.x31, s.y31);
						}
					}
				}
			}
		}

		private void makeUnique(TIntArrayList allPoints, TIntArrayList allPointsUnique) {
			allPoints.sort();
			int p = 0;
			TIntIterator it = allPoints.iterator();
			while(it.hasNext()) {
				int nxt = it.next();
				if(p != nxt) {
					allPointsUnique.add(nxt);
					p = nxt;
				}
			}			
		}

	}

}
