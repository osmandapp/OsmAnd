package net.osmand.router.gtfs;

import static net.osmand.router.gtfs.GtfsFixture.TUE;
import static net.osmand.router.gtfs.GtfsFixture.epoch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.osmand.data.LatLon;
import net.osmand.router.gtfs.guidance.GuidanceController;

public class GtfsIndexGuidanceNamesTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private GtfsIndex ix;
	private GtfsRoutePlanner planner;

	@Before
	public void setUp() throws IOException {
		GtfsFixture f = GtfsFixture.basic();
		f.trip("t1", "R1", "WD", "A 08:10:00", "B 08:12:00", "C 08:15:00");
		f.trip("t2", "R2", "WD", "A 08:20:00", "B 08:22:00", "C 08:25:00");
		File out = new File(tmp.newFolder(), "index.bin");
		new GtfsFeedImporter(null).importFeed(f.write(tmp.newFolder(), "feed.zip"), out);
		ix = GtfsIndex.open(out, true);
		planner = new GtfsRoutePlanner(ix);
	}

	@Test
	public void namesComeFromTheIndex() {
		GtfsIndexGuidanceNames names = new GtfsIndexGuidanceNames(ix);
		int a = -1;
		for (int i = 0; i < ix.getStopCount(); i++) {
			if (ix.getStopName(i).equals("Stop A")) {
				a = i;
			}
		}
		assertEquals("Stop A", names.stopName(a));
		assertNull(names.stopName(-1));
		assertNull(names.stopName(ix.getStopCount()));
		LatLon loc = names.stopLocation(a);
		assertEquals(GtfsFixture.at('A').getLatitude(), loc.getLatitude(), 1e-4);
		assertEquals(GtfsFixture.at('A').getLongitude(), loc.getLongitude(), 1e-4);
		assertNull(names.stopLocation(-1));
	}

	@Test
	public void plannedRideIsGuidedWithLineNameAndHeadsign() {
		List<GtfsItinerary> its = planner.plan(GtfsFixture.at('A'), GtfsFixture.at('C'), epoch(TUE, "08:00"),
				new GtfsPlanOptions(), null);
		GtfsLeg ride = null;
		for (GtfsLeg l : its.get(0).legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				ride = l;
			}
		}
		GtfsIndexGuidanceNames names = new GtfsIndexGuidanceNames(ix);
		assertEquals("L1", names.routeName(ride));
		assertEquals("To t1", names.headsign(ride));
		GuidanceController c = new GuidanceController(its.get(0), names);
		c.start(epoch(TUE, "08:00"));
		c.onCommand(GuidanceController.Command.BOARDED, epoch(TUE, "08:10"));
		assertEquals("guidance.rideStarted", c.status(epoch(TUE, "08:10")).textKey);
		assertEquals("Stop C", c.status(epoch(TUE, "08:10")).args.get(0));
	}
}
