package net.osmand.router;

import java.util.ArrayList;
import java.util.Iterator;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;



public class RoutingContext {
	private static int DEFAULT_HEURISTIC_COEFFICIENT = 1;
	private static int ZOOM_TO_LOAD_TILES = 13;  // 12?, 14?
	public static int ITERATIONS_TO_RUN_GC = 100;
	
	// 1. parameters of routing and different tweaks
	private double heuristicCoefficient = DEFAULT_HEURISTIC_COEFFICIENT;
	private int zoomToLoadTileWithRoads = ZOOM_TO_LOAD_TILES;
	private boolean useRelaxingStrategy = true;
	// null - 2 ways, true - direct way, false - reverse way
	private Boolean planRoadDirection = null;
	private VehicleRouter router = new CarRouter();
	private boolean useDynamicRoadPrioritising = true;
	// not used right now
	private boolean usingShortestWay = false;

	// 2. Routing memory cache (big objects)
	TIntObjectHashMap<RoutingTile> tiles = new TIntObjectHashMap<RoutingContext.RoutingTile>();

	int garbageCollectorIteration = 0;
	
	// 4. Warm object caches
	TLongSet nonRestrictedIds = new TLongHashSet();
	ArrayList<RouteSegment> segmentsToVisitPrescripted = new ArrayList<BinaryRoutePlanner.RouteSegment>(5);
	ArrayList<RouteSegment> segmentsToVisitNotForbidden = new ArrayList<BinaryRoutePlanner.RouteSegment>(5);
	
	public int targetEndX;
	public int targetEndY;
	public int startX;
	public int startY;
	public float estimatedDistance;
	
	RouteSegment finalDirectRoute = null;
	int finalDirectEndSegment = 0;
	RouteSegment finalReverseRoute = null;
	int finalReverseEndSegment = 0;


	// 3. debug information (package accessor)
	long timeToLoad = 0;
	long timeToCalculate = 0;
	int loadedTiles = 0;
	int maxLoadedTiles = 0;
	int loadedPrevUnloadedTiles = 0;
	int unloadedTiles = 0;
	TIntHashSet distinctUnloadedTiles = new TIntHashSet();
	int visitedSegments = 0;
	int relaxedSegments = 0;
	// callback of processing segments
	RouteSegmentVisitor visitor = null;
	
	
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
	
	
	public void setVisitor(RouteSegmentVisitor visitor) {
		this.visitor = visitor;
	}

	public boolean isUseDynamicRoadPrioritising() {
		return useDynamicRoadPrioritising;
	}

	public int getZoomToLoadTileWithRoads() {
		return zoomToLoadTileWithRoads;
	}
	
	public boolean isUseRelaxingStrategy() {
		return useRelaxingStrategy;
	}
	
	public void setUseRelaxingStrategy(boolean useRelaxingStrategy) {
		this.useRelaxingStrategy = useRelaxingStrategy;
	}
	
	public void setUseDynamicRoadPrioritising(boolean useDynamicRoadPrioritising) {
		this.useDynamicRoadPrioritising = useDynamicRoadPrioritising;
	}
	
	public void setUsingShortestWay(boolean usingShortestWay) {
		this.usingShortestWay = usingShortestWay;
	}
	
	public boolean isUsingShortestWay() {
		return usingShortestWay;
	}
	

	public void setRouter(VehicleRouter router) {
		this.router = router;
	}
	
	public void setHeuristicCoefficient(double heuristicCoefficient) {
		this.heuristicCoefficient = heuristicCoefficient;
	}

	public VehicleRouter getRouter() {
		return router;
	}

	public boolean planRouteIn2Directions() {
		return planRoadDirection == null;
	}

	public Boolean getPlanRoadDirection() {
		return planRoadDirection;
	}

	public void setPlanRoadDirection(Boolean planRoadDirection) {
		this.planRoadDirection = planRoadDirection;
	}

	public int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, double o2DistanceFromStart, double o2DistanceToEnd) {
		return BinaryRoutePlanner.roadPriorityComparator(o1DistanceFromStart, o1DistanceToEnd, o2DistanceFromStart, o2DistanceToEnd,
				heuristicCoefficient);
	}
	
	public RoutingTile getRoutingTile(int x31, int y31){
		int xloc = x31 >> (31 - zoomToLoadTileWithRoads);
		int yloc = y31 >> (31 - zoomToLoadTileWithRoads);
		int l = (xloc << zoomToLoadTileWithRoads) + yloc;
		RoutingTile tl = tiles.get(l);
		if(tl == null) {
			tl = new RoutingTile(xloc, yloc, zoomToLoadTileWithRoads);
			tiles.put(l, tl);
		}
		return tiles.get(l);
	}
	
	public void unloadTile(RoutingTile tile, boolean createEmpty){
		int l = (tile.tileX << zoomToLoadTileWithRoads) + tile.tileY;
		RoutingTile old = tiles.remove(l);
		RoutingTile n = new RoutingTile(tile.tileX, tile.tileY, zoomToLoadTileWithRoads);
		n.isLoaded = old.isLoaded;
		n.setUnloaded();
		tiles.put(l, n);
		unloadedTiles++;
		distinctUnloadedTiles.add(l);
	}
	
	public static class RoutingTile {
		private int tileX;
		private int tileY;
		private int zoom;
		private int isLoaded;
		// make it without get/set for fast access
		public int access;
		
		public RoutingTile(int tileX, int tileY, int zoom) {
			this.tileX = tileX;
			this.tileY = tileY;
			this.zoom = zoom;
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
		}
		
		public void setLoaded() {
			isLoaded = Math.abs(isLoaded) + 1;
		}
		
		TLongObjectMap<RouteSegment> routes = new TLongObjectHashMap<RouteSegment>();
		TIntSet loadedTiles = new TIntHashSet();
		// TODO delete this object ?
		TLongObjectHashMap<RouteDataObject> idObjects = new TLongObjectHashMap<RouteDataObject>();
		
		public boolean checkContains(int x31, int y31) {
			return tileX == (x31 >> (31 - zoom)) && tileY == (y31 >> (31 - zoom));
		}
		
		public TLongObjectMap<RouteSegment> getLoadedRoutes() {
			return routes;
		}
	}
}