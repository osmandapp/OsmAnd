package net.osmand.router.gtfs.selftest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.osmand.data.LatLon;
import net.osmand.router.TimedTransportRoute;
import net.osmand.router.TransportRouteRequest;
import net.osmand.router.TransportRouteResult;
import net.osmand.router.gtfs.GtfsIndexSource;
import net.osmand.router.gtfs.GtfsTransitRouteProvider;

/**
 * The "walking vs. transit" setting reaches the planner through the provider: on the Avenue J area to Murrow trip at
 * 07:25 the earliest bus arrives first with the default, and "less walking" puts the subway ahead of it.
 * Needs the real MTA feeds: -Dgtfs.mta.dir=...
 */
public class GtfsWalkPreferenceProviderTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static List<TransportRouteResult> plan(GtfsTransitRouteProvider p, long depart) {
		return p.calculateRoutes(new TransportRouteRequest(new LatLon(40.6272, -73.9615), new LatLon(40.6212, -73.9598),
				depart, new HashMap<String, String>(), null, null));
	}

	@Test
	public void lessWalkingChangesTheFirstOptionAndFastestKeepsTheEarliestArrival() throws Exception {
		String dir = System.getProperty("gtfs.mta.dir");
		Assume.assumeTrue("gtfs.mta.dir is not set", dir != null && !dir.isEmpty());
		List<File> zips = new ArrayList<>();
		for (String n : new String[] {"gtfs_b", "gtfs_busco", "gtfs_bx", "gtfs_m", "gtfs_q", "gtfs_si", "gtfs_subway"}) {
			File f = new File(dir, n + ".zip");
			Assume.assumeTrue(f + " is missing", f.isFile());
			zips.add(f);
		}
		SelfTestEnv env = new SelfTestEnv(tmp.newFolder("w"), zips);
		env.sink = l -> { };
		GtfsIndexSource source = new GtfsIndexSource(env.ensureIndex());
		int[] percent = {0};
		GtfsTransitRouteProvider p = new GtfsTransitRouteProvider(source, () -> 800);
		p.setWalkWeightPercent(() -> percent[0]);
		long depart = ZonedDateTime.of(2026, 11, 12, 7, 25, 0, 0, ZoneId.of("America/New_York")).toEpochSecond();

		List<TransportRouteResult> fastest = plan(p, depart);
		assertNotNull(fastest);
		assertTrue(fastest.size() >= 2);
		long best = ((TimedTransportRoute) fastest.get(0)).getArrivalTimeSec();
		for (TransportRouteResult r : fastest) {
			assertTrue("fastest puts the earliest arrival first", best <= ((TimedTransportRoute) r).getArrivalTimeSec());
		}

		percent[0] = 200;
		List<TransportRouteResult> lessWalking = plan(p, depart);
		assertNotNull(lessWalking);
		assertEquals(fastest.size(), lessWalking.size());
		long first = ((TimedTransportRoute) lessWalking.get(0)).getArrivalTimeSec();
		assertTrue("less walking leads with a slower, shorter-walk option", first > best);
		double walkFirst = lessWalking.get(0).getWalkDist();
		double walkFastest = fastest.get(0).getWalkDist();
		assertTrue("and that option walks less (" + walkFirst + " vs " + walkFastest + ")", walkFirst < walkFastest);

		percent[0] = 0;
		assertEquals("back to the default, the same first option",
				best, ((TimedTransportRoute) plan(p, depart).get(0)).getArrivalTimeSec());
	}
}
