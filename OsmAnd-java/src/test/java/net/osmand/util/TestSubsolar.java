package net.osmand.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
	static int MAX_ITERATIONS = 1000;
	static int ALTITUDE_PRECISION = 1000;
	static int AZIMUTH_PRECISION = 1000;
	static double MIN_ALTITUDE = 30;
	static double ERR = 0;
	
	static boolean PRINT = false;
	
	static Random RND = new Random(Instant.parse("2025-01-01T00:00:00Z").getEpochSecond());
	
	static class Stats {
		int tests = 0, duplicate = 0, fail = 0;
		Map<Double, Integer> errorDistr = new TreeMap<Double, Integer>();
		
		void calculateError(LatLon l, LatLon ch) {
			double rndErr = roundError(MapUtils.getDistance(l, ch)) / 1000.0;
			if (!errorDistr.containsKey(rndErr)) {
				errorDistr.put(rndErr, 1);
			} else {
				errorDistr.put(rndErr, 1 + errorDistr.get(rndErr));
			}
		}
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
		double lon = 0;
		Stats s = new Stats();
		
//		MIN_ALTITUDE = 20;
//		ERR = 0.00;
//		for (int m = 1; m <= 12; m++) {
//			for (double lat = -60; lat <= 60; lat += 5) {
//				for (int h = 0; h < 24; h++) {
//					runSingleTest(Body.Sun, lat, lon, s, m, h);
//				}
//			}
//		}
//		System.out.printf("-------\n\nTESTS %d, duplicate %d, failed %d\n ", s.tests, s.duplicate, s.fail);
//		System.out.println(s.errorDistr);
		
//		MIN_ALTITUDE = 20;
//		ERR = 1;
//		for (int minInc = 5; minInc <= 60; minInc += 5) {
//			System.out.println("MIN INC - " + minInc);
//			runMinutesTest(Body.Sun, 40, lon, s, 6, minInc);
//		}
		
		// 52.3676, 4.9041
//		LatLon с1 = calcCoordinatesOneShot(Body.Sun, "2025-09-09T11:00:00Z", 168, 42);
//		LatLon с2 = calcCoordinatesOneShot(Body.Sun, "2025-09-09T11:00:00Z", 168.5, 42.5);
//		LatLon с3 = calcCoordinatesOneShot(Body.Sun, "2025-09-09T11:00:00Z", 167.5, 41.5);
//		System.out.println(MapUtils.getDistance(с1, с2) + " " + MapUtils.getDistance(с1, с3));
//		System.out.println(calcCoordinatesOneShot(Body.Sun, "2025-09-09T13:30:00Z", 216, 37));
//		System.out.println(calcCoordinatesOneShot(Body.Sun, "2025-09-09T15:54:00Z", 252, 20));
//		System.out.println(calcCoordinatesOneShot(Body.Moon, "2025-09-09T03:30:00Z", 230, 29));
//		
		double starAlt; 
		// capella
		Astronomy.defineStar(Body.Star1, 5.28, 46.00, 100);
		starAlt = 65.71;
		// aldebaran: { ra: 4.59, dec: 16.51, name: 'Aldebaran',
		Astronomy.defineStar(Body.Star1, 4.59, 16.51, 100);
		starAlt = 38.82;
		
		LatLon l11 = calcCoordinates2Bodies("2025-09-11T08:00:00Z", Body.Sun, 24.7, Body.Moon, 17.38, true);
		LatLon l12 = calcCoordinates2Bodies("2025-09-11T08:00:00Z", Body.Sun, 24.7, Body.Moon, 17.38, false);
		LatLon l21 = calcCoordinates2Bodies("2025-09-11T08:00:00Z", Body.Star1, starAlt, Body.Moon, 17.38, true);
		LatLon l22 = calcCoordinates2Bodies("2025-09-11T08:00:00Z", Body.Star1, starAlt, Body.Moon, 17.38, false);
		LatLon l31 = calcCoordinates2Bodies("2025-09-11T08:00:00Z", Body.Star1, starAlt, Body.Sun, 24.7, true);
		LatLon l32 = calcCoordinates2Bodies("2025-09-11T08:00:00Z", Body.Star1, starAlt, Body.Sun, 24.7, false);
		LatLon pnt = new LatLon(52.367, 4.904);
		System.out.println(l12 + " " + l11 + " " + l21 + " " + l22 + " " + l31 + " " + l32);
		check(pnt, midPoint(l11, l21));
		check(pnt, midPoint(l32, l21, l11));
		
		
	}

	private static void check(LatLon check, LatLon l) {
		System.out.println(MapUtils.getDistance(check, l) / 1000.0 + " " + l);
	}

	private static LatLon midPoint(LatLon... ls) {
		double lon = 0;
		double lat = 0;
		for(int i = 0; i < ls.length; i++) {
			lat += ls[i].getLatitude() / ls.length;
			lon += ls[i].getLongitude() / ls.length;
		}
		return new LatLon(lat, lon);
	}

	protected static LatLon calcCoordinates2Bodies(String timeS, Body body1, double targetAlt1, Body body2, double targetAlt2, boolean dir) {
		MAX_ITERATIONS = 100000;
		double ALT_PRECISION = 0.01;
		Time time = Time.fromMillisecondsSince1970(Instant.parse(timeS).getEpochSecond() * 1000);
		LatLon projPoint1 = calculateProjPoint(body1, time, PRINT);
		LatLon projPoint2 = calculateProjPoint(body2, time, PRINT);
		
		
		// initial step
		LatLon closest1 = null, closest2 = null;
		double minDist = -1;
		double closestDelta1 = 0, closestDelta2 = 0;
		float azm = projPoint1.toLocation().bearingTo(projPoint2.toLocation());
//		System.out.println(projPoint1);
//		System.out.println(projPoint2);
		int iter = 0;
		for (int magn = 1; magn <= 1000; magn *= 10) {
			double deltaAround1 = closest1 == null ? (dir ? 90 : -90) : closestDelta1;
			double deltaAround2 = closest1 == null ? (dir ? 90 : -90) : closestDelta2;
			int steps = closest1 == null ? 9 : 15;
			List<LatLon> points1 = new ArrayList<LatLon>();
			List<Double> deltas1 = new ArrayList<Double>();
			List<Double> alts1 = new ArrayList<Double>();
			List<LatLon> points2 = new ArrayList<LatLon>();
			List<Double> deltas2 = new ArrayList<Double>();
			List<Double> alts2 = new ArrayList<Double>();
			for (int i = -steps; i < steps; i++) {
				double delta1 = deltaAround1 + i * 10.0 / magn;
				double delta2 = deltaAround2 + i * 10.0 / magn;
				double alt1 = targetAlt1;
				LatLon pnt1 = move(projPoint1, 90 - alt1, azm + delta1);
				Topocentric target1 = calcAltitude(body1, time, pnt1, false);
				while (iter++ < MAX_ITERATIONS && Math.abs(target1.getAltitude() - targetAlt1) > ALT_PRECISION) {
					alt1 -= (target1.getAltitude() - targetAlt1);
					pnt1 = move(projPoint1, 90 - alt1, azm + delta1);
					target1 = calcAltitude(body1, time, pnt1, false);
				}

				//	System.out.println(target1.getAltitude() + " == "+ targetAlt1 + " " + iter);
				double alt2 = targetAlt2;
				LatLon pnt2 = move(projPoint2, 90 - alt2, -(delta2 + azm));
				Topocentric target2 = calcAltitude(body2, time, pnt2, false);
				while (iter++ < MAX_ITERATIONS && Math.abs(target2.getAltitude() - targetAlt2) > ALT_PRECISION) {
					alt2 -= (target2.getAltitude() - targetAlt2);
					pnt2 = move(projPoint2, 90 - alt2, -(delta2 + azm));
					target2 = calcAltitude(body2, time, pnt2, false);
				}
				points1.add(pnt1);
				deltas1.add(delta1);
				alts1.add(alt1);
				points2.add(pnt2);
				deltas2.add(delta2);
				alts2.add(alt2);
				// System.out.println(target2.getAltitude() + " == "+ targetAlt2 + " " + iter);
			}
			for (int i = 0; i < points1.size(); i++) {
				for (int j = 0; j < points2.size(); j++) {
					LatLon pnt1 = points1.get(i);
					LatLon pnt2 = points2.get(j);
					double delta1 = deltas1.get(i);
					double delta2 = deltas2.get(j);
					double dist = MapUtils.getDistance(pnt1, pnt2);
//					System.out.println(
//							delta1 + " " + delta2 + " " + (int) (MapUtils.getDistance(pnt1, pnt2) / 1000) + " " + pnt1 + " " + pnt2);
					if (dist < minDist || minDist < 0) {
						closestDelta1 = delta1;
						closestDelta2 = delta2;
						minDist = dist;
						closest1 = pnt1;
						closest2 = pnt2;
					}
				}
			}
			System.out.println("-------");
			
			System.out.printf("Dist %.2f (iter %d), delta - %.3f %.3f, points - %s %s\n", minDist / 1000, iter,
					closestDelta1, closestDelta2, closest1, closest2);
		}
		return closest1;
	}
	
	protected static LatLon calcCoordinatesOneShot(Body body, String timeS, double azm, double alt) {
		Time time = Time.fromMillisecondsSince1970(Instant.parse(timeS).getEpochSecond() * 1000);
		LatLon projPoint = calculateProjPoint(body, time, PRINT);
		LatLon res = calculateCoordinatesIteration(body, time, projPoint, new Topocentric(azm, alt, 1, 1), null, PRINT);
		System.out.println(res);
		return res;
	}

	protected static void runMinutesTest(Body body, double lat, double lon, Stats s, int month, int minInc) {
		double sumLat = 0, sumLon = 0;
		int cnt = 0;
		LatLon pnt = new LatLon(lat, lon);
		int startHour = 12;
		int hours = 4;
		s.errorDistr.clear();
		for (int min = 0; min < 60 * hours; min += minInc) {
			String utcTimeString = String.format("2025-01-%02dT%02d:%02d:00Z", month, startHour + min / 60, min % 60);
			long timeS = Instant.parse(utcTimeString).getEpochSecond() * 1000;
			Time time = Time.fromMillisecondsSince1970(timeS);
			Topocentric targetHor = error(calcAltitude(body, time, pnt, false));
			if (targetHor.getAltitude() < MIN_ALTITUDE) {
				continue;
			}
			LatLon projPoint = calculateProjPoint(body, time, PRINT);
			LatLon coords = calculateCoordinatesIteration(body, time, projPoint, targetHor, pnt, PRINT);
			sumLat += coords.getLatitude();
			sumLon += coords.getLongitude();
			cnt++;
			s.calculateError(pnt, coords);
		}
		LatLon res = new LatLon(sumLat / cnt, sumLon / cnt);
		double err = MapUtils.getDistance(res, pnt);
		double rndErr = roundError(err);
		System.out.printf("Error %.2f km - %d measurements \n", rndErr / 1000, cnt);
		System.out.println("Error distribution " + s.errorDistr);
		
	}
	
	private static Topocentric error(Topocentric targetHor) {
		return new Topocentric(targetHor.getAzimuth() + ERR * (RND.nextDouble() - 0.5), 
				targetHor.getAltitude() + ERR * (RND.nextDouble() - 0.5), targetHor.getRa(), targetHor.getDec());
	}

	protected static void runSingleTest(Body body, double lat, double lon, Stats s, int month, int hour) {
		String utcTimeString = String.format("2025-01-%02dT%02d:00:00Z", month, hour);
		long timeS = Instant.parse(utcTimeString).getEpochSecond() * 1000;
		Time time = Time.fromMillisecondsSince1970(timeS);
		LatLon pnt = new LatLon(lat, lon);
		Topocentric targetHor = error(calcAltitude(body, time, pnt, PRINT));
		if (targetHor.getAltitude() < MIN_ALTITUDE) {
			return;
		}
		LatLon projPoint = calculateProjPoint(body, time, PRINT);
		LatLon coords = calculateCoordinatesIteration(body, time, projPoint, targetHor, pnt, PRINT);
		double err = MapUtils.getDistance(coords, pnt);
		s.tests++;
		s.calculateError(pnt, coords);
		if (err > 1_000) {
			s.duplicate++;
			
			System.out.println("\n----------------");
			System.out.println(time.toDateTime());
			System.out.println("SUBSOL  " + projPoint);
			System.out.printf("POINT %s (%.3f bearing, %.3f dist)\n", pnt,
					pnt.toLocation().bearingTo(projPoint.toLocation()), pnt.toLocation().distanceTo(projPoint.toLocation())/ 1000f);
			Topocentric target = calcAltitude(body, time, pnt, true);
			System.out.printf("CALC  %s (%.3f bearing, %.3f dist)\n", coords,
					coords.toLocation().bearingTo(projPoint.toLocation()), coords.toLocation().distanceTo(projPoint.toLocation())/ 1000f);
			Topocentric calc = calcAltitude(body, time, coords, true);
			double altErr = Math.abs(calc.getAltitude() - target.getAltitude());
			double azmErr = Math.abs(Math.abs(calc.getAzimuth() - target.getAzimuth()));
			if (altErr > 1.0d / ALTITUDE_PRECISION  || azmErr > 1.0d / AZIMUTH_PRECISION ) {
				s.fail++;
				System.err.println(altErr + " " + azmErr);
			}
			coords = calculateCoordinatesIteration(body, time, projPoint, targetHor, pnt, true);
			
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
	
	private static LatLon calculateProjPoint(Body body, Time time, boolean print) {
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

		if (print && check != null) {
//			calcAltitude(body, time, pnt, true);
			System.out.printf("ERROR %.3f km %d iterations, Lat %.5f Lon %.5f \n", MapUtils.getDistance(check, pnt)/1000, iter, pnt.getLatitude(),
					pnt.getLongitude());
		}
		return pnt;

	}

	

	
}
