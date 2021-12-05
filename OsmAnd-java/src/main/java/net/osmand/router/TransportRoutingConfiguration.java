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
	
	public int maxNumberOfChanges = 3;  
	
	public int finishTimeSeconds = 1200;

	public int maxRouteTime = 60 * 60 * 10; // 10 hours
	public int maxRouteDistance = 0; // distance for maxRouteTime
	public int maxRouteIncreaseSpeed = 30; // speed to increase route time



	public GeneralRouter router;
	// cache values from router for fast access
	public float walkSpeed = (float) (3.6 / 3.6); // m/s
	public float defaultTravelSpeed = (float) (60 / 3.6); // m/s
	public int stopTime = 30;
	public int changeTime = 180;
	public int boardingTime = 180;
	
	public boolean useSchedule;
	// 10 seconds based
	public int scheduleTimeOfDay = 12 * 60 * 6; // 12:00 - 60*6*12
	public int scheduleMaxTime = 50 * 6; // not appropriate variable, should be dynamic
	// day since 2000
	public int scheduleDayNumber;

	private Map<String, Integer> rawTypes = new HashMap<String, Integer>();
	private Map<String, Float> speed = new TreeMap<String, Float>();
	
	
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
	
	
	public int getChangeTime() {
		return useSchedule ? 0 : changeTime;
	}
	
	public int getBoardingTime() {
		return boardingTime;
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
			finishTimeSeconds =  router.getIntAttribute("delayForAlternativesRoutes", finishTimeSeconds);
			String mn = params.get("max_num_changes");
			maxNumberOfChanges = (int) RoutingConfiguration.parseSilentFloat(mn, maxNumberOfChanges);
			
			walkSpeed = router.getFloatAttribute("minDefaultSpeed", this.walkSpeed * 3.6f) / 3.6f;
			defaultTravelSpeed = router.getFloatAttribute("maxDefaultSpeed", this.defaultTravelSpeed * 3.6f) / 3.6f;
			
			RouteAttributeContext obstacles = router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES);
			stopTime =  obstacles.evaluateInt(getRawBitset("time", "stop"), stopTime);
			changeTime =  obstacles.evaluateInt(getRawBitset("time", "change"), changeTime);
			boardingTime =  obstacles.evaluateInt(getRawBitset("time", "boarding"), boardingTime);
					
			RouteAttributeContext spds = router.getObjContext(RouteDataObjectAttribute.ROAD_SPEED);
			walkSpeed = spds.evaluateFloat(getRawBitset("route", "walk"), walkSpeed);
			
			
		}
	}
	
}
