package net.osmand.router;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.Collator;
import net.osmand.PlatformUtil;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RoutingConfiguration {
	
	public static final int DEFAULT_MEMORY_LIMIT = 30;
	public Map<String, String> attributes = new LinkedHashMap<String, String>();

	// 1. parameters of routing and different tweaks
	// Influence on A* : f(x) + heuristicCoefficient*g(X)
	public float heuristicCoefficient = 1;
	
	// 1.1 tile load parameters (should not affect routing)
	public int ZOOM_TO_LOAD_TILES = 16;
	public int memoryLimitation;

	// 1.2 Build A* graph in backward/forward direction (can affect results)
	// 0 - 2 ways, 1 - direct way, -1 - reverse way
	public int planRoadDirection = 0;

	// 1.3 Router specific coefficients and restrictions
	public VehicleRouter router = new GeneralRouter(GeneralRouterProfile.CAR, new LinkedHashMap<String, String>());
	public String routerName = "";
	
	// 1.4 Used to calculate route in movement
	public Double initialDirection;
	
	// 1.5 Recalculate distance help
	public float recalculateDistance = 10000f;
	


	public static class Builder {
		// Design time storage
		private String defaultRouter = "";
		private Map<String, GeneralRouter> routers = new LinkedHashMap<String, GeneralRouter>();
		private Map<String, String> attributes = new LinkedHashMap<String, String>();

		public RoutingConfiguration build(String router, int memoryLimitMB) {
			return build(router, null, memoryLimitMB, new String[0]);
		}
		
		public RoutingConfiguration build(String router, int memoryLimitMB, String[] specialization) {
			return build(router, null, memoryLimitMB, specialization);
		}
		public RoutingConfiguration build(String router, Double direction, int memoryLimitMB, String[] specialization) {
			if (!routers.containsKey(router)) {
				router = defaultRouter;
			}
			RoutingConfiguration i = new RoutingConfiguration();
			if (routers.containsKey(router)) {
				i.router = routers.get(router);
				if (specialization != null) {
					for (String s : specialization) {
						i.router = i.router.specifyParameter(s);
					}
				}
				i.routerName = router;
			}
			attributes.put("routerName", router);
			i.attributes.putAll(attributes);
			i.initialDirection = direction;
			i.recalculateDistance = parseSilentFloat(getAttribute(i.router, "recalculateDistanceHelp"), i.recalculateDistance) ;
			i.heuristicCoefficient = parseSilentFloat(getAttribute(i.router, "heuristicCoefficient"), i.heuristicCoefficient);
			i.ZOOM_TO_LOAD_TILES = parseSilentInt(getAttribute(i.router, "zoomToLoadTiles"), i.ZOOM_TO_LOAD_TILES);
			int desirable = parseSilentInt(getAttribute(i.router, "memoryLimitInMB"), 0);
			if(desirable != 0) {
				i.memoryLimitation = desirable * (1 << 20); 
			} else {
				if(memoryLimitMB == 0) {
					memoryLimitMB = DEFAULT_MEMORY_LIMIT;
				}
				i.memoryLimitation = memoryLimitMB * (1 << 20);
			}
			i.planRoadDirection = parseSilentInt(getAttribute(i.router, "planRoadDirection"), i.planRoadDirection);
			
			return i;
		}
		
		private String getAttribute(VehicleRouter router, String propertyName) {
		    String attr = router.getAttribute(propertyName);
			if (attr != null) {
				return attr;
			}
			return attributes.get(propertyName);
		}
		
	}

	private static int parseSilentInt(String t, int v) {
		if (t == null || t.length() == 0) {
			return v;
		}
		return Integer.parseInt(t);
	}


	private static float parseSilentFloat(String t, float v) {
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
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		final RoutingConfiguration.Builder config = new RoutingConfiguration.Builder();
		GeneralRouter currentRouter = null;
		String previousKey = null;
		String previousTag = null;
		int tok;
		parser.setInput(is, "UTF-8");
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.START_TAG) {
				String name = parser.getName();
				if ("osmand_routing_config".equals(name)) {
					config.defaultRouter = parser.getAttributeValue("", "defaultProfile");
				} else if ("routingProfile".equals(name)) {
					currentRouter = parseRoutingProfile(parser, config);
				} else if ("attribute".equals(name)) {
					parseAttribute(parser, config, currentRouter);
					previousKey = parser.getAttributeValue("", "name");
					previousTag = name;
				} else if ("specialization".equals(name)) {
					parseSpecialization(parser, currentRouter, previousKey, previousTag);
				} else {
					previousKey = parser.getAttributeValue("", "tag") + "$" + parser.getAttributeValue("", "value");
					previousTag = name;
					if (parseCurrentRule(parser, currentRouter, previousKey, name)) {

					} else {

					}
				}
			}
		}
		return config;
	}

	private static boolean parseCurrentRule(XmlPullParser parser, GeneralRouter currentRouter, String key, String name) {
		if ("road".equals(name)) {
			currentRouter.highwayPriorities.put(key, parseSilentFloat(parser.getAttributeValue("", "priority"), 1));
			currentRouter.highwaySpeed.put(key, parseSilentFloat(parser.getAttributeValue("", "speed"), 10));
			return true;
		} else if ("obstacle".equals(name)) {
			float penalty = parseSilentFloat(parser.getAttributeValue("", "penalty"), 0);
			currentRouter.obstacles.put(key, penalty);
			float routingPenalty = parseSilentFloat(parser.getAttributeValue("", "routingPenalty"), penalty);
			currentRouter.routingObstacles.put(key, routingPenalty);
			return true;
		} else if ("avoid".equals(name)) {
			float priority = parseSilentFloat(parser.getAttributeValue("", "decreasedPriority"), 0);
			if (priority == 0) {
				currentRouter.avoid.put(key, priority);
			} else {
				currentRouter.highwayPriorities.put(key, priority);
			}
			return true;
		} else {
			return false;
		}
	}

	private static void parseSpecialization(XmlPullParser parser, GeneralRouter currentRouter, String previousKey, String previousTag) {
		String in = parser.getAttributeValue("","input");
		if (previousKey != null) {
			String k = in + ":" + previousKey;
			if (parser.getAttributeValue("","penalty") != null) {
				float penalty = parseSilentFloat(parser.getAttributeValue("","penalty"), 0);
				currentRouter.obstacles.put(k, penalty);
				float routingPenalty = parseSilentFloat(parser.getAttributeValue("","routingPenalty"), penalty );
				currentRouter.routingObstacles.put(k, routingPenalty);
			}
			if (parser.getAttributeValue("","priority") != null) {
				currentRouter.highwayPriorities.put(k, parseSilentFloat(parser.getAttributeValue("","priority"), 0));
			}
			if (parser.getAttributeValue("","speed") != null) {
				currentRouter.highwaySpeed.put(k, parseSilentFloat(parser.getAttributeValue("","speed"), 0));
			}
			if ("attribute".equals(previousTag)) {
				currentRouter.attributes.put(k, parser.getAttributeValue("","value"));
			}
			if ("avoid".equals(previousTag)) {
				float priority = parseSilentFloat(parser.getAttributeValue("","decreasedPriority"), 0);
				if (priority == 0) {
					currentRouter.avoid.put(k, priority);
				} else {
					currentRouter.highwayPriorities.put(k, priority);
				}
			}
		}
	}

	private static GeneralRouter parseRoutingProfile(XmlPullParser parser, final RoutingConfiguration.Builder config) {
		GeneralRouter currentRouter;
		String currentSelectedRouter = parser.getAttributeValue("", "name");
		Map<String, String> attrs = new LinkedHashMap<String, String>();
		for(int i=0; i< parser.getAttributeCount(); i++) {
			attrs.put(parser.getAttributeName(i), parser.getAttributeValue(i));
		}
		GeneralRouterProfile c = Algorithms.parseEnumValue(GeneralRouterProfile.values(), 
				parser.getAttributeValue("", "baseProfile"), GeneralRouterProfile.CAR);
		currentRouter = new GeneralRouter(c, attrs);
		config.routers.put(currentSelectedRouter, currentRouter);
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
