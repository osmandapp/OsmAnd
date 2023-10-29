package net.osmand.util;

import java.util.concurrent.Callable;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MapUtilsTest {
	private final float MAX_DEVIATION = 1.0f; // %
	private final int TEST_PERFORMANCE_TIMEOUT = 0; // ms, 0 to disable

	private void dist(int ideal, LatLon A, LatLon B, String info) throws Exception {
		int x1 = MapUtils.get31TileNumberX(A.getLongitude());
		int x2 = MapUtils.get31TileNumberX(B.getLongitude());
		int y1 = MapUtils.get31TileNumberY(A.getLatitude());
		int y2 = MapUtils.get31TileNumberY(B.getLatitude());

		PerformanceTester<Integer> getDistance =
				new PerformanceTester<>(() -> (int) MapUtils.getDistance(A, B), "getDistance");
		PerformanceTester<Integer> measuredDist31 =
				new PerformanceTester<>(() -> (int) MapUtils.measuredDist31(x1, y1, x2, y2), "measuredDist31");
		PerformanceTester<Integer> squareRootDist31 =
				new PerformanceTester<>(() -> (int) MapUtils.squareRootDist31(x1, y1, x2, y2), "squareRootDist31");

		int getDistanceMeters = getDistance.testPerformance();
		int measuredDist31Meters = measuredDist31.testPerformance();
		int squareRootDist31Meters = squareRootDist31.testPerformance();

		float deviation = Math.abs((float) squareRootDist31Meters / (float) ideal * 100 - 100);

		float factor = squareRootDist31.getCounter() / measuredDist31.getCounter(); // fast method vs slow method

		System.err.printf(
				"Got squareRootDist31() deviation %.2f%% from precise %d m [%s] speedup=%.2f\n",
				deviation, getDistanceMeters, info, factor
		);

		Assert.assertEquals(ideal, getDistanceMeters); // haversine method - ideal
		Assert.assertEquals(getDistanceMeters, measuredDist31Meters); // just re-check with x/y
		Assert.assertTrue(deviation < MAX_DEVIATION); // max 1% for squareRootDist31() method
	}

	private class PerformanceTester<T> {
		private String name = "tester";
		private Callable<T> method = null;
		private int timeout = TEST_PERFORMANCE_TIMEOUT;
		private long startTime = 0, endTime = 0;
		private int counter = 0;

		public PerformanceTester(Callable<T> method, int timeout, String name) {
			this.method = method;
			this.timeout = timeout;
			this.name = name;
		}

		public PerformanceTester(Callable<T> method, int timeout) {
			this.method = method;
			this.timeout = timeout;
		}

		public PerformanceTester(Callable<T> method, String name) {
			this.method = method;
			this.name = name;
		}

		public PerformanceTester(Callable<T> method) {
			this.method = method;
		}

		public T testPerformance() throws Exception {
			T result = null;
			startTime = System.currentTimeMillis();
			do {
				counter++;
				result = method.call();
				endTime = System.currentTimeMillis();
			} while (endTime - startTime < timeout);
			return result;
		}

		public float getCounter() {
			return (float) counter;
		}

		public String toString() {
			float delta = endTime - startTime;
			float rps = timeout > 0 ? (float) counter * ((float) timeout / delta) * (1000 / (float) timeout) : 0;
			return String.format("%s %.2f run per second", name, rps);
		}
	}

	@Test
	public void testDistance() throws Exception {
		dist(558, new LatLon(40.82126752, 14.42274696),
				new LatLon(40.82125128, 14.42937738), "IT-Vesuvio (x)");
		dist(543, new LatLon(40.82375190, 14.42624456),
				new LatLon(40.81886422, 14.42618018), "IT-Vesuvio (y)");
		dist(4_729, new LatLon(-39.29491586, 174.03285739),
				new LatLon(-39.29584579, 174.08778903), "NZ-Taranaki (x)");
		dist(4_532, new LatLon(-39.27883939, 174.06337019),
				new LatLon(-39.31948845, 174.06714675), "NZ-Taranaki (y)");
		dist(19_270, new LatLon(-39.29212599, 173.95213362),
				new LatLon(-39.29451731, 174.17598006), "NZ-Egmont (x)");
		dist(19_363, new LatLon(-39.20864435, 174.06371352),
				new LatLon(-39.38267339, 174.06989333), "NZ-Egmont (y)");
		dist(16_000_459, new LatLon(-33.70408492, 151.04180026),
				new LatLon(40.76570188, -74.00214505), "Sydney-NewYork (far)");
		dist(54_808, new LatLon(48.22579901, 16.37482333),
				new LatLon(48.16034314, 17.10747409), "Wien-Bratislava (nearby)");
		dist(930_981, new LatLon(52.52435240, 13.40508151),
				new LatLon(51.55122931, -0.13007474), "Berlin-London (x-axis)");
		dist(1_602_242, new LatLon(38.11914201, 13.36113620),
				new LatLon(52.52435240, 13.40508151), "Palermo-Berlin (y-axis)");
		dist(6_994_820, new LatLon(48.93849659, 2.24297214),
				new LatLon(19.25116676, 72.73125339), "Paris-Mumbai (xy-axis)");
		dist(788_180, new LatLon(60.01115926, 10.63476563),
				new LatLon(60.18641482, 24.87304688), "Oslo-Helsinki (north-x-axis)");
		dist(2_190_230, new LatLon(-70.66645729, -69.20288086),
				new LatLon(-51.66564060, -57.88348915), "Alexander-Falkland-Islands (south-y-axis)");
		dist(7_902_004, new LatLon(-0.89518678, 32.83611534),
				new LatLon(1.29094680, 103.85174034), "Lake-Viktoria-Singapore (equator-x-axis)");
		dist(1_176_417, new LatLon(-6.12400803, 106.81804894),
				new LatLon(3.13519342, 101.69842003), "Jakarta-Kuala-Lumpur (equator-y-axis)");
	}
}
