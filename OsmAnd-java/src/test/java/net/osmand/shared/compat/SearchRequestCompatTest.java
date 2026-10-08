package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;

import org.junit.Test;

import java.util.Random;

/**
 * {@link net.osmand.shared.binary.SearchRequest} is a copy of
 * {@link BinaryMapIndexReader.SearchRequest}, and the part of it that decides where a search looks
 * has to land on the same tiles: a box around a point, a box set outright, and the tile a point of
 * a path falls into. Searching for tracks around a tap is a box of a radius around a point, so a
 * copy that rounds differently would look in the wrong place.
 */
public class SearchRequestCompatTest {

	@Test
	public void boxOfARadiusIsTheSame() {
		Random random = new Random(3);
		for (int i = 0; i < 20000; i++) {
			double lat = random.nextDouble() * 170 - 85;
			double lon = random.nextDouble() * 360 - 180;
			int radius = 1 + random.nextInt(5000);

			BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> java =
					BinaryMapIndexReader.buildSearchRequest(0, 0, 0, 0, 15, null);
			java.setBBoxRadius(lat, lon, radius);
			net.osmand.shared.binary.SearchRequest<net.osmand.shared.routing.RouteDataObject> copy =
					net.osmand.shared.binary.SearchRequest.buildSearchRouteRequest(0, 0, 0, 0);
			copy.setBBoxRadius(lat, lon, radius);

			String m = "lat " + lat + " lon " + lon + " radius " + radius;
			assertEquals(m + " left", java.getLeft(), copy.getLeft());
			assertEquals(m + " right", java.getRight(), copy.getRight());
			assertEquals(m + " top", java.getTop(), copy.getTop());
			assertEquals(m + " bottom", java.getBottom(), copy.getBottom());
			assertEquals(m + " tile on path",
					java.getTileHashOnPath(lat, lon), copy.getTileHashOnPath(lat, lon));
		}
	}

	@Test
	public void boxSetOutrightIsTheSame() {
		Random random = new Random(5);
		for (int i = 0; i < 20000; i++) {
			int x = random.nextInt(Integer.MAX_VALUE);
			int y = random.nextInt(Integer.MAX_VALUE);
			int left = random.nextInt(Integer.MAX_VALUE);
			int top = random.nextInt(Integer.MAX_VALUE);
			int right = left + random.nextInt(1 << 20);
			int bottom = top + random.nextInt(1 << 20);

			BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> java =
					BinaryMapIndexReader.buildSearchRequest(0, 0, 0, 0, 15, null);
			java.setBBox(x, y, left, top, right, bottom);
			net.osmand.shared.binary.SearchRequest<net.osmand.shared.routing.RouteDataObject> copy =
					net.osmand.shared.binary.SearchRequest.buildSearchRouteRequest(0, 0, 0, 0);
			copy.setBBox(x, y, left, top, right, bottom);

			int l = random.nextInt(Integer.MAX_VALUE);
			int t = random.nextInt(Integer.MAX_VALUE);
			int r = l + random.nextInt(1 << 22);
			int b = t + random.nextInt(1 << 22);
			assertEquals("intersects", java.intersects(l, t, r, b), copy.intersects(l, t, r, b));
			assertEquals("contains", java.contains(l, t, r, b), copy.contains(l, t, r, b));
			assertEquals("bbox specified", java.isBboxSpecified(), copy.isBboxSpecified());
		}
	}
}
