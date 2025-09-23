package net.osmand.router;

import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.osm.edit.Node;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.GeneralRouter.RouteAttributeContext;
import net.osmand.router.GeneralRouter.RouteDataObjectAttribute;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import gnu.trove.list.array.TIntArrayList;

public class RoutingConfiguration {

	public static final int DEFAULT_MEMORY_LIMIT = 30;
	public static final int DEFAULT_NATIVE_MEMORY_LIMIT = 256;
	public static final float DEVIATION_RADIUS = 3000;
	public static final double DEFAULT_PENALTY_FOR_REVERSE_DIRECTION = 60; // if no penaltyForReverseDirection in xml
	public Map<String, String> attributes = new LinkedHashMap<String, String>();

	// 1. parameters of routing and different tweaks
	// Influence on A* : f(x) + heuristicCoefficient*g(X)
	public float heuristicCoefficient = 1;
	
	// 1.1 tile load parameters (should not affect routing)
	public int ZOOM_TO_LOAD_TILES = 16;
	public long memoryLimitation;
	public long memoryMaxHits = -1;
	public long nativeMemoryLimitation;

	// 1.2 Build A* graph in backward/forward direction (can affect results)
	// 0 - 2 ways, 1 - direct way, -1 - reverse way
	public int planRoadDirection = 0;

	// 1.3 Router specific coefficients and restrictions
	// use GeneralRouter and not interface to simplify native access !
	public GeneralRouter router = new GeneralRouter(GeneralRouterProfile.CAR, new LinkedHashMap<String, String>());
	public String routerName = "";
	
	// 1.4 Used to calculate route in movement
	public Double initialDirection;
	public Double targetDirection;
	public double penaltyForReverseDirection = DEFAULT_PENALTY_FOR_REVERSE_DIRECTION; // -1 means reverse is forbidden

	// 1.5 Recalculate distance help
	public float recalculateDistance = 20000f;

	// 1.6 Time to calculate all access restrictions based on conditions
	public long routeCalculationTime = 0;

	// 1.6.1. Apply "unlimited" :conditional tags (used by HHRoutingShortcutCreator)
	public Map<String, String> ambiguousConditionalTags;
	
	// 1.7 Maximum visited segments
	public int MAX_VISITED = -1;


	// extra points to be inserted in ways (quad tree is based on 31 coords)
	private QuadTree<DirectionPoint> directionPoints;

	public int directionPointsRadius = 30; // 30 m

	// ! MAIN parameter to approximate (35m good for custom recorded tracks)
	public float minPointApproximation = 50;

	// don't search subsegments shorter than specified distance (also used to step back for car turns)
	public float minStepApproximation = 100;

	// This parameter could speed up or slow down evaluation (better to make bigger for long routes and smaller for short)
	public float maxStepApproximation = 3000;

	// Parameter to smoother the track itself (could be 0 if it's not recorded track)
	public float smoothenPointsNoRoute = 5;

	public boolean showMinorTurns = false;


	public QuadTree<DirectionPoint> getDirectionPoints() {
		return directionPoints;
	}

	public static class DirectionPoint extends Node {
		private static final long serialVersionUID = -7496599771204656505L;
		public double distance = Double.MAX_VALUE;
		public RouteDataObject connected;
		public TIntArrayList types = new TIntArrayList();
		public int connectedx;
		public int connectedy;
		public final static String TAG = "osmand_dp";
		public final static String DELETE_TYPE = "osmand_delete_point";
		public final static String CREATE_TYPE = "osmand_add_point";
		public final static String ANGLE_TAG = "apply_direction_angle";
		public final static double MAX_ANGLE_DIFF = 45;//in degrees

		public DirectionPoint(Node n) {
			super(n, n.getId());
		}

		// gets angle or Double.NaN if empty
		public double getAngle() {
			String angle = getTag(ANGLE_TAG);
			if (angle != null) {
				try {
					return Double.parseDouble(angle);
				} catch (NumberFormatException e) {
					throw new RuntimeException(e);
				}
			}
			return Double.NaN;
		}

	}

	public NativeLibrary.NativeDirectionPoint[] getNativeDirectionPoints() {
		if (directionPoints == null) {
			return new NativeLibrary.NativeDirectionPoint[0];
		}
		QuadRect rect = new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
		List<DirectionPoint> points = directionPoints.queryInBox(rect, new ArrayList<DirectionPoint>());
		NativeLibrary.NativeDirectionPoint[] result = new NativeLibrary.NativeDirectionPoint[points.size()];
		for (int i = 0; i < points.size(); i++) {
			DirectionPoint point = points.get(i);
			result[i] = new NativeLibrary.NativeDirectionPoint(point.getLatitude(), point.getLongitude(), point.getTags());
		}
		return result;
	}

	public static class Builder {
		// Design time storage
		private String defaultRouter = "";
		private Map<String, GeneralRouter> routers = new LinkedHashMap<>();
		private Map<String, String> attributes = new LinkedHashMap<>();
		private Set<Long> impassableRoadLocations = new HashSet<>();
		private QuadTree<Node> directionPointsBuilder;

		public Builder() {
		}

		public Builder(Map<String, String> defaultAttributes) {
			attributes.putAll(defaultAttributes);
		}

		// Example
//		{
//			impassableRoadLocations.add(23000069L);
//		}

		public RoutingConfiguration build(String router, RoutingMemoryLimits memoryLimits) {
			return build(router, null, memoryLimits, new LinkedHashMap<String, String>());
		}
		
		public RoutingConfiguration build(String router,
		                                  RoutingMemoryLimits memoryLimits,
		                                  Map<String, String> params) {
			return build(router, null, memoryLimits, params);
		}
		
		public RoutingConfiguration build(String router,
		                                  Double direction,
		                                  RoutingMemoryLimits memoryLimits,
		                                  Map<String, String> params) {
			String derivedProfile = null;
			if (!routers.containsKey(router)) {
				for (Map.Entry<String, GeneralRouter> r : routers.entrySet()) {
					String derivedProfiles = r.getValue().getAttribute("derivedProfiles");
					if (derivedProfiles != null && derivedProfiles.contains(router)) {
						derivedProfile = router;
						router = r.getKey();
						break;
					}
				}
				if (derivedProfile == null) {
					router = defaultRouter;
				}
			}
			if (derivedProfile != null) {
				params.put("profile_" + derivedProfile, String.valueOf(true));
			}
			RoutingConfiguration i = new RoutingConfiguration();
			if (routers.containsKey(router)) {
				i.router = routers.get(router);
				if (params != null) {
					i.router = i.router.build(params);
				}
				i.routerName = router;
			}
			attributes.put("routerName", router);
			i.attributes.putAll(attributes);
			i.initialDirection = direction;
			i.recalculateDistance = parseSilentFloat(getAttribute(i.router, "recalculateDistanceHelp"), i.recalculateDistance);
			i.heuristicCoefficient = parseSilentFloat(getAttribute(i.router, "heuristicCoefficient"), i.heuristicCoefficient);
			i.minPointApproximation = parseSilentFloat(getAttribute(i.router, "minPointApproximation"), i.minPointApproximation);
			i.minStepApproximation = parseSilentFloat(getAttribute(i.router, "minStepApproximation"), i.minStepApproximation);
			i.maxStepApproximation = parseSilentFloat(getAttribute(i.router, "maxStepApproximation"), i.maxStepApproximation);
			i.smoothenPointsNoRoute = parseSilentFloat(getAttribute(i.router, "smoothenPointsNoRoute"), i.smoothenPointsNoRoute);
			i.penaltyForReverseDirection = parseSilentFloat(getAttribute(i.router, "penaltyForReverseDirection"), (float) i.penaltyForReverseDirection);

			i.router.setImpassableRoads(new HashSet<>(impassableRoadLocations));
			i.ZOOM_TO_LOAD_TILES = parseSilentInt(getAttribute(i.router, "zoomToLoadTiles"), i.ZOOM_TO_LOAD_TILES);
			int memoryLimitMB = memoryLimits.memoryLimitMb;
			int desirable = parseSilentInt(getAttribute(i.router, "memoryLimitInMB"), 0);
			if (desirable != 0) {
				i.memoryLimitation = desirable * (1l << 20);
			} else {
				if (memoryLimitMB == 0) {
					memoryLimitMB = DEFAULT_MEMORY_LIMIT;
				}
				i.memoryLimitation = memoryLimitMB * (1l << 20);
			}
			int desirableNativeLimit = parseSilentInt(getAttribute(i.router, "nativeMemoryLimitInMB"), 0);
			if (desirableNativeLimit != 0) {
				i.nativeMemoryLimitation = desirableNativeLimit * (1l << 20);
			} else {
				i.nativeMemoryLimitation = memoryLimits.nativeMemoryLimitMb * (1l << 20);
			}
			i.planRoadDirection = parseSilentInt(getAttribute(i.router, "planRoadDirection"), i.planRoadDirection);
			if (directionPointsBuilder != null) {
				QuadRect rect = new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
				List<net.osmand.osm.edit.Node> lst = directionPointsBuilder.queryInBox(rect, new ArrayList<Node>());
				i.directionPoints = new QuadTree<>(rect, 14, 0.5f);
				for(Node n : lst) {
					DirectionPoint dp = new DirectionPoint(n);
					int x = MapUtils.get31TileNumberX(dp.getLongitude());
					int y = MapUtils.get31TileNumberY(dp.getLatitude());
					i.directionPoints.insert(dp, new QuadRect(x, y, x, y));
				}
			}
//			i.planRoadDirection = 1;
			return i;
		}
		
		public Builder setDirectionPoints(QuadTree<Node> directionPoints) {
			this.directionPointsBuilder = directionPoints;
			return this;
		}
		
		public void clearImpassableRoadLocations() {
			impassableRoadLocations.clear();
		}
		
		public Set<Long> getImpassableRoadLocations() {
			return impassableRoadLocations;
		}
		
		public Builder addImpassableRoad(long routeId) {
			impassableRoadLocations.add(routeId);
			return this;
		}

		public Map<String, String> getAttributes() {
			return attributes;
		}

		private String getAttribute(VehicleRouter router, String propertyName) {
			if (router.containsAttribute(propertyName)) {
				return router.getAttribute(propertyName);
			}
			return attributes.get(propertyName);
		}
		
		
		public String getDefaultRouter() {
			return defaultRouter;
		}

		public GeneralRouter getRouter(String routingProfileName) {
			return routers.get(routingProfileName);
			
		}

		public String getRoutingProfileKeyByFileName(String fileName) {
			if (fileName != null && routers != null) {
				for (Map.Entry<String, GeneralRouter> router : routers.entrySet()) {
					if (fileName.equals(router.getValue().getFilename())) {
						return router.getKey();
					}
				}
			}
			return null;
		}

		public Map<String, GeneralRouter> getAllRouters() {
			return routers;
		}

		public void removeImpassableRoad(long routeId) {
			impassableRoadLocations.remove(routeId);
		}
	}

	public static int parseSilentInt(String t, int v) {
		if (t == null || t.length() == 0) {
			return v;
		}
		return Integer.parseInt(t);
	}


	public static float parseSilentFloat(String t, float v) {
		if (t == null || t.length() == 0) {
			return v;
		}
		return Float.parseFloat(t);
	}

	
	private static RoutingConfiguration.Builder DEFAULT;

	public static RoutingConfiguration.Builder getDefault() {
		if (DEFAULT == null) {
			DEFAULT = parseDefault();
		}
		return DEFAULT;
	}


	public static Builder parseDefault() {
		try {
			return parseFromInputStream(RoutingConfiguration.class.getResourceAsStream("routing.xml"));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static RoutingConfiguration.Builder parseFromInputStream(InputStream is) throws IOException, XmlPullParserException {
		return parseFromInputStream(is, null, new RoutingConfiguration.Builder());
	}

	public static RoutingConfiguration.Builder parseFromInputStream(InputStream is, String filename, RoutingConfiguration.Builder config) throws IOException, XmlPullParserException {
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		GeneralRouter currentRouter = null;
		RouteDataObjectAttribute currentAttribute = null;
		String preType = null;
		Stack<RoutingRule> rulesStck = new Stack<RoutingConfiguration.RoutingRule>();
		parser.setInput(is, "UTF-8");
		int tok;
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.START_TAG) {
				String name = parser.getName();
				if ("osmand_routing_config".equals(name)) {
					config.defaultRouter = parser.getAttributeValue("", "defaultProfile");
				} else if ("routingProfile".equals(name)) {
					currentRouter = parseRoutingProfile(parser, config, filename);
				} else if ("attribute".equals(name)) {
					parseAttribute(parser, config, currentRouter);
				} else if ("parameter".equals(name)) {
					parseRoutingParameter(parser, currentRouter);
				} else if ("point".equals(name) || "way".equals(name)) {
					String attribute = parser.getAttributeValue("", "attribute");
					currentAttribute = RouteDataObjectAttribute.getValueOf(attribute);
					preType = parser.getAttributeValue("", "type");
				} else {
					parseRoutingRule(parser, currentRouter, currentAttribute, preType, rulesStck);
				}
			} else if (tok == XmlPullParser.END_TAG) {
				String pname = parser.getName();
				if (checkTag(pname)) {
					rulesStck.pop();
				}
			}
		}
		is.close();
		return config;
	}

	private static void parseRoutingParameter(XmlPullParser parser, GeneralRouter currentRouter) {
		String description = parser.getAttributeValue("", "description");
		String group = parser.getAttributeValue("", "group");
		String name = parser.getAttributeValue("", "name");
		String id = parser.getAttributeValue("", "id");
		String type = parser.getAttributeValue("", "type");
		String profilesList = parser.getAttributeValue("", "profiles");
		String[] profiles = Algorithms.isEmpty(profilesList) ? null : profilesList.split(",");
		boolean defaultValue = Boolean.parseBoolean(parser.getAttributeValue("", "default"));
		if ("boolean".equalsIgnoreCase(type)) {
			currentRouter.registerBooleanParameter(id, Algorithms.isEmpty(group) ? null : group, name, description, profiles, defaultValue);
		} else if ("numeric".equalsIgnoreCase(type)) {
			String values = parser.getAttributeValue("", "values");
			String valueDescriptions = parser.getAttributeValue("", "valueDescriptions");
			String[] vlsDesc = valueDescriptions.split(",");
			String[] strValues = values.split(",");
			Double[] vls = new Double[strValues.length];
			for (int i = 0; i < vls.length; i++) {
				vls[i] = Double.parseDouble(strValues[i].trim());
			}
			currentRouter.registerNumericParameter(id, name, description, profiles, vls , vlsDesc);
		} else {
			throw new UnsupportedOperationException("Unsupported routing parameter type - " + type);
		}
	}
	
	private static class RoutingRule {
		String tagName;
		String t;
		String v;
		String param;
		String value1;
		String value2;
		String type;
	}

	public static class RoutingMemoryLimits {
		public int memoryLimitMb;
		public int nativeMemoryLimitMb;

		public RoutingMemoryLimits(int memoryLimitMb, int nativeMemoryLimitMb) {
			this.memoryLimitMb = memoryLimitMb;
			this.nativeMemoryLimitMb = nativeMemoryLimitMb;
		}
	}

	private static void parseRoutingRule(XmlPullParser parser, GeneralRouter currentRouter, RouteDataObjectAttribute attr,
			String parentType, Stack<RoutingRule> stack) {
		String pname = parser.getName();
		if (checkTag(pname)) {
			if(attr == null){
				throw new NullPointerException("Select tag filter outside road attribute < " + pname + " > : "+parser.getLineNumber());
			}
			RoutingRule rr = new RoutingRule();
			rr.tagName = pname;
			rr.t = parser.getAttributeValue("", "t");
			rr.v = parser.getAttributeValue("", "v");
			rr.param = parser.getAttributeValue("", "param");
			rr.value1 = parser.getAttributeValue("", "value1");
			rr.value2 = parser.getAttributeValue("", "value2");
			rr.type = parser.getAttributeValue("", "type");
			if((rr.type == null || rr.type.length() == 0) &&
					parentType != null && parentType.length() > 0) {
				rr.type = parentType;
			}
			
			RouteAttributeContext ctx = currentRouter.getObjContext(attr);
			if ("select".equals(rr.tagName)) {
				String val = parser.getAttributeValue("", "value");
				String type = rr.type;
				ctx.registerNewRule(val, type);
				addSubclause(rr, ctx);
				for (int i = 0; i < stack.size(); i++) {
					addSubclause(stack.get(i), ctx);
				}
			} else if ("min".equals(rr.tagName) || "max".equals(rr.tagName)) {
				String initVal = parser.getAttributeValue("", "value1");
				String type = rr.type;
				ctx.registerNewRule(initVal, type);
				addSubclause(rr, ctx);
			} else if (stack.size() > 0 && "select".equals(stack.peek().tagName)) {
				addSubclause(rr, ctx);
			}
			stack.push(rr);
		}
	}

	private static boolean checkTag(String pname) {
		return "select".equals(pname) || "if".equals(pname) || "ifnot".equals(pname)
				|| "gt".equals(pname) || "le".equals(pname) || "eq".equals(pname)
				|| "min".equals(pname) || "max".equals(pname);
	}

	private static void addSubclause(RoutingRule rr, RouteAttributeContext ctx) {
		boolean not = "ifnot".equals(rr.tagName);
		if(!Algorithms.isEmpty(rr.param)) {
			if (rr.param.contains(",")) {
				String[] params = rr.param.split(",");
				for (String p : params) {
					p = p.trim();
					if (!Algorithms.isEmpty(p)) {
						ctx.getLastRule().registerAndParamCondition(p, not);
					}
				}
			} else {
				ctx.getLastRule().registerAndParamCondition(rr.param, not);
			}
		}
		if (!Algorithms.isEmpty(rr.t)) {
			ctx.getLastRule().registerAndTagValueCondition(rr.t, Algorithms.isEmpty(rr.v) ? null : rr.v, not);
		}
		switch (rr.tagName) {
			case "gt":
				ctx.getLastRule().registerGreatCondition(rr.value1, rr.value2, rr.type);
				break;
			case "le":
				ctx.getLastRule().registerLessCondition(rr.value1, rr.value2, rr.type);
				break;
			case "eq":
				ctx.getLastRule().registerEqualCondition(rr.value1, rr.value2, rr.type);
				break;
			case "min":
				ctx.getLastRule().registerMinExpression(rr.value1, rr.value2, rr.type);
				break;
			case "max":
				ctx.getLastRule().registerMaxExpression(rr.value1, rr.value2, rr.type);
				break;
		}
	}

	private static GeneralRouter parseRoutingProfile(XmlPullParser parser, final RoutingConfiguration.Builder config, String filename) {
		String currentSelectedRouterName = parser.getAttributeValue("", "name");
		Map<String, String> attrs = new LinkedHashMap<String, String>();
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			attrs.put(parser.getAttributeName(i), parser.getAttributeValue(i));
		}
		GeneralRouterProfile c = Algorithms.parseEnumValue(GeneralRouterProfile.values(), 
				parser.getAttributeValue("", "baseProfile"), GeneralRouterProfile.CAR);
		GeneralRouter currentRouter = new GeneralRouter(c, attrs);
		currentRouter.setProfileName(currentSelectedRouterName);
		if (filename != null) {
			currentRouter.setFilename(filename);
			currentSelectedRouterName = filename + "/" + currentSelectedRouterName;
		}

		config.routers.put(currentSelectedRouterName, currentRouter);
		return currentRouter;
	}

	private static void parseAttribute(XmlPullParser parser, final RoutingConfiguration.Builder config, GeneralRouter currentRouter) {
		if(currentRouter != null) {
			currentRouter.addAttribute(parser.getAttributeValue("", "name"), 
					parser.getAttributeValue("", "value"));
		} else {
			config.attributes.put(parser.getAttributeValue("", "name"),
					parser.getAttributeValue("", "value"));
		}
	}
	
}
