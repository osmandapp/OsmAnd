package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RouteStatisticsHelper {

	public static final String UNDEFINED_ATTR = "undefined";
	private static final double H_STEP = 5;
	private static final double H_SLOPE_APPROX = 100;
	private static final int MIN_INCLINE = -100;
	private static final int MIN_DIVIDED_INCLINE = -20;
	private static final int MAX_INCLINE = 100;
	private static final int MAX_DIVIDED_INCLINE = 20;
	private static final int STEP = 4;
	private static final int NUM;
	private static final int[] BOUNDARIES_ARRAY;

	static {
		NUM = ((MAX_DIVIDED_INCLINE - MIN_DIVIDED_INCLINE) / STEP) + 3;
		BOUNDARIES_ARRAY = new int[NUM];
		BOUNDARIES_ARRAY[0] = MIN_INCLINE;
		for (int i = 1; i < NUM - 1; i++) {
			BOUNDARIES_ARRAY[i] = MIN_DIVIDED_INCLINE + (i - 1) * STEP;
		}
		BOUNDARIES_ARRAY[NUM - 1] = MAX_INCLINE;
	}



	public static class RouteStatistics {
		public final List<RouteSegmentAttribute> elements;
		public final Map<String, RouteSegmentAttribute> partition;
		public final float totalDistance;

		private RouteStatistics(List<RouteSegmentAttribute> elements,
								Map<String, RouteSegmentAttribute> partition,
								float totalDistance) {
			this.elements = elements;
			this.partition = partition;
			this.totalDistance = totalDistance;
		}
	}

	private static class RouteSegmentWithIncline {
		RouteDataObject obj;
		float dist;
		float h;
		float[] interpolatedHeightByStep;
		float[] slopeByStep;
	}

	public static List<RouteStatistics> calculateRouteStatistic(List<RouteSegmentResult> route, RenderingRulesStorage currentRenderer,
																RenderingRulesStorage defaultRenderer, RenderingRuleSearchRequest currentSearchRequest,
																RenderingRuleSearchRequest defaultSearchRequest) {
		List<RouteSegmentWithIncline> routeSegmentWithInclines = calculateInclineRouteSegments(route);
		List<String> attributeNames = new ArrayList<>();
		for (String s : currentRenderer.getRenderingAttributeNames()) {
			if (s.startsWith("routeInfo_")) {
				attributeNames.add(s);
			}
		}
		if(attributeNames.isEmpty()) {
			for (String s : defaultRenderer.getRenderingAttributeNames()) {
				if (s.startsWith("routeInfo_")) {
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
			result.add(statisticComputer.computeStatistic(routeSegmentWithInclines, attributeName));
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
				incl.interpolatedHeightByStep = new float[(int) (incl.dist / H_STEP) - 1];
				totalArrayHeightsLength += incl.interpolatedHeightByStep.length;
			}
			if(heightValues != null && heightValues.length > 0) {
				int indH = 2;
				float distCum = 0;
				prevH = heightValues[1];
				incl.h = prevH ;
				while(indStep < incl.interpolatedHeightByStep.length && indH < heightValues.length) {
					float dist = heightValues[indH] + distCum;
					if(dist > (indStep + 1) * H_STEP) {
						if(dist == distCum) {
							incl.interpolatedHeightByStep[indStep] = prevH;
						} else {
							incl.interpolatedHeightByStep[indStep] = (float) (prevH +
									((indStep + 1) * H_STEP - distCum) *
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
			while(indStep < incl.interpolatedHeightByStep.length) {
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
		for(int i = 0; i < input.size(); i ++) {
			RouteSegmentWithIncline rswi = input.get(i);
			if(rswi.interpolatedHeightByStep != null) {
				rswi.slopeByStep = new float[rswi.interpolatedHeightByStep.length];
				for (int k = 0; k < rswi.slopeByStep.length; k++) {
					if (iter > slopeSmoothShift && iter + slopeSmoothShift < heightArray.length) {
						rswi.slopeByStep[k] =
								(float) ((heightArray[iter + slopeSmoothShift] - heightArray[iter - slopeSmoothShift]) * 100
										/ H_SLOPE_APPROX);
					}
					iter++;
				}
			}
		}
		return input;
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
			return new RouteStatistics(routeAttributes, partition, totalDistance);
		}

		Map<String, RouteSegmentAttribute> makePartition(List<RouteSegmentAttribute> routeAttributes) {
			Map<String, RouteSegmentAttribute> partition = new TreeMap<>();
			for (RouteSegmentAttribute attribute : routeAttributes) {
				RouteSegmentAttribute attr = partition.get(attribute.propertyName);
				if (attr == null) {
					attr = new RouteSegmentAttribute(attribute);
					partition.put(attribute.propertyName, attr);
				}
				attr.incrementDistanceBy(attribute.getDistance());
			}
			return partition;
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
				RouteSegmentAttribute current = classifySegment(attribute, segment);
				if (prev != null && prev.getPropertyName() != null &&
						prev.getPropertyName().equals(current.getPropertyName())) {
					prev.incrementDistanceBy(current.distance);
				} else {
					routes.add(current);
					prev = current;
				}
			}
			return routes;
		}


		public RouteSegmentAttribute classifySegment(String attribute, RouteSegmentWithIncline segment) {
			RouteSegmentAttribute res = new RouteSegmentAttribute(UNDEFINED_ATTR, 0);
			RenderingRuleSearchRequest currentRequest = new RenderingRuleSearchRequest(currentRenderingRuleSearchRequest);
			if (searchRenderingAttribute(attribute, currentRenderer, currentRequest, segment)) {
				res = new RouteSegmentAttribute(currentRequest.getStringPropertyValue(currentRenderer.PROPS.R_ATTR_STRING_VALUE),
						currentRequest.getIntPropertyValue(currentRenderer.PROPS.R_ATTR_COLOR_VALUE));
			} else {
				RenderingRuleSearchRequest defaultRequest = new RenderingRuleSearchRequest(defaultRenderingRuleSearchRequest);
				if (searchRenderingAttribute(attribute, defaultRenderer, defaultRequest, segment)) {
					res = new RouteSegmentAttribute(defaultRequest.getStringPropertyValue(defaultRenderer.PROPS.R_ATTR_STRING_VALUE),
							defaultRequest.getIntPropertyValue(defaultRenderer.PROPS.R_ATTR_COLOR_VALUE));
				}
			}
			res.distance = segment.dist;
			return res;
		}

		protected boolean searchRenderingAttribute(String attribute,
												   RenderingRulesStorage rrs, RenderingRuleSearchRequest req, RouteSegmentWithIncline segment) {
			//String additional = attrName + "=" + attribute;
			RouteDataObject obj = segment.obj;
			int[] tps = obj.getTypes();
			String additional = "";
			for (int k = 0; k < tps.length; k++) {
				BinaryMapRouteReaderAdapter.RouteTypeRule tp = obj.region.quickGetEncodingRule(tps[k]);
				if (tp.getTag().equals("highway") || tp.getTag().equals("route") ||
						tp.getTag().equals("railway") || tp.getTag().equals("aeroway") || tp.getTag().equals("aerialway")) {
					req.setStringFilter(rrs.PROPS.R_TAG, tp.getTag());
					req.setStringFilter(rrs.PROPS.R_TAG, tp.getValue());
				} else {
					if (additional.length() > 0) {
						additional += ";";
					}
					additional += tp.getTag() + "=" + tp.getValue();
				}
			}
			req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional);
			return req.searchRenderingAttribute(attribute);

		}
	}
	public static class RouteSegmentAttribute {

		private final int color;
		private final String propertyName;
		private float distance;

		RouteSegmentAttribute(String propertyName, int color) {
			this.propertyName = propertyName;
			this.color = color;
		}

		RouteSegmentAttribute(RouteSegmentAttribute segmentAttribute) {
			this.propertyName = segmentAttribute.getPropertyName();
			this.color = segmentAttribute.getColor();
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
			return "RouteSegmentAttribute{" +
					"propertyName='" + propertyName + '\'' +
					", color='" + color + '\'' +
					", distance=" + distance +
					'}';
		}
	}


}