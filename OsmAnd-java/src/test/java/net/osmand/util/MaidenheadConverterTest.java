package net.osmand.util;

import org.junit.Assert;
import org.junit.Test;
import net.osmand.data.LatLon;

public class MaidenheadConverterTest {
	@Test
	public void testAccuracyString() {
		final String[] in = {
			"AA",
			"AA00",
			"AA00AA",
			"AA00AA00",
			"AA00AA00AA",
			"AA00AA00AA00",
			"AA00AA00AA00AA",
			"AA00AA00AA00AA00",
			"AA00AA00AA00AA00AA",
			"AA00AA00AA00AA00AA00",
		};
		final double[] out = {
			180.0 / 18.0,
			180.0 / (18.0 * 10),
			180.0 / (18.0 * 10 * 24),
			180.0 / (18.0 * 10 * 24 * 10),
			180.0 / (18.0 * 10 * 24 * 10 * 24),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10 * 24),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10 * 24 * 10),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10 * 24 * 10 * 24),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10 * 24 * 10 * 24 * 10),
		};
		for (int i = 0; i < in.length; i++) {
			String loc = in[i];
			LatLon res = MaidenheadConverter.precision(loc);
			double exp = out[i];
			Assert.assertEquals(exp * 2.0, res.getLongitude(), 0.0000000000000001);
			Assert.assertEquals(exp, res.getLatitude(), 0.0000000000000001);
		}
	}

	@Test
	public void testAccuracyNumber() {
		final double[] out = {
			180.0 / 18.0,
			180.0 / (18.0 * 10),
			180.0 / (18.0 * 10 * 24),
			180.0 / (18.0 * 10 * 24 * 10),
			180.0 / (18.0 * 10 * 24 * 10 * 24),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10 * 24),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10 * 24 * 10),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10 * 24 * 10 * 24),
			180.0 / (18.0 * 10 * 24 * 10 * 24 * 10 * 24 * 10 * 24 * 10),
		};
		for (int i = 0; i < out.length; i++) {
			LatLon res = MaidenheadConverter.precision(i + 1);
			double exp = out[i];
			Assert.assertEquals(exp * 2.0, res.getLongitude(), 0.0000000000000001);
			Assert.assertEquals(exp, res.getLatitude(), 0.0000000000000001);
		}
	}

	@Test
	public void testStandardMaidenheadToLatLon() {
		final String[] in = {
			"AA00AA",
			"AA00aa",
			"JJ55MM",
			"RR99XX",
		};
		final LatLon[] out = {
			new LatLon(-90.0, -180.0),
			new LatLon(-90.0, -180.0),
			new LatLon(5.5, 11.0),
			new LatLon(89.95833333333333, 179.91666666666667),
		};
		// the 3-pair Maidenhead locator is accurate up to 5' longitude (=0.08333°) and 2.5' latitude (=0.04167°)
		double precision = 180.0 / (18.0 * 10 * 24);
		for (int i = 0; i < in.length; i++) {
			LatLon res = MaidenheadConverter.maidenheadToLatLon(in[i]);
			LatLon exp = out[i];
			Assert.assertEquals(exp.getLongitude(), res.getLongitude(), 2.0 * precision);
			Assert.assertEquals(exp.getLatitude(), res.getLatitude(), precision);
		}
	}

	@Test
	public void testStandardLatLonToMaidenhead() {
		final LatLon[] in = {
			new LatLon(-90.0, -180.0),
			new LatLon(5.5, 11.0),
			new LatLon(89.95833333333333, 179.91666666666667),
			new LatLon(90.0, 180.0),
		};
		final String[] out = {
			"AA00AA",
			"JJ55MM",
			"RR99XX",
			"AA00AA",
		};
		for (int i = 0; i < in.length; i++) {
			String res = MaidenheadConverter.latLonToMaidenhead(in[i]);
			String exp = out[i];
			Assert.assertEquals(exp, res);
			Assert.assertEquals(exp, res);
		}
	}
}
