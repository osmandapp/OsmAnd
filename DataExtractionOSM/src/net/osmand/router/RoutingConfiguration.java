package net.osmand.router;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.osmand.router.GeneralRouter.GeneralRouterProfile;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RoutingConfiguration {
	// 1. parameters of routing and different tweaks
	// Influence on A* : f(x) + heuristicCoefficient*g(X)
	public double heuristicCoefficient = 1;

	// 1.1 tile load parameters (should not affect routing)
	public int ZOOM_TO_LOAD_TILES = 13; // 12?, 14?
	public int ITERATIONS_TO_RUN_GC = 100;
	public int NUMBER_OF_DESIRABLE_TILES_IN_MEMORY = 25;

	// 1.2 Dynamic road prioritizing (heuristic)
	public boolean useDynamicRoadPrioritising = true;
	public int dynamicRoadPriorityDistance = 0;

	// 1.3 Relaxing strategy
	public boolean useRelaxingStrategy = true;
	public int ITERATIONS_TO_RELAX_NODES = 100;
	public double RELAX_NODES_IF_START_DIST_COEF = 3;

	// 1.4 Build A* graph in backward/forward direction (can affect results)
	// 0 - 2 ways, 1 - direct way, -1 - reverse way
	public int planRoadDirection = 0;

	// 1.5 Router specific coefficients and restrictions
	public VehicleRouter router = new GeneralRouter();
	public String routerName = "";
	
	// 1.6 Used to calculate route in movement
	public Double initialDirection;


	public static class Builder {
		// Design time storage
		private String defaultRouter = "";
		private Map<String, VehicleRouter> routers = new LinkedHashMap<String, VehicleRouter>();
		private Map<String, String> attributes = new LinkedHashMap<String, String>();

		public RoutingConfiguration build(String router, boolean useShortestWay) {
			return build(router, useShortestWay, null);
		}
		public RoutingConfiguration build(String router, boolean useShortestWay, Double direction) {
			if (!routers.containsKey(router)) {
				router = defaultRouter;
			}
			RoutingConfiguration i = new RoutingConfiguration();
			i.initialDirection = direction;
			i.heuristicCoefficient = parseSilentDouble(getAttribute(router, "heuristicCoefficient"), i.heuristicCoefficient);
			i.ZOOM_TO_LOAD_TILES = parseSilentInt(getAttribute(router, "zoomToLoadTiles"), i.ZOOM_TO_LOAD_TILES);
			i.ITERATIONS_TO_RUN_GC = parseSilentInt(getAttribute(router, "iterationsToRunGC"), i.ITERATIONS_TO_RUN_GC);
			i.NUMBER_OF_DESIRABLE_TILES_IN_MEMORY = parseSilentInt(getAttribute(router, "desirableTilesInMemory"),
					i.NUMBER_OF_DESIRABLE_TILES_IN_MEMORY);

			i.useDynamicRoadPrioritising = parseSilentBoolean(getAttribute(router, "useDynamicRoadPrioritising"), i.useDynamicRoadPrioritising);
			i.useRelaxingStrategy = parseSilentBoolean(getAttribute(router, "useRelaxingStrategy"), i.useRelaxingStrategy);
			i.dynamicRoadPriorityDistance = parseSilentInt(getAttribute(router, "dynamicRoadPriorityDistance"), i.dynamicRoadPriorityDistance);
			i.ITERATIONS_TO_RELAX_NODES = parseSilentInt(getAttribute(router, "iterationsToRelaxRoutes"), i.ITERATIONS_TO_RELAX_NODES);
			i.RELAX_NODES_IF_START_DIST_COEF = parseSilentDouble(getAttribute(router, "relaxNodesIfStartDistSmallCoeff"), i.RELAX_NODES_IF_START_DIST_COEF);
			i.planRoadDirection = parseSilentInt(getAttribute(router, "planRoadDirection"), i.planRoadDirection);

			if (!routers.containsKey(router)) {
				return i;
			}
			i.router = routers.get(router);
			i.routerName = router;
			return i;
		}
		
		private String getAttribute(String router, String propertyName) {
			if (attributes.containsKey(router + "$" + propertyName)) {
				return attributes.get(router + "$" + propertyName);
			}
			return attributes.get(propertyName);
		}
		
	}

	private static int parseSilentInt(String t, int v) {
		if (t == null) {
			return v;
		}
		return Integer.parseInt(t);
	}

	private static boolean parseSilentBoolean(String t, boolean v) {
		if (t == null) {
			return v;
		}
		return Boolean.parseBoolean(t);
	}

	private static double parseSilentDouble(String t, double v) {
		if (t == null) {
			return v;
		}
		return Double.parseDouble(t);
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

	public static RoutingConfiguration.Builder parseFromInputStream(InputStream is) throws SAXException, IOException {
		try {
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			final RoutingConfiguration.Builder config = new RoutingConfiguration.Builder();
			DefaultHandler handler = new DefaultHandler() {
				String currentSelectedRouter = null;
				GeneralRouter currentRouter = null;
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					String name = parser.isNamespaceAware() ? localName : qName;
					if("osmand_routing_config".equals(name)) {
						config.defaultRouter = attributes.getValue("defaultProfile");
					} else if("attribute".equals(name)) {
						String key = attributes.getValue("name");
						if(currentSelectedRouter != null) {
							key = currentSelectedRouter +"$"+key;
						}
						config.attributes.put(key, attributes.getValue("value"));
					} else if("routingProfile".equals(name)) {
						currentSelectedRouter = attributes.getValue("name");
						currentRouter = new GeneralRouter();
						currentRouter.profile = GeneralRouterProfile.valueOf(attributes.getValue("baseProfile").toUpperCase());
						config.routers.put(currentSelectedRouter, currentRouter);
						currentRouter.restrictionsAware = parseSilentBoolean(attributes.getValue("restrictionsAware"), 
								currentRouter.restrictionsAware);
						currentRouter.followSpeedLimitations = parseSilentBoolean(attributes.getValue("followSpeedLimitations"), 
								currentRouter.followSpeedLimitations);
						currentRouter.onewayAware = parseSilentBoolean(attributes.getValue("onewayAware"), 
								currentRouter.onewayAware);
						currentRouter.minDefaultSpeed = parseSilentDouble(attributes.getValue("minDefaultSpeed"), 
								currentRouter.minDefaultSpeed);
						currentRouter.maxDefaultSpeed = parseSilentDouble(attributes.getValue("maxDefaultSpeed"), 
								currentRouter.maxDefaultSpeed);
						currentRouter.leftTurn = parseSilentDouble(attributes.getValue("leftTurn"), 
								currentRouter.leftTurn);
						currentRouter.rightTurn = parseSilentDouble(attributes.getValue("rightTurn"), 
								currentRouter.rightTurn);
					} else if("highway".equals(name)) {
						String key = attributes.getValue("value");
						currentRouter.highwayPriorities.put(key, parseSilentDouble(attributes.getValue("priority"), 
								1));
						currentRouter.highwayFuturePriorities.put(key, parseSilentDouble(attributes.getValue("dynamicPriority"), 
								1));
						currentRouter.highwaySpeed.put(key, parseSilentDouble(attributes.getValue("speed"), 
								currentRouter.minDefaultSpeed));
					} else if("obstacle".equals(name)) {
						String key = attributes.getValue("tag") + "$" + attributes.getValue("value");
						currentRouter.obstacles.put(key, parseSilentDouble(attributes.getValue("penalty"), 
								0));
					} else if("avoid".equals(name)) {
						String key = attributes.getValue("tag") + "$" + attributes.getValue("value");
						currentRouter.avoidElements.put(key, parseSilentDouble(attributes.getValue("coefficient"), 
								0));
					}
				}

			};
			parser.parse(is, handler);
			return config;
		} catch (ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}
	
}
