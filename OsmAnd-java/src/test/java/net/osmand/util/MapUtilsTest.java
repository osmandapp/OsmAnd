package net.osmand.util;

import java.io.IOException;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MapUtilsTest {
	static final float MAX_DEVIATION = 1.0f; // %

	private void dist(int ideal, LatLon A, LatLon B, String info) {
		int getDistanceMeters = (int) MapUtils.getDistance(A, B);

		int x1 = MapUtils.get31TileNumberX(A.getLongitude());
		int x2 = MapUtils.get31TileNumberX(B.getLongitude());
		int y1 = MapUtils.get31TileNumberY(A.getLatitude());
		int y2 = MapUtils.get31TileNumberY(B.getLatitude());

		int measuredDist31Meters = (int) MapUtils.measuredDist31(x1, y1, x2, y2);

		int squareRootDist31Meters = (int) MapUtils.squareRootDist31(x1, y1, x2, y2);
		float deviation = Math.abs((float) squareRootDist31Meters / (float) ideal * 100 - 100);

		System.err.printf("Got squaAreRootDist31() deviation %.2f%% from precise %d m [%s]\n",
				deviation, getDistanceMeters, info);

		Assert.assertEquals(ideal, getDistanceMeters); // haversine method - ideal
		Assert.assertEquals(getDistanceMeters, measuredDist31Meters); // just re-check with x/y
		Assert.assertTrue(deviation < MAX_DEVIATION); // max 1% for squareRootDist31() method
	}

	@Test
	public void testDistance() throws IOException {
		dist(558, new LatLon(40.82126752, 14.42274696),
				new LatLon(40.82125128, 14.42937738), "IT-Vesuvio (x)");
		dist(543, new LatLon(40.82375190, 14.42624456),
				new LatLon(40.81886422, 14.42618018), "IT-Vesuvio (y)");
		dist(4729, new LatLon(-39.29491586, 174.03285739),
				new LatLon(-39.29584579, 174.08778903), "NZ-Taranaki (x)");
		dist(4532, new LatLon(-39.27883939, 174.06337019),
				new LatLon(-39.31948845, 174.06714675), "NZ-Taranaki (y)");
		dist(19270, new LatLon(-39.29212599, 173.95213362),
				new LatLon(-39.29451731, 174.17598006), "NZ-Egmont (x)");
		dist(19363, new LatLon(-39.20864435, 174.06371352),
				new LatLon(-39.38267339, 174.06989333), "NZ-Egmont (y)");
		dist(16000459, new LatLon(-33.70408492, 151.04180026),
				new LatLon(40.76570188, -74.00214505), "Sydney-NewYork (far)");
		dist(54808, new LatLon(48.22579901, 16.37482333),
				new LatLon(48.16034314, 17.10747409), "Wien-Bratislava (nearby)");
		dist(930981, new LatLon(52.52435240, 13.40508151),
				new LatLon(51.55122931, -0.13007474), "Berlin-London (x-axis)");
		dist(1602242, new LatLon(38.11914201, 13.36113620),
				new LatLon(52.52435240, 13.40508151), "Palermo-Berlin (y-axis)");
		dist(6994820, new LatLon(48.93849659, 2.24297214),
				new LatLon(19.25116676, 72.73125339), "Paris-Mumbai (xy-axis)");
		dist(788180, new LatLon(60.01115926, 10.63476563),
				new LatLon(60.18641482, 24.87304688), "Oslo-Helsinki (north-x-axis)");
		dist(2190230, new LatLon(-70.66645729, -69.20288086),
				new LatLon(-51.66564060, -57.88348915), "Alexander-Falkland-Islands (south-y-axis)");
		dist(7902004, new LatLon(-0.89518678, 32.83611534),
				new LatLon(1.29094680, 103.85174034), "Lake-Viktoria-Singapore (equator-x-axis)");
		dist(1176417, new LatLon(-6.12400803, 106.81804894),
				new LatLon(3.13519342, 101.69842003), "Jakarta-Kuala-Lumpur (equator-y-axis)");
	}
}
