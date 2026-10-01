package net.osmand.plus.plugins.externalsensors;

import static net.osmand.plus.plugins.externalsensors.devices.ble.TpmsAdvertisementDecoder.COMPANY_GENERIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.plugins.externalsensors.devices.ble.TpmsAdvertisementDecoder;
import net.osmand.plus.plugins.externalsensors.devices.ble.TpmsAdvertisementDecoder.Reading;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Packets are the generic TPMS samples from the Theengs decoder tests (theengs/decoder, test_ble.cpp),
 * with the 2-byte company id 00 01 stripped as Android's ScanRecord does.
 */
@RunWith(AndroidJUnit4.class)
public class TpmsAdvertisementDecoderTest {

	@Test
	public void decodesGenericFrontLeft() {
		Reading r = decode("80eacaddeefff46503007c0c00003300");
		assertEquals(222.708f, r.getPressureKpa(), 0.001f);
		assertEquals(31.96f, r.getTemperatureC(), 0.001f);
		assertEquals(51, (int) r.getBatteryPercent());
		assertEquals(0, (int) r.getWheel());
		assertFalse(r.getAlarm());
	}

	@Test
	public void decodesGenericRearLeft() {
		Reading r = decode("82eacaddeeff11fc0300aa0600005300");
		assertEquals(261.137f, r.getPressureKpa(), 0.001f);
		assertEquals(17.06f, r.getTemperatureC(), 0.001f);
		assertEquals(83, (int) r.getBatteryPercent());
		assertEquals(2, (int) r.getWheel());
	}

	@Test
	public void ignoresOtherDevicesWithTheSameCompanyId() {
		assertNull(TpmsAdvertisementDecoder.decode(COMPANY_GENERIC, hex("80aaaaddeefff46503007c0c00003300")));
		assertNull(TpmsAdvertisementDecoder.decode(COMPANY_GENERIC, hex("80eacaddeeff")));
		assertNull(TpmsAdvertisementDecoder.decode(0x004C, hex("80eacaddeefff46503007c0c00003300")));
	}

	private static Reading decode(String data) {
		Reading reading = TpmsAdvertisementDecoder.decode(COMPANY_GENERIC, hex(data));
		assertNotNull(reading);
		return reading;
	}

	private static byte[] hex(String s) {
		byte[] res = new byte[s.length() / 2];
		for (int i = 0; i < res.length; i++) {
			res[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
		}
		return res;
	}
}
