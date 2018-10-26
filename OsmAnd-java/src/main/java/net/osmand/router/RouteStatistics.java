package net.osmand.router;

import java.util.ArrayList;
import java.util.List;

public class RouteStatistics {

    private static final String UNDEFINED = "undefined";

    private final List<RouteSegmentAttribute> routeSurfaceStatistic;
    private final List<RouteSegmentAttribute> routeSmoothnessStatistic;
    private final List<RouteSegmentAttribute> routeClassStatistic;

    public RouteStatistics(List<RouteSegmentAttribute> routeSurfaceStatistic,
                           List<RouteSegmentAttribute> routeSmoothnessStatistic,
                           List<RouteSegmentAttribute> routeClassStatistic) {
        this.routeSurfaceStatistic = routeSurfaceStatistic;
        this.routeSmoothnessStatistic = routeSmoothnessStatistic;
        this.routeClassStatistic = routeClassStatistic;
    }

    public static RouteStatistics calculate(List<RouteSegmentResult> route) {
        RouteStatisticComputer routeSurfaceStatisticComputer = new RouteSegmentSurfaceStatisticComputer();
        RouteStatisticComputer routeSmoothnessStatisticComputer = new RouteSegmentSmoothnessStatisticComputer();
        RouteStatisticComputer routeClassStatisticComputer = new RouteSegmentClassStatisticComputer();

        return new RouteStatistics(routeSurfaceStatisticComputer.computeStatistic(route),
                routeSmoothnessStatisticComputer.computeStatistic(route),
                routeClassStatisticComputer.computeStatistic(route));
    }

    public List<RouteSegmentAttribute> getRouteSurfaceStatistic() {
        return routeSurfaceStatistic;
    }

    public List<RouteSegmentAttribute> getRouteSmoothnessStatistic() {
        return routeSmoothnessStatistic;
    }

    public List<RouteSegmentAttribute> getRouteClassStatistic() {
        return routeClassStatistic;
    }

    private abstract static class RouteStatisticComputer {

        protected List<RouteSegmentAttribute> computeStatistic(List<RouteSegmentResult> segments) {
            int index = 0;
            List<RouteSegmentAttribute> routeSurfaces = new ArrayList<>();
            String prev = null;
            for (RouteSegmentResult segment : segments) {
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

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            return segment.getSurface();
        }
    }

    private static class RouteSegmentSmoothnessStatisticComputer extends RouteStatisticComputer {

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            return segment.getSmoothness();
        }
    }

    private static class RouteSegmentClassStatisticComputer extends RouteStatisticComputer {

        @Override
        public String getAttribute(RouteSegmentResult segment) {
            return segment.getHighway();
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
}
