package net.osmand.router;

import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public abstract class VehicleRouter {

	/**
	 * Accepts line to use it for routing
	 * 
	 * @param way
	 * @return
	 */
	public abstract boolean acceptLine(RouteDataObject way);
	
	public abstract boolean restrictionsAwayre();

	
	public int isOneWay(RouteDataObject road) {
		return road.getOneway();
	}
	
	public String getHighway(RouteDataObject road) {
		return road.getHighway();
	}
	
	
	/**
	 * Used for algorithm to multiply h(x) part A* based on current road
	 */
	public abstract double getFutureRoadPriority(RouteDataObject road);

	/**
	 * return delay in seconds
	 */
	public double defineObstacle(RouteDataObject road, int point) {
		// no obstacles
		return 0;
	}

	/**
	 * return speed in m/s for vehicle for specified road
	 */
	public abstract double defineSpeed(RouteDataObject road);
	
	/**
	 * define priority to multiply the speed for g(x) A* 
	 */
	public abstract double defineSpeedPriority(RouteDataObject road);

	/**
	 * Used for A* routing to calculate g(x)
	 * 
	 * @return minimal speed at road in m/s
	 */
	public abstract double getMinDefaultSpeed();

	/**
	 * Used for A* routing to predict h(x) : it should be great any g(x)
	 * 
	 * @return maximum speed to calculate shortest distance
	 */
	public abstract double getMaxDefaultSpeed();

	/**
	 * Calculate turn time 
	 */
	public abstract double calculateTurnTime(RouteSegment segment, int segmentEnd, RouteSegment prev, int prevSegmentEnd) ;
}