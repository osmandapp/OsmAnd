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

public abstract class ProjMath {

    /**
     * North pole latitude in radians.
     */
    public static final transient float NORTH_POLE_F = MoreMath.HALF_PI;

    /**
     * South pole latitude in radians.
     */
    public static final transient float SOUTH_POLE_F = -NORTH_POLE_F;

    /**
     * North pole latitude in radians.
     */
    public static final transient double NORTH_POLE_D = MoreMath.HALF_PI_D;

    /**
     * North pole latitude in degrees.
     */
    public static final transient double NORTH_POLE_DEG_D = 90d;

    /**
     * South pole latitude in radians.
     */
    public static final transient double SOUTH_POLE_D = -NORTH_POLE_D;

    /**
     * South pole latitude in degrees.
     */
    public static final transient double SOUTH_POLE_DEG_D = -NORTH_POLE_DEG_D;

    /**
     * Dateline longitude in radians.
     */
    public static final transient float DATELINE_F = (float) Math.PI;

    /**
     * Dateline longitude in radians.
     */
    public static final transient double DATELINE_D = Math.PI;

    /**
     * Dateline longitude in degrees.
     */
    public static final transient double DATELINE_DEG_D = 180d;

    /**
     * Longitude range in radians.
     */
    public static final transient float LON_RANGE_F = MoreMath.TWO_PI;

    /**
     * Longitude range in radians.
     */
    public static final transient double LON_RANGE_D = MoreMath.TWO_PI_D;

    /**
     * Longitude range in degrees.
     */
    public static final transient double LON_RANGE_DEG_D = 360d;

    public static final double DEGREES_TO_MILS = 17.77777777777777777778;

    /**
     * rounds the quantity away from 0.
     * 
     * @param x
     *            in value
     * @return double
     * @see #qint(double)
     */
    public static final double roundAdjust(double x) {
        return qint_old(x);
    }

    /**
     * Rounds the quantity away from 0.
     * 
     * @param x
     *            value
     * @return double
     */
    public static final double qint(double x) {
        return qint_new(x);
    }

    private static final double qint_old(double x) {
        return (((int) x) < 0) ? (x - 0.5) : (x + 0.5);
    }

    private static final double qint_new(double x) {
        // -1 or +1 away from zero
        return (x <= 0.0) ? (x - 1.0) : (x + 1.0);
    }

    /**
     * Calculate the shortest arc distance between two lons.
     * 
     * @param lon1
     *            radians
     * @param lon2
     *            radians
     * @return float distance
     */
    public static final float lonDistance(float lon1, float lon2) {
        return (float) Math.min(Math.abs(lon1 - lon2), ((lon1 < 0) ? lon1 + Math.PI : Math.PI - lon1) + ((lon2 < 0) ? lon2 + Math.PI : Math.PI - lon2));
    }

    /**
     * Convert between decimal degrees and scoords.
     * 
     * @param deg
     *            degrees
     * @return long scoords
     * 
     */
    public static final long DEG_TO_SC(double deg) {
        return (long) (deg * 3600000);
    }

    /**
     * Convert between decimal degrees and scoords.
     * 
     * @param sc
     *            scoords
     * @return double decimal degrees
     */
    public static final double SC_TO_DEG(int sc) {
        return ((sc) / (60.0 * 60.0 * 1000.0));
    }

    /**
     * Convert radians to mils.
     * 
     * @param rad
     *            radians
     * @return double mils
     */
    public static final double radToMils(double rad) {
        double degrees = Math.toDegrees(rad);
        degrees = (degrees < 0) ? 360 + degrees : degrees;
        return degrees * DEGREES_TO_MILS;
    }

    /**
     * Convert radians to degrees.
     * 
     * @param rad
     *            radians
     * @return double decimal degrees
     */
    public static final double radToDeg(double rad) {
        return Math.toDegrees(rad);
    }

    /**
     * Convert radians to degrees.
     * 
     * @param rad
     *            radians
     * @return float decimal degrees
     */
    public static final float radToDeg(float rad) {
        return (float) Math.toDegrees(rad);
    }

    /**
     * Convert degrees to radians.
     * 
     * @param deg
     *            degrees
     * @return double radians
     */
    public static final double degToRad(double deg) {
        return Math.toRadians(deg);
    }

    /**
     * Convert degrees to radians.
     * 
     * @param deg
     *            degrees
     * @return float radians
     */
    public static final float degToRad(float deg) {
        return (float) Math.toRadians(deg);
    }

    /**
     * Generate a hashCode value for a lat/lon pair.
     * 
     * @param lat
     *            latitude
     * @param lon
     *            longitude
     * @return int hashcode
     * 
     */
    public static final int hashLatLon(float lat, float lon) {
        if (lat == -0f) lat = 0f;// handle negative zero (anything else?)
        if (lon == -0f) lon = 0f;
        int tmp = Float.floatToIntBits(lat);
        int hash = (tmp << 5) | (tmp >> 27);// rotate the lat bits
        return hash ^ Float.floatToIntBits(lon);// XOR with lon
    }

    /**
     * Converts an array of decimal degrees float lat/lons to float radians in
     * place.
     * 
     * @param degs
     *            float[] lat/lons in decimal degrees
     * @return float[] lat/lons in radians
     */
    public static final float[] arrayDegToRad(float[] degs) {
        for (int i = 0; i < degs.length; i++) {
            degs[i] = degToRad(degs[i]);
        }
        return degs;
    }

    /**
     * Converts an array of radian float lat/lons to decimal degrees in place.
     * 
     * @param rads
     *            float[] lat/lons in radians
     * @return float[] lat/lons in decimal degrees
     */
    public static final float[] arrayRadToDeg(float[] rads) {
        for (int i = 0; i < rads.length; i++) {
            rads[i] = radToDeg(rads[i]);
        }
        return rads;
    }

    /**
     * Converts an array of decimal degrees double lat/lons to double radians in
     * place.
     * 
     * @param degs
     *            double[] lat/lons in decimal degrees
     * @return double[] lat/lons in radians
     */
    public static final double[] arrayDegToRad(double[] degs) {
        for (int i = 0; i < degs.length; i++) {
            degs[i] = degToRad(degs[i]);
        }
        return degs;
    }

    /**
     * Converts an array of radian double lat/lons to decimal degrees in place.
     * 
     * @param rads
     *            double[] lat/lons in radians
     * @return double[] lat/lons in decimal degrees
     */
    public static final double[] arrayRadToDeg(double[] rads) {
        for (int i = 0; i < rads.length; i++) {
            rads[i] = radToDeg(rads[i]);
        }
        return rads;
    }

    /**
     * Normalizes radian latitude. Normalizes latitude if at or exceeds epsilon
     * distance from a pole.
     * 
     * @param lat
     *            float latitude in radians
     * @param epsilon
     *            epsilon (&gt;= 0) radians distance from pole
     * @return float latitude (-PI/2 &lt;= phi &lt;= PI/2)
     * @see Proj#normalizeLatitude(float)
     * @see com.bbn.openmap.LatLonPoint#normalizeLatitude(float)
     */
    public static final float normalizeLatitude(float lat, float epsilon) {
        if (lat > NORTH_POLE_F - epsilon) {
            return NORTH_POLE_F - epsilon;
        } else if (lat < SOUTH_POLE_F + epsilon) {
            return SOUTH_POLE_F + epsilon;
        }
        return lat;
    }

    /**
     * Normalizes radian latitude. Normalizes latitude if at or exceeds epsilon
     * distance from a pole.
     * 
     * @param lat
     *            double latitude in radians
     * @param epsilon
     *            epsilon (&gt;= 0) radians distance from pole
     * @return double latitude (-PI/2 &lt;= phi &lt;= PI/2)
     * @see Proj#normalizeLatitude(float)
     */
    public static final double normalizeLatitude(double lat, double epsilon) {
        if (lat > NORTH_POLE_D - epsilon) {
            return NORTH_POLE_D - epsilon;
        } else if (lat < SOUTH_POLE_D + epsilon) {
            return SOUTH_POLE_D + epsilon;
        }
        return lat;
    }

    /**
     * Sets radian longitude to something sane.
     * 
     * @param lon
     *            float longitude in radians
     * @return float longitude (-PI &lt;= lambda &lt; PI)
     */
    public static final float wrapLongitude(float lon) {
        if ((lon < -DATELINE_F) || (lon > DATELINE_F)) {
            lon += DATELINE_F;
            lon %= LON_RANGE_F;
            lon += (lon < 0) ? DATELINE_F : -DATELINE_F;
        }
        return lon;
    }

    /**
     * Sets radian longitude to something sane.
     * 
     * @param lon
     *            double longitude in radians
     * @return double longitude (-PI &lt;= lambda &lt; PI)
     * @see #wrapLongitude(float)
     */
    public static final double wrapLongitude(double lon) {
        if ((lon < -DATELINE_D) || (lon > DATELINE_D)) {
            lon += DATELINE_D;
            lon %= LON_RANGE_D;
            lon += (lon < 0) ? DATELINE_D : -DATELINE_D;
        }
        return lon;
    }

    /**
     * Sets degree longitude to something sane.
     * 
     * @param lon
     *            double longitude in degrees
     * @return double longitude (-180 &lt;= lambda &lt; 180)
     */
    public static final double wrapLongitudeDeg(double lon) {
        if ((lon < -DATELINE_DEG_D) || (lon > DATELINE_DEG_D)) {
            lon += DATELINE_DEG_D;
            lon %= LON_RANGE_DEG_D;
            lon += (lon < 0) ? DATELINE_DEG_D : -DATELINE_DEG_D;
        }
        return lon;
    }

    /**
     * Converts units (km, nm, miles, etc) to decimal degrees for a spherical
     * planet. This does not check for arc distances &gt; 1/2 planet
     * circumference, which are better represented as (2pi - calculated arc).
     * 
     * @param u
     *            units float value
     * @param uCircumference
     *            units circumference of planet
     * @return float decimal degrees
     */
    public static final float sphericalUnitsToDeg(float u, float uCircumference) {
        return 360f * (u / uCircumference);
    }

    /**
     * Converts units (km, nm, miles, etc) to arc radians for a spherical
     * planet. This does not check for arc distances &gt; 1/2 planet
     * circumference, which are better represented as (2pi - calculated arc).
     * 
     * @param u
     *            units float value
     * @param uCircumference
     *            units circumference of planet
     * @return float arc radians
     */
    public static final float sphericalUnitsToRad(float u, float uCircumference) {
        return MoreMath.TWO_PI * (u / uCircumference);
    }

    /**
     * Calculate the geocentric latitude given a geographic latitude. According
     * to John Synder: <br>
     * "The geographic or geodetic latitude is the angle which a line
     * perpendicular to the surface of the ellipsoid at the given point makes
     * with the plane of the equator. ...The geocentric latitude is the angle
     * made by a line to the center of the ellipsoid with the equatorial plane".
     * ( <i>Map Projections --A Working Manual </i>, p 13)
     * <p>
     * Translated from Ken Anderson's lisp code <i>Freeing the Essence of
     * Computation </i>
     * 
     * @param lat
     *            float geographic latitude in radians
     * @param flat
     *            float flatening factor
     * @return float geocentric latitude in radians
     * @see #geographic_latitude
     */
    public static final float geocentricLatitude(float lat, float flat) {
        float f = 1.0f - flat;
        return (float) Math.atan((f * f) * (float) Math.tan(lat));
    }

    /**
     * Calculate the geographic latitude given a geocentric latitude. Translated
     * from Ken Anderson's lisp code <i>Freeing the Essence of Computation </i>
     * 
     * @param lat
     *            float geocentric latitude in radians
     * @param flat
     *            float flatening factor
     * @return float geographic latitude in radians
     * @see #geocentric_latitude
     */
    public static final float geographicLatitude(float lat, float flat) {
        float f = 1.0f - flat;
        return (float) Math.atan((float) Math.tan(lat) / (f * f));
    }

    /**
     * Generic test for seeing if an left longitude value and a right longitude
     * value seem to constitute crossing the dateline.
     * 
     * @param leftLon
     *            the leftmost longitude, in decimal degrees. Expected to
     *            represent the location of the left side of a map window.
     * @param rightLon
     *            the rightmost longitude, in decimal degrees. Expected to
     *            represent the location of the right side of a map window.
     * @param projScale
     *            the projection scale, considered if the two values are very
     *            close to each other and leftLon less than rightLon.
     * @return true if it seems like these two longitude values represent a
     *         dateline crossing.
     */
    public static boolean isCrossingDateline(double leftLon, double rightLon, float projScale) {
        // if the left longitude is greater than the right, we're obviously
        // crossing the dateline. If they are approximately equal, we could be
        // showing the whole earth, but only if the scale is significantly
        // large. If the scale is small, we could be really zoomed in.
        return ((leftLon > rightLon) || (MoreMath.approximately_equal(leftLon, rightLon, .001f) && projScale > 1000000f));
    }
}