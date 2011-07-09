package net.osmand.router;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Collection;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;



public class RoutingContext {
	private static int DEFAULT_HEURISTIC_COEFFICIENT = 1;
	private static int ZOOM_TO_LOAD_TILES = 13;  // 12?, 14?
	
	// 1. parameters of routing and different tweaks
	private int heuristicCoefficient = DEFAULT_HEURISTIC_COEFFICIENT;
	private int zoomToLoadTileWithRoads = ZOOM_TO_LOAD_TILES;
	private boolean useStrategyOfIncreasingRoadPriorities = true;
	// null - 2 ways, true - direct way, false - reverse way
	private Boolean planRoadDirection = null;
	private VehicleRouter router = new CarRouter();
	private boolean useDynamicRoadPrioritising = true;
	private boolean usingShortestWay = false;

	
	// 2. Routing memory cache
	TLongObjectMap<BinaryMapDataObject> idObjects = new TLongObjectHashMap<BinaryMapDataObject>();
	TLongObjectMap<RouteSegment> routes = new TLongObjectHashMap<RouteSegment>();
	TIntSet loadedTiles = new TIntHashSet();

	// 3. debug information (package accessor)
	long timeToLoad = 0;
	long timeToCalculate = 0;
	int visitedSegments = 0;
	// callback of processing segments
	RouteSegmentVisitor visitor = null;
	
	
	public RouteSegmentVisitor getVisitor() {
		return visitor;
	}
	
	public TLongObjectMap<RouteSegment> getLoadedRoutes() {
		return routes;
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
	
	public boolean isUseStrategyOfIncreasingRoadPriorities() {
		return useStrategyOfIncreasingRoadPriorities && planRoadDirection == null;
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
	
	public void setHeuristicCoefficient(int heuristicCoefficient) {
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

	public boolean useStrategyOfIncreasingRoadPriorities() {
		return planRouteIn2Directions() && useStrategyOfIncreasingRoadPriorities;
	}

	public void setUseStrategyOfIncreasingRoadPriorities(boolean useStrategyOfIncreasingRoadPriorities) {
		this.useStrategyOfIncreasingRoadPriorities = useStrategyOfIncreasingRoadPriorities;
	}

	public Collection<BinaryMapDataObject> values() {
		return idObjects.valueCollection();
	}

	public int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, double o2DistanceFromStart, double o2DistanceToEnd) {
		return BinaryRoutePlanner.roadPriorityComparator(o1DistanceFromStart, o1DistanceToEnd, o2DistanceFromStart, o2DistanceToEnd,
				heuristicCoefficient);
	}
}