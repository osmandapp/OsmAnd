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
	
	public static final int DEFAULT_MEMORY_LIMIT = 30;
	

	// 1.1 tile load parameters (should not affect routing)
	public int ZOOM_TO_LOAD_TILES = 16;
	public int memoryLimitation;

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
	public VehicleRouter router = new GeneralRouter(GeneralRouterProfile.CAR, new LinkedHashMap<String, String>());
	public String routerName = "";
	
	// 1.6 Used to calculate route in movement
	public Double initialDirection;


	public static class Builder {
		// Design time storage
		private String defaultRouter = "";
		private Map<String, GeneralRouter> routers = new LinkedHashMap<String, GeneralRouter>();
		private Map<String, String> attributes = new LinkedHashMap<String, String>();

		public RoutingConfiguration build(String router, int memoryLimitMB, String... specialization) {
			return build(router, null, memoryLimitMB, specialization);
		}
		public RoutingConfiguration build(String router, Double direction, int memoryLimitMB, String... specialization) {
			if (!routers.containsKey(router)) {
				router = defaultRouter;
			}
			RoutingConfiguration i = new RoutingConfiguration();
			i.initialDirection = direction;
			i.heuristicCoefficient = parseSilentDouble(getAttribute(router, "heuristicCoefficient"), i.heuristicCoefficient);
			i.ZOOM_TO_LOAD_TILES = parseSilentInt(getAttribute(router, "zoomToLoadTiles"), i.ZOOM_TO_LOAD_TILES);
			int desirable = parseSilentInt(getAttribute(router, "memoryLimitInMB"), 0);
			if(desirable != 0) {
				i.memoryLimitation = desirable * (1 << 20); 
			} else {
				if(memoryLimitMB == 0) {
					memoryLimitMB = DEFAULT_MEMORY_LIMIT;
				}
				i.memoryLimitation = memoryLimitMB * (1 << 20);
			}
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
			if(specialization != null) {
				for(String s : specialization) {
					i.router = i.router.specialization(s);
				}
			}
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
		if (t == null || t.length() == 0) {
			return v;
		}
		return Integer.parseInt(t);
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
				String previousKey = null;
				String previousTag = null;
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					String name = parser.isNamespaceAware() ? localName : qName;
					if("osmand_routing_config".equals(name)) {
						config.defaultRouter = attributes.getValue("defaultProfile");
					} else if("attribute".equals(name)) {
						previousKey = attributes.getValue("name");
						previousTag = name;
						if (currentSelectedRouter != null) {
							previousKey = currentSelectedRouter + "$" + previousKey;
						}
						config.attributes.put(previousKey, attributes.getValue("value"));
					} else if("routingProfile".equals(name)) {
						currentSelectedRouter = attributes.getValue("name");
						Map<String, String> attrs = new LinkedHashMap<String, String>();
						for(int i=0; i< attributes.getLength(); i++) {
							attrs.put(parser.isNamespaceAware() ? attributes.getLocalName(i) : attributes.getQName(i), attributes.getValue(i));
						}
						currentRouter = new GeneralRouter(GeneralRouterProfile.valueOf(attributes.getValue("baseProfile").toUpperCase()), attrs);
						config.routers.put(currentSelectedRouter, currentRouter);
					} else if ("specialization".equals(name)) {
						String in = attributes.getValue("input");
						if (previousKey != null) {
							String k = in + ":" + previousKey;
							if (attributes.getValue("penalty") != null) {
								double penalty = parseSilentDouble(attributes.getValue("penalty"), 0);
								currentRouter.obstacles.put(k, penalty);
								double routingPenalty = parseSilentDouble(attributes.getValue("routingPenalty"), penalty );
								currentRouter.routingObstacles.put(k, routingPenalty);
							}
							if (attributes.getValue("priority") != null) {
								currentRouter.highwayPriorities.put(k, parseSilentDouble(attributes.getValue("priority"), 0));
							}
							if (attributes.getValue("dynamicPriority") != null) {
								currentRouter.highwayFuturePriorities.put(k, parseSilentDouble(attributes.getValue("dynamicPriority"), 0));
							}
							if (attributes.getValue("speed") != null) {
								currentRouter.highwaySpeed.put(k, parseSilentDouble(attributes.getValue("speed"), 0));
							}
							if ("avoid".equals(previousTag)) {
								double priority = parseSilentDouble(attributes.getValue("decreasedPriority"), 0);
								if (priority == 0) {
									currentRouter.avoid.put(k, priority);
								} else {
									currentRouter.highwayPriorities.put(k, priority);
								}
							}
						}

					} else if("road".equals(name)) {
						previousTag = name;
						previousKey = attributes.getValue("tag") +"$" + attributes.getValue("value");
						currentRouter.highwayPriorities.put(previousKey, parseSilentDouble(attributes.getValue("priority"), 
								1));
						currentRouter.highwayFuturePriorities.put(previousKey, parseSilentDouble(attributes.getValue("dynamicPriority"), 
								1));
						currentRouter.highwaySpeed.put(previousKey, parseSilentDouble(attributes.getValue("speed"), 
								10));
					} else if("obstacle".equals(name)) {
						previousTag = name;
						previousKey = attributes.getValue("tag") + "$" + attributes.getValue("value");
						double penalty = parseSilentDouble(attributes.getValue("penalty"), 0);
						currentRouter.obstacles.put(previousKey, penalty);
						double routingPenalty = parseSilentDouble(attributes.getValue("routingPenalty"), penalty );
						currentRouter.routingObstacles.put(previousKey, routingPenalty);
					} else if("avoid".equals(name)) {
						previousTag = name;
						previousKey = attributes.getValue("tag") + "$" + attributes.getValue("value");
						double priority = parseSilentDouble(attributes.getValue("decreasedPriority"), 
								0);
						if(priority == 0) {
							currentRouter.avoid.put(previousKey, priority);
						}  else {
							currentRouter.highwayPriorities.put(previousKey, priority);
						}
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
