package net.osmand.plus.plugins.panoramax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.utils.OsmAndFormatter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Two properties matter here. The filtering decision has to stay identical for both renderers,
 * and equality has to represent whether the rendered output can change, because that is what
 * decides when the OpenGL provider's cached tiles are thrown away.
 */
@RunWith(AndroidJUnit4.class)
public class PanoramaxFilterStateTest {

	private static final long JAN_01 = 1767225600000L;  // 2026-01-01 UTC
	private static final long JAN_02 = 1767312000000L;
	private static final long DEC_30 = 1798588800000L;
	private static final long DEC_31 = 1798675200000L;

	private static final String ACCOUNT = "38676669-edf7-4831-9bc9-f90ec9da63d9";

	// The Etc zones invert the sign, so these are UTC-8 and UTC+3. Neither observes DST.
	private static final ZoneId WEST = ZoneId.of("Etc/GMT+8");
	private static final ZoneId EAST = ZoneId.of("Etc/GMT-3");

	private static Map<String, Object> picture(String account, String timestamp, String type) {
		Map<String, Object> data = new HashMap<>();
		data.put(PanoramaxImage.IMAGE_ID_KEY, "40714a31-b4ad-4d44-8ba1-82691538d32c");
		if (account != null) {
			data.put(PanoramaxImage.ACCOUNT_ID_KEY, account);
		}
		if (timestamp != null) {
			data.put(PanoramaxImage.TIMESTAMP_KEY, timestamp);
		}
		if (type != null) {
			data.put(PanoramaxImage.TYPE_KEY, type);
		}
		return data;
	}

	@Test
	public void filtersOutDataThatIsNotAFeature() {
		PanoramaxFilterState state = new PanoramaxFilterState(false, "", 0, 0, false);
		assertTrue(state.filtered(null));
		assertTrue(state.filtered("not a feature"));
	}

	@Test
	public void keepsEverythingWhenNothingIsFiltered() {
		PanoramaxFilterState state = new PanoramaxFilterState(false, "", 0, 0, false);
		assertFalse(state.filtered(picture(ACCOUNT, "2026-06-15 12:00:00+00", "flat")));
	}

	@Test
	public void disabledFilterIgnoresContributorAndDates() {
		// Values that would exclude the picture if the filter were on.
		PanoramaxFilterState state = new PanoramaxFilterState(false, "somebody-else", DEC_31, DEC_31, false);
		assertFalse(state.filtered(picture(ACCOUNT, "2026-06-15 12:00:00+00", "flat")));
	}

	@Test
	public void contributorMustMatchWhenSet() {
		PanoramaxFilterState state = new PanoramaxFilterState(true, ACCOUNT, 0, 0, false);
		assertFalse(state.filtered(picture(ACCOUNT, null, null)));
		assertTrue(state.filtered(picture("somebody-else", null, null)));
		// A feature with no account cannot match a contributor filter.
		assertTrue(state.filtered(picture(null, null, null)));
	}

	@Test
	public void fromIsALowerBound() {
		PanoramaxFilterState state = new PanoramaxFilterState(true, "", JAN_02, 0, false);
		assertTrue(state.filtered(picture(null, "2026-01-01 12:00:00+00", null)));
		assertFalse(state.filtered(picture(null, "2026-01-03 12:00:00+00", null)));
	}

	@Test
	public void toIsAnUpperBound() {
		PanoramaxFilterState state = new PanoramaxFilterState(true, "", 0, DEC_30, false);
		assertFalse(state.filtered(picture(null, "2026-12-29 12:00:00+00", null)));
		assertTrue(state.filtered(picture(null, "2026-12-31 12:00:00+00", null)));
	}

	@Test
	public void bothBoundariesFormARange() {
		PanoramaxFilterState state = new PanoramaxFilterState(true, "", JAN_02, DEC_30, false);
		assertTrue(state.filtered(picture(null, "2026-01-01 12:00:00+00", null)));
		assertFalse(state.filtered(picture(null, "2026-06-15 12:00:00+00", null)));
		assertTrue(state.filtered(picture(null, "2026-12-31 12:00:00+00", null)));
	}

	private static Map<String, Object> sequence(String date) {
		Map<String, Object> data = new HashMap<>();
		data.put(PanoramaxImage.IMAGE_ID_KEY, "37009ea3-a511-4322-8898-e39aed958ec3");
		data.put(PanoramaxImage.DATE_KEY, date);
		return data;
	}

	// A sequence is dated by a plain day, so it is compared by date. The boundaries below are
	// built the way the filter screen builds them, which keeps these cases zone independent.

	@Test
	public void dateOnlySequenceOnTheFromDayIsKept() {
		PanoramaxFilterState state = new PanoramaxFilterState(true, "",
				OsmAndFormatter.getStartOfDay(2026, Calendar.SEPTEMBER, 5), 0, false);
		assertFalse(state.filtered(sequence("2026-09-05")));
		assertTrue(state.filtered(sequence("2026-09-04")));
	}

	@Test
	public void dateOnlySequenceOnTheToDayIsKept() {
		PanoramaxFilterState state = new PanoramaxFilterState(true, "", 0,
				OsmAndFormatter.getEndOfDay(2026, Calendar.SEPTEMBER, 5), false);
		assertFalse(state.filtered(sequence("2026-09-05")));
		assertTrue(state.filtered(sequence("2026-09-06")));
	}

	@Test
	public void dateOnlySequenceIsKeptBySingleDayRange() {
		PanoramaxFilterState state = new PanoramaxFilterState(true, "",
				OsmAndFormatter.getStartOfDay(2026, Calendar.SEPTEMBER, 5),
				OsmAndFormatter.getEndOfDay(2026, Calendar.SEPTEMBER, 5), false);
		assertFalse(state.filtered(sequence("2026-09-05")));
		assertTrue(state.filtered(sequence("2026-09-04")));
		assertTrue(state.filtered(sequence("2026-09-06")));
	}

	@Test
	public void panoOnlyAppliesWhetherOrNotTheFilterIsEnabled() {
		for (boolean enabled : new boolean[] {true, false}) {
			PanoramaxFilterState state = new PanoramaxFilterState(enabled, "", 0, 0, true);
			assertFalse(state.filtered(picture(null, null, "equirectangular")));
			assertTrue(state.filtered(picture(null, null, "flat")));
			// The property can be absent, which is not a panorama.
			assertTrue(state.filtered(picture(null, null, null)));
		}
	}

	// The old key was (hasFilter << 1) + from + to + pano, so shifting both dates inward by a
	// day left the sum unchanged and the OpenGL provider kept serving tiles from the old range.
	@Test
	public void equalityDistinguishesDateRangesThatUsedToCollide() {
		PanoramaxFilterState wide = new PanoramaxFilterState(true, "", JAN_01, DEC_31, false);
		PanoramaxFilterState narrow = new PanoramaxFilterState(true, "", JAN_02, DEC_30, false);
		assertEquals(JAN_01 + DEC_31, JAN_02 + DEC_30);
		assertNotEquals(wide, narrow);
	}

	@Test
	public void equalityCoversTheContributorTheOldKeyIgnored() {
		PanoramaxFilterState one = new PanoramaxFilterState(true, ACCOUNT, 0, 0, false);
		PanoramaxFilterState other = new PanoramaxFilterState(true, "somebody-else", 0, 0, false);
		assertNotEquals(one, other);
	}

	@Test
	public void inactiveContributorAndDatesDoNotChangeEffectiveState() {
		PanoramaxFilterState one = new PanoramaxFilterState(false, ACCOUNT, JAN_01, DEC_31, false);
		PanoramaxFilterState other = new PanoramaxFilterState(false, "somebody-else", JAN_02, DEC_30, false);
		assertEquals(one, other);
		assertEquals(one.hashCode(), other.hashCode());
	}

	@Test
	public void panoOnlyChangesEffectiveStateEvenWhileDisabled() {
		PanoramaxFilterState off = new PanoramaxFilterState(false, "", 0, 0, false);
		PanoramaxFilterState on = new PanoramaxFilterState(false, "", 0, 0, true);
		assertNotEquals(off, on);
	}

	@Test
	public void equalStatesHaveEqualHashCodes() {
		PanoramaxFilterState one = new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_30, true);
		PanoramaxFilterState other = new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_30, true);
		assertEquals(one, other);
		assertEquals(one.hashCode(), other.hashCode());
	}

	@Test
	public void equalStatesShareACacheKey() {
		PanoramaxFilterState one = new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_30, true);
		PanoramaxFilterState other = new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_30, true);
		assertEquals(one.getCacheKey(), other.getCacheKey());
	}

	@Test
	public void everyRenderedComponentChangesTheCacheKey() {
		String base = new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_30, false).getCacheKey();
		assertNotEquals(base, new PanoramaxFilterState(false, ACCOUNT, JAN_02, DEC_30, false).getCacheKey());
		assertNotEquals(base, new PanoramaxFilterState(true, "somebody-else", JAN_02, DEC_30, false).getCacheKey());
		assertNotEquals(base, new PanoramaxFilterState(true, ACCOUNT, JAN_01, DEC_30, false).getCacheKey());
		assertNotEquals(base, new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_31, false).getCacheKey());
		assertNotEquals(base, new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_30, true).getCacheKey());
	}

	@Test
	public void disabledStateIgnoresContributorAndDatesInTheCacheKey() {
		PanoramaxFilterState one = new PanoramaxFilterState(false, ACCOUNT, JAN_01, DEC_31, false);
		PanoramaxFilterState other = new PanoramaxFilterState(false, "somebody-else", JAN_02, DEC_30, false);
		assertEquals(one.getCacheKey(), other.getCacheKey());
	}

	@Test
	public void panoOnlyChangesTheCacheKeyEvenWhileDisabled() {
		PanoramaxFilterState off = new PanoramaxFilterState(false, "", 0, 0, false);
		PanoramaxFilterState on = new PanoramaxFilterState(false, "", 0, 0, true);
		assertNotEquals(off.getCacheKey(), on.getCacheKey());
	}

	// The stored key starts out empty, so an empty key would skip the migration invalidation.
	@Test
	public void cacheKeyIsNeverEmpty() {
		assertFalse(new PanoramaxFilterState(false, "", 0, 0, false).getCacheKey().isEmpty());
		assertFalse(new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_30, true).getCacheKey().isEmpty());
	}

	// The same instant can resolve to different local dates in different zones.
	@Test
	public void renderIdentityFollowsTheLocalDateBoundaries() {
		PanoramaxFilterState west = new PanoramaxFilterState(true, "", JAN_02, 0, false, WEST);
		PanoramaxFilterState east = new PanoramaxFilterState(true, "", JAN_02, 0, false, EAST);

		assertNotEquals(west, east);
		assertNotEquals(west.getCacheKey(), east.getCacheKey());
	}

	@Test
	public void sameZoneKeepsTheSameRenderIdentity() {
		PanoramaxFilterState one = new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_30, true, WEST);
		PanoramaxFilterState other = new PanoramaxFilterState(true, ACCOUNT, JAN_02, DEC_30, true, WEST);

		assertEquals(one, other);
		assertEquals(one.hashCode(), other.hashCode());
		assertEquals(one.getCacheKey(), other.getCacheKey());
	}
}
