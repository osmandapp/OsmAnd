package net.osmand.util;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import io.github.cosinekitty.astronomy.Aberration;
import io.github.cosinekitty.astronomy.Astronomy;
import io.github.cosinekitty.astronomy.Body;
import io.github.cosinekitty.astronomy.EquatorEpoch;
import io.github.cosinekitty.astronomy.Equatorial;
import io.github.cosinekitty.astronomy.Observer;
import io.github.cosinekitty.astronomy.Refraction;
import io.github.cosinekitty.astronomy.Time;
import io.github.cosinekitty.astronomy.Topocentric;
import net.osmand.data.LatLon;

public class TestSubsolar {
	static final int MAX_ITERATIONS = 1000;
	static final int ALTITUDE_PRECISION = 1000;
	static final int AZIMUTH_PRECISION = 1000;
	static final double MIN_ALTITUDE = 30;
	
	static boolean PRINT = false;
	
	static class Stats {
		int tests = 0, duplicate = 0, fail = 0;
		Map<Double, Integer> errorDistr = new TreeMap<Double, Integer>();
	}
	// STATS: Precision 0.001, Iterations 1000, Latitude <= 60
	// Errors in meters (TESTS)
	// Alt>= 25 (2091):  {1.0=4, 5.0=73, 10.0=314, 50.0=838, 100.0=848, 500.0=14}
	// Alt>= 20 (2350): {1.0=4, 5.0=74, 10.0=335, 50.0=888, 100.0=948, 500.0=100, 1000.0=1}
	// Alt>= 15 (2650): {1.0=4, 5.0=74, 10.0=356, 50.0=937, 100.0=1021, 500.0=173, 1000.0=34, 5000.0=12, 500000.0=17, 1000000.0=22}
	// Alt>= 10 (2965): {1.0=4, 5.0=74, 10.0=363, 50.0=994, 100.0=1099, 500.0=236, 1000.0=35, 5000.0=47, 500000.0=30, 1000000.0=46, 1.0E7=37}


	// examples
	// https://github.com/cosinekitty/astronomy/blob/master/demo/java/src/main/java/io/github/cosinekitty/astronomy/demo/RiseSetCulm.java
	public static void main(String[] args) throws InterruptedException {
		Body body = Body.Sun;
		double lon = 0;
		Stats s = new Stats();
		for (int m = 1; m <= 12; m++) {
			for (double lat = -60; lat <= 60; lat += 5) {
//		double lat = -60; {
				for (int h = 0; h < 24; h++) {
//			int h = 18; {
					runTest(body, lat, lon, s, m, h);
				}
			}
		}
		System.out.printf("-------\n\nTESTS %d, duplicate %d, failed %d\n ", s.tests, s.duplicate, s.fail);
		System.out.println(s.errorDistr);
	}

	private static void runTest(Body body, double lat, double lon, Stats s, int m, int h) {
		String utcTimeString = String.format("2025-01-%02dT%02d:00:00Z", m, h);
		long timeS = Instant.parse(utcTimeString).getEpochSecond() * 1000;
		// timeS = System.currentTimeMillis();
		Time time = Time.fromMillisecondsSince1970(timeS);
		LatLon pnt = new LatLon(lat, lon);
		Topocentric targetHor = calcAltitude(body, time, pnt, PRINT);
		if (targetHor.getAltitude() < MIN_ALTITUDE) {
			return;
		}
		LatLon subsolar = calculateSubsolar(body, time, PRINT);
		LatLon coords = calculateCoordinatesIteration(body, time, subsolar, targetHor, pnt, PRINT);
		double err = MapUtils.getDistance(coords, pnt);
		double rndErr = roundError(err);
		s.tests++;
		if (!s.errorDistr.containsKey(rndErr)) {
			s.errorDistr.put(rndErr, 1);
		} else {
			s.errorDistr.put(rndErr, 1 + s.errorDistr.get(rndErr));
		}
		if (err > 1_000) {
			s.duplicate++;
			
			System.out.println("\n----------------");
			System.out.println(time.toDateTime());
			System.out.println("SUBSOL  " + subsolar);
			System.out.printf("POINT %s (%.3f bearing, %.3f dist)\n", pnt,
					pnt.toLocation().bearingTo(subsolar.toLocation()), pnt.toLocation().distanceTo(subsolar.toLocation())/ 1000f);
			Topocentric target = calcAltitude(body, time, pnt, true);
			System.out.printf("CALC  %s (%.3f bearing, %.3f dist)\n", coords,
					coords.toLocation().bearingTo(subsolar.toLocation()), coords.toLocation().distanceTo(subsolar.toLocation())/ 1000f);
			Topocentric calc = calcAltitude(body, time, coords, true);
			double altErr = Math.abs(calc.getAltitude() - target.getAltitude());
			double azmErr = Math.abs(Math.abs(calc.getAzimuth() - target.getAzimuth()));
			if (altErr > 1.0d / ALTITUDE_PRECISION  || azmErr > 1.0d / AZIMUTH_PRECISION ) {
				s.fail++;
				System.err.println(altErr + " " + azmErr);
			}
			coords = calculateCoordinatesIteration(body, time, subsolar, targetHor, pnt, true);
			
		}
	}
	
	public static double roundError(double err) {
        // Handle non-positive or very small inputs, returning 1.0 as a floor.
        if (err <= 0) {
            return 1.0;
        }

        // 1. Find the order of magnitude (the power of 10).
        // e.g., for 78, exponent is 1; for 251, exponent is 2.
        double exponent = Math.floor(Math.log10(err));
        double powerOf10 = Math.pow(10, exponent);
        // 2. Normalize the number to be between 1.0 and 10.0.
        // e.g., 78 becomes 7.8; 251 becomes 2.51.
        double normalizedErr = err / powerOf10;

        // 3. Round the normalized number to 1, 5, or 10 based on thresholds.
        double roundedNorm;
        if (normalizedErr < 2.5) {
            roundedNorm = 1.0;
        } else if (normalizedErr < 7.5) {
            roundedNorm = 5.0;
        } else {
            roundedNorm = 10.0;
        }

        // 4. Calculate the result by scaling back up.
        double result = roundedNorm * powerOf10;
        
        // 5. Per the example 0.2 -> 1, if the result is less than 1, return 1.
        return Math.max(1.0, result);
    }
	

	private static Topocentric calcAltitude(Body body, Time time, LatLon point, boolean print) {
		// example calculate for star
//		double dec = 0;
//		double ra = 5.35;
//		double distancely = 10;
//		Astronomy.defineStar(Body.Star1, dec, ra, distancely);
		Observer observer = new Observer(point.getLatitude(), point.getLongitude(), 0.0);
//		Equatorial equ_2000 = Astronomy.equator(body, time, observer, EquatorEpoch.J2000, Aberration.Corrected);
		Equatorial equ = Astronomy.equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected);
		Topocentric hor = Astronomy.horizon(time, observer, equ.getRa(), equ.getDec(), Refraction.None); // Refraction.Normal
		if (print) {
			System.out.printf("%-8s %4.5f° alt, %4.5f°\n", body, hor.getAltitude(), hor.getAzimuth());
		}
				
		return hor;

	}
	
	private static LatLon calculateSubsolar(Body body, Time time, boolean print) {
		Equatorial equ = Astronomy.equator(body, time, new Observer(0, 0, 0.0), EquatorEpoch.OfDate, Aberration.Corrected);
		double greenwichSiderealTime = Astronomy.siderealTime(time);
		double subsolarLatitude = equ.getDec();
        double subsolarLongitude = -(greenwichSiderealTime - equ.getRa()) * 15.0;
        LatLon sublat = align(new LatLon(subsolarLatitude, subsolarLongitude));
		if (print) {
			System.out.printf("Subsolar: %.5f %.5f \n", sublat.getLatitude(), sublat.getLongitude());
		}
		return sublat;
	}
	
	private static LatLon align(LatLon point) {
		double clat = MapUtils.normalizeDegrees360((float) point.getLatitude());
		double clon = MapUtils.normalizeDegrees360((float) point.getLongitude());
		if (clon > 180)
			clon -= 360;
		while (clat > 90)
			clat -= 180;
		while (clat < -90)
			clat += 180;
		return new LatLon(clat, clon);
	}
	
	private static double roundAlt(double altitude) {
		int rndAlt = (int) (altitude * ALTITUDE_PRECISION);
		return rndAlt / (1.0 * ALTITUDE_PRECISION);
	}
	
	private static double roundAzm(double azimuth) {
		int rndAzm = (int) (azimuth * AZIMUTH_PRECISION);
		return rndAzm / (1.0 * AZIMUTH_PRECISION);
	}
	
	private static LatLon move(LatLon from, double angleDist, double bearing) {
		return align(MapUtils.greatCircleDestinationPoint(from.getLatitude(), from.getLongitude(),
				Math.toRadians(angleDist) * MapUtils.EARTH_RADIUS_A, bearing));
	}

	private static LatLon calculateCoordinatesIteration(Body body, Time time, LatLon subsolar, 
			Topocentric targetHor, LatLon check, boolean print) {
		LatLon pnt = subsolar;
		int iter = 0;
		double targetAltitude = targetHor.getAltitude();
		double targetAzm = targetHor.getAzimuth();
		double altitudeFromSubsolar = 90 - targetAltitude;
		double bearingFromSubsolar = 180 + targetAzm;
		// initial step
		pnt = move(subsolar, altitudeFromSubsolar, bearingFromSubsolar);
		Topocentric current = calcAltitude(body, time, pnt, false);
		double deltaAzimuth = MapUtils.degreesDiff(current.getAzimuth(), targetAzm);
		double deltaAltitude = MapUtils.degreesDiff(current.getAltitude(), targetAltitude);
		for (; iter < MAX_ITERATIONS && (roundAzm(deltaAzimuth) != 0 || roundAlt(deltaAltitude) != 0); iter++) {
			bearingFromSubsolar = bearingFromSubsolar - deltaAzimuth;
			pnt = move(subsolar, altitudeFromSubsolar + deltaAltitude, bearingFromSubsolar);
			current = calcAltitude(body, time, pnt, false);
			deltaAzimuth = MapUtils.degreesDiff(current.getAzimuth(), targetAzm);
			deltaAltitude = MapUtils.degreesDiff(current.getAltitude(), targetAltitude);
//			float actualBearing = subsolar.toLocation().bearingTo(pnt.toLocation());
//			diff = MapUtils.degreesDiff(actualBearing, 180 + targetAzm);
//			diff = MapUtils.degreesDiff(current.getAzimuth(), targetAzm);
//			System.out.println("Diff azm=" + MapUtils.degreesDiff(current.getAzimuth(), targetAzm) + " alt="
//					+ MapUtils.degreesDiff(current.getAltitude(), targetAltitude));
		}

		if (print) {
//			calcAltitude(body, time, pnt, true);
			System.out.printf("ERROR %.3f km %d iterations, Lat %.5f Lon %.5f \n", MapUtils.getDistance(check, pnt)/1000, iter, pnt.getLatitude(),
					pnt.getLongitude());
		}
		return pnt;

	}

	

	protected static LatLon calculateCoordinates(Body body, Time time, LatLon iterPoint, 
			Topocentric targetHor, LatLon check, boolean print) {
		LatLon pnt = iterPoint;
		double targetAltitude = roundAlt(targetHor.getAltitude());
		double targetAzm = roundAzm(targetHor.getAzimuth());
		Topocentric current = calcAltitude(body, time, pnt, print);
		double moveDistanceAngle = current.getAltitude() - targetAltitude;
		double azmDistAngle = 0;
		pnt = align(MapUtils.rhumbDestinationPoint(iterPoint.getLatitude(), iterPoint.getLongitude(),
				Math.toRadians(moveDistanceAngle) * MapUtils.EARTH_RADIUS_A, 180 + targetAzm));
		if (current.getAltitude() < 89.9) {
			current = calcAltitude(body, time, pnt, print);
			// move horizontal direction
			azmDistAngle = Math.sin(Math.toRadians(current.getAzimuth() - targetAzm))
					* current.getAltitude() /2;
			pnt = align(MapUtils.rhumbDestinationPoint(pnt.getLatitude(), pnt.getLongitude(),
					Math.toRadians(azmDistAngle) * MapUtils.EARTH_RADIUS_A, 90 + targetAzm));
		}
		if (print) {
			System.out.printf("Move %.5f ^, %.5f > \n", moveDistanceAngle, azmDistAngle);
			System.out.printf("Calc (err %.3f km) Lat %.5f Lon %.5f \n", MapUtils.getDistance(check, pnt)/1000, pnt.getLatitude(),
					pnt.getLongitude());
		}
		return pnt;

	}
	
}
