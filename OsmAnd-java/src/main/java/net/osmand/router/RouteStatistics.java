package net.osmand.router;

import java.util.*;

public class RouteStatistics {

    private static final String UNDEFINED = "undefined";

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

    public Statistics getRouteSteepnessStatistic() {
        RouteStatisticComputer statisticComputer = new RouteSteepnessStatisticComputer(route);
        return statisticComputer.computeStatistic();
    }


    private abstract static class RouteStatisticComputer {

        private final List<RouteSegmentResult> route;

        public RouteStatisticComputer(List<RouteSegmentResult> route) {
            this.route = new ArrayList<>(route);
        }

        private Map<String, RouteSegmentAttribute> makePartition(List<RouteSegmentAttribute> routeAttributes) {
            Map<String, RouteSegmentAttribute> partition = new HashMap<>();
            for (RouteSegmentAttribute attribute : routeAttributes) {
                String key = attribute.getAttribute();
                RouteSegmentAttribute pattr = partition.get(key);
                if (pattr == null) {
                    pattr = new RouteSegmentAttribute(attribute.getIndex(), attribute.getAttribute());
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
                if (current == null) {
                    current = UNDEFINED;
                }
                if (prev != null && !prev.equals(current)) {
                    index++;
                }
                if (index >= routes.size()) {
                    routes.add(new RouteSegmentAttribute(index, current));
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

    }



    private static class RouteSurfaceStatisticComputer extends RouteStatisticComputer {

        public RouteSurfaceStatisticComputer(List<RouteSegmentResult> route) {
            super(route);
        }

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            String segmentSurface = segment.getSurface();
            if (segmentSurface == null) {
                return null;
            }
            for (RoadSurface roadSurface : RoadSurface.values()) {
                if (roadSurface.contains(segmentSurface)) {
                    return roadSurface.name().toLowerCase();
                }
            }
            return null;
        }
    }

    private static class RouteSmoothnessStatisticComputer extends RouteStatisticComputer {

        public RouteSmoothnessStatisticComputer(List<RouteSegmentResult> route) {
            super(route);
        }

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            return segment.getSmoothness();
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
                return null;
            }
            for (RoadClass roadClass : RoadClass.values()) {
                if (roadClass.contains(segmentClass)) {
                    return roadClass.name().toLowerCase();
                }
            }
            return null;
        }
    }


    private static class RouteSteepnessStatisticComputer extends RouteStatisticComputer {

        public RouteSteepnessStatisticComputer(List<RouteSegmentResult> route) {
            super(route);
        }

        private int computeIncline(float prevHeight, float currHeight, float distance) {
            float incline = (currHeight - prevHeight) / distance * 100;
            if (incline > 30f) {
                incline = 30;
            }
            if (incline < -30f) {
                incline = -30;
            }
            if (Float.isInfinite(incline) || Float.isNaN(incline)) {
                incline = 0f;
            }
            return Math.round(incline);
        }

        private List<Incline> computeSegmentInclines() {
            List<Incline> inclines = new ArrayList<>();
            for (RouteSegmentResult segment : getRoute()) {
                float[] heights = segment.getHeightValues();
                if (heights.length == 0) {
                    Incline incline = new Incline(0, segment.getDistance());
                    inclines.add(incline);
                    continue;
                }
                for (int index = 1; index < heights.length / 2; index++) {
                    int prevHeightIndex = 2 * (index - 1) + 1;
                    int currHeightIndex = 2 * index + 1;
                    int distanceBetweenHeightsIndex = 2 * index;
                    float prevHeight = heights[prevHeightIndex];
                    float currHeight = heights[currHeightIndex];
                    float distanceBetweenHeights = heights[distanceBetweenHeightsIndex];
                    int computedIncline = computeIncline(prevHeight, currHeight, distanceBetweenHeights);
                    Incline incline = new Incline(computedIncline, distanceBetweenHeights);
                    inclines.add(incline);
                }
            }
            return inclines;
        }

        @Override
        public List<RouteSegmentAttribute> processRoute() {
            List<RouteSegmentAttribute> routeInclines = new ArrayList<>();
            int index = 0;
            String prev = null;
            for (Incline incline : computeSegmentInclines()) {
                String current = incline.getBoundariesAsString();
                if (prev != null && !prev.equals(current)) {
                    index++;
                }
                if (index >= routeInclines.size()) {
                    routeInclines.add(new RouteSegmentAttribute(index, current));
                }
                RouteSegmentAttribute routeIncline = routeInclines.get(index);
                routeIncline.incrementDistanceBy(incline.getDistance());
                prev = current;
            }
            return routeInclines;
        }

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            return null;
        }
    }



    public static class RouteSegmentAttribute {

        private final int index;

        private final String attribute;

        private float distance;

        public RouteSegmentAttribute(int index, String attribute) {
            this.index = index;
            this.attribute = attribute;
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

        public void incrementDistanceBy(float distance) {
            this.distance += distance;
        }

        @Override
        public String toString() {
            return "{" +
                    "index=" + index +
                    ", attribute='" + attribute + '\'' +
                    ", distance=" + distance +
                    '}';
        }
    }

    private static class Incline {

        private int inclineValue;
        private final float distance;

        public Incline(int inclineValue, float distance) {
            this.inclineValue = inclineValue;
            this.distance = distance;
        }


        public int getValue() {
            return inclineValue;
        }

        public float getDistance() {
            return distance;
        }

        public String getBoundariesAsString() {
            return String.valueOf(getValue());
        }

        @Override
        public String toString() {
            return "Incline{" +
                    ", incline=" + inclineValue +
                    ", distance=" + distance +
                    '}';
        }
    }

    public static class Statistics  {

        private final List<RouteSegmentAttribute> elements;
        private final Map<String, RouteSegmentAttribute> partition;
        private final float totalDistance;

        public Statistics(List<RouteSegmentAttribute> elements,
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
            return new ArrayList<>(elements);
        }

        public Map<String, RouteSegmentAttribute> getPartition() {
            return new HashMap<>(partition);
        }
    }

    public enum RoadClass {
        MOTORWAY("motorway", "motorway_link"),
        STATE_ROAD("trunk", "trunk_link", "primary", "primary_link"),
        ROAD("secondary", "secondary_link", "tertiary", "tertiary_link", "unclassified"),
        STREET("residential", "living_street"),
        SERVICE("service"),
        TRACK("track", "road"),
        FOOTWAY("footway"),
        PATH("path"),
        CYCLE_WAY("cycleway");

        final Set<String> roadClasses = new TreeSet<>();

        RoadClass(String... classes) {
           roadClasses.addAll(Arrays.asList(classes));
        }

        boolean contains(String roadClass) {
            return roadClasses.contains(roadClass);
        }
    }

    public enum RoadSurface {
        PAVED("paved"),
        UNPAVED("unpaved"),
        ASPHALT("asphalt"),
        CONCRETE("concrete"),
        COMPACTED("compacted"),
        GRAVEL("gravel"),
        FINE_GRAVEL("fine_gravel"),
        PAVING_STONES("paving_stones"),
        SETT("sett"),
        COBBLESTONE("cobblestone"),
        PEBBLESTONE("pebblestone"),
        STONE("stone"),
        METAL("metal"),
        GROUND("ground", "mud"),
        WOOD("wood"),
        GRASS_PAVER("grass_paver"),
        GRASS("grass"),
        SAND("sand"),
        SALT("salt"),
        SNOW("snow"),
        ICE("ice"),
        CLAY("clay");

        final Set<String> surfaces = new TreeSet<>();

        RoadSurface(String... surfaces) {
            this.surfaces.addAll(Arrays.asList(surfaces));
        }

        boolean contains(String surface) {
            return surfaces.contains(surface);
        }
    }
}
