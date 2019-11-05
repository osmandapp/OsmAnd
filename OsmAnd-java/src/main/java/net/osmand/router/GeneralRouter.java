package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import gnu.trove.set.hash.TLongHashSet;

public class GeneralRouter implements VehicleRouter {
	
	private static final float CAR_SHORTEST_DEFAULT_SPEED = 55/3.6f;
	public static final String USE_SHORTEST_WAY = "short_way";
	public static final String USE_HEIGHT_OBSTACLES = "height_obstacles";
	public static final String AVOID_FERRIES = "avoid_ferries";
	public static final String AVOID_TOLL = "avoid_toll";
	public static final String AVOID_MOTORWAY = "avoid_motorway";
	public static final String AVOID_UNPAVED = "avoid_unpaved";
	public static final String PREFER_MOTORWAYS = "prefer_motorway";
	public static final String ALLOW_PRIVATE = "allow_private";
	public static final String ALLOW_MOTORWAYS = "allow_motorway";
	public static final String DEFAULT_SPEED = "default_speed";
	public static final String MIN_SPEED = "min_speed";
	public static final String MAX_SPEED = "max_speed";
	public static final String VEHICLE_HEIGHT = "height";
	public static final String VEHICLE_WEIGHT = "weight";
	public static final String VEHICLE_WIDTH = "width";

	private final RouteAttributeContext[] objectAttributes;
	public final Map<String, String> attributes;
	private final Map<String, RoutingParameter> parameters; 
	private final Map<String, Integer> universalRules;
	private final List<String> universalRulesById;
	private final Map<String, BitSet> tagRuleMask;
	private final ArrayList<Object> ruleToValue;
	private boolean shortestRoute;
	private boolean heightObstacles;
	private boolean allowPrivate;
	private String filename = null;
	private String profileName = "";

	private Map<RouteRegion, Map<Integer, Integer>> regionConvert = new LinkedHashMap<RouteRegion, Map<Integer,Integer>>();
	
	// cached values
	private boolean restrictionsAware = true;
	private float leftTurn;
	private float roundaboutTurn;
	private float rightTurn;
	// speed in m/s
	private float minSpeed = 0.28f;
	// speed in m/s
	private float defaultSpeed = 1f;
	// speed in m/s
	private float maxSpeed = 10f;

	private TLongHashSet impassableRoads;
	private GeneralRouterProfile profile;
	
	
	public enum RouteDataObjectAttribute {
		ROAD_SPEED("speed"),
		ROAD_PRIORITIES("priority"),
		ACCESS("access"),
		OBSTACLES("obstacle_time"),
		ROUTING_OBSTACLES("obstacle"),
		ONEWAY("oneway"),
		PENALTY_TRANSITION("penalty_transition"),
		OBSTACLE_SRTM_ALT_SPEED("obstacle_srtm_alt_speed"),
		AREA("area");
		public final String nm; 
		RouteDataObjectAttribute(String name) {
			nm = name;
		}
		
		public static RouteDataObjectAttribute getValueOf(String s){
			for(RouteDataObjectAttribute a : RouteDataObjectAttribute.values()){
				if(a.nm.equals(s)){
					return a;
				}
			}
			return null;
		}
	}
	
	public enum GeneralRouterProfile {
		CAR,
		PEDESTRIAN,
		BICYCLE,
		BOAT,
		PUBLIC_TRANSPORT
	}

	
	public enum RoutingParameterType {
		NUMERIC,
		BOOLEAN,
		SYMBOLIC
	}
	
	public GeneralRouter(GeneralRouterProfile profile, Map<String, String> attributes) {
		this.profile = profile;
		this.attributes = new LinkedHashMap<String, String>();
		Iterator<Entry<String, String>> e = attributes.entrySet().iterator();
		while(e.hasNext()){
			Entry<String, String> next = e.next();
			addAttribute(next.getKey(), next.getValue());
		}
		objectAttributes = new RouteAttributeContext[RouteDataObjectAttribute.values().length];
		for (int i = 0; i < objectAttributes.length; i++) {
			objectAttributes[i] = new RouteAttributeContext();
		}
		universalRules = new LinkedHashMap<String, Integer>();
		universalRulesById = new ArrayList<String>();
		tagRuleMask = new LinkedHashMap<String, BitSet>();
		ruleToValue = new ArrayList<Object>();
		parameters = new LinkedHashMap<String, GeneralRouter.RoutingParameter>();
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public GeneralRouter(GeneralRouter parent, Map<String, String> params) {
		this.profile = parent.profile;
		this.attributes = new LinkedHashMap<String, String>();
		Iterator<Entry<String, String>> e = parent.attributes.entrySet().iterator();
		while (e.hasNext()) {
			Entry<String, String> next = e.next();
			addAttribute(next.getKey(), next.getValue());
		}
		// do not copy, keep linked
		universalRules = parent.universalRules;
		universalRulesById = parent.universalRulesById;
		tagRuleMask = parent.tagRuleMask;
		ruleToValue = parent.ruleToValue;
		parameters = parent.parameters;
		
		objectAttributes = new RouteAttributeContext[RouteDataObjectAttribute.values().length];
		for (int i = 0; i < objectAttributes.length; i++) {
			objectAttributes[i] = new RouteAttributeContext(parent.objectAttributes[i], params);
		}
		allowPrivate = params.containsKey(ALLOW_PRIVATE) && parseSilentBoolean(params.get(ALLOW_PRIVATE), false) ;
		shortestRoute = params.containsKey(USE_SHORTEST_WAY) && parseSilentBoolean(params.get(USE_SHORTEST_WAY), false);
		heightObstacles = params.containsKey(USE_HEIGHT_OBSTACLES) && parseSilentBoolean(params.get(USE_HEIGHT_OBSTACLES), false); 
		if(shortestRoute) {
			maxSpeed = Math.min(CAR_SHORTEST_DEFAULT_SPEED, maxSpeed);
		}
		if (params.containsKey(DEFAULT_SPEED)) {
			defaultSpeed = parseSilentFloat(params.get(DEFAULT_SPEED), defaultSpeed);
		}
		if (params.containsKey(MIN_SPEED)) {
			minSpeed = parseSilentFloat(params.get(MIN_SPEED), minSpeed);
		}
		if (params.containsKey(MAX_SPEED)) {
			maxSpeed = parseSilentFloat(params.get(MAX_SPEED), maxSpeed);
		}
	}
	
	public GeneralRouterProfile getProfile() {
		return profile;
	}

	public boolean getHeightObstacles() {
		return heightObstacles;
	}

	public Map<String, RoutingParameter> getParameters() {
		return parameters;
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
		} else if(k.equals("minDefaultSpeed") || k.equals("defaultSpeed")) {
			defaultSpeed = parseSilentFloat(v, defaultSpeed * 3.6f) / 3.6f;
		} else if( k.equals("minSpeed")) {
			minSpeed = parseSilentFloat(v, minSpeed * 3.6f) / 3.6f;
		} else if(k.equals("maxDefaultSpeed") || k.equals("maxSpeed")) {
			maxSpeed = parseSilentFloat(v, maxSpeed * 3.6f) / 3.6f;
		}
	}
	
	public RouteAttributeContext getObjContext(RouteDataObjectAttribute a) {
		return objectAttributes[a.ordinal()];
	}
	

	public void registerBooleanParameter(String id, String group, String name, String description, boolean defaultValue) {
		RoutingParameter rp = new RoutingParameter();
		rp.group = group;
		rp.name = name;
		rp.description = description;
		rp.id = id;
		rp.type = RoutingParameterType.BOOLEAN;
		rp.defaultBoolean = defaultValue;
		parameters.put(rp.id, rp);
		
	}

	public void registerNumericParameter(String id, String name, String description, Double[] vls, String[] vlsDescriptions) {
		RoutingParameter rp = new RoutingParameter();
		rp.name = name;
		rp.description = description;
		rp.id = id;
		rp.possibleValues = vls;
		rp.possibleValueDescriptions = vlsDescriptions;
		rp.type = RoutingParameterType.NUMERIC;
		parameters.put(rp.id, rp);		
	}

	@Override
	public boolean acceptLine(RouteDataObject way) {
		int res = getObjContext(RouteDataObjectAttribute.ACCESS).evaluateInt(way, 0);
		if(impassableRoads != null && impassableRoads.contains(way.id)) {
			return false;
		}
		return res >= 0;
	}

	public boolean isAllowPrivate() {
		return allowPrivate;
	}

	public long[] getImpassableRoadIds() {
		if(impassableRoads == null) {
			return new long[0];
		}
		return impassableRoads.toArray();
	}
	
	public int registerTagValueAttribute(String tag, String value) {
		String key = tag +"$"+value;
		if(universalRules.containsKey(key)) {
			return universalRules.get(key);
		}
		int id = universalRules.size();
		universalRulesById.add(key);
		universalRules.put(key, id);
		if(!tagRuleMask.containsKey(tag)) {
			tagRuleMask.put(tag, new BitSet());
		}
		tagRuleMask.get(tag).set(id);
		return id;
	}
	
	
	private Object parseValue(String value, String type) {
		float vl = -1;
		value = value.trim();
		if("speed".equals(type)) {
			vl = RouteDataObject.parseSpeed(value, vl);
		} else if("weight".equals(type)) {
			vl = RouteDataObject.parseWeightInTon(value, vl);
		} else if("length".equals(type)) {
			vl = RouteDataObject.parseLength(value, vl);
		} else {
			int i = Algorithms.findFirstNumberEndIndex(value);
			if (i > 0) {
				// could be negative
				return Float.parseFloat(value.substring(0, i));
			}
		}
		if(vl == -1) {
			return null;
		}
		return vl;
	}
	
	private Object parseValueFromTag(int id, String type) {
		while (ruleToValue.size() <= id) {
			ruleToValue.add(null);
		}
		Object res = ruleToValue.get(id);
		if (res == null) {
			String v = universalRulesById.get(id);
			String value = v.substring(v.indexOf('$') + 1);
			res = parseValue(value, type);
			if (res == null) {
				res = "";
			}
			ruleToValue.set(id, res);
		}
		if ("".equals(res)) {
			return null;
		}
		return res;
	}
	
	@Override
	public GeneralRouter build(Map<String, String> params) {
		return new GeneralRouter(this, params);
	}

	@Override
	public boolean restrictionsAware() {
		return restrictionsAware;
	}
	
	@Override
	public float defineObstacle(RouteDataObject road, int point) {
		int[] pointTypes = road.getPointTypes(point);
		if(pointTypes != null) {
			return getObjContext(RouteDataObjectAttribute.OBSTACLES).evaluateFloat(road.region, pointTypes, 0);
		}
		return 0;
	}
	
	@Override
	public float defineRoutingObstacle(RouteDataObject road, int point) {
		int[] pointTypes = road.getPointTypes(point);
		if(pointTypes != null){
			return getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES).evaluateFloat(road.region, pointTypes, 0);
		}
		return 0;
	}
	
	@Override
	public double defineHeightObstacle(RouteDataObject road, short startIndex, short endIndex) {
		if(!heightObstacles) {
			return 0;
		}
		float[] heightArray = road.calculateHeightArray();
		if(heightArray == null || heightArray.length == 0 ) {
			return 0;
		}
		
		double sum = 0;
		int knext;
		RouteAttributeContext objContext = getObjContext(RouteDataObjectAttribute.OBSTACLE_SRTM_ALT_SPEED);
		for(int k = startIndex; k != endIndex; k = knext) {
			knext = startIndex < endIndex ? k + 1 : k - 1;
			double dist = startIndex < endIndex ? heightArray[2 * knext] : heightArray[2 * k]  ;
			double diff = heightArray[2 * knext + 1] - heightArray[2 * k + 1] ;
			if(diff != 0 && dist > 0) {
				double incl = Math.abs(diff / dist);
				int percentIncl = (int) (incl * 100);
				percentIncl = (percentIncl + 2)/ 3 * 3 - 2; // 1, 4, 7, 10, .   
				if(percentIncl >= 1) {
					objContext.paramContext.incline = diff > 0 ? percentIncl : -percentIncl;
					sum += objContext.evaluateFloat(road, 0) * (diff > 0? diff : -diff );
				}
			}
		}
		return sum;
	}
	
	
	@Override
	public int isOneWay(RouteDataObject road) {
		return getObjContext(RouteDataObjectAttribute.ONEWAY).evaluateInt(road, 0);
	}
	
	@Override
	public boolean isArea(RouteDataObject road) {
		return getObjContext(RouteDataObjectAttribute.AREA).evaluateInt(road, 0) == 1;
	}
	
	@Override
	public float getPenaltyTransition(RouteDataObject road) {
		return getObjContext(RouteDataObjectAttribute.PENALTY_TRANSITION).evaluateInt(road, 0);
	}

	@Override
	public float defineRoutingSpeed(RouteDataObject road) {
		float spd = getObjContext(RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, defaultSpeed);
		return Math.max(Math.min(spd, maxSpeed), minSpeed);
	}
	
	
	@Override
	public float defineVehicleSpeed(RouteDataObject road) {
		float spd = getObjContext(RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, defaultSpeed);
		return Math.max(Math.min(spd, maxSpeed), minSpeed);
	}

	@Override
	public float defineSpeedPriority(RouteDataObject road) {
		return getObjContext(RouteDataObjectAttribute.ROAD_PRIORITIES).evaluateFloat(road, 1f);
	}

	@Override
	public float getDefaultSpeed() {
		return defaultSpeed;
	}

	@Override
	public float getMinSpeed() {
		return minSpeed;
	}

	@Override
	public float getMaxSpeed() {
		return maxSpeed;
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
		float ts = getPenaltyTransition(segment.getRoad());
		float prevTs = getPenaltyTransition(prev.getRoad());
		if(prevTs != ts) {
			return Math.abs(ts - prevTs) / 2;
		}
//		int[] pt = prev.getRoad().getPointTypes(prevSegmentEnd);
//		if(pt != null) {
//			RouteRegion reg = prev.getRoad().region;
//			for (int i = 0; i < pt.length; i++) {
//				RouteTypeRule r = reg.quickGetEncodingRule(pt[i]);
//				if ("highway".equals(r.getTag()) && "traffic_signals".equals(r.getValue())) {
//					// traffic signals don't add turn info
//					return 0;
//				}
//			}
//		}
		
		
		if(segment.getRoad().roundabout() && !prev.getRoad().roundabout()) {
			double rt = getRoundaboutTurn();
			if(rt > 0) {
				return rt;
			}
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
	public boolean containsAttribute(String attribute) {
		return attributes.containsKey(attribute);
	}
	
	@Override
	public String getAttribute(String attribute) {
		return attributes.get(attribute);
	}
	
	public float getFloatAttribute(String attribute, float v) {
		return parseSilentFloat(getAttribute(attribute), v);
	}
	
	public int getIntAttribute(String attribute, int v) {
		return (int) parseSilentFloat(getAttribute(attribute), v);
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
	
	
	
	public static class RoutingParameter {
		private String id;
		private String group;
		private String name;
		private String description;
		private RoutingParameterType type;
		private Object[] possibleValues;
		private String[] possibleValueDescriptions;
		private boolean defaultBoolean;
		
		public String getId() {
			return id;
		}

		public String getGroup() {
			return group;
		}
		public String getName() {
			return name;
		}
		public String getDescription() {
			return description;
		}
		public RoutingParameterType getType() {
			return type;
		}
		public String[] getPossibleValueDescriptions() {
			return possibleValueDescriptions;
		}
		public Object[] getPossibleValues() {
			return possibleValues;
		}
		public boolean getDefaultBoolean() {
			return defaultBoolean;
		}
	}
	
	
	private class ParameterContext {
		private Map<String, String> vars;
		private double incline = 0;
	}
	
	public class RouteAttributeContext {
		List<RouteAttributeEvalRule> rules = new ArrayList<RouteAttributeEvalRule>();
		ParameterContext paramContext = null;
		
		public RouteAttributeContext(){
		}
		public RouteAttributeContext(RouteAttributeContext original, Map<String, String> params){
			if (params != null) {
				paramContext = new ParameterContext();
				paramContext.vars = params;
			}
			for(RouteAttributeEvalRule rt : original.rules){
				if(checkParameter(rt)){
					rules.add(rt);
				}
			}
		}
		
		public RouteAttributeEvalRule[] getRules() {
			return rules.toArray(new RouteAttributeEvalRule[rules.size()]);
		}
		
		public String[] getParamKeys() {
			if(paramContext == null) {
				return new String[0];
			}
			return paramContext.vars.keySet().toArray(new String[paramContext.vars.size()]);
		}
		
		public String[] getParamValues() {
			if(paramContext == null) {
				return new String[0];
			}
			return paramContext.vars.values().toArray(new String[paramContext.vars.size()]);
		}
		
		private Object evaluate(RouteDataObject ro) {
			return evaluate(convert(ro.region, ro.types));
		}

		public void printRules(PrintStream out) {
			for(RouteAttributeEvalRule r : rules) {
				r.printRule(out);
			}
		}

		public RouteAttributeEvalRule registerNewRule(String selectValue, String selectType) {
			RouteAttributeEvalRule ev = new RouteAttributeEvalRule();
			ev.registerSelectValue(selectValue, selectType);
			rules.add(ev);	
			return ev;
		}
		
		public RouteAttributeEvalRule getLastRule() {
			return rules.get(rules.size() - 1);
		}

		private Object evaluate(BitSet types) {
			for (int k = 0; k < rules.size(); k++) {
				RouteAttributeEvalRule r = rules.get(k);
				Object o = r.eval(types, paramContext);
				if (o != null) {
					return o;
				}
			}
			return null;
		}

		private boolean checkParameter(RouteAttributeEvalRule r) {
			if (paramContext != null && r.parameters.size() > 0) {
				for (String p : r.parameters) {
					boolean not = false;
					if (p.startsWith("-")) {
						not = true;
						p = p.substring(1);
					}
					boolean val = paramContext.vars.containsKey(p);
					if (not && val) {
						return false;
					} else if (!not && !val) {
						return false;
					}
				}
			}
			return true;
		}
		
		public int evaluateInt(RouteDataObject ro, int defValue) {
			Object o = evaluate(ro);
			if(!(o instanceof Number)) {
				return defValue;
			}
			return ((Number)o).intValue();
		}
		
		public int evaluateInt(RouteRegion region, int[] types, int defValue) {
			Object o = evaluate(convert(region, types));
			if(!(o instanceof Number)){
				return defValue;
			}
			return ((Number)o).intValue();
		}
		
		public int evaluateInt(BitSet rawTypes, int defValue) {
			Object o = evaluate(rawTypes);
			if(!(o instanceof Number)){
				return defValue;
			}
			return ((Number)o).intValue();
		}
		
		public float evaluateFloat(RouteDataObject ro, float defValue) {
			Object o = evaluate(ro);
			if(!(o instanceof Number)) {
				return defValue;
			}
			return ((Number)o).floatValue();
		}
		
		public float evaluateFloat(RouteRegion region, int[] types, float defValue) {
			Object o = evaluate(convert(region, types));
			if(!(o instanceof Number)) {
				return defValue;
			}
			return ((Number)o).floatValue();
		}
		
		public float evaluateFloat(BitSet rawTypes, float defValue) {
			Object o = evaluate(rawTypes);
			if(!(o instanceof Number)){
				return defValue;
			}
			return ((Number)o).floatValue();
		}
		
		private BitSet convert(RouteRegion reg, int[] types) {
			BitSet b = new BitSet(universalRules.size());
			Map<Integer, Integer> map = regionConvert.get(reg);
			if(map == null){
				map = new HashMap<Integer, Integer>();
				regionConvert.put(reg, map);
			}
			for(int k = 0; k < types.length; k++) {
				Integer nid = map.get(types[k]);
				if(nid == null){
					RouteTypeRule r = reg.quickGetEncodingRule(types[k]);
					nid = registerTagValueAttribute(r.getTag(), r.getValue());
					map.put(types[k], nid);
				}
				b.set(nid);
			}
			return b;
		}
	}

	public class RouteAttributeExpression {
		public static final int LESS_EXPRESSION = 1;
		public static final int GREAT_EXPRESSION = 2;
		public static final int EQUAL_EXPRESSION = 3;
		
		public RouteAttributeExpression(String[] vs, String valueType, int expressionId) {
			this.expressionType = expressionId;
			this.values = vs;
			if (vs.length < 2) {
				throw new IllegalStateException("Expression should have at least 2 arguments");
			}
			this.cacheValues = new Number[vs.length];
			this.valueType = valueType;
			for (int i = 0; i < vs.length; i++) {
				if(!vs[i].startsWith("$") && !vs[i].startsWith(":")) {
					Object o = parseValue(vs[i], valueType);
					if (o instanceof Number) {
						cacheValues[i] = (Number) o;
					}
				}
			}
		}
		// definition
		private String[] values;
		private int expressionType;
		private String valueType;
		// numbers		
		private Number[] cacheValues;
		
		public boolean matches(BitSet types, ParameterContext paramContext) {
			double f1 = calculateExprValue(0, types, paramContext);
			double f2 = calculateExprValue(1, types, paramContext);
			if (Double.isNaN(f1) || Double.isNaN(f2)) {
				return false;
			}
			if (expressionType == LESS_EXPRESSION) {
				return f1 <= f2;
			} else if (expressionType == GREAT_EXPRESSION) {
				return f1 >= f2;
			} else if (expressionType == EQUAL_EXPRESSION) {
				return f1 == f2;
			}
			return false;
		}

		private double calculateExprValue(int id, BitSet types, ParameterContext paramContext) {
			String value = values[id];
			Number cacheValue = cacheValues[id];
			if(cacheValue != null) {
				return cacheValue.doubleValue();
			}
			Object o = null;
			if (value instanceof String && value.toString().startsWith("$")) {
				BitSet mask = tagRuleMask.get(value.toString().substring(1));
				if (mask != null && mask.intersects(types)) {
					BitSet findBit = new BitSet(mask.length());
					findBit.or(mask);
					findBit.and(types);
					int v = findBit.nextSetBit(0);
					o = parseValueFromTag(v, valueType);
				}
			} else if (value instanceof String && value.equals(":incline")) {
				return paramContext.incline;
			} else if (value instanceof String && value.toString().startsWith(":")) {
				String p = ((String) value).substring(1);
				if (paramContext != null && paramContext.vars.containsKey(p)) {
					o = parseValue(paramContext.vars.get(p), valueType);
				}
			}
			
			if(o instanceof Number) {
				return ((Number)o).doubleValue();
			}
			return Double.NaN;
		}
		
	}
	

	public class RouteAttributeEvalRule {
		protected List<String> parameters = new ArrayList<String>() ;
		protected List<String> tagValueCondDefTag = new ArrayList<String>();
		protected List<String> tagValueCondDefValue = new ArrayList<String>();
		protected List<Boolean> tagValueCondDefNot = new ArrayList<Boolean>();
		
		protected String selectValueDef = null;
		protected Object selectValue = null;
		protected String selectType = null;
		protected BitSet filterTypes = new BitSet();
		protected BitSet filterNotTypes = new BitSet();
		protected BitSet evalFilterTypes = new BitSet();
		
		protected Set<String> onlyTags = new LinkedHashSet<String>();
		protected Set<String> onlyNotTags = new LinkedHashSet<String>();
		protected List<RouteAttributeExpression> expressions = new ArrayList<RouteAttributeExpression>();
		
		
		public RouteAttributeExpression[] getExpressions() {
			return expressions.toArray(new RouteAttributeExpression[expressions.size()]);
		}
		
		public String[] getParameters() {
			return parameters.toArray(new String[parameters.size()]);
		}
		
		public String[] getTagValueCondDefTag() {
			return tagValueCondDefTag.toArray(new String[tagValueCondDefTag.size()]);
		}
		
		public String[] getTagValueCondDefValue() {
			return tagValueCondDefValue.toArray(new String[tagValueCondDefValue.size()]);
		}
		
		public boolean[] getTagValueCondDefNot() {
			boolean[] r = new boolean[tagValueCondDefNot.size()];
			for (int i = 0; i < r.length; i++) {
				r[i] = tagValueCondDefNot.get(i).booleanValue();
			}
			return r;
		}
		
		public void registerSelectValue(String value, String type) {
			selectType = type;
			selectValueDef = value;
			if(value.startsWith(":") || value.startsWith("$")) {
				selectValue = value;
			} else {
				selectValue = parseValue(value, type);
				if(selectValue == null) {
					System.err.println("Routing.xml select value '" + value+"' was not registered");
				}
			}
		}
		
		public void printRule(PrintStream out) {
			out.print(" Select " + selectValue  + " if ");
			for(int k = 0; k < filterTypes.length(); k++) {
				if(filterTypes.get(k)) {
					String key = universalRulesById.get(k);
					out.print(key + " ");
				}
			}
			if(filterNotTypes.length() > 0) {
				out.print(" ifnot ");
			}
			for(int k = 0; k < filterNotTypes.length(); k++) {
				if(filterNotTypes.get(k)) {
					String key = universalRulesById.get(k);
					out.print(key + " ");
				}
			}
			for(int k = 0; k < parameters.size(); k++) {
				out.print(" param="+parameters.get(k));
			}
			if(onlyTags.size() > 0) {
				out.print(" match tag = " + onlyTags);
			}
			if(onlyNotTags.size() > 0) {
				out.print(" not match tag = " + onlyNotTags);
			}
			if(expressions.size() > 0) {
				out.println(" subexpressions " + expressions.size());
			}
			out.println();
		}

		public void registerAndTagValueCondition(String tag, String value, boolean not) {
			tagValueCondDefTag.add(tag);
			tagValueCondDefValue.add(value);
			tagValueCondDefNot.add(not);
			if(value == null) { 
				if (not) {
					onlyNotTags.add(tag);
				} else {
					onlyTags.add(tag);
				}
			} else {
				int vtype = registerTagValueAttribute(tag, value);
				if(not) {
					filterNotTypes.set(vtype);
				} else {
					filterTypes.set(vtype);
				}
			}
		}
		
		public void registerLessCondition(String value1, String value2, String valueType) {
			expressions.add(new RouteAttributeExpression(new String[] { value1, value2 }, valueType,
					RouteAttributeExpression.LESS_EXPRESSION));
		}
		
		public void registerGreatCondition(String value1, String value2, String valueType) {
			expressions.add(new RouteAttributeExpression(new String[] { value1, value2 }, valueType,
					RouteAttributeExpression.GREAT_EXPRESSION));
		}
		
		public void registerEqualCondition(String value1, String value2, String valueType) {
			expressions.add(new RouteAttributeExpression(new String[] { value1, value2 }, valueType,
					RouteAttributeExpression.EQUAL_EXPRESSION));
		}
		
		public void registerAndParamCondition(String param, boolean not) {
			param = not ? "-" + param : param;
			parameters.add(param);
		}

		public synchronized Object eval(BitSet types, ParameterContext paramContext) {
			if (matches(types, paramContext)) {
				return calcSelectValue(types, paramContext);
			}
			return null;
		}
		

		protected Object calcSelectValue(BitSet types, ParameterContext paramContext) {
			if (selectValue instanceof String && selectValue.toString().startsWith("$")) {
				BitSet mask = tagRuleMask.get(selectValue.toString().substring(1));
				if (mask != null && mask.intersects(types)) {
					BitSet findBit = new BitSet(mask.length());
					findBit.or(mask);
					findBit.and(types);
					int value = findBit.nextSetBit(0);
					return parseValueFromTag(value, selectType);
				}
			} else if (selectValue instanceof String && selectValue.toString().startsWith(":")) {
				String p = ((String) selectValue).substring(1);
				if (paramContext != null && paramContext.vars.containsKey(p)) {
					selectValue = parseValue(paramContext.vars.get(p), selectType);
				} else {
					return null;
				}
			}
			return selectValue;
		}

		public boolean matches(BitSet types, ParameterContext paramContext) {
			if(!checkAllTypesShouldBePresent(types)) {
				return false;
			}
			if(!checkAllTypesShouldNotBePresent(types)) {
				return false;
			}
			if(!checkFreeTags(types)) {
				return false;
			}
			if(!checkNotFreeTags(types)) {
				return false;
			}
			if(!checkExpressions(types, paramContext)) {
				return false;
			}
			return true;
		}

		private boolean checkExpressions(BitSet types, ParameterContext paramContext) {
			for(RouteAttributeExpression e : expressions){
				if(!e.matches(types, paramContext)) {
					return false;
				}
			}
			return true;
		}

		private boolean checkFreeTags(BitSet types) {
			for (String ts : onlyTags) {
				BitSet b = tagRuleMask.get(ts);
				if (b == null || !b.intersects(types)) {
					return false;
				}
			}
			return true;
		}
		
		private boolean checkNotFreeTags(BitSet types) {
			for (String ts : onlyNotTags) {
				BitSet b = tagRuleMask.get(ts);
				if (b != null && b.intersects(types)) {
					return false;
				}
			}
			return true;
		}

		private boolean checkAllTypesShouldNotBePresent(BitSet types) {
			if(filterNotTypes.intersects(types)) {
				return false;
			}
			return true;
		}

		private boolean checkAllTypesShouldBePresent(BitSet types) {
			// Bitset method subset is missing "filterTypes.isSubset(types)"    
			// reset previous evaluation
			// evalFilterTypes.clear(); // not needed same as or()
			evalFilterTypes.or(filterTypes);
			// evaluate bit intersection and check if filterTypes contained as set in types
			evalFilterTypes.and(types);
			if(!evalFilterTypes.equals(filterTypes)) {
				return false;
			}
			return true;
		}
		
	}


	public void printRules(PrintStream out) {
		for(int i = 0; i < RouteDataObjectAttribute.values().length ; i++) {
			out.println(RouteDataObjectAttribute.values()[i]);
			objectAttributes[i].printRules(out);
		}
		
	}

	public void addImpassableRoads(Set<Long> impassableRoads) {
		if (impassableRoads != null && !impassableRoads.isEmpty()) {
			if (this.impassableRoads == null) {
				this.impassableRoads = new TLongHashSet();
			}
			this.impassableRoads.addAll(impassableRoads);
		}		
	}
}

