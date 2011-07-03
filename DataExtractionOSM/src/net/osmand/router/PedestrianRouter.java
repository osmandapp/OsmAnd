package net.osmand.router;

import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public class PedestrianRouter extends VehicleRouter {
	// no distinguish for speed in city/outside city (for now)
	private Map<String, Double> pedestrianNotDefinedValues = new LinkedHashMap<String, Double>();
	private Map<String, Double> pedestrianPriorityValues = new LinkedHashMap<String, Double>();
	// in m/s
	{
		pedestrianNotDefinedValues.put("motorway", 1.8d);
		pedestrianNotDefinedValues.put("motorway_link", 1.8d);
		pedestrianNotDefinedValues.put("trunk", 1.8d);
		pedestrianNotDefinedValues.put("trunk_link", 1.8d);
		pedestrianNotDefinedValues.put("primary", 1.8d);
		pedestrianNotDefinedValues.put("primary_link", 1.8d);
		pedestrianNotDefinedValues.put("secondary", 1.8d);
		pedestrianNotDefinedValues.put("secondary_link", 1.8d);
		pedestrianNotDefinedValues.put("tertiary", 1.8d);
		pedestrianNotDefinedValues.put("tertiary_link", 1.8d);
		pedestrianNotDefinedValues.put("residential", 1.8d);
		pedestrianNotDefinedValues.put("road", 1.8d);
		pedestrianNotDefinedValues.put("service", 1.8d);
		pedestrianNotDefinedValues.put("unclassified", 1.8d);
		pedestrianNotDefinedValues.put("track", 1.5d);
		pedestrianNotDefinedValues.put("path", 1.5d);
		pedestrianNotDefinedValues.put("living_street", 1.8d);
		pedestrianNotDefinedValues.put("pedestrian", 1.8d);
		pedestrianNotDefinedValues.put("footway", 1.8d);
		pedestrianNotDefinedValues.put("byway", 1.8d);
		pedestrianNotDefinedValues.put("cycleway", 1.8d);
		pedestrianNotDefinedValues.put("bridleway", 1.8d);
		pedestrianNotDefinedValues.put("services", 1.8d);
		pedestrianNotDefinedValues.put("steps", 1.5d);
		
		

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

	public boolean acceptLine(TagValuePair pair) {
		if (pair.tag.equals("highway")) {
			return pedestrianNotDefinedValues.containsKey(pair.value);
		}
		return false;
	}

	public boolean acceptPoint(TagValuePair pair) {
		if (pair.tag.equals("highway") && pair.value.equals("traffic_signals")) {
			return true;
		} else if (pair.tag.equals("railway") && pair.value.equals("crossing")) {
			return true;
		} else if (pair.tag.equals("railway") && pair.value.equals("level_crossing")) {
			return true;
		}
		return false;
	}

	public boolean isOneWay(int highwayAttributes) {
		return MapRenderingTypes.isOneWayWay(highwayAttributes) || MapRenderingTypes.isRoundabout(highwayAttributes);
	}

	/**
	 * return delay in seconds
	 */
	public double defineObstacle(BinaryMapDataObject road, int point) {
		if ((road.getTypes()[0] & 3) == MapRenderingTypes.POINT_TYPE) {
			// possibly not only first type needed ?
			TagValuePair pair = road.getTagValue(0);
			if (pair != null) {
				if (pair.tag.equals("highway") && pair.value.equals("traffic_signals")) {
					return 20;
				} else if (pair.tag.equals("railway") && pair.value.equals("crossing")) {
					return 15;
				} else if (pair.tag.equals("railway") && pair.value.equals("level_crossing")) {
					return 15;
				}
			}
		}
		return 0;
	}

	/**
	 * return speed in m/s
	 */
	public double defineSpeed(BinaryMapDataObject road) {
		TagValuePair pair = road.getTagValue(0);
		double speed = 1.5d;
		boolean highway = "highway".equals(pair.tag);
		double priority = highway && pedestrianPriorityValues.containsKey(pair.value) ? pedestrianPriorityValues.get(pair.value) : 1d;
		if (speed == 0 && highway) {
			Double value = pedestrianNotDefinedValues.get(pair.value);
			if (value != null) {
				speed = value;
			}
		}
		return speed * priority;
	}

	/**
	 * Used for A* routing to calculate g(x)
	 * 
	 * @return minimal speed at road
	 */
	public double getMinDefaultSpeed() {
		return 1;
	}

	/**
	 * Used for A* routing to predict h(x) : it should be great than (!) any g(x)
	 * 
	 * @return maximum speed to calculate shortest distance
	 */
	public double getMaxDefaultSpeed() {
		return 2;
	}

	public double calculateTurnTime(int middley, int middlex, int x, int y, RouteSegment segment, RouteSegment next, int j) {
		return 0;
	}

}