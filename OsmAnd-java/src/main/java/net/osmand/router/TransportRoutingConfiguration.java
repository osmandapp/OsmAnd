package net.osmand.router;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.data.TransportRoute;
import net.osmand.router.GeneralRouter.RouteAttributeContext;
import net.osmand.router.GeneralRouter.RouteDataObjectAttribute;

public class TransportRoutingConfiguration {

	public int ZOOM_TO_LOAD_TILES = 15;
	
	public int walkRadius = 1500; // ? 3000
	
	public int walkChangeRadius = 300; 
	
	public int maxNumberOfChanges = 2; // replaced with max_num_changes

	public int ptLimitResultsByNumber = 50; // pt_limit - limit number of best routes (0 = unlimited)

	public double increaseForAlternativesRoutes = 2.0;
	public double increaseForAltRoutesWalking = 3.0;
	
	public int combineAltRoutesDiffStops = 120;
	public int combineAltRoutesSumDiffStops = 300;

	public int maxRouteTime = 60 * 60 * 10; // 10 hours
	public int maxRouteDistance = 0; // distance for maxRouteTime
	public int maxRouteIncreaseSpeed = 30; // speed to increase route time


	public GeneralRouter router;
	// cache values from router for fast access
	public float walkSpeed = (float) (3.6 / 3.6); // m/s
	public float defaultTravelSpeed = (float) (60 / 3.6); // m/s
	
	
	private int defaultStopTime = 0;
	private Map<String, Integer> stopTimes = new HashMap<String, Integer>();
	private int defaultBoardingTime = 0;
	private Map<String, Integer> boardingTimes = new HashMap<String, Integer>();
	private int defaultChangeTime = 0;
	private Map<String, Integer> changingTimes = new HashMap<String, Integer>();
	// ferry crossing, see FerryRoutingHelper
	public int ferryBoardingTime;
	public int ferryTerminalTime;
	
	public boolean useSchedule;
	// 10 seconds based
	public int scheduleTimeOfDay = 12 * 60 * 6; // 12:00 - 60*6*12
	public int scheduleMaxTime = 50 * 6; // not appropriate variable, should be dynamic
	// day since 2000
	public int scheduleDayNumber;

	private Map<String, Integer> rawTypes = new HashMap<String, Integer>();
	private Map<String, Float> speed = new TreeMap<String, Float>();
	
	
	public int getStopTime(String routeType) {
		int time;
		if (stopTimes.containsKey(routeType)) {
			time = stopTimes.get(routeType);
		} else {
			RouteAttributeContext obstacles = router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES);
			time = obstacles.evaluateInt(getRawBitset("stop", routeType), 0);
			stopTimes.put(routeType, time);
		}
		if (time > 0) {
			return time;
		}
		if (defaultStopTime == 0) {
			RouteAttributeContext obstacles = router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES);
			defaultStopTime = obstacles.evaluateInt(getRawBitset("stop", ""), 30);
		}
		return defaultStopTime;
	}
	
	// waiting for a vehicle (half of its interval if known) and getting on it
	public double getBoardingTime(String routeType, int intervalSeconds) {
		if (FerryRoutingHelper.FERRY.equals(routeType)) {
			return FerryRoutingHelper.getBoardingTime(ferryBoardingTime, ferryTerminalTime, intervalSeconds);
		}
		return intervalSeconds > 0 ? intervalSeconds / 2.0 : getBoardingTime(routeType);
	}

	// Route timing for TransportRoutePlanner: the planner uses only these methods,
	// the rules of particular vehicles (ferries, see TransportFerryHelper) are applied here.

	// waiting for the vehicle and getting on it at the stop (nothing if the ride just continues there)
	public double getBoardingTime(TransportRoute route, int stop) {
		return isContinuation(route, stop) ? 0 : getBoardingTime(route.getType(), route.calcIntervalInSeconds());
	}

	// meters per second, 0 if the route isn't used
	public double getTravelSpeed(TransportRoute route) {
		float speed = getSpeedByRouteType(route.getType());
		return speed == 0 ? 0 : TransportFerryHelper.getTravelSpeed(route, speed);
	}

	// ride from the previous stop to the stop, the vehicle stands at the previous stop before it
	public double getRideTime(TransportRoute route, double distance, double speed) {
		int stopTime = TransportFerryHelper.isFerry(route) ? 0 : getStopTime(route.getType());
		return stopTime + distance / speed;
	}

	// the vehicle stands at the stop when the ride continues past it (in addition to getRideTime)
	public double getStandingTime(TransportRoute route, int stop) {
		return TransportFerryHelper.getStopTime(this, route, stop);
	}

	// getting off the vehicle at the stop
	public double getAlightingTime(TransportRoute route, int stop) {
		return TransportFerryHelper.getAlightingTime(this, route, stop);
	}

	// the vehicle of the route is carried over water on the way to the stop (a bus on a ferry)
	public double getCrossingTime(TransportRoute route, int stop) {
		return TransportFerryHelper.getCrossingTime(this, route, stop);
	}

	// the whole ride goes over water: walking can't replace it
	public boolean isOverWater(TransportRoute route) {
		return TransportFerryHelper.isFerry(route);
	}

	// the ride continues on the next route at the stop without getting off (it can't be reached on foot)
	public boolean isContinuation(TransportRoute route, int stop) {
		return TransportFerryHelper.isJunctionStop(route, stop);
	}

	// getting off at the stop is possible after boarding at the stop from
	public boolean canGetOff(TransportRoute route, int from, int stop) {
		return !TransportFerryHelper.isSameTerminal(route, from, stop);
	}

	public double getChangeTime(TransportRoute from, int stop, TransportRoute to) {
		return isContinuation(from, stop) ? 0 : getChangeTime(from.getType(), to.getType());
	}

	// the route geometry between two stops may go against the stops order
	// (parallel ways of ferry berths are merged into a way going there and back)
	public boolean isGeometryReversible(TransportRoute route) {
		return TransportFerryHelper.isFerry(route);
	}

	public int getBoardingTime(String routeType) {
		int time;
		if (boardingTimes.containsKey(routeType)) {
			time = boardingTimes.get(routeType);
		} else {
			RouteAttributeContext obstacles = router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES);
			time = obstacles.evaluateInt(getRawBitset("boarding", routeType), 0);
			boardingTimes.put(routeType, time);
		}
		if (time > 0) {
			return time;
		}
		if (defaultBoardingTime == 0) {
			RouteAttributeContext obstacles = router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES);
			defaultBoardingTime = obstacles.evaluateInt(getRawBitset("boarding", ""), 150);
		}
		return defaultBoardingTime;
	}
	
	public int getChangeTime(String fromRouteType, String toRouteType) {
		int time;
		String key = fromRouteType + "_" + toRouteType;
		if (changingTimes.containsKey(key)) {
			time = changingTimes.get(key);
		} else {
			RouteAttributeContext obstacles = router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES);
			time = obstacles.evaluateInt(getRawBitset("change", key), 0);
			changingTimes.put(key, time);
		}
		if (time > 0) {
			return time;
		}
		if (defaultChangeTime == 0) {
			RouteAttributeContext obstacles = router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES);
			defaultChangeTime = obstacles.evaluateInt(getRawBitset("change", ""), 240);
		}
		return defaultChangeTime;
	}

	
	public float getSpeedByRouteType(String routeType) {
		Float sl = speed.get(routeType);
		if(sl == null) {
			RouteAttributeContext spds = router.getObjContext(RouteDataObjectAttribute.ROAD_SPEED);
			sl = spds.evaluateFloat(getRawBitset("route", routeType), defaultTravelSpeed);
			speed.put(routeType, sl);
		}
		return sl.floatValue();
	}
	
	private int getRawType(String tg, String vl) {
		String key = tg + "$"+vl;
		if(!rawTypes.containsKey(key)) {
			int at = router.registerTagValueAttribute(tg, vl);
			rawTypes.put(key, at);
		}
		return rawTypes.get(key);
	}
	
	private BitSet getRawBitset(String tg, String vl) {
		BitSet bs = new BitSet();
		bs.set(getRawType(tg, vl));
		return bs;
	}
	

	public TransportRoutingConfiguration(RoutingConfiguration.Builder config, GeneralRouter prouter, Map<String, String> params) {
		if(prouter != null) {
			this.router = prouter.build(params);
			ferryBoardingTime = RoutingConfiguration.parseSilentInt(
					config.getAttribute(prouter, FerryRoutingHelper.BOARDING_TIME_ATTRIBUTE), 0);
			ferryTerminalTime = RoutingConfiguration.parseSilentInt(
					config.getAttribute(prouter, FerryRoutingHelper.TERMINAL_TIME_ATTRIBUTE), 0);
			walkRadius =  router.getIntAttribute("walkRadius", walkRadius);
			walkChangeRadius =  router.getIntAttribute("walkChangeRadius", walkChangeRadius);
			ZOOM_TO_LOAD_TILES =  router.getIntAttribute("zoomToLoadTiles", ZOOM_TO_LOAD_TILES);
			maxNumberOfChanges =  router.getIntAttribute("maxNumberOfChanges", maxNumberOfChanges);
			maxRouteTime =  router.getIntAttribute("maxRouteTime", maxRouteTime);
			maxRouteIncreaseSpeed =  router.getIntAttribute("maxRouteIncreaseSpeed", maxRouteIncreaseSpeed);
			maxRouteDistance =  router.getIntAttribute("maxRouteDistance", maxRouteDistance);

			increaseForAlternativesRoutes = 
					router.getFloatAttribute("increaseForAlternativesRoutes", (float) increaseForAlternativesRoutes);
			increaseForAltRoutesWalking = 
					router.getFloatAttribute("increaseForAltRoutesWalking", (float) increaseForAltRoutesWalking);
			
			combineAltRoutesDiffStops = router.getIntAttribute("combineAltRoutesDiffStops", combineAltRoutesDiffStops);
			combineAltRoutesSumDiffStops = router.getIntAttribute("combineAltRoutesSumDiffStops", combineAltRoutesSumDiffStops);
			
			maxNumberOfChanges =
					(int) RoutingConfiguration.parseSilentFloat(params.get("max_num_changes"), maxNumberOfChanges);
			ptLimitResultsByNumber =
					(int) RoutingConfiguration.parseSilentFloat(params.get("pt_limit"), ptLimitResultsByNumber);

			walkSpeed = router.getFloatAttribute("minDefaultSpeed", this.walkSpeed * 3.6f) / 3.6f;
			defaultTravelSpeed = router.getFloatAttribute("maxDefaultSpeed", this.defaultTravelSpeed * 3.6f) / 3.6f;
			
			RouteAttributeContext spds = router.getObjContext(RouteDataObjectAttribute.ROAD_SPEED);
			walkSpeed = spds.evaluateFloat(getRawBitset("route", "walk"), walkSpeed);
		}
	}
	
}
