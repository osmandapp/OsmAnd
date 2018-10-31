package net.osmand.router;

import java.util.*;

public class RouteStatistics {

    private static final String UNDEFINED = "undefined";

    private final RouteStatisticComputer routeSurfaceStatisticComputer;
    private final RouteStatisticComputer routeSmoothnessStatisticComputer;
    private final RouteStatisticComputer routeClassStatisticComputer;
    private final RouteStatisticComputer routeSteepnessStatisticComputer;


    private RouteStatistics(RouteStatisticComputer routeSurfaceStatisticComputer,
                            RouteStatisticComputer routeSmoothnessStatisticComputer,
                            RouteStatisticComputer routeClassStatisticComputer,
                            RouteStatisticComputer routeSteepnessStatisticComputer) {
        this.routeSurfaceStatisticComputer = routeSurfaceStatisticComputer;
        this.routeSmoothnessStatisticComputer = routeSmoothnessStatisticComputer;
        this.routeClassStatisticComputer = routeClassStatisticComputer;
        this.routeSteepnessStatisticComputer = routeSteepnessStatisticComputer;
    }

    public static RouteStatistics newRouteStatistic(List<RouteSegmentResult> route) {
        RouteStatisticComputer routeSurfaceStatisticComputer = new RouteSegmentSurfaceStatisticComputer(route);
        RouteStatisticComputer routeSmoothnessStatisticComputer = new RouteSegmentSmoothnessStatisticComputer(route);
        RouteStatisticComputer routeClassStatisticComputer = new RouteSegmentClassStatisticComputer(route);
        RouteStatisticComputer routeSteepnessStatisticComputer = new RouteSegmentSteepnessStatisticComputer(route);
        return new RouteStatistics(routeSurfaceStatisticComputer,
                routeSmoothnessStatisticComputer,
                routeClassStatisticComputer,
                routeSteepnessStatisticComputer);
    }

    public List<RouteSegmentAttribute> getRouteSurfaceStatistic() {
        return routeSurfaceStatisticComputer.computeStatistic();
    }

    public List<RouteSegmentAttribute> getRouteSmoothnessStatistic() {
        return routeSmoothnessStatisticComputer.computeStatistic();
    }

    public List<RouteSegmentAttribute> getRouteClassStatistic() {
        return routeClassStatisticComputer.computeStatistic();
    }

    public List<RouteSegmentAttribute> getRouteSteepnessStatistic() {
        return routeSteepnessStatisticComputer.computeStatistic();
    }


    private abstract static class RouteStatisticComputer {

        private final List<RouteSegmentResult> route;

        public RouteStatisticComputer(List<RouteSegmentResult> route) {
            this.route = new ArrayList<>(route);
        }

        public List<RouteSegmentResult> getRoute() {
            return route;
        }

        protected List<RouteSegmentAttribute> computeStatistic() {
            int index = 0;
            List<RouteSegmentAttribute> routeSurfaces = new ArrayList<>();
            String prev = null;
            for (RouteSegmentResult segment : getRoute()) {
                String current = getAttribute(segment);
                if (current == null) {
                    current = UNDEFINED;
                }
                if (prev != null && !prev.equals(current)) {
                    index++;
                }
                if (index >= routeSurfaces.size()) {
                    routeSurfaces.add(new RouteSegmentAttribute(index, current));
                }
                RouteSegmentAttribute surface = routeSurfaces.get(index);
                surface.incrementDistanceBy(segment.getDistance());
                prev = current;
            }
            return routeSurfaces;
        }

        public abstract String getAttribute(RouteSegmentResult segment);

    }

    private static class RouteSegmentSurfaceStatisticComputer extends RouteStatisticComputer {

        public RouteSegmentSurfaceStatisticComputer(List<RouteSegmentResult> route) {
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

    private static class RouteSegmentSmoothnessStatisticComputer extends RouteStatisticComputer {

        public RouteSegmentSmoothnessStatisticComputer(List<RouteSegmentResult> route) {
            super(route);
        }

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            return segment.getSmoothness();
        }
    }

    private static class RouteSegmentClassStatisticComputer extends RouteStatisticComputer {

        public RouteSegmentClassStatisticComputer(List<RouteSegmentResult> route) {
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

    private static class RouteSegmentSteepnessStatisticComputer extends RouteStatisticComputer {

        public RouteSegmentSteepnessStatisticComputer(List<RouteSegmentResult> route) {
            super(route);
        }

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            return null;
        }

        @Override
        public List<RouteSegmentAttribute> computeStatistic() {
            List<RouteSegmentAttribute> routeInclines = new ArrayList<>();
            int inclineIndex = 0;
            for (RouteSegmentResult segment : getRoute()) {
                float[] heights = segment.getHeightValues();
                if (heights.length == 0) {
                    RouteSegmentIncline routeIncline = new RouteSegmentIncline(inclineIndex++);
                    routeIncline.mayJoin(0);
                    routeIncline.incrementDistanceBy(segment.getDistance());
                    routeInclines.add(routeIncline);
                    continue;
                }
                for (int index = 1; index < heights.length / 2; index++) {
                    int prevHeightIndex = 2 * (index - 1) + 1;
                    int currHeightIndex = 2 * index + 1;
                    int distanceBetweenHeightsIndex = 2 * index;
                    float prevHeight = heights[prevHeightIndex];
                    float currHeight = heights[currHeightIndex];
                    float distanceBetweenHeights = heights[distanceBetweenHeightsIndex];
                    float incline = computeIncline(prevHeight, currHeight, distanceBetweenHeights);

                    if (inclineIndex >= routeInclines.size()) {
                        routeInclines.add(new RouteSegmentIncline(inclineIndex));
                    }

                    RouteSegmentIncline routeIncline = (RouteSegmentIncline) routeInclines.get(inclineIndex);

                    if (routeIncline.mayJoin(incline)) {
                        routeIncline.addIncline(incline);
                        routeIncline.incrementDistanceBy(distanceBetweenHeights);
                    } else {
                        inclineIndex++;
                    }
                }
            }
            return routeInclines;
        }

        private float computeIncline(float prevHeight, float currHeight, float distance) {
            float incline = (currHeight - prevHeight) / distance;
            if (Float.isInfinite(incline) || Float.isNaN(incline)) {
                incline = 0f;
            }
            return incline * 100;
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

    public static class RouteSegmentIncline extends RouteSegmentAttribute {

        private static final float MAX_INCLINE = 30;
        private static final float MIN_INCLINE = -30;
        private static final float STEP = 3;
        private static final int NUM;
        private static final float[] INTERVALS;

        static {
            NUM = (int) ((MAX_INCLINE - MIN_INCLINE) / STEP) + 1;
            INTERVALS = new float[NUM];
            for (int k = 0; k < INTERVALS.length; k++) {
                INTERVALS[k] = STEP * k + MIN_INCLINE;
            }
        }

        private final List<Float> inclines = new ArrayList<>();
        private float upperBoundary;
        private float lowerBoundary;
        private float middlePoint;

        public RouteSegmentIncline(int index) {
            super(index,"incline");
        }

        private void determineBoundaries(float incline) {
            for (int pos = 1; pos < INTERVALS.length; pos++) {
                float lower = INTERVALS[pos - 1];
                float upper = INTERVALS[pos];
                if (incline >= lower && incline < upper) {
                    this.lowerBoundary = lower;
                    this.upperBoundary = upper;
                    this.middlePoint = (upperBoundary + lowerBoundary) / 2f;
                    break;
                }
            }
        }

        public float getUpperBoundary() {
            return upperBoundary;
        }

        public float getLowerBoundary() {
            return lowerBoundary;
        }

        public boolean mayJoin(float incline) {
            if (lowerBoundary == upperBoundary) {
                determineBoundaries(incline);
            }
            return incline >= lowerBoundary && incline < upperBoundary;
        }

        public void addIncline(float incline) {
            inclines.add(incline);
        }

        public float getMiddlePoint() {
            return this.middlePoint;
        }

        @Override
        public String toString() {
            return "RouteSegmentIncline{" +
                    "index=" + getIndex() +
                    ", distance=" + getDistance() +
                    ", inclines=" + inclines +
                    ", upperBoundary=" + upperBoundary +
                    ", lowerBoundary=" + lowerBoundary +
                    ", middlePoint=" + middlePoint +
                    '}';
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
}
