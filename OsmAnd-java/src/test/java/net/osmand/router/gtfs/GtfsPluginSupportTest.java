package net.osmand.router.gtfs;

import static net.osmand.router.gtfs.GtfsFixture.TUE;
import static net.osmand.router.gtfs.GtfsFixture.at;
import static net.osmand.router.gtfs.GtfsFixture.epoch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.osmand.data.LatLon;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.TransportRouteRequest;
import net.osmand.router.TransportRouteResult;
import net.osmand.router.gtfs.GtfsIndexSummary.Validity;

/** The parts of the GTFS plugin that do not need Android: feed bookkeeping, summary, coverage, provider. */
public class GtfsPluginSupportTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File zip; // stops A..D on one line, a trip A to D on a Tuesday morning
	private GtfsFeedStore store;

	@Before
	public void setUp() throws IOException {
		GtfsFixture f = GtfsFixture.basic();
		f.trip("t1", "R1", "WD", "A 08:10:00", "B 08:12:00", "C 08:15:00", "D 08:18:00");
		zip = f.write(tmp.newFolder(), "feed.zip");
		store = new GtfsFeedStore(tmp.newFolder());
	}

	private File add(String name) throws IOException {
		return store.addFeed(name, Files.newInputStream(zip.toPath()), zip.length(), null);
	}

	private static TransportRouteRequest request(LatLon from, LatLon to, long depart, RouteCalculationProgress p) {
		return new TransportRouteRequest(from, to, depart, new HashMap<String, String>(), null, p);
	}

	// ---- store

	@Test
	public void safeNameStripsPathsAndOddCharacters() {
		assertEquals("evil.zip", GtfsFeedStore.safeName("../../evil.zip"));
		assertEquals("a_b_c.zip", GtfsFeedStore.safeName("x/y\\a b c.ZIP"));
		assertEquals("feed.zip", GtfsFeedStore.safeName(null));
		assertEquals("feed.zip", GtfsFeedStore.safeName("..."));
		assertEquals("MTA_subway.zip", GtfsFeedStore.safeName("MTA subway"));
		assertTrue(GtfsFeedStore.safeName(new String(new char[500]).replace('\0', 'x')).length() <= 84);
	}

	@Test
	public void addListRemoveAndDuplicateNames() throws IOException {
		assertTrue(store.listFeeds().isEmpty());
		add("mta.zip");
		add("mta.zip");
		List<GtfsFeedStore.Feed> feeds = store.listFeeds();
		assertEquals(2, feeds.size());
		assertEquals("mta-2.zip", feeds.get(0).name.equals("mta-2.zip") ? feeds.get(0).name : feeds.get(1).name);
		assertEquals(zip.length(), feeds.get(0).sizeBytes);
		assertTrue(store.removeFeed("mta.zip"));
		assertFalse(store.removeFeed("mta.zip"));
		assertFalse(store.removeFeed("../index"));
		assertEquals(1, store.listFeeds().size());
	}

	@Test
	public void nonZipIsRejectedAndLeavesNothing() throws IOException {
		try {
			store.addFeed("notes.zip", new ByteArrayInputStream("hello world".getBytes()), 11, null);
			fail();
		} catch (IOException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("not a zip"));
		}
		try {
			store.addFeed("empty.zip", new ByteArrayInputStream(new byte[0]), 0, null);
			fail();
		} catch (IOException e) {
			assertTrue(e.getMessage().contains("not a zip"));
		}
		assertTrue(store.listFeeds().isEmpty());
	}

	@Test
	public void cancelledCopyLeavesNoPartFile() throws IOException {
		try {
			store.addFeed("a.zip", Files.newInputStream(zip.toPath()), zip.length(), () -> true);
			fail();
		} catch (GtfsFeedStore.CancelledException expected) {
		}
		assertTrue(store.listFeeds().isEmpty());
		// a .part left by a killed process is cleaned up and never listed
		File dirWithLeftover = tmp.newFolder("leftover");
		new File(dirWithLeftover, "feeds").mkdirs();
		File part = new File(dirWithLeftover, "feeds/old.zip.part");
		assertTrue(part.createNewFile());
		GtfsFeedStore s2 = new GtfsFeedStore(dirWithLeftover);
		assertTrue(s2.listFeeds().isEmpty());
		s2.deleteLeftovers();
		assertFalse(part.exists());
	}

	@Test
	public void lowSpaceIsReportedBeforeCopying() throws IOException {
		try {
			store.addFeed("big.zip", new ByteArrayInputStream(new byte[0]), Long.MAX_VALUE / 2, null);
			fail();
		} catch (IOException e) {
			assertTrue(e.getMessage(), e.getMessage().startsWith("Not enough free space"));
		}
	}

	@Test
	public void rebuildMakesIndexAndRemovingTheLastFeedDeletesIt() throws IOException {
		add("feed.zip");
		GtfsFeedImporter.Report r = store.rebuild(null);
		assertEquals(4, r.stops);
		assertTrue(store.hasIndex());
		assertTrue(store.removeFeed("feed.zip"));
		assertNull(store.rebuild(null));
		assertFalse(store.hasIndex());
	}

	@Test
	public void failedRebuildKeepsThePreviousIndex() throws IOException {
		add("feed.zip");
		store.rebuild(null);
		byte[] before = Files.readAllBytes(store.getIndexFile().toPath());
		File feeds = new File(store.getIndexFile().getParentFile(), "feeds");
		Files.write(new File(feeds, "broken.zip").toPath(), "PK\3\4 not really".getBytes());
		try {
			store.rebuild(null);
			fail();
		} catch (IOException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("broken.zip"));
		}
		assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(store.getIndexFile().toPath())));
	}

	@Test
	public void stagedRemovalCanBeUndoneAndLeftoversAreRepaired() throws IOException {
		add("a.zip");
		add("b.zip");
		assertTrue(store.stageRemoval("a.zip"));
		assertFalse(store.stageRemoval("a.zip"));
		assertEquals(1, store.listFeeds().size());
		store.finishRemoval("a.zip", false);
		assertEquals(2, store.listFeeds().size());
		assertTrue(store.stageRemoval("a.zip"));
		store.finishRemoval("a.zip", true);
		assertEquals("b.zip", store.listFeeds().get(0).name);
		// killed between stage and finish: the next start puts the feed back and drops build leftovers
		assertTrue(store.stageRemoval("b.zip"));
		File tmpIndex = new File(store.getIndexFile().getParentFile(), "gtfs.index.tmp.run0");
		assertTrue(tmpIndex.createNewFile());
		store.deleteLeftovers();
		assertEquals(1, store.listFeeds().size());
		assertFalse(tmpIndex.exists());
	}

	@Test
	public void feedMissingStopTimesNamesTheFile() throws IOException {
		File bad = GtfsFixture.zipOf(tmp.newFolder(), "bad.zip", "stops.txt", "stop_id,stop_name,stop_lat,stop_lon\nA,A,1,1\n");
		store.addFeed("bad.zip", Files.newInputStream(bad.toPath()), bad.length(), null);
		try {
			store.rebuild(null);
			fail();
		} catch (IOException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("missing"));
		}
		assertFalse(store.hasIndex());
	}

	// ---- summary and coverage

	@Test
	public void summaryReportsCountsAndValidity() throws IOException {
		add("feed.zip");
		store.rebuild(null);
		GtfsIndex ix = GtfsIndex.open(store.getIndexFile());
		GtfsIndexSummary s = GtfsIndexSummary.of(ix, store.getIndexFile());
		assertEquals(4, s.stopCount);
		assertEquals(1, s.feedCount);
		assertEquals(store.getIndexFile().length(), s.sizeBytes);
		assertEquals(LocalDate.of(2026, 1, 1), s.validFrom);
		assertEquals(LocalDate.of(2027, 12, 31), s.validTo);
		assertEquals(Validity.NOT_STARTED, s.validity(LocalDate.of(2025, 12, 31)));
		assertEquals(Validity.ACTIVE, s.validity(LocalDate.of(2026, 1, 1)));
		assertEquals(Validity.ACTIVE, s.validity(LocalDate.of(2027, 12, 31)));
		assertEquals(Validity.EXPIRED, s.validity(LocalDate.of(2028, 1, 1)));
		assertEquals(Validity.ACTIVE, s.validity(epoch(TUE, "12:00") * 1000));
		assertEquals(Validity.NO_SERVICE, new GtfsIndexSummary(1, 1, 1, 1, 1, 1, 1, null, null, null).validity(LocalDate.now()));
	}

	@Test
	public void coverageUsesBoxPlusMargin() {
		GtfsCoverage c = new GtfsCoverage(40.0, 41.0, -74.0, -73.0);
		assertTrue(c.contains(new LatLon(40.5, -73.5), 0));
		assertFalse(c.contains(new LatLon(41.005, -73.5), 0));
		assertTrue(c.contains(new LatLon(41.005, -73.5), 800)); // 0.005 deg = 556 m
		assertFalse(c.contains(new LatLon(41.02, -73.5), 800));
		assertTrue(c.contains(new LatLon(40.5, -74.008), 800)); // 0.008 deg of lon is 680 m at this latitude
		assertFalse(c.contains(new LatLon(40.5, -74.02), 800));
		assertFalse(c.contains(null, 800));
	}

	@Test
	public void coverageFromIndexSpansItsStops() throws IOException {
		add("feed.zip");
		store.rebuild(null);
		GtfsCoverage c = GtfsCoverage.of(GtfsIndex.open(store.getIndexFile()));
		assertTrue(c.contains(at('A'), 0));
		assertTrue(c.contains(at('D'), 0));
		assertFalse(c.contains(new LatLon(40.80, -73.99), 100));
		assertFalse(c.contains(new LatLon(40.70, -73.5), 100));
	}

	// ---- index source

	@Test
	public void sourceIsLazyCachedAndNoticesAChange() throws IOException {
		GtfsIndexSource src = new GtfsIndexSource(store.getIndexFile());
		assertNull(src.get());
		assertNull(src.getLastError());
		add("feed.zip");
		store.rebuild(null);
		GtfsIndexSource.Loaded a = src.get();
		assertNotNull(a);
		assertSame(a, src.get());
		add("second.zip");
		store.rebuild(null);
		GtfsIndexSource.Loaded b = src.get();
		assertNotNull(b);
		assertNotSame(a, b);
		assertEquals(2, b.index.getFeedCount());
		store.removeFeed("feed.zip");
		store.removeFeed("second.zip");
		store.rebuild(null);
		assertNull(src.get());
	}

	@Test
	public void sourceReportsACorruptFileOnce() throws IOException {
		File ix = store.getIndexFile();
		Files.write(ix.toPath(), new byte[200]);
		GtfsIndexSource src = new GtfsIndexSource(ix);
		assertNull(src.get());
		assertNotNull(src.getLastError());
		src.invalidate();
		assertNull(src.getLastError());
	}

	// ---- provider

	@Test
	public void providerPlansFromTheIndexAndReturnsTimedResults() throws IOException {
		add("feed.zip");
		store.rebuild(null);
		GtfsTransitRouteProvider p = new GtfsTransitRouteProvider(new GtfsIndexSource(store.getIndexFile()), () -> 800);
		List<TransportRouteResult> res = p.calculateRoutes(request(at('A'), at('D'), epoch(TUE, "08:00"), new RouteCalculationProgress()));
		assertNotNull(res);
		assertFalse(res.isEmpty());
		assertEquals(epoch(TUE, "08:18"), ((net.osmand.router.TimedTransportRoute) res.get(0)).getArrivalTimeSec());
	}

	@Test
	public void providerStepsAsideWithoutIndexOutsideCoverageOrWithoutTrips() throws IOException {
		GtfsTransitRouteProvider none = new GtfsTransitRouteProvider(new GtfsIndexSource(store.getIndexFile()), () -> 800);
		assertNull(none.calculateRoutes(request(at('A'), at('D'), epoch(TUE, "08:00"), null)));

		add("feed.zip");
		store.rebuild(null);
		GtfsTransitRouteProvider p = new GtfsTransitRouteProvider(new GtfsIndexSource(store.getIndexFile()), () -> 800);
		LatLon far = new LatLon(40.85, -73.98);
		assertNull(p.calculateRoutes(request(at('A'), far, epoch(TUE, "08:00"), null)));
		assertNull(p.calculateRoutes(request(far, at('D'), epoch(TUE, "08:00"), null)));
		// after the last trip of the day, and on a Saturday (no service): nothing found
		assertNull(p.calculateRoutes(request(at('A'), at('D'), epoch(TUE, "23:00"), null)));
		assertNull(p.calculateRoutes(request(at('A'), at('D'), epoch(GtfsFixture.SAT, "08:00"), null)));
	}

	@Test
	public void providerHonoursTheWalkingLimitSetting() throws IOException {
		add("feed.zip");
		store.rebuild(null);
		int[] walk = {800};
		GtfsTransitRouteProvider p = new GtfsTransitRouteProvider(new GtfsIndexSource(store.getIndexFile()), () -> walk[0]);
		LatLon nearA = new LatLon(at('A').getLatitude(), at('A').getLongitude() - 0.006); // about 510 m west of A (660 m with the walk detour factor)
		assertNotNull(p.calculateRoutes(request(nearA, at('D'), epoch(TUE, "08:00"), null)));
		walk[0] = 300;
		assertNull(p.calculateRoutes(request(nearA, at('D'), epoch(TUE, "08:00"), null)));
	}

	@Test
	public void providerReturnsNullWhenCancelled() throws IOException {
		add("feed.zip");
		store.rebuild(null);
		GtfsTransitRouteProvider p = new GtfsTransitRouteProvider(new GtfsIndexSource(store.getIndexFile()), () -> 800);
		RouteCalculationProgress progress = new RouteCalculationProgress();
		progress.isCancelled = true;
		assertNull(p.calculateRoutes(request(at('A'), at('D'), epoch(TUE, "08:00"), progress)));
	}
}
