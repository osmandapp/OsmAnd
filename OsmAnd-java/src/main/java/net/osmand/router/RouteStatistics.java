package net.osmand.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class RouteStatistics {

    private final List<RouteSegmentResult> route;

    private RouteStatistics(List<RouteSegmentResult> route) {
        this.route = route;
    }

    public static RouteStatistics newRouteStatistic(List<RouteSegmentResult> route) {
        return new RouteStatistics(route);
    }

    public Statistics getRouteSurfaceStatistic() {
        RouteStatisticComputer statisticComputer = new RouteSurfaceStatisticComputer(route);
        return statisticComputer.computeStatistic();
    }

    public Statistics getRouteSmoothnessStatistic() {
        RouteStatisticComputer statisticComputer = new RouteSmoothnessStatisticComputer(route);
        return statisticComputer.computeStatistic();
    }

    public Statistics getRouteClassStatistic() {
        RouteStatisticComputer statisticComputer = new RouteClassStatisticComputer(route);
        return statisticComputer.computeStatistic();
    }

    public Statistics getRouteSteepnessStatistic(List<Incline> inclines) {
        RouteStatisticComputer statisticComputer = new RouteSteepnessStatisticComputer(inclines);
        return statisticComputer.computeStatistic();
    }


    private abstract static class RouteStatisticComputer<E extends Comparable<E>> {

        private final List<RouteSegmentResult> route;

        private final StatisticType type;

        public RouteStatisticComputer(List<RouteSegmentResult> route, StatisticType type) {
            this.route = route;
            this.type = type;
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
                    String colorAttrName = getColorAttrName(current);
                    String colorName = getColorName(current);
                    routes.add(new RouteSegmentAttribute<>(index, current, colorAttrName, colorName));
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

        public abstract String getColorAttrName(E attribute);

        public abstract String getColorName(E attribute);
    }

    private static class RouteSurfaceStatisticComputer extends RouteStatisticComputer<RoadSurface> {

        public RouteSurfaceStatisticComputer(List<RouteSegmentResult> route) {
            super(route, StatisticType.SURFACE);
        }

        @Override
        public RoadSurface getAttribute(RouteSegmentResult segment) {
            String segmentSurface = segment.getSurface();
            if (segmentSurface == null) {
                return RoadSurface.UNDEFINED;
            }
            for (RoadSurface roadSurface : RoadSurface.values()) {
                if (roadSurface.contains(segmentSurface)) {
                    return roadSurface;
                }
            }
            return RoadSurface.UNDEFINED;
        }

	    @Override
	    public String getColorAttrName(RoadSurface attribute) {
		    return attribute.getColorAttrName();
	    }

	    @Override
	    public String getColorName(RoadSurface attribute) {
		    return attribute.getColorName();
	    }
    }

    private static class RouteSmoothnessStatisticComputer extends RouteStatisticComputer<RoadSmoothness> {

        public RouteSmoothnessStatisticComputer(List<RouteSegmentResult> route) {
            super(route, StatisticType.SMOOTHNESS);
        }

        @Override
        public RoadSmoothness getAttribute(RouteSegmentResult segment) {
            String segmentSmoothness = segment.getSmoothness();
            if (segmentSmoothness == null) {
                return RoadSmoothness.UNDEFINED;
            }
            for (RoadSmoothness roadSmoothness : RoadSmoothness.values()) {
                if (roadSmoothness.contains(segmentSmoothness)) {
                    return roadSmoothness;
                }
            }
            return RoadSmoothness.UNDEFINED;
        }

	    @Override
	    public String getColorAttrName(RoadSmoothness attribute) {
		    return attribute.getColorAttrName();
	    }

	    @Override
	    public String getColorName(RoadSmoothness attribute) {
		    return attribute.getColorName();
	    }
    }

    private static class RouteClassStatisticComputer extends RouteStatisticComputer<RoadClass> {

        public RouteClassStatisticComputer(List<RouteSegmentResult> route) {
            super(route, StatisticType.CLASS);
        }

        @Override
        public RoadClass getAttribute(RouteSegmentResult segment) {
            String segmentClass = segment.getHighway();
            if (segmentClass == null) {
                return RoadClass.UNDEFINED;
            }
            for (RoadClass roadClass : RoadClass.values()) {
                if (roadClass.contains(segmentClass)) {
                    return roadClass;
                }
            }
            return RoadClass.UNDEFINED;
        }

	    @Override
	    public String getColorAttrName(RoadClass attribute) {
		    return attribute.getColorAttrName();
	    }

	    @Override
	    public String getColorName(RoadClass attribute) {
		    return attribute.getColorName();
	    }
    }

    private static class RouteSteepnessStatisticComputer extends RouteStatisticComputer<Boundaries> {

        private static final String POSITIVE_INCLINE_COLOR_ATTR_NAME = "greenColor";
        private static final String NEGATIVE_INCLINE_COLOR_ATTR_NAME = "redColor";

        private final List<Incline> inclines;

        public RouteSteepnessStatisticComputer(List<Incline> inclines) {
            super(null, StatisticType.STEEPNESS);
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
	                String colorAttrName = getColorAttrName(current);
	                String colorName = getColorName(current);
                    RouteSegmentAttribute<Boundaries> attribute = new RouteSegmentAttribute<>(index, current, colorAttrName, colorName);
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
        public String getColorAttrName(Boundaries attribute) {
            return attribute.getLowerBoundary() >= 0 ? POSITIVE_INCLINE_COLOR_ATTR_NAME : NEGATIVE_INCLINE_COLOR_ATTR_NAME;
        }

	    @Override
	    public String getColorName(Boundaries attribute) {
		    return null;
	    }
    }

    public static class RouteSegmentAttribute<E> {

        private final int index;
        private final E attribute;
        private final String colorAttrName;
        private final String colorName;

        private float distance;
        private float initDistance;

        public RouteSegmentAttribute(int index, E attribute, String colorAttrName, String colorName) {
            this.index = index;
            this.attribute = attribute;
            this.colorAttrName = colorAttrName;
            this.colorName = colorName;
        }

        public RouteSegmentAttribute(RouteSegmentAttribute<E> segmentAttribute) {
            this.index = segmentAttribute.getIndex();
            this.attribute = segmentAttribute.getAttribute();
            this.colorAttrName = segmentAttribute.getColorAttrName();
            this.colorName = segmentAttribute.getColorName();
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

        public String getColorAttrName() {
            return colorAttrName;
        }

        public String getColorName() {
            return colorName;
        }

        @Override
        public String toString() {
            return "RouteSegmentAttribute{" +
                    "index=" + index +
                    ", attribute='" + attribute + '\'' +
                    ", colorAttrName='" + colorAttrName + '\'' +
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
        private static final int MAX_INCLINE = 100;
        private static final int STEP = 4;
        private static final int NUM;
        private static final int[] BOUNDARIES_ARRAY;

        static {
            NUM = ((MAX_INCLINE - MIN_INCLINE) / STEP + 1);
            BOUNDARIES_ARRAY = new int[NUM];
            for (int i = 0; i < NUM; i++) {
                BOUNDARIES_ARRAY[i] = MIN_INCLINE + i * STEP;
            }
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
            return  (int) (getLowerBoundary() - boundaries.getLowerBoundary());
        }

        @Override
        public String toString() {
            return String.format("%d-%d", Math.round(getLowerBoundary()), Math.round(getUpperBoundary()));
        }
    }

    public static class Statistics<E>  {

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

	public enum RoadClass {
		MOTORWAY(null, "#ffa200", "motorway", "motorway_link"),
		STATE_ROAD(null, "#ffae1d", "trunk", "trunk_link", "primary", "primary_link"),
		ROAD(null, "#ffb939", "secondary", "secondary_link", "tertiary", "tertiary_link", "unclassified"),
		STREET(null, "#ffc554", "residential", "living_street"),
		SERVICE(null, "#ffd070", "service"),
		TRACK(null, "#ffdb8a", "track", "road"),
		FOOTWAY(null, "#ffe7a7", "footway"),
		CYCLE_WAY(null, "#fff4c6", "cycleway"),
		PATH(null, "#fffadd", "path"),
		UNDEFINED(null, "#DCDBDD", "undefined");

		final Set<String> roadClasses = new TreeSet<>();
		final String colorAttrName;
		final String colorName;

		RoadClass(String colorAttrName, String colorName, String... classes) {
			roadClasses.addAll(Arrays.asList(classes));
			this.colorAttrName = colorAttrName;
			this.colorName = colorName;
		}

		boolean contains(String roadClass) {
			return roadClasses.contains(roadClass);
		}

		String getColorAttrName() {
			return colorAttrName;
		}

		public String getColorName() {
			return this.colorName;
		}
	}

	public enum RoadSurface {
		ASPHALT(null, "#6f687e", "asphalt"),
		CONCRETE(null, "#a7cdf8", "concrete"),
		UNPAVED(null, "#cc9900", "unpaved"),
		PAVED(null, "#a7cdf8", "paved"),
		COMPACTED(null, "#cbcbe8", "compacted"),
		FINE_GRAVEL(null, "#cbcbe8", "fine_gravel"),
		PAVING_STONES(null, "#a7cdf8", "paving_stones"),
		SETT(null, "#a7cdf8", "sett"),
		COBBLESTONE(null, "#a7cdf8", "cobblestone"),
		PEBBLESTONE("#a7cdf8", "pebblestone"),
		STONE(null, "#a7cdf8", "stone"),
		METAL(null, "#a7cdf8", "metal"),
		GRASS_PAVER(null, "#a7bef8", "grass_paver"),
		WOOD(null, "#a7cdf8", "wood"),
		GRAVEL(null, "#cbcbe8", "gravel"),
		GROUND(null, "#cc9900", "ground", "mud"),
		CLAY(null, "#cc9900", "clay"),
		GRASS(null, "#1fbe1f", "grass"),
		SAND(null, "#ffd700", "sand"),
		SALT(null, "#7eded8", "salt"),
		SNOW(null, "#9feeef", "snow"),
		ICE(null, "#9feeef", "ice"),
		UNDEFINED(null, "#e8e8e8", "undefined");

		final Set<String> surfaces = new TreeSet<>();
		final String colorAttrName;
		final String colorName;

		RoadSurface(String colorAttrName, String colorName, String... surfaces) {
			this.surfaces.addAll(Arrays.asList(surfaces));
			this.colorAttrName = colorAttrName;
			this.colorName = colorName;
		}

		boolean contains(String surface) {
			return surfaces.contains(surface);
		}

		public String getColorAttrName() {
			return this.colorAttrName;
		}

		public String getColorName() {
			return this.colorName;
		}
	}

	public enum RoadSmoothness {
		EXCELLENT("orangeColor", null, "excellent"),
		GOOD("brownColor", null, "good"),
		INTERMEDIATE("darkyellowColor", null, "intermediate"),
		BAD("yellowColor", null, "bad"),
		VERY_BAD("lightgreenColor", null, "very_bad"),
		HORRIBLE("greenColor", null, "horrible"),
		VERY_HORRIBLE("lightblueColor", null, "very_horrible"),
		IMPASSABLE("blueColor", null, "impassable"),
		UNDEFINED("redColor", null, "undefined");

		final Set<String> surfaces = new TreeSet<>();
		final String colorAttrName;
		final String colorName;

		RoadSmoothness(String colorAttrName, String colorName, String... surfaces) {
			this.surfaces.addAll(Arrays.asList(surfaces));
			this.colorAttrName = colorAttrName;
			this.colorName = colorName;
		}

		boolean contains(String surface) {
			return surfaces.contains(surface);
		}

		public String getColorAttrName() {
			return this.colorAttrName;
		}

		public String getColorName() {
			return this.colorName;
		}
	}

	public enum StatisticType {
		CLASS,
		SURFACE,
		SMOOTHNESS,
		STEEPNESS
	}
}
