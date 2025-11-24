package net.osmand.router;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.router.GeneralRouter.RouteAttributeContext;
import net.osmand.router.GeneralRouter.RouteDataObjectAttribute;

public class TransportRoutingConfiguration {

	public int ZOOM_TO_LOAD_TILES = 15;
	
	public int walkRadius = 1500; // ? 3000
	
	public int walkChangeRadius = 300; 
	
	public int maxNumberOfChanges = 2; // replaced with max_num_changes
	
	public int finishTimeSeconds = 1200; // deprecated // TODO remove after JNI fix
	
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
	

	public TransportRoutingConfiguration(GeneralRouter prouter, Map<String, String> params) {
		if(prouter != null) {
			this.router = prouter.build(params);
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
			
			String mn = params.get("max_num_changes");
			maxNumberOfChanges = (int) RoutingConfiguration.parseSilentFloat(mn, maxNumberOfChanges);
			
			walkSpeed = router.getFloatAttribute("minDefaultSpeed", this.walkSpeed * 3.6f) / 3.6f;
			defaultTravelSpeed = router.getFloatAttribute("maxDefaultSpeed", this.defaultTravelSpeed * 3.6f) / 3.6f;
			
			RouteAttributeContext spds = router.getObjContext(RouteDataObjectAttribute.ROAD_SPEED);
			walkSpeed = spds.evaluateFloat(getRawBitset("route", "walk"), walkSpeed);
			
			
		}
	}
	
}
