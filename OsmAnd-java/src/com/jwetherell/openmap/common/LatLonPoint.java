
// **********************************************************************
//
// <copyright>
//
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
//
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
//
// </copyright>
// **********************************************************************

package com.jwetherell.openmap.common;

public class LatLonPoint {

    public final static double NORTH_POLE = 90.0;
    public final static double SOUTH_POLE = -NORTH_POLE;
    public final static double DATELINE = 180.0;
    public final static double LON_RANGE = 360.0;
    public final static double EQUIVALENT_TOLERANCE = 0.00001;

    protected double lat;
    protected double lon;
    protected transient double radLat;
    protected transient double radLon;

    /**
     * Default constructor, values set to 0, 0.
     */
    public LatLonPoint() {
    }

    /**
     * Set the latitude, longitude for this point in decimal degrees.
     * 
     * @param lat
     *            latitude
     * @param lon
     *            longitude.
     */
    public LatLonPoint(double lat, double lon) {
        setLatLon(lat, lon, false);
    }

    /**
     * Set the latitude, longitude for this point, with the option of noting
     * whether the values are in degrees or radians.
     * 
     * @param lat
     *            latitude
     * @param lon
     *            longitude.
     * @param isRadians
     *            true of values are radians.
     */
    public LatLonPoint(double lat, double lon, boolean isRadian) {
        setLatLon(lat, lon, isRadian);
    }

    /**
     * Create Double version from another LatLonPoint.
     * 
     * @param llp
     */
    public LatLonPoint(LatLonPoint llp) {
        setLatLon(llp.getY(), llp.getX(), false);
    }

    /**
     * Point2D method, inheriting signature!!
     * 
     * @param x
     *            longitude value in decimal degrees.
     * @param y
     *            latitude value in decimal degrees.
     */
    public void setLocation(double x, double y) {
        setLatLon(y, x, false);
    }

    /**
     * Set latitude and longitude.
     * 
     * @param lat
     *            latitude in decimal degrees.
     * @param lon
     *            longitude in decimal degrees.
     */
    public void setLatLon(double lat, double lon) {
        setLatLon(lat, lon, false);
    }

    /**
     * Set latitude and longitude.
     * 
     * @param lat
     *            latitude.
     * @param lon
     *            longitude.
     * @param isRadians
     *            true if lat/lon values are radians.
     */
    public void setLatLon(double lat, double lon, boolean isRadians) {
        if (isRadians) {
            radLat = lat;
            radLon = lon;
            this.lat = ProjMath.radToDeg(lat);
            this.lon = ProjMath.radToDeg(lon);
        } else {
            this.lat = normalizeLatitude(lat);
            this.lon = wrapLongitude(lon);
            radLat = ProjMath.degToRad(lat);
            radLon = ProjMath.degToRad(lon);
        }
    }

    /**
     * @return longitude in decimal degrees.
     */
    public double getX() {
        return lon;
    }

    /**
     * @return latitude in decimal degrees.
     */
    public double getY() {
        return lat;
    }

    /**
     * @return float latitude in decimal degrees.
     */
    public float getLatitude() {
        return (float) lat;
    }

    /**
     * @return float longitude in decimal degrees.
     */
    public float getLongitude() {
        return (float) lon;
    }

    /**
     * @return radian longitude.
     */
    public double getRadLon() {
        return radLon;
    }

    /**
     * @return radian latitude.
     */
    public double getRadLat() {
        return radLat;
    }

    /**
     * Set latitude.
     * 
     * @param lat
     *            latitude in decimal degrees
     */
    public void setLatitude(double lat) {
        this.lat = normalizeLatitude(lat);
        radLat = ProjMath.degToRad(lat);
    }

    /**
     * Set longitude.
     * 
     * @param lon
     *            longitude in decimal degrees
     */
    public void setLongitude(double lon) {
        this.lon = wrapLongitude(lon);
        radLon = ProjMath.degToRad(lon);
    }

    /**
     * Set location values from another lat/lon point.
     * 
     * @param llp
     */
    public void setLatLon(LatLonPoint llp) {
        setLatLon(llp.getY(), llp.getX(), false);
    }

    /**
     * Ensure latitude is between the poles.
     * 
     * @param lat
     * @return
     */
    public final static float normalizeLatitude(float lat) {
        return (float) normalizeLatitude((double) lat);
    }

    /**
     * Sets latitude to something sane.
     * 
     * @param lat
     *            latitude in decimal degrees
     * @return float normalized latitude in decimal degrees (&minus;90&deg; &le;
     *         &phi; &le; 90&deg;)
     */
    public final static double normalizeLatitude(double lat) {
        if (lat > NORTH_POLE) {
            lat = NORTH_POLE;
        }
        if (lat < SOUTH_POLE) {
            lat = SOUTH_POLE;
        }
        return lat;
    }

    /**
     * Ensure the longitude is between the date line.
     * 
     * @param lon
     * @return
     */
    public final static float wrapLongitude(float lon) {
        return (float) wrapLongitude((double) lon);
    }

    /**
     * Sets longitude to something sane.
     * 
     * @param lon
     *            longitude in decimal degrees
     * @return float wrapped longitude in decimal degrees (&minus;180&deg; &le;
     *         &lambda; &le; 180&deg;)
     */
    public final static double wrapLongitude(double lon) {
        if ((lon < -DATELINE) || (lon > DATELINE)) {
            lon += DATELINE;
            lon = lon % LON_RANGE;
            lon = (lon < 0) ? DATELINE + lon : -DATELINE + lon;
        }
        return lon;
    }

    /**
     * Check if latitude is bogus. Latitude is invalid if lat &gt; 90&deg; or if
     * lat &lt; &minus;90&deg;.
     * 
     * @param lat
     *            latitude in decimal degrees
     * @return boolean true if latitude is invalid
     */
    public static boolean isInvalidLatitude(double lat) {
        return ((lat > NORTH_POLE) || (lat < SOUTH_POLE));
    }

    /**
     * Check if longitude is bogus. Longitude is invalid if lon &gt; 180&deg; or
     * if lon &lt; &minus;180&deg;.
     * 
     * @param lon
     *            longitude in decimal degrees
     * @return boolean true if longitude is invalid
     */
    public static boolean isInvalidLongitude(double lon) {
        return ((lon < -DATELINE) || (lon > DATELINE));
    }

    /**
     * Determines whether two LatLonPoints are equal.
     * 
     * @param obj
     *            Object
     * @return Whether the two points are equal up to a tolerance of 10 <sup>-5
     *         </sup> degrees in latitude and longitude.
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LatLonPoint pt = (LatLonPoint) obj;
        return (MoreMath.approximately_equal(getY(), pt.getY(), EQUIVALENT_TOLERANCE) && MoreMath.approximately_equal(getX(), pt.getX(), EQUIVALENT_TOLERANCE));
    }

    /**
     * Find the distance to another LatLonPoint, based on a earth spherical
     * model.
     * 
     * @param toPoint
     *            LatLonPoint
     * @return distance, in radians. You can use an com.bbn.openmap.proj.Length
     *         to convert the radians to other units.
     */
    public double distance(LatLonPoint toPoint) {
        return GreatCircle.sphericalDistance(getRadLat(), getRadLon(), toPoint.getRadLat(), toPoint.getRadLon());
    }

    /**
     * Find the azimuth to another point, based on the spherical earth model.
     * 
     * @param toPoint
     *            LatLonPoint
     * @return the azimuth `Az' east of north from this point bearing toward the
     *         one provided as an argument.(-PI &lt;= Az &lt;= PI).
     * 
     */
    public double azimuth(LatLonPoint toPoint) {
        return GreatCircle.sphericalAzimuth(getRadLat(), getRadLon(), toPoint.getRadLat(), toPoint.getRadLon());
    }

    /**
     * Get a new LatLonPoint a distance and azimuth from another point, based on
     * the spherical earth model.
     * 
     * @param distance
     *            radians
     * @param azimuth
     *            radians
     * @return
     */
    public LatLonPoint getPoint(double distance, double azimuth) {
        return GreatCircle.sphericalBetween(getRadLat(), getRadLon(), distance, azimuth);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Lat=" + lat + ", Lon=" + lon;
    }
}