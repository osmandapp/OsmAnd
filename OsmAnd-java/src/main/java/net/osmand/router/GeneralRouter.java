package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TLongHashSet;

public class GeneralRouter implements VehicleRouter {
	
	private static final float CAR_SHORTEST_DEFAULT_SPEED = 55/3.6f;
	private static final float BICYCLE_SHORTEST_DEFAULT_SPEED = 15/3.6f;
	public static int IMPASSABLE_ROAD_SHIFT = 0; // 6 is better
	
	public static final String USE_SHORTEST_WAY = "short_way";
	public static final String USE_HEIGHT_OBSTACLES = "height_obstacles";
	public static final String GROUP_RELIEF_SMOOTHNESS_FACTOR = "relief_smoothness_factor";
	public static final String AVOID_FERRIES = "avoid_ferries";
	public static final String AVOID_TOLL = "avoid_toll";
	public static final String AVOID_MOTORWAY = "avoid_motorway";
	public static final String AVOID_UNPAVED = "avoid_unpaved";
	public static final String PREFER_MOTORWAYS = "prefer_motorway";
	public static final String ALLOW_PRIVATE = "allow_private";
	public static final String ALLOW_PRIVATE_FOR_TRUCK = "allow_private_for_truck";
	public static final String HAZMAT_CATEGORY = "hazmat_category";
	public static final String GOODS_RESTRICTIONS = "goods_restrictions";
	public static final String ALLOW_MOTORWAYS = "allow_motorway";
	public static final String DEFAULT_SPEED = "default_speed";
	public static final String MIN_SPEED = "min_speed";
	public static final String MAX_SPEED = "max_speed";
	public static final String VEHICLE_HEIGHT = "height";
	public static final String VEHICLE_WEIGHT = "weight";
	public static final String VEHICLE_WIDTH = "width";
	public static final String VEHICLE_LENGTH = "length";
	public static final String MOTOR_TYPE = "motor_type";
	public static final String MAX_AXLE_LOAD = "maxaxleload";
	public static final String WEIGHT_RATING = "weightrating";
	public static final String ALLOW_VIA_FERRATA = "allow_via_ferrata";
	public static final String CHECK_ALLOW_PRIVATE_NEEDED = "check_allow_private_needed";

	private static boolean USE_CACHE = true;
	public static long TIMER = 0;

	private final RouteAttributeContext[] objectAttributes;
	public final Map<String, String> attributes;
	private final Map<String, RoutingParameter> parameters;
	private final Map<String, String> parameterValues; 
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
	private float sharpTurn;
	private float roundaboutTurn;
	private float slightTurn;
	// speed in m/s
	private float minSpeed = 0.28f;
	// speed in m/s
	private float defaultSpeed = 1f;
	// speed in m/s
	private float maxSpeed = 10f;
	// speed in m/s (used for shortest route)
	private float maxVehicleSpeed;

	private TLongHashSet impassableRoads;
	
	private GeneralRouterProfile profile;
	
	Map<RouteRegion, Map<IntHolder, Float>>[] evalCache;

	public String[] hhNativeFilter = new String[0]; // getFilteredTags() as flat Array (JNI)
	public String[] hhNativeParameterValues = new String[0]; // parameterValues as flat Array (JNI)
	private final GeneralRouter root;
		
	
	public enum RouteDataObjectAttribute {
		ROAD_SPEED("speed"),
		ROAD_PRIORITIES("priority"),
		DESTINATION_PRIORITIES("destination_priority"),
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
		SKI,
		MOPED,
		TRAIN,
		PUBLIC_TRANSPORT,
		HORSEBACKRIDING;
		
		public String getBaseProfile() {
			return this.toString().toLowerCase();
		}
	}
	
	public enum RoutingParameterType {
		NUMERIC,
		BOOLEAN,
		SYMBOLIC
	}
	
	public GeneralRouter(GeneralRouter copy, Map<String, String> params) {
		this.root = copy.root;
		GeneralRouter parent = root;
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
		parameterValues = params;
		tagRuleMask = parent.tagRuleMask;
		ruleToValue = parent.ruleToValue;
		parameters = parent.parameters;
		profileName = parent.profileName;
		
		objectAttributes = new RouteAttributeContext[RouteDataObjectAttribute.values().length];
		for (int i = 0; i < objectAttributes.length; i++) {
			objectAttributes[i] = new RouteAttributeContext(parent.objectAttributes[i], params);
		}
		shortestRoute = params.containsKey(USE_SHORTEST_WAY) && parseSilentBoolean(params.get(USE_SHORTEST_WAY), false);
		heightObstacles = params.containsKey(USE_HEIGHT_OBSTACLES) && parseSilentBoolean(params.get(USE_HEIGHT_OBSTACLES), false);

		if (params.containsKey("profile_truck")) {
			allowPrivate = params.containsKey(ALLOW_PRIVATE_FOR_TRUCK) && parseSilentBoolean(params.get(ALLOW_PRIVATE_FOR_TRUCK), false);
		} else {
			allowPrivate = params.containsKey(ALLOW_PRIVATE) && parseSilentBoolean(params.get(ALLOW_PRIVATE), false);
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
		maxVehicleSpeed = maxSpeed;
		if (shortestRoute) {
			if (profile == GeneralRouterProfile.BICYCLE) {
				maxSpeed = Math.min(BICYCLE_SHORTEST_DEFAULT_SPEED, maxSpeed);
			} else {
				maxSpeed = Math.min(CAR_SHORTEST_DEFAULT_SPEED, maxSpeed);
			}
		}
		initCaches();
	}
	
	public GeneralRouter(GeneralRouterProfile profile, Map<String, String> attributes) {
		this.root = this;
		this.profile = profile;
		this.attributes = new LinkedHashMap<String, String>();
		this.parameterValues = new LinkedHashMap<String, String>();
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
		
		initCaches();

	}

	@SuppressWarnings("unchecked")
	private void initCaches() {
		int l = RouteDataObjectAttribute.values().length;
		evalCache = new Map[l];
		for (int i = 0; i < l; i++) {
			evalCache[i] = new HashMap<>();
		}
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

	
	public GeneralRouterProfile getProfile() {
		return profile;
	}

	public boolean getHeightObstacles() {
		return heightObstacles;
	}

	public Map<String, RoutingParameter> getParameters() {
		return parameters;
	}
	
	public Map<String, String> getParameterValues() {
		return parameterValues;
	}
	
	public List<String> serializeParameterValues(Map<String, String> vls) {
		List<String> ls = new ArrayList<String>();
		for (Entry<String, String> e : vls.entrySet()) {
			String val = e.getValue();
			if (val.isEmpty() || "true".equals(val) || "false".equals(val)) {
				ls.add(e.getKey());
			} else {
				ls.add(e.getKey() + "=" + val);
			}
		}
		return ls;
	}

	public void addAttribute(String k, String v) {
		attributes.put(k, v);
		if (k.equals("restrictionsAware")) {
			restrictionsAware = parseSilentBoolean(v, restrictionsAware);
		} else if (k.equals("sharpTurn") || k.equals("leftTurn")) {
			sharpTurn = parseSilentFloat(v, sharpTurn);
		} else if (k.equals("slightTurn") || k.equals("rightTurn")) {
			slightTurn = parseSilentFloat(v, slightTurn);
		} else if (k.equals("roundaboutTurn")) {
			roundaboutTurn = parseSilentFloat(v, roundaboutTurn);
		} else if (k.equals("minDefaultSpeed") || k.equals("defaultSpeed")) {
			defaultSpeed = parseSilentFloat(v, defaultSpeed * 3.6f) / 3.6f;
		} else if (k.equals("minSpeed")) {
			minSpeed = parseSilentFloat(v, minSpeed * 3.6f) / 3.6f;
		} else if (k.equals("maxDefaultSpeed") || k.equals("maxSpeed")) {
			maxSpeed = parseSilentFloat(v, maxSpeed * 3.6f) / 3.6f;
		}
	}
	
	public RouteAttributeContext getObjContext(RouteDataObjectAttribute a) {
		return objectAttributes[a.ordinal()];
	}
	

	public void registerBooleanParameter(String id, String group, String name, String description, String[] profiles, boolean defaultValue) {
		RoutingParameter rp = new RoutingParameter();
		rp.id = id;
		rp.group = group;
		rp.name = name;
		rp.description = description;
		rp.profiles = profiles;
		rp.type = RoutingParameterType.BOOLEAN;
		rp.defaultBoolean = defaultValue;
		parameters.put(rp.id, rp);
		
	}

	public void registerNumericParameter(String id, String name, String description, String[] profiles, Double[] vls, String[] vlsDescriptions) {
		RoutingParameter rp = new RoutingParameter();
		rp.name = name;
		rp.description = description;
		rp.id = id;
		rp.profiles = profiles;
		rp.possibleValues = vls;
		rp.possibleValueDescriptions = vlsDescriptions;
		rp.type = RoutingParameterType.NUMERIC;
		parameters.put(rp.id, rp);
	}

	@Override
	public boolean acceptLine(RouteDataObject way) {
		Float res = getCache(RouteDataObjectAttribute.ACCESS, way);
		if (res == null) {
			res = (float) getObjContext(RouteDataObjectAttribute.ACCESS).evaluateInt(way, 0);
			putCache(RouteDataObjectAttribute.ACCESS, way, res);
		}
		if (impassableRoads != null && impassableRoads.contains(way.id >> IMPASSABLE_ROAD_SHIFT)) {
			return false;
		}
		return res >= 0;
	}

	public boolean isAllowPrivate() {
		return allowPrivate;
	}

	public long[] getImpassableRoadIds() {
		if (impassableRoads == null) {
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
	public float defineObstacle(RouteDataObject road, int point, boolean dir) {
		int[] pointTypes = road.getPointTypes(point);
		if(pointTypes != null) {
			Float obst = getCache(RouteDataObjectAttribute.OBSTACLES, road.region, pointTypes, dir);
			if (obst == null) {
				int[] filteredPointTypes = filterDirectionTags(road, pointTypes, dir);
				obst = getObjContext(RouteDataObjectAttribute.OBSTACLES).evaluateFloat(road.region, filteredPointTypes, 0);
				putCache(RouteDataObjectAttribute.OBSTACLES, road.region, pointTypes, obst, dir);
			}
			return obst;
		}
		return 0;
	}
	
	@Override
	public float defineRoutingObstacle(RouteDataObject road, int point, boolean dir) {
		int[] pointTypes = road.getPointTypes(point);
		if(pointTypes != null) {
			Float obst = getCache(RouteDataObjectAttribute.ROUTING_OBSTACLES, road.region, pointTypes, dir);
			if (obst == null) {
				int[] filteredPointTypes = filterDirectionTags(road, pointTypes, dir);
				obst = getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES).evaluateFloat(road.region, filteredPointTypes, 0);
				putCache(RouteDataObjectAttribute.ROUTING_OBSTACLES, road.region, pointTypes, obst, dir);
			}
			return obst;
		}
		return 0;
	}

	private int[] filterDirectionTags(RouteDataObject road, int[] pointTypes, boolean forwardDir) {
		int wayDirection = forwardDir ? 1 : -1;
		int direction = 0;
		int tdirection = 0;
		int hdirection = 0;
		for (int type : pointTypes) {
			if (type == road.region.directionBackward) {
				direction = -1;
			} else if (type == road.region.directionForward) {
				direction = 1;
			} else if (type == road.region.directionTrafficSignalsBackward) {
				tdirection = -1;
			} else if (type == road.region.directionTrafficSignalsForward) {
				tdirection = 1;
			} else if (type == road.region.maxheightBackward) {
				hdirection = -1;
			} else if (type == road.region.maxheightForward) {
				hdirection = 1;
			}
		}
		if (direction != 0 || tdirection != 0 || hdirection != 0) {
			TIntArrayList filteredPointTypes = new TIntArrayList();
			for (int type : pointTypes) {
				if (((type == road.region.stopSign || type == road.region.giveWaySign) && direction == wayDirection)
						|| (type == road.region.trafficSignals && tdirection == wayDirection)
						|| (hdirection == wayDirection)) {
					continue;
				}
				filteredPointTypes.add(type);
			}
			return filteredPointTypes.toArray();
		}
		return pointTypes;
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
		Float res = getCache(RouteDataObjectAttribute.ONEWAY, road);
		if(res == null) {
			res = (float) getObjContext(RouteDataObjectAttribute.ONEWAY).evaluateInt(road, 0);
			putCache(RouteDataObjectAttribute.ONEWAY, road, res);
		}
		return res.intValue();
	}
	
	@Override
	public boolean isArea(RouteDataObject road) {
		return getObjContext(RouteDataObjectAttribute.AREA).evaluateInt(road, 0) == 1;
	}
	
	@Override
	public float getPenaltyTransition(RouteDataObject road) {
		Float vl = getCache(RouteDataObjectAttribute.PENALTY_TRANSITION, road);
		if (vl == null) {
			vl = (float) getObjContext(RouteDataObjectAttribute.PENALTY_TRANSITION).evaluateInt(road, 0);
			putCache(RouteDataObjectAttribute.PENALTY_TRANSITION, road, vl);
		}
		return vl;
	}

	@Override
	public float defineRoutingSpeed(RouteDataObject road, boolean dir) {
		Float definedSpd = getCache(RouteDataObjectAttribute.ROAD_SPEED, road, dir);
		if (definedSpd == null) {
			// not implemented direction usage
			float spd = getObjContext(RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, defaultSpeed);
 			definedSpd = Math.max(Math.min(spd, maxSpeed), minSpeed);
			putCache(RouteDataObjectAttribute.ROAD_SPEED, road, definedSpd, dir);
		}
		return definedSpd;
	}
	
	@Override
	public float defineVehicleSpeed(RouteDataObject road, boolean dir) {
		// don't use cache cause max/min is different for routing speed
		if (maxVehicleSpeed != maxSpeed) {
			// not implemented direction usage
			float spd = getObjContext(RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, defaultSpeed);
			return Math.max(Math.min(spd, maxVehicleSpeed), minSpeed);
		}
		Float sp = getCache(RouteDataObjectAttribute.ROAD_SPEED, road, dir);
		if (sp == null) {
			// not implemented direction usage
			float spd = getObjContext(RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, defaultSpeed);
			sp = Math.max(Math.min(spd, maxVehicleSpeed), minSpeed);
			putCache(RouteDataObjectAttribute.ROAD_SPEED, road, sp, dir);
		}
		return sp;
	}
	
	@Override
	public float defineSpeedPriority(RouteDataObject road, boolean dir) {
		Float sp = getCache(RouteDataObjectAttribute.ROAD_PRIORITIES, road, dir);
		if(sp == null) {
			// not implemented direction usage
			sp = getObjContext(RouteDataObjectAttribute.ROAD_PRIORITIES).evaluateFloat(road, 1f);
			putCache(RouteDataObjectAttribute.ROAD_PRIORITIES, road, sp, dir);
		}
		return sp;
	}
	
	@Override
	public float defineDestinationPriority(RouteDataObject road) {
		Float sp = getCache(RouteDataObjectAttribute.DESTINATION_PRIORITIES, road);
		if(sp == null) {
			sp = getObjContext(RouteDataObjectAttribute.DESTINATION_PRIORITIES).evaluateFloat(road, 1f);
			putCache(RouteDataObjectAttribute.DESTINATION_PRIORITIES, road, sp, false);
		}
		return sp;
	}

	private void putCache(RouteDataObjectAttribute attr, RouteDataObject road, Float val) {
		putCache(attr, road.region, road.types, val, false);
	}
	
	private void putCache(RouteDataObjectAttribute attr, RouteDataObject road, Float val, boolean extra) {
		putCache(attr, road.region, road.types, val, extra);
	}
	
	private void putCache(RouteDataObjectAttribute attr, RouteRegion reg, int[] types, Float val, boolean extra) {
//		TIMER -= System.nanoTime();
		Map<RouteRegion, Map<IntHolder, Float>> ch = evalCache[attr.ordinal()];
		if (USE_CACHE) {
			Map<IntHolder, Float> rM = ch.get(reg);
			if (rM == null) {
				rM = new HashMap<IntHolder, Float>();
				ch.put(reg, rM);
			}
			rM.put(new IntHolder(types, extra), val);
		}
//		TIMER += System.nanoTime();
	}
	
	class IntHolder {
		private final int[] array;
		private final boolean extra;

		IntHolder(int[] ts, boolean extra) {
			array = ts;
			this.extra = extra;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(array) + (extra ? 1 : 0);
		}

		@Override
		public boolean equals(Object other) {
			if (array == other) {
				return true;
			}
			if (!(other instanceof IntHolder)) {
				return false;
			}
			if (((IntHolder) other).extra != this.extra) {
				return false;
			}
			// noinspection unchecked
			return Arrays.equals(array, ((IntHolder) other).array);
		}
	}

	private Float getCache(RouteDataObjectAttribute attr, RouteDataObject road) {
		return getCache(attr, road.region, road.types, false);
	}
	
	private Float getCache(RouteDataObjectAttribute attr, RouteDataObject road, boolean extra) {
		return getCache(attr, road.region, road.types, extra);
	}
	
	private Float getCache(RouteDataObjectAttribute attr, RouteRegion reg, int[] types, boolean extra) {
		Map<RouteRegion, Map<IntHolder, Float>> ch = evalCache[attr.ordinal()];
//		TIMER -= System.nanoTime();
		if (USE_CACHE) {
			Map<IntHolder, Float> rM = ch.get(reg);
			if (rM == null) {
				return null;
			}
			Float vl = rM.get(new IntHolder(types, extra));
			if(vl != null) {
//				TIMER += System.nanoTime();
				return vl;
			}
		}
		return null;
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
		return sharpTurn;
	}
	
	public double getRightTurn() {
		return slightTurn;
	}
	
	public double getRoundaboutTurn() {
		return roundaboutTurn;
	}
	
	@Override
	public double calculateTurnTime(RouteSegment segment, RouteSegment prev) {
		float ts = getPenaltyTransition(segment.getRoad());
		float prevTs = getPenaltyTransition(prev.getRoad());
		float totalPenalty = 0;
		if (prevTs != ts) {
			totalPenalty += Math.abs(ts - prevTs) / 2;
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
		if (shortestRoute) {
			return totalPenalty;
		}
		if(segment.getRoad().roundabout() && !prev.getRoad().roundabout()) {
			double rt = getRoundaboutTurn();
			if(rt > 0) {
				totalPenalty += rt;
			}
		} else if (getLeftTurn() > 0 || getRightTurn() > 0) {
			double a1 = segment.getRoad().directionRoute(segment.getSegmentStart(), segment.isPositive());
			double a2 = prev.getRoad().directionRoute(prev.getSegmentEnd(), !prev.isPositive());
			double diff = Math.abs(MapUtils.alignAngleDifference(a1 - a2 - Math.PI));
			if (diff > Math.PI / 1.5) {
				totalPenalty += getLeftTurn(); // >120 degree (U-turn)
			} else if (diff > Math.PI / 3) {
				totalPenalty += getRightTurn(); // >60 degree (standard)
			} else if (diff > Math.PI / 6) {
				totalPenalty += getRightTurn() / 2; // >30 degree (light)
				// totalPenalty += getRightTurn() * diff * 3 / Math.PI; // to think
			}
		}
		
		return totalPenalty;
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
		private String[] profiles;
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
		public String getDefaultString() {
			return type == RoutingParameterType.NUMERIC ? "0.0" : "-";
		}
		public String[] getProfiles() {
			return profiles;
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
			for (RouteAttributeEvalRule rt : original.rules) {
				if (checkParameter(rt)) {
					rules.add(rt);
				}
			}
		}
		
		public RouteAttributeEvalRule[] getRules() {
			return rules.toArray(new RouteAttributeEvalRule[0]);
		}
		
		public String[] getParamKeys() {
			if(paramContext == null) {
				return new String[0];
			}
			return paramContext.vars.keySet().toArray(new String[0]);
		}
		
		public String[] getParamValues() {
			if(paramContext == null) {
				return new String[0];
			}
			return paramContext.vars.values().toArray(new String[0]);
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

		private synchronized Object evaluate(BitSet types) {
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
				if (nid == null) {
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
		public static final int MIN_EXPRESSION = 4;
		public static final int MAX_EXPRESSION = 5;

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

		private Double calculateExprValue(BitSet types, ParameterContext paramContext) {
			double f1 = calculateExprValue(0, types, paramContext);
			double f2 = calculateExprValue(1, types, paramContext);
			if (!Double.isNaN(f1) && !Double.isNaN(f2)) {
				switch (expressionType) {
					case MIN_EXPRESSION:
						return Math.min(f1, f2);
					case MAX_EXPRESSION:
						return Math.max(f1, f2);
				}
			}
			return null;
		}

		private double calculateExprValue(int id, BitSet types, ParameterContext paramContext) {
			String value = values[id];
			Number cacheValue = cacheValues[id];
			if (cacheValue != null) {
				return cacheValue.doubleValue();
			}
			Object o = null;
			if (value != null && value.startsWith("$")) {
				BitSet mask = tagRuleMask.get(value.substring(1));
				if (mask != null && mask.intersects(types)) {
					BitSet findBit = new BitSet(mask.length());
					findBit.or(mask);
					findBit.and(types);
					int v = findBit.nextSetBit(0);
					o = parseValueFromTag(v, valueType);
				}
			} else if (value != null && value.equals(":incline")) {
				return paramContext.incline;
			} else if (value != null && value.startsWith(":")) {
				String p = value.substring(1);
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
		protected RouteAttributeExpression selectExpression = null;
		protected BitSet filterTypes = new BitSet();
		protected BitSet filterNotTypes = new BitSet();
		protected BitSet evalFilterTypes = new BitSet();
		
		protected Set<String> onlyTags = new LinkedHashSet<String>();
		protected Set<String> onlyNotTags = new LinkedHashSet<String>();
		protected List<RouteAttributeExpression> conditionExpressions = new ArrayList<RouteAttributeExpression>();
		
		
		public RouteAttributeExpression[] getExpressions() {
			return conditionExpressions.toArray(new RouteAttributeExpression[0]);
		}
		
		public String[] getParameters() {
			return parameters.toArray(new String[0]);
		}
		
		public String[] getTagValueCondDefTag() {
			return tagValueCondDefTag.toArray(new String[0]);
		}
		
		public String[] getTagValueCondDefValue() {
			return tagValueCondDefValue.toArray(new String[0]);
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
			out.print(" Select " + selectValue + " if ");
			for (int k = 0; k < filterTypes.length(); k++) {
				if (filterTypes.get(k)) {
					String key = universalRulesById.get(k);
					out.print(key + " ");
				}
			}
			if (filterNotTypes.length() > 0) {
				out.print(" ifnot ");
			}
			for (int k = 0; k < filterNotTypes.length(); k++) {
				if (filterNotTypes.get(k)) {
					String key = universalRulesById.get(k);
					out.print(key + " ");
				}
			}
			for (int k = 0; k < parameters.size(); k++) {
				out.print(" param=" + parameters.get(k));
			}
			if (onlyTags.size() > 0) {
				out.print(" match tag = " + onlyTags);
			}
			if (onlyNotTags.size() > 0) {
				out.print(" not match tag = " + onlyNotTags);
			}
			if (conditionExpressions.size() > 0) {
				out.println(" subexpressions " + conditionExpressions.size());
			}
			if (selectExpression != null) {
				out.println("  selectexpression " + selectExpression.expressionType);
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
			conditionExpressions.add(new RouteAttributeExpression(new String[] { value1, value2 }, valueType,
					RouteAttributeExpression.LESS_EXPRESSION));
		}

		public void registerGreatCondition(String value1, String value2, String valueType) {
			conditionExpressions.add(new RouteAttributeExpression(new String[]{value1, value2}, valueType,
					RouteAttributeExpression.GREAT_EXPRESSION));
		}

		public void registerEqualCondition(String value1, String value2, String valueType) {
			conditionExpressions.add(new RouteAttributeExpression(new String[]{value1, value2}, valueType,
					RouteAttributeExpression.EQUAL_EXPRESSION));
		}

		public void registerMinExpression(String value1, String value2, String valueType) {
			selectExpression = new RouteAttributeExpression(new String[] { value1, value2 }, valueType,
					RouteAttributeExpression.MIN_EXPRESSION);
		}

		public void registerMaxExpression(String value1, String value2, String valueType) {
			selectExpression = new RouteAttributeExpression(new String[] { value1, value2 }, valueType,
					RouteAttributeExpression.MAX_EXPRESSION);
		}

		public void registerAndParamCondition(String param, boolean not) {
			param = not ? "-" + param : param;
			parameters.add(param);
		}

		public Object eval(BitSet types, ParameterContext paramContext) {
			if (matches(types, paramContext)) {
				return calcSelectValue(types, paramContext);
			}
			return null;
		}
		

		protected Object calcSelectValue(BitSet types, ParameterContext paramContext) {
			if (selectExpression != null) {
				selectValue = selectExpression.calculateExprValue(types, paramContext);
			} else if (selectValue instanceof String && selectValue.toString().startsWith("$")) {
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
			if (!checkAllTypesShouldBePresent(types)) {
				return false;
			}
			if (!checkAllTypesShouldNotBePresent(types)) {
				return false;
			}
			if (!checkFreeTags(types)) {
				return false;
			}
			if (!checkNotFreeTags(types)) {
				return false;
			}
			if (!checkExpressions(types, paramContext)) {
				return false;
			}
			return true;
		}

		private boolean checkExpressions(BitSet types, ParameterContext paramContext) {
			for (RouteAttributeExpression e : conditionExpressions) {
				if (!e.matches(types, paramContext)) {
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

	
	public void clearCaches() {
		if (evalCache != null) {
			for (int i = 0; i < evalCache.length; i++) {
				evalCache[i].clear();
			}
		}
	}

	public void printRules(PrintStream out) {
		for(int i = 0; i < RouteDataObjectAttribute.values().length ; i++) {
			out.println(RouteDataObjectAttribute.values()[i]);
			objectAttributes[i].printRules(out);
		}
		
	}

	public void setImpassableRoads(Set<Long> impassableRoads) {
		if (impassableRoads != null && !impassableRoads.isEmpty()) {
			this.impassableRoads = new TLongHashSet(impassableRoads);
		} else if (this.impassableRoads != null) {
			this.impassableRoads.clear();
		}
	}
}

