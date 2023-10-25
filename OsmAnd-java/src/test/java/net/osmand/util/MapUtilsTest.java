package net.osmand.util;

import java.io.IOException;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

//  return MapUtils.measuredDist31(x1, y1, x2, y2);
//  return MapUtils.squareRootDist31(x1, y1, x2, y2);
//  return new LatLon(MapUtils.get31LatitudeY(preciseY), MapUtils.get31LongitudeX(preciseX));
//  dst = ((int) (MapUtils.squareRootDist31(x, y, xe, ye) * 10)) / 10.0f + " m";
//  public static LatLon calculateMidPoint(LatLon s1, LatLon s2) {
//	public static int get31TileNumberX(double longitude)
//	public static int get31TileNumberY(double latitude)
//	public static double get31LongitudeX(int tileX)
//	public static double get31LatitudeY(int tileY)

public class MapUtilsTest {

	private void dist(int ideal, LatLon A, LatLon B, String info) {
		int getDistanceMeters = (int) MapUtils.getDistance(A, B); // haversine method - ideal

		int x1 = MapUtils.get31TileNumberX(A.getLongitude());
		int x2 = MapUtils.get31TileNumberX(B.getLongitude());
		int y1 = MapUtils.get31TileNumberY(A.getLatitude());
		int y2 = MapUtils.get31TileNumberY(B.getLatitude());

		int measuredDist31Meters = (int) MapUtils.measuredDist31(x1, y1, x2, y2); // just re-check with x/y

		Assert.assertEquals(ideal, getDistanceMeters);
		Assert.assertEquals(getDistanceMeters, measuredDist31Meters);

		int squareRootDist31Meters = (int) MapUtils.squareRootDist31(x1, y1, x2, y2);
		float deviation = Math.abs((float) squareRootDist31Meters / (float) ideal * 100 - 100);

		System.out.printf("deviation %.2f%%, ideal %d km, %s\n", deviation, ideal / 1000, info);
	}

	@Test
	public void testDistance() throws IOException {
		dist(54808, new LatLon(48.22579901, 16.37482333), new LatLon(48.16034314, 17.10747409), "Wien-Bratislava (nearby)");
		dist(930981, new LatLon(52.52435240, 13.40508151), new LatLon(51.55122931, -0.13007474), "Berlin-London (x-axis)");
		dist(1602242, new LatLon(38.11914201, 13.36113620), new LatLon(52.52435240, 13.40508151), "Palermo-Berlin (y-axis)");
		dist(6994820, new LatLon(48.93849659, 2.24297214), new LatLon(19.25116676, 72.73125339), "Paris-Mumbai (xy-axis)");
		dist(7902004, new LatLon(-0.89518678, 32.83611534), new LatLon(1.29094680, 103.85174034), "Lake-Viktoria-Singapore (equator-x-axis)");
		dist(1176417, new LatLon(-6.12400803, 106.81804894), new LatLon(3.13519342, 101.69842003), "Jakarta-Kuala-Lumpur (equator-y-axis)");
		dist(2190230, new LatLon(-70.66645729, -69.20288086), new LatLon(-51.66564060, -57.88348915), "Alexander-Island-Falkland-Islands (south-y-axis)");
		dist(788180, new LatLon(60.01115926, 10.63476563), new LatLon(60.18641482, 24.87304688), "Oslo-Helsinki (north-x-axis)");
		dist(16000459, new LatLon(-33.70408492, 151.04180026), new LatLon(40.76570188, -74.00214505), "Sydney-NewYork (far away)");
	}
}
