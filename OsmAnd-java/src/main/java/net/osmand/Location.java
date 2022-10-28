package net.osmand;
/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * A class representing a geographic location sensed at a particular
 * time (a "fix").  A location consists of a latitude and longitude, a
 * UTC timestamp. and optionally information on altitude, speed, and
 * bearing.
 *
 * <p> Information specific to a particular provider or class of
 * providers may be communicated to the application using getExtras,
 * which returns a Bundle of key/value pairs.  Each provider will only
 * provide those entries for which information is available.
 */
public class Location {
    
    private String mProvider;
    private long mTime = 0;
    private double mLatitude = 0.0;
    private double mLongitude = 0.0;
    private boolean mHasAltitude = false;
    private double mAltitude = 0.0f;
    private boolean mHasSpeed = false;
    private float mSpeed = 0.0f;
    private boolean mHasBearing = false;
    private float mBearing = 0.0f;
    private boolean mHasAccuracy = false;
    private float mAccuracy = 0.0f;
    private boolean mHasVerticalAccuracy = false;
    private float mVerticalAccuracy = 0.0f;

    // Cache the inputs and outputs of computeDistanceAndBearing
    // so calls to distanceTo() and bearingTo() can share work
    private double mLat1 = 0.0;
    private double mLon1 = 0.0;
    private double mLat2 = 0.0;
    private double mLon2 = 0.0;
    private float mDistance = 0.0f;
    private float mInitialBearing = 0.0f;
    // Scratchpad
    private float[] mResults = new float[2];
    
    /**
     * Constructs a new Location.  By default, time, latitude,
     * longitude, and numSatellites are 0; hasAltitude, hasSpeed, and
     * hasBearing are false; and there is no extra information.
     *
     * @param provider the name of the location provider that generated this
     * location fix.
     */
    public Location(String provider) {
        mProvider = provider;
    }

	public Location(String provider, double lat, double lon) {
		mProvider = provider;
		setLatitude(lat);
		setLongitude(lon);
	}

    /**
     * Constructs a new Location object that is a copy of the given
     * location.
     */
    public Location(Location l) {
        set(l);
    }

    /**
     * Sets the contents of the location to the values from the given location.
     */
    public void set(Location l) {
        mProvider = l.mProvider;
        mTime = l.mTime;
        mLatitude = l.mLatitude;
        mLongitude = l.mLongitude;
        mHasAltitude = l.mHasAltitude;
        mAltitude = l.mAltitude;
        mHasSpeed = l.mHasSpeed;
        mSpeed = l.mSpeed;
        mHasBearing = l.mHasBearing;
        mBearing = l.mBearing;
        mHasAccuracy = l.mHasAccuracy;
        mAccuracy = l.mAccuracy;
        mHasVerticalAccuracy = l.mHasVerticalAccuracy;
        mVerticalAccuracy = l.mVerticalAccuracy;
    }

    /**
     * Clears the contents of the location.
     */
    public void reset() {
        mProvider = null;
        mTime = 0;
        mLatitude = 0;
        mLongitude = 0;
        mHasAltitude = false;
        mAltitude = 0;
        mHasSpeed = false;
        mSpeed = 0;
        mHasBearing = false;
        mBearing = 0;
        mHasAccuracy = false;
        mAccuracy = 0;
    }

    private static void computeDistanceAndBearing(double lat1, double lon1,
        double lat2, double lon2, float[] results) {
        // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
        // using the "Inverse Formula" (section 4)
        int MAXITERS = 20;
        // Convert lat/long to radians
        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;

        double a = 6378137.0; // WGS84 major axis
        double b = 6356752.3142; // WGS84 semi-major axis
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

        double L = lon2 - lon1;
        double A = 0.0;
        double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(U2);
        double sinU1 = Math.sin(U1);
        double sinU2 = Math.sin(U2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;

        double sigma = 0.0;
        double deltaSigma = 0.0;
        double cosSqAlpha = 0.0;
        double cos2SM = 0.0;
        double cosSigma = 0.0;
        double sinSigma = 0.0;
        double cosLambda = 0.0;
        double sinLambda = 0.0;

        double lambda = L; // initial guess
        for (int iter = 0; iter < MAXITERS; iter++) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2; // (14)
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
            sigma = Math.atan2(sinSigma, cosSigma); // (16)
            double sinAlpha = (sinSigma == 0) ? 0.0 :
                cosU1cosU2 * sinLambda / sinSigma; // (17)
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 :
                cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
            A = 1 + (uSquared / 16384.0) * // (3)
                (4096.0 + uSquared *
                 (-768 + uSquared * (320.0 - 175.0 * uSquared)));
            double B = (uSquared / 1024.0) * // (4)
                (256.0 + uSquared *
                 (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
            double C = (f / 16.0) *
                cosSqAlpha *
                (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = B * sinSigma * // (6)
                (cos2SM + (B / 4.0) *
                 (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                  (B / 6.0) * cos2SM *
                  (-3.0 + 4.0 * sinSigma * sinSigma) *
                  (-3.0 + 4.0 * cos2SMSq)));

            lambda = L +
                (1.0 - C) * f * sinAlpha *
                (sigma + C * sinSigma *
                 (cos2SM + C * cosSigma *
                  (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) {
                break;
            }
        }

        float distance = (float) (b * A * (sigma - deltaSigma));
        results[0] = distance;
        if (results.length > 1) {
            float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
                cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
            initialBearing *= 180.0 / Math.PI;
            results[1] = initialBearing;
            if (results.length > 2) {
                float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
                    -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
                finalBearing *= 180.0 / Math.PI;
                results[2] = finalBearing;
            }
        }
    }

    /**
     * Computes the approximate distance in meters between two
     * locations, and optionally the initial and final bearings of the
     * shortest path between them.  Distance and bearing are defined using the
     * WGS84 ellipsoid.
     *
     * <p> The computed distance is stored in results[0].  If results has length
     * 2 or greater, the initial bearing is stored in results[1]. If results has
     * length 3 or greater, the final bearing is stored in results[2].
     *
     * @param startLatitude the starting latitude
     * @param startLongitude the starting longitude
     * @param endLatitude the ending latitude
     * @param endLongitude the ending longitude
     * @param results an array of floats to hold the results
     *
     * @throws IllegalArgumentException if results is null or has length < 1
     */
    public static void distanceBetween(double startLatitude, double startLongitude,
        double endLatitude, double endLongitude, float[] results) {
        if (results == null || results.length < 1) {
            throw new IllegalArgumentException("results is null or has length < 1");
        }
        computeDistanceAndBearing(startLatitude, startLongitude,
            endLatitude, endLongitude, results);
    }

    /**
     * Returns the approximate distance in meters between this
     * location and the given location.  Distance is defined using
     * the WGS84 ellipsoid.
     *
     * @param dest the destination location
     * @return the approximate distance in meters
     */
    public float distanceTo(Location dest) {
        // See if we already have the result
        synchronized (mResults) {
            if (mLatitude != mLat1 || mLongitude != mLon1 ||
                dest.mLatitude != mLat2 || dest.mLongitude != mLon2) {
                computeDistanceAndBearing(mLatitude, mLongitude,
                    dest.mLatitude, dest.mLongitude, mResults);
                mLat1 = mLatitude;
                mLon1 = mLongitude;
                mLat2 = dest.mLatitude;
                mLon2 = dest.mLongitude;
                mDistance = mResults[0];
                mInitialBearing = mResults[1];
            }
            return mDistance;
        }
    }

    /**
     * Returns the approximate initial bearing in degrees East of true
     * North when traveling along the shortest path between this
     * location and the given location.  The shortest path is defined
     * using the WGS84 ellipsoid.  Locations that are (nearly)
     * antipodal may produce meaningless results.
     *
     * @param dest the destination location
     * @return the initial bearing in degrees
     */
    public float bearingTo(Location dest) {
        synchronized (mResults) {
            // See if we already have the result
            if (mLatitude != mLat1 || mLongitude != mLon1 ||
                            dest.mLatitude != mLat2 || dest.mLongitude != mLon2) {
                computeDistanceAndBearing(mLatitude, mLongitude,
                    dest.mLatitude, dest.mLongitude, mResults);
                mLat1 = mLatitude;
                mLon1 = mLongitude;
                mLat2 = dest.mLatitude;
                mLon2 = dest.mLongitude;
                mDistance = mResults[0];
                mInitialBearing = mResults[1];
            }
            return mInitialBearing;
        }
    }

    /**
     * Returns the name of the provider that generated this fix,
     * or null if it is not associated with a provider.
     */
    public String getProvider() {
        return mProvider;
    }

    /**
     * Sets the name of the provider that generated this fix.
     */
    public void setProvider(String provider) {
        mProvider = provider;
    }

    /**
     * Returns the UTC time of this fix, in milliseconds since January 1,
     * 1970.
     */
    public long getTime() {
        return mTime;
    }

    /**
     * Sets the UTC time of this fix, in milliseconds since January 1,
     * 1970.
     */
    public void setTime(long time) {
        mTime = time;
    }

    /**
     * Returns the latitude of this fix.
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Sets the latitude of this fix.
     */
    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    /**
     * Returns the longitude of this fix.
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * Sets the longitude of this fix.
     */
    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    /**
     * Returns true if this fix contains altitude information, false
     * otherwise.
     */
    public boolean hasAltitude() {
        return mHasAltitude;
    }

    /**
     * Returns the altitude of this fix.  If {@link #hasAltitude} is false,
     * 0.0f is returned.
     */
    public double getAltitude() {
        return mAltitude;
    }

    /**
     * Sets the altitude of this fix.  Following this call,
     * hasAltitude() will return true.
     */
    public void setAltitude(double altitude) {
        mAltitude = altitude;
        mHasAltitude = true;
    }

    /**
     * Clears the altitude of this fix.  Following this call,
     * hasAltitude() will return false.
     */
    public void removeAltitude() {
        mAltitude = 0.0f;
        mHasAltitude = false;
    }

    /**
     * Returns true if this fix contains speed information, false
     * otherwise.  The default implementation returns false.
     */
    public boolean hasSpeed() {
        return mHasSpeed;
    }

    /**
     * Returns the speed of the device over ground in meters/second.
     * If hasSpeed() is false, 0.0f is returned.
     */
    public float getSpeed() {
        return mSpeed;
    }

    /**
     * Sets the speed of this fix, in meters/second.  Following this
     * call, hasSpeed() will return true.
     */
    public void setSpeed(float speed) {
        mSpeed = speed;
        mHasSpeed = true;
    }

    /**
     * Clears the speed of this fix.  Following this call, hasSpeed()
     * will return false.
     */
    public void removeSpeed() {
        mSpeed = 0.0f;
        mHasSpeed = false;
    }

    /**
     * Returns true if the provider is able to report bearing information,
     * false otherwise.  The default implementation returns false.
     */
    public boolean hasBearing() {
        return mHasBearing;
    }

    /**
     * Returns the direction of travel in degrees East of true
     * North. If hasBearing() is false, 0.0 is returned.
     */
    public float getBearing() {
        return mBearing;
    }

    /**
     * Sets the bearing of this fix.  Following this call, hasBearing()
     * will return true.
     */
    public void setBearing(float bearing) {
        while (bearing < 0.0f) {
            bearing += 360.0f;
        }
        while (bearing >= 360.0f) {
            bearing -= 360.0f;
        }
        mBearing = bearing;
        mHasBearing = true;
    }

    /**
     * Clears the bearing of this fix.  Following this call, hasBearing()
     * will return false.
     */
    public void removeBearing() {
        mBearing = 0.0f;
        mHasBearing = false;
    }

    /**
     * Returns true if the provider is able to report accuracy information,
     * false otherwise.  The default implementation returns false.
     */
    public boolean hasAccuracy() {
        return mHasAccuracy;
    }

    /**
     * Returns the accuracy of the fix in meters. If hasAccuracy() is false,
     * 0.0 is returned.
     */
    public float getAccuracy() {
        return mAccuracy;
    }

    /**
     * Sets the accuracy of this fix.  Following this call, hasAccuracy()
     * will return true.
     */
    public void setAccuracy(float accuracy) {
        mAccuracy = accuracy;
        mHasAccuracy = true;
    }

    /**
     * Clears the accuracy of this fix.  Following this call, hasAccuracy()
     * will return false.
     */
    public void removeAccuracy() {
        mAccuracy = 0.0f;
        mHasAccuracy = false;
    }


    /**
     * Returns true if the provider is able to report vertical accuracy information,
     * false otherwise.  The default implementation returns false.
     */
    public boolean hasVerticalAccuracy() {
        return mHasVerticalAccuracy;
    }

    /**
     * Returns the accuracy of the fix in meters. If hasVerticalAccuracy() is false,
     * 0.0 is returned.
     */
    public float getVerticalAccuracy() {
        return mVerticalAccuracy;
    }

    /**
     * Sets the accuracy of this fix.  Following this call, hasVerticalAccuracy()
     * will return true.
     */
    public void setVerticalAccuracy(float verticalAccuracy) {
        this.mVerticalAccuracy = verticalAccuracy;
        mHasVerticalAccuracy = true;
    }

    /**
     * Clears the vertical accuracy of this fix.  Following this call, hasVerticalAccuracy()
     * will return false.
     */
    public void removeVerticalAccuracy() {
        mVerticalAccuracy = 0.0f;
        mHasVerticalAccuracy = false;
    }

    @Override public String toString() {
        return "Location[mProvider=" + mProvider +
            ",mTime=" + mTime +
            ",mLatitude=" + mLatitude +
            ",mLongitude=" + mLongitude +
            ",mHasAltitude=" + mHasAltitude +
            ",mAltitude=" + mAltitude +
            ",mHasSpeed=" + mHasSpeed +
            ",mSpeed=" + mSpeed +
            ",mHasBearing=" + mHasBearing +
            ",mBearing=" + mBearing +
            ",mHasAccuracy=" + mHasAccuracy +
            ",mAccuracy=" + mAccuracy +
            ",mHasVerticalAccuracy=" + mHasVerticalAccuracy +
            ",mVerticalAccuracy=" + mVerticalAccuracy;
    }

    

    
}
