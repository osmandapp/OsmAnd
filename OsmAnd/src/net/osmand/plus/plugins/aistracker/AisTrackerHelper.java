package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_CPA;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_TCPA;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;

public final class AisTrackerHelper {

    private static class Vector {
        public double x;
        public double y;
        public Vector(double a, double b) {
            this.x = a;
            this.y = b;
        }
        public Vector(@NonNull Vector a) {
            this.x = a.x;
            this.y = a.y;
        }
        @NonNull
        public Vector multiply(double a) {
            return new Vector(this.x * a, this.y * a);
        }
        @NonNull
        public Vector add(@NonNull Vector a) {
            return new Vector(this.x + a.x, this.y + a.y);
        }
    }

    /* calculate the Time to Closest Point of Approach (TCPA) of two moving objects:
    *  object 1 at position x and velocity vector vx
    *  object 2 at position y and velocity vectoy vy,
    *  For the calculation, cartesian ccordinates are assumed with a cartesian distance metricx
    *  -> attention: by using sherical coordinates, this will produce an error! */
    private double getTcpa(@NonNull Vector x, @NonNull Vector y, @NonNull Vector vx, @NonNull Vector vy) {
        Vector dx = new Vector(y.x - x.x, y.y - x.y);
        Vector dv = new Vector(vy.x - vx.x, vy.y - vx.y);
        return -(((dx.x * dv.x) + (dx.y * dv.y)) / ((dv.x * dv.x) + (dv.y * dv.y))); // TODO: check for Div/0
    }

    /* to calculate the Time to Closest Point of Approach (TCPA) between the objects x and y,
    *  it is presumed that x and y both contain their position, speed and course */
    public double getTcpa(@NonNull Location x, @NonNull Location y) {
        if (checkSpeedAndBearing(x, y)) {
            return INVALID_TCPA;
        }
        Vector vx = courseToVector(x.getBearing(), getSpeedInNodes(x));
        Vector vy = courseToVector(y.getBearing(), getSpeedInNodes(y));
        return getTcpa(locationToVector(x), locationToVector(y), vx, vy);
    }

    /* to calculate the Time to Closest Point of Approach (TCPA) between the objects x and own location,
     *  it is presumed that x contains its position, speed and course */
    public double getTcpa(@NonNull Location x, @Nullable OsmAndLocationProvider locationProvider) {
        if (locationProvider != null) {
            Location myLocation = locationProvider.getLastKnownLocation();
            if (myLocation != null) {
                return getTcpa(x, myLocation);
            }
        }
        return INVALID_TCPA;
    }
    @Nullable
    private Location getCpa(@NonNull Location x, @NonNull Location y, boolean useFirstAsReference) {
        if (checkSpeedAndBearing(x, y)) {
            return null;
        }
        double tcpa = getTcpa(x,y);
        Location base = useFirstAsReference ? x : y;
        Vector v = courseToVector(base.getBearing(), getSpeedInNodes(base));
        Vector newPos = getNewPosition(locationToVector(base), v, tcpa);
        Location newX = new Location(base);
        newX.setLongitude(newPos.x);
        newX.setLatitude(newPos.y);
        return newX;
    }

    /* to calculate the Closest Point of Approach (CPA) between the objects x and y,
     * it is presumed that x and y both contain their position, speed and course.
     * This function returns the position of first object x at time of TCPA */
    @Nullable
    public Location getCpa1(@NonNull Location x, @NonNull Location y) {
        return getCpa(x, y, true);
    }
    /* to calculate the Closest Point of Approach (CPA) between the objects x and y,
     * it is presumed that x and y both contain their position, speed and course.
     * This function returns the position of second object y at time of TCPA */
    @Nullable
    public Location getCpa2(@NonNull Location x, @NonNull Location y) {
        return getCpa(x, y, false);
    }

    /* caluclate the distance between the given objects at their Closest Point of Approach (CPA)*/
    public float getCpaDistance(@NonNull Location x, @NonNull Location y) {
        Location cpaX = getCpa1(x,y);
        Location cpaY = getCpa2(x,y);
        if ((cpaX != null) && (cpaY != null)) {
            return cpaX.distanceTo(cpaY);
        } else {
            return INVALID_CPA;
        }
    }

    /* caluclate the distance between the given object and own position at their Closest Point of Approach (CPA)*/
    public float getCpaDistance(@NonNull Location x, @Nullable OsmAndLocationProvider locationProvider) {
        if (locationProvider != null) {
            Location myLocation = locationProvider.getLastKnownLocation();
            if (myLocation != null) {
                return getCpaDistance(x, myLocation);
            }
        }
        return INVALID_CPA;
    }

    /* calculate the new position of a moving object with coordinates x and velocity vector v
       after the given time,
    *  -> attention: by using sherical coordinates, this will produce an error! */
    @NonNull
    private Vector getNewPosition(@NonNull Vector x, @NonNull Vector v, double time) {
        return new Vector(x.add(v.multiply(time)));
    }

    /* calculate a velocity vector from givem course (COG) and speed (SOG).
       COG is given as heading, SOG as scalar */
    @NonNull
    private Vector courseToVector(double cog, double sog) {
        double alpha = cog + 90.0d;
        while (alpha < 0) { alpha += 360.0d; }
        while (alpha > 360.0d ) { alpha -= 360.0d; }
        alpha = Math.toRadians(alpha);
        return new Vector(Math.sin(alpha) * sog, Math.cos(alpha) * sog);
    }

    @NonNull
    private Vector locationToVector(@NonNull Location loc) {
        return new Vector(loc.getLongitude(), loc.getLatitude());
    }
    private boolean checkSpeedAndBearing(@NonNull Location x, @NonNull Location y) {
        if (!x.hasBearing() || !y.hasBearing() || !x.hasSpeed() || !y.hasSpeed()) {
            Log.d("AisTrackerHelper", "some input data is missing: x.hasBearing->"
                    + x.hasBearing() + ", y.hasBearing->" + y.hasBearing() + ", x.hasSpeed->"
                    + x.hasSpeed() + ", y.hasSpeed" + y.hasSpeed());
            return true;
        } else {
            return false;
        }
    }

    private float getSpeedInNodes(@NonNull Location loc) {
        return loc.getSpeed() * 1852 / 3600;
    }
}
