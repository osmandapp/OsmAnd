package net.osmand.router.gtfs;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.router.gtfs.guidance.GuidanceNames;

/**
 * Synthetic itineraries for the guidance tests. Lives in this package because the GtfsLeg and GtfsItinerary
 * factories are package-private. Stops 0..9 lie on one west-east line 850 m apart; stops 10..19 on a line
 * 222 m north of it (same spacing), so stop 3 and stop 10 are a 222 m transfer walk apart.
 */
public final class GuidanceTestItineraries {

	public static final ZoneId NY = ZoneId.of("America/New_York");
	/** 2026-11-17 08:00 New York. */
	public static final long T0 = ZonedDateTime.of(2026, 11, 17, 8, 0, 0, 0, NY).toEpochSecond();
	/** 2026-11-17 23:50 New York; the standard itinerary then boards at 00:00. */
	public static final long T_LATE = ZonedDateTime.of(2026, 11, 17, 23, 50, 0, 0, NY).toEpochSecond();
	public static final LatLon ORIGIN = new LatLon(40.699, -74.00);
	public static final LatLon DEST = new LatLon(40.703, -74.00);

	private GuidanceTestItineraries() {
	}

	public static LatLon loc(int stop) {
		return stop < 10 ? new LatLon(40.70, -74.00 + 0.01 * stop) : new LatLon(40.702, -74.03 + 0.01 * (stop - 10));
	}

	/** A point this many metres north (negative: south) of p. */
	public static LatLon north(LatLon p, double meters) {
		return new LatLon(p.getLatitude() + meters / 111195.0, p.getLongitude());
	}

	/** A point this many metres east of p (at latitude about 40.7). */
	public static LatLon east(LatLon p, double meters) {
		return new LatLon(p.getLatitude(), p.getLongitude() + meters / 84300.0);
	}

	/** Stops are called "Stop N"; trip 1 heads "Uptown", others have no headsign; route ids give the line name. */
	public static GuidanceNames names() {
		return new GuidanceNames() {
			@Override
			public String stopName(int stopIdx) {
				return "Stop " + stopIdx;
			}

			@Override
			public LatLon stopLocation(int stopIdx) {
				return loc(stopIdx);
			}

			@Override
			public String headsign(GtfsLeg ride) {
				return ride.tripIdx == 1 ? "Uptown" : null;
			}
		};
	}

	/** Same names but without stop locations (location-based ride progress is off). */
	public static GuidanceNames namesWithoutLocations() {
		final GuidanceNames n = names();
		return new GuidanceNames() {
			@Override
			public String stopName(int stopIdx) {
				return n.stopName(stopIdx);
			}

			@Override
			public String headsign(GtfsLeg ride) {
				return n.headsign(ride);
			}
		};
	}

	public static GtfsLeg walk(long start, long end, int fromStop, int toStop, LatLon from, LatLon to) {
		return GtfsLeg.walk(start, end, fromStop, toStop, from, to, 100);
	}

	/**
	 * A ride that departs the first stop at firstDep, then every hopSec; each later stop arrives 30 s before it
	 * departs, and the last stop's departure equals its arrival.
	 */
	public static GtfsLeg ride(int tripIdx, String routeId, long firstDep, int hopSec, int... stops) {
		int n = stops.length;
		long[] arr = new long[n], dep = new long[n];
		for (int k = 0; k < n; k++) {
			dep[k] = firstDep + (long) k * hopSec;
			arr[k] = k == 0 ? dep[k] : dep[k] - 30;
		}
		dep[n - 1] = arr[n - 1];
		return GtfsLeg.ride(tripIdx, 0, 5, 5 + n - 1, stops, arr, dep, loc(stops[0]), loc(stops[n - 1]),
				"f:trip" + tripIdx, "f:" + routeId);
	}

	public static GtfsItinerary itinerary(GtfsLeg... legs) {
		List<GtfsLeg> list = new ArrayList<>(Arrays.asList(legs));
		int rides = 0;
		for (GtfsLeg l : legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				rides++;
			}
		}
		return new GtfsItinerary(list, legs[0].startSec, legs[legs.length - 1].endSec, Math.max(0, rides - 1));
	}

	/**
	 * Walk 0-180 s to stop 0; ride Q (trip 1) stops 0..3 departing at +600 s; transfer walk to stop 10
	 * (+1110..+1290); ride F (trip 2) stops 10..13 departing at +1500; final walk +2010..+2130 to DEST.
	 */
	public static GtfsItinerary standard(long base) {
		GtfsLeg q = ride(1, "Q", base + 600, 180, 0, 1, 2, 3);
		GtfsLeg f = ride(2, "F", base + 1500, 180, 10, 11, 12, 13);
		return itinerary(walk(base, base + 180, -1, 0, ORIGIN, loc(0)), q,
				walk(q.endSec, q.endSec + 180, 3, 10, loc(3), loc(10)), f,
				walk(f.endSec, f.endSec + 120, 13, -1, loc(13), DEST));
	}

	/** Just the subway ride: stops 0..5 departing at +300, 120 s apart. */
	public static GtfsItinerary singleRide(long base) {
		return itinerary(ride(1, "Q", base + 300, 120, 0, 1, 2, 3, 4, 5));
	}
}
