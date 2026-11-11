package net.osmand.router.gtfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.osmand.data.LatLon;

/**
 * Real-feed check, skipped unless the system property gtfs.mta.dir names a directory with the MTA
 * zips (gtfs_subway, gtfs_b, gtfs_q, gtfs_bx, gtfs_m, gtfs_si, gtfs_busco). It writes a 250 MB index
 * to java.io.tmpdir. The expected counts are those of the feeds downloaded on 2026-11-10.
 */
public class GtfsMtaSmokeTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void fullNycFeed() throws Exception {
		String dir = System.getProperty("gtfs.mta.dir");
		Assume.assumeTrue("gtfs.mta.dir is not set", dir != null && !dir.isEmpty());
		List<File> zips = new ArrayList<>();
		for (String name : new String[] {"gtfs_subway", "gtfs_b", "gtfs_q", "gtfs_bx", "gtfs_m", "gtfs_si", "gtfs_busco"}) {
			File zip = new File(dir, name + ".zip");
			Assume.assumeTrue(zip + " is missing", zip.isFile());
			zips.add(zip);
		}
		File out = new File(tmp.newFolder(), "mta.idx");
		GtfsFeedImporter.Report report = new GtfsFeedImporter(null).importFeeds(zips, out);
		assertEquals(7_999_282L, report.stopTimeRows);
		GtfsIndex index = GtfsIndex.open(out, true);
		assertEquals(250_842, index.getTripCount());
		assertEquals(15_921, index.getStopCount());
		assertEquals(7_748_440, index.getConnectionCount());
		assertEquals(7_999_282, index.getStopTimeCount());

		// Ocean Av/Foster Av (Brooklyn) to Times Sq-42 St, a Tuesday at 08:00: the Q train from Cortelyou Rd
		LocalDate day = LocalDate.of(2026, 11, 17);
		Assume.assumeTrue("the feed no longer covers " + day,
				day.toEpochDay() >= index.getValidFrom() && day.toEpochDay() <= index.getValidTo());
		long depart = ZonedDateTime.of(day, LocalTime.of(8, 0), ZoneId.of("America/New_York")).toEpochSecond();
		List<GtfsItinerary> result = new GtfsRoutePlanner(index).plan(new LatLon(40.6383, -73.9624),
				new LatLon(40.7553, -73.9869), depart, null, null);
		assertFalse(result.isEmpty());
		GtfsItinerary best = result.get(0);
		long minutes = (best.arriveSec - depart) / 60;
		assertTrue("door to door " + minutes + " min", minutes >= 30 && minutes <= 50);
		boolean q = false;
		for (GtfsLeg leg : best.legs) {
			if (leg.type == GtfsLeg.Type.RIDE && "Q".equals(index.getRouteShortName(leg.routeIdx))) {
				q = true;
				assertEquals("Times Sq-42 St", index.getStopName(leg.toStop));
			}
		}
		assertTrue("expected the Q train", q);
	}
}
