package net.osmand.router.gtfs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.Test;

public class GtfsCalendarTest {

	private GtfsCsv csv(String text) throws IOException {
		return new GtfsCsv(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	private GtfsCalendar build(String calendar, String dates) throws IOException {
		GtfsCalendar.Builder b = new GtfsCalendar.Builder();
		b.startFeed();
		if (calendar != null) {
			b.readCalendar(csv(calendar));
		}
		if (dates != null) {
			b.readCalendarDates(csv(dates));
		}
		return b.build(GtfsFixture.NY);
	}

	private static final String HEADER = "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\n";

	@Test
	public void weekdayPatternAndDateRange() throws IOException {
		GtfsCalendar cal = build(HEADER + "WD,1,1,1,1,1,0,0,20261101,20261130\nSU,0,0,0,0,0,0,1,20261101,20261130\n", null);
		assertTrue(cal.activeServices(GtfsFixture.TUE)[0]);
		assertFalse(cal.activeServices(GtfsFixture.TUE)[1]);
		assertTrue(cal.activeServices(GtfsFixture.SUN)[1]);
		assertFalse(cal.activeServices(GtfsFixture.SAT)[0]);
		assertFalse(cal.activeServices(LocalDate.of(2026, 12, 1))[0]); // after the end date
		assertFalse(cal.activeServices(LocalDate.of(2026, 10, 30))[0]); // before the start date
	}

	@Test
	public void exceptionsAddAndRemoveAndLaterRowWins() throws IOException {
		GtfsCalendar cal = build(HEADER + "WD,1,1,1,1,1,0,0,20261101,20261130\n",
				"service_id,date,exception_type\nWD,20261126,2\nWD,20261121,1\nWD,20261122,1\nWD,20261122,2\nXTRA,20261123,1\n");
		assertFalse(cal.activeServices(LocalDate.of(2026, 11, 26))[0]); // Thanksgiving removed
		assertTrue(cal.activeServices(GtfsFixture.SAT)[0]); // Saturday added
		assertFalse(cal.activeServices(GtfsFixture.SUN)[0]);
		assertEquals(2, cal.getServiceCount()); // XTRA exists only in calendar_dates
		assertTrue(cal.activeServices(LocalDate.of(2026, 11, 23))[1]);
		assertFalse(cal.activeServices(LocalDate.of(2026, 11, 24))[1]);
	}

	@Test
	public void calendarDatesAloneAreEnough() throws IOException {
		GtfsCalendar.Builder b = new GtfsCalendar.Builder();
		b.startFeed();
		b.readCalendarDates(csv("service_id,date,exception_type\nS,20261117,1\nS,20261118,1\nS,20261119,2\n"));
		assertEquals(LocalDate.of(2026, 11, 17).toEpochDay(), b.getValidFrom());
		assertEquals(LocalDate.of(2026, 11, 18).toEpochDay(), b.getValidTo());
		GtfsCalendar cal = b.build(GtfsFixture.NY);
		assertTrue(cal.activeServices(LocalDate.of(2026, 11, 17))[0]);
		assertFalse(cal.activeServices(LocalDate.of(2026, 11, 19))[0]);
		assertFalse(cal.activeServices(LocalDate.of(2026, 11, 20))[0]);
	}

	@Test
	public void serviceIdsAreScopedPerFeed() throws IOException {
		GtfsCalendar.Builder b = new GtfsCalendar.Builder();
		b.startFeed();
		b.readCalendar(csv(HEADER + "WD,1,1,1,1,1,0,0,20261101,20261130\n"));
		assertEquals(0, b.serviceIndex("WD"));
		b.startFeed();
		assertEquals(-1, b.serviceIndex("WD"));
		b.readCalendar(csv(HEADER + "WD,0,0,0,0,0,0,1,20261101,20261130\n"));
		assertEquals(1, b.serviceIndex("WD"));
	}

	@Test
	public void serviceDayStartsAtNoonMinusTwelveHours() throws IOException {
		GtfsCalendar cal = build(HEADER + "WD,1,1,1,1,1,0,0,20260101,20271231\n", null);
		// ordinary day: midnight local (EST = UTC-5)
		assertEquals(Instant.parse("2026-12-01T05:00:00Z").getEpochSecond(), cal.serviceDayStart(LocalDate.of(2026, 12, 1)));
		// fall back (25 hours): service time 0 is 01:00 EDT = 05:00Z
		assertEquals(Instant.parse("2026-11-01T05:00:00Z").getEpochSecond(), cal.serviceDayStart(LocalDate.of(2026, 11, 1)));
		// spring forward (23 hours): service time 0 is 23:00 EST of the day before = 04:00Z
		assertEquals(Instant.parse("2027-03-14T04:00:00Z").getEpochSecond(), cal.serviceDayStart(LocalDate.of(2027, 3, 14)));
	}

	@Test
	public void cursorOffsetsFollowTheRealDayLength() throws IOException {
		GtfsCalendar cal = build(HEADER + "WD,1,1,1,1,1,0,0,20260101,20271231\n", null);
		assertArrayEquals(new int[] {-86400, 0, 86400}, cal.cursorOffsets(LocalDate.of(2026, 12, 1)));
		// the service day of Oct 31 runs from 00:00 EDT to 01:00 EDT after the clocks went back: 25 h
		assertArrayEquals(new int[] {-90000, 0, 86400}, cal.cursorOffsets(LocalDate.of(2026, 11, 1)));
		assertArrayEquals(new int[] {-86400, 0, 86400}, cal.cursorOffsets(LocalDate.of(2026, 11, 2)));
		assertArrayEquals(new int[] {-86400, 0, 82800}, cal.cursorOffsets(LocalDate.of(2027, 3, 13)));
		assertArrayEquals(new int[] {-82800, 0, 86400}, cal.cursorOffsets(LocalDate.of(2027, 3, 14)));
	}

	@Test
	public void localDateUsesTheAgencyZone() throws IOException {
		GtfsCalendar cal = build(HEADER + "WD,1,1,1,1,1,0,0,20260101,20271231\n", null);
		long utc = Instant.parse("2026-11-18T03:30:00Z").getEpochSecond(); // still 17 Nov in New York
		assertEquals(LocalDate.of(2026, 11, 17), cal.localDate(utc));
	}
}
