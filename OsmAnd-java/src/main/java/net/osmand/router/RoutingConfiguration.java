package net.osmand.router;

import net.osmand.PlatformUtil;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.GeneralRouter.RouteAttributeContext;
import net.osmand.router.GeneralRouter.RouteDataObjectAttribute;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class RoutingConfiguration {

	public static final int DEFAULT_MEMORY_LIMIT = 30;
	public static final float DEVIATION_RADIUS = 3000;
	public Map<String, String> attributes = new LinkedHashMap<String, String>();

	// 1. parameters of routing and different tweaks
	// Influence on A* : f(x) + heuristicCoefficient*g(X)
	public float heuristicCoefficient = 1;
	
	// 1.1 tile load parameters (should not affect routing)
	public int ZOOM_TO_LOAD_TILES = 16;
	public long memoryLimitation;

	// 1.2 Build A* graph in backward/forward direction (can affect results)
	// 0 - 2 ways, 1 - direct way, -1 - reverse way
	public int planRoadDirection = 0;

	// 1.3 Router specific coefficients and restrictions
	// use GeneralRouter and not interface to simplify native access !
	public GeneralRouter router = new GeneralRouter(GeneralRouterProfile.CAR, new LinkedHashMap<String, String>());
	public String routerName = "";
	
	// 1.4 Used to calculate route in movement
	public Double initialDirection;
	
	// 1.5 Recalculate distance help
	public float recalculateDistance = 20000f;
	
	// 1.6 Time to calculate all access restrictions based on conditions
	public long routeCalculationTime = 0;
	
	public static class Builder {
		// Design time storage
		private String defaultRouter = "";
		private Map<String, GeneralRouter> routers = new LinkedHashMap<>();
		private Map<String, String> attributes = new LinkedHashMap<>();
		private Set<Long> impassableRoadLocations = new HashSet<>();

		public Builder() {

		}

		public Builder(Map<String, String> defaultAttributes) {
			attributes.putAll(defaultAttributes);
		}

		// Example
//		{
//			impassableRoadLocations.add(23000069L);
//		}

		public RoutingConfiguration build(String router, int memoryLimitMB) {
			return build(router, null, memoryLimitMB, null);
		}
		
		public RoutingConfiguration build(String router, int memoryLimitMB, Map<String, String> params) {
			return build(router, null, memoryLimitMB, params);
		}
		
		public RoutingConfiguration build(String router, Double direction, int memoryLimitMB, Map<String, String> params) {
			if (!routers.containsKey(router)) {
				router = defaultRouter;
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
			i.recalculateDistance = parseSilentFloat(getAttribute(i.router, "recalculateDistanceHelp"), i.recalculateDistance) ;
			i.heuristicCoefficient = parseSilentFloat(getAttribute(i.router, "heuristicCoefficient"), i.heuristicCoefficient);
			i.router.addImpassableRoads(new HashSet<>(impassableRoadLocations));
			i.ZOOM_TO_LOAD_TILES = parseSilentInt(getAttribute(i.router, "zoomToLoadTiles"), i.ZOOM_TO_LOAD_TILES);
			int desirable = parseSilentInt(getAttribute(i.router, "memoryLimitInMB"), 0);
			if(desirable != 0) {
				i.memoryLimitation = desirable * (1l << 20);
			} else {
				if(memoryLimitMB == 0) {
					memoryLimitMB = DEFAULT_MEMORY_LIMIT;
				}
				i.memoryLimitation = memoryLimitMB * (1l << 20);
			}
			i.planRoadDirection = parseSilentInt(getAttribute(i.router, "planRoadDirection"), i.planRoadDirection);
//			i.planRoadDirection = 1;
			return i;
		}

		public Set<Long> getImpassableRoadLocations() {
			return impassableRoadLocations;
		}
		
		public boolean addImpassableRoad(long routeId) {
			return impassableRoadLocations.add(routeId);
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
			try {
				DEFAULT = parseFromInputStream(RoutingConfiguration.class.getResourceAsStream("routing.xml"));
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		return DEFAULT;
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
		boolean defaultValue = Boolean.parseBoolean(parser.getAttributeValue("", "default"));
		if ("boolean".equalsIgnoreCase(type)) {
			currentRouter.registerBooleanParameter(id, Algorithms.isEmpty(group) ? null : group, name, description, defaultValue);
		} else if ("numeric".equalsIgnoreCase(type)) {
			String values = parser.getAttributeValue("", "values");
			String valueDescriptions = parser.getAttributeValue("", "valueDescriptions");
			String[] strValues = values.split(",");
			Double[] vls = new Double[strValues.length];
			for (int i = 0; i < vls.length; i++) {
				vls[i] = Double.parseDouble(strValues[i].trim());
			}
			currentRouter.registerNumericParameter(id, name, description, vls , 
					valueDescriptions.split(","));
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
			if("select".equals(rr.tagName)) {
				String val = parser.getAttributeValue("", "value");
				String type = rr.type;
				ctx.registerNewRule(val, type);
				addSubclause(rr, ctx);
				for (int i = 0; i < stack.size(); i++) {
					addSubclause(stack.get(i), ctx);
				}
			} else if (stack.size() > 0 && "select".equals(stack.peek().tagName)) {
				addSubclause(rr, ctx);
			}
			stack.push(rr);
		}
	}

	private static boolean checkTag(String pname) {
		return "select".equals(pname) || "if".equals(pname) || "ifnot".equals(pname)
				|| "gt".equals(pname) || "le".equals(pname) || "eq".equals(pname);
	}

	private static void addSubclause(RoutingRule rr, RouteAttributeContext ctx) {
		boolean not = "ifnot".equals(rr.tagName);
		if(!Algorithms.isEmpty(rr.param)) {
			ctx.getLastRule().registerAndParamCondition(rr.param, not);
		}
		if (!Algorithms.isEmpty(rr.t)) {
			ctx.getLastRule().registerAndTagValueCondition(rr.t, Algorithms.isEmpty(rr.v) ? null : rr.v, not);
		}
		if ("gt".equals(rr.tagName)) {
			ctx.getLastRule().registerGreatCondition(rr.value1, rr.value2, rr.type);
		} else if ("le".equals(rr.tagName)) {
			ctx.getLastRule().registerLessCondition(rr.value1, rr.value2, rr.type);
		} else if ("eq".equals(rr.tagName)) {
			ctx.getLastRule().registerEqualCondition(rr.value1, rr.value2, rr.type);
		}
	}

	private static GeneralRouter parseRoutingProfile(XmlPullParser parser, final RoutingConfiguration.Builder config, String filename) {
		String currentSelectedRouterName = parser.getAttributeValue("", "name");
		Map<String, String> attrs = new LinkedHashMap<String, String>();
		for(int i=0; i< parser.getAttributeCount(); i++) {
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
