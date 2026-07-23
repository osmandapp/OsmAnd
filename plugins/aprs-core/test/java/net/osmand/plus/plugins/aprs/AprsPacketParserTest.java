package net.osmand.plus.plugins.aprs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AprsPacketParserTest {

	private AprsPacketParser parser;

	@Before
	public void setUp() {
		parser = new AprsPacketParser();
	}

	@Test
	public void parseCourseSpeedAfterSymbolWithoutLeadingSlash() {
		AprsStation s = parser.parseAprsPayload("LB1XX-9",
				"!6041.12N/01043.40E/>045/011/Portable QRV 144.800");
		Assert.assertNotNull(s);
		Assert.assertEquals('>', s.getSymbolCode());
		Assert.assertEquals(45, s.getCourse());
		Assert.assertEquals(11, s.getSpeed());
		Assert.assertTrue(s.getComment().contains("Portable"));
		Assert.assertEquals(144.800, s.getQrvFrequencyMhz(), 0.001);
	}

	@Test
	public void parseUncompressedPosition() {
		AprsStation s = parser.parseAprsPayload("LA1BBA-7",
				"!5958.45N/01044.12E>Test comment QRV 144.230");
		Assert.assertNotNull(s);
		Assert.assertEquals("LA1BBA-7", s.getCallsign());
		Assert.assertEquals('/', s.getSymbolTable());
		Assert.assertEquals('>', s.getSymbolCode());
		Assert.assertEquals(59.974167, s.getLatitude(), 0.01);
		Assert.assertEquals(10.735333, s.getLongitude(), 0.01);
		Assert.assertEquals(144.230, s.getQrvFrequencyMhz(), 0.001);
	}

	@Test
	public void qrvDetectionCaseInsensitive() {
		AprsStation s = new AprsStation("TEST-1");
		s.setComment("Operating qrv 145.500 today");
		parser.applyQrvDetection(s);
		Assert.assertEquals(145.500, s.getQrvFrequencyMhz(), 0.001);

		s.setComment("Qrv 144.800");
		parser.applyQrvDetection(s);
		Assert.assertEquals(144.800, s.getQrvFrequencyMhz(), 0.001);
	}

	@Test
	public void parseMessagePacket() {
		AprsStation s = parser.parseAprsPayload("LA2XYZ",
				":LA1ABC :Hello world QRV 144.800");
		Assert.assertNotNull(s);
		Assert.assertNotNull(s.getMessageText());
		Assert.assertTrue(s.getMessageText().contains("Hello"));
		Assert.assertEquals(144.800, s.getQrvFrequencyMhz(), 0.001);
	}

	@Test
	public void parseCompressedPosition() {
		AprsStation s = parser.parseAprsPayload("N0CALL",
				"=/5L!!<(>>`\"5b");
		Assert.assertNotNull(s);
		Assert.assertTrue(s.hasPosition());
	}
}
