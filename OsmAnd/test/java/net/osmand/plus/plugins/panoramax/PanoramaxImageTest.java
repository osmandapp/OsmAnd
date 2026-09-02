package net.osmand.plus.plugins.panoramax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

/**
 * The timestamp cases below are the shapes actually observed in Panoramax tiles rather than
 * invented ones. In a sample of 1249 pictures from a single tile the fractional part had three
 * digits 1037 times, two digits 125 times, none 44 times, six digits 26 times, one digit 11
 * times, five digits 5 times and four digits once, so precision is genuinely variable and a
 * fixed pattern would silently drop a sixth of the data out of the date filter.
 */
@RunWith(AndroidJUnit4.class)
public class PanoramaxImageTest {

	@Test
	public void parsesTheThreeDigitFractionThatDominatesRealTiles() {
		assertEquals(1721288904248L, PanoramaxImage.parseTimestamp("2024-07-18 07:48:24.248+00"));
	}

	@Test
	public void keepsMillisecondsWhateverThePrecisionOnTheWire() {
		long base = PanoramaxImage.parseTimestamp("2024-07-18 07:48:24+00");
		// one digit means tenths of a second, not one millisecond
		assertEquals(base + 100, PanoramaxImage.parseTimestamp("2024-07-18 07:48:24.1+00"));
		assertEquals(base + 120, PanoramaxImage.parseTimestamp("2024-07-18 07:48:24.12+00"));
		assertEquals(base + 123, PanoramaxImage.parseTimestamp("2024-07-18 07:48:24.123+00"));
		// six digits are microseconds and must be truncated, not read as milliseconds
		assertEquals(base + 123, PanoramaxImage.parseTimestamp("2024-07-18 07:48:24.123456+00"));
	}

	@Test
	public void acceptsBothSeparatorsAndEveryOffsetForm() {
		long expected = PanoramaxImage.parseTimestamp("2024-07-18 07:48:24+00");
		assertEquals(expected, PanoramaxImage.parseTimestamp("2024-07-18T07:48:24+00"));
		assertEquals(expected, PanoramaxImage.parseTimestamp("2024-07-18T07:48:24Z"));
		assertEquals(expected, PanoramaxImage.parseTimestamp("2024-07-18T07:48:24+00:00"));
		assertEquals(expected, PanoramaxImage.parseTimestamp("2024-07-18T07:48:24+0000"));
		// a missing offset is read as UTC
		assertEquals(expected, PanoramaxImage.parseTimestamp("2024-07-18T07:48:24"));
	}

	@Test
	public void appliesNonZeroOffsetsInBothDirections() {
		long utc = PanoramaxImage.parseTimestamp("2024-07-18T07:48:24Z");
		assertEquals(utc - 2 * 3600_000L, PanoramaxImage.parseTimestamp("2024-07-18T07:48:24+02:00"));
		assertEquals(utc + 3 * 3600_000L, PanoramaxImage.parseTimestamp("2024-07-18T07:48:24-0300"));
		assertEquals(utc - (5 * 3600_000L + 30 * 60_000L),
				PanoramaxImage.parseTimestamp("2024-07-18T07:48:24+05:30"));
	}

	@Test
	public void returnsZeroRatherThanThrowingOnUnusableInput() {
		assertEquals(0, PanoramaxImage.parseTimestamp(null));
		assertEquals(0, PanoramaxImage.parseTimestamp(""));
		assertEquals(0, PanoramaxImage.parseTimestamp("not a timestamp"));
		assertEquals(0, PanoramaxImage.parseTimestamp("2024-13-45 99:99:99+00"));
	}

	@Test
	public void readsEveryPropertyOfARealPictureFeature() {
		Map<String, Object> data = new HashMap<>();
		data.put(PanoramaxImage.IMAGE_ID_KEY, "40714a31-b4ad-4d44-8ba1-82691538d32c");
		data.put(PanoramaxImage.TIMESTAMP_KEY, "2024-07-18 07:48:24.248+00");
		data.put(PanoramaxImage.HEADING_KEY, 241L);
		data.put(PanoramaxImage.ACCOUNT_ID_KEY, "38676669-edf7-4831-9bc9-f90ec9da63d9");
		data.put(PanoramaxImage.SEQUENCE_ID_KEY, "37009ea3-a511-4322-8898-e39aed958ec3");
		data.put(PanoramaxImage.TYPE_KEY, "equirectangular");

		PanoramaxImage image = new PanoramaxImage(48.8566, 2.3522);
		assertTrue(image.setData(data));
		assertEquals("40714a31-b4ad-4d44-8ba1-82691538d32c", image.getImageId());
		assertEquals(1721288904248L, image.getCapturedAt());
		assertEquals(241, image.getCompassAngle(), 0.0001);
		assertEquals("38676669-edf7-4831-9bc9-f90ec9da63d9", image.getAccountId());
		assertEquals("37009ea3-a511-4322-8898-e39aed958ec3", image.getSKey());
		assertTrue(image.isPanoramicImage());
	}

	@Test
	public void treatsFlatPicturesAsNonPanoramic() {
		Map<String, Object> data = new HashMap<>();
		data.put(PanoramaxImage.IMAGE_ID_KEY, "id");
		data.put(PanoramaxImage.TYPE_KEY, "flat");

		PanoramaxImage image = new PanoramaxImage(0, 0);
		assertTrue(image.setData(data));
		assertFalse(image.isPanoramicImage());
	}

	/**
	 * Instances only have to emit the properties the tile specification makes mandatory, so a
	 * feature carrying nothing but an id has to survive. The Mapillary equivalent rejects it.
	 */
	@Test
	public void toleratesAFeatureThatCarriesOnlyAnId() {
		Map<String, Object> data = new HashMap<>();
		data.put(PanoramaxImage.IMAGE_ID_KEY, "40714a31-b4ad-4d44-8ba1-82691538d32c");

		PanoramaxImage image = new PanoramaxImage(0, 0);
		assertTrue(image.setData(data));
		assertEquals(0, image.getCapturedAt());
		assertEquals(-1, image.getCompassAngle(), 0.0001);
		assertNull(image.getAccountId());
		assertNull(image.getSKey());
		assertFalse(image.isPanoramicImage());
	}

	// The two tile layers date their features with different keys. "pictures" uses "ts" with a
	// full timestamp, "sequences" uses "date" with a plain day. Both are filtered against the
	// same date range, so reading only "ts" scored every sequence as 0 and made every sequence
	// line disappear as soon as a date filter was set. These cover that.

	@Test
	public void readsTheTimestampOfAPictureFeature() {
		Map<String, Object> picture = new HashMap<>();
		picture.put(PanoramaxImage.TIMESTAMP_KEY, "2024-07-18 07:48:24.248+00");
		assertEquals(1721288904248L, PanoramaxImage.parseCaptureTime(picture));
	}

	@Test
	public void readsThePlainDayOfASequenceFeature() {
		Map<String, Object> sequence = new HashMap<>();
		sequence.put(PanoramaxImage.DATE_KEY, "2025-04-15");
		// midnight UTC on that day, and crucially not 0
		assertEquals(1744675200000L, PanoramaxImage.parseCaptureTime(sequence));
	}

	@Test
	public void parsesADateWithNoTimePart() {
		assertEquals(1721347200000L, PanoramaxImage.parseTimestamp("2024-07-19"));
	}

	@Test
	public void prefersTheTimestampWhenAFeatureSomehowCarriesBoth() {
		Map<String, Object> both = new HashMap<>();
		both.put(PanoramaxImage.TIMESTAMP_KEY, "2024-07-18 07:48:24.248+00");
		both.put(PanoramaxImage.DATE_KEY, "2025-04-15");
		assertEquals(1721288904248L, PanoramaxImage.parseCaptureTime(both));
	}

	@Test
	public void returnsZeroWhenAFeatureCarriesNoDateAtAll() {
		assertEquals(0, PanoramaxImage.parseCaptureTime(new HashMap<String, Object>()));
		assertEquals(0, PanoramaxImage.parseCaptureTime(null));
	}

	@Test
	public void rejectsAFeatureWithoutAnId() {
		PanoramaxImage image = new PanoramaxImage(0, 0);
		assertFalse(image.setData(new HashMap<String, Object>()));
		assertFalse(image.setData(null));
	}
}
