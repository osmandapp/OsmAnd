package net.osmand.router;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.util.MapUtils;

public class GeneralRouter extends VehicleRouter {
	public static final String USE_SHORTEST_WAY = "short_way";
	public static final String AVOID_FERRIES = "avoid_ferries";
	public static final String AVOID_TOLL = "avoid_toll";
	public static final String AVOID_MOTORWAY = "avoid_motorway";
	public static final String AVOID_UNPAVED = "avoid_unpaved";
	public static final String PREFER_MOTORWAYS = "prefer_motorway";
	
	public Map<String, Float> highwaySpeed ;
	public Map<String, Float> highwayPriorities ;
	public Map<String, Float> avoid ;
	public Map<String, Float> obstacles;
	public Map<String, Float> routingObstacles;
	public Map<String, String> attributes;
	
	
	private GeneralRouterProfile profile;

	// cached values
	private boolean restrictionsAware = true;
	private float leftTurn;
	private float roundaboutTurn;
	private float rightTurn;
	private boolean onewayAware = true;
	private boolean followSpeedLimitations = true;
	private float minDefaultSpeed = 10;
	private float maxDefaultSpeed = 10;

	public enum GeneralRouterProfile {
		CAR,
		PEDESTRIAN,
		BICYCLE
	}
	
	public GeneralRouter(GeneralRouterProfile profile, Map<String, String> attributes) {
		this.attributes = new LinkedHashMap<String, String>(attributes);
		this.profile = profile;
		highwaySpeed = new LinkedHashMap<String, Float>();
		highwayPriorities = new LinkedHashMap<String, Float>();
		avoid = new LinkedHashMap<String, Float>();
		obstacles = new LinkedHashMap<String, Float>();
		routingObstacles = new LinkedHashMap<String, Float>();
		Iterator<Entry<String, String>> e = attributes.entrySet().iterator();
		while(e.hasNext()){
			Entry<String, String> next = e.next();
			addAttribute(next.getKey(), next.getValue());
		}
	}

	public GeneralRouter(GeneralRouter pr, Map<String, String> attributes) {
		this.highwaySpeed = new LinkedHashMap<String, Float>(pr.highwaySpeed);
		this.highwayPriorities = new LinkedHashMap<String, Float>(pr.highwayPriorities);
		this.avoid = new LinkedHashMap<String, Float>(pr.avoid);
		this.obstacles = new LinkedHashMap<String, Float>(pr.obstacles);
		this.routingObstacles = new LinkedHashMap<String, Float>(pr.routingObstacles);
		this.attributes = new LinkedHashMap<String, String>();
		this.profile = pr.profile;
		Iterator<Entry<String, String>> e = attributes.entrySet().iterator();
		while(e.hasNext()){
			Entry<String, String> next = e.next();
			addAttribute(next.getKey(), next.getValue());
		}
		
	}
	
	public void addAttribute(String k, String v) {
		attributes.put(k, v);
		if(k.equals("restrictionsAware")) {
			restrictionsAware = parseSilentBoolean(v, restrictionsAware);
		} else if(k.equals("onewayAware")) {
			onewayAware = parseSilentBoolean(v, onewayAware);
		} else if(k.equals("followSpeedLimitations")) {
			followSpeedLimitations = parseSilentBoolean(v, followSpeedLimitations);
		} else if(k.equals("leftTurn")) {
			leftTurn = parseSilentFloat(v, leftTurn);
		} else if(k.equals("rightTurn")) {
			rightTurn = parseSilentFloat(v, rightTurn);
		} else if(k.equals("roundaboutTurn")) {
			roundaboutTurn = parseSilentFloat(v, roundaboutTurn);
		} else if(k.equals("minDefaultSpeed")) {
			minDefaultSpeed = parseSilentFloat(v, minDefaultSpeed * 3.6f) / 3.6f;
		} else if(k.equals("maxDefaultSpeed")) {
			maxDefaultSpeed = parseSilentFloat(v, maxDefaultSpeed * 3.6f) / 3.6f;
		}
	}

	@Override
	public boolean acceptLine(RouteDataObject way) {
		int[] types = way.getTypes();
		RouteRegion reg = way.region;
		return acceptLine(types, reg);
	}
	
	public boolean acceptLine(int[] types, RouteRegion reg) {
		if(!highwaySpeed.containsKey(RouteDataObject.getHighway(types, reg))) {
			boolean accepted = false;
			for (int i = 0; i < types.length; i++) {
				RouteTypeRule r = reg.quickGetEncodingRule(types[i]);
				Float sp = highwaySpeed.get(r.getTag()+"$"+r.getValue());
				if(sp != null){
					if(sp.floatValue() > 0) {
						accepted = true;
					}
					break;
				}
			}
			if(!accepted) {
				return false;
			}
		}
		
		
		for(int i=0; i<types.length; i++) {
			RouteTypeRule r = reg.quickGetEncodingRule(types[i]);
			String k = r.getTag() + "$" + r.getValue();
			if(avoid.containsKey(k)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean restrictionsAware() {
		return restrictionsAware;
	}
	
	@Override
	public float defineObstacle(RouteDataObject road, int point) {
		int[] pointTypes = road.getPointTypes(point);
		if(pointTypes == null) {
			return 0;
		}
		RouteRegion reg = road.region;
		int sz = pointTypes.length;
		for(int i=0; i<sz; i++) {
			RouteTypeRule r = reg.quickGetEncodingRule(pointTypes[i]);
			Float v = obstacles.get(r.getTag() + "$" + r.getValue());
			if(v != null ){
				return v;
			}
			v = obstacles.get(r.getTag() + "$");
			if(v != null ){
				return v;
			}
		}
		return 0;
	}
	
	@Override
	public float defineRoutingObstacle(RouteDataObject road, int point) {
		int[] pointTypes = road.getPointTypes(point);
		if(pointTypes == null) {
			return 0;
		}
		RouteRegion reg = road.region;
		int sz = pointTypes.length;
		for(int i=0; i<sz; i++) {
			RouteTypeRule r = reg.quickGetEncodingRule(pointTypes[i]);
			Float v = routingObstacles.get(r.getTag() + "$" + r.getValue());
			if(v != null ){
				return v;
			}
			v = routingObstacles.get(r.getTag() + "$");
			if(v != null ){
				return v;
			}
		}
		return 0;
	}
	
	public boolean isOnewayAware() {
		return onewayAware;
	}
	
	@Override
	public int isOneWay(RouteDataObject road) {
		if (!isOnewayAware()) {
			return 0;
		}
		return road.getOneway();
	}

	
	
	public boolean isFollowSpeedLimitations(){
		return followSpeedLimitations;
	}
	
	private static boolean parseSilentBoolean(String t, boolean v) {
		if (t == null || t.length() == 0) {
			return v;
		}
		return Boolean.parseBoolean(t);
	}

	private static float parseSilentFloat(String t, float v) {
		if (t == null || t.length() == 0) {
			return v;
		}
		return Float.parseFloat(t);
	}

	@Override
	public float defineSpeed(RouteDataObject road) {
		if (isFollowSpeedLimitations()) {
			float m = road.getMaximumSpeed();
			if(m > 0) {
				return m;
			}
		}
		
		Float value = null;
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
		return value / 3.6f;
	}

	@Override
	public float defineSpeedPriority(RouteDataObject road) {
		float priority = 1;
		for (int i = 0; i < road.types.length; i++) {
			RouteTypeRule r = road.region.quickGetEncodingRule(road.types[i]);
			if(highwayPriorities.containsKey(r.getTag()+"$"+r.getValue())){
				priority *= highwayPriorities.get(r.getTag()+"$"+r.getValue());
			}
		}
		return priority;
	}

	@Override
	public float getMinDefaultSpeed() {
		return minDefaultSpeed;
	}

	@Override
	public float getMaxDefaultSpeed() {
		return maxDefaultSpeed ;
	}

	
	public double getLeftTurn() {
		return leftTurn;
	}
	
	public double getRightTurn() {
		return rightTurn;
	}
	public double getRoundaboutTurn() {
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
			double a1 = segment.getRoad().directionRoute(segment.getSegmentStart(), segment.getSegmentStart() < segmentEnd);
			double a2 = prev.getRoad().directionRoute(prevSegmentEnd, prevSegmentEnd < prev.getSegmentStart());
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
	
	private <T> void specialize(String specializationTag, Map<String, T> m){
		ArrayList<String> ks = new ArrayList<String>(m.keySet());
		for(String s : ks){
			if(s.startsWith(specializationTag +":")) {
				String tagName = s.substring((specializationTag +":").length());
				m.put(tagName, m.get(s));
			}
		}
	}

	@Override
	public GeneralRouter specifyParameter(String specializationTag) {
		Map<String, String> attrs = new LinkedHashMap<String, String>(attributes);
		for(String s : attributes.keySet()){
			if(s.startsWith(specializationTag +":")) {
				String tagName = s.substring((specializationTag +":").length());
				attrs.put(tagName, attributes.get(s));
			}
		}
		GeneralRouter gr = new GeneralRouter(this, attrs);
		gr.specialize(specializationTag, gr.highwayPriorities);
		gr.specialize(specializationTag, gr.highwaySpeed);
		gr.specialize(specializationTag, gr.avoid);
		gr.specialize(specializationTag, gr.obstacles);
		gr.specialize(specializationTag, gr.routingObstacles);
		gr.specialize(specializationTag, gr.attributes);
		
		return gr;
	}
	
	@Override
	public boolean containsAttribute(String attribute) {
		return attributes.containsKey(attribute);
	}
	
	@Override
	public String getAttribute(String attribute) {
		return attributes.get(attribute);
	}
	

}

