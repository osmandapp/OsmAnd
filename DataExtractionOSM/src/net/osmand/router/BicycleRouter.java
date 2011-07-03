package net.osmand.router;

import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.osm.MapRenderingTypes;
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
		bicycleNotDefinedValues.put("footway", 3d);
		bicycleNotDefinedValues.put("byway", 4d);
		bicycleNotDefinedValues.put("cycleway", 6d);
		bicycleNotDefinedValues.put("bridleway", 5d);
		bicycleNotDefinedValues.put("services", 5d);
		bicycleNotDefinedValues.put("steps", 1d);
		
		

		bicyclePriorityValues.put("motorway", 0.8);
		bicyclePriorityValues.put("motorway_link", 0.8);
		bicyclePriorityValues.put("trunk", 0.8);
		bicyclePriorityValues.put("trunk_link", 0.8);
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
		bicyclePriorityValues.put("cycleway", 1.5d);
		bicyclePriorityValues.put("bridleway", 1d);
		bicyclePriorityValues.put("services", 1d);
		bicyclePriorityValues.put("steps", 0.5d);
	}

	public boolean acceptLine(TagValuePair pair) {
		if (pair.tag.equals("highway")) {
			return bicycleNotDefinedValues.containsKey(pair.value);
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
					return 30;
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
		double speed = 4d;
		boolean highway = "highway".equals(pair.tag);
		double priority = highway && bicyclePriorityValues.containsKey(pair.value) ? bicyclePriorityValues.get(pair.value) : 1d;
		if (speed == 0 && highway) {
			Double value = bicycleNotDefinedValues.get(pair.value);
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
		return 2;
	}

	/**
	 * Used for A* routing to predict h(x) : it should be great than (!) any g(x)
	 * 
	 * @return maximum speed to calculate shortest distance
	 */
	public double getMaxDefaultSpeed() {
		return 9;
	}

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