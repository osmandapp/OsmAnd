package net.osmand.plus.views;

import net.osmand.plus.GPXUtilities;

public class WptPt2 {

    public double lat;
    public double lon;
    public long time = 0;
    public double ele = Double.NaN;
    public double speed = 0;
    public int colourARGB = 0;					// point colour (used for altitude/speed colouring)
    public double distance = 0.0;				// cumulative distance, if in a track
    public double angle = 0;

    public WptPt2(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public WptPt2(GPXUtilities.WptPt pt)  {
        lat = pt.lat;
        lon = pt.lon;
        time = pt.time;
        ele = pt.ele;
        speed = pt.speed;
        angle = 0;
    }

    public WptPt2(double lat, double lon, long time, double ele, double speed, double distance, double angle) {
        this.lat = lat;
        this.lon = lon;
        this.time = time;
        this.ele = ele;
        this.speed = speed;
        this.distance = distance;
        this.angle = angle;
    }
}
