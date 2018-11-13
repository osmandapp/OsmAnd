package net.osmand.router;

import java.util.*;

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


    private abstract static class RouteStatisticComputer {

        private final List<RouteSegmentResult> route;

        public RouteStatisticComputer(List<RouteSegmentResult> route) {
            this.route = route;
        }

        protected Map<String, RouteSegmentAttribute> makePartition(List<RouteSegmentAttribute> routeAttributes) {
            Map<String, RouteSegmentAttribute> partition = new TreeMap<>();
            for (RouteSegmentAttribute attribute : routeAttributes) {
                String key = attribute.getAttribute();
                RouteSegmentAttribute pattr = partition.get(key);
                if (pattr == null) {
                    pattr = new RouteSegmentAttribute(attribute.getIndex(), attribute.getAttribute(), attribute.getColorAttrName());
                    partition.put(key, pattr);
                }
                pattr.incrementDistanceBy(attribute.getDistance());
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

        protected List<RouteSegmentResult> getRoute() {
            return route;
        }

        protected List<RouteSegmentAttribute> processRoute() {
            int index = 0;
            List<RouteSegmentAttribute> routes = new ArrayList<>();
            String prev = null;
            for (RouteSegmentResult segment : getRoute()) {
                String current = getAttribute(segment);
                if (prev != null && !prev.equals(current)) {
                    index++;
                }
                if (index >= routes.size()) {
                    String colorAttrName = determineColor(current);
                    routes.add(new RouteSegmentAttribute(index, current, colorAttrName));
                }
                RouteSegmentAttribute surface = routes.get(index);
                surface.incrementDistanceBy(segment.getDistance());
                prev = current;
            }
            return routes;
        }

        public Statistics computeStatistic() {
            List<RouteSegmentAttribute> routeAttributes = processRoute();
            Map<String, RouteSegmentAttribute> partition = makePartition(routeAttributes);
            float totalDistance = computeTotalDistance(routeAttributes);
            return new Statistics(routeAttributes, partition, totalDistance);
        }

        public abstract String getAttribute(RouteSegmentResult segment);

        public abstract String determineColor(String attribute);

    }

    private static class RouteSurfaceStatisticComputer extends RouteStatisticComputer {

        public RouteSurfaceStatisticComputer(List<RouteSegmentResult> route) {
            super(route);
        }

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            String segmentSurface = segment.getSurface();
            if (segmentSurface == null) {
                return RoadSurface.UNDEFINED.name().toLowerCase();
            }
            for (RoadSurface roadSurface : RoadSurface.values()) {
                if (roadSurface.contains(segmentSurface)) {
                    return roadSurface.name().toLowerCase();
                }
            }
            return RoadSurface.UNDEFINED.name().toLowerCase();
        }

        @Override
        public String determineColor(String attribute) {
            RoadSurface roadSurface = RoadSurface.valueOf(attribute.toUpperCase());
            return roadSurface.getColorAttrName();
        }
    }

    private static class RouteSmoothnessStatisticComputer extends RouteStatisticComputer {

        public RouteSmoothnessStatisticComputer(List<RouteSegmentResult> route) {
            super(route);
        }

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            String segmentSmoothness = segment.getSurface();
            if (segmentSmoothness == null) {
                return RoadSmoothness.UNDEFINED.name().toLowerCase();
            }
            for (RoadSmoothness roadSmoothness : RoadSmoothness.values()) {
                if (roadSmoothness.contains(segmentSmoothness)) {
                    return roadSmoothness.name().toLowerCase();
                }
            }
            return RoadSmoothness.UNDEFINED.name().toLowerCase();
        }

        @Override
        public String determineColor(String attribute) {
            RoadSmoothness roadSmoothness = RoadSmoothness.valueOf(attribute.toUpperCase());
            return roadSmoothness.getColorAttrName();
        }
    }


    private static class RouteClassStatisticComputer extends RouteStatisticComputer {

        public RouteClassStatisticComputer(List<RouteSegmentResult> route) {
            super(route);
        }

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            String segmentClass = segment.getHighway();
            if (segmentClass == null) {
                return RoadClass.UNDEFINED.name().toLowerCase();
            }
            for (RoadClass roadClass : RoadClass.values()) {
                if (roadClass.contains(segmentClass)) {
                    return roadClass.name().toLowerCase();
                }
            }
            return RoadClass.UNDEFINED.name().toLowerCase();
        }

        @Override
        public String determineColor(String attribute) {
            RoadClass roadClass = RoadClass.valueOf(attribute.toUpperCase());
            return roadClass.getColorAttrName();
        }
    }


    private static class RouteSteepnessStatisticComputer extends RouteStatisticComputer {

        private static final String POSITIVE_INCLINE_COLOR_ATTR_NAME = "greenColor";
        private static final String NEGATIVE_INCLINE_COLOR_ATTR_NAME = "redColor";

        private final List<Incline> inclines;

        public RouteSteepnessStatisticComputer(List<Incline> inclines) {
            super(null);
            this.inclines = inclines;
        }

        @Override
        public List<RouteSegmentAttribute> processRoute() {
            List<RouteSegmentAttribute> routeInclines = new ArrayList<>();
            int index = 0;
            String prev = null;
            Incline prevIncline = null;
            for (Incline incline : inclines) {
                String current = incline.getBoundariesAsString();
                if (prev != null && !prev.equals(current)) {
                    index++;
                }
                if (index >= routeInclines.size()) {
                    String colorAttrName = determineColor(incline);
                    RouteSegmentAttribute attribute = new RouteSegmentAttribute(index, current, colorAttrName);
                    if (
                            //index > 0 &&
                            prevIncline != null) {
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
        public String getAttribute(RouteSegmentResult segment) {
            return null;
        }

        @Override
        public String determineColor(String attribute) {
            return null;
        }

        public String determineColor(Incline incline) {
            float value = incline.getValue();
            return value >= 0 ? POSITIVE_INCLINE_COLOR_ATTR_NAME : NEGATIVE_INCLINE_COLOR_ATTR_NAME;
        }
    }


    public static class RouteSegmentAttribute {

        private final int index;
        private final String attribute;
        private final String colorAttrName;

        private float distance;
        private float initDistance;

        public RouteSegmentAttribute(int index, String attribute, String colorAttrName) {
            this.index = index;
            this.attribute = attribute;
            this.colorAttrName = colorAttrName;
        }

        public int getIndex() {
            return index;
        }

        public String getAttribute() {
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

        public String getBoundariesAsString() {
            return String.format("%d-%d", Math.round(boundaries.getLowerBoundary()), Math.round(boundaries.getUpperBoundary()));
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
        private static final int[] BOUNDARIES;

        static {
            NUM = (int) ((MAX_INCLINE - MIN_INCLINE) / STEP + 1);
            BOUNDARIES = new int[NUM];
            for (int i = 0; i < NUM; i++) {
                BOUNDARIES[i] = MIN_INCLINE + i * STEP;
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
                if (incline >= BOUNDARIES[i - 1] && incline < BOUNDARIES[i]) {
                    return new Boundaries(BOUNDARIES[i], BOUNDARIES[i - 1]);
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
    }

    public static class Statistics  {

        private final List<RouteSegmentAttribute> elements;
        private final Map<String, RouteSegmentAttribute> partition;
        private final float totalDistance;

        private Statistics(List<RouteSegmentAttribute> elements,
                          Map<String, RouteSegmentAttribute> partition,
                          float totalDistance) {
            this.elements = elements;
            this.partition = partition;
            this.totalDistance = totalDistance;
        }

        public float getTotalDistance() {
            return totalDistance;
        }

        public List<RouteSegmentAttribute> getElements() {
            return elements;
        }

        public Map<String, RouteSegmentAttribute> getPartition() {
            return partition;
        }
    }

    public enum RoadClass {
        UNDEFINED("whitewaterSectionGrade0Color", "undefined"),
        MOTORWAY("motorwayRoadColor", "motorway", "motorway_link"),
        STATE_ROAD("trunkRoadColor" , "trunk", "trunk_link", "primary", "primary_link"),
        ROAD("secondaryRoadColor", "secondary", "secondary_link", "tertiary", "tertiary_link", "unclassified"),
        STREET("residentialRoadColor" ,"residential", "living_street"),
        SERVICE("serviceRoadColor", "service"),
        TRACK("trackColor", "track", "road"),
        FOOTWAY("footwayColor", "footway"),
        PATH("pathColor", "path"),
        CYCLE_WAY("cyclewayColor", "cycleway");

        final Set<String> roadClasses = new TreeSet<>();
        final String colorAttrName;

        RoadClass(String colorAttrName, String... classes) {
           roadClasses.addAll(Arrays.asList(classes));
           this.colorAttrName =  colorAttrName;
        }

        boolean contains(String roadClass) {
            return roadClasses.contains(roadClass);
        }

        String getColorAttrName() {
            return colorAttrName;
        }
    }

    public enum RoadSurface {
        UNDEFINED("whitewaterSectionGrade0Color", "undefined"),
        PAVED("motorwayRoadColor", "paved"),
        UNPAVED("motorwayRoadShadowColor", "unpaved"),
        ASPHALT("trunkRoadColor", "asphalt"),
        CONCRETE("primaryRoadColor", "concrete"),
        COMPACTED("secondaryRoadColor", "compacted"),
        GRAVEL("tertiaryRoadColor", "gravel"),
        FINE_GRAVEL("residentialRoadColor", "fine_gravel"),
        PAVING_STONES("serviceRoadColor", "paving_stones"),
        SETT("roadRoadColor", "sett"),
        COBBLESTONE("pedestrianRoadColor", "cobblestone"),
        PEBBLESTONE("racewayColor", "pebblestone"),
        STONE("trackColor", "stone"),
        METAL("footwayColor", "metal"),
        GROUND("pathColor", "ground", "mud"),
        WOOD("cycleRouteColor", "wood"),
        GRASS_PAVER("osmcBlackColor", "grass_paver"),
        GRASS("osmcBlueColor", "grass"),
        SAND("osmcGreenColor", "sand"),
        SALT("osmcRedColor", "salt"),
        SNOW("osmcYellowColor", "snow"),
        ICE("osmcOrangeColor", "ice"),
        CLAY("osmcBrownColor", "clay");

        final Set<String> surfaces = new TreeSet<>();
        final String colorAttrName;

        RoadSurface(String colorAttrName, String... surfaces) {
            this.surfaces.addAll(Arrays.asList(surfaces));
            this.colorAttrName = colorAttrName;
        }

        boolean contains(String surface) {
            return surfaces.contains(surface);
        }

        public String getColorAttrName() {
            return this.colorAttrName;
        }
    }

    public enum RoadSmoothness {
        UNDEFINED("redColor", "undefined"),
        EXCELLENT("orangeColor", "excellent"),
        GOOD("brownColor", "good"),
        INTERMEDIATE("darkyellowColor", "intermediate"),
        BAD("yellowColor", "bad"),
        VERY_BAD("lightgreenColor", "very_bad"),
        HORRIBLE("greenColor", "horrible"),
        VERY_HORRIBLE("lightblueColor", "very_horrible"),
        IMPASSABLE("blueColor", "impassable");

        final Set<String> surfaces = new TreeSet<>();
        final String colorAttrName;

        RoadSmoothness(String colorAttrName, String... surfaces) {
            this.surfaces.addAll(Arrays.asList(surfaces));
            this.colorAttrName = colorAttrName;
        }

        boolean contains(String surface) {
            return surfaces.contains(surface);
        }

        public String getColorAttrName() {
            return this.colorAttrName;
        }
    }
}
