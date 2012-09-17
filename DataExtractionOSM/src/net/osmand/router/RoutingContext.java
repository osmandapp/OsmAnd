package net.osmand.router;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.NativeLibrary;
import net.osmand.NativeLibrary.NativeRouteSearchResult;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;



public class RoutingContext {
	
	public final RoutingConfiguration config;
	// 1. Initial variables
	private int garbageCollectorIteration = 0;
	private int relaxingIteration = 0;
	public long firstRoadId = 0;
	public int firstRoadDirection = 0;
	
	public Interruptable interruptable;
	public List<RouteSegmentResult> previouslyCalculatedRoute;
	

	// 2. Routing memory cache (big objects)
	TIntObjectHashMap<RoutingTile> tiles = new TIntObjectHashMap<RoutingContext.RoutingTile>();

	
	// 3. Warm object caches
	TLongSet nonRestrictedIds = new TLongHashSet();
	ArrayList<RouteSegment> segmentsToVisitPrescripted = new ArrayList<BinaryRoutePlanner.RouteSegment>(5);
	ArrayList<RouteSegment> segmentsToVisitNotForbidden = new ArrayList<BinaryRoutePlanner.RouteSegment>(5);
	
	// 4. Final results
	RouteSegment finalDirectRoute = null;
	int finalDirectEndSegment = 0;
	RouteSegment finalReverseRoute = null;
	int finalReverseEndSegment = 0;


	// 5. debug information (package accessor)
	long timeToLoad = 0;
	long timeToCalculate = 0;
	public int loadedTiles = 0;
	int distinctLoadedTiles = 0;
	int maxLoadedTiles = 0;
	int loadedPrevUnloadedTiles = 0;
	int unloadedTiles = 0;
	TIntHashSet distinctUnloadedTiles = new TIntHashSet();
	public int visitedSegments = 0;
	public int relaxedSegments = 0;
	// callback of processing segments
	RouteSegmentVisitor visitor = null;
	
	public RoutingContext(RoutingConfiguration config) {
		this.config = config;
	}
	
	
	public RouteSegmentVisitor getVisitor() {
		return visitor;
	}
	
	public int getCurrentlyLoadedTiles() {
		int cnt = 0;
		Iterator<RoutingTile> it = tiles.valueCollection().iterator();
		while (it.hasNext()) {
			if (it.next().isLoaded()) {
				cnt++;
			}
		}
		return cnt;
	}
	
	public boolean runTilesGC() {
		garbageCollectorIteration++;
		int loadedTilesCritical = config.NUMBER_OF_DESIRABLE_TILES_IN_MEMORY * 3 /2;
		if (garbageCollectorIteration > config.ITERATIONS_TO_RUN_GC ||
				getCurrentlyLoadedTiles() > loadedTilesCritical) {
			garbageCollectorIteration = 0;
			return true;
		}
		return false;
	}
	
	public boolean runRelaxingStrategy(){
		if(!isUseRelaxingStrategy()){
			return false;
		}
		relaxingIteration++;
		if(relaxingIteration > config.ITERATIONS_TO_RELAX_NODES){
			relaxingIteration = 0;
			return true;
		}
		return false;
	}
	
	
	public void setVisitor(RouteSegmentVisitor visitor) {
		this.visitor = visitor;
	}

	public boolean isUseDynamicRoadPrioritising() {
		return config.useDynamicRoadPrioritising;
	}
	
	public int getDynamicRoadPriorityDistance() {
		return config.dynamicRoadPriorityDistance;
	}

	public boolean isUseRelaxingStrategy() {
		return config.useRelaxingStrategy;
	}
	
	public void setUseRelaxingStrategy(boolean useRelaxingStrategy) {
		config.useRelaxingStrategy = useRelaxingStrategy;
	}
	
	public void setUseDynamicRoadPrioritising(boolean useDynamicRoadPrioritising) {
		config.useDynamicRoadPrioritising = useDynamicRoadPrioritising;
	}
	
	

	public void setRouter(VehicleRouter router) {
		config.router = router;
	}
	
	public void setHeuristicCoefficient(double heuristicCoefficient) {
		config.heuristicCoefficient = heuristicCoefficient;
	}

	public VehicleRouter getRouter() {
		return config.router;
	}

	public boolean planRouteIn2Directions() {
		return config.planRoadDirection == 0;
	}

	public int getPlanRoadDirection() {
		return config.planRoadDirection;
	}

	public void setPlanRoadDirection(int planRoadDirection) {
		config.planRoadDirection = planRoadDirection;
	}

	public int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, double o2DistanceFromStart, double o2DistanceToEnd) {
		return BinaryRoutePlanner.roadPriorityComparator(o1DistanceFromStart, o1DistanceToEnd, o2DistanceFromStart, o2DistanceToEnd,
				config.heuristicCoefficient);
	}
	
	public RoutingTile getRoutingTile(int x31, int y31){
		int xloc = x31 >> (31 - config.ZOOM_TO_LOAD_TILES);
		int yloc = y31 >> (31 - config.ZOOM_TO_LOAD_TILES);
		int l = (xloc << config.ZOOM_TO_LOAD_TILES) + yloc;
		RoutingTile tl = tiles.get(l);
		if(tl == null) {
			tl = new RoutingTile(xloc, yloc, config.ZOOM_TO_LOAD_TILES);
			tiles.put(l, tl);
		}
		return tiles.get(l);
	}
	
	public void unloadTile(RoutingTile tile, boolean createEmpty){
		int l = (tile.tileX << config.ZOOM_TO_LOAD_TILES) + tile.tileY;
		RoutingTile old = tiles.remove(l);
		RoutingTile n = new RoutingTile(tile.tileX, tile.tileY, config.ZOOM_TO_LOAD_TILES);
		n.isLoaded = old.isLoaded;
		n.setUnloaded();
		tiles.put(l, n);
		unloadedTiles++;
		distinctUnloadedTiles.add(l);
	}
	
	
	public void loadTileData(final RoutingTile tile, final List<RouteDataObject> toFillIn, NativeLibrary nativeLib, 
			Map<BinaryMapIndexReader, List<RouteSubregion>> map) {
		
		long now = System.nanoTime();
		final int zoomToLoad = 31 - tile.getZoom();
		final int tileX = tile.getTileX();
		final int tileY = tile.getTileY();
		ResultMatcher<RouteDataObject> matcher = new ResultMatcher<RouteDataObject>() {
			int intersectionObjects = 0, all = 0, allout = 0;
			@Override
			public boolean publish(RouteDataObject o) {
				if(o.getPointsLength() == 0) {
					return false;
				}
				all++;
				boolean out = false;
				int minx, maxx, miny, maxy;
				minx = maxx = o.getPoint31XTile(0);
				miny = maxy = o.getPoint31YTile(0);
				for (int ti = 0; ti < o.getPointsLength(); ti++) {
					minx = Math.min(o.getPoint31XTile(ti), minx);
					maxx = Math.max(o.getPoint31XTile(ti), maxx);
					miny = Math.min(o.getPoint31YTile(ti), miny);
					maxy = Math.max(o.getPoint31YTile(ti), maxy);
					if(!tile.checkContains(o.getPoint31XTile(ti), o.getPoint31YTile(ti))) {
						out = true;
					}
				}
				minx = minx >> zoomToLoad;
				maxx = maxx >> zoomToLoad;
				miny = miny >> zoomToLoad;
				maxy = maxy >> zoomToLoad;
				if(minx > tileX || maxx < tileX || miny > tileY || maxy < tileY) {
					allout++;
					return false;
				}
				if(out) {
					intersectionObjects++;
				}

				if (toFillIn != null) {
					if (getRouter().acceptLine(o)) {
						toFillIn.add(o);
					}
				}
				registerRouteDataObject(o, tile);
				return false;
			}
			
			@Override
			public String toString() {
				return "Tile " + tileX + "/"+ tileY + " boundaries " + intersectionObjects +  " of " + all + " out " + allout;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};
		
		boolean loadData = toFillIn != null;
		List<NativeRouteSearchResult> nativeRouteSearchResults = new ArrayList<NativeRouteSearchResult>();
		SearchRequest<RouteDataObject> request = BinaryMapIndexReader.buildSearchRouteRequest(tileX << zoomToLoad,
				(tileX + 1) << zoomToLoad, tileY << zoomToLoad, (tileY + 1) << zoomToLoad, matcher);
		for (Entry<BinaryMapIndexReader, List<RouteSubregion>> r : map.entrySet()) {
			if(nativeLib != null) {
				try {
					r.getKey().initRouteRegionsIfNeeded(request);
				} catch (IOException e) {
					throw new RuntimeException("Loading data exception", e);
				}
				for(RouteRegion reg : r.getKey().getRoutingIndexes()) {
					NativeRouteSearchResult rs = nativeLoadRegion(request, reg, nativeLib, loadData);
					if(rs != null) {
						if(!loadData){
							if (rs.nativeHandler != 0) {
								nativeRouteSearchResults.add(rs);
							}
						} else {
							if(rs.objects != null){
								for(RouteDataObject ro : rs.objects) {
									if(ro != null) {
										request.publish(ro);
									}
								}
							}
						}
					}
				}
			} else {
				try {
					r.getKey().searchRouteIndex(request, r.getValue());
				} catch (IOException e) {
					throw new RuntimeException("Loading data exception", e);
				}
			}
		}
		loadedTiles++;
		if (tile.isUnloaded()) {
			loadedPrevUnloadedTiles++;
		} else {
			distinctLoadedTiles++;
		}
		tile.setLoaded();
		if(nativeRouteSearchResults.size() > 0) {
			tile.nativeLib = nativeLib;
			tile.nativeResults = nativeRouteSearchResults;
			
		}
		timeToLoad += (System.nanoTime() - now);
	}

	


	private NativeRouteSearchResult nativeLoadRegion(SearchRequest<RouteDataObject> request, RouteRegion reg, NativeLibrary nativeLib, boolean loadData) {
		boolean intersects = false;
		for(RouteSubregion sub : reg.getSubregions()) {
			if(request.intersects(sub.left, sub.top, sub.right, sub.bottom)) {
				intersects = true;
				break;
			}
		}
		if(intersects) {
			return nativeLib.loadRouteRegion(reg, request.getLeft(), request.getRight(), request.getTop(), request.getBottom(), loadData);
		}
		return null;
	}
	
	
	/*private */void registerRouteDataObject(RouteDataObject o, RoutingTile suggestedTile ) {
		if(!getRouter().acceptLine(o)){
			return;
		}
		RoutingTile tl = suggestedTile;
		RouteDataObject old = tl.idObjects.get(o.id);
		// sometimes way is present only partially in one index
		if (old != null && old.getPointsLength() >= o.getPointsLength()) {
			return;
		};
		for (int j = 0; j < o.getPointsLength(); j++) {
			int x = o.getPoint31XTile(j);
			int y = o.getPoint31YTile(j);
			if(!tl.checkContains(x, y)){
				// don't register in different tiles
				// in order to throw out tile object easily
				continue;
			}
			long l = (((long) x) << 31) + (long) y;
			RouteSegment segment = new RouteSegment(o , j);
			RouteSegment prev = tl.getSegment(l, this);
			boolean i = true;
			if (prev != null) {
				if (old == null) {
					segment.next = prev;
				} else if (prev.road == old) {
					segment.next = prev.next;
				} else {
					// segment somewhere in the middle replace element in linked list
					RouteSegment rr = prev;
					while (rr != null) {
						if (rr.road == old) {
							prev.next = segment;
							segment.next = rr.next;
							break;
						}
						prev = rr;
						rr = rr.next;
					}
					i = false;
				}
			}
			if (i) {
				tl.routes.put(l, segment);
				tl.idObjects.put(o.id, o);
			}
		}
	}
	
	public void copyLoadedDataAndClearCaches(RoutingContext ctx) {
		for(RoutingTile tl : ctx.tiles.valueCollection()) {
			if(tl.isLoaded()) {
				this.tiles.put(tl.getId(), tl);
				for(RouteSegment rs : tl.routes.valueCollection()) {
					RouteSegment s = rs;
					while(s != null) {
						s.parentRoute = null;
						s.parentSegmentEnd = 0;
						s.distanceFromStart = 0;
						s.distanceToEnd = 0;
						s = s.next;
					}
				}
			}
		}
	}
	
	public static class RoutingTile {
		private int tileX;
		private int tileY;
		private int zoom;
		private int isLoaded;
		// make it without get/set for fast access
		public int access;
		
		private NativeLibrary nativeLib;
		// null if it doesn't work with native results
		private List<NativeRouteSearchResult> nativeResults;
		private TLongHashSet excludeDuplications = new TLongHashSet();
		
		private TLongObjectMap<RouteSegment> routes = new TLongObjectHashMap<RouteSegment>();
		private TLongObjectHashMap<RouteDataObject> idObjects = new TLongObjectHashMap<RouteDataObject>();
		
		public RouteSegment getSegment(long id, RoutingContext ctx) {
			if(nativeResults != null) {
				RouteSegment original = loadNativeRouteSegment(id, ctx);
				return original;
			}
			return routes.get(id);
		}

		private RouteSegment loadNativeRouteSegment(long id, RoutingContext ctx) {
			int y31 = (int) (id & Integer.MAX_VALUE);
			int x31 = (int) (id >> 31);
			excludeDuplications.clear();
			RouteSegment original = null;
			RouteSegment prev = null;
			for (NativeRouteSearchResult rs : nativeResults) {
				RouteDataObject[] res = nativeLib.getDataObjects(rs, x31, y31);
				if (res != null) {
					for (RouteDataObject ro : res) {
						boolean accept = ro != null && !excludeDuplications.contains(ro.id);
						if(ctx != null && accept) {
							accept = ctx.getRouter().acceptLine(ro);
						}
						if (accept) {
							excludeDuplications.add(ro.id);
							for (int i = 0; i < ro.pointsX.length; i++) {
								if (ro.getPoint31XTile(i) == x31 && ro.getPoint31YTile(i) == y31) {
									RouteSegment segment = new RouteSegment(ro, i);
									if (prev != null) {
										prev.next = segment;
										prev = segment;
									} else {
										original = segment;
										prev = segment;
									}
								}
							}

						}
					}
				}
			}
			return original;
		}
		
		public RoutingTile(int tileX, int tileY, int zoom) {
			this.tileX = tileX;
			this.tileY = tileY;
			this.zoom = zoom;
		}
		
		public int getId(){
			return (tileX << zoom) + tileY;
		}
		
		public int getZoom() {
			return zoom;
		}
		
		public int getTileX() {
			return tileX;
		}
		
		public int getTileY() {
			return tileY;
		}
		
		public boolean isLoaded() {
			return isLoaded > 0;
		}
		
		public int getUnloadCont(){
			return Math.abs(isLoaded);
		}
		
		public boolean isUnloaded() {
			return isLoaded < 0;
		}
		
		public void setUnloaded() {
			if(isLoaded == 0) {
				this.isLoaded = -1;	
			} else {
				isLoaded = -Math.abs(isLoaded);
			}
			if(nativeResults != null) {
				for(NativeRouteSearchResult rs : nativeResults) {
					rs.deleteNativeResult();
				}
			}
		}
		
		public void setLoaded() {
			isLoaded = Math.abs(isLoaded) + 1;
		}
		
		
		public boolean checkContains(int x31, int y31) {
			return tileX == (x31 >> (31 - zoom)) && tileY == (y31 >> (31 - zoom));
		}
		
	}

}