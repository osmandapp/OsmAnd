package net.osmand.util;

import static org.junit.Assert.assertEquals;

import net.osmand.data.QuadPointDouble;
import net.osmand.shared.data.KQuadPointDouble;
import net.osmand.shared.util.KMapUtils;

import org.junit.Test;

import java.util.Random;

/**
 * {@link KMapUtils#getProjectionPoint31} is a port of {@link MapUtils#getProjectionPoint31}, added
 * for the shared {@link net.osmand.shared.routing.PrecalculatedRouteDirection}. The java one stays -
 * {@code RoutingContext} attaches direction points with it - so the two have to agree, and this
 * module is the only one where both are on the classpath.
 *
 * The projection is a routing cost, so a difference here moves routes.
 */
public class KMapUtilsProjectionCompatTest {

	private void assertSame(int px, int py, int stx, int sty, int endx, int endy) {
		QuadPointDouble expected = MapUtils.getProjectionPoint31(px, py, stx, sty, endx, endy);
		KQuadPointDouble actual = KMapUtils.INSTANCE.getProjectionPoint31(px, py, stx, sty, endx, endy);
		String where = px + "," + py + " onto " + stx + "," + sty + " - " + endx + "," + endy;
		assertEquals(where, expected.x, actual.getX(), 0);
		assertEquals(where, expected.y, actual.getY(), 0);
	}

	@Test
	public void testProjectionMatchesTheJavaOne() {
		// a point beside a segment, and both ends of the clamp
		assertSame(1000, 1000, 0, 0, 2000, 0);
		assertSame(-500, 1000, 0, 0, 2000, 0);
		assertSame(5000, 1000, 0, 0, 2000, 0);
		// a degenerate segment
		assertSame(1000, 1000, 700, 700, 700, 700);
	}

	@Test
	public void testProjectionMatchesTheJavaOneOnRandomInput() {
		Random random = new Random(20260908L);
		for (int i = 0; i < 100000; i++) {
			int stx = random.nextInt(Integer.MAX_VALUE);
			int sty = random.nextInt(Integer.MAX_VALUE);
			// segments a road could actually have, and a point somewhere near them
			int endx = clamp(stx + random.nextInt(20000) - 10000);
			int endy = clamp(sty + random.nextInt(20000) - 10000);
			int px = clamp(stx + random.nextInt(40000) - 20000);
			int py = clamp(sty + random.nextInt(40000) - 20000);
			assertSame(px, py, stx, sty, endx, endy);
		}
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(Integer.MAX_VALUE, v));
	}
}
