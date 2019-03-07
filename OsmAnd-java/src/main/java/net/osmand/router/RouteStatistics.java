package net.osmand.router;

import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RouteStatistics {

	private static final String UNDEFINED_ATTR = "undefined";

	private final List<RouteSegmentResult> route;
	private final RenderingRulesStorage rrs;

	private final boolean nightMode;

	private RouteStatistics(List<RouteSegmentResult> route, RenderingRulesStorage rrs, boolean nightMode) {
		this.route = route;
		this.rrs = rrs;
		this.nightMode = nightMode;
	}

	public static RouteStatistics newRouteStatistic(List<RouteSegmentResult> route, RenderingRulesStorage rrs, boolean nightMode) {
		return new RouteStatistics(route, rrs, nightMode);
	}

	public Statistics getRouteSurfaceStatistic() {
		RouteStatisticComputer statisticComputer = new RouteSurfaceStatisticComputer(route, rrs, nightMode);
		return statisticComputer.computeStatistic();
	}

	public Statistics getRouteSmoothnessStatistic() {
		RouteStatisticComputer statisticComputer = new RouteSmoothnessStatisticComputer(route, rrs, nightMode);
		return statisticComputer.computeStatistic();
	}

	public Statistics getRouteClassStatistic() {
		RouteStatisticComputer statisticComputer = new RouteClassStatisticComputer(route, rrs, nightMode);
		return statisticComputer.computeStatistic();
	}

	public Statistics getRouteSteepnessStatistic(List<Incline> inclines) {
		RouteStatisticComputer statisticComputer = new RouteSteepnessStatisticComputer(inclines, rrs, nightMode);
		return statisticComputer.computeStatistic();
	}


	private abstract static class RouteStatisticComputer<E extends Comparable<E>> {

		private final List<RouteSegmentResult> route;
		private final StatisticType type;

		protected final RenderingRulesStorage rrs;
		protected final boolean nightMode;

		public RouteStatisticComputer(RenderingRulesStorage rrs, List<RouteSegmentResult> route, StatisticType type, boolean nightMode) {
			this.route = route;
			this.rrs = rrs;
			this.type = type;
			this.nightMode = nightMode;
		}

		protected Map<E, RouteSegmentAttribute<E>> makePartition(List<RouteSegmentAttribute<E>> routeAttributes) {
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

		protected List<RouteSegmentResult> getRoute() {
			return route;
		}

		protected List<RouteSegmentAttribute<E>> processRoute() {
			int index = 0;
			List<RouteSegmentAttribute<E>> routes = new ArrayList<>();
			E prev = null;
			for (RouteSegmentResult segment : getRoute()) {
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

		public Statistics<E> computeStatistic() {
			List<RouteSegmentAttribute<E>> routeAttributes = processRoute();
			Map<E, RouteSegmentAttribute<E>> partition = makePartition(routeAttributes);
			float totalDistance = computeTotalDistance(routeAttributes);
			return new Statistics<>(routeAttributes, partition, totalDistance, type);
		}

		public abstract E getAttribute(RouteSegmentResult segment);

		public abstract int getColor(E attribute);

		public abstract String getPropertyName(E attribute);
	}

	private static class RouteSurfaceStatisticComputer extends RouteStatisticComputer<String> {

		private static final String SURFACE_ATTR = "surface";
		private static final String SURFACE_COLOR_ATTR = "surfaceColor";

		public RouteSurfaceStatisticComputer(List<RouteSegmentResult> route, RenderingRulesStorage rrs, boolean nightMode) {
			super(rrs, route, StatisticType.SURFACE, nightMode);
		}

		@Override
		public String getAttribute(RouteSegmentResult segment) {
			String segmentSurface = segment.getSurface();
			if (segmentSurface == null) {
				return UNDEFINED_ATTR;
			}
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			if (searchRenderingAttribute(req, segmentSurface)) {
				return segmentSurface;
			}
			return UNDEFINED_ATTR;
		}

		@Override
		public int getColor(String attribute) {
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			int color = 0;
			if (searchRenderingAttribute(req, attribute)) {
				color = req.getIntPropertyValue(rrs.PROPS.R_ATTR_COLOR_VALUE);
			}
			return color;
		}

		@Override
		public String getPropertyName(String attribute) {
			return attribute;
		}

		private boolean searchRenderingAttribute(RenderingRuleSearchRequest req, String attribute) {
			String additional = SURFACE_ATTR + "=" + attribute;
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
			req.setStringFilter(rrs.PROPS.R_ATTR_STRING_VALUE, SURFACE_ATTR + "_" + attribute);
			req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional);
			return req.searchRenderingAttribute(SURFACE_COLOR_ATTR);
		}
	}

	private static class RouteSmoothnessStatisticComputer extends RouteStatisticComputer<String> {

		private static final String SMOOTHNESS_ATTR = "smoothness";
		private static final String SMOOTHNESS_COLOR_ATTR = "smoothnessColor";

		public RouteSmoothnessStatisticComputer(List<RouteSegmentResult> route, RenderingRulesStorage rrs, boolean nightMode) {
			super(rrs, route, StatisticType.SMOOTHNESS, nightMode);
		}

		@Override
		public String getAttribute(RouteSegmentResult segment) {
			String segmentSmoothness = segment.getSmoothness();
			if (segmentSmoothness == null) {
				return UNDEFINED_ATTR;
			}
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			if (searchRenderingAttribute(req, segmentSmoothness)) {
				return segmentSmoothness;
			}
			return UNDEFINED_ATTR;
		}

		@Override
		public int getColor(String attribute) {
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			int color = 0;
			if (searchRenderingAttribute(req, attribute)) {
				color = req.getIntPropertyValue(rrs.PROPS.R_ATTR_COLOR_VALUE);
			}
			return color;
		}

		@Override
		public String getPropertyName(String attribute) {
			return attribute;
		}

		private boolean searchRenderingAttribute(RenderingRuleSearchRequest req, String attribute) {
			String additional = SMOOTHNESS_ATTR + "=" + attribute;
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
			req.setStringFilter(rrs.PROPS.R_ATTR_STRING_VALUE, SMOOTHNESS_ATTR + "_" + attribute);
			req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional);
			return req.searchRenderingAttribute(SMOOTHNESS_COLOR_ATTR);
		}
	}

	private static class RouteClassStatisticComputer extends RouteStatisticComputer<String> {

		private static final String HIGHWAY_ATTR = "highway";
		private static final String ROAD_CLASS_COLOR_ATTR = "roadClassColor";

		public RouteClassStatisticComputer(List<RouteSegmentResult> route, RenderingRulesStorage rrs, boolean nightMode) {
			super(rrs, route, StatisticType.CLASS, nightMode);
		}

		@Override
		public String getAttribute(RouteSegmentResult segment) {
			String segmentClass = segment.getHighway();
			if (segmentClass == null) {
				return UNDEFINED_ATTR;
			}
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			String type = null;
			if (searchRenderingAttribute(req, segmentClass)) {
				type = req.getStringPropertyValue(rrs.PROPS.R_ATTR_STRING_VALUE);
				if (req.searchRenderingAttribute(type)) {
					type = req.getStringPropertyValue(rrs.PROPS.R_ATTR_STRING_VALUE);
				}
			}
			return type != null ? type : UNDEFINED_ATTR;
		}

		@Override
		public int getColor(String attribute) {
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			int color = 0;
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
			if (req.searchRenderingAttribute(attribute)) {
				color = req.getIntPropertyValue(rrs.PROPS.R_ATTR_COLOR_VALUE);
			}
			return color;
		}

		@Override
		public String getPropertyName(String attribute) {
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			String type = "";
			if (searchRenderingAttribute(req, attribute)) {
				type = req.getStringPropertyValue(rrs.PROPS.R_ATTR_STRING_VALUE);
			}
			return type != null ? type : attribute;
		}

		private boolean searchRenderingAttribute(RenderingRuleSearchRequest req, String attribute) {
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
			req.setStringFilter(rrs.PROPS.R_TAG, HIGHWAY_ATTR);
			req.setStringFilter(rrs.PROPS.R_VALUE, attribute);
			return req.searchRenderingAttribute(ROAD_CLASS_COLOR_ATTR);
		}
	}

	private static class RouteSteepnessStatisticComputer extends RouteStatisticComputer<Boundaries> {

		private static final String STEEPNESS_ATTR = "steepness";
		private static final String STEEPNESS_COLOR_ATTR = "steepnessColor";

		private final List<Incline> inclines;

		public RouteSteepnessStatisticComputer(List<Incline> inclines, RenderingRulesStorage rrs, boolean nightMode) {
			super(rrs, null, StatisticType.STEEPNESS, nightMode);
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
            /*
                no-op
             */
			return null;
		}

		@Override
		public int getColor(Boundaries attribute) {
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
			int index = -1;
			for (int i = 1; i < Boundaries.NUM; i++) {
				int val = Boundaries.BOUNDARIES_ARRAY[i];
				if (val == (int) attribute.lowerBoundary) {
					index = i;
					break;
				}
			}
			if (searchRenderingAttribute(req, index)) {
				return req.getIntPropertyValue(rrs.PROPS.R_ATTR_COLOR_VALUE);
			}
			return 0;
		}

		@Override
		public String getPropertyName(Boundaries attribute) {
			return attribute.toString();
		}

		private boolean searchRenderingAttribute(RenderingRuleSearchRequest req, int attribute) {
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
			req.setStringFilter(rrs.PROPS.R_TAG, STEEPNESS_ATTR);
			req.setIntFilter(rrs.PROPS.R_ATTR_INT_VALUE, attribute);
			return req.searchRenderingAttribute(STEEPNESS_COLOR_ATTR);
		}
	}

	public static class RouteSegmentAttribute<E> {

		private final int index;
		private final E attribute;
		private final int color;
		private final String propertyName;

		private float distance;
		private float initDistance;

		public RouteSegmentAttribute(int index, E attribute, String propertyName, int color) {
			this.index = index;
			this.attribute = attribute;
			this.propertyName = propertyName;
			this.color = color;
		}

		public RouteSegmentAttribute(RouteSegmentAttribute<E> segmentAttribute) {
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

		private float inclineValue;
		private final float distance;
		private final Boundaries boundaries;

		public Incline(float inclineValue, float distance) {
			this.inclineValue = inclineValue;
			this.distance = distance;
			this.boundaries = Boundaries.newBoundariesFor(inclineValue);
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

		public static Boundaries newBoundariesFor(float incline) {
			if (incline > MAX_INCLINE) {
				return new Boundaries(MAX_INCLINE, MAX_INCLINE - STEP);
			}
			if (incline < MIN_INCLINE) {
				return new Boundaries(MIN_INCLINE + STEP, MIN_INCLINE);
			}
			for (int i = 1; i < NUM; i++) {
				if (incline >= BOUNDARIES_ARRAY[i - 1] && incline < BOUNDARIES_ARRAY[i]) {
					return new Boundaries(BOUNDARIES_ARRAY[i], BOUNDARIES_ARRAY[i - 1]);
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
			return String.format("%d%% - %d%%", Math.round(getLowerBoundary()), Math.round(getUpperBoundary()));
		}
	}

	public static class Statistics<E> {

		private final List<RouteSegmentAttribute<E>> elements;
		private final Map<E, RouteSegmentAttribute<E>> partition;
		private final float totalDistance;
		private final StatisticType type;

		private Statistics(List<RouteSegmentAttribute<E>> elements,
		                   Map<E, RouteSegmentAttribute<E>> partition,
		                   float totalDistance, StatisticType type) {
			this.elements = elements;
			this.partition = partition;
			this.totalDistance = totalDistance;
			this.type = type;
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

		public StatisticType getStatisticType() {
			return type;
		}
	}

	public enum StatisticType {
		CLASS,
		SURFACE,
		SMOOTHNESS,
		STEEPNESS
	}
}