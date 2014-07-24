package net.osmand.router;

import java.util.Map;

import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

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
	public float defineObstacle(RouteDataObject road, int point);
	
	/**
	 * return delay in seconds (0 no obstacles)
	 */
	public float defineRoutingObstacle(RouteDataObject road, int point);

	/**
	 * return routing speed in m/s for vehicle for specified road
	 */
	public float defineRoutingSpeed(RouteDataObject road);
	
	/**
	 * return real speed in m/s for vehicle for specified road
	 */
	public float defineVehicleSpeed(RouteDataObject road);
	
	/**
	 * define priority to multiply the speed for g(x) A* 
	 */
	public float defineSpeedPriority(RouteDataObject road);

	/**
	 * Used for A* routing to calculate g(x)
	 * 
	 * @return minimal speed at road in m/s
	 */
	public float getMinDefaultSpeed();

	/**
	 * Used for A* routing to predict h(x) : it should be great any g(x)
	 * 
	 * @return maximum speed to calculate shortest distance
	 */
	public float getMaxDefaultSpeed();
	
	/**
	 * aware of road restrictions
	 */
	public boolean restrictionsAware();
	
	/**
	 * Calculate turn time 
	 */
	public double calculateTurnTime(RouteSegment segment, int segmentEnd, RouteSegment prev, int prevSegmentEnd);
	
		
	public VehicleRouter build(Map<String, String> params);
	
}