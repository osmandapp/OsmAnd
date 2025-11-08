package net.osmand.util;

import net.osmand.data.QuadRect;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.util.KMapUtils;
import org.junit.Assert;
import org.junit.Test;

import net.osmand.data.LatLon;

public class MapUtilsTest {

	static final double DIST_M = 1000_000; // in meters
	static final double LON_TEST = 15; // positive
	
	public static void main(String[] args) {
//		double dist = 10_000_000;
		double lon = LON_TEST;
		 
		for (int dist = 1_000_000; dist <= 15_000_000; dist += 100_000) {
		for (int lat = 30; lat <= 30; lat += 5) {
			LatLon ll = new LatLon(lat, lon);
			LatLon gc = MapUtils.greatCircleDestinationPoint(ll.getLatitude(), ll.getLongitude(), dist, 90);
			LatLon rhumb = MapUtils.rhumbDestinationPoint(ll.getLatitude(), ll.getLongitude(), dist, 90);
			LatLon opp = new LatLon(-lat, 180 - lon);
//			System.out.println(dist + " " + gc + " " + rhumb);
			double diff = MapUtils.getDistance(rhumb, opp) - MapUtils.getDistance(gc, opp);
//			System.out.println("Great Circle from pole dist=" + MapUtils.getDistance(gc, opp)/1000.0f);
//			System.out.println("Rhumb from pole dist=" + MapUtils.getDistance(rhumb, opp)/1000.0f);
			System.out.println("GC-Rhumb diff=" + diff/1000.0);
			Assert.assertTrue(diff > 0);
		}
		}
		System.out.println();
	}
	
	@Test
	public void movingEastGetsClosterToOppositePoint() {
		double lon = LON_TEST;
		// Test that movign east actually goes to the opposite point 
		// Based on the fact that great circle is perpindicular to the great circle passing point and North/South pole
		for (int lat = -75; lat <= 75; lat += 5) {
			double prevDist = 0;
			for (int dist = 1_000_000; dist <= 15_000_000; dist += 100_000) {
				LatLon ll = new LatLon(lat, lon);
				LatLon gc = MapUtils.greatCircleDestinationPoint(ll.getLatitude(), ll.getLongitude(), dist, 90);
				LatLon rhumb = MapUtils.rhumbDestinationPoint(ll.getLatitude(), ll.getLongitude(), dist, 90);
				LatLon opp = new LatLon(-lat, 180 - lon);
//				System.out.println(dist + " " + gc + " " + rhumb);
				
				double diff = MapUtils.getDistance(rhumb, opp) - MapUtils.getDistance(gc, opp);
//			System.out.println("Great Circle from pole dist=" + MapUtils.getDistance(gc, opp)/1000.0f);
//			System.out.println("Rhumb from pole dist=" + MapUtils.getDistance(rhumb, opp)/1000.0f);
//				System.out.println("GC-Rhumb diff=" + diff / 1000.0);
				if (lat == 0) {
					Assert.assertTrue(String.format("Failed for lat %.5f, lon %.5f - distance %.3f", (double) lat, lon,
							MapUtils.getDistance(rhumb, gc) / 1000.0), MapUtils.getDistance(rhumb, gc) < 10);
				} else {
					Assert.assertTrue(diff > 0);
				}
				
				double gcDist = MapUtils.getDistance(gc, opp); // MapUtils.getDistance(rhumb, opp) - will fail for rhumb
				if (prevDist != 0) {
					Assert.assertTrue(prevDist > gcDist);
				}
				prevDist = gcDist;
			}
		}
	}
	
	@Test
	public void movingNorthSame() {
		double dist = DIST_M;
		double lon = LON_TEST;
		for (int lat = -80; lat <= 75; lat += 5) {
			LatLon ll = new LatLon(lat, lon);
			LatLon same1 = MapUtils.greatCircleDestinationPoint(ll.getLatitude(), ll.getLongitude(), dist, 0);
			LatLon same2 = MapUtils.rhumbDestinationPoint(ll.getLatitude(), ll.getLongitude(), dist, 0);
			Assert.assertTrue(String.format("Failed for lat %.5f, lon %.5f - distance %.3f", (double) lat, lon,
					MapUtils.getDistance(same1, same2) / 1000.0), MapUtils.getDistance(same1, same2) < 10);
		}
	}
	
	@Test
	public void movingSouthSame() {
		double dist = DIST_M;
		double lon = 15;
		for (int lat = -70; lat <= 75; lat += 5) {
			LatLon ll = new LatLon(lat, lon);
			LatLon same1 = MapUtils.greatCircleDestinationPoint(ll.getLatitude(), ll.getLongitude(), dist, 180);
			LatLon same2 = MapUtils.rhumbDestinationPoint(ll.getLatitude(), ll.getLongitude(), dist, 180);
			Assert.assertTrue(String.format("Failed for lat %.5f, lon %.5f - distance %.3f", (double) lat, lon,
					MapUtils.getDistance(same1, same2) / 1000.0), MapUtils.getDistance(same1, same2) < 10);
		}
	}

	@Test // ~10ms
	public void calculateLatLonBboxTest() {
		final double MAX_DIFF = 0.04; // 4% tolerance
		for (double lat = MapUtils.MIN_LATITUDE + 10; lat <= MapUtils.MAX_LATITUDE - 10; lat += 10) {
			for (double lon = MapUtils.MIN_LONGITUDE + 10; lon <= MapUtils.MAX_LONGITUDE - 10; lon += 10) {
				for (double radius = 50; radius < 55000; radius *= 2) {
					QuadRect bboxJava = MapUtils.calculateLatLonBbox(lat, lon, (int) radius);
					double yJava = MapUtils.getDistance(bboxJava.top, bboxJava.left, bboxJava.top, bboxJava.right);
					double xJava = MapUtils.getDistance(bboxJava.top, bboxJava.left, bboxJava.bottom, bboxJava.left);
					KQuadRect bboxKotlin = KMapUtils.INSTANCE.calculateLatLonBbox(lat, lon, (int) radius);
					double yKotlin = KMapUtils.INSTANCE.getDistance(
							bboxKotlin.getTop(), bboxKotlin.getLeft(), bboxKotlin.getTop(), bboxKotlin.getRight());
					double xKotlin = KMapUtils.INSTANCE.getDistance(
							bboxKotlin.getTop(), bboxKotlin.getLeft(), bboxKotlin.getBottom(), bboxKotlin.getLeft());
					Assert.assertTrue("yJava", Math.abs(radius - yJava / 2) / radius < MAX_DIFF);
					Assert.assertTrue("xJava", Math.abs(radius - xJava / 2) / radius < MAX_DIFF);
					Assert.assertTrue("yKotlin", Math.abs(radius - yKotlin / 2) / radius < MAX_DIFF);
					Assert.assertTrue("xKotlin", Math.abs(radius - xKotlin / 2) / radius < MAX_DIFF);
				}
			}
		}
	}

}
