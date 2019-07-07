package net.osmand.router;

import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class RouteStatisticsHelper {

	public static final String UNDEFINED_ATTR = "undefined";

	public static List<RouteStatistics> calculateRouteStatistic(List<RouteSegmentResult> route, RenderingRulesStorage currentRenderer,
																RenderingRulesStorage defaultRenderer, RenderingRuleSearchRequest currentSearchRequest,
																RenderingRuleSearchRequest defaultSearchRequest) {

		List<RouteStatistics> result = new ArrayList<>();

		String[] attrNames = { "surface", "highway", "smoothness" };
		String[] colorAttrNames = { "surfaceColor", "roadClassColor", "smoothnessColor" };

		for (int i = 0; i < attrNames.length; i++) {
			String attrName = attrNames[i];
			String colorAttrName = colorAttrNames[i];
			RouteStatisticComputer statisticComputer = new RoutePlainStatisticComputer(route, attrName, colorAttrName, currentRenderer, defaultRenderer, currentSearchRequest, defaultSearchRequest);
			result.add(statisticComputer.computeStatistic());
		}

		return result;
	}

	private abstract static class RouteStatisticComputer<E extends Comparable<E>> {

		final List<RouteSegmentResult> route;
		final String attrName;
		final String colorAttrName;
		final RenderingRulesStorage currentRenderer;
		final RenderingRulesStorage defaultRenderer;
		final RenderingRuleSearchRequest currentRenderingRuleSearchRequest;
		final RenderingRuleSearchRequest defaultRenderingRuleSearchRequest;

		RouteStatisticComputer(List<RouteSegmentResult> route, String attrName, String colorAttrName,
							   RenderingRulesStorage currentRenderer, RenderingRulesStorage defaultRenderer,
							   RenderingRuleSearchRequest currentRenderingRuleSearchRequest, RenderingRuleSearchRequest defaultRenderingRuleSearchRequest) {
			this.route = route;
			this.attrName = attrName;
			this.colorAttrName = colorAttrName;
			this.currentRenderer = currentRenderer;
			this.defaultRenderer = defaultRenderer;
			this.currentRenderingRuleSearchRequest = currentRenderingRuleSearchRequest;
			this.defaultRenderingRuleSearchRequest = defaultRenderingRuleSearchRequest;
		}

		Map<E, RouteSegmentAttribute<E>> makePartition(List<RouteSegmentAttribute<E>> routeAttributes) {
			Map<E, RouteSegmentAttribute<E>> partition = new TreeMap<>();
			for (RouteSegmentAttribute<E> attribute : routeAttributes) {
				E key = attribute.getAttribute();
				RouteSegmentAttribute<E> pattr = partition.get(key);
				if (pattr == null) {
					pattr = new RouteSegmentAttribute<>(attribute);
					partition.put(key, pattr);
				}
				pattr.incrementDistanceBy(attribute.getDistance());
			}
			return partition;
		}

		private float computeTotalDistance(List<RouteSegmentAttribute<E>> attributes) {
			float distance = 0f;
			for (RouteSegmentAttribute attribute : attributes) {
				distance += attribute.getDistance();
			}
			return distance;
		}

		protected List<RouteSegmentAttribute<E>> processRoute() {
			int index = 0;
			List<RouteSegmentAttribute<E>> routes = new ArrayList<>();
			E prev = null;
			for (RouteSegmentResult segment : route) {
				E current = getAttribute(segment);
				if (prev != null && !prev.equals(current)) {
					index++;
				}
				if (index >= routes.size()) {
					int color = getColor(current);
					String propertyName = getPropertyName(current);
					routes.add(new RouteSegmentAttribute<>(index, current, propertyName, color));
				}
				RouteSegmentAttribute surface = routes.get(index);
				surface.incrementDistanceBy(segment.getDistance());
				prev = current;
			}
			return routes;
		}

		RouteStatistics<E> computeStatistic() {
			List<RouteSegmentAttribute<E>> routeAttributes = processRoute();
			Map<E, RouteSegmentAttribute<E>> partition = makePartition(routeAttributes);
			float totalDistance = computeTotalDistance(routeAttributes);
			return new RouteStatistics<>(routeAttributes, partition, totalDistance);
		}

		RenderingRuleSearchRequest getSearchRequest(boolean useCurrentRenderer) {
			return new RenderingRuleSearchRequest(useCurrentRenderer ? currentRenderingRuleSearchRequest : defaultRenderingRuleSearchRequest);
		}

		public int getColor(E attribute) {
			int color = 0;
			RenderingRuleSearchRequest currentRequest = getSearchRequest(true);
			if (searchRenderingAttribute(currentRenderer, currentRequest, attribute)) {
				color = currentRequest.getIntPropertyValue(currentRenderer.PROPS.R_ATTR_COLOR_VALUE);
			} else {
				RenderingRuleSearchRequest defaultRequest = getSearchRequest(false);
				if (searchRenderingAttribute(defaultRenderer, defaultRequest, attribute)) {
					color = defaultRequest.getIntPropertyValue(defaultRenderer.PROPS.R_ATTR_COLOR_VALUE);
				}
			}
			return color;
		}

		public abstract E getAttribute(RouteSegmentResult segment);

		public abstract String getPropertyName(E attribute);

		protected abstract boolean searchRenderingAttribute(RenderingRulesStorage rrs, RenderingRuleSearchRequest req, E attribute);
	}

	private static class RoutePlainStatisticComputer extends RouteStatisticComputer<String> {

		public RoutePlainStatisticComputer(List<RouteSegmentResult> route, String attrName, String colorAttrName,
										   RenderingRulesStorage currentRenderer, RenderingRulesStorage defaultRenderer,
										   RenderingRuleSearchRequest currentSearchRequest, RenderingRuleSearchRequest defaultSearchRequest) {
			super(route, attrName, colorAttrName, currentRenderer, defaultRenderer, currentSearchRequest, defaultSearchRequest);
		}

		@Override
		public String getAttribute(RouteSegmentResult segment) {
			String attribute = segment.getObjectAttribute(attrName);
			if (attribute == null) {
				return UNDEFINED_ATTR;
			}
			String type = getAttributeType(attribute);
			return type != null ? type : UNDEFINED_ATTR;
		}

		private String getAttributeType(String attribute) {
			String type = null;
			RenderingRuleSearchRequest currentRequest = getSearchRequest(true);
			if (searchRenderingAttribute(currentRenderer, currentRequest, attribute)) {
				type = currentRequest.getStringPropertyValue(currentRenderer.PROPS.R_ATTR_STRING_VALUE);
				if (currentRequest.searchRenderingAttribute(type)) {
					type = currentRequest.getStringPropertyValue(currentRenderer.PROPS.R_ATTR_STRING_VALUE);
				}
			} else {
				RenderingRuleSearchRequest defaultRequest = getSearchRequest(false);
				if (searchRenderingAttribute(defaultRenderer, defaultRequest, attribute)) {
					type = defaultRequest.getStringPropertyValue(currentRenderer.PROPS.R_ATTR_STRING_VALUE);
					if (defaultRequest.searchRenderingAttribute(type)) {
						type = defaultRequest.getStringPropertyValue(currentRenderer.PROPS.R_ATTR_STRING_VALUE);
					}
				}
			}
			return type == null ? UNDEFINED_ATTR : type;
		}

		@Override
		public String getPropertyName(String attribute) {
			String type = getAttributeType(attribute);
			return type != null ? type : attribute;
		}

		@Override
		protected boolean searchRenderingAttribute(RenderingRulesStorage rrs, RenderingRuleSearchRequest req, String attribute) {
			String additional = attrName + "=" + attribute;
			req.setStringFilter(rrs.PROPS.R_ATTR_STRING_VALUE, attrName + "_" + attribute);
			req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional);
			boolean result = req.searchRenderingAttribute(colorAttrName);
			/*
			if (!result) {
				req.clearState();
				req.setStringFilter(rrs.PROPS.R_TAG, attrName);
				req.setStringFilter(rrs.PROPS.R_VALUE, attribute);
				result = req.searchRenderingAttribute(colorAttrName);
			}
			*/
			return result;
		}
	}

	private static class RouteBoundariesStatisticComputer extends RouteStatisticComputer<Boundaries> {

		private final List<Incline> inclines;

		public RouteBoundariesStatisticComputer(List<Incline> inclines, String attrName, String colorAttrName,
												RenderingRulesStorage currentRenderer, RenderingRulesStorage defaultRenderer,
												RenderingRuleSearchRequest currentSearchRequest, RenderingRuleSearchRequest defaultSearchRequest) {
			super(null, attrName, colorAttrName, currentRenderer, defaultRenderer, currentSearchRequest, defaultSearchRequest);
			this.inclines = inclines;
		}

		@Override
		public List<RouteSegmentAttribute<Boundaries>> processRoute() {
			List<RouteSegmentAttribute<Boundaries>> routeInclines = new ArrayList<>();
			int index = 0;
			Boundaries prev = null;
			Incline prevIncline = null;
			for (Incline incline : inclines) {
				Boundaries current = incline.getBoundaries();
				if (prev != null && !prev.equals(current)) {
					index++;
				}
				if (index >= routeInclines.size()) {
					String propertyName = getPropertyName(current);
					int color = getColor(current);
					RouteSegmentAttribute<Boundaries> attribute = new RouteSegmentAttribute<>(index, current, propertyName, color);
					if (prevIncline != null) {
						attribute.setInitDistance(prevIncline.getDistance());
					}
					routeInclines.add(attribute);
				}
				RouteSegmentAttribute routeIncline = routeInclines.get(index);
				routeIncline.relativeSum(incline.getDistance());
				prev = current;
				prevIncline = incline;
			}
			return routeInclines;
		}

		@Override
		public Boundaries getAttribute(RouteSegmentResult segment) {
			return null;
		}

		@Override
		public String getPropertyName(Boundaries attribute) {
			int lowerBoundary = Math.round(attribute.getLowerBoundary());
			int upperBoundary = Math.round(attribute.getUpperBoundary());
			if (lowerBoundary >= Boundaries.MIN_DIVIDED_INCLINE) {
				lowerBoundary++;
			}
			return String.format(Locale.US, "%d%% ... %d%%", lowerBoundary, upperBoundary);
		}

		@Override
		public boolean searchRenderingAttribute(RenderingRulesStorage rrs, RenderingRuleSearchRequest req, Boundaries attribute) {
			int lowerBoundary = Math.round(attribute.getLowerBoundary());
			int upperBoundary = Math.round(attribute.getUpperBoundary());
			StringBuilder range = new StringBuilder();
			if (lowerBoundary >= Boundaries.MIN_DIVIDED_INCLINE) {
				lowerBoundary++;
			} else {
				lowerBoundary = Boundaries.MIN_INCLINE;
			}
			if (upperBoundary > Boundaries.MAX_DIVIDED_INCLINE) {
				upperBoundary = Boundaries.MAX_INCLINE;
			}
			range.append(lowerBoundary);
			range.append(upperBoundary < 0 ? "_" : "-");
			range.append(upperBoundary);
			String additional = attrName + "=" + range;
			req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional);
			return req.searchRenderingAttribute(colorAttrName);
		}
	}

	public static class RouteSegmentAttribute<E> {

		private final int index;
		private final E attribute;
		private final int color;
		private final String propertyName;

		private float distance;
		private float initDistance;

		RouteSegmentAttribute(int index, E attribute, String propertyName, int color) {
			this.index = index;
			this.attribute = attribute;
			this.propertyName = propertyName;
			this.color = color;
		}

		RouteSegmentAttribute(RouteSegmentAttribute<E> segmentAttribute) {
			this.index = segmentAttribute.getIndex();
			this.attribute = segmentAttribute.getAttribute();
			this.propertyName = segmentAttribute.getPropertyName();
			this.color = segmentAttribute.getColor();
		}

		public int getIndex() {
			return index;
		}

		public E getAttribute() {
			return attribute;
		}

		public float getDistance() {
			return distance;
		}

		public void setInitDistance(float initDistance) {
			this.initDistance = initDistance;
		}

		public void incrementDistanceBy(float distance) {
			this.distance += distance;
		}

		public void relativeSum(float distance) {
			this.distance = this.distance + ((distance - this.initDistance) - this.distance);
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
					"index=" + index +
					", attribute='" + attribute + '\'' +
					", color='" + color + '\'' +
					", distance=" + distance +
					'}';
		}
	}

	public static class Incline {

		private final float inclineValue;
		private final float distance;
		private Boundaries boundaries;

		public Incline(float inclineValue, float distance) {
			this.inclineValue = inclineValue;
			this.distance = distance;
		}

		public void computeBoundaries(float minIncline, float maxIncline) {
			this.boundaries = Boundaries.newBoundariesFor(inclineValue, minIncline, maxIncline);
		}

		public float getValue() {
			return inclineValue;
		}

		public float getDistance() {
			return distance;
		}

		public Boundaries getBoundaries() {
			return this.boundaries;
		}

		@Override
		public String toString() {
			return "Incline{" +
					", incline=" + inclineValue +
					", distance=" + distance +
					'}';
		}
	}

	public static class Boundaries implements Comparable<Boundaries> {

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

		private final float upperBoundary;
		private final float lowerBoundary;

		private Boundaries(float upperBoundary, float lowerBoundary) {
			this.upperBoundary = upperBoundary;
			this.lowerBoundary = lowerBoundary;
		}

		public static Boundaries newBoundariesFor(float incline, float minIncline, float maxIncline) {
			int maxRoundedIncline = Math.round(maxIncline);
			int minRoundedIncline = Math.round(minIncline);
			if (incline > MAX_INCLINE) {
				return new Boundaries(MAX_INCLINE, MAX_DIVIDED_INCLINE);
			}
			if (incline < MIN_INCLINE) {
				return new Boundaries(MIN_DIVIDED_INCLINE, MIN_INCLINE);
			}
			for (int i = 1; i < NUM; i++) {
				if (incline >= BOUNDARIES_ARRAY[i - 1] && incline < BOUNDARIES_ARRAY[i]) {
					if (i == 1) {
						return new Boundaries(BOUNDARIES_ARRAY[i], minRoundedIncline);
					} else if (i == NUM - 1) {
						return new Boundaries(maxRoundedIncline, BOUNDARIES_ARRAY[i - 1]);
					} else {
						return new Boundaries(BOUNDARIES_ARRAY[i], BOUNDARIES_ARRAY[i - 1]);
					}
				}
			}
			return null;
		}

		public float getUpperBoundary() {
			return upperBoundary;
		}

		public float getLowerBoundary() {
			return lowerBoundary;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Boundaries that = (Boundaries) o;

			if (Float.compare(that.upperBoundary, upperBoundary) != 0) return false;
			return Float.compare(that.lowerBoundary, lowerBoundary) == 0;
		}

		@Override
		public int hashCode() {
			int result = (upperBoundary != +0.0f ? Float.floatToIntBits(upperBoundary) : 0);
			result = 31 * result + (lowerBoundary != +0.0f ? Float.floatToIntBits(lowerBoundary) : 0);
			return result;
		}

		@Override
		public int compareTo(Boundaries boundaries) {
			return (int) (getLowerBoundary() - boundaries.getLowerBoundary());
		}

		@Override
		public String toString() {
			return String.format(Locale.US, "%d%% ... %d%%", Math.round(getLowerBoundary()), Math.round(getUpperBoundary()));
		}
	}

	public static class RouteStatistics<E> {

		private final List<RouteSegmentAttribute<E>> elements;
		private final Map<E, RouteSegmentAttribute<E>> partition;
		private final float totalDistance;

		private RouteStatistics(List<RouteSegmentAttribute<E>> elements,
								Map<E, RouteSegmentAttribute<E>> partition,
								float totalDistance) {
			this.elements = elements;
			this.partition = partition;
			this.totalDistance = totalDistance;
		}

		public float getTotalDistance() {
			return totalDistance;
		}

		public List<RouteSegmentAttribute<E>> getElements() {
			return elements;
		}

		public Map<E, RouteSegmentAttribute<E>> getPartition() {
			return partition;
		}
	}
}