package net.osmand.router.gtfs;

import java.io.IOException;
import java.nio.IntBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

/**
 * Which services run on which day, and where a service day starts on the clock.
 * <p>
 * Built from calendar.txt (weekday pattern and date range) and calendar_dates.txt (added and
 * removed dates). Either file may be missing, but not both. All dates are epoch days.
 * <p>
 * A GTFS service day does not start at midnight: time 00:00:00 is "noon minus 12 hours" in the agency
 * time zone, so a day with a DST change is 23 or 25 hours long, and trips may run past 24:00:00.
 * A query therefore has to look at three service days: yesterday (trips that run past midnight),
 * today and tomorrow (a late-evening query that reaches trips of the next service day).
 */
public final class GtfsCalendar {

	public static final int EXCEPTION_ADDED = 1;
	public static final int EXCEPTION_REMOVED = 2;

	private static final int NEVER_START = Integer.MAX_VALUE;
	private static final int NEVER_END = Integer.MIN_VALUE;

	final int[] weekdayMask; // bit 0 = Monday ... bit 6 = Sunday
	final int[] startDay;
	final int[] endDay;
	final int[] exceptionOffset; // CSR into exceptionDay/exceptionType, length services + 1
	final int[] exceptionDay;
	final int[] exceptionType;
	private final ZoneId zone;

	GtfsCalendar(int[] weekdayMask, int[] startDay, int[] endDay, int[] exceptionOffset, int[] exceptionDay,
	             int[] exceptionType, ZoneId zone) {
		this.weekdayMask = weekdayMask;
		this.startDay = startDay;
		this.endDay = endDay;
		this.exceptionOffset = exceptionOffset;
		this.exceptionDay = exceptionDay;
		this.exceptionType = exceptionType;
		this.zone = zone;
	}

	public ZoneId getZone() {
		return zone;
	}

	public int getServiceCount() {
		return weekdayMask.length;
	}

	/** Services running on the given calendar date (exceptions applied). */
	public boolean[] activeServices(LocalDate date) {
		int day = (int) date.toEpochDay();
		int dow = date.getDayOfWeek().getValue() - 1;
		boolean[] active = new boolean[weekdayMask.length];
		for (int s = 0; s < active.length; s++) {
			boolean on = day >= startDay[s] && day <= endDay[s] && ((weekdayMask[s] >> dow) & 1) == 1;
			for (int j = exceptionOffset[s]; j < exceptionOffset[s + 1]; j++) {
				if (exceptionDay[j] == day) {
					on = exceptionType[j] == EXCEPTION_ADDED;
				}
			}
			active[s] = on;
		}
		return active;
	}

	/** Bitset over trip indices of the trips whose service runs on the date. */
	public long[] activeTrips(LocalDate date, IntBuffer tripService, int tripCount) {
		boolean[] services = activeServices(date);
		long[] bits = new long[(tripCount + 63) >> 6];
		for (int t = 0; t < tripCount; t++) {
			int s = tripService.get(t);
			if (s >= 0 && services[s]) {
				bits[t >> 6] |= 1L << (t & 63);
			}
		}
		return bits;
	}

	/** Epoch second of service time 00:00:00 of the date: noon minus 12 hours in the agency zone. */
	public long serviceDayStart(LocalDate date) {
		return ZonedDateTime.of(date, LocalTime.NOON, zone).toEpochSecond() - 12 * 3600;
	}

	/** Calendar date at the given instant in the agency zone. */
	public LocalDate localDate(long epochSec) {
		return Instant.ofEpochSecond(epochSec).atZone(zone).toLocalDate();
	}

	/**
	 * Start of yesterday's, today's and tomorrow's service day relative to the start of today's, in
	 * seconds: {-86400, 0, 86400} except around a DST change.
	 */
	public int[] cursorOffsets(LocalDate today) {
		long t0 = serviceDayStart(today);
		return new int[] { (int) (serviceDayStart(today.minusDays(1)) - t0), 0,
				(int) (serviceDayStart(today.plusDays(1)) - t0) };
	}

	/** Collects services from the calendar files of one or more feeds. */
	public static final class Builder {
		private final TIntArrayList mask = new TIntArrayList();
		private final TIntArrayList start = new TIntArrayList();
		private final TIntArrayList end = new TIntArrayList();
		private final TIntArrayList excService = new TIntArrayList();
		private final TIntArrayList excDay = new TIntArrayList();
		private final TIntArrayList excType = new TIntArrayList();
		private Map<String, Integer> ids = new HashMap<>();
		private int validFrom = Integer.MAX_VALUE;
		private int validTo = Integer.MIN_VALUE;

		/** Service ids are per feed: call before the files of each feed. */
		public void startFeed() {
			ids = new HashMap<>();
		}

		/** Global service index of a service id of the current feed, or -1. */
		public int serviceIndex(String serviceId) {
			Integer i = ids.get(serviceId);
			return i == null ? -1 : i;
		}

		private int serviceFor(String id) {
			Integer i = ids.get(id);
			if (i == null) {
				i = mask.size();
				ids.put(id, i);
				mask.add(0);
				start.add(NEVER_START);
				end.add(NEVER_END);
			}
			return i;
		}

		public void readCalendar(GtfsCsv csv) throws IOException {
			String[] names = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
			int[] dayCols = new int[7];
			for (int d = 0; d < 7; d++) {
				dayCols[d] = csv.column(names[d]);
			}
			int idCol = csv.column("service_id"), startCol = csv.column("start_date"), endCol = csv.column("end_date");
			while (csv.next()) {
				int s = csv.getDate(startCol), e = csv.getDate(endCol);
				if (s == GtfsCsv.NO_DATE || e == GtfsCsv.NO_DATE || csv.isEmpty(idCol)) {
					continue;
				}
				int m = 0;
				for (int d = 0; d < 7; d++) {
					if (csv.getInt(dayCols[d], 0) == 1) {
						m |= 1 << d;
					}
				}
				int i = serviceFor(csv.get(idCol));
				mask.set(i, m);
				start.set(i, s);
				end.set(i, e);
				validFrom = Math.min(validFrom, s);
				validTo = Math.max(validTo, e);
			}
		}

		public void readCalendarDates(GtfsCsv csv) throws IOException {
			int idCol = csv.column("service_id"), dateCol = csv.column("date"), typeCol = csv.column("exception_type");
			while (csv.next()) {
				int day = csv.getDate(dateCol), type = csv.getInt(typeCol, 0);
				if (day == GtfsCsv.NO_DATE || csv.isEmpty(idCol) || (type != EXCEPTION_ADDED && type != EXCEPTION_REMOVED)) {
					continue;
				}
				excService.add(serviceFor(csv.get(idCol)));
				excDay.add(day);
				excType.add(type);
				if (type == EXCEPTION_ADDED) {
					validFrom = Math.min(validFrom, day);
					validTo = Math.max(validTo, day);
				}
			}
		}

		/** First day with any service (epoch day), or Integer.MAX_VALUE if there is none. */
		public int getValidFrom() {
			return validFrom;
		}

		public int getValidTo() {
			return validTo;
		}

		public GtfsCalendar build(ZoneId zone) {
			int n = mask.size(), nx = excService.size();
			int[] off = new int[n + 1];
			for (int i = 0; i < nx; i++) {
				off[excService.get(i) + 1]++;
			}
			for (int i = 0; i < n; i++) {
				off[i + 1] += off[i];
			}
			int[] day = new int[nx], type = new int[nx], cur = new int[n];
			System.arraycopy(off, 0, cur, 0, n);
			for (int i = 0; i < nx; i++) { // stable: a later row for the same date wins
				int p = cur[excService.get(i)]++;
				day[p] = excDay.get(i);
				type[p] = excType.get(i);
			}
			return new GtfsCalendar(mask.toArray(), start.toArray(), end.toArray(), off, day, type, zone);
		}
	}
}
