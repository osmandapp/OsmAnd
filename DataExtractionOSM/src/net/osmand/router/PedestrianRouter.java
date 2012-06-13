package net.osmand.router;


import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteDataObject;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public class PedestrianRouter extends VehicleRouter {
	// no distinguish for speed in city/outside city (for now)
	private Map<String, Double> pedestrianNotDefinedValues = new LinkedHashMap<String, Double>();
	private Map<String, Double> pedestrianPriorityValues = new LinkedHashMap<String, Double>();
	// in m/s
	{
		pedestrianNotDefinedValues.put("motorway", 1.2d);
		pedestrianNotDefinedValues.put("motorway_link", 1.2d);
		pedestrianNotDefinedValues.put("trunk", 1.2d);
		pedestrianNotDefinedValues.put("trunk_link", 1.2d);
		pedestrianNotDefinedValues.put("primary", 1.3d);
		pedestrianNotDefinedValues.put("primary_link", 1.3d);
		pedestrianNotDefinedValues.put("secondary", 1.4d);
		pedestrianNotDefinedValues.put("secondary_link", 1.4d);
		pedestrianNotDefinedValues.put("tertiary", 1.8d);
		pedestrianNotDefinedValues.put("tertiary_link", 1.8d);
		pedestrianNotDefinedValues.put("residential", 1.8d);
		pedestrianNotDefinedValues.put("road", 1.8d);
		pedestrianNotDefinedValues.put("service", 1.8d);
		pedestrianNotDefinedValues.put("unclassified", 1.8d);
		pedestrianNotDefinedValues.put("track", 1.5d);
		pedestrianNotDefinedValues.put("path", 1.5d);
		pedestrianNotDefinedValues.put("living_street", 2d);
		pedestrianNotDefinedValues.put("pedestrian", 2d);
		pedestrianNotDefinedValues.put("footway", 2d);
		pedestrianNotDefinedValues.put("byway", 1.8d);
		pedestrianNotDefinedValues.put("cycleway", 1.8d);
		pedestrianNotDefinedValues.put("bridleway", 1.8d);
		pedestrianNotDefinedValues.put("services", 1.8d);
		pedestrianNotDefinedValues.put("steps", 1.3d);
		
		

		pedestrianPriorityValues.put("motorway", 0.7);
		pedestrianPriorityValues.put("motorway_link", 0.7);
		pedestrianPriorityValues.put("trunk", 0.7);
		pedestrianPriorityValues.put("trunk_link", 0.7);
		pedestrianPriorityValues.put("primary", 0.8);
		pedestrianPriorityValues.put("primary_link", 0.8);
		pedestrianPriorityValues.put("secondary", 0.8);
		pedestrianPriorityValues.put("secondary_link", 0.8d);
		pedestrianPriorityValues.put("tertiary", 0.9d);
		pedestrianPriorityValues.put("tertiary_link", 0.9d);
		pedestrianPriorityValues.put("residential", 1d);
		pedestrianPriorityValues.put("service", 1d);
		pedestrianPriorityValues.put("unclassified", 1d);
		pedestrianPriorityValues.put("road", 1d);
		pedestrianPriorityValues.put("track", 1d);
		pedestrianPriorityValues.put("path", 1d);
		pedestrianPriorityValues.put("living_street", 1d);
		pedestrianPriorityValues.put("pedestrian", 1.2d);
		pedestrianPriorityValues.put("footway", 1.2d);
		pedestrianPriorityValues.put("byway", 1d);
		pedestrianPriorityValues.put("cycleway", 0.9d);
		pedestrianPriorityValues.put("bridleway", 0.9d);
		pedestrianPriorityValues.put("services", 1d);
		pedestrianPriorityValues.put("steps", 1.2d);
	}

	@Override
	public double getFutureRoadPriority(RouteDataObject road) {
		String highway = getHighway(road);
		double priority = pedestrianPriorityValues.containsKey(highway) ? pedestrianPriorityValues.get(highway) : 1d;
		return priority;
	}
	
	@Override
	public double defineSpeedPriority(RouteDataObject road) {
		String highway = getHighway(road);
		double priority = pedestrianPriorityValues.containsKey(highway) ? pedestrianPriorityValues.get(highway) : 1d;
		return priority;
	}

	@Override
	public int isOneWay(RouteDataObject road) {
		return 0;
	}

	@Override
	public boolean acceptLine(RouteDataObject way) {
		return pedestrianNotDefinedValues.containsKey(way.getHighway());
	}


	public boolean isOneWay(int highwayAttributes) {
		return MapRenderingTypes.isOneWayWay(highwayAttributes) || MapRenderingTypes.isRoundabout(highwayAttributes);
	}

	/**
	 * return delay in seconds
	 */
	@Override
	public double defineObstacle(RouteDataObject road, int point) {
		int[] pointTypes = road.getPointTypes(point);
		if(pointTypes == null) {
			return 0;
		}
		RouteRegion reg = road.region;
		int sz = pointTypes.length;
		for(int i=0; i<sz; i++) {
			RouteTypeRule r = reg.quickGetEncodingRule(pointTypes[i]);
			if(r.getType() == RouteTypeRule.TRAFFIC_SIGNALS) {
				return 20;
			} else if(r.getType() == RouteTypeRule.RAILWAY_CROSSING) {
				return 15;
			}
		}
		return 0;
	}

	/**
	 * return speed in m/s
	 */
	@Override
	public double defineSpeed(RouteDataObject road) {
		double speed = 1.5d;
		String highway = getHighway(road);
		if (highway != null) {
			Double value = pedestrianNotDefinedValues.get(highway);
			if (value != null) {
				speed = value;
			}
		}
		return speed;
	}

	/**
	 * Used for A* routing to calculate g(x)
	 * 
	 * @return minimal speed at road
	 */
	@Override
	public double getMinDefaultSpeed() {
		return 1;
	}

	/**
	 * Used for A* routing to predict h(x) : it should be great than (!) any g(x)
	 * 
	 * @return maximum speed to calculate shortest distance
	 */
	@Override
	public double getMaxDefaultSpeed() {
		return 1.8;
	}

	@Override
	public double calculateTurnTime(RouteSegment segment, RouteSegment next, int j) {
		return 0;
	}

}