package net.osmand.router;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.util.MapUtils;

public class TransportRoutePlanner {
	
	public void searchRouteInternal(final TransportRoutingContext ctx, TransportRouteSegment start, TransportRouteSegment end,
			RouteSegment recalculationEnd) throws InterruptedException, IOException {
		Comparator<TransportRouteSegment> nonHeuristicSegmentsComparator = new NonHeuristicSegmentsComparator();
		PriorityQueue<TransportRouteSegment> graphDirectSegments = new PriorityQueue<TransportRouteSegment>(50, new SegmentsComparator(
				ctx));
		PriorityQueue<TransportRouteSegment> graphReverseSegments = new PriorityQueue<TransportRouteSegment>(50, new SegmentsComparator(
				ctx));
		// Set to not visit one segment twice (stores road.id << X + segmentStart)
		TLongObjectHashMap<TransportRouteSegment> visitedDirectSegments = new TLongObjectHashMap<TransportRouteSegment>();
		TLongObjectHashMap<TransportRouteSegment> visitedOppositeSegments = new TLongObjectHashMap<TransportRouteSegment>();


		// Extract & analyze segment with min(f(x)) from queue while final segment is not found
		boolean forwardSearch = true;

		PriorityQueue<TransportRouteSegment> graphSegments = graphDirectSegments;

		FinalRouteSegment finalSegment = null;
		boolean onlyBackward = ctx.getPlanRoadDirection() < 0;
		boolean onlyForward = ctx.getPlanRoadDirection() > 0;
		while (!graphSegments.isEmpty()) {
			TransportRouteSegment segment = graphSegments.poll();
			// TODO break when it is final segment
//			if (segment instanceof FinalRouteSegment) {
//				finalSegment = (FinalRouteSegment) segment;
//				break;
//			}
			ctx.visitedSegments++;
			if (forwardSearch) {
				boolean doNotAddIntersections = onlyBackward;
				processRouteSegment(ctx, false, graphDirectSegments, visitedDirectSegments, segment,
						visitedOppositeSegments, doNotAddIntersections);
			} else {
				boolean doNotAddIntersections = onlyForward;
				processRouteSegment(ctx, true, graphReverseSegments, visitedOppositeSegments, segment,
						visitedDirectSegments, doNotAddIntersections);
			}
			updateCalculationProgress(ctx, graphDirectSegments, graphReverseSegments);

			checkIfGraphIsEmpty(ctx, ctx.getPlanRoadDirection() <= 0, graphReverseSegments, end,
					visitedOppositeSegments, "Route is not found to selected target point.");
			checkIfGraphIsEmpty(ctx, ctx.getPlanRoadDirection() >= 0, graphDirectSegments, start,
					visitedDirectSegments, "Route is not found from selected start point.");
			if (ctx.planRouteIn2Directions()) {
				forwardSearch = (nonHeuristicSegmentsComparator.compare(graphDirectSegments.peek(),
						graphReverseSegments.peek()) < 0);
			} else {
				// different strategy : use onedirectional graph
				forwardSearch = onlyForward;
				if (onlyBackward && !graphDirectSegments.isEmpty()) {
					forwardSearch = true;
				}
				if (onlyForward && !graphReverseSegments.isEmpty()) {
					forwardSearch = false;
				}
			}

			if (forwardSearch) {
				graphSegments = graphDirectSegments;
			} else {
				graphSegments = graphReverseSegments;
			}
			// check if interrupted
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				throw new InterruptedException("Route calculation interrupted");
			}
		}
	}
	
	
	private void checkIfGraphIsEmpty(TransportRoutingContext ctx, boolean b,
			PriorityQueue<TransportRouteSegment> graphReverseSegments, TransportRouteSegment end,
			TLongObjectHashMap<TransportRouteSegment> visitedOppositeSegments, String string) {
		// TODO Auto-generated method stub
	}


	private void updateCalculationProgress(TransportRoutingContext ctx,
			PriorityQueue<TransportRouteSegment> graphDirectSegments, PriorityQueue<TransportRouteSegment> graphReverseSegments) {
		// TODO Auto-generated method stub
	}

	private static int sleft = MapUtils.get31TileNumberX(4.7495);
	private static int sright = MapUtils.get31TileNumberX(4.8608);
	private static int stop = MapUtils.get31TileNumberY(52.3395);
	private static int sbottom = MapUtils.get31TileNumberY(52.2589);
	private static int szoom = 15;

	@SuppressWarnings("unused")
	private void processRouteSegment(final TransportRoutingContext ctx, boolean reverseWaySearch,
			PriorityQueue<TransportRouteSegment> graphSegments, TLongObjectHashMap<TransportRouteSegment> visitedSegments, 
			TransportRouteSegment segment, TLongObjectHashMap<TransportRouteSegment> oppositeSegments, boolean doNotAddIntersections) throws IOException {
		final TransportRoute road = segment.road;
		BinaryMapIndexReader reader = ctx.map.keySet().iterator().next();
		List<TransportStop> transportStops = reader.searchTransportIndex(BinaryMapIndexReader.buildSearchTransportRequest(sleft, sright, stop, sbottom,
				-1, null));
		for(TransportStop s : transportStops) {
			int[] referencesToRoutes = s.getReferencesToRoutes();
			TIntObjectHashMap<TransportRoute> routes = reader.getTransportRoutes(referencesToRoutes);
		}
	}
	
	

	public static void main(String[] args) throws IOException {
		File fl = new File(System.getProperty("maps.dir"), "Netherlands_amstelveen.obf");
		RandomAccessFile raf = new RandomAccessFile(fl, "r");
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf, fl);
		
		LatLon start = new LatLon(52.28094, 4.853248);
		LatLon end = new LatLon(52.320988, 4.87256);
		TransportRoutingContext ctx = new TransportRoutingContext(reader);
		TransportRoutePlanner planner = new TransportRoutePlanner();
		planner.buildRoute(ctx, start, end);
		
	}

	private void buildRoute(TransportRoutingContext ctx, LatLon start, LatLon end) throws IOException {
		double limit = 1000;
		BinaryMapIndexReader reader = ctx.map.keySet().iterator().next();
		
		List<TransportRouteSegment> startStops = ctx.getTransportStops(start);
		List<TransportRouteSegment> endStops = ctx.getTransportStops(end);
		
		TLongObjectHashMap<TransportRouteSegment> endSegments = new TLongObjectHashMap<TransportRouteSegment>();
		for(TransportRouteSegment s : endStops) {
			endSegments.put(s.getId(), s);
		}
		PriorityQueue<TransportRouteSegment> queue = new PriorityQueue<TransportRoutePlanner.TransportRouteSegment>();
		queue.addAll(startStops);
		while(!queue.isEmpty()) {
			TransportRouteSegment segment = queue.poll();
			long l = segment.getId();
			for (int i = 0; i < segment.getLength() - segment.segStart; i++) {
				TransportRouteSegment finish = endSegments.get(l+i);
				if(finish != null){
					System.out.println(segment  + " " + finish);
				}
			}
		}
	
	}

	private static class SegmentsComparator implements Comparator<TransportRouteSegment> {
		final TransportRoutingContext ctx;

		public SegmentsComparator(TransportRoutingContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public int compare(TransportRouteSegment o1, TransportRouteSegment o2) {
			return BinaryRoutePlanner.roadPriorityComparator(o1.distanceFromStart, o1.distanceToEnd, 
					o2.distanceFromStart, o2.distanceToEnd, ctx.config.heuristicCoefficient);
		}
	}
	
	private static class NonHeuristicSegmentsComparator implements Comparator<TransportRouteSegment> {
		public NonHeuristicSegmentsComparator() {
		}

		@Override
		public int compare(TransportRouteSegment o1, TransportRouteSegment o2) {
			return BinaryRoutePlanner.
					roadPriorityComparator(o1.distanceFromStart, o1.distanceToEnd, o2.distanceFromStart, o2.distanceToEnd, 0);
		}
	}
	
	public RouteSegment initRouteSegment(final TransportRoutingContext ctx, TransportRouteSegment segment, boolean positiveDirection) {
//		if (segment.getSegmentStart() == 0 && !positiveDirection && segment.getRoad().getPointsLength() > 0) {
//			segment = loadSameSegment(ctx, segment, 1);
////		} else if (segment.getSegmentStart() == segment.getRoad().getPointsLength() - 1 && positiveDirection && segment.getSegmentStart() > 0) {
//		// assymetric cause we calculate initial point differently (segmentStart means that point is between ]segmentStart-1, segmentStart]
//		} else if (segment.getSegmentStart() > 0 && positiveDirection) {
//			segment = loadSameSegment(ctx, segment, segment.getSegmentStart() - 1);
//		}
//		if (segment == null) {
//			return null;
//		}
		return null;
	}
	
	public static class TransportRouteSegment {
		public double distanceToEnd;
		final short segStart;
		final TransportRoute road;
		private static final int SHIFT = 10; // assume less than 1024 stops
		
		TransportRouteSegment parentRoute = null;
		float distanceFromStart = 0;
		
		public TransportRouteSegment(TransportRoute road, int stopIndex) {
			this.road = road;
			this.segStart = (short) stopIndex;
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
			return String.format("Route stop: %s %s", road, road.getForwardStops().get(segStart));
		}

	}
	
	public static class TransportRoutingContext {
		
		public final int walkRadius = 5000; //  meters from start/end/intermediate locations
		public int searchThreshold = 10; // don't search stops beyond searchThreshold*dist(start, end) radius 
		
		public int visitedSegments;
		public RoutingConfiguration config;
		public RouteCalculationProgress calculationProgress;
		public TLongObjectHashMap<TransportStop> visitedTransportStops = new TLongObjectHashMap<TransportStop>();
		private TIntArrayList visitedTiles = new TIntArrayList();
		private static final int ZOOM_TO_LOAD_TILES = 14;
		
		public QuadTree<TransportRouteSegment> quadTree = 
				new QuadTree<TransportRouteSegment>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE), 8, 0.55f);
		public final Map<BinaryMapIndexReader, TIntObjectHashMap<TransportRoute>> map = 
				new LinkedHashMap<BinaryMapIndexReader, TIntObjectHashMap<TransportRoute>>();
		
		private final int walkRaidusIn31;
		
		
		public TransportRoutingContext(BinaryMapIndexReader... readers) {
			walkRaidusIn31 = (int) (walkRadius / MapUtils.getTileDistanceWidth(31));
			for (BinaryMapIndexReader r : readers) {
				map.put(r, new TIntObjectHashMap<TransportRoute>());
			}
		}
		
		public List<TransportRouteSegment> getTransportStops(LatLon start) throws IOException {
			int y = MapUtils.get31TileNumberY(start.getLatitude());
			int x = MapUtils.get31TileNumberX(start.getLongitude());
			return getTransportStops(x, y);
		}
		
		public List<TransportRouteSegment> getTransportStops(int x, int y) throws IOException {
			return loadNativeTransportStops(x, y);
		}

		private List<TransportRouteSegment> loadNativeTransportStops(int sx, int sy) throws IOException {
			List<TransportRouteSegment> allstops = new ArrayList<TransportRouteSegment>();
			int lx = (sx - walkRaidusIn31 ) >> (31 - ZOOM_TO_LOAD_TILES);
			int rx = (sx + walkRaidusIn31 ) >> (31 - ZOOM_TO_LOAD_TILES);
			int ty = (sy - walkRaidusIn31 ) >> (31 - ZOOM_TO_LOAD_TILES);
			int by = (sy + walkRaidusIn31 ) >> (31 - ZOOM_TO_LOAD_TILES);
			for(int x = lx; x <= rx; x++) {
				for(int y = ty; y <= by; y++) {
					loadTile(x, y);
				}
			}
			quadTree.queryInBox(new QuadRect(sx - walkRaidusIn31, sy - walkRaidusIn31, sx + walkRaidusIn31, 
					sy + walkRadius), allstops);
			return allstops;
		}


		private void loadTile(int x, int y) throws IOException {
			int tileId = x << (ZOOM_TO_LOAD_TILES + 1) + y;
			if(visitedTiles.contains(tileId)) {
				return;
			}
			int pz = (31 - ZOOM_TO_LOAD_TILES);
			SearchRequest<TransportStop> sr = BinaryMapIndexReader.buildSearchTransportRequest(x << pz, (x + 1) << pz, 
					y << pz, (y + 1) << pz, -1, null);
			TIntArrayList allPoints = new TIntArrayList();
			TIntArrayList allPointsUnique = new TIntArrayList();
			for(BinaryMapIndexReader r : map.keySet()) {
				sr.clearSearchResults();
				List<TransportStop> stops = r.searchTransportIndex(sr);
				for(TransportStop s : stops) {
					if(!visitedTransportStops.contains(s.getId())) {
						visitedTransportStops.put(s.getId(), s);
						allPoints.addAll(s.getReferencesToRoutes());
						
					}
				}
				makeUnique(allPoints, allPointsUnique);
				if(allPointsUnique.size() > 0) {
					loadTransportSegments(allPointsUnique, r, stops);
				}
			}			
			visitedTiles.add(tileId);
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

		public int getPlanRoadDirection() {
			return config.planRoadDirection;
		}
		
		public SearchRequest<TransportStop> getBuildRequest(LatLon start) {
			int sy = MapUtils.get31TileNumberY(start.getLatitude());
			int sx = MapUtils.get31TileNumberX(start.getLongitude());
			int w = (int) (walkRadius / MapUtils.getTileDistanceWidth(31));
			int h = (int) (walkRadius / MapUtils.getTileDistanceWidth(31));
			return BinaryMapIndexReader.buildSearchTransportRequest(sx - w, sx + w, sy - h, sy + h, -1, null);
		}

		public VehicleRouter getRouter() {
			return config.router;
		}

		public boolean planRouteIn2Directions() {
			return config.planRoadDirection == 0;
		}
	}

}
