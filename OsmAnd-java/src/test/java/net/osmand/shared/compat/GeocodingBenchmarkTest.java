package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchPhraseCompatTest.unbits;
import static net.osmand.shared.compat.SearchPhraseCompatTest.unhex;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.router.RoutingContext;

import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The java half of {@code GeocodingBenchmarkTest} in OsmAnd-shared: how long {@code GeocodingUtilities}
 * takes at the points {@link GeocodingCompatTest} geocoded, in the default context of each file - to
 * find the roads near each point, and to find the addresses on them and sort them, as the search tests
 * do. The files and the points come from its dump.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_GEOCODING_BENCHMARK} is
 * set, and measures java alone, in a jvm of its own; the copy is measured by the shared half.
 * <pre>
 * OSMAND_GEOCODING_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*GeocodingBenchmarkTest" --rerun -i
 * </pre>
 */
public class GeocodingBenchmarkTest {

	private static final File DUMP = new File("build/geocoding-java.txt");
	private static final int WARMUP_ROUNDS = 2;
	private static final int MEASURED_ROUNDS = 5;
	private static final String[] KINDS = {"roads near a point", "addresses of the roads sorted"};

	@Test
	public void benchmarkGeocoding() throws Exception {
		Assume.assumeTrue("set OSMAND_GEOCODING_BENCHMARK to run", System.getenv("OSMAND_GEOCODING_BENCHMARK") != null);
		Assume.assumeTrue("run GeocodingCompatTest first", DUMP.exists());
		GeocodingCompatTest.setUp();
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		List<RoutingContext> contexts = new ArrayList<>();
		List<Integer> fileOf = new ArrayList<>();
		List<double[]> points = new ArrayList<>();
		try (BufferedReader in = Files.newBufferedReader(DUMP.toPath(), StandardCharsets.UTF_8)) {
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				if (line.startsWith("F\t")) {
					File file = new File(unhex(line.split("\t")[2]));
					BinaryMapIndexReader reader = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
					readers.add(reader);
					contexts.add(GeocodingUtilities.buildDefaultContextForPOI(reader));
				} else if (line.startsWith("P\t")) {
					String[] f = line.split("\t", 6);
					if (f[2].equals("0")) {
						fileOf.add(readers.size() - 1);
						points.add(new double[] {unbits(f[3]), unbits(f[4])});
					}
				}
			}
		}
		GeocodingUtilities utils = new GeocodingUtilities();
		double[] best = new double[KINDS.length];
		java.util.Arrays.fill(best, Double.MAX_VALUE);
		long[] counts = new long[KINDS.length];
		try {
			for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
				long[] nanos = new long[KINDS.length];
				long[] made = new long[KINDS.length];
				long start = System.nanoTime();
				for (int i = 0; i < points.size(); i++) {
					made[0] += utils.reverseGeocodingSearch(contexts.get(fileOf.get(i)), points.get(i)[0], points.get(i)[1], false).size();
				}
				nanos[0] = System.nanoTime() - start;
				// sorting changes the results it sorts: fresh ones, found before the clock starts
				List<List<GeocodingResult>> found = new ArrayList<>();
				for (int i = 0; i < points.size(); i++) {
					found.add(utils.reverseGeocodingSearch(contexts.get(fileOf.get(i)), points.get(i)[0], points.get(i)[1], false));
				}
				start = System.nanoTime();
				for (int i = 0; i < points.size(); i++) {
					made[1] += utils.sortGeocodingResults(List.of(readers.get(fileOf.get(i))), found.get(i)).size();
				}
				nanos[1] = System.nanoTime() - start;
				if (round >= WARMUP_ROUNDS) {
					for (int k = 0; k < KINDS.length; k++) {
						best[k] = Math.min(best[k], nanos[k] / 1e6);
						counts[k] = made[k];
					}
				}
			}
		} finally {
			// the default contexts open the file once more each
			for (RoutingContext c : contexts) {
				for (BinaryMapIndexReader r : c.map.keySet()) {
					r.close();
				}
			}
			for (BinaryMapIndexReader r : readers) {
				r.close();
			}
			GeocodingCompatTest.close();
		}
		System.out.println();
		System.out.println("### geocoding, java, " + readers.size() + " files, " + points.size() + " points, best of " + MEASURED_ROUNDS);
		double total = 0;
		for (int k = 0; k < KINDS.length; k++) {
			System.out.println(String.format(Locale.US, "  %-40s %9.1f %9d", KINDS[k], best[k], counts[k]));
			total += best[k];
		}
		System.out.println(String.format(Locale.US, "  %-40s %9.1f %9d", "all", total, 0));
	}
}
