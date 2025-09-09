package net.osmand.util;

import java.time.Instant;

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
	static final int MAX_ITERATIONS = 10000;
	static final int ALTITUDE_PRECISION = 100_000;
	static final int AZIMUTH_PRECISION = 100_000;
	
	static boolean PRINT = false;
	// examples
	// https://github.com/cosinekitty/astronomy/blob/master/demo/java/src/main/java/io/github/cosinekitty/astronomy/demo/RiseSetCulm.java
	public static void main(String[] args) throws InterruptedException {

		double MIN_ALTITUDE = 30;
		Body body = Body.Sun;
		int tests = 0, duplicate = 0, fail = 0;
		double lon = 0;
		for(int m = 1; m <= 12; m++) {
		for (double lat = -60; lat <= 60; lat +=1) {
//		double lat = 20; {
			for (int h = 0; h < 24; h++) {
//			int h = 18; {
				String utcTimeString = String.format("2025-01-%02dT%02d:00:00Z", m, h);
				long timeS = Instant.parse(utcTimeString).getEpochSecond() * 1000;
				// timeS = System.currentTimeMillis();
				Time time = Time.fromMillisecondsSince1970(timeS);
				LatLon pnt = new LatLon(lat, lon);
				Topocentric targetHor = calcAltitude(body, time, pnt, PRINT);
				if (targetHor.getAltitude() < MIN_ALTITUDE) {
					continue;
				}
				LatLon subsolar = calculateSubsolar(body, time, PRINT);
				LatLon coords = calculateCoordinatesIteration(body, time, subsolar, targetHor, pnt, PRINT);
//				for (; iter < MAX_ITERATIONS; iter++) {
//					Topocentric c = calcAltitude(body, time, coords, false);
//					if (roundAlt(targetHor.getAltitude() - c.getAltitude()) == 0.0
//							&& roundAzm(targetHor.getAzimuth() - c.getAzimuth()) == 0.0) {
//						break;
//					}
//					coords = calculateCoordinates(body, time, coords, targetHor, pnt, PRINT);
//				}
				double err = MapUtils.getDistance(coords, pnt) / 1000;
				tests++;
				if (err > 1) {
					duplicate++;
					
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
						fail++;
						System.err.println(altErr + " " + azmErr);
					}
					coords = calculateCoordinatesIteration(body, time, subsolar, targetHor, pnt, true);
					
				}
			}
		}
		}
		System.out.printf("-------\n\nTESTS %d, duplicate %d, failed %d ", tests, duplicate, fail);
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
		double moveDistanceAngle = 90 - targetAltitude;
		double bearingFromSubsolar = 180 + targetAzm;
		// initial step
		pnt = move(subsolar, moveDistanceAngle, bearingFromSubsolar);
		Topocentric current = calcAltitude(body, time, pnt, false);
		
		double deltaAzimuth = MapUtils.degreesDiff(current.getAzimuth(), targetAzm);
		for (; iter < MAX_ITERATIONS / 2 && roundAzm(deltaAzimuth) != 0; iter++) {
			bearingFromSubsolar = bearingFromSubsolar - deltaAzimuth;
			pnt = move(subsolar, moveDistanceAngle, bearingFromSubsolar);
			current = calcAltitude(body, time, pnt, false);
			deltaAzimuth = MapUtils.degreesDiff(current.getAzimuth(), targetAzm);
//			float actualBearing = subsolar.toLocation().bearingTo(pnt.toLocation());
//			diff = MapUtils.degreesDiff(actualBearing, 180 + targetAzm);
//			diff = MapUtils.degreesDiff(current.getAzimuth(), targetAzm);
//			System.out.println("Diff azm=" + MapUtils.degreesDiff(current.getAzimuth(), targetAzm) + " alt="
//					+ MapUtils.degreesDiff(current.getAltitude(), targetAltitude));
		}

		moveDistanceAngle = current.getAltitude() - targetAltitude;
		for (; iter < MAX_ITERATIONS / 2 && roundAlt(moveDistanceAngle) != 0; iter++) {
			moveDistanceAngle = current.getAltitude() - targetAltitude;
			pnt = move(pnt, moveDistanceAngle, 180 + targetAzm);
			current = calcAltitude(body, time, pnt, false);
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
