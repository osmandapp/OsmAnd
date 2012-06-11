package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;

import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteDataObject;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public class BicycleRouter extends VehicleRouter {
	// no distinguish for speed in city/outside city (for now)
	private Map<String, Double> bicycleNotDefinedValues = new LinkedHashMap<String, Double>();
	private Map<String, Double> bicyclePriorityValues = new LinkedHashMap<String, Double>();
	// in m/s
	{
		bicycleNotDefinedValues.put("motorway", 6d);
		bicycleNotDefinedValues.put("motorway_link", 6d);
		bicycleNotDefinedValues.put("trunk", 6d);
		bicycleNotDefinedValues.put("trunk_link", 6d);
		bicycleNotDefinedValues.put("primary", 6d);
		bicycleNotDefinedValues.put("primary_link", 6d);
		bicycleNotDefinedValues.put("secondary", 6d);
		bicycleNotDefinedValues.put("secondary_link", 6d);
		bicycleNotDefinedValues.put("tertiary", 6d);
		bicycleNotDefinedValues.put("tertiary_link", 6d);
		bicycleNotDefinedValues.put("residential", 6d);
		bicycleNotDefinedValues.put("road", 6d);
		bicycleNotDefinedValues.put("service", 5d);
		bicycleNotDefinedValues.put("unclassified", 5d);
		bicycleNotDefinedValues.put("track", 4d);
		bicycleNotDefinedValues.put("path", 4d);
		bicycleNotDefinedValues.put("living_street", 5d);
		bicycleNotDefinedValues.put("pedestrian", 3d);
		bicycleNotDefinedValues.put("footway", 4d);
		bicycleNotDefinedValues.put("byway", 4d);
		bicycleNotDefinedValues.put("cycleway", 6d);
		bicycleNotDefinedValues.put("bridleway", 3d);
		bicycleNotDefinedValues.put("services", 5d);
		bicycleNotDefinedValues.put("steps", 1d);
		
		

		bicyclePriorityValues.put("motorway", 0.7);
		bicyclePriorityValues.put("motorway_link", 0.7);
		bicyclePriorityValues.put("trunk", 0.7);
		bicyclePriorityValues.put("trunk_link", 0.7);
		bicyclePriorityValues.put("primary", 0.9);
		bicyclePriorityValues.put("primary_link", 0.9);
		bicyclePriorityValues.put("secondary", 1d);
		bicyclePriorityValues.put("secondary_link", 1.0d);
		bicyclePriorityValues.put("tertiary", 1.0d);
		bicyclePriorityValues.put("tertiary_link", 1.0d);
		bicyclePriorityValues.put("residential", 1d);
		bicyclePriorityValues.put("service", 1d);
		bicyclePriorityValues.put("unclassified", 0.9d);
		bicyclePriorityValues.put("road", 1d);
		bicyclePriorityValues.put("track", 0.9d);
		bicyclePriorityValues.put("path", 0.9d);
		bicyclePriorityValues.put("living_street", 1d);
		bicyclePriorityValues.put("pedestrian", 0.9d);
		bicyclePriorityValues.put("footway", 0.9d);
		bicyclePriorityValues.put("byway", 1d);
		bicyclePriorityValues.put("cycleway", 1.3d);
		bicyclePriorityValues.put("bridleway", 0.8d);
		bicyclePriorityValues.put("services", 1d);
		bicyclePriorityValues.put("steps", 0.6d);
	}

	@Override
	public boolean acceptLine(RouteDataObject way) {
		return bicycleNotDefinedValues.containsKey(way.getHighway());
	}


	/**
	 * return delay in seconds
	 */
	@Override
	public double defineObstacle(RouteDataObject road, int point) {
		TIntArrayList pointTypes = road.getPointTypes(point);
		if(pointTypes == null) {
			return 0;
		}
		RouteRegion reg = road.region;
		int sz = pointTypes.size();
		for(int i=0; i<sz; i++) {
			RouteTypeRule r = reg.quickGetEncodingRule(pointTypes.getQuick(i));
			if(r.getType() == RouteTypeRule.TRAFFIC_SIGNALS) {
				return 30;
			} else if(r.getType() == RouteTypeRule.RAILWAY_CROSSING) {
				return 15;
			}
		}
		return 0;
	}
	
	@Override
	public double getFutureRoadPriority(RouteDataObject road) {
		String highway = getHighway(road);
		double priority = bicyclePriorityValues.containsKey(highway) ? bicyclePriorityValues.get(highway) : 1d;
		return priority;
	}

	/**
	 * return speed in m/s
	 */
	@Override
	public double defineSpeed(RouteDataObject road) {
		Double value = bicycleNotDefinedValues.get(getHighway(road));
		if (value == null) {
			value = 4d;
		}
		return value / 3.6d ;
	}
	
	@Override
	public double defineSpeedPriority(RouteDataObject road) {
		String highway = getHighway(road);
		double priority = bicyclePriorityValues.containsKey(highway) ? bicyclePriorityValues.get(highway) : 0.5d;
		return priority;
	}


	/**
	 * Used for A* routing to calculate g(x)
	 * 
	 * @return minimal speed at road
	 */
	@Override
	public double getMinDefaultSpeed() {
		return 2;
	}

	/**
	 * Used for A* routing to predict h(x) : it should be great than (!) any g(x)
	 * 
	 * @return maximum speed to calculate shortest distance
	 */
	@Override
	public double getMaxDefaultSpeed() {
		return 6;
	}

	@Override
	public double calculateTurnTime(RouteSegment segment, RouteSegment next, int segmentEnd) {
		boolean end = (segmentEnd == segment.road.getPointsLength() - 1 || segmentEnd == 0);
		boolean start = next.segmentStart == 0;
		if (end) {
			if(!start){
				return 5;
			}
			return 0;
		} else {
			return 5;
		}
	}

}