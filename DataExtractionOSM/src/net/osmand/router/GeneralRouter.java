package net.osmand.router;

import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.binary.RouteDataObject;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.osm.MapUtils;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

public class GeneralRouter extends VehicleRouter {
	public static final String USE_SHORTEST_WAY = "short_way";
	
	Map<String, Double> highwaySpeed ;
	Map<String, Double> highwayPriorities ;
	Map<String, Double> highwayFuturePriorities ;
	Map<String, Double> avoid ;
	Map<String, Double> obstacles;
	Map<String, String> attributes;
	
	
	private GeneralRouterProfile profile;

	// cached values
	private Boolean restrictionsAware;
	private Double leftTurn;
	private Double roundaboutTurn;
	private Double rightTurn;
	private Boolean onewayAware;
	private Boolean followSpeedLimitations;
	private Double minDefaultSpeed;
	private Double maxDefaultSpeed;

	public enum GeneralRouterProfile {
		CAR,
		PEDESTRIAN,
		BICYCLE
	}
	
	public GeneralRouter(GeneralRouterProfile profile, Map<String, String> attributes) {
		this.attributes = new LinkedHashMap<String, String>(attributes);
		this.profile = profile;
		highwaySpeed = new LinkedHashMap<String, Double>();
		highwayPriorities = new LinkedHashMap<String, Double>();
		highwayFuturePriorities = new LinkedHashMap<String, Double>();
		avoid = new LinkedHashMap<String, Double>();
		obstacles = new LinkedHashMap<String, Double>();
	}

	public GeneralRouter(GeneralRouter pr) {
		this.highwaySpeed = new LinkedHashMap<String, Double>(pr.highwaySpeed);
		this.highwayPriorities = new LinkedHashMap<String, Double>(pr.highwayPriorities);
		this.highwayFuturePriorities = new LinkedHashMap<String, Double>(pr.highwayFuturePriorities);
		this.avoid = new LinkedHashMap<String, Double>(pr.avoid);
		this.obstacles = new LinkedHashMap<String, Double>(pr.obstacles);
		this.attributes = new LinkedHashMap<String, String>(pr.attributes);
		this.profile = pr.profile;
	}

	@Override
	public boolean acceptLine(RouteDataObject way) {
		if(!highwaySpeed.containsKey(way.getHighway())) {
			boolean accepted = false;
			for (int i = 0; i < way.types.length; i++) {
				RouteTypeRule r = way.region.quickGetEncodingRule(way.types[i]);
				Double sp = highwaySpeed.get(r.getTag()+"$"+r.getValue());
				if(sp != null){
					if(sp.doubleValue() > 0) {
						accepted = true;
					}
					break;
				}
			}
			if(!accepted) {
				return false;
			}
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
	public boolean restrictionsAware() {
		if(restrictionsAware == null) {
			restrictionsAware = parseSilentBoolean(attributes.get("restrictionsAware"), true);
		}
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
	
	public boolean isOnewayAware() {
		if(onewayAware == null) {
			onewayAware = parseSilentBoolean(attributes.get("onewayAware"), true);
		}
		return onewayAware;
	}
	
	@Override
	public int isOneWay(RouteDataObject road) {
		if (!isOnewayAware()) {
			return 0;
		}
		return super.isOneWay(road);
	}

	@Override
	public double getFutureRoadPriority(RouteDataObject road) {
		double priority = 1;
		for (int i = 0; i < road.types.length; i++) {
			RouteTypeRule r = road.region.quickGetEncodingRule(road.types[i]);
			if(highwayFuturePriorities.containsKey(r.getTag()+"$"+r.getValue())){
				priority = highwayFuturePriorities.get(r.getTag()+"$"+r.getValue());
				break;
			}
		}
		return priority;
	}
	
	public boolean isFollowSpeedLimitations(){
		if(followSpeedLimitations == null){
			followSpeedLimitations = parseSilentBoolean(attributes.get("followSpeedLimitations"), true);
		}
		return followSpeedLimitations;
	}
	
	private static boolean parseSilentBoolean(String t, boolean v) {
		if (t == null || t.length() == 0) {
			return v;
		}
		return Boolean.parseBoolean(t);
	}

	private static double parseSilentDouble(String t, double v) {
		if (t == null || t.length() == 0) {
			return v;
		}
		return Double.parseDouble(t);
	}

	@Override
	public double defineSpeed(RouteDataObject road) {
		if (isFollowSpeedLimitations()) {
			float m = road.getMaximumSpeed();
			if(m > 0) {
				return m;
			}
		}
		
		Double value = null;
		for (int i = 0; i < road.types.length; i++) {
			RouteTypeRule r = road.region.quickGetEncodingRule(road.types[i]);
			if(highwaySpeed.containsKey(r.getTag()+"$"+r.getValue())){
				value = highwaySpeed.get(r.getTag()+"$"+r.getValue());
				break;
			}
		}
		if (value == null) {
			return getMinDefaultSpeed();
		}
		return value / 3.6d;
	}

	@Override
	public double defineSpeedPriority(RouteDataObject road) {
		double priority = 1;
		for (int i = 0; i < road.types.length; i++) {
			RouteTypeRule r = road.region.quickGetEncodingRule(road.types[i]);
			if(highwayPriorities.containsKey(r.getTag()+"$"+r.getValue())){
				priority *= highwayPriorities.get(r.getTag()+"$"+r.getValue());
			}
		}
		return priority;
	}

	@Override
	public double getMinDefaultSpeed() {
		if(minDefaultSpeed == null ){
			minDefaultSpeed = parseSilentDouble(attributes.get("minDefaultSpeed"), 10);
		}
		return minDefaultSpeed / 3.6d;
	}

	@Override
	public double getMaxDefaultSpeed() {
		if(maxDefaultSpeed == null ){
			maxDefaultSpeed = parseSilentDouble(attributes.get("maxDefaultSpeed"), 10);
		}
		return maxDefaultSpeed / 3.6d;
	}

	
	public double getLeftTurn() {
		if(leftTurn == null) {
			leftTurn = parseSilentDouble(attributes.get("leftTurn"), 0);
		}
		return leftTurn;
	}
	
	public double getRightTurn() {
		if(rightTurn == null) {
			rightTurn = parseSilentDouble(attributes.get("rightTurn"), 0);
		}
		return rightTurn;
	}
	public double getRoundaboutTurn() {
		if(roundaboutTurn == null) {
			roundaboutTurn = parseSilentDouble(attributes.get("roundaboutTurn"), 0);
		}
		return roundaboutTurn;
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
		double rt = getRoundaboutTurn();
		if(rt > 0 && !prev.getRoad().roundabout() && segment.getRoad().roundabout()) {
			return rt;
		}
		if (getLeftTurn() > 0 || getRightTurn() > 0) {
			double a1 = segment.getRoad().directionRoute(segment.segmentStart, segment.segmentStart < segmentEnd);
			double a2 = prev.getRoad().directionRoute(prevSegmentEnd, prevSegmentEnd < prev.segmentStart);
			double diff = Math.abs(MapUtils.alignAngleDifference(a1 - a2 - Math.PI));
			// more like UT
			if (diff > 2 * Math.PI / 3) {
				return getLeftTurn();
			} else if (diff > Math.PI / 2) {
				return getRightTurn();
			}
			return 0;
		}
		return 0;
	}

	@Override
	public GeneralRouter specialization(String specializationTag) {
		GeneralRouter gr = new GeneralRouter(this);
		return gr;
	}
	

}
