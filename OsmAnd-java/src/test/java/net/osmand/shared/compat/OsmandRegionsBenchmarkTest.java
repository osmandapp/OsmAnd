package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.shared.data.KLatLon;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Timing of {@link net.osmand.shared.map.OsmandRegions} against {@link OsmandRegions}: reading
 * {@code regions.ocbf} into the tree, which an app does once at start, and the two lookups by point
 * the search and the routing make, the smallest region under a point and the regions to download
 * there.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_REGIONS_BENCHMARK} is
 * set. It asserts only that both sides found the same number of regions, so a slow machine never
 * turns the build red; what they find is compared in {@link OsmandRegionsCompatTest}.
 * <pre>
 * OSMAND_REGIONS_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*OsmandRegionsBenchmarkTest" --rerun -i
 * </pre>
 * On the {@code regions.ocbf} of 24.09.2026, 7 MB with 1 724 regions, best of five in ms; the native
 * column is the cross platform half in OsmAnd-shared, {@code OsmandRegionsBenchmarkTest.kt}, on the
 * release simulator binary:
 * <pre>
 * what                               java    copy  native   count
 * read the file into the tree        46.8    44.1   143.1    1724
 * smallest region, 500 points       406.3   284.4   714.8     498
 * regions to download, 500 points   406.4   283.2   712.3    1027
 * </pre>
 * The copy on the jvm takes 0.70 to 0.94 of java's time. Kotlin/Native reads the file at 3.2 times
 * the copy on the jvm and looks a point up in 1.4 ms, 2.5 times. A lookup decodes the polygons of
 * every region whose box holds the point, and most of that time is in
 * {@code CodedInputStream.readRawVarint32}, which every section of the shared reader reads through.
 */
public class OsmandRegionsBenchmarkTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");
	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;
	private static final int POINTS = 500;

	@Test
	public void readingRegions() throws IOException {
		Assume.assumeTrue("set OSMAND_REGIONS_BENCHMARK to run", System.getenv("OSMAND_REGIONS_BENCHMARK") != null);
		long[] best = new long[6];
		java.util.Arrays.fill(best, Long.MAX_VALUE);
		int regions = 0;
		int smallest = 0;
		int toDownload = 0;
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long started = System.nanoTime();
			OsmandRegions java = new OsmandRegions(REGIONS.getPath());
			long javaOpen = System.nanoTime() - started;
			started = System.nanoTime();
			net.osmand.shared.map.OsmandRegions copy = new net.osmand.shared.map.OsmandRegions(REGIONS.getPath());
			long copyOpen = System.nanoTime() - started;
			assertEquals("regions", java.getAllRegionData().size(), copy.getAllRegionData().size());

			List<LatLon> points = points(java);
			started = System.nanoTime();
			int jsmallest = 0;
			for (LatLon ll : points) {
				if (java.getSmallestBinaryMapDataObjectAt(ll) != null) {
					jsmallest++;
				}
			}
			long javaSmallest = System.nanoTime() - started;
			started = System.nanoTime();
			int ksmallest = 0;
			for (LatLon ll : points) {
				if (copy.getSmallestBinaryMapDataObjectAt(new KLatLon(ll.getLatitude(), ll.getLongitude())) != null) {
					ksmallest++;
				}
			}
			long copySmallest = System.nanoTime() - started;
			assertEquals("smallest", jsmallest, ksmallest);

			started = System.nanoTime();
			int jdownload = 0;
			for (LatLon ll : points) {
				jdownload += java.getRegionsToDownload(ll.getLatitude(), ll.getLongitude()).size();
			}
			long javaDownload = System.nanoTime() - started;
			started = System.nanoTime();
			int kdownload = 0;
			for (LatLon ll : points) {
				kdownload += copy.getRegionsToDownload(ll.getLatitude(), ll.getLongitude()).size();
			}
			long copyDownload = System.nanoTime() - started;
			assertEquals("to download", jdownload, kdownload);

			java.close();
			copy.close();
			if (round >= WARMUP_ROUNDS) {
				long[] times = {javaOpen, copyOpen, javaSmallest, copySmallest, javaDownload, copyDownload};
				for (int i = 0; i < times.length; i++) {
					best[i] = Math.min(best[i], times[i]);
				}
				regions = java.getAllRegionData().size();
				smallest = jsmallest;
				toDownload = jdownload;
			}
		}
		System.out.println();
		System.out.println("### " + REGIONS.getName() + ", " + (REGIONS.length() >> 20) + " MB, best of " + MEASURED_ROUNDS);
		System.out.printf("%-34s %10s %10s %6s %9s%n", "what", "java", "copy", "x", "count");
		row("read the file into the tree", best[0], best[1], regions);
		row("smallest region, " + POINTS + " points", best[2], best[3], smallest);
		row("regions to download, " + POINTS + " points", best[4], best[5], toDownload);
	}

	private static void row(String what, long java, long copy, int count) {
		System.out.printf("%-34s %7.1f ms %7.1f ms %6.2f %9d%n", what, java / 1e6, copy / 1e6, (double) copy / java, count);
	}

	/** The centres of the first regions of the tree, which are on land and inside some region. */
	private static List<LatLon> points(OsmandRegions regions) {
		List<WorldRegion> tree = new ArrayList<>();
		collect(regions.getWorldRegion(), tree);
		List<LatLon> points = new ArrayList<>();
		for (WorldRegion r : tree) {
			if (r.getRegionCenter() != null && points.size() < POINTS) {
				points.add(r.getRegionCenter());
			}
		}
		return points;
	}

	private static void collect(WorldRegion r, List<WorldRegion> result) {
		result.add(r);
		for (WorldRegion s : r.getSubregions()) {
			collect(s, result);
		}
	}
}
