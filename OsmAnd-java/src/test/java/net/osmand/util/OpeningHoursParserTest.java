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
	 *                         "Open until HH:mm"     - close in 5 hours
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
	 *                         "Open until HH:mm"     - close in 5 hours
	 *                         "Will close at HH:mm" - close in 2 hours
	 *                         "Will open on HH:mm (Mo,Tu,We,Th,Fr,Sa,Su)" - open in >5 hours
	 *                         "Will open tomorrow at HH:mm" - open in >5 hours tomorrow
	 *                         "Open 24/7"           - open 24/7
	 * @param sequenceIndex sequence index of rules separated by ||
	 */
	private void testInfo(String time, OpeningHours hours, String expected, int sequenceIndex) throws ParseException {
		testInfo(time, hours, expected, sequenceIndex, false);
	}

	private void testShortInfo(String time, OpeningHours hours, String expected) throws ParseException {
		testInfo(time, hours, expected, OpeningHours.ALL_SEQUENCES, true);
	}

	private void testInfo(String time, OpeningHours hours, String expected, int sequenceIndex, boolean brief) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).parse(time));

		OpeningHours.Info info = sequenceIndex == OpeningHours.ALL_SEQUENCES
				? hours.getCombinedInfo(cal)
				: hours.getInfo(cal).get(sequenceIndex);
		String description = brief ? info.getShortInfo() : info.getInfo();
		boolean result = expected.equalsIgnoreCase(description.replace("\u202F", " "));

		String fmt = String.format("  %sok: Expected %s (%s): %s (rule %s)\n",
				(!result ? "NOT " : ""), time, expected, description, hours.getCurrentRuleTime(cal, sequenceIndex));
		System.out.println(fmt);
		Assert.assertTrue(fmt, result);
	}

	private void testParsedAndAssembledCorrectly(String expected, OpeningHours hours) {
		String assembledString = hours.toString().replace("\u202F", " ");
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

		hours = parseOpenedHours("2024 Jan 1-Dec 31");
		System.out.println(hours);
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("01.01.2025 00:00", hours, false);

		hours = parseOpenedHours("2024 Jan 01-Dec 31");
		System.out.println(hours);
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("01.01.2025 00:00", hours, false);

		hours = parseOpenedHours("2024 Jan 01-2024 Dec 31");
		System.out.println(hours);
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("01.01.2025 00:00", hours, false);

		hours = parseOpenedHours("2024 Jan 01-2025 Dec 31");
		System.out.println(hours);
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("01.01.2025 00:00", hours, true);
		testOpened("31.12.2025 23:59", hours, true);
		testOpened("01.01.2026 00:00", hours, false);

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
		testInfo("03.10.2020 14:00", hours, "Open until 18:30");
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

		// test open from/until
		hours = parseOpenedHours("Mo-Fr 08:30-17:00; 12:00-12:40 off;");
		System.out.println(hours);
		testInfo("15.01.2018 09:00", hours, "Open until 12:00");
		testInfo("15.01.2018 11:00", hours, "Will close at 12:00");
		testInfo("15.01.2018 12:00", hours, "Will open at 12:40");

		hours = parseOpenedHours("Mo-Fr: 9:00-13:00, 14:00-18:00");
		System.out.println(hours);
		testInfo("15.01.2018 08:00", hours, "Will open at 09:00");
		testInfo("15.01.2018 09:00", hours, "Open until 13:00");
		testInfo("15.01.2018 12:00", hours, "Will close at 13:00");
		testInfo("15.01.2018 13:10", hours, "Will open at 14:00");
		testInfo("15.01.2018 14:00", hours, "Open until 18:00");
		testInfo("15.01.2018 16:00", hours, "Will close at 18:00");
		testInfo("15.01.2018 18:10", hours, "Will open tomorrow at 09:00");

		hours = parseOpenedHours("Mo-Sa 02:00-10:00; Th off");
		System.out.println(hours);
		testInfo("15.01.2018 23:00", hours, "Will open tomorrow at 02:00");

		hours = parseOpenedHours("Mo-Sa 23:00-02:00; Th off");
		System.out.println(hours);
		testInfo("15.01.2018 22:00", hours, "Will open at 23:00");
		testInfo("15.01.2018 23:00", hours, "Open until 02:00");
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
		testInfo("23.01.2018 10:00", hours, "Open until 17:00");
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
		testInfo("22.01.2018 00:00", hours, "Open until 04:00 - McDrive", 1);
		testInfo("22.01.2018 02:00", hours, "Will close at 04:00 - McDrive", 1);
		testInfo("27.01.2018 02:00", hours, "Open until 24:00 - McDrive", 1);

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
		testInfo("16.02.2018 12:00", hours, "Open until 15:00");
		testInfo("16.02.2018 14:00", hours, "Will close at 15:00");
		testInfo("16.02.2018 16:00", hours, "Will open at 17:00");
		testInfo("16.02.2018 18:00", hours, "Open until 23:00");

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
		testComma();
		testYearFormats();
		testGetShortInfo();
		testTimeRestrictedOffRules();
		testMonthRuleOverride();
		testHolidayWithWeekday();
		testNthWeekdayOfMonth();
		testOvernightNextOpening();
		testRealWorldSchedules();
	}

	private void testOvernightNextOpening() throws ParseException {
		// overnight rules of other days must not report an opening time for today,
		// and a still running overnight session determines the closing time
		OpeningHoursParser.initLocalStrings(Locale.UK);
		OpeningHoursParser.setTwelveHourFormattingEnabled(false, Locale.UK);
		OpeningHours hours = parseOpenedHours("Mo-Th,Su 09:00-00:30; Fr 09:00-16:30");
		System.out.println(hours);
		// Friday evening: Saturday is closed, so the next opening is on Sunday
		// (was "Open from 09:00", which means "opens today")
		testOpened("06.02.2026 21:00", hours, false);
		testInfo("06.02.2026 21:00", hours, "Will open on 09:00 Sun.");
		testInfo("06.02.2026 19:00", hours, "Will open on 09:00 Sun.");
		// Friday 00:15 is inside the Thursday session which ends 00:30
		// (was "Open until 16:30" from the Friday rule)
		testOpened("06.02.2026 00:15", hours, true);
		testInfo("06.02.2026 00:15", hours, "Will close at 00:30");
		// unchanged behavior around it
		testInfo("06.02.2026 12:00", hours, "Open until 16:30");
		testInfo("06.02.2026 15:00", hours, "Will close at 16:30");
		testOpened("07.02.2026 12:00", hours, false);
		testInfo("07.02.2026 12:00", hours, "Will open tomorrow at 09:00");
		// Sunday evening: the overnight session closes at 00:30 the next day, so the
		// "closing soon" warning must also trigger before midnight (was "Open until 00:30")
		testOpened("08.02.2026 23:00", hours, true);
		testInfo("08.02.2026 22:00", hours, "Open until 00:30"); // 2.5 h to closing
		testInfo("08.02.2026 23:00", hours, "Will close at 00:30"); // 1.5 h to closing
		testInfo("08.02.2026 23:50", hours, "Will close at 00:30"); // 40 min to closing
		testInfo("09.02.2026 07:00", hours, "Will open at 09:00");
	}

	// Real-world schedules (the kind actually tagged on OSM shops, bars, markets, museums)
	// exercising the fixes of this PR end to end: time-restricted "off", seasonal month
	// overrides, nth weekday of month and overnight next-open/close.
	private void testRealWorldSchedules() throws ParseException {
		OpeningHoursParser.initLocalStrings(Locale.UK);
		OpeningHoursParser.setTwelveHourFormattingEnabled(false, Locale.UK);

		// weekend-only nightclub, overnight into the next morning; next opening after the
		// last overnight day (Sat) skips the whole week to the following Friday
		OpeningHours hours = parseOpenedHours("Fr,Sa 20:00-04:00");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("Fr, Sa 20:00-04:00", hours);
		testInfo("03.01.2025 19:00", hours, "Will open at 20:00");     // Fri, opens in 1 h
		testInfo("03.01.2025 23:30", hours, "Open until 04:00");       // Fri night, closes 04:00
		testInfo("04.01.2025 02:00", hours, "Will close at 04:00");    // Sat 02:00, Fri session
		testInfo("05.01.2025 01:00", hours, "Open until 04:00");       // Sun 01:00, Sat session
		testOpened("05.01.2025 05:00", hours, false);
		testInfo("05.01.2025 05:00", hours, "Will open on 20:00 Fri."); // closed until next Fri
		testInfo("06.01.2025 12:00", hours, "Will open on 20:00 Fri.");

		// neighbourhood bar, mix of overnight and non-overnight days; the "closing soon"
		// warning must trigger before midnight for the overnight days too
		hours = parseOpenedHours("We-Th 18:00-01:00; Fr-Sa 18:00-03:00; Su 16:00-23:00");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("We, Th 18:00-01:00; Fr, Sa 18:00-03:00; Su 16:00-23:00", hours);
		testInfo("04.06.2025 23:30", hours, "Will close at 01:00");    // Wed 23:30 -> 90 min to close
		testInfo("05.06.2025 00:30", hours, "Will close at 01:00");    // Thu 00:30, Wed session
		testInfo("07.06.2025 02:00", hours, "Will close at 03:00");    // Sat 02:00, Fri session
		testInfo("08.06.2025 22:00", hours, "Will close at 23:00");    // Sun evening
		testOpened("08.06.2025 03:00", hours, false);
		testInfo("08.06.2025 03:00", hours, "Open from 16:00");        // Sun early morning, opens 16:00
		testInfo("10.06.2025 20:00", hours, "Will open tomorrow at 18:00"); // Tue closed

		// ice-cream parlour with a reduced winter schedule that wraps the year end (Dec-Feb);
		// the winter rule must win inside the summer time window too (#23457 family)
		hours = parseOpenedHours("Mo-Su 12:00-22:00; Dec-Feb Mo-Su 13:00-18:00");
		System.out.println(hours);
		testInfo("15.01.2026 14:00", hours, "Open until 18:00");       // winter override
		testOpened("07.02.2026 12:30", hours, false);
		testInfo("07.02.2026 12:30", hours, "Will open at 13:00");     // 12:30 winter-closed
		testInfo("10.12.2025 20:00", hours, "Will open tomorrow at 13:00");
		testInfo("20.06.2025 21:00", hours, "Will close at 22:00");    // summer
		testInfo("30.11.2025 19:00", hours, "Open until 22:00");       // Nov still summer

		// museum with a Monday closing day and a wrap-around winter season (Nov-Mar)
		hours = parseOpenedHours("Tu-Su 10:00-18:00; Nov-Mar Tu-Su 10:00-16:00");
		System.out.println(hours);
		testInfo("14.02.2026 15:00", hours, "Will close at 16:00");    // winter
		testInfo("15.05.2025 17:00", hours, "Will close at 18:00");    // summer
		testOpened("08.06.2025 19:00", hours, false);
		testInfo("08.06.2025 19:00", hours, "Will open on 10:00 Tue."); // Sun evening, Mon closed
		testInfo("20.01.2026 17:00", hours, "Will open tomorrow at 10:00");
		testInfo("20.12.2025 07:30", hours, "Open from 10:00");

		// pharmacy with a lunch closure; a passed lunch break must not shorten the afternoon
		// closing time (#22931) and the reopening is the end of the "off" range
		hours = parseOpenedHours("Mo-Fr 08:30-18:30; Sa 09:00-13:00; Mo-Fr 13:00-14:00 off");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("Mo-Fr 08:30-18:30; Sa 09:00-13:00; Mo-Fr 13:00-14:00 off", hours);
		testInfo("02.06.2025 10:30", hours, "Open until 13:00");       // closes for lunch
		testOpened("02.06.2025 13:20", hours, false);
		testInfo("02.06.2025 13:20", hours, "Will open at 14:00");     // lunch break
		testInfo("02.06.2025 15:00", hours, "Open until 18:30");       // afternoon, full closing time
		testInfo("02.06.2025 17:00", hours, "Will close at 18:30");
		testInfo("07.06.2025 11:00", hours, "Will close at 13:00");    // Saturday
		testInfo("08.06.2025 12:00", hours, "Will open tomorrow at 08:30");

		// weekly farmers market plus a "first Sunday of the month" special during the season
		hours = parseOpenedHours("We,Sa 07:00-13:00; Apr-Oct Su[1] 08:00-16:00");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("We, Sa 07:00-13:00; Apr-Oct Su[1] 08:00-16:00", hours);
		testOpened("06.07.2025 07:30", hours, false);
		testInfo("06.07.2025 07:30", hours, "Will open at 08:00");     // 1st Sunday of July
		testInfo("06.07.2025 09:00", hours, "Open until 16:00");
		testInfo("06.07.2025 15:00", hours, "Will close at 16:00");
		testOpened("13.07.2025 10:00", hours, false);                 // 2nd Sunday, no market
		testInfo("13.07.2025 10:00", hours, "Will open on 07:00 Wed.");
		testInfo("04.10.2025 12:00", hours, "Will close at 13:00");    // Saturday market
		testOpened("07.01.2025 08:00", hours, false);                 // January, out of season

		// rural church sharing a priest: mass on 1st/3rd/5th Sundays in the morning,
		// on 2nd/4th Sundays in the evening; the tricky 5th-Sunday occurrence must count
		hours = parseOpenedHours("Su[1,3,5] 09:00-10:00; Su[2,4] 18:00-19:00");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("Su[1,3,5] 09:00-10:00; Su[2,4] 18:00-19:00", hours);
		testOpened("03.08.2025 09:30", hours, true);                  // 1st Sunday morning
		testInfo("03.08.2025 09:30", hours, "Will close at 10:00");
		testOpened("10.08.2025 09:30", hours, false);                 // 2nd Sunday, no morning mass
		testInfo("10.08.2025 09:30", hours, "Open from 18:00");
		testInfo("10.08.2025 18:30", hours, "Will close at 19:00");   // 2nd Sunday evening
		testOpened("31.08.2025 09:45", hours, true);                  // 5th Sunday counts
		testInfo("31.08.2025 09:45", hours, "Will close at 10:00");
		testInfo("06.08.2025 12:00", hours, "Will open on 18:00 Sun."); // next is 2nd Sunday

		// bakery open every day including Sunday morning, closed on public holidays; the
		// trailing "PH off" must not disturb the regular Sunday hours
		hours = parseOpenedHours("Mo-Fr 06:00-18:30; Sa 06:30-13:00; Su 07:30-11:00; PH off");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("Mo-Fr 06:00-18:30; Sa 06:30-13:00; Su 07:30-11:00; PH off", hours);
		testInfo("12.10.2025 08:00", hours, "Open until 11:00");       // Sunday morning
		testInfo("12.10.2025 09:30", hours, "Will close at 11:00");
		testOpened("12.10.2025 12:00", hours, false);
		testInfo("12.10.2025 12:00", hours, "Will open tomorrow at 06:00");
		testInfo("11.10.2025 13:30", hours, "Will open tomorrow at 07:30"); // Sat -> Sun
		testInfo("10.10.2025 18:00", hours, "Will close at 18:30");    // Friday
		testInfo("06.10.2025 03:00", hours, "Open from 06:00");

		// self-service car wash with a reduced-noise winter evening off window; the seasonal
		// "off" must only shorten its own window and only in its months
		hours = parseOpenedHours("Mo-Sa 07:00-21:00; Nov-Feb 19:00-21:00 off");
		System.out.println(hours);
		testInfo("15.01.2025 18:30", hours, "Will close at 19:00");    // winter, off shortens evening
		testInfo("16.07.2025 18:30", hours, "Open until 21:00");       // summer, no off
		testInfo("16.07.2025 19:30", hours, "Will close at 21:00");
		testInfo("16.07.2025 04:30", hours, "Open from 07:00");
		testInfo("15.01.2025 06:00", hours, "Will open at 07:00");
	}

	private void testHolidayWithWeekday() throws ParseException {
		// "PH Su" means "public holidays falling on Sunday" and must not fill
		// the weekday range Mo-Su (#23990)
		OpeningHoursParser.initLocalStrings(Locale.UK);
		OpeningHoursParser.setTwelveHourFormattingEnabled(false, Locale.UK);
		OpeningHours hours = parseOpenedHours("Tu-Sa,PH 10:00-12:00,14:00-19:00; PH Su off");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("Tu-Sa, PH 10:00-12:00, 14:00-19:00; PH off", hours);
		testOpened("07.10.2025 11:00", hours, true);  // regular Tuesday must stay open
		testOpened("05.10.2025 11:00", hours, false); // regular Sunday
		testOpened("06.10.2025 11:00", hours, false); // Monday
		testInfo("07.10.2025 11:00", hours, "Will close at 12:00");

		// without holiday info the rule can not be applied to regular weekdays
		hours = parseOpenedHours("PH Su 08:30-12:30");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("PH 08:30-12:30", hours);
		testOpened("06.10.2025 09:00", hours, false); // Monday
		testOpened("05.10.2025 09:00", hours, false); // regular Sunday

		hours = parseOpenedHours("SH Mo-Fr 10:00-14:00");
		System.out.println(hours);
		testOpened("06.10.2025 11:00", hours, false); // regular Monday
	}

	private void testNthWeekdayOfMonth() throws ParseException {
		// nth weekday of the month like "Su[1]", "Su[-1]" or "Su[1,3]" (#23990)
		OpeningHoursParser.initLocalStrings(Locale.UK);
		OpeningHoursParser.setTwelveHourFormattingEnabled(false, Locale.UK);
		OpeningHours hours = parseOpenedHours("Jul Su[1] 08:00-18:00");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("Jul Su[1] 08:00-18:00", hours);
		testOpened("06.07.2025 10:00", hours, true);  // 1st Sunday of July
		testOpened("13.07.2025 10:00", hours, false); // 2nd Sunday
		testOpened("07.07.2025 10:00", hours, false); // Monday
		testOpened("01.06.2025 10:00", hours, false); // Sunday outside July

		hours = parseOpenedHours("Nov Su[-1] 08:00-18:00");
		System.out.println(hours);
		testParsedAndAssembledCorrectly("Nov Su[-1] 08:00-18:00", hours);
		testOpened("30.11.2025 10:00", hours, true);  // last Sunday of November
		testOpened("23.11.2025 10:00", hours, false); // 4th but not last Sunday

		hours = parseOpenedHours("Su[1,3] 08:00-12:00");
		System.out.println(hours);
		testOpened("05.10.2025 09:00", hours, true);  // 1st Sunday
		testOpened("12.10.2025 09:00", hours, false); // 2nd Sunday
		testOpened("19.10.2025 09:00", hours, true);  // 3rd Sunday

		// full rule from #23990
		hours = parseOpenedHours("Tu-Sa,PH 10:00-12:00,14:00-19:00; PH Su off; May 01,Dec 25 off; Jul Su[1] 08:00-18:00; Nov Su[4] 08:00-18:00");
		System.out.println(hours);
		testOpened("07.10.2025 11:00", hours, true);  // regular Tuesday
		testOpened("01.05.2025 11:00", hours, false); // May 1st
		testOpened("06.07.2025 09:00", hours, true);  // 1st Sunday of July
		testOpened("23.11.2025 09:00", hours, true);  // 4th Sunday of November
		testOpened("05.10.2025 11:00", hours, false); // regular Sunday

		// library from #7857, the "Sa[1,3]" rule was parsed as "24/7" before
		hours = parseOpenedHours("Jul-Aug Mo,Tu 13:00-19:00; Jul-Aug We-Fr 08:00-14:00; Jul-Aug Sa off; "
				+ "Jan-Jun,Sep-Dec Mo,Tu 13:00-19:00; Jan-Jun,Sep-Dec We-Fr 08:00-16:00; Jan-Jun,Sep-Dec Sa[1,3] 09:00-13:00; PH off");
		System.out.println(hours);
		testOpened("05.11.2019 05:00", hours, false); // issue scenario: Tuesday 5 AM, was "open 24/7"
		testOpened("05.11.2019 14:00", hours, true);
		testOpened("01.11.2025 10:00", hours, true);  // 1st Saturday of November
		testOpened("08.11.2025 10:00", hours, false); // 2nd Saturday
		testOpened("15.11.2025 10:00", hours, true);  // 3rd Saturday
		testOpened("05.07.2025 10:00", hours, false); // Saturday in July is off
		testOpened("09.07.2025 09:00", hours, true);  // Wednesday in July
	}

	private void testTimeRestrictedOffRules() throws ParseException {
		// "off" rules with time ranges must only turn off their own time windows
		// and must not discard opening/closing times found by other rules (#22907)
		OpeningHoursParser.initLocalStrings(Locale.UK);
		OpeningHoursParser.setTwelveHourFormattingEnabled(false, Locale.UK);
		OpeningHours hours = parseOpenedHours("Mo-Fr 08:30-12:30,14:00-19:30; Sa 09:00-12:30; Jul-Aug 19:00-19:30 off; PH off");
		System.out.println(hours);
		// Wednesday inside the Jul-Aug period
		testOpened("02.07.2025 13:50", hours, false);
		testInfo("02.07.2025 13:50", hours, "Will open at 14:00");
		testInfo("02.07.2025 05:00", hours, "Open from 08:30");
		testInfo("02.07.2025 10:00", hours, "Open until 12:30");
		testInfo("02.07.2025 12:20", hours, "Will close at 12:30");
		// the "off" range shortens the evening interval
		testOpened("02.07.2025 14:05", hours, true);
		testInfo("02.07.2025 14:05", hours, "Open until 19:00");
		testOpened("02.07.2025 19:10", hours, false);
		testInfo("02.07.2025 21:00", hours, "Will open tomorrow at 08:30");
		// outside the Jul-Aug period the "off" rule has no effect
		testInfo("03.09.2025 13:50", hours, "Will open at 14:00");
		testInfo("03.09.2025 14:05", hours, "Open until 19:30");

		// lunch break: reopening time is the end of the "off" range
		hours = parseOpenedHours("Mo-Fr 08:00-18:00; Mo-Fr 12:00-13:00 off");
		System.out.println(hours);
		testOpened("06.10.2025 12:30", hours, false);
		testInfo("06.10.2025 12:30", hours, "Will open at 13:00");
		testInfo("06.10.2025 10:30", hours, "Will close at 12:00");
		testInfo("06.10.2025 14:00", hours, "Open until 18:00");

		// a passed "off" range must not affect the closing time anymore (#22931)
		hours = parseOpenedHours("Tu-Fr 08:00-17:00; Mo-Fr 12:00-13:00 off \"Lunch\"");
		System.out.println(hours);
		testInfo("07.10.2025 09:00", hours, "Open until 12:00 - Lunch");
		testInfo("07.10.2025 12:30", hours, "Will open at 13:00 - Lunch");
		testInfo("07.10.2025 15:00", hours, "Will close at 17:00");

		// multiple "off" time ranges in one rule
		hours = parseOpenedHours("Mo-Fr 08:00-20:00; Mo-Fr 10:00-10:30,15:00-15:30 off");
		System.out.println(hours);
		testInfo("06.10.2025 09:00", hours, "Will close at 10:00");
		testInfo("06.10.2025 10:15", hours, "Will open at 10:30");
		testInfo("06.10.2025 12:00", hours, "Open until 15:00");
		testInfo("06.10.2025 16:00", hours, "Open until 20:00");

		// whole-day "off" rules by year/day-month ranges must discard the opening time of that day (#21780)
		hours = parseOpenedHours("Mo-Fr 09:00-20:00; Sa 09:00-18:00; 2025 Jan 07 - 2025 Feb 26 closed");
		System.out.println(hours);
		testOpened("23.01.2025 07:40", hours, false);
		testInfo("23.01.2025 07:40", hours, "2025 Jan 7-2025 Feb 26 off");
		testOpened("23.01.2025 12:00", hours, false);
		testInfo("27.02.2025 09:30", hours, "Open until 20:00");
		testInfo("06.01.2025 12:00", hours, "Open until 20:00");
	}

	private void testMonthRuleOverride() throws ParseException {
		// later month rules override the default rule also inside the default time window (#23457)
		OpeningHoursParser.initLocalStrings(Locale.UK);
		OpeningHoursParser.setTwelveHourFormattingEnabled(false, Locale.UK);
		OpeningHours hours = parseOpenedHours("07:00-17:00; Mar 07:00-19:00; Apr 07:00-21:00; May-Aug 07:00-22:00; Sep 07:00-21:00; Oct 07:00-19:00");
		System.out.println(hours);
		testOpened("12.09.2025 14:09", hours, true);
		testInfo("12.09.2025 14:09", hours, "Open until 21:00");
		testInfo("12.09.2025 18:00", hours, "Open until 21:00");
		testInfo("12.09.2025 20:00", hours, "Will close at 21:00");
		testOpened("12.09.2025 21:30", hours, false);
		testInfo("12.09.2025 21:30", hours, "Will open tomorrow at 07:00");
		testInfo("12.01.2025 14:09", hours, "Open until 17:00");
		testInfo("12.06.2025 21:30", hours, "Will close at 22:00");
		testInfo("12.03.2025 18:30", hours, "Will close at 19:00");
	}

	private void testGetShortInfo() throws ParseException {
		OpeningHoursParser.initLocalStrings(Locale.UK);
		OpeningHoursParser.setTwelveHourFormattingEnabled(false, Locale.UK);
		OpeningHours hours = parseOpenedHours("24/7");
		testShortInfo("16.02.2018 12:00", hours, "24/7");

		hours = parseOpenedHours("Mo-Fr 12:00-15:00, Tu-Fr 17:00-23:00, Sa 12:00-23:00, Su 14:00-23:00");
		testShortInfo("16.02.2018 09:45", hours, "From 12:00");
		testShortInfo("16.02.2018 12:00", hours, "Until 15:00");
		testShortInfo("16.02.2018 14:00", hours, "Until 15:00");
		testShortInfo("16.02.2018 16:00", hours, "From 17:00");

		hours = parseOpenedHours("Mo-Fr 09:00-18:00");
		testShortInfo("18.02.2018 12:00", hours, "Tomorrow 09:00");

		hours = parseOpenedHours("Mo-Fr 08:00-12:00, Mo,Tu,Th 15:00-17:00; PH off");
		testShortInfo("09.08.2019 15:00", hours, "From 08:00 Mon");

		hours = parseOpenedHours("Mo-Fr; PH off");
		testShortInfo("09.08.2019 15:00", hours, "Mon-Fri");
	}

	private void testYearFormats() throws ParseException {
		OpeningHours hours = parseOpenedHours("2024 Jan-Dec");
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("01.01.2025 00:00", hours, false);

		hours = parseOpenedHours("2024-2025 Jan 1-Dec 31");
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("31.12.2025 23:59", hours, true);
		testOpened("01.01.2026 00:00", hours, false);

		hours = parseOpenedHours("2024,2025 Jan 1-Dec 31");
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("31.12.2025 23:59", hours, true);
		testOpened("01.01.2026 00:00", hours, false);

		hours = parseOpenedHours("2024");
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("01.01.2025 00:00", hours, false);

		hours = parseOpenedHours("2024,2026");
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("15.06.2025 12:00", hours, false);
		testOpened("01.01.2026 00:00", hours, true);
		testOpened("31.12.2026 23:59", hours, true);
		testOpened("01.01.2027 00:00", hours, false);

		hours = parseOpenedHours("2024,2026 Jan 1-Dec 31");
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2024 23:59", hours, true);
		testOpened("15.06.2025 12:00", hours, false);
		testOpened("01.01.2026 00:00", hours, true);
		testOpened("31.12.2026 23:59", hours, true);
		testOpened("01.01.2027 00:00", hours, false);

		hours = parseOpenedHours("2024,2026-2027 Jan 1-Dec 31");
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("15.06.2025 12:00", hours, false);
		testOpened("01.01.2026 00:00", hours, true);
		testOpened("31.12.2027 23:59", hours, true);
		testOpened("01.01.2028 00:00", hours, false);

		hours = parseOpenedHours("2024-2025,2027-2028 Jan 1-Dec 31");
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("31.12.2025 23:59", hours, true);
		testOpened("15.06.2026 12:00", hours, false);
		testOpened("01.01.2027 00:00", hours, true);
		testOpened("31.12.2028 23:59", hours, true);
		testOpened("01.01.2029 00:00", hours, false);

		hours = parseOpenedHours("2024,2026,2028 Jan 1-Dec 31");
		testOpened("31.12.2023 23:59", hours, false);
		testOpened("01.01.2024 00:00", hours, true);
		testOpened("15.06.2025 12:00", hours, false);
		testOpened("01.01.2026 00:00", hours, true);
		testOpened("15.06.2027 12:00", hours, false);
		testOpened("01.01.2028 00:00", hours, true);
		testOpened("01.01.2029 00:00", hours, false);
	}
	
	private void testComma() throws ParseException {
		OpeningHoursParser.setTwelveHourFormattingEnabled(true, Locale.US);

		OpeningHours hours = parseOpenedHours("Mo-Fr 09:00-13:00,Tu 14:00-18:00, Th 14:00-17:00; We \"Nach Vereinbarung\"; Sa,Su,PH closed");
		System.out.println(hours);
		testOpened("24.03.2025 10:00", hours, true); // Mo
		testOpened("24.03.2025 13:30", hours, false);
		testOpened("24.03.2025 17:50", hours, false);
		testOpened("25.03.2025 10:00", hours, true); // Tu
		testOpened("25.03.2025 13:30", hours, false);
		testOpened("25.03.2025 17:50", hours, true);
		testInfo("24.03.2025 16:00", hours, "Will open tomorrow at 9:00 AM"); // Mo
		testInfo("25.03.2025 10:00", hours, "Open until 1:00 PM"); // Tu
		testInfo("25.03.2025 13:30", hours, "Will open at 2:00 PM");
		testInfo("25.03.2025 17:50", hours, "Will close at 6:00 PM");
		testInfo("25.03.2025 18:50", hours, "Will open on 9:00 AM Thu."); // not ok
	}


	private void testAmPm() throws ParseException {
		OpeningHoursParser.setTwelveHourFormattingEnabled(true, Locale.US);

		OpeningHours hours = parseOpenedHours("Mo-Fr: 9:00-13:00, 14:00-18:00");
		System.out.println(hours);
		testInfo("15.01.2018 08:00", hours, "Will open at 9:00 AM");
		testInfo("15.01.2018 09:00", hours, "Open until 1:00 PM");
		testInfo("15.01.2018 12:00", hours, "Will close at 1:00 PM");
		testInfo("15.01.2018 13:10", hours, "Will open at 2:00 PM");
		testInfo("15.01.2018 14:00", hours, "Open until 6:00 PM");
		testInfo("15.01.2018 16:00", hours, "Will close at 6:00 PM");
		testInfo("15.01.2018 18:10", hours, "Will open tomorrow at 9:00 AM");

		// Don't write AM or PM twice for range
		String string = "Mo-Fr 04:30-10:00, 07:30-23:00; Sa, Su, PH 13:30-23:00";
		hours = parseOpenedHours(string);
		testParsedAndAssembledCorrectly("Mo-Fr 4:30-10:00 AM, 7:30 AM-11:00 PM; Sa, Su, PH 1:30-11:00 PM", hours);

		string = "Mo-Fr 00:00-12:00, 12:00-24:00;";
		hours = parseOpenedHours(string);
		testParsedAndAssembledCorrectly("Mo-Fr 12:00 AM-12:00 PM, 12:00 PM-12:00 AM", hours);

		OpeningHoursParser.setTwelveHourFormattingEnabled(true, Locale.TRADITIONAL_CHINESE);
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
