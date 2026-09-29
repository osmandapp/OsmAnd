package net.osmand.test.junit;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.utils.OsmAndFormatter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

/**
 * A picked day has to become the first and the last millisecond of that local day, whatever the
 * current time of day is.
 */
@RunWith(AndroidJUnit4.class)
public class OsmAndFormatterDayBoundaryTest {

	private static final int YEAR = 2026;
	private static final int MONTH = Calendar.JUNE;
	private static final int DAY_OF_MONTH = 17;

	@Test
	public void startOfDayIsFirstMillisecondOfLocalDay() {
		assertLocalTime(OsmAndFormatter.getStartOfDay(YEAR, MONTH, DAY_OF_MONTH), 0, 0, 0, 0);
	}

	@Test
	public void endOfDayIsLastMillisecondOfLocalDay() {
		assertLocalTime(OsmAndFormatter.getEndOfDay(YEAR, MONTH, DAY_OF_MONTH), 23, 59, 59, 999);
	}

	@Test
	public void startOfDayForTimeMatchesStartOfDay() {
		Calendar afternoon = Calendar.getInstance();
		afternoon.clear();
		afternoon.set(YEAR, MONTH, DAY_OF_MONTH, 13, 37);

		long startOfDay = OsmAndFormatter.getStartOfDay(YEAR, MONTH, DAY_OF_MONTH);
		long endOfDay = OsmAndFormatter.getEndOfDay(YEAR, MONTH, DAY_OF_MONTH);

		assertEquals(startOfDay, OsmAndFormatter.getStartOfDayForTime(startOfDay));
		assertEquals(startOfDay, OsmAndFormatter.getStartOfDayForTime(afternoon.getTimeInMillis()));
		assertEquals(startOfDay, OsmAndFormatter.getStartOfDayForTime(endOfDay));
	}

	private static void assertLocalTime(long time, int hour, int minute, int second, int millisecond) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		assertEquals(YEAR, calendar.get(Calendar.YEAR));
		assertEquals(MONTH, calendar.get(Calendar.MONTH));
		assertEquals(DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
		assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(minute, calendar.get(Calendar.MINUTE));
		assertEquals(second, calendar.get(Calendar.SECOND));
		assertEquals(millisecond, calendar.get(Calendar.MILLISECOND));
	}
}
