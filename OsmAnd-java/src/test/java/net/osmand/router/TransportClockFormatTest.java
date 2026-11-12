package net.osmand.router;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

public class TransportClockFormatTest {

	private static final ZoneId NY = ZoneId.of("America/New_York");
	private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

	private static long epoch(ZoneId zone, int h, int m, int s) {
		return LocalDateTime.of(2026, 11, 17, h, m, s).atZone(zone).toEpochSecond();
	}

	private static TimedTransportRoute timed(long dep, long arr, ZoneId zone) {
		return new TimedTransportRoute() {
			@Override
			public long getDepartureTimeSec() {
				return dep;
			}

			@Override
			public long getArrivalTimeSec() {
				return arr;
			}

			@Override
			public ZoneId getTimeZone() {
				return zone;
			}
		};
	}

	@Test
	public void twentyFourHour() {
		TransportClockFormat f = new TransportClockFormat(true, Locale.US);
		assertEquals("08:05", f.format(epoch(NY, 8, 5, 0), NY));
		assertEquals("00:00", f.format(epoch(NY, 0, 0, 0), NY));
		assertEquals("23:59", f.format(epoch(NY, 23, 59, 59), NY));
	}

	@Test
	public void twelveHour() {
		TransportClockFormat f = new TransportClockFormat(false, Locale.US);
		assertEquals("8:05 AM", f.format(epoch(NY, 8, 5, 0), NY));
		assertEquals("12:00 PM", f.format(epoch(NY, 12, 0, 0), NY));
		assertEquals("12:30 AM", f.format(epoch(NY, 0, 30, 0), NY));
	}

	@Test
	public void secondsAreCutNotRounded() {
		TransportClockFormat f = new TransportClockFormat(true, Locale.US);
		assertEquals("08:05", f.format(epoch(NY, 8, 5, 59), NY));
	}

	@Test
	public void theRouteZoneIsUsedNotTheDeviceZone() {
		TransportClockFormat f = new TransportClockFormat(true, Locale.US);
		long t = epoch(NY, 8, 15, 0);
		assertEquals("08:15", f.format(t, NY));
		assertEquals("22:15", f.format(t, TOKYO));
	}

	@Test
	public void rangeAcrossMidnightShowsBothClocks() {
		TransportClockFormat f = new TransportClockFormat(true, Locale.US);
		TimedTransportRoute r = timed(epoch(NY, 23, 50, 0), epoch(NY, 23, 50, 0) + 40 * 60, NY);
		assertEquals("23:50 – 00:30 +1", f.formatRange(r));
		assertEquals("00:30 +1", f.formatArrival(r));
		assertEquals("23:50", f.formatDeparture(r));
	}

	@Test
	public void daylightSavingChangeIsHandledByTheZone() {
		TransportClockFormat f = new TransportClockFormat(true, Locale.US);
		// 2026-11-01 01:30 EDT, one hour of wall clock later is 01:30 EST: 2 h of elapsed time
		long before = LocalDateTime.of(2026, 11, 1, 1, 30).atZone(NY).withEarlierOffsetAtOverlap().toEpochSecond();
		TimedTransportRoute r = timed(before, before + 3600, NY);
		assertEquals("01:30 – 01:30", f.formatRange(r));
	}
}
