package net.osmand.router;

import java.util.ArrayList;
import java.util.Iterator;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;



public class RoutingContext {
	
	// 1. parameters of routing and different tweaks
	// Influence on A* : f(x) + heuristicCoefficient*g(X)
	private double heuristicCoefficient = 1;
	
	// 1.1 tile load parameters (should not affect routing)
	public int ZOOM_TO_LOAD_TILES = 13; //12?, 14?
	public int ITERATIONS_TO_RUN_GC = 100;
	public int NUMBER_OF_DESIRABLE_TILES_IN_MEMORY = 25;
	private int garbageCollectorIteration = 0;
	
	// 1.2 Dynamic road prioritizing (heuristic)
	private boolean useDynamicRoadPrioritising = true;
	
	// 1.3 Relaxing strategy
	private boolean useRelaxingStrategy = true;
	public int ITERATIONS_TO_RELAX_NODES = 100;
	private int relaxingIteration = 0;
	
	// 1.4 Build A* graph in backward/forward direction (can affect results)
	// null - 2 ways, true - direct way, false - reverse way
	private Boolean planRoadDirection = null;

	// 1.5 Router specific coefficients and restrictions
	private VehicleRouter router = new CarRouter();
	
	// not used right now
	private boolean usingShortestWay = false;
	
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
	
	public boolean runTilesGC() {
		garbageCollectorIteration++;
		int loadedTilesCritical = NUMBER_OF_DESIRABLE_TILES_IN_MEMORY * 3 /2;
		if (garbageCollectorIteration > ITERATIONS_TO_RUN_GC ||
				getCurrentlyLoadedTiles() > loadedTilesCritical) {
			garbageCollectorIteration = 0;
			return true;
		}
		return false;
	}
	
	public boolean runRelaxingStrategy(){
		relaxingIteration++;
		if(relaxingIteration > ITERATIONS_TO_RELAX_NODES){
			relaxingIteration = 0;
			return true;
		}
		return false;
	}
	
	
	public void setVisitor(RouteSegmentVisitor visitor) {
		this.visitor = visitor;
	}

	public boolean isUseDynamicRoadPrioritising() {
		return useDynamicRoadPrioritising;
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
		int xloc = x31 >> (31 - ZOOM_TO_LOAD_TILES);
		int yloc = y31 >> (31 - ZOOM_TO_LOAD_TILES);
		int l = (xloc << ZOOM_TO_LOAD_TILES) + yloc;
		RoutingTile tl = tiles.get(l);
		if(tl == null) {
			tl = new RoutingTile(xloc, yloc, ZOOM_TO_LOAD_TILES);
			tiles.put(l, tl);
		}
		return tiles.get(l);
	}
	
	public void unloadTile(RoutingTile tile, boolean createEmpty){
		int l = (tile.tileX << ZOOM_TO_LOAD_TILES) + tile.tileY;
		RoutingTile old = tiles.remove(l);
		RoutingTile n = new RoutingTile(tile.tileX, tile.tileY, ZOOM_TO_LOAD_TILES);
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
		
		TLongObjectMap<RouteSegment> routes = new TLongObjectHashMap<RouteSegment>();
		TLongObjectHashMap<RouteDataObject> idObjects = new TLongObjectHashMap<RouteDataObject>();
		
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
		}
		
		public void setLoaded() {
			isLoaded = Math.abs(isLoaded) + 1;
		}
		
		
		public boolean checkContains(int x31, int y31) {
			return tileX == (x31 >> (31 - zoom)) && tileY == (y31 >> (31 - zoom));
		}
		
		public TLongObjectMap<RouteSegment> getLoadedRoutes() {
			return routes;
		}
	}
}