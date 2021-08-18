package net.osmand.util;

import net.osmand.util.OpeningHoursParser.OpeningHours;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Class used to parse opening hours
 * <p/>
 * the method "parseOpenedHours" will parse an OSM opening_hours string and
 * return an object of the type OpeningHours. That object can be used to check
 * if the OSM feature is open at a certain time.
 */
public class OpeningHoursParserTest {


	/**
	 * test if the calculated opening hours are what you expect
	 *
	 * @param time
	 *            the time to test in the format "dd.MM.yyyy HH:mm"
	 * @param hours
	 *            the OpeningHours object
	 * @param expected
	 *            the expected state
	 */
	public void testOpened(String time, OpeningHours hours, boolean expected) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).parse(time));
		boolean calculated = hours.isOpenedForTimeV2(cal, OpeningHours.ALL_SEQUENCES);
		String fmt = String.format("  %sok: Expected %s: %b = %b (rule %s)\n",
				((calculated != expected) ? "NOT " : ""), time, expected, calculated,
				hours.getCurrentRuleTime(cal, OpeningHours.ALL_SEQUENCES));
		System.out.println(fmt);
		org.junit.Assert.assertEquals(fmt, expected, calculated);
	}

	/**
	 * test if the calculated opening hours are what you expect
	 *
	 * @param time        the time to test in the format "dd.MM.yyyy HH:mm"
	 * @param hours       the OpeningHours object
	 * @param expected    the expected string in format:
	 *                         "Open from HH:mm"     - open in 5 hours
	 *                         "Will open at HH:mm"  - open in 2 hours
	 *                         "Open till HH:mm"     - close in 5 hours
	 *                         "Will close at HH:mm" - close in 2 hours
	 *                         "Will open on HH:mm (Mo,Tu,We,Th,Fr,Sa,Su)" - open in >5 hours
	 *                         "Will open tomorrow at HH:mm" - open in >5 hours tomorrow
	 *                         "Open 24/7"           - open 24/7
	 */
	private void testInfo(String time, OpeningHours hours, String expected) throws ParseException {
		testInfo(time, hours, expected, OpeningHours.ALL_SEQUENCES);
	}

	/**
	 * test if the calculated opening hours are what you expect
	 *
	 * @param time        the time to test in the format "dd.MM.yyyy HH:mm"
	 * @param hours       the OpeningHours object
	 * @param expected    the expected string in format:
	 *                         "Open from HH:mm"     - open in 5 hours
	 *                         "Will open at HH:mm"  - open in 2 hours
	 *                         "Open till HH:mm"     - close in 5 hours
	 *                         "Will close at HH:mm" - close in 2 hours
	 *                         "Will open on HH:mm (Mo,Tu,We,Th,Fr,Sa,Su)" - open in >5 hours
	 *                         "Will open tomorrow at HH:mm" - open in >5 hours tomorrow
	 *                         "Open 24/7"           - open 24/7
	 * @param sequenceIndex sequence index of rules separated by ||
	 */
	private void testInfo(String time, OpeningHours hours, String expected, int sequenceIndex) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).parse(time));

		String description;
		boolean result;
		if (sequenceIndex == OpeningHours.ALL_SEQUENCES) {
			OpeningHours.Info info = hours.getCombinedInfo(cal);
			description = info.getInfo();
			result = expected.equalsIgnoreCase(description);
		} else {
			List<OpeningHours.Info> infos = hours.getInfo(cal);
			OpeningHours.Info info = infos.get(sequenceIndex);
			description = info.getInfo();
			result = expected.equalsIgnoreCase(description);
		}

		String fmt = String.format("  %sok: Expected %s (%s): %s (rule %s)\n",
				(!result ? "NOT " : ""), time, expected, description, hours.getCurrentRuleTime(cal, sequenceIndex));
		System.out.println(fmt);
		org.junit.Assert.assertEquals(fmt, true, result);
		
	}
	
	private  void testParsedAndAssembledCorrectly(String timeString, OpeningHours hours) {
		String assembledString = hours.toString();
		boolean isCorrect = assembledString.equalsIgnoreCase(timeString);
		String fmt = String.format("  %sok: Expected: \"%s\" got: \"%s\"\n",
				(!isCorrect ? "NOT " : ""), timeString, assembledString);
		System.out.println(fmt);
		org.junit.Assert.assertEquals(fmt, true, isCorrect);

	}

	@Test
	public void testOpeningHours() throws ParseException {
	}

	private static OpeningHours parseOpenedHours(String string) {
		return OpeningHoursParser.parseOpenedHours(string);
	}
}
