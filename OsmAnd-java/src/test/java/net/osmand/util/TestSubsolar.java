package net.osmand.util;

import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
		int maxClusterSize, iterations, failed, found;
		public float calcTime;
		
		void calculateError(LatLon l, LatLon ch) {
			double rndErr = roundError(MapUtils.getDistance(l, ch)) / 1000.0;
			if (!errorDistr.containsKey(rndErr)) {
				errorDistr.put(rndErr, 1);
			} else {
				errorDistr.put(rndErr, 1 + errorDistr.get(rndErr));
			}
		}
	}
	
	static class Star {
		double ra;
		double dec;
		double app_ma;
		String name;
		String key; 
		
		String wid;
		
		@Override
		public String toString() {
			return String.format("%s (%s) - %.3f, %.3f deg, %.1f mag", name, wid, ra, dec, app_ma);
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
	
	private static boolean ESTIMATE_ERR_SUN_ALT_AZM = false;
	private static boolean ESTIMATE_ERR_BY_SUN_ALT_AZM_MULT_MIN = false;
	private static boolean SINGLE_APPROXIMATE_BY_ALT_AZM = false;
	private static boolean APPROXIMATE_BY_BODIES_ALTITUDES = false;
	private static boolean APPROXIMATE_JUST_BY_ALTITUDES = true;
	public static void main(String[] args) throws InterruptedException {
		
		if (ESTIMATE_ERR_SUN_ALT_AZM) {
			double lon = 0;
			MAX_ITERATIONS = 1000;
			MIN_ALTITUDE = 20;
			Stats s = new Stats();
			ERR = 0.01;
			for (int m = 1; m <= 12; m++) {
				for (double lat = -60; lat <= 60; lat += 5) {
					for (int h = 0; h < 24; h++) {
						runSingleTest(Body.Sun, lat, lon, s, m, h);
					}
				}
			}
			System.out.printf("-------\n\nTESTS %d, duplicate %d, failed %d\n ", s.tests, s.duplicate, s.fail);
			System.out.println(s.errorDistr);
		}

		if (ESTIMATE_ERR_BY_SUN_ALT_AZM_MULT_MIN) {
			Stats s = new Stats();
			double lon = 0;
			MIN_ALTITUDE = 20;
			ERR = 1;
			for (int minInc = 5; minInc <= 60; minInc += 5) {
				System.out.println("MIN INC - " + minInc);
				runMinutesTest(Body.Sun, 40, lon, s, 6, minInc);
			}
		}
		if (SINGLE_APPROXIMATE_BY_ALT_AZM) {
			// 52.3676, 4.9041
			LatLon с1 = calcCoordinatesOneShot(Body.Sun, "2025-09-09T11:00:00Z", 168, 42);
			LatLon с2 = calcCoordinatesOneShot(Body.Sun, "2025-09-09T11:00:00Z", 168.5, 42.5);
			LatLon с3 = calcCoordinatesOneShot(Body.Sun, "2025-09-09T11:00:00Z", 167.5, 41.5);
			System.out.println(MapUtils.getDistance(с1, с2) + " " + MapUtils.getDistance(с1, с3));
			System.out.println(calcCoordinatesOneShot(Body.Sun, "2025-09-09T13:30:00Z", 216, 37));
			System.out.println(calcCoordinatesOneShot(Body.Sun, "2025-09-09T15:54:00Z", 252, 20));
			System.out.println(calcCoordinatesOneShot(Body.Moon, "2025-09-09T03:30:00Z", 230, 29));
		}
		

		if (APPROXIMATE_BY_BODIES_ALTITUDES) {
			Map<String, Star> mapStars = initStars();
			Stats s = new Stats();
			String time = "2025-09-11T08:00:00Z";
			LatLon testPnt = new LatLon(52.367, 4.904);
			
			
//			String[] bodiesStr = { "Sun", "Moon", "capella", "aldebaran", "betelgeuse" };
			String[] bodiesStr = { "Sun", "Moon", "pollux", "aldebaran", "regulus" }; // incorrect
			double[] alts = { 24.65, 17.91, 65.71, 38.92, 40.04 }; // timeanddate
//			double[] alts = { 24.5, 17.8, 65.8, 38.7, 40 }; // rough errors
			Body[] bodies = initBodies(mapStars, bodiesStr);
			LatLon res = calcPositionBodies(s, bodies, alts, time, null, 0);
			check(testPnt, res,
					String.format("- %d cluster size from %d points (%d failed, %d iterations) - %.3f sec\n %s",
							s.maxClusterSize, s.found, s.failed, s.iterations, s.calcTime, res));
			// compare altitudes
			Time timeT = Time.fromMillisecondsSince1970(Instant.parse(time).getEpochSecond() * 1000);
			for (int i = 0; i < bodies.length; i++) {
				Topocentric alt = calcAltitude(bodies[i], timeT, testPnt, false);
				System.out.printf("Body %s, err correction %.3f\n", bodies[i], alt.getAltitude() - alts[i]);
			}
		}
		
		if (APPROXIMATE_JUST_BY_ALTITUDES) {
			Map<String, Star> mapStars = initStars();
			List<Star> stars = new ArrayList<Star>(mapStars.values());
			Stats s = new Stats();
			String time = "2025-09-11T08:00:00Z";
			LatLon testPnt = new LatLon(52.367, 4.904);
			double aroundThresholdKm = 1000;
			LatLon aroundPnt = new LatLon(48, 5);
			
			if (MapUtils.getDistance(aroundPnt, testPnt) / 1000 > aroundThresholdKm) {
				System.out.println(MapUtils.getDistance(aroundPnt, testPnt)/1000 + " km - around point is outside radius");
				return;
			}
			
			double[] alts = { 17.91, 65.71, 40.04, 38.92 }; // timeanddate
			for(int i = 0; i < stars.size(); i++ ) {
				System.out.println("--- " + stars.get(i).key);
				for (int j = 0; j < stars.size(); j++) {
					if (j == i) {
						continue;
					}
					String[] bodiesStr = { "Moon", stars.get(i).key, stars.get(j).key};
					LatLon res = calcPositionBodies(s, initBodies(mapStars, bodiesStr), alts, time, aroundPnt, aroundThresholdKm);
					if (res != null) {
						System.out.println(stars.get(i).key + " " + stars.get(j).key +".... ");
						for (int k = 0; k < stars.size(); k++) {
							if (j == k || i == k) {
								continue;
							}
							String[] bodiesStr2 = { "Moon", stars.get(i).key, stars.get(j).key, stars.get(k).key};
							LatLon detailedRes = calcPositionBodies(s, initBodies(mapStars, bodiesStr2), alts, time, aroundPnt, aroundThresholdKm);
							if (detailedRes != null) {
								System.out.println(
										stars.get(i).key + " " + stars.get(j).key + " " + stars.get(k).key + "!!");
								check(testPnt, res, String.format(
										"- %d cluster size from %d points (%d failed, %d iterations) - %.3f sec",
										s.maxClusterSize, s.found, s.failed, s.iterations, s.calcTime));
							}
						}
						
					}
				}
			}
		}
		
	}
	
	private static Map<String, Star> initStars() {
		Map<String, Star> mapStars = new Gson().fromJson(new InputStreamReader(TestSubsolar.class.getResourceAsStream("/stars/stars.json")),
				new TypeToken<Map<String, Star>>(){}.getType());
		
		for(String key : mapStars.keySet()) {
			mapStars.get(key).key = key;
		}
		return mapStars;
	}

	private static Body[] initBodies(Map<String, Star> mapStars, String[] bodiesStr) {
		Body[] bodies = new Body[bodiesStr.length];
		int starInd = 0;
		Body[] starConstants = {Body.Star1, Body.Star2, Body.Star3, Body.Star4, Body.Star5 };
		for(int i = 0; i < bodies.length; i++) {
			Star star = mapStars.get(bodiesStr[i].toLowerCase());
			if(star != null) {
				bodies[i] = starConstants[starInd++];
				Astronomy.defineStar(bodies[i], star.ra, star.dec, 5);
//				System.out.println("Define " + star.name + " " + starInd + " "  + star.ra + ", " + star.dec);
			} else {
				bodies[i] = Body.valueOf(bodiesStr[i]);
				if (bodies[i] == null) {
					throw new IllegalArgumentException(bodiesStr[i]);
				}
			}
		}
		return bodies;
	}
	
	public static List<List<LatLon>> clusterByProximity(List<LatLon> points, double thresholdDistanceMeters) {
        List<List<LatLon>> clusters = new ArrayList<>();
        // Make a copy to avoid modifying the original list
        List<LatLon> remainingPoints = new ArrayList<>(points);

        while (!remainingPoints.isEmpty()) {
            // Start a new cluster with the first remaining point
            List<LatLon> newCluster = new ArrayList<>();
            LatLon clusterSeed = remainingPoints.remove(0);
            newCluster.add(clusterSeed);
            clusters.add(newCluster);

            // Use an iterator to safely remove items while iterating
            Iterator<LatLon> it = remainingPoints.iterator();
            while (it.hasNext()) {
                LatLon potentialMember = it.next();
                // Check distance against the original seed point of the cluster
                if (MapUtils.getDistance(clusterSeed, potentialMember) <= thresholdDistanceMeters) {
                    newCluster.add(potentialMember);
                    it.remove(); // Move point from remaining to the new cluster
                }
            }
        }

        return clusters;
    }

	private static LatLon calcPositionBodies(Stats s, Body[] bodies, double[] alts, String time, 
			LatLon aroundPnt, double aroundThresholdKm) {
		long nt = System.nanoTime();
		double MIN_THRESHOLD = 4_000;
		double MAX_THRESHOLD = 500_000; 
		int MIN_SIZE_CLUSTER = 3;
		double threshold = MIN_THRESHOLD;
		List<LatLon> l = new ArrayList<LatLon>();
		s.found = 0;
		s.failed = 0;
		for (int i = 0; i < bodies.length; i++) {
			for (int j = i + 1; j < bodies.length; j++) {
				LatLon a1 = calcCoordinates2Bodies(s, time, bodies[i], alts[i], bodies[j], alts[j], 
						true, aroundPnt, aroundThresholdKm);
				if (a1 != null) {
					if (aroundPnt == null || MapUtils.getDistance(a1, aroundPnt) < aroundThresholdKm * 1000) {
						s.found++;
						l.add(a1);
					}
				} else {
					s.failed++;
				}
				LatLon a2 = calcCoordinates2Bodies(s, time, bodies[i], alts[i], bodies[j], alts[j], 
						false, aroundPnt, aroundThresholdKm);
				if (a2 != null) {
					if (aroundPnt == null || MapUtils.getDistance(a2, aroundPnt) < aroundThresholdKm * 1000) {
						s.found++;
						l.add(a2);
					}
				} else {
					s.failed++;
				}
			}
		}
		if (l.size() < MIN_SIZE_CLUSTER) {
			return null;
		}
		
		List<LatLon> maxSizeCluster = null;
		while (maxSizeCluster == null && threshold < MAX_THRESHOLD) {
			threshold *= 4;
			List<List<LatLon>> clusters = clusterByProximity(l, threshold);
			for (List<LatLon> cl : clusters) {
				if (cl.size() > (maxSizeCluster != null ? maxSizeCluster.size() : MIN_SIZE_CLUSTER - 1)) {
					maxSizeCluster = cl;
				}
//				check(check, midPoint(cl.toArray(new LatLon[0])), cl.size() + " " + cl);
			}
			
		}
		s.calcTime += (System.nanoTime() - nt) / 1e9f;
		if (maxSizeCluster == null) {
			s.maxClusterSize = 0;
			return null;
		}
		s.maxClusterSize = maxSizeCluster.size();
		return midPoint(maxSizeCluster.toArray(new LatLon[0]));
	}

	private static void check(LatLon check, LatLon l, Object msg) {
		System.out.printf("Error %.2f km %s \n", l == null ?  Float.POSITIVE_INFINITY : MapUtils.getDistance(check, l) / 1000.0, msg);
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

	protected static LatLon calcCoordinates2Bodies(Stats s, String timeS, Body body1, double targetAlt1, 
			Body body2, double targetAlt2, boolean dir, LatLon check, double thresholdKm) {
		MAX_ITERATIONS = 100000;
		double ALT_PRECISION = 0.01;
		double MIN_THRESHOLD = 10000;
		Time time = Time.fromMillisecondsSince1970(Instant.parse(timeS).getEpochSecond() * 1000);
		LatLon projPoint1 = calculateProjPoint(body1, time, PRINT);
		LatLon projPoint2 = calculateProjPoint(body2, time, PRINT);
		
		
		// initial step
		LatLon closest1 = null;
		double minDist = -1;
		double closestDelta1 = 0, closestDelta2 = 0;
		float azm = projPoint1.toLocation().bearingTo(projPoint2.toLocation());
//		System.out.println(projPoint1);
//		System.out.println(projPoint2);
		int iter = 0;
		// magnitude - 100 (2km), 1000 (200m), 10000 (20m)
		for (int magn = 1; magn <= 10000; magn *= 10) {
			double deltaAround1 = closest1 == null ? (dir ? 90 : -90) : closestDelta1;
			double deltaAround2 = closest1 == null ? (dir ? 90 : -90) : closestDelta2;
			int steps = closest1 == null ? 9 : 25;
			List<LatLon> points1 = new ArrayList<LatLon>();
			List<Double> deltas1 = new ArrayList<Double>();
			List<LatLon> points2 = new ArrayList<LatLon>();
			List<Double> deltas2 = new ArrayList<Double>();
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
				points2.add(pnt2);
				deltas2.add(delta2);
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
					}
				}
			}
			if (check != null && magn > 1 && minDist < thresholdKm * 1000 * 10
					&& MapUtils.getDistance(check, closest1) > thresholdKm * 1000) {
				return null;
			}
			
		}
		s.iterations += iter;
//		System.out.printf("Dist %.2f (iter %d), delta - %.3f %.3f, points - %s %s\n", minDist / 1000, iter,
//				closestDelta1, closestDelta2, closest1, closest2);
		if (minDist > MIN_THRESHOLD) {
			return null;
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
		Equatorial equ;
		// planets needs to be tested with time and date
		if (body != Body.Sun && body != Body.Moon) {
			equ = Astronomy.equator(body, time, observer, EquatorEpoch.J2000, Aberration.Corrected);
		} else {
			equ = Astronomy.equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected);
		}
		Topocentric hor = Astronomy.horizon(time, observer, equ.getRa(), equ.getDec(), Refraction.Normal); // Refraction.Normal
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
