package net.osmand;

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
import net.osmand.util.MapUtils;

public class TestSubsolar {
	static final int MAX_ITERATIONS = 1000;
	static final double ALTITUDE_TOLERANCE_DEG = 1.0e-6;
	static final int ALTITUDE_PRECISION = 100;
	static final int AZIMUTH_PRECISION = 100;
	// examples
	// https://github.com/cosinekitty/astronomy/blob/master/demo/java/src/main/java/io/github/cosinekitty/astronomy/demo/RiseSetCulm.java
	public static void main(String[] args) throws InterruptedException {

		Body body = Body.Sun;
		for (int i = 5; i < 20; i++) {
			System.out.println("\n----------------");
			String utcTimeString = String.format("2025-05-10T%02d:00:00Z", i);
			long timeS = Instant.parse(utcTimeString).getEpochSecond() * 1000;
			//        timeS = System.currentTimeMillis();
			Time time = Time.fromMillisecondsSince1970(timeS);
			LatLon pnt = new LatLon(52.36723, 45.90412);
			System.out.println("POINT " + pnt + " " + time.toDateTime());
			Topocentric targetHor = calcAltitude(body, time, pnt, true);
			LatLon subsolar = calculateSubsolar(body, time);
			System.out.println("--------");
			LatLon coords = calculateCoordinates(body, time, subsolar, targetHor, pnt, false);
			for (int k = 0; k < MAX_ITERATIONS; k++) {
				coords = calculateCoordinates(body, time, coords, targetHor, pnt, false);
			}
			coords = calculateCoordinates(body, time, coords, targetHor, pnt, true);
			if (MapUtils.getDistance(coords, pnt) > 1000) {
				System.err.println("############");
			}
		}
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
	
	private static LatLon calculateSubsolar(Body body, Time time) {
		Equatorial equ = Astronomy.equator(body, time, new Observer(0, 0, 0.0), EquatorEpoch.OfDate, Aberration.Corrected);
		double greenwichSiderealTime = Astronomy.siderealTime(time);
		double subsolarLatitude = equ.getDec();
        double subsolarLongitude = -(greenwichSiderealTime - equ.getRa()) * 15.0;
		// Normalize longitude to the range -180 to +180
        while (subsolarLongitude <= -180.0) {
            subsolarLongitude += 360.0;
        }
        while (subsolarLongitude > 180.0) {
            subsolarLongitude -= 360.0;
        }
		int latDeg = (int) subsolarLatitude;
		double latMin = (Math.abs(subsolarLatitude) - Math.abs(latDeg)) * 60;

		int lonDeg = (int) subsolarLongitude;
		double lonMin = (Math.abs(subsolarLongitude) - Math.abs(lonDeg)) * 60;
		System.out.printf("Subsolar: %.5f (%d°%.2f'), %.5f (%d°%.2f')\n", subsolarLatitude, latDeg, latMin, subsolarLongitude, lonDeg, lonMin);
		return new LatLon(subsolarLatitude, subsolarLongitude);
	}
	
	private static LatLon align(LatLon point) {
		double clat = MapUtils.normalizeDegrees360((float) point.getLatitude());
		double clon = MapUtils.normalizeDegrees360((float) point.getLongitude());
		if (clon > 180)
			clon -= 360;
		while (clat > 180)
			clat -= 180;
		while (clat < -180)
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

	private static LatLon calculateCoordinates(Body body, Time time, LatLon iterPoint, 
			Topocentric targetHor, LatLon check, boolean print) {
		double targetAltitude = roundAlt(targetHor.getAltitude());
		double targetAzm = roundAzm(targetHor.getAzimuth());
		Topocentric current = calcAltitude(body, time, iterPoint, print);
		double moveDistanceAngle = current.getAltitude() - targetAltitude;
		double azmDistAngle = 0;
		LatLon pnt = align(MapUtils.rhumbDestinationPoint(iterPoint.getLatitude(), iterPoint.getLongitude(),
				Math.toRadians(moveDistanceAngle) * MapUtils.EARTH_RADIUS_A, 180 + targetAzm));
		if (current.getAltitude() < 89.9) {
			// move horizontal direction
			azmDistAngle = Math.sin(Math.toRadians(current.getAzimuth() - targetAzm))
					* current.getAltitude() / 2;
			pnt = align(MapUtils.rhumbDestinationPoint(pnt.getLatitude(), pnt.getLongitude(),
					Math.toRadians(azmDistAngle) * MapUtils.EARTH_RADIUS_A, 90 + targetAzm));
		}
		if (print) {
			System.out.printf("Move %.5f ^, %.5f > \n", moveDistanceAngle, azmDistAngle);
			System.out.printf("Calc (err %.3f km) %.5f %.5f \n", MapUtils.getDistance(check, pnt)/1000, pnt.getLatitude(),
					pnt.getLongitude());
		}
		return pnt;

	}

	

	
}
