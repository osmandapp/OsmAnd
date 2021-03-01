package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RouteStatisticsHelper {

	public static final String UNDEFINED_ATTR = "undefined";
	public static final String ROUTE_INFO_PREFIX = "routeInfo_";
	private static final double H_STEP = 5;
	private static final double H_SLOPE_APPROX = 100;
	private static final int MIN_INCLINE = -101;
	private static final int MIN_DIVIDED_INCLINE = -20;
	private static final int MAX_INCLINE = 100;
	private static final int MAX_DIVIDED_INCLINE = 20;
	private static final int STEP = 4;
	private static final int[] BOUNDARIES_ARRAY;
	private static final String[] BOUNDARIES_CLASS;

	static {
		int NUM = ((MAX_DIVIDED_INCLINE - MIN_DIVIDED_INCLINE) / STEP) + 3;
		BOUNDARIES_ARRAY = new int[NUM];
		BOUNDARIES_CLASS = new String[NUM];
		BOUNDARIES_ARRAY[0] = MIN_INCLINE;
		BOUNDARIES_CLASS[0] = "steepness=" + (MIN_INCLINE + 1) + "_" + MIN_DIVIDED_INCLINE;
		for (int i = 1; i < NUM - 1; i++) {
			BOUNDARIES_ARRAY[i] = MIN_DIVIDED_INCLINE + (i - 1) * STEP;
			BOUNDARIES_CLASS[i] = "steepness=" + (BOUNDARIES_ARRAY[i - 1] + 1) + "_" + BOUNDARIES_ARRAY[i];
		}
		BOUNDARIES_ARRAY[NUM - 1] = MAX_INCLINE;
		BOUNDARIES_CLASS[NUM - 1] = "steepness="+MAX_DIVIDED_INCLINE+"_"+MAX_INCLINE;
	}



	public static class RouteStatistics {
		public final List<RouteSegmentAttribute> elements;
		public final Map<String, RouteSegmentAttribute> partition;
		public final float totalDistance;
		public final String name;

		private RouteStatistics(String name, List<RouteSegmentAttribute> elements,
								Map<String, RouteSegmentAttribute> partition,
								float totalDistance) {
			this.name = name.startsWith(ROUTE_INFO_PREFIX) ? name.substring(ROUTE_INFO_PREFIX.length()) : name;
			this.elements = elements;
			this.partition = partition;
			this.totalDistance = totalDistance;
		}
		
		@Override
		public String toString() {
			StringBuilder s  = new StringBuilder("Statistic '").append(name).append("':");
			for (RouteSegmentAttribute a : elements) {
				s.append(String.format(" %.0fm %s,", a.distance, a.getUserPropertyName()));
			}
			s.append("\n  Partition: ").append(partition);
			return s.toString();
		}
	}
	

	private static class RouteSegmentWithIncline {
		RouteDataObject obj;
		float dist;
		float h;
		float[] interpolatedHeightByStep;
		float[] slopeByStep;
		String[] slopeClassUserString;
		int[] slopeClass;
	}

	public static List<RouteStatistics> calculateRouteStatistic(List<RouteSegmentResult> route, RenderingRulesStorage currentRenderer,
																RenderingRulesStorage defaultRenderer, RenderingRuleSearchRequest currentSearchRequest,
																RenderingRuleSearchRequest defaultSearchRequest) {
		List<RouteSegmentWithIncline> routeSegmentWithInclines = calculateInclineRouteSegments(route);
		List<String> attributeNames = new ArrayList<>();
		if (currentRenderer != null) {
			for (String s : currentRenderer.getRenderingAttributeNames()) {
				if (s.startsWith(ROUTE_INFO_PREFIX)) {
					attributeNames.add(s);
				}
			}
		}
		if(attributeNames.isEmpty()) {
			for (String s : defaultRenderer.getRenderingAttributeNames()) {
				if (s.startsWith(ROUTE_INFO_PREFIX)) {
					attributeNames.add(s);
				}
			}
		}

		// "steepnessColor", "surfaceColor", "roadClassColor", "smoothnessColor"
		// steepness=-19_-16
		List<RouteStatistics>  result = new ArrayList<>();
		for(String attributeName : attributeNames) {
			RouteStatisticComputer statisticComputer =
					new RouteStatisticComputer(currentRenderer, defaultRenderer, currentSearchRequest, defaultSearchRequest);
			RouteStatistics routeStatistics = statisticComputer.computeStatistic(routeSegmentWithInclines, attributeName);
			if (!routeStatistics.partition.isEmpty() && (routeStatistics.partition.size() != 1 || !routeStatistics.partition.containsKey(UNDEFINED_ATTR))) {
				result.add(routeStatistics);
			}
		}

		return result;
	}

	private static List<RouteSegmentWithIncline>  calculateInclineRouteSegments(List<RouteSegmentResult> route) {
		List<RouteSegmentWithIncline> input = new ArrayList<>();
		float prevHeight = 0;
		int totalArrayHeightsLength = 0;
		for(RouteSegmentResult r : route) {
			float[] heightValues = r.getHeightValues();
			RouteSegmentWithIncline incl = new RouteSegmentWithIncline();
			incl.dist = r.getDistance();
			incl.obj = r.getObject();
			input.add(incl);
			float prevH = prevHeight;
			int indStep = 0;
			if(incl.dist > H_STEP) {
				// for 10.1 meters 3 points (0, 5, 10)
				incl.interpolatedHeightByStep = new float[(int) ((incl.dist) / H_STEP) + 1];
				totalArrayHeightsLength += incl.interpolatedHeightByStep.length;
			}
			if(heightValues != null && heightValues.length > 0) {
				int indH = 2;
				float distCum = 0;
				prevH = heightValues[1];
				incl.h = prevH ;
				if(incl.interpolatedHeightByStep != null && incl.interpolatedHeightByStep.length > indStep) {
					incl.interpolatedHeightByStep[indStep++] = prevH;
				}
				while(incl.interpolatedHeightByStep != null && 
						indStep < incl.interpolatedHeightByStep.length && indH < heightValues.length) {
					float dist = heightValues[indH] + distCum;
					if(dist > indStep * H_STEP) {
						if(dist == distCum) {
							incl.interpolatedHeightByStep[indStep] = prevH;
						} else {
							incl.interpolatedHeightByStep[indStep] = (float) (prevH +
									(indStep * H_STEP - distCum) *
											(heightValues[indH + 1] - prevH) / (dist - distCum));
						}
						indStep++;
					} else {
						distCum = dist;
						prevH = heightValues[indH + 1];
						indH += 2;
					}
				}

			} else {
				incl.h = prevH;
			}
			while(incl.interpolatedHeightByStep != null && 
					indStep < incl.interpolatedHeightByStep.length) {
				incl.interpolatedHeightByStep[indStep++] = prevH;
			}
			prevHeight = prevH;
		}
		int slopeSmoothShift = (int) (H_SLOPE_APPROX / (2 * H_STEP));
		float[] heightArray = new float[totalArrayHeightsLength];
		int iter = 0;
		for(int i = 0; i < input.size(); i ++) {
			RouteSegmentWithIncline rswi = input.get(i);
			for(int k = 0; rswi.interpolatedHeightByStep != null &&
						k < rswi.interpolatedHeightByStep.length; k++) {
				heightArray[iter++] = rswi.interpolatedHeightByStep[k];
			}
		}
		iter = 0;
		int minSlope = Integer.MAX_VALUE;
		int maxSlope = Integer.MIN_VALUE;
		for(int i = 0; i < input.size(); i ++) {
			RouteSegmentWithIncline rswi = input.get(i);
			if(rswi.interpolatedHeightByStep != null) {
				rswi.slopeByStep = new float[rswi.interpolatedHeightByStep.length];
				for (int k = 0; k < rswi.slopeByStep.length; k++) {
					if (iter > slopeSmoothShift && iter + slopeSmoothShift < heightArray.length) {
						double slope = (heightArray[iter + slopeSmoothShift] - heightArray[iter - slopeSmoothShift]) * 100
								/ H_SLOPE_APPROX;
						rswi.slopeByStep[k] = (float) slope;
						minSlope = Math.min((int) slope, minSlope);
						maxSlope = Math.max((int) slope, maxSlope);
						
						
					}
					iter++;
				}
			}
		}
		String[] classFormattedStrings = new String[BOUNDARIES_ARRAY.length];
		classFormattedStrings[0] = formatSlopeString(minSlope, MIN_DIVIDED_INCLINE);
		classFormattedStrings[1] = formatSlopeString(minSlope, MIN_DIVIDED_INCLINE);
		classFormattedStrings[BOUNDARIES_ARRAY.length - 1] = formatSlopeString(MAX_DIVIDED_INCLINE, maxSlope);
		for (int k = 2; k < BOUNDARIES_ARRAY.length - 1; k++) {
			classFormattedStrings[k] = formatSlopeString(BOUNDARIES_ARRAY[k - 1], BOUNDARIES_ARRAY[k]);
		}
		for(int i = 0; i < input.size(); i ++) {
			RouteSegmentWithIncline rswi = input.get(i);
			if(rswi.slopeByStep != null) {
				rswi.slopeClass = new int[rswi.slopeByStep.length];
				rswi.slopeClassUserString = new String[rswi.slopeByStep.length];
				for (int t = 0; t < rswi.slopeClass.length; t++) {
					for (int k = 0; k < BOUNDARIES_ARRAY.length; k++) {
						if (rswi.slopeByStep[t] <= BOUNDARIES_ARRAY[k] || k == BOUNDARIES_ARRAY.length - 1) {
							rswi.slopeClass[t] = k;
							rswi.slopeClassUserString[t] = classFormattedStrings[k];
							break;
						}
					}
					// end of break
				}
			}
		}
		return input;
	}


	private static String formatSlopeString(int slope, int next) {
		return String.format("%d%% .. %d%%", slope, next);
	}


	private static class RouteStatisticComputer {

		final RenderingRulesStorage currentRenderer;
		final RenderingRulesStorage defaultRenderer;
		final RenderingRuleSearchRequest currentRenderingRuleSearchRequest;
		final RenderingRuleSearchRequest defaultRenderingRuleSearchRequest;

		RouteStatisticComputer(RenderingRulesStorage currentRenderer, RenderingRulesStorage defaultRenderer,
							   RenderingRuleSearchRequest currentRenderingRuleSearchRequest, RenderingRuleSearchRequest defaultRenderingRuleSearchRequest) {
			this.currentRenderer = currentRenderer;
			this.defaultRenderer = defaultRenderer;
			this.currentRenderingRuleSearchRequest = currentRenderingRuleSearchRequest;
			this.defaultRenderingRuleSearchRequest = defaultRenderingRuleSearchRequest;
		}


		public RouteStatistics computeStatistic(List<RouteSegmentWithIncline> route, String attribute) {
			List<RouteSegmentAttribute> routeAttributes = processRoute(route, attribute);
			Map<String, RouteSegmentAttribute> partition = makePartition(routeAttributes);
			float totalDistance = computeTotalDistance(routeAttributes);
			return new RouteStatistics(attribute, routeAttributes, partition, totalDistance);
		}

		Map<String, RouteSegmentAttribute> makePartition(List<RouteSegmentAttribute> routeAttributes) {
			final Map<String, RouteSegmentAttribute> partition = new TreeMap<>();
			for (RouteSegmentAttribute attribute : routeAttributes) {
				RouteSegmentAttribute attr = partition.get(attribute.getUserPropertyName());
				if (attr == null) {
					attr = new RouteSegmentAttribute(attribute);
					partition.put(attribute.getUserPropertyName(), attr);
				}
				attr.incrementDistanceBy(attribute.getDistance());
			}
			List<String> keys = new ArrayList<String>(partition.keySet());
			Collections.sort(keys, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					if (o1.equalsIgnoreCase(UNDEFINED_ATTR)) {
						return 1;
					}
					if (o2.equalsIgnoreCase(UNDEFINED_ATTR)) {
						return -1;
					}
					int cmp = Integer.compare(partition.get(o1).slopeIndex, partition.get(o2).slopeIndex);
					if(cmp != 0) {
						return cmp;
					}
					return -Float.compare(partition.get(o1).getDistance(), partition.get(o2).getDistance());
				}
			});
			Map<String, RouteSegmentAttribute> sorted = new LinkedHashMap<String, RouteStatisticsHelper.RouteSegmentAttribute>();
			for (String k : keys) {
				sorted.put(k, partition.get(k));
			}

			return sorted;
		}

		private float computeTotalDistance(List<RouteSegmentAttribute> attributes) {
			float distance = 0f;
			for (RouteSegmentAttribute attribute : attributes) {
				distance += attribute.getDistance();
			}
			return distance;
		}

		protected List<RouteSegmentAttribute> processRoute(List<RouteSegmentWithIncline> route, String attribute) {
			List<RouteSegmentAttribute> routes = new ArrayList<>();
			RouteSegmentAttribute prev = null;
			for (RouteSegmentWithIncline segment : route) {
				if(segment.slopeClass == null || segment.slopeClass.length == 0) {
					RouteSegmentAttribute current = classifySegment(attribute, -1, segment);
					current.distance = segment.dist;
					if (prev != null && prev.getPropertyName() != null &&
						prev.getPropertyName().equals(current.getPropertyName())) {
						prev.incrementDistanceBy(current.distance);
					} else {
						routes.add(current);
						prev = current;
					}
				} else {
					for(int i = 0; i < segment.slopeClass.length; i++) {
						float d = (float) (i == 0 ? (segment.dist - H_STEP * (segment.slopeClass.length - 1)) : H_STEP);
						if(i > 0 && segment.slopeClass[i] == segment.slopeClass[i-1]) {
							prev.incrementDistanceBy(d);
						} else {
							RouteSegmentAttribute current = classifySegment(attribute, 
									segment.slopeClass[i], segment);
							current.distance = d;
							if (prev != null && prev.getPropertyName() != null &&
								prev.getPropertyName().equals(current.getPropertyName())) {
								prev.incrementDistanceBy(current.distance);
							} else {
								if(current.slopeIndex == segment.slopeClass[i]) {
									current.setUserPropertyName(segment.slopeClassUserString[i]);
								}
								routes.add(current);
								prev = current;
							}
						}
					}
				}
			}
			return routes;
		}


		public RouteSegmentAttribute classifySegment(String attribute, int slopeClass, RouteSegmentWithIncline segment) {
			RouteSegmentAttribute res = new RouteSegmentAttribute(UNDEFINED_ATTR, 0, -1);
			RenderingRuleSearchRequest currentRequest = 
					currentRenderer == null ? null : new RenderingRuleSearchRequest(currentRenderingRuleSearchRequest);
			if (currentRenderer != null && searchRenderingAttribute(attribute, currentRenderer, currentRequest, segment, slopeClass)) {
				res = new RouteSegmentAttribute(currentRequest.getStringPropertyValue(currentRenderer.PROPS.R_ATTR_STRING_VALUE),
						currentRequest.getIntPropertyValue(currentRenderer.PROPS.R_ATTR_COLOR_VALUE), slopeClass);
			} else {
				RenderingRuleSearchRequest defaultRequest = new RenderingRuleSearchRequest(defaultRenderingRuleSearchRequest);
				if (searchRenderingAttribute(attribute, defaultRenderer, defaultRequest, segment, slopeClass)) {
					res = new RouteSegmentAttribute(
							defaultRequest.getStringPropertyValue(defaultRenderer.PROPS.R_ATTR_STRING_VALUE),
							defaultRequest.getIntPropertyValue(defaultRenderer.PROPS.R_ATTR_COLOR_VALUE), slopeClass);
				}
			}
			return res;
		}

		protected boolean searchRenderingAttribute(String attribute,
												   RenderingRulesStorage rrs, RenderingRuleSearchRequest req, RouteSegmentWithIncline segment,
												   int slopeClass) {
			//String additional = attrName + "=" + attribute;
			boolean mainTagAdded = false;
			StringBuilder additional = new StringBuilder(slopeClass >= 0 ? (BOUNDARIES_CLASS[slopeClass] + ";") : "");
			RouteDataObject obj = segment.obj;
			for (int type : obj.getTypes()) {
				BinaryMapRouteReaderAdapter.RouteTypeRule tp = obj.region.quickGetEncodingRule(type);
				if (tp.getTag().equals("highway") || tp.getTag().equals("route")
						|| tp.getTag().equals("railway") || tp.getTag().equals("aeroway")
						|| tp.getTag().equals("aerialway") || tp.getTag().equals("piste:type")) {
					if (!mainTagAdded) {
						req.setStringFilter(rrs.PROPS.R_TAG, tp.getTag());
						req.setStringFilter(rrs.PROPS.R_VALUE, tp.getValue());
						mainTagAdded = true;
					}
				} else {
					additional.append(tp.getTag()).append("=").append(tp.getValue()).append(";");
				}
			}
			req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional.toString());
			return req.searchRenderingAttribute(attribute);
		}
	}

	public static class RouteSegmentAttribute {

		private final int color;
		private final String propertyName;
		private final int slopeIndex;
		private float distance;
		private String userPropertyName;

		RouteSegmentAttribute(String propertyName, int color, int slopeIndex) {
			this.propertyName = propertyName == null ? UNDEFINED_ATTR : propertyName;
			this.slopeIndex = slopeIndex >= 0 && BOUNDARIES_CLASS[slopeIndex].endsWith(this.propertyName) ? slopeIndex : -1;
 			this.color = color;
		}

		RouteSegmentAttribute(RouteSegmentAttribute segmentAttribute) {
			this.propertyName = segmentAttribute.getPropertyName();
			this.color = segmentAttribute.getColor();
			this.slopeIndex = segmentAttribute.slopeIndex;
			this.userPropertyName = segmentAttribute.userPropertyName;
		}

		public String getUserPropertyName() {
			return userPropertyName == null ? propertyName : userPropertyName;
		}

		public void setUserPropertyName(String userPropertyName) {
			this.userPropertyName = userPropertyName;
		}

		public float getDistance() {
			return distance;
		}

		public void incrementDistanceBy(float distance) {
			this.distance += distance;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public int getColor() {
			return color;
		}

		@Override
		public String toString() {
			return String.format("%s - %.0f m %d", getUserPropertyName(), getDistance(), getColor());
		}
	}


}