package net.osmand.router;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Iterator;

import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;



public class RoutingContext {
	
	public final RoutingConfiguration config;
	// 1. Initial variables
	private int garbageCollectorIteration = 0;
	private int relaxingIteration = 0;
	public long firstRoadId = 0;
	public int firstRoadDirection = 0;;
	

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