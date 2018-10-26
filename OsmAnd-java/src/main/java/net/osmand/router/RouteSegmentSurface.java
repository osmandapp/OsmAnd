package net.osmand.router;

public class RouteSegmentSurface {

    private final int index;

    private final String surface;

    private float distance;

    public RouteSegmentSurface(int index, String surface) {
        this.index = index;
        this.surface = surface;
    }

    public int getIndex() {
        return index;
    }

    public String getSurface() {
        return surface;
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
                ", surface='" + surface + '\'' +
                ", distance=" + distance +
                '}';
    }
}
