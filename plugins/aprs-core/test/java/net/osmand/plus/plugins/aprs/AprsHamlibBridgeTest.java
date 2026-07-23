package net.osmand.plus.plugins.aprs;

import org.junit.Assert;
import org.junit.Test;

public class AprsHamlibBridgeTest {

	@Test
	public void frequencyCommandFormat() {
		double mhz = 144.230;
		long hz = Math.round(mhz * 1_000_000);
		String cmd = "F " + hz + "\n";
		Assert.assertEquals("F 144230000\n", cmd);
	}
}
