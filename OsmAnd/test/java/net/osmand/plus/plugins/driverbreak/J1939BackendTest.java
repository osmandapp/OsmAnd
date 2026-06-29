package net.osmand.plus.plugins.driverbreak;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for J1939 PGN decoding in {@link J1939Backend}.
 */
@RunWith(AndroidJUnit4.class)
public class J1939BackendTest {

	@Test
	public void decodeFuelRateEec2Pgn0xF003() {
		byte[] frame = {0, 0x64, 0x00, 0, 0, 0, 0, 0};
		float rate = J1939Backend.decodeFuelRateEec2(frame);
		Assert.assertEquals(5.0f, rate, 0.001f);
	}

	@Test
	public void decodeFuelLevelLfe() {
		byte[] frame = {0, 0, 0x32, 0, 0, 0, 0, 0};
		float level = J1939Backend.decodeFuelLevelLfe(frame);
		Assert.assertEquals(20.0f, level, 0.001f);
	}

	@Test
	public void decodeFuelRateFeeaFromNavitSource() {
		byte[] frame = {0, 0, 0x64, 0x00, 0, 0, 0, 0};
		float rate = J1939Backend.decodeFuelRateFeea(frame);
		Assert.assertEquals(5.0f, rate, 0.001f);
	}
}
