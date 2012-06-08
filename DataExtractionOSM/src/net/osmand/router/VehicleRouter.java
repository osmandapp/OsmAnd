package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteDataObject;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public abstract class VehicleRouter {

	/**
	 * Accepts line to use it for routing
	 * 
	 * @param way
	 * @return
	 */
	public abstract boolean acceptLine(RouteDataObject way);

	
	public int isOneWay(RouteDataObject road) {
		RouteRegion reg = road.region;
		int sz = road.types.size();
		for(int i=0; i<sz; i++) {
			RouteTypeRule r = reg.quickGetEncodingRule(road.types.getQuick(i));
			if(r.onewayDirection() != 0) {
				return r.onewayDirection();
			} else if(r.roundabout()) {
				return 1;
			}
		}
		return 0;
	}
	
	public String getHighway(RouteDataObject road) {
		return road.getHighway();
	}
	
	/**
	 * Used for algorithm of increasing road priorities (actually make sense only for car routing)
	 * other routers can increase/decrease road priorities in the middle of route
	 * @param road
	 * @return
	 */
	public double getRoadPriorityHeuristicToIncrease(RouteDataObject road) {
		return 1;
	}
	
	
	/**
	 * Used for algorithm to estimate end distance
	 * @param road
	 * @return
	 */
	public double getRoadPriorityToCalculateRoute(RouteDataObject road) {
		return 1;
	}

	/**
	 * return delay in seconds
	 */
	public double defineObstacle(RouteDataObject road, int point) {
		// no obstacles
		return 0;
	}

	/**
	 * return speed in m/s for vehicle
	 */
	public abstract double defineSpeed(RouteDataObject road);

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