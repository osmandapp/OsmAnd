package net.osmand.router;


import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteDataObject;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public class CarRouter extends VehicleRouter {
	// no distinguish for speed in city/outside city (for now)
	private Map<String, Double> autoNotDefinedValues = new LinkedHashMap<String, Double>();
	private Map<String, Double> autoPriorityValues = new LinkedHashMap<String, Double>();
	{
		autoNotDefinedValues.put("motorway", 110d);
		autoNotDefinedValues.put("motorway_link", 80d);
		autoNotDefinedValues.put("trunk", 100d);
		autoNotDefinedValues.put("trunk_link", 80d);
		autoNotDefinedValues.put("primary", 65d);//generally linking larger towns.
		autoNotDefinedValues.put("primary_link", 45d);
		autoNotDefinedValues.put("secondary", 50d);//generally linking smaller towns and villages
		autoNotDefinedValues.put("secondary_link", 40d);
		autoNotDefinedValues.put("tertiary", 35d);//important urban roads
		autoNotDefinedValues.put("tertiary_link", 30d);
		autoNotDefinedValues.put("unclassified", 30d);//lowest form of grid network, usually 90% of urban roads
		autoNotDefinedValues.put("road", 30d);//road = no type, no review and may be not accurate
		autoNotDefinedValues.put("residential", 20d);//primarily for access to properties, small roads with 1/2 intersections
		autoNotDefinedValues.put("service", 15d);//parking + private roads
		autoNotDefinedValues.put("track", 15d);//very bad roads
		autoNotDefinedValues.put("path", 10d);//may not be usable by cars!!
		autoNotDefinedValues.put("living_street", 10d);//too small for cars usually
		//car are able to enter in highway=pedestrian with restrictions

		autoPriorityValues.put("motorway", 1.5);
		autoPriorityValues.put("motorway_link", 1.3); 
		autoPriorityValues.put("trunk", 1.5);
		autoPriorityValues.put("trunk_link", 1.3);
		autoPriorityValues.put("primary", 1.3d);
		autoPriorityValues.put("primary_link", 1.1d);
		autoPriorityValues.put("secondary", 1.1d);
		autoPriorityValues.put("secondary_link", 1.0d);
		autoPriorityValues.put("tertiary", 0.85d);
		autoPriorityValues.put("tertiary_link", 0.85d);
		autoPriorityValues.put("unclassified", 0.7d);
		autoPriorityValues.put("residential", 0.4d);
		autoPriorityValues.put("road", 0.4d);
		autoPriorityValues.put("service", 0.2d);
		autoPriorityValues.put("track", 0.2d);
		autoPriorityValues.put("path", 0.1d);
		autoPriorityValues.put("living_street", 0.1d);
	}

	@Override
	public boolean acceptLine(RouteDataObject way) {
		return autoNotDefinedValues.containsKey(way.getHighway());
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
				return 35;
			} else if(r.getType() == RouteTypeRule.RAILWAY_CROSSING) {
				return 20;
			}
		}
		return 0;
	}
	
	
	@Override
	public double getFutureRoadPriority(RouteDataObject road) {
		String highway = getHighway(road);
		double priority = highway != null && autoPriorityValues.containsKey(highway) ? autoPriorityValues.get(highway) : 0.5d;
		// keep it in boundaries otherwise
		// (it will use first founded exist for trunk even if it in another city and make Uturn there)
		if (priority > 1.4) {
			return 1.4d;
		} else if (priority < 0.5d) {
			return 0.5d;
		}
		return priority;
	};

	/**
	 * return speed in m/s
	 */
	@Override
	public double defineSpeed(RouteDataObject road) {
		String highway = null;
		RouteRegion reg = road.region;
		int sz = road.types.length;
		for (int i = 0; i < sz; i++) {
			RouteTypeRule r = reg.quickGetEncodingRule(road.types[i]);
			float maxSpeed = r.maxSpeed();
			if (maxSpeed > 0) {
				return maxSpeed;
			} else if (highway == null) {
				highway = r.highwayRoad();
			}
		}
		
		Double value = autoNotDefinedValues.get(highway);
		if (value == null) {
			value = 45d;
		}
		return value / 3.6d;
	}
	
	@Override
	public double defineSpeedPriority(RouteDataObject road) {
		String highway = road.getHighway();
		double priority = highway != null && autoPriorityValues.containsKey(highway) ? autoPriorityValues.get(highway) : 0.5d;
		return priority;
	}

	/**
	 * Used for A* routing to calculate g(x)
	 * 
	 * @return minimal speed at road
	 */
	@Override
	public double getMinDefaultSpeed() {
		return 12;
	}

	/**
	 * Used for A* routing to predict h(x) : it should be < (!) any g(x)
	 * 
	 * @return maximum speed to calculate shortest distance
	 */
	@Override
	public double getMaxDefaultSpeed() {
		return 30;
	}
	
	private double directionRoute(RouteSegment segment, int segmentEnd, boolean opp){
		boolean plus = segmentEnd == 0;
		int x = segment.road.getPoint31XTile(segmentEnd);
		int y = segment.road.getPoint31YTile(segmentEnd);
		int nx = segmentEnd;
		int px = x;
		int py = y;
		do {
			if(plus){
				nx++;
				if(nx >= segment.road.getPointsLength()){
					break;
				}
			} else {
				nx--;
				if(nx < 0){
					break;
				}
			}
			px = segment.road.getPoint31XTile(nx);
			py = segment.road.getPoint31YTile(nx);
		} while(Math.abs(px - x) + Math.abs(py - y) < 100);
		
		if(opp){
			return Math.atan2(py - y, px - x);
		} else {
			return Math.atan2(y - py, x - px);
		}
	}

	@Override
	public double calculateTurnTime(RouteSegment segment, RouteSegment next, int segmentEnd) {
		boolean end = (segmentEnd == segment.road.getPointsLength() - 1 || segmentEnd == 0);
		boolean start = next.segmentStart == 0 || next.segmentStart == next.getRoad().getPointsLength() - 1;
		// that addition highly affects to trunk roads !(prefer trunk/motorway)
		if (end && start) {
			// next.road.getId() >> 1 != segment.road.getId() >> 1
			if (next.road.getPointsLength() > 1) {
				double a1 = directionRoute(segment, segmentEnd, false);
				double a2 = directionRoute(next, next.segmentStart, true);
				double diff = Math.abs(a1 - a2);
				if (diff > Math.PI / 2 && diff < 3 * Math.PI / 2) {
					return 20;
				}
			}
			return 0;
		} else {
			return 15;
		}
	}

}