package net.osmand.router;

import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.binary.RouteDataObject;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.osm.MapUtils;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public class GeneralRouter extends VehicleRouter {
	Map<String, Double> highwaySpeed = new LinkedHashMap<String, Double>();
	Map<String, Double> highwayPriorities = new LinkedHashMap<String, Double>();
	Map<String, Double> highwayFuturePriorities = new LinkedHashMap<String, Double>();
	Map<String, Double> avoid = new LinkedHashMap<String, Double>();
	Map<String, Double> obstacles = new LinkedHashMap<String, Double>();
	boolean followSpeedLimitations = true;
	boolean restrictionsAware = true;
	boolean onewayAware = true;
	double minDefaultSpeed = 10;
	double maxDefaultSpeed = 10;
	double leftTurn = 0;
	double roundaboutTurn = 0;
	double rightTurn = 0;
	GeneralRouterProfile profile;
	
	public enum GeneralRouterProfile {
		CAR,
		PEDESTRIAN,
		BICYCLE
	}

	@Override
	public boolean acceptLine(RouteDataObject way) {
		if(!highwaySpeed.containsKey(way.getHighway())) {
			return false;
		}
		int[] s = way.getTypes();
		
		for(int i=0; i<s.length; i++) {
			RouteTypeRule r = way.region.quickGetEncodingRule(s[i]);
			String k = r.getTag() + "$" + r.getValue();
			if(avoid.containsKey(k)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean restrictionsAwayre() {
		return restrictionsAware;
	}
	
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
			String key = r.getTag() + "$" + r.getValue();
			Double v = obstacles.get(key);
			if(v != null ){
				return v;
			}
		}
		return 0;
	}
	
	@Override
	public int isOneWay(RouteDataObject road) {
		if (!onewayAware) {
			return 0;
		}
		return super.isOneWay(road);
	}

	@Override
	public double getFutureRoadPriority(RouteDataObject road) {
		String highway = road.getHighway();
		double priority = highway != null && highwayFuturePriorities.containsKey(highway) ? highwayFuturePriorities.get(highway) : 1d;
		return priority;
	}

	@Override
	public double defineSpeed(RouteDataObject road) {
		if (followSpeedLimitations) {
			float m = road.getMaximumSpeed();
			if(m > 0) {
				return m;
			}
		}
		
		Double value = null;
		for (int i = 0; i < road.types.length; i++) {
			RouteTypeRule r = road.region.quickGetEncodingRule(road.types[i]);
			String highway = r.highwayRoad();
			if (highway != null && highwaySpeed.containsKey(highway)) {
				value = highwaySpeed.get(highway);
				break;
			} else if(highwaySpeed.containsKey(r.getTag()+"$"+r.getValue())){
				value = highwaySpeed.get(r.getTag()+"$"+r.getValue());
				break;
			}
		}
		if (value == null) {
			value = minDefaultSpeed;
		}
		return value / 3.6d;
	}

	@Override
	public double defineSpeedPriority(RouteDataObject road) {
		double priority = 1;
		for (int i = 0; i < road.types.length; i++) {
			RouteTypeRule r = road.region.quickGetEncodingRule(road.types[i]);
			String highway = r.highwayRoad();
			if (highway != null && highwayPriorities.containsKey(highway)) {
				priority *= highwayPriorities.get(highway);
			} else if(highwayPriorities.containsKey(r.getTag()+"$"+r.getValue())){
				priority *= highwayPriorities.get(r.getTag()+"$"+r.getValue());
			}
		}
		return priority;
	}

	@Override
	public double getMinDefaultSpeed() {
		return minDefaultSpeed / 3.6d;
	}

	@Override
	public double getMaxDefaultSpeed() {
		return maxDefaultSpeed / 3.6d;
	}

	
	@Override
	public double calculateTurnTime(RouteSegment segment, int segmentEnd, RouteSegment prev, int prevSegmentEnd) {
		int[] pt = prev.getRoad().getPointTypes(prevSegmentEnd);
		if(pt != null) {
			RouteRegion reg = prev.getRoad().region;
			for(int i=0; i<pt.length; i++) {
				RouteTypeRule r = reg.quickGetEncodingRule(pt[i]);
				if("highway".equals(r.getTag()) && "traffic_signals".equals(r.getValue())) {
					// traffic signals don't add turn info 
					return 0;
				}
			}
		}
		if(roundaboutTurn > 0 && !prev.getRoad().roundabout() && segment.getRoad().roundabout()) {
			return roundaboutTurn;
		}
		if (leftTurn > 0 || rightTurn > 0) {
			double a1 = segment.getRoad().directionRoute(segment.segmentStart, segment.segmentStart < segmentEnd);
			double a2 = prev.getRoad().directionRoute(prevSegmentEnd, segmentEnd < prev.segmentStart);
			double diff = Math.abs(MapUtils.alignAngleDifference(a1 - a2 - Math.PI));
			// more like UT
			if (diff > 2 * Math.PI / 3) {
				return leftTurn;
			} else if (diff > Math.PI / 2) {
				return rightTurn;
			}
			return 0;
		}
		return 0;
	}
	

}
