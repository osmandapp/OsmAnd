package net.osmand.router;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.util.MapUtils;

public class NewGeneralRouter extends VehicleRouter {
	
	public RouteAttributeContext roadSpeed ;
	public RouteAttributeContext roadPriorities ;
	public RouteAttributeContext access ;
	public RouteAttributeContext obstacles;
	public RouteAttributeContext routingObstacles;
	public RouteAttributeContext oneway;
	public Map<String, String> attributes;
	
	private Map<String, RoutingParameter> parameters = new LinkedHashMap<String, RoutingParameter>(); 
	private Map<String, Integer> universalRules = new LinkedHashMap<String, Integer>();
	private Map<String, List<RouteEvaluationRule>> freeTagRules = new HashMap<String, List<RouteEvaluationRule>>();
	private Map<RouteRegion, Map<Integer, Integer>> regionConvert = new LinkedHashMap<RouteRegion, Map<Integer,Integer>>();
	
	
	private GeneralRouterProfile profile;

	// cached values
	private boolean restrictionsAware = true;
	private float leftTurn;
	private float roundaboutTurn;
	private float rightTurn;
	private float minDefaultSpeed = 10;
	private float maxDefaultSpeed = 10;
	
	public enum RoutingParameterType {
		NUMERIC,
		BOOLEAN,
		SYMBOLIC
	}
	public enum GeneralRouterProfile {
		CAR,
		PEDESTRIAN,
		BICYCLE
	}
	
	public NewGeneralRouter(GeneralRouterProfile profile, Map<String, String> attributes) {
		this.attributes = new LinkedHashMap<String, String>();
		this.profile = profile;
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
		int[] utypes = convert(way.region, way.getTypes());
		int res = access.evaluateInt(utypes, 0);
		return res >= 0;
	}
	
	private int[] convert(RouteRegion reg, int[] types) {
		int[] utypes = new int[types.length];
		Map<Integer, Integer> map = regionConvert.get(reg);
		if(map == null){
			map = new HashMap<Integer, Integer>();
			regionConvert.put(reg, map);
		}
		for(int k = 0; k < types.length; k++) {
			Integer nid = map.get(types[k]);
			if(nid == null){
				RouteTypeRule r = reg.quickGetEncodingRule(types[k]);
				nid = registerRule(r.getTag(), r.getValue());
				map.put(types[k], nid);
			}
			utypes[k] = nid;
		}
		Arrays.sort(utypes);
		return utypes;
	}

	public int registerRule(String tag, String value) {
		String key = tag +"$"+value;
		if(universalRules.containsKey(key)) {
			return universalRules.get(key);
		}
		universalRules.put(key, universalRules.size());
		return universalRules.size() - 1;
	}
	
	public void build(){
		freeTagRules.clear();
		
		roadSpeed.newEvaluationContext();
		roadPriorities.newEvaluationContext();
		access.newEvaluationContext();
		obstacles.newEvaluationContext();
		routingObstacles.newEvaluationContext();
		oneway.newEvaluationContext();
	}
	
	public void registerFreeTagRule(RouteEvaluationRule r, String tag) {
		if(!freeTagRules.containsKey(tag)) {
			freeTagRules.put(tag, new ArrayList<NewGeneralRouter.RouteEvaluationRule>());
		}
		freeTagRules.get(tag).add(r);
		Iterator<Entry<String, Integer>> it = universalRules.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer> e = it.next();
			String key = e.getKey();
			if (key.startsWith(tag + "$")) {
				r.insertType(e.getValue(), false);
			}
			if (tag.startsWith("-") && universalRules.containsKey(tag.substring(1) + "$")) {
				r.insertType(e.getValue(), true);
			}
		}
	}

	private void updateFreeTagRules() {
		
	}

	private void updateFreeTagRules(List<RouteEvaluationRule> flist, int value, boolean not) {
		
		
	}

	@Override
	public boolean restrictionsAware() {
		return restrictionsAware;
	}
	
	@Override
	public float defineObstacle(RouteDataObject road, int point) {
		int[] ts = road.getPointTypes(point);
		if(ts != null) {
			
		}
		// TODO
		return 0;
	}
	
	@Override
	public float defineRoutingObstacle(RouteDataObject road, int point) {
		// TODO
		return 0;
	}
	
	@Override
	public int isOneWay(RouteDataObject road) {
		// TODO
		return 0;
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
		// TODO
		return 0;
	}

	@Override
	public float defineSpeedPriority(RouteDataObject road) {
		return 0;
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
	

	@Override
	public NewGeneralRouter specifyParameter(String parameter) {
		// TODO
		return this;
	}
	
	@Override
	public boolean containsAttribute(String attribute) {
		return attributes.containsKey(attribute);
	}
	
	@Override
	public String getAttribute(String attribute) {
		return attributes.get(attribute);
	}
	
	
	public static class RoutingParameter {
		public String id;
		public String name;
		public String description;
		public RoutingParameterType type;
		public String[] possibleValues;
		public String[] possibleValueDescriptions;
		public Object value;
	}
	
	private class RouteEvaluationRule {
		
		private List<String> tag = new ArrayList<String>();
		private List<String> values = new ArrayList<String>();
		private List<String> parameters = new ArrayList<String>() ;
		
		//// evaluation variables
		private boolean parameterValue = true; 
		private TByteArrayList notType = new TByteArrayList();
		private TIntArrayList sortedTypeArrays = new TIntArrayList();
		
		public void newEvaluationContext(){
			evaluateParameterExpr();
			sortedTypeArrays.clear();
			notType.clear();
			for(int i = 0; i<tag.size(); i++) {
				if(values.get(i) == null) { 
					registerFreeTagRule(this, tag.get(i));
				} else {
					registerRule(tag.get(i), values.get(i));
				}
			}
			//FIXME check parameters!
		}

		public void insertType(int t, boolean b) {
			int i = sortedTypeArrays.binarySearch(t);
			if (i < 0) {
				sortedTypeArrays.insert(-(i + 1), t);
				notType.insert(-(i + 1), (byte) (b ? 1 : -1));
			}
		}

		private void evaluateParameterExpr() {
			parameterValue = true;
			Map<String, RoutingParameter> defParams = NewGeneralRouter.this.parameters;
			for(String p : parameters) {
				boolean not = false;
				if(p.startsWith("-")) {
					not = true;
					p = p.substring(1);
				}
				boolean val = false;
				if(defParams.containsKey(p)) {
					RoutingParameter t = defParams.get(p);
					val = t.type == RoutingParameterType.BOOLEAN && t.value != null && ((Boolean)t.value).booleanValue();
				}
				if(not && val){
					parameterValue = false;
					break;
				} else if(!not && !val){
					parameterValue = false;
					break;
				}
				
			}
		}
		
		public boolean isAlwaysFalse() {
			return !parameterValue; 
		}
		
		public boolean isAlwaysTrue() {
			return parameterValue && isConst(); 
		}
		
		public boolean isConst(){
			return tag == null;
		}

		public boolean matches(int[] types) {
			if(isAlwaysFalse()){
				return false;
			}
			// TODO <gt value1=":weight" value2="$maxweight"/>
			// TODO
			return true;
		}
		
	}
	
	private class RouteAttributeContext {
		List<RouteEvaluationRule> rules = new ArrayList<RouteEvaluationRule>();
		List<Object> values = new ArrayList<Object>(); 
		
		public void newEvaluationContext(){
			for (int k = 0; k < rules.size(); k++) {
				rules.get(k).newEvaluationContext();
			}
		}
		
		public Object evaluate(int[] types) {
			for (int k = 0; k < rules.size(); k++) {
				if(rules.get(k).matches(types)){
					return values.get(k);
				}
			}
			return null;
		}
		
		public int evaluateInt(int[] types, int defValue) {
			Object o = evaluate(types);
			if(o == null) {
				return defValue;
			}
			return ((Number)o).intValue();
		}
		
		public float evaluateFloat(int[] types, float defValue) {
			Object o = evaluate(types);
			if(o == null) {
				return defValue;
			}
			return ((Float)o).intValue();
		}
		
	}
	

}

