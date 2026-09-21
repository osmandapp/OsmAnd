package net.osmand.router;

import java.util.List;
import java.util.Map;

import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.shared.routing.GeneralRouterProfile;

public interface VehicleRouter {

	
	public boolean containsAttribute(String attribute);
	
	public String getAttribute(String attribute);
	
	/**
	 * return if the road is accepted for routing
	 */
	public boolean acceptLine(RouteDataObject way);
	
	/**
	 * return oneway +/- 1 if it is oneway and 0 if both ways
	 */
	public int isOneWay(RouteDataObject road);
	
	/**
	 * return penalty transition in seconds
	 */
	public float getPenaltyTransition(RouteDataObject road);
	
	/**
	 * return delay in seconds (0 no obstacles)
	 */
	public float defineObstacle(RouteDataObject road, int point, boolean isBackwardDir);
	
	/**
	 * return delay in seconds for height obstacles
	 */
	public double defineHeightObstacle(RouteDataObject road, short startIndex, short endIndex);
	
	/**
	 * return delay in seconds (0 no obstacles)
	 */
	public float defineRoutingObstacle(RouteDataObject road, int point, boolean isBackwardDir);

	/**
	 * return routing speed in m/s for vehicle for specified road
	 */
	public float defineRoutingSpeed(RouteDataObject road, boolean dir);
	
	/**
	 * return real speed in m/s for vehicle for specified road
	 */
	public float defineVehicleSpeed(RouteDataObject road, boolean dir);
	
	/**
	 * define priority to multiply the speed for g(x) A* 
	 */
	public float defineSpeedPriority(RouteDataObject road, boolean dir);
	
	/**
	 * define destination priority
	 */
	float defineDestinationPriority(RouteDataObject road);

	/**
	 * Used for A* routing to calculate g(x)
	 * 
	 * @return minimal speed at road in m/s
	 */
	public float getDefaultSpeed();

	/**
	 * Used as minimal threshold of default speed
	 *
	 * @return minimal speed at road in m/s
	 */
	public float getMinSpeed();

	/**
	 * Used for A* routing to predict h(x) : it should be great any g(x)
	 * 
	 * @return maximum speed to calculate shortest distance
	 */
	public float getMaxSpeed();
	
	/**
	 * aware of road restrictions
	 */
	public boolean restrictionsAware();
	
	/**
	 * @param obj
	 * @return if road supports area routing
	 */
	public boolean isArea(RouteDataObject obj);
	
	/**
	 * Calculate turn time 
	 */
	public double calculateTurnTime(RouteSegment segment, RouteSegment prev);

	/**
	 * Calculate time to leave one road for another (e.g. getting on or off a ferry)
	 * @param from road segment before the change in the direction of travel
	 * @param to road segment after the change in the direction of travel
	 */
	default double calculateRoadChangeTime(RouteSegment from, RouteSegment to) {
		return 0;
	}

	/**
	 * Calculate time of a stop at the start point of the segment coming from its parent segment
	 * (e.g. a ferry at a terminal on the way)
	 */
	default double calculateStopTime(RouteSegment segment) {
		return 0;
	}

	/**
	 * Correct estimated times of the calculated route segments which depend on the whole route
	 * (e.g. getting on and off a ferry is paid only for a whole crossing)
	 */
	default void updateSegmentTimes(List<RouteSegmentResult> result) {
	}

		
	public VehicleRouter build(Map<String, String> params);

	public GeneralRouterProfile getProfile();
	
}