package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_CPA;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_TCPA;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jwetherell.openmap.common.LatLonPoint;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;

public final class AisTrackerHelper {
    private static long lastCorrectionUpdate = 0;
    private static double correctionFactor = 1.0d;
    private static final long maxCorrectionUpdateAgeInMin = 60;
    private static class Vector {
        public double x; // Latitude (grows in North direction)
        public double y; // Longitude (grows in East direction)
        public Vector(double a, double b) {
            this.x = a;
            this.y = b;
        }
        public Vector(@NonNull Vector a) {
            this.x = a.x;
            this.y = a.y;
        }
        @NonNull
        public Vector sub(@NonNull Vector a) {
            return new Vector(this.x - a.x, this.y - a.y);
        }
        public double dot(@NonNull Vector a) { return (this.x * a.x) + (this.y * a.y); }
    }
    public static class Cpa {
        private double tcpa; // in hours
        private float cpaDist; // in miles
        private Location newPos1; // position of first object at time tcpa
        private Location newPos2; // position of first object at time tcpa
        public Cpa() {
            reset();
        }
        public void reset() {
            cpaDist = INVALID_CPA;
            tcpa = INVALID_TCPA;
            newPos1 = null;
            newPos2 = null;
        }
        public void setTcpa(double x) { this.tcpa = x; }
        public void setCpaDist(float x) { this.cpaDist = x; }
        public void setCpaPos1(Location loc) { this.newPos1 = loc; }
        public void setCpaPos2(Location loc) { this.newPos2 = loc; }
        public double getTcpa() { return tcpa; }
        public float getCpaDist() { return cpaDist; }
        public Location getCpaPos1() { return newPos1; }
        public Location getCpaPos2() { return newPos2; }
    }

    /* calculate the Time to Closest Point of Approach (TCPA) of two moving objects:
    *  object 1 at position x and velocity vector vx
    *  object 2 at position y and velocity vectoy vy,
    *  For the calculation, cartesian ccordinates are assumed with a cartesian distance metricx
    *  -> attention: by using sherical coordinates, this will produce an error! */
    private static double getTcpa(@NonNull Vector x, @NonNull Vector y,
                                  @NonNull Vector vx, @NonNull Vector vy, double lonCorrection) {
        Vector dx = new Vector( y.sub(x));
        Vector dv = new Vector(vy.sub(vx));
        double divisor = dv.dot(dv); // TODO: check for Div/0
        return -(((dx.x * dv.x) + (dx.y * dv.y / lonCorrection)) / divisor); // TODO: check for Div/0
    }

    /* to calculate the Time to Closest Point of Approach (TCPA) between the objects x and y,
    *  it is presumed that x and y both contain their position, speed and course */
    private static double getTcpa(@NonNull Location x, @NonNull Location y, double lonCorrection) {
        if (checkSpeedAndBearing(x, y)) {
            return INVALID_TCPA;
        }
        if (lonCorrection < 0.001) {
            // in this case the lonCorrection is considered invalid -> new calculation
            lonCorrection = getLonCorrection(x);
        }
        return getTcpa(locationToVector(x), locationToVector(y),
                courseToVector(x.getBearing(), getSpeedInKnots(x)),
                courseToVector(y.getBearing(), getSpeedInKnots(y)), lonCorrection);
    }

    public static double getTcpa(@NonNull Location x, @NonNull Location y) {
        return getTcpa(x, y, 0.0d);
    }

    /* to calculate the Time to Closest Point of Approach (TCPA) between the objects x and own location,
     *  it is presumed that x contains its position, speed and course */
    public static double getTcpa(@NonNull Location x, @Nullable OsmAndLocationProvider locationProvider) {
        if (locationProvider != null) {
            Location myLocation = locationProvider.getLastKnownLocation();
            if (myLocation != null) {
                long now = System.currentTimeMillis();
                if (((now - lastCorrectionUpdate) / 1000 / 60) > maxCorrectionUpdateAgeInMin) {
                    lastCorrectionUpdate = now;
                    correctionFactor = getLonCorrection(myLocation);
                }
                return getTcpa(x, myLocation, correctionFactor);
            }
        }
        return INVALID_TCPA;
    }

    @Nullable
    private static Location getCpa(@NonNull Location x, @NonNull Location y, boolean useFirstAsReference) {
        if (checkSpeedAndBearing(x, y)) {
            return null;
        }
        double tcpa = getTcpa(x,y);
        Location base = useFirstAsReference ? x : y;
        return getNewPosition(base, tcpa);
    }

    /* to calculate the Closest Point of Approach (CPA) between the objects x and y,
     * it is presumed that x and y both contain their position, speed and course.
     * This function returns the position of first object x at time of TCPA */
    @Nullable
    public static Location getCpa1(@NonNull Location x, @NonNull Location y) {
        return getCpa(x, y, true);
    }

    /* to calculate the Closest Point of Approach (CPA) between the objects x and y,
     * it is presumed that x and y both contain their position, speed and course.
     * This function returns the position of second object y at time of TCPA */
    @Nullable
    public static Location getCpa2(@NonNull Location x, @NonNull Location y) {
        return getCpa(x, y, false);
    }

    /* caluclate the distance between the given objects at their Closest Point of Approach (CPA) */
    public static float getCpaDistance(@NonNull Location x, @NonNull Location y) {
        Location cpaX = getCpa1(x,y);
        Location cpaY = getCpa2(x,y);
        if ((cpaX != null) && (cpaY != null)) {
            return meterToMiles(cpaX.distanceTo(cpaY));
        } else {
            return INVALID_CPA;
        }
    }

    /* caluclate the distance between the given object and own position at their Closest Point of Approach (CPA) */
    public static float getCpaDistance(@NonNull Location x, @Nullable OsmAndLocationProvider locationProvider) {
        if (locationProvider != null) {
            Location myLocation = locationProvider.getLastKnownLocation();
            if (myLocation != null) {
                return getCpaDistance(x, myLocation);
            }
        }
        return INVALID_CPA;
    }

    public static void getCpa(@NonNull Location loc1, @NonNull Location loc2,
                              @NonNull Cpa result) {
        if (!checkSpeedAndBearing(loc1, loc2)) {
            double tcpa = getTcpa(loc1, loc2);
            Location cpaX = getNewPosition(loc1, tcpa);
            Location cpaY = getNewPosition(loc2, tcpa);
            result.setTcpa(tcpa);
            result.setCpaPos1(cpaX);
            result.setCpaPos2(cpaY);
            if ((cpaX != null) && (cpaY != null)) {
                result.setCpaDist(meterToMiles(cpaX.distanceTo(cpaY)));
            }
        }
    }

    public static void getCpa(@NonNull Location loc, @Nullable OsmAndLocationProvider locationProvider,
                              @NonNull Cpa result) {
        if (locationProvider != null) {
            Location myLocation = locationProvider.getLastKnownLocation();
            if (myLocation != null) {
                getCpa(myLocation, loc, result);
            }
        }
    }

    private static double bearingInRad(float bearingInDegrees) {
        double res = bearingInDegrees * 2 * Math.PI / 360.0;
        while (res >= Math.PI) { res -= (2 * Math.PI); }
        return res;
    }

    @Nullable
    public static Location getNewPosition(@Nullable Location x, double time) {
        if (x != null) {
            if (x.hasBearing() && x.hasSpeed()) {
                LatLonPoint a = new LatLonPoint(x.getLatitude(), x.getLongitude());
                LatLonPoint b = a.getPoint(x.getSpeed() * time * Math.PI / 5556.0, bearingInRad(x.getBearing()));
                Location newX = new Location(x);
                newX.setLongitude(b.getLongitude());
                newX.setLatitude(b.getLatitude());
                return newX;
            } else {
                Log.d("AisTrackerHelper", "getNewPosition(): y.hasBearing->"
                        + x.hasBearing() + ", x.hasSpeed->" + x.hasSpeed());
                return null;
            }
        } else {
            return null;
        }
    }

    private static double getLonCorrection(@Nullable Location loc) {
        if (loc != null) {
            Location x = new Location(loc);
            // simulate a "measurement" trio towards East...
            x.setSpeed(knotsToMeterPerSecond(1.0f)); // speed -> 1 kn
            x.setBearing(90.0f);                           // course -> east
            Location yEast = getNewPosition(x, 1.0);  // new position after 1 hour

            if (yEast != null) {
                double diffLon = yEast.getLongitude() - x.getLongitude();
                return diffLon * 60.0; // correction factor for longitude
            }
        }
        return 1.0f; // fallback
    }

    public static float knotsToMeterPerSecond(float speed) {
        return speed * 1852 / 3600;
    }
    public static float meterPerSecondToKnots(float speed) {
        return speed * 3600 / 1852;
    }

    public static float meterToMiles(float x) {
        return x / 1852.0f;
    }

    /* calculate a velocity vector from givem course (COG) and speed (SOG).
       COG is given as heading, SOG as scalar */
    @NonNull
    private static Vector courseToVector(double cog, double sog) {
        double alpha = 450.0d - cog;
        while (alpha < 0) { alpha += 360.0d; }
        while (alpha >= 360.0d ) { alpha -= 360.0d; }
        alpha = Math.toRadians(alpha);
        return new Vector(Math.sin(alpha) * sog, Math.cos(alpha) * sog);
    }

    @NonNull
    private static Vector locationToVector(@NonNull Location loc) {
        return new Vector(loc.getLatitude() * 60.0, loc.getLongitude() * 60.0);
    }

    private static boolean checkSpeedAndBearing(@NonNull Location x, @NonNull Location y) {
        if (!x.hasBearing() || !y.hasBearing() || !x.hasSpeed() || !y.hasSpeed()) {
            Log.d("AisTrackerHelper", "some input data is missing: x.hasBearing->"
                    + x.hasBearing() + ", y.hasBearing->" + y.hasBearing() + ", x.hasSpeed->"
                    + x.hasSpeed() + ", y.hasSpeed" + y.hasSpeed());
            return true;
        } else {
            return false;
        }
    }

    private static float getSpeedInKnots(@NonNull Location loc) {
        return meterPerSecondToKnots(loc.getSpeed());
    }
}
