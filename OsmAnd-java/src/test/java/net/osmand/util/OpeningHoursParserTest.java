package net.osmand.util;

import net.osmand.util.OpeningHoursParser.OpeningHours;

import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
	 * @param time     the time to test in the format "dd.MM.yyyy HH:mm"
	 * @param hours    the OpeningHours object
	 * @param expected the expected state
	 */
	public void testOpened(String time, OpeningHours hours, boolean expected) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).parse(time));
		boolean calculated = hours.isOpenedForTimeV2(cal, OpeningHours.ALL_SEQUENCES);
		String fmt = String.format("  %sok: Expected %s: %b = %b (rule %s)\n",
				((calculated != expected) ? "NOT " : ""), time, expected, calculated,
				hours.getCurrentRuleTime(cal, OpeningHours.ALL_SEQUENCES));
		System.out.println(fmt);
		Assert.assertEquals(fmt, expected, calculated);
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

		OpeningHours.Info info = sequenceIndex == OpeningHours.ALL_SEQUENCES
				? hours.getCombinedInfo(cal)
				: hours.getInfo(cal).get(sequenceIndex);
		String description = info.getInfo();
		boolean result = expected.equalsIgnoreCase(description);

		String fmt = String.format("  %sok: Expected %s (%s): %s (rule %s)\n",
				(!result ? "NOT " : ""), time, expected, description, hours.getCurrentRuleTime(cal, sequenceIndex));
		System.out.println(fmt);
		Assert.assertTrue(fmt, result);
	}

	private void testParsedAndAssembledCorrectly(String expected, OpeningHours hours) {
		String assembledString = hours.toString();
		boolean isCorrect = assembledString.equalsIgnoreCase(expected);
		String fmt = String.format("  %sok: Expected: \"%s\" got: \"%s\"\n",
				(!isCorrect ? "NOT " : ""), expected, assembledString);
		System.out.println(fmt);
		Assert.assertTrue(fmt, isCorrect);
	}

	@Test
	public void testOpeningHours() throws ParseException {
		// 0. not properly supported
		// hours = parseOpenedHours("Mo-Su (sunrise-00:30)-(sunset+00:30)");

		OpeningHoursParser.initLocalStrings(Locale.UK);
		OpeningHoursParser.setTwelveHourFormattingEnabled(false, Locale.UK);
		OpeningHours hours = parseOpenedHours("Mo-Fr 11:00-22:00; Sa,Su,PH 12:00-22:00; 2022 jul 31-2022 Aug 31 off \"Betriebsferien\"");
		System.out.println(hours);
		testOpened("25.08.2022 11:30", hours, false);
		testOpened("31.08.2022 21:59", hours, false);
		testOpened("01.09.2022 11:00", hours, true); // Thursday
		testInfo("25.08.2022 11:30", hours, "Will open on 11:00 Thu."); // (2022 jul 31-2022 Aug 31 off "Betriebsferien")

		hours = parseOpenedHours("Mo-Fr 10:00-18:30; We 10:00-14:00; Sa 10:00-13:00; Dec-Feb Mo-Fr 11:00-17:00; Dec-Feb We off; Dec-Feb Sa 11:00-13:00; Dec 24-Dec 31 off \"Inventurarbeiten\"; PH off");
		System.out.println(hours);
		testOpened("05.11.2022 10:30", hours, true); // saturday
		testOpened("05.12.2022 10:30", hours, false); // Thursday
		testOpened("05.12.2022 11:30", hours, true);
		testOpened("30.12.2022 11:00", hours, false);
		testInfo("29.12.2022 14:00", hours, "Will open on 11:00 Mon.");
		testInfo("30.12.2022 14:00", hours, "Will open on 11:00 Mon.");

		hours = parseOpenedHours("2022 Oct 24 - 2023 Oct 30");
		System.out.println(hours);
		testOpened("20.10.2022 10:00", hours, false);
		testOpened("20.06.2023 10:00", hours, true);
		testOpened("01.11.2023 10:00", hours, false);
		testOpened("31.12.2023 10:00", hours, false);
		
		hours = parseOpenedHours("2022 Oct 30 - 2023 Oct 24");
		System.out.println(hours);
		testOpened("25.10.2023 10:00", hours, false);
		
		hours = parseOpenedHours("2022 Oct 24 - 2023 Aug 30");
		System.out.println(hours);
		testOpened("25.10.2022 10:00", hours, true);
		testOpened("25.09.2023 10:00", hours, false);
		testOpened("25.09.2022 10:00", hours, false);
		testOpened("25.08.2022 10:00", hours, false);
		testOpened("25.08.2023 10:00", hours, true);

//		test for opening_hours not handled correctly #17521
		hours = parseOpenedHours("11:00-14:00,17:00-22:00; We off; Fr,Sa 11:00-14:00,17:00-00:00");
		System.out.println(hours);
		testOpened("28.06.2023 12:00", hours, false); // We 

		hours = parseOpenedHours("Mo 09:00-12:00; We,Sa 13:30-17:00, Apr 01-Oct 31 We,Sa 17:00-18:30; PH off");
		System.out.println(hours);
		testInfo("03.10.2020 14:00", hours, "Open till 18:30");
		hours = parseOpenedHours("PH,Mo-Su 09:00-22:00");
		System.out.println(hours);
		testOpened("13.10.2021 11:54", hours, true);
		hours = parseOpenedHours("Mo-We 07:00-21:00, Th-Fr 07:00-21:30, PH,Sa-Su 08:00-21:00");
		System.out.println(hours);
		testOpened("29.08.2021 10:09", hours, true);
		hours = parseOpenedHours("Mo-Fr 08:00-12:30, Mo-We 12:30-16:30 \"Sur rendez-vous\", Fr 12:30-15:30 \"Sur rendez-vous\"");
		System.out.println(hours);
		testInfo("13.10.2019 18:00", hours, "Will open tomorrow at 08:00");

		hours = parseOpenedHours("2019 Oct 1 - 2024 dec 31 ");
		System.out.println(hours);
		testOpened("30.09.2019 10:30", hours, false);
		testOpened("01.10.2019 10:30", hours, true);
		testOpened("05.02.2023 10:30", hours, true);
		testOpened("31.08.2024 10:30", hours, true);
		testOpened("31.12.2024 10:30", hours, true);
		testOpened("01.01.2025 10:30", hours, false);

		hours = parseOpenedHours("2019 Oct - 2024 dec");
		System.out.println(hours);
		testOpened("30.09.2019 10:30", hours, false);
		testOpened("01.10.2019 10:30", hours, true);
		testOpened("05.02.2023 10:30", hours, true);
		testOpened("31.12.2024 10:30", hours, true);
		testOpened("01.01.2025 10:30", hours, false);

		hours = parseOpenedHours("2019 Apr 1 - 2020 Apr 1");
		System.out.println(hours);
		testOpened("01.04.2018 15:00", hours, false);
		testOpened("01.04.2019 15:00", hours, true);
		testOpened("01.04.2020 15:00", hours, true);
		testOpened("01.08.2019 15:00", hours, true);

		hours = parseOpenedHours("2019 Apr 15 -  2020 Mar 1");
		System.out.println(hours);
		testOpened("01.04.2018 15:00", hours, false);
		testOpened("01.04.2019 15:00", hours, false);
		testOpened("15.04.2019 15:00", hours, true);
		testOpened("15.09.2019 15:00", hours, true);
		testOpened("15.02.2020 15:00", hours, true);
		testOpened("15.03.2020 15:00", hours, false);
		testOpened("15.04.2020 15:00", hours, false);

		hours = parseOpenedHours("2019 Jul 23 05:00-24:00; 2019 Jul 24-2019 Jul 26 00:00-24:00; 2019 Jul 27 00:00-18:00");
		System.out.println(hours);
		testOpened("23.07.2018 15:00", hours, false);
		testOpened("23.07.2019 15:00", hours, true);
		testOpened("23.07.2019 04:00", hours, false);
		testOpened("23.07.2020 15:00", hours, false);
		testOpened("25.07.2018 15:00", hours, false);
		testOpened("24.07.2019 15:00", hours, true);
		testOpened("25.07.2019 04:00", hours, true);
		testOpened("26.07.2019 15:00", hours, true);
		testOpened("25.07.2020 15:00", hours, false);
		testOpened("27.07.2018 15:00", hours, false);
		testOpened("27.07.2019 15:00", hours, true);
		testOpened("27.07.2019 19:00", hours, false);
		testOpened("27.07.2020 15:00", hours, false);

		hours = parseOpenedHours("2019 Sep 1 - 2022 Apr 1");
		System.out.println(hours);
		testOpened("01.02.2018 15:00", hours, false);
		testOpened("29.05.2019 15:00", hours, false);
		testOpened("05.09.2019 11:00", hours, true);
		testOpened("05.02.2020 11:00", hours, true);
		testOpened("03.06.2020 11:00", hours, true);
		testOpened("05.02.2021 11:00", hours, true);
		testOpened("05.02.2022 11:00", hours, true);
		testOpened("05.02.2023 11:00", hours, false);

		hours = parseOpenedHours("2019 Apr 15 - 2019 Sep 1: Mo-Fr 00:00-24:00");
		System.out.println(hours);
		testOpened("06.04.2019 15:00", hours, false);
		testOpened("29.05.2019 15:00", hours, true);
		testOpened("25.07.2019 11:00", hours, true);
		testOpened("12.07.2018 11:00", hours, false);
		testOpened("18.07.2020 11:00", hours, false);
		testOpened("28.07.2021 11:00", hours, false);

		hours = parseOpenedHours("2019 Sep 1 - 2020 Apr 1");
		System.out.println(hours);
		testOpened("01.04.2019 15:00", hours, false);
		testOpened("29.05.2019 15:00", hours, false);
		testOpened("05.09.2019 11:00", hours, true);
		testOpened("05.02.2020 11:00", hours, true);
		testOpened("05.06.2020 11:00", hours, false);
		testOpened("05.02.2021 11:00", hours, false);

		hours = parseOpenedHours("2019 Apr 15 - 2019 Sep 1");
		System.out.println(hours);
		testOpened("01.04.2019 15:00", hours, false);
		testOpened("29.05.2019 15:00", hours, true);
		testOpened("27.07.2019 15:00", hours, true);
		testOpened("05.09.2019 11:00", hours, false);
		testOpened("05.06.2018 11:00", hours, false);
		testOpened("05.06.2020 11:00", hours, false);

		hours = parseOpenedHours("Apr 15 - Sep 1");
		System.out.println(hours);
		testOpened("01.04.2019 15:00", hours, false);
		testOpened("29.05.2019 15:00", hours, true);
		testOpened("27.07.2019 15:00", hours, true);
		testOpened("05.09.2019 11:00", hours, false);

		hours = parseOpenedHours("Apr 15 - Sep 1: Mo-Fr 00:00-24:00");
		System.out.println(hours);
		testOpened("01.04.2019 15:00", hours, false);
		testOpened("29.05.2019 15:00", hours, true);
		testOpened("24.07.2019 15:00", hours, true);
		testOpened("27.07.2019 15:00", hours, false);
		testOpened("05.09.2019 11:00", hours, false);

		hours = parseOpenedHours("Apr 05-Oct 24: Fr 08:00-16:00");
		System.out.println(hours);
		testOpened("26.08.2018 15:00", hours, false);
		testOpened("29.03.2019 15:00", hours, false);
		testOpened("05.04.2019 11:00", hours, true);

		hours = parseOpenedHours("Oct 24-Apr 05: Fr 08:00-16:00");
		System.out.println(hours);
		testOpened("26.08.2018 15:00", hours, false);
		testOpened("29.03.2019 15:00", hours, true);
		testOpened("26.04.2019 11:00", hours, false);

		hours = parseOpenedHours("Oct 24-Apr 05, Jun 10-Jun 20, Jul 6-12: Fr 08:00-16:00");
		System.out.println(hours);
		testOpened("26.08.2018 15:00", hours, false);
		testOpened("02.01.2019 15:00", hours, false);
		testOpened("29.03.2019 15:00", hours, true);
		testOpened("26.04.2019 11:00", hours, false);

		hours = parseOpenedHours("Apr 05-24: Fr 08:00-16:00");
		System.out.println(hours);
		testOpened("12.10.2018 11:00", hours, false);
		testOpened("12.04.2019 15:00", hours, true);
		testOpened("27.04.2019 15:00", hours, false);

		hours = parseOpenedHours("Apr 5: Fr 08:00-16:00");
		System.out.println(hours);
		testOpened("05.04.2019 15:00", hours, true);
		testOpened("06.04.2019 15:00", hours, false);

		hours = parseOpenedHours("Apr 24-05: Fr 08:00-16:00");
		System.out.println(hours);
		testOpened("12.10.2018 11:00", hours, false);
		testOpened("12.04.2018 15:00", hours, false);

		hours = parseOpenedHours("Apr: Fr 08:00-16:00");
		System.out.println(hours);
		testOpened("12.10.2018 11:00", hours, false);
		testOpened("12.04.2019 15:00", hours, true);

		hours = parseOpenedHours("Apr-Oct: Fr 08:00-16:00");
		System.out.println(hours);
		testOpened("09.11.2018 11:00", hours, false);
		testOpened("12.10.2018 11:00", hours, true);
		testOpened("24.08.2018 15:00", hours, true);
		testOpened("09.03.2018 15:00", hours, false);

		hours = parseOpenedHours("Apr, Oct: Fr 08:00-16:00");
		System.out.println(hours);
		testOpened("09.11.2018 11:00", hours, false);
		testOpened("12.10.2018 11:00", hours, true);
		testOpened("24.08.2018 15:00", hours, false);
		testOpened("12.04.2019 15:00", hours, true);

		// test basic case
		hours = parseOpenedHours("Mo-Fr 08:30-14:40"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("09.08.2012 11:00", hours, true);
		testOpened("09.08.2012 16:00", hours, false);
		//hours = parseOpenedHours("mo-fr 07:00-19:00; sa 12:00-18:00");

		String string = "Mo-Fr 11:30-15:00, 17:30-23:00; Sa, Su, PH 11:30-23:00";
		hours = parseOpenedHours(string);
		testParsedAndAssembledCorrectly("Mo-Fr 11:30-15:00, 17:30-23:00; Sa, Su, PH 11:30-23:00", hours);
		System.out.println(hours);
		testOpened("7.09.2015 14:54", hours, true); // monday
		testOpened("7.09.2015 15:05", hours, false);
		testOpened("6.09.2015 16:05", hours, true);

		// two time and date ranges
		hours = parseOpenedHours("Mo-We, Fr 08:30-14:40,15:00-19:00"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 14:00", hours, true);
		testOpened("08.08.2012 14:50", hours, false);
		testOpened("10.08.2012 15:00", hours, true);

		// test exception on general schema
		hours = parseOpenedHours("Mo-Sa 08:30-14:40; Tu 08:00 - 14:00"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("07.08.2012 14:20", hours, false);
		testOpened("07.08.2012 08:15", hours, true); // Tuesday

		// test off value
		hours = parseOpenedHours("Mo-Sa 09:00-18:25; Th off"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 12:00", hours, true);
		testOpened("09.08.2012 12:00", hours, false);

		// test 24/7
		hours = parseOpenedHours("24/7"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 23:59", hours, true);
		testOpened("08.08.2012 12:23", hours, true);
		testOpened("08.08.2012 06:23", hours, true);
		hours = parseOpenedHours("24/7 closed \"Temporarily, for major repairs\"");
		System.out.println(hours);
		testOpened("13.10.2019 18:00", hours, false);
		testInfo("13.10.2019 18:00", hours, "24/7 off - Temporarily, for major repairs");

		// some people seem to use the following syntax:
		hours = parseOpenedHours("Sa-Su 24/7");
		System.out.println(hours);
		hours = parseOpenedHours("Mo-Fr 9-19");
		System.out.println(hours);
		hours = parseOpenedHours("09:00-17:00");
		System.out.println(hours);
		hours = parseOpenedHours("sunrise-sunset");
		System.out.println(hours);
		hours = parseOpenedHours("10:00+");
		System.out.println(hours);
		hours = parseOpenedHours("Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise");
		System.out.println(hours);
		testOpened("12.08.2012 04:00", hours, true);
		testOpened("12.08.2012 23:00", hours, true);
		testOpened("08.08.2012 12:00", hours, false);
		testOpened("08.08.2012 05:00", hours, true);

		// test simple day wrap
		hours = parseOpenedHours("Mo 20:00-02:00");
		System.out.println(hours);
		testOpened("05.05.2013 10:30", hours, false);
		testOpened("05.05.2013 23:59", hours, false);
		testOpened("06.05.2013 10:30", hours, false);
		testOpened("06.05.2013 20:30", hours, true);
		testOpened("06.05.2013 23:59", hours, true);
		testOpened("07.05.2013 00:00", hours, true);
		testOpened("07.05.2013 00:30", hours, true);
		testOpened("07.05.2013 01:59", hours, true);
		testOpened("07.05.2013 20:30", hours, false);

		// test maximum day wrap
		hours = parseOpenedHours("Su 10:00-10:00");
		System.out.println(hours);
		testOpened("05.05.2013 09:59", hours, false);
		testOpened("05.05.2013 10:00", hours, true);
		testOpened("05.05.2013 23:59", hours, true);
		testOpened("06.05.2013 00:00", hours, true);
		testOpened("06.05.2013 09:59", hours, true);
		testOpened("06.05.2013 10:00", hours, false);

		// test day wrap as seen on OSM
		hours = parseOpenedHours("Tu-Th 07:00-2:00; Fr 17:00-4:00; Sa 18:00-05:00; Su,Mo off");
		System.out.println(hours);
		testOpened("05.05.2013 04:59", hours, true); // sunday 05.05.2013
		testOpened("05.05.2013 05:00", hours, false);
		testOpened("05.05.2013 12:30", hours, false);
		testOpened("06.05.2013 10:30", hours, false);
		testOpened("07.05.2013 01:00", hours, false);
		testOpened("07.05.2013 20:25", hours, true);
		testOpened("07.05.2013 23:59", hours, true);
		testOpened("08.05.2013 00:00", hours, true);
		testOpened("08.05.2013 02:00", hours, false);

		// test day wrap as seen on OSM
		hours = parseOpenedHours("Mo-Th 09:00-03:00; Fr-Sa 09:00-04:00; Su off");
		testOpened("11.05.2015 08:59", hours, false);
		testOpened("11.05.2015 09:01", hours, true);
		testOpened("12.05.2015 02:59", hours, true);
		testOpened("12.05.2015 03:00", hours, false);
		testOpened("16.05.2015 03:59", hours, true);
		testOpened("16.05.2015 04:01", hours, false);
		testOpened("17.05.2015 01:00", hours, true);
		testOpened("17.05.2015 04:01", hours, false);

		hours = parseOpenedHours("Tu-Th 07:00-2:00; Fr 17:00-4:00; Sa 18:00-05:00; Su,Mo off");
		testOpened("11.05.2015 08:59", hours, false);
		testOpened("11.05.2015 09:01", hours, false);
		testOpened("12.05.2015 01:59", hours, false);
		testOpened("12.05.2015 02:59", hours, false);
		testOpened("12.05.2015 03:00", hours, false);
		testOpened("13.05.2015 01:59", hours, true);
		testOpened("13.05.2015 02:59", hours, false);
		testOpened("16.05.2015 03:59", hours, true);
		testOpened("16.05.2015 04:01", hours, false);
		testOpened("17.05.2015 01:00", hours, true);
		testOpened("17.05.2015 05:01", hours, false);

		// tests single month value
		hours = parseOpenedHours("May: 07:00-19:00");
		System.out.println(hours);
		testOpened("05.05.2013 12:00", hours, true);
		testOpened("05.05.2013 05:00", hours, false);
		testOpened("05.05.2013 21:00", hours, false);
		testOpened("05.01.2013 12:00", hours, false);
		testOpened("05.01.2013 05:00", hours, false);

		// tests multi month value
		hours = parseOpenedHours("Apr-Sep 8:00-22:00; Oct-Mar 10:00-18:00");
		System.out.println(hours);
		testOpened("05.03.2013 15:00", hours, true);
		testOpened("05.03.2013 20:00", hours, false);

		testOpened("05.05.2013 20:00", hours, true);
		testOpened("05.05.2013 23:00", hours, false);

		testOpened("05.10.2013 15:00", hours, true);
		testOpened("05.10.2013 20:00", hours, false);

		// Test time with breaks
		hours = parseOpenedHours("Mo-Fr: 9:00-13:00, 14:00-18:00");
		System.out.println(hours);
		testOpened("02.12.2015 12:00", hours, true);
		testOpened("02.12.2015 13:30", hours, false);
		testOpened("02.12.2015 16:00", hours, true);

		testOpened("05.12.2015 16:00", hours, false);

		hours = parseOpenedHours("Mo-Su 07:00-23:00; Dec 25 08:00-20:00");
		System.out.println(hours);
		testOpened("25.12.2015 07:00", hours, false);
		testOpened("24.12.2015 07:00", hours, true);
		testOpened("24.12.2015 22:00", hours, true);
		testOpened("25.12.2015 08:00", hours, true);
		testOpened("25.12.2015 22:00", hours, false);

		hours = parseOpenedHours("Mo-Su 07:00-23:00; Dec 25 off");
		System.out.println(hours);
		testOpened("25.12.2015 14:00", hours, false);
		testOpened("24.12.2015 08:00", hours, true);

		// easter itself as public holiday is not supported
		hours = parseOpenedHours("Mo-Su 07:00-23:00; Easter off; Dec 25 off");
		System.out.println(hours);
		testOpened("25.12.2015 14:00", hours, false);
		testOpened("24.12.2015 08:00", hours, true);

		// test time off (not days
		hours = parseOpenedHours("Mo-Fr 08:30-17:00; 12:00-12:40 off;");
		System.out.println(hours);
		testOpened("07.05.2017 14:00", hours, false); // Sunday
		testOpened("06.05.2017 12:15", hours, false); // Saturday
		testOpened("05.05.2017 14:00", hours, true); // Friday
		testOpened("05.05.2017 12:15", hours, false);
		testOpened("05.05.2017 12:00", hours, false);
		testOpened("05.05.2017 11:45", hours, true);

		// Test holidays
		String hoursString = "mo-fr 11:00-21:00; PH off";
		hours = OpeningHoursParser.parseOpenedHoursHandleErrors(hoursString);
		testParsedAndAssembledCorrectly("mo-fr 11:00-21:00; PH off", hours);

		// test open from/till
		hours = parseOpenedHours("Mo-Fr 08:30-17:00; 12:00-12:40 off;");
		System.out.println(hours);
		testInfo("15.01.2018 09:00", hours, "Open till 12:00");
		testInfo("15.01.2018 11:00", hours, "Will close at 12:00");
		testInfo("15.01.2018 12:00", hours, "Will open at 12:40");

		hours = parseOpenedHours("Mo-Fr: 9:00-13:00, 14:00-18:00");
		System.out.println(hours);
		testInfo("15.01.2018 08:00", hours, "Will open at 09:00");
		testInfo("15.01.2018 09:00", hours, "Open till 13:00");
		testInfo("15.01.2018 12:00", hours, "Will close at 13:00");
		testInfo("15.01.2018 13:10", hours, "Will open at 14:00");
		testInfo("15.01.2018 14:00", hours, "Open till 18:00");
		testInfo("15.01.2018 16:00", hours, "Will close at 18:00");
		testInfo("15.01.2018 18:10", hours, "Will open tomorrow at 09:00");

		hours = parseOpenedHours("Mo-Sa 02:00-10:00; Th off");
		System.out.println(hours);
		testInfo("15.01.2018 23:00", hours, "Will open tomorrow at 02:00");

		hours = parseOpenedHours("Mo-Sa 23:00-02:00; Th off");
		System.out.println(hours);
		testInfo("15.01.2018 22:00", hours, "Will open at 23:00");
		testInfo("15.01.2018 23:00", hours, "Open till 02:00");
		testInfo("16.01.2018 00:30", hours, "Will close at 02:00");
		testInfo("16.01.2018 02:00", hours, "Open from 23:00");

		hours = parseOpenedHours("Mo-Sa 08:30-17:00; Th off");
		System.out.println(hours);
		testInfo("17.01.2018 20:00", hours, "Will open on 08:30 Fri.");
		testInfo("18.01.2018 05:00", hours, "Will open tomorrow at 08:30");
		testInfo("20.01.2018 05:00", hours, "Open from 08:30");
		testInfo("21.01.2018 05:00", hours, "Will open tomorrow at 08:30");
		testInfo("22.01.2018 02:00", hours, "Open from 08:30");
		testInfo("22.01.2018 04:00", hours, "Open from 08:30");
		testInfo("22.01.2018 07:00", hours, "Will open at 08:30");
		testInfo("23.01.2018 10:00", hours, "Open till 17:00");
		testInfo("23.01.2018 16:00", hours, "Will close at 17:00");

		hours = parseOpenedHours("24/7");
		System.out.println(hours);
		testInfo("24.01.2018 02:00", hours, "Open 24/7");

		hours = parseOpenedHours("Mo-Su 07:00-23:00, Fr 08:00-20:00");
		System.out.println(hours);
		testOpened("15.01.2018 06:45", hours, false);
		testOpened("15.01.2018 07:45", hours, true);
		testOpened("15.01.2018 23:45", hours, false);
		testOpened("19.01.2018 07:45", hours, false);
		testOpened("19.01.2018 08:45", hours, true);
		testOpened("19.01.2018 20:45", hours, false);

		// test fallback case
		hours = parseOpenedHours(
				"07:00-01:00 open \"Restaurant\" || Mo 00:00-04:00,07:00-04:00; Tu-Th 07:00-04:00; Fr 07:00-24:00; Sa,Su 00:00-24:00 open \"McDrive\"");
		System.out.println(hours);
		testOpened("22.01.2018 00:30", hours, true);
		testOpened("22.01.2018 08:00", hours, true);
		testOpened("22.01.2018 03:30", hours, true);
		testOpened("22.01.2018 05:00", hours, false);
		testOpened("23.01.2018 05:00", hours, false);
		testOpened("27.01.2018 05:00", hours, true);
		testOpened("28.01.2018 05:00", hours, true);

		testInfo("22.01.2018 05:00", hours, "Will open at 07:00 - Restaurant", 0);
		testInfo("26.01.2018 00:00", hours, "Will close at 01:00 - Restaurant", 0);
		testInfo("22.01.2018 05:00", hours, "Will open at 07:00 - McDrive", 1);
		testInfo("22.01.2018 00:00", hours, "Open till 04:00 - McDrive", 1);
		testInfo("22.01.2018 02:00", hours, "Will close at 04:00 - McDrive", 1);
		testInfo("27.01.2018 02:00", hours, "Open till 24:00 - McDrive", 1);

		hours = parseOpenedHours("07:00-03:00 open \"Restaurant\" || 24/7 open \"McDrive\"");
		System.out.println(hours);
		testOpened("22.01.2018 02:00", hours, true);
		testOpened("22.01.2018 17:00", hours, true);
		testInfo("22.01.2018 05:00", hours, "Will open at 07:00 - Restaurant", 0);
		testInfo("22.01.2018 04:00", hours, "McDrive", 1);

		hours = parseOpenedHours("Mo-Fr 12:00-15:00, Tu-Fr 17:00-23:00, Sa 12:00-23:00, Su 14:00-23:00");
		System.out.println(hours);
		testOpened("16.02.2018 14:00", hours, true);
		testOpened("16.02.2018 16:00", hours, false);
		testOpened("16.02.2018 17:00", hours, true);
		testInfo("16.02.2018 9:45", hours, "Open from 12:00");
		testInfo("16.02.2018 12:00", hours, "Open till 15:00");
		testInfo("16.02.2018 14:00", hours, "Will close at 15:00");
		testInfo("16.02.2018 16:00", hours, "Will open at 17:00");
		testInfo("16.02.2018 18:00", hours, "Open till 23:00");

		hours = parseOpenedHours("Mo-Fr 08:00-12:00, Mo,Tu,Th 15:00-17:00; PH off");
		System.out.println(hours);
		testOpened("09.08.2019 15:00", hours, false);
		testInfo("09.08.2019 15:00", hours, "Will open on 08:00 Mon.");

		hours = parseOpenedHours(
				"Mo-Fr 10:00-21:00; Sa 12:00-23:00; PH \"Wird auf der Homepage bekannt gegeben.\"");
		testParsedAndAssembledCorrectly(
				"Mo-Fr 10:00-21:00; Sa 12:00-23:00; PH - Wird auf der Homepage bekannt gegeben.", hours);
		System.out.println(hours);

		testAmPm();
	}

	private void testAmPm() throws ParseException {
		OpeningHoursParser.setTwelveHourFormattingEnabled(true, Locale.US);

		OpeningHours hours = parseOpenedHours("Mo-Fr: 9:00-13:00, 14:00-18:00");
		System.out.println(hours);
		testInfo("15.01.2018 08:00", hours, "Will open at 9:00 AM");
		testInfo("15.01.2018 09:00", hours, "Open till 1:00 PM");
		testInfo("15.01.2018 12:00", hours, "Will close at 1:00 PM");
		testInfo("15.01.2018 13:10", hours, "Will open at 2:00 PM");
		testInfo("15.01.2018 14:00", hours, "Open till 6:00 PM");
		testInfo("15.01.2018 16:00", hours, "Will close at 6:00 PM");
		testInfo("15.01.2018 18:10", hours, "Will open tomorrow at 9:00 AM");

		// Don't write AM or PM twice for range
		String string = "Mo-Fr 04:30-10:00, 07:30-23:00; Sa, Su, PH 13:30-23:00";
		hours = parseOpenedHours(string);
		testParsedAndAssembledCorrectly("Mo-Fr 4:30-10:00 AM, 7:30 AM-11:00 PM; Sa, Su, PH 1:30-11:00 PM", hours);

		string = "Mo-Fr 00:00-12:00, 12:00-24:00;";
		hours = parseOpenedHours(string);
		testParsedAndAssembledCorrectly("Mo-Fr 12:00 AM-12:00 PM, 12:00 PM-12:00 AM", hours);

		OpeningHoursParser.setTwelveHourFormattingEnabled(true, Locale.CHINESE);
		string = "Mo-Fr 04:30-10:00, 07:30-23:00; Sa, Su, PH 13:30-23:00";
		hours = parseOpenedHours(string);
		testParsedAndAssembledCorrectly("Mo-Fr 上午4:30-10:00, 上午7:30-下午11:00; Sa, Su, PH 下午1:30-11:00", hours);

		OpeningHoursParser.setTwelveHourFormattingEnabled(true, new Locale("ar"));
		string = "Mo-Fr 04:30-10:00, 07:30-23:00; Sa, Su, PH 13:30-23:00";
		hours = parseOpenedHours(string);
		testParsedAndAssembledCorrectly("Mo-Fr ٤:٣٠-١٠:٠٠ ص, ٧:٣٠ ص-١١:٠٠ م; Sa, Su, PH ١:٣٠-١١:٠٠ م", hours);
	}

	private static OpeningHours parseOpenedHours(String string) {
		return OpeningHoursParser.parseOpenedHours(string);
	}
}
