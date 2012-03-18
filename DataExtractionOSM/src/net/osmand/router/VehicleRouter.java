package net.osmand.router;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public abstract class VehicleRouter {

	/**
	 * Accepts line to use it for routing
	 * 
	 * @param pair
	 * @return
	 */
	public abstract boolean acceptLine(TagValuePair pair);

	/**
	 * Accepts point to use it for routing
	 * 
	 * @param pair
	 * @return
	 */
	public abstract boolean acceptPoint(TagValuePair pair);

	
	public int getHighwayAttributes(BinaryMapDataObject road){
		throw new UnsupportedOperationException();
	}
	
	public boolean isOneWay(BinaryMapDataObject road) {
		int attributes = getHighwayAttributes(road);
		return MapRenderingTypes.isOneWayWay(attributes) || MapRenderingTypes.isRoundabout(attributes);
	}
	
	/**
	 * Used for algorithm of increasing road priorities (actually make sense only for car routing)
	 * other routers can increase/decrease road priorities in the middle of route
	 * @param road
	 * @return
	 */
	public double getRoadPriorityHeuristicToIncrease(BinaryMapDataObject road) {
		return 1;
	}
	
	
	/**
	 * Used for algorithm to estimate end distance
	 * @param road
	 * @return
	 */
	public double getRoadPriorityToCalculateRoute(BinaryMapDataObject road) {
		return 1;
	}

	/**
	 * return delay in seconds
	 */
	public double defineObstacle(BinaryMapDataObject road, int point) {
		// no obstacles
		return 0;
	}

	/**
	 * return speed in m/s for vehicle
	 */
	public abstract double defineSpeed(BinaryMapDataObject road);

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
	public abstract double calculateTurnTime(RouteSegment segment, RouteSegment next, int segmentEnd) ;
}