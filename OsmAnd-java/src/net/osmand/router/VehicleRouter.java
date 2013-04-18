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
	
	public abstract boolean restrictionsAware();

	public abstract int isOneWay(RouteDataObject road);
	
	public abstract boolean containsAttribute(String attribute);
	
	public abstract String getAttribute(String attribute);
	
	
	/**
	 * return delay in seconds
	 */
	public float defineObstacle(RouteDataObject road, int point) {
		// no obstacles
		return 0;
	}
	
	public float defineRoutingObstacle(RouteDataObject road, int point) {
		// no obstacles
		return 0;
	}

	/**
	 * return speed in m/s for vehicle for specified road
	 */
	public abstract float defineSpeed(RouteDataObject road);
	
	/**
	 * define priority to multiply the speed for g(x) A* 
	 */
	public abstract float defineSpeedPriority(RouteDataObject road);

	/**
	 * Used for A* routing to calculate g(x)
	 * 
	 * @return minimal speed at road in m/s
	 */
	public abstract float getMinDefaultSpeed();

	/**
	 * Used for A* routing to predict h(x) : it should be great any g(x)
	 * 
	 * @return maximum speed to calculate shortest distance
	 */
	public abstract float getMaxDefaultSpeed();
	
	public abstract VehicleRouter specifyParameter(String tag);

	/**
	 * Calculate turn time 
	 */
	public abstract double calculateTurnTime(RouteSegment segment, int segmentEnd, RouteSegment prev, int prevSegmentEnd) ;
}