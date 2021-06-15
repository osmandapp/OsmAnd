package net.osmand.router;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.lang3.tuple.Pair;

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
		int startPointIdx;
		int endPointIdx;
		boolean idxShifted = false;
		float dist;
		float h;
		float[] interpolatedHeightByStep;
		float[] slopeByStep;
		String[] slopeClassUserString;
		int[] slopeClass;
	}

	public static List<RouteStatistics> calculateRouteStatistic(List<RouteSegmentResult> route,
	                                                            RenderingRulesStorage currentRenderer,
	                                                            RenderingRulesStorage defaultRenderer,
	                                                            RenderingRuleSearchRequest currentSearchRequest,
	                                                            RenderingRuleSearchRequest defaultSearchRequest) {
		return calculateRouteStatistic(route, null, currentRenderer, defaultRenderer,
				currentSearchRequest, defaultSearchRequest);
	}

	public static List<RouteStatistics> calculateRouteStatistic(List<RouteSegmentResult> route,
	                                                            List<String> attributesNames,
	                                                            RenderingRulesStorage currentRenderer,
	                                                            RenderingRulesStorage defaultRenderer,
	                                                            RenderingRuleSearchRequest currentSearchRequest,
	                                                            RenderingRuleSearchRequest defaultSearchRequest) {
		if (route == null) {
			return Collections.emptyList();
		}
		List<RouteSegmentWithIncline> routeSegmentWithInclines = calculateInclineRouteSegments(route);
		// "steepnessColor", "surfaceColor", "roadClassColor", "smoothnessColor"
		// steepness=-19_-16
		List<RouteStatistics>  result = new ArrayList<>();
		if (Algorithms.isEmpty(attributesNames)) {
			attributesNames = getRouteStatisticAttrsNames(currentRenderer, defaultRenderer);
		}
		for (String attr : attributesNames) {
			RouteStatisticComputer statisticComputer =
					new RouteStatisticComputer(currentRenderer, defaultRenderer, currentSearchRequest, defaultSearchRequest);
			RouteStatistics routeStatistics = statisticComputer.computeStatistic(routeSegmentWithInclines, attr);
			Map<String, RouteSegmentAttribute> partitions = routeStatistics.partition;
			if (!partitions.isEmpty() && (partitions.size() != 1 || !routeStatistics.partition.containsKey(UNDEFINED_ATTR))) {
				result.add(routeStatistics);
			}
		}
		return result;
	}

	public static List<String> getRouteStatisticAttrsNames(RenderingRulesStorage currentRenderer,
														   RenderingRulesStorage defaultRenderer) {
		List<String> attributeNames = new ArrayList<>();
		if (currentRenderer != null) {
			for (String s : currentRenderer.getRenderingAttributeNames()) {
				if (s.startsWith(ROUTE_INFO_PREFIX)) {
					attributeNames.add(s);
				}
			}
		}
		if (attributeNames.isEmpty()) {
			for (String s : defaultRenderer.getRenderingAttributeNames()) {
				if (s.startsWith(ROUTE_INFO_PREFIX)) {
					attributeNames.add(s);
				}
			}
		}
		return attributeNames;
	}

	private static List<RouteSegmentWithIncline> calculateInclineRouteSegments(List<RouteSegmentResult> route) {
		List<RouteSegmentWithIncline> input = new ArrayList<>();
		float prevHeight = 0;
		int totalArrayHeightsLength = 0;
		for (int segmentIdx = 0; segmentIdx < route.size(); segmentIdx++) {
			RouteSegmentResult r = route.get(segmentIdx);

			float[] heightValues = r.getHeightValues();
			RouteSegmentWithIncline incl = new RouteSegmentWithIncline();
			incl.obj = r.getObject();
			incl.startPointIdx = getStartEndPointIndex(route, segmentIdx, true);
			incl.endPointIdx = r.getEndPointIndex();
			incl.idxShifted = incl.startPointIdx != r.getStartPointIndex()
					|| incl.endPointIdx != getStartEndPointIndex(route, segmentIdx, false);
			incl.dist = r.getDistance();
			input.add(incl);
			float prevH = prevHeight;
			int indStep = 0;

			if (incl.dist > H_STEP) {
				// for 10.1 meters 3 points (0, 5, 10)
				incl.interpolatedHeightByStep = new float[(int) ((incl.dist) / H_STEP) + 1];
				totalArrayHeightsLength += incl.interpolatedHeightByStep.length;
			}
			if (heightValues != null && heightValues.length > 0) {
				int indH = 2;
				float distCum = 0;
				prevH = heightValues[1];
				incl.h = prevH ;
				if(incl.interpolatedHeightByStep != null && incl.interpolatedHeightByStep.length > indStep) {
					incl.interpolatedHeightByStep[indStep++] = prevH;
				}
				while (incl.interpolatedHeightByStep != null &&
						indStep < incl.interpolatedHeightByStep.length && indH < heightValues.length) {
					float dist = heightValues[indH] + distCum;
					if (dist > indStep * H_STEP) {
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
			while (incl.interpolatedHeightByStep != null &&
					indStep < incl.interpolatedHeightByStep.length) {
				incl.interpolatedHeightByStep[indStep++] = prevH;
			}
			prevHeight = prevH;
		}

		int slopeSmoothShift = (int) (H_SLOPE_APPROX / (2 * H_STEP));
		float[] heightArray = new float[totalArrayHeightsLength];
		int iter = 0;
		for (int i = 0; i < input.size(); i ++) {
			RouteSegmentWithIncline rswi = input.get(i);
			for(int k = 0; rswi.interpolatedHeightByStep != null &&
						k < rswi.interpolatedHeightByStep.length; k++) {
				heightArray[iter++] = rswi.interpolatedHeightByStep[k];
			}
		}

		iter = 0;
		int minSlope = Integer.MAX_VALUE;
		int maxSlope = Integer.MIN_VALUE;
		for (int i = 0; i < input.size(); i ++) {
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

		for (int i = 0; i < input.size(); i ++) {
			RouteSegmentWithIncline rswi = input.get(i);
			if (rswi.slopeByStep != null) {
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

	private static int getStartEndPointIndex(List<RouteSegmentResult> segments, int idx, boolean start) {
		RouteSegmentResult segment = segments.get(idx);
		int startIdx = segment.getStartPointIndex();
		int endIdx = segment.getEndPointIndex();
		boolean first = idx == 0;
		boolean last = idx + 1 == segments.size();
		boolean firstOrLast = first || last;
		if (!firstOrLast || first && !start || last && start) {
			return start ? startIdx : endIdx;
		}
		float initialDist = segment.getDistance();
		if (Math.floor(initialDist) == Math.floor(getSegmentDistFromPoints(segment))) {
			return start ? startIdx : endIdx;
		} else {
			int inc = startIdx < endIdx ? 1 : -1;
			return start ? startIdx - inc : endIdx + inc;
		}
	}

	private static double getSegmentDistFromPoints(RouteSegmentResult segment) {
		double dist = 0;
		int startIdx = segment.getStartPointIndex();
		int endIdx = segment.getEndPointIndex();
		int next;
		int inc = startIdx < endIdx ? 1 : -1;
		for (int i = startIdx; i != endIdx; i = next) {
			next = i + inc;
			dist += MapUtils.getDistance(segment.getPoint(i), segment.getPoint(next));
		}
		return dist;
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

			for (int segmentIdx = 0; segmentIdx < route.size(); segmentIdx++) {
				RouteSegmentWithIncline segment = route.get(segmentIdx);

				if (segment.slopeClass == null || segment.slopeClass.length == 0) {
					RouteSegmentAttribute current = classifySegment(attribute, -1, segment);
					if (equalPropertyNames(prev, current)) {
						prev.incrementDistanceBy(segment.dist);
					} else {
						current.distance = segment.dist;
						routes.add(current);
						prev = current;
					}
				} else {
					prev = processSegmentWithIncline(prev, route, segmentIdx, attribute, routes);
				}
			}
			return routes;
		}

		private RouteSegmentAttribute processSegmentWithIncline(RouteSegmentAttribute prev,
																List<RouteSegmentWithIncline> segments,
		                                                        int segmentIdx,
																String attribute,
																List<RouteSegmentAttribute> routes) {
			RouteSegmentWithIncline segment = segments.get(segmentIdx);
			float dist = 0;

			for (int i = 0; i < segment.slopeClass.length; i++) {
				float d = (float) (i == 0 ? (segment.dist - H_STEP * (segment.slopeClass.length - 1)) : H_STEP);
				int currSlopeClass = segment.slopeClass[i];

				if (i > 0 && currSlopeClass == segment.slopeClass[i - 1]) {
					prev.incrementDistanceBy(d);
					dist += d;
					setStartEndLocationIfNeeded(prev, segments, segmentIdx, dist, true);
					if (i + 1 == segment.slopeClass.length) {
						setStartEndLocationIfNeeded(prev, segments, segmentIdx, dist, false);
					}
					continue;
				}

				RouteSegmentAttribute current = classifySegment(attribute, currSlopeClass, segment);
				current.distance = d;
				if (equalPropertyNames(prev, current)) {
					prev.incrementDistanceBy(current.distance);
					dist += d;
					setStartEndLocationIfNeeded(prev, segments, segmentIdx, dist, true);
					continue;
				}

				if (current.slopeIndex == currSlopeClass) {
					current.setUserPropertyName(segment.slopeClassUserString[i]);
				}
				dist = d;
				if (prev != null) {
					setStartEndLocationIfNeeded(prev, segments, segmentIdx, dist, false);
				}
				setStartEndLocationIfNeeded(current, segments, segmentIdx, dist, true);
				routes.add(current);
				prev = current;

			}

			return prev;
		}

		private void setStartEndLocationIfNeeded(RouteSegmentAttribute attr, List<RouteSegmentWithIncline> segments,
		                                         int startIdx, float dist, boolean start) {
			if (start && attr.getStartLocation() != null) {
				return;
			}
			Pair<Location, Integer> locationInfo = computeLocationAndIndex(segments, startIdx, dist);
			if (locationInfo != null) {
				attr.setStartEndLocationInfo(locationInfo, start);
			}
		}

		private Pair<Location, Integer> computeLocationAndIndex(List<RouteSegmentWithIncline> segments,
		                                                        int startSegmentIdx,
																float processedRouteSegmentDist) {
			RouteSegmentWithIncline segment = segments.get(startSegmentIdx);
			int next;
			int inc = segment.startPointIdx < segment.endPointIdx ? 1 : -1;
			int idx = segment.startPointIdx;
			RouteDataObject obj = segment.obj;
			if (segment.idxShifted && startSegmentIdx + 1 == segments.size()) {
				int end = segment.endPointIdx;
				processedRouteSegmentDist -= (float) MapUtils.measuredDist31(obj.getPoint31XTile(end),
						obj.getPoint31YTile(end), obj.getPoint31XTile(end + inc), obj.getPoint31YTile(end + inc));
			}
			float dist = processedRouteSegmentDist;

			for (int i = segment.startPointIdx; i != segment.endPointIdx; i = next) {
				idx = i;
				next = i + inc;
				dist = (float) MapUtils.measuredDist31(obj.getPoint31XTile(i), obj.getPoint31YTile(i),
						obj.getPoint31XTile(next), obj.getPoint31YTile(next));
				if (processedRouteSegmentDist - dist <= 0) {
					break;
				}
				processedRouteSegmentDist -= dist;
			}

			boolean first = startSegmentIdx == 0;
			if (segment.idxShifted && first && idx == segment.startPointIdx) {
					return null;
			}

			double ratio = processedRouteSegmentDist / dist;
			double prevLat = MapUtils.get31LatitudeY(obj.getPoint31YTile(idx));
			double prevLon = MapUtils.get31LongitudeX(obj.getPoint31XTile(idx));
			double nextLat = MapUtils.get31LatitudeY(obj.getPoint31YTile(idx + inc));
			double nextLon = MapUtils.get31LongitudeX(obj.getPoint31XTile(idx + inc));
			double lat = prevLat + ratio * (nextLat - prevLat);
			double lon = prevLon + ratio * (nextLon - prevLon);
			Location location = new Location("", lat, lon);

			int actualStartIdx = first && segment.idxShifted ? segment.startPointIdx + inc : segment.startPointIdx;
			int locationIdx = getLocationIdx(segments, startSegmentIdx, Math.abs(actualStartIdx - idx));
			return Pair.of(location, locationIdx);
		}

		private int getLocationIdx(List<RouteSegmentWithIncline> segments, int currSegmentIdx, int pointOrder) {
			int locationsCount = 0;
			for (int segmentIdx = 0; segmentIdx < currSegmentIdx; segmentIdx++) {
				RouteSegmentWithIncline segment = segments.get(segmentIdx);
				locationsCount += Math.abs(segment.startPointIdx - segment.endPointIdx);
			}
			return locationsCount + pointOrder;
		}

		private boolean equalPropertyNames(RouteSegmentAttribute left, RouteSegmentAttribute right) {
			return left != null && left.propertyName != null && right != null
					&& left.propertyName.equals(right.propertyName);
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

		private Location startLocation;
		private Location endLocation;
		private int startLocationIdx;
		private int endLocationIdx;

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

		private void setStartEndLocationInfo(Pair<Location, Integer> pair, boolean start) {
			if (pair == null) {
				return;
			}
			if (start) {
				startLocation = pair.getLeft();
				startLocationIdx = pair.getRight();
			} else {
				endLocation = pair.getLeft();
				endLocationIdx = pair.getRight();
			}
		}

		public Location getStartLocation() {
			return startLocation;
		}

		public Location getEndLocation() {
			return endLocation;
		}

		public int getStartLocationIdx() {
			return startLocationIdx;
		}

		public int getEndLocationIdx() {
			return endLocationIdx;
		}

		@Override
		public String toString() {
			return String.format("%s - %.0f m %d", getUserPropertyName(), getDistance(), getColor());
		}
	}
}