package net.osmand.router.gtfs;

import static net.osmand.router.gtfs.GtfsFixture.NY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GtfsImporterTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private GtfsFixture twoLines() {
		return GtfsFixture.basic()
				.trip("t1", "R1", "WD", "A 08:00:00", "B 08:10:00")
				.trip("t2", "R2", "WD", "B 08:15:00", "C 08:25:00", "D 08:35:00")
				.trip("t3", "R2", "WD", "B 08:45:00", "C 08:55:00", "D 09:05:00");
	}

	private GtfsFeedImporter.Report importFeed(GtfsFixture f, File index) throws IOException {
		return new GtfsFeedImporter(null).importFeed(f.write(tmp.getRoot(), "feed.zip"), index);
	}

	private GtfsIndex index(GtfsFixture f) throws IOException {
		File out = new File(tmp.newFolder(), "index.bin");
		importFeed(f, out);
		return GtfsIndex.open(out, true);
	}

	private void assertImportFails(File zip, String messagePart) throws IOException {
		File out = new File(tmp.newFolder(), "index.bin");
		try {
			new GtfsFeedImporter(null).importFeed(zip, out);
			fail("expected an ImportException");
		} catch (GtfsFeedImporter.ImportException e) {
			assertTrue(e.getMessage(), e.getMessage().contains(messagePart));
			assertFalse(e.isCancelled());
		}
		assertEquals("no index and no temporary files may remain", 0, out.getParentFile().list().length);
	}

	@Test
	public void importsTablesAndAccessors() throws IOException {
		GtfsIndex ix = index(twoLines());
		assertEquals(4, ix.getStopCount());
		assertEquals(3, ix.getTripCount());
		assertEquals(2, ix.getRouteCount());
		assertEquals(5, ix.getConnectionCount());
		assertEquals(8, ix.getStopTimeCount());
		assertEquals(NY, ix.getZone());
		assertEquals("Stop B", ix.getStopName(1));
		assertEquals("feed:B", ix.getStopId(1));
		assertEquals(40.70, ix.getStopLat(1), 1e-7);
		assertEquals(-73.99, ix.getStopLon(1), 1e-7);
		assertEquals("L1", ix.getRouteShortName(0));
		assertEquals("Line Two", ix.getRouteLongName(1));
		assertEquals(1, ix.getRouteType(0));
		assertEquals(0xFF0000, ix.getRouteColor(0));
		assertEquals(GtfsIndex.NO_COLOR, ix.getRouteColor(1));
		assertEquals("To t2", ix.getTripHeadsign(1));
		assertEquals(1, ix.getTripRoute(1));
		assertEquals(3, ix.getTripStopCount(1));
		assertEquals(3, ix.getTripStop(1, 2));
		assertEquals(8 * 3600 + 25 * 60, ix.getTripArrival(1, 1));
		assertEquals(java.time.LocalDate.of(2026, 1, 1).toEpochDay(), ix.getValidFrom());
		assertEquals(java.time.LocalDate.of(2027, 12, 31).toEpochDay(), ix.getValidTo());
		assertEquals(1, ix.getFeedCount());
	}

	@Test
	public void importIsDeterministic() throws IOException {
		File a = new File(tmp.newFolder(), "index.bin"), b = new File(tmp.newFolder(), "index.bin");
		GtfsFixture f = twoLines();
		File zip = f.write(tmp.getRoot(), "feed.zip");
		new GtfsFeedImporter(null).importFeed(zip, a);
		new GtfsFeedImporter(null).importFeed(zip, b);
		assertArrayEquals(Files.readAllBytes(a.toPath()), Files.readAllBytes(b.toPath()));
	}

	@Test
	public void stopTimesInAnyOrderWithinATrip() throws IOException {
		GtfsFixture f = GtfsFixture.basic();
		f.row("trips.txt", "R1,WD,t1,x");
		f.row("stop_times.txt", "t1,08:20:00,08:20:00,C,3,,");
		f.row("stop_times.txt", "t1,08:00:00,08:00:00,A,1,,");
		f.row("stop_times.txt", "t1,08:10:00,08:10:00,B,2,,");
		GtfsIndex ix = index(f);
		assertEquals(3, ix.getTripStopCount(0));
		assertEquals(0, ix.getTripStop(0, 0));
		assertEquals(1, ix.getTripStop(0, 1));
		assertEquals(2, ix.getTripStop(0, 2));
		assertEquals(2, ix.getConnectionCount());
		assertEquals(8 * 3600, ix.connDep.get(0));
	}

	@Test
	public void stopTimesOfDifferentTripsInterleaved() throws IOException {
		GtfsFixture f = GtfsFixture.basic();
		f.row("trips.txt", "R1,WD,t1,x");
		f.row("trips.txt", "R2,WD,t2,y");
		f.row("stop_times.txt", "t1,08:00:00,08:00:00,A,1,,");
		f.row("stop_times.txt", "t2,09:00:00,09:00:00,C,1,,");
		f.row("stop_times.txt", "t1,08:10:00,08:10:00,B,2,,");
		f.row("stop_times.txt", "t2,09:10:00,09:10:00,D,2,,");
		File out = new File(tmp.newFolder(), "index.bin");
		GtfsFeedImporter.Report r = importFeed(f, out);
		GtfsIndex ix = GtfsIndex.open(out, true);
		assertEquals(4, r.stopTimeRows);
		assertEquals(2, ix.getConnectionCount());
		assertEquals(2, ix.getTripStopCount(0));
		assertEquals(0, ix.getTripStop(0, 0));
		assertEquals(1, ix.getTripStop(0, 1));
		assertEquals(2, ix.getTripStop(1, 0));
		assertEquals(3, ix.getTripStop(1, 1));
		assertEquals(9 * 3600 + 600, ix.getTripArrival(1, 1));
	}

	@Test
	public void blankTimesAreFilledAndInterpolated() throws IOException {
		GtfsFixture f = GtfsFixture.basic().trip("t1", "R1", "WD", "A 08:00:00", "B - -", "C - 08:22:00", "D 08:30:00");
		GtfsFeedImporter.Report r = importFeed(f, new File(tmp.newFolder(), "index.bin"));
		assertEquals(1, r.tripsInterpolated);
		GtfsIndex ix = index(f);
		assertEquals(4, ix.getTripStopCount(0));
		assertEquals(8 * 3600 + 11 * 60, ix.getTripArrival(0, 1)); // 08:00 .. 08:22 in three steps: B = 08:00 + 1/2 * 22 min
		assertEquals(8 * 3600 + 11 * 60, ix.getTripDeparture(0, 1));
		assertEquals(8 * 3600 + 22 * 60, ix.getTripArrival(0, 2)); // arrival copied from the departure
		assertEquals(3, ix.getConnectionCount());
	}

	@Test
	public void tripWithoutTimesAtTheEndsIsSkipped() throws IOException {
		GtfsFixture f = GtfsFixture.basic().trip("t1", "R1", "WD", "A -", "B 08:10:00", "C 08:20:00")
				.trip("t2", "R1", "WD", "A 09:00:00", "B 09:10:00");
		GtfsFeedImporter.Report r = importFeed(f, new File(tmp.newFolder(), "index.bin"));
		assertEquals(1, r.tripsSkipped);
		GtfsIndex ix = index(f);
		assertEquals(0, ix.getTripStopCount(0));
		assertEquals(1, ix.getConnectionCount());
	}

	@Test
	public void calendarDatesOnlyIsAccepted() throws IOException {
		GtfsFixture f = twoLines();
		f.files.remove("calendar.txt");
		f.file("calendar_dates.txt", "service_id,date,exception_type", "WD,20261117,1");
		GtfsIndex ix = index(f);
		assertEquals(java.time.LocalDate.of(2026, 11, 17).toEpochDay(), ix.getValidFrom());
		assertEquals(java.time.LocalDate.of(2026, 11, 17).toEpochDay(), ix.getValidTo());
		assertEquals(5, ix.getConnectionCount());
	}

	@Test
	public void frequencyTripsAreSkippedWithAWarning() throws IOException {
		GtfsFixture f = twoLines();
		f.file("frequencies.txt", "trip_id,start_time,end_time,headway_secs", "t3,08:00:00,12:00:00,600");
		File out = new File(tmp.newFolder(), "index.bin");
		GtfsFeedImporter.Report r = importFeed(f, out);
		assertEquals(1, r.warnings.size());
		assertTrue(r.warnings.get(0), r.warnings.get(0).contains("frequencies.txt"));
		assertEquals(1, r.tripsSkipped);
		GtfsIndex ix = GtfsIndex.open(out, true);
		assertEquals(3, ix.getTripCount());
		assertEquals(3, ix.getConnectionCount()); // t3's two connections are missing
		assertEquals(0, ix.getTripStopCount(2));
	}

	@Test
	public void feedInsideAFolderOfTheZipIsFound() throws IOException {
		GtfsFixture f = twoLines();
		String[] parts = new String[f.files.size() * 2];
		int i = 0;
		for (java.util.Map.Entry<String, java.util.List<String>> e : f.files.entrySet()) {
			parts[i++] = "gtfs/" + e.getKey();
			parts[i++] = String.join("\n", e.getValue()) + "\n";
		}
		File out = new File(tmp.newFolder(), "index.bin");
		new GtfsFeedImporter(null).importFeed(GtfsFixture.zipOf(tmp.getRoot(), "nested.zip", parts), out);
		assertEquals(5, GtfsIndex.open(out, true).getConnectionCount());
	}

	@Test
	public void severalFeedsShareOneIndexWithoutClashes() throws IOException {
		File z1 = twoLines().write(tmp.newFolder(), "one.zip");
		File z2 = GtfsFixture.basic().trip("t1", "R1", "WD", "C 10:00:00", "D 10:10:00").write(tmp.newFolder(), "two.zip");
		File out = new File(tmp.newFolder(), "index.bin");
		GtfsFeedImporter.Report r = new GtfsFeedImporter(null).importFeeds(Arrays.asList(z1, z2), out);
		GtfsIndex ix = GtfsIndex.open(out, true);
		assertEquals(2, ix.getFeedCount());
		assertEquals(8, ix.getStopCount());
		assertEquals(4, ix.getTripCount()); // trip id t1 exists in both feeds
		assertEquals(6, ix.getConnectionCount());
		assertEquals(r.connections, ix.getConnectionCount());
		assertEquals("one:A", ix.getStopId(0));
		assertEquals("two:A", ix.getStopId(4));
		assertEquals(10 * 3600, ix.connDep.get(ix.getConnectionCount() - 1)); // merged by departure
		assertEquals(2, ix.getTripRoute(3)); // route indices of feed two start after the two routes of feed one
	}

	// ------------------------------------------------------------------ bad input

	@Test
	public void notAZip() throws IOException {
		File f = tmp.newFile("junk.zip");
		Files.write(f.toPath(), "this is not a zip file at all, just text".getBytes());
		assertImportFails(f, "junk.zip");
	}

	@Test
	public void zeroByteFile() throws IOException {
		assertImportFails(tmp.newFile("zero.zip"), "zero.zip");
	}

	@Test
	public void emptyZipWithoutEntries() throws IOException {
		assertImportFails(GtfsFixture.zipOf(tmp.getRoot(), "empty.zip"), "stops.txt");
	}

	@Test
	public void missingStopsTxt() throws IOException {
		GtfsFixture f = twoLines();
		f.files.remove("stops.txt");
		assertImportFails(f.write(tmp.getRoot(), "nostops.zip"), "stops.txt");
	}

	@Test
	public void missingStopTimesAndTripsAndRoutes() throws IOException {
		for (String name : new String[] {"stop_times.txt", "trips.txt", "routes.txt"}) {
			GtfsFixture f = twoLines();
			f.files.remove(name);
			assertImportFails(f.write(tmp.newFolder(), "x.zip"), name);
		}
	}

	@Test
	public void neitherCalendarNorCalendarDates() throws IOException {
		GtfsFixture f = twoLines();
		f.files.remove("calendar.txt");
		assertImportFails(f.write(tmp.getRoot(), "nocal.zip"), "calendar");
	}

	@Test
	public void emptyStopsFileIsReportedByName() throws IOException {
		GtfsFixture f = twoLines();
		f.files.put("stops.txt", Collections.<String>emptyList());
		assertImportFails(f.write(tmp.getRoot(), "badcsv.zip"), "stops.txt");
	}

	// ------------------------------------------------------------------ index file checks

	private File goodIndex() throws IOException {
		File out = new File(tmp.newFolder(), "index.bin");
		importFeed(twoLines(), out);
		return out;
	}

	private void patch(File f, long pos, int... bytes) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
			raf.seek(pos);
			for (int b : bytes) {
				raf.write(b);
			}
		}
	}

	@Test
	public void wrongVersionIsRejected() throws IOException {
		File f = goodIndex();
		patch(f, GtfsIndex.H_VERSION, 99, 0, 0, 0);
		try {
			GtfsIndex.open(f);
			fail();
		} catch (GtfsIndexException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("version"));
		}
	}

	@Test
	public void checksumMismatchIsRejectedOnRequest() throws IOException {
		File f = goodIndex();
		GtfsIndex.open(f, true).getStopCount(); // intact
		patch(f, f.length() - 12, 0x55);
		assertEquals(4, GtfsIndex.open(f).getStopCount()); // structure is fine, only the CRC knows
		try {
			GtfsIndex.open(f, true);
			fail();
		} catch (GtfsIndexException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("checksum"));
		}
		assertFalse(GtfsIndex.open(f).verifyChecksum());
	}

	@Test
	public void badMagicTruncatedAndTinyFilesAreRejected() throws IOException {
		File magic = goodIndex();
		patch(magic, 0, 'X');
		File cut = goodIndex();
		try (RandomAccessFile raf = new RandomAccessFile(cut, "rw")) {
			raf.setLength(raf.length() - 40);
		}
		File tiny = tmp.newFile("tiny.bin");
		Files.write(tiny.toPath(), new byte[10]);
		File empty = tmp.newFile("empty.bin");
		for (File f : new File[] {magic, cut, tiny, empty, new File(tmp.getRoot(), "missing.bin")}) {
			try {
				GtfsIndex.open(f, true);
				fail(f.getName());
			} catch (GtfsIndexException e) {
				// expected
			} catch (java.nio.file.NoSuchFileException e) {
				assertEquals("missing.bin", f.getName());
			}
		}
	}

	// ------------------------------------------------------------------ atomic write

	@Test
	public void failedImportLeavesTheOldIndexUntouched() throws IOException {
		File out = goodIndex();
		byte[] before = Files.readAllBytes(out.toPath());
		File good = twoLines().write(tmp.newFolder(), "good.zip");
		GtfsFixture broken = twoLines();
		broken.files.remove("stops.txt");
		File bad = broken.write(tmp.newFolder(), "bad.zip");
		try {
			new GtfsFeedImporter(null).importFeeds(Arrays.asList(good, bad), out); // fails after the first feed is read
			fail();
		} catch (GtfsFeedImporter.ImportException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("stops.txt"));
		}
		assertArrayEquals(before, Files.readAllBytes(out.toPath()));
		assertEquals(Collections.singletonList("index.bin"), Arrays.asList(out.getParentFile().list()));
		assertEquals(4, GtfsIndex.open(out, true).getStopCount());
	}

	@Test
	public void cancelledImportLeavesTheOldIndexUntouched() throws IOException {
		File out = goodIndex();
		byte[] before = Files.readAllBytes(out.toPath());
		File zip = twoLines().write(tmp.newFolder(), "again.zip");
		GtfsProgressListener cancel = new GtfsProgressListener() {
			@Override
			public void onProgress(String stage, int percent) {
			}

			@Override
			public boolean isCancelled() {
				return true;
			}
		};
		try {
			new GtfsFeedImporter(cancel).importFeed(zip, out);
			fail();
		} catch (GtfsFeedImporter.ImportException e) {
			assertTrue(e.isCancelled());
		}
		assertArrayEquals(before, Files.readAllBytes(out.toPath()));
		assertEquals(1, out.getParentFile().list().length);
	}

	@Test
	public void freeSpaceHookCanRefuseTheImport() throws IOException {
		File out = goodIndex();
		byte[] before = Files.readAllBytes(out.toPath());
		File zip = twoLines().write(tmp.newFolder(), "again.zip");
		try {
			new GtfsFeedImporter(null).setFreeSpaceCheck((dir, bytes) -> {
				assertTrue(bytes > 0);
				throw new IOException("disk full");
			}).importFeed(zip, out);
			fail();
		} catch (IOException e) {
			assertEquals("disk full", e.getMessage());
		}
		assertArrayEquals(before, Files.readAllBytes(out.toPath()));
		assertEquals(1, out.getParentFile().list().length);
	}

	@Test
	public void staleTemporaryFilesFromAKilledImportAreRemoved() throws IOException {
		File out = new File(tmp.newFolder(), "index.bin");
		Files.write(new File(out.getParentFile(), "index.bin.tmp").toPath(), new byte[100]);
		Files.write(new File(out.getParentFile(), "index.bin.tmp.run0").toPath(), new byte[100]);
		importFeed(twoLines(), out);
		assertEquals(Collections.singletonList("index.bin"), Arrays.asList(out.getParentFile().list()));
	}
}
