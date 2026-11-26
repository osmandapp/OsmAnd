package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchPhraseCompatTest.unhex;
import static net.osmand.shared.compat.SearchPhraseCompatTest.unhexList;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore;

import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Timing of the spatial text search as the search of the app runs it, {@code SearchUICore.immediateSearch} of
 * {@code init(true)}, over the queries {@link SpatialTextSearchAPICompatTest} wrote to
 * {@code build/spatial-search-api-java.txt}: complete and still being typed, from the centers of the dump over all its
 * maps, by one core as the app keeps one, best of {@link SpatialTextSearchBenchmarkTest#ROUNDS} rounds after one to warm
 * up. {@code SpatialTextSearchAPIBenchmarkTest} of the commonTest of OsmAnd-shared times the copy over the same.
 * <p>
 * Behind {@code OSMAND_SEARCH_COMPAT}, see {@link SearchCompat}, and {@code OSMAND_SPATIAL_SEARCH_BENCHMARK}:
 * <pre>
 * OSMAND_SEARCH_COMPAT=1 OSMAND_SPATIAL_SEARCH_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*SpatialTextSearchAPIBenchmarkTest" --rerun -i
 * </pre>
 */
public class SpatialTextSearchAPIBenchmarkTest {

	/** The queries of a center of the dump. */
	private static class Run {
		final LatLon center;
		final List<String> texts = new ArrayList<>();

		Run(LatLon center) {
			this.center = center;
		}
	}

	@Test
	public void search() throws Exception {
		SearchCompat.assumeEnabled();
		Assume.assumeTrue("set OSMAND_SPATIAL_SEARCH_BENCHMARK to run", System.getenv("OSMAND_SPATIAL_SEARCH_BENCHMARK") != null);
		String path = System.getenv("OSMAND_SPATIAL_SEARCH_API_JAVA_DUMP");
		File dump = new File(path == null ? "build/spatial-search-api-java.txt" : path);
		Assume.assumeTrue(dump + " is missing: run SpatialTextSearchAPICompatTest first", dump.exists());
		Map<String, Map<String, String>> langPhrases = new HashMap<>();
		List<Run> runs = new ArrayList<>();
		List<String> maps = new ArrayList<>();
		String regionsPath = null;
		// line by line: the dump is hundreds of megabytes
		try (BufferedReader reader = Files.newBufferedReader(dump.toPath(), StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] f = line.split("\t");
				switch (f[0]) {
				case "G":
					regionsPath = unhex(f[1]);
					break;
				case "M":
					maps = unhexList(f[1]);
					break;
				case "Y":
					langPhrases.computeIfAbsent(f[1], k -> new HashMap<>()).put(unhex(f[2]), unhex(f[3]));
					break;
				case "C":
					runs.add(new Run(new LatLon(Double.longBitsToDouble(Long.parseUnsignedLong(f[1], 16)),
							Double.longBitsToDouble(Long.parseUnsignedLong(f[2], 16)))));
					break;
				case "Q":
					// the queries of the English search, by one core as the app keeps one
					if (f[2].equals("en")) {
						runs.get(runs.size() - 1).texts.add(unhex(f[1]));
					}
					break;
				default:
					break;
				}
			}
		}
		SearchApisCompatTest.setUpTypes();
		MapPoiTypes.getDefault().setPoiTranslator(new SearchApisCompatTest.JavaTranslator(SearchApisCompatTest.poiPhrases,
				langPhrases));
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		OsmandRegions regions = new OsmandRegions(regionsPath);
		try {
			for (String p : maps) {
				File f = new File(p);
				readers.add(new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
			}
			SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), "en", false);
			core.init(true);
			core.getSearchSettings().setOfflineIndexes(readers);
			core.getSearchSettings().setRegions(regions);
			int[] queries = new int[2];
			double[] best = {Double.MAX_VALUE, Double.MAX_VALUE};
			// what the core keeps after each round
			List<Long> heap = new ArrayList<>();
			Runtime runtime = Runtime.getRuntime();
			for (int round = 0; round <= SpatialTextSearchBenchmarkTest.ROUNDS; round++) {
				double[] ms = new double[2];
				for (Run run : runs) {
					for (String text : run.texts) {
						int kind = text.endsWith(" ") ? 0 : 1;
						long start = System.nanoTime();
						core.immediateSearch(text, run.center);
						ms[kind] += (System.nanoTime() - start) / 1e6;
						if (round == 0) {
							queries[kind]++;
						}
					}
				}
				if (round > 0) {
					best[0] = Math.min(best[0], ms[0]);
					best[1] = Math.min(best[1], ms[1]);
				}
				System.gc();
				heap.add((runtime.totalMemory() - runtime.freeMemory()) >> 20);
			}
			System.out.printf("SpatialTextSearchAPIBenchmarkTest on java: %d complete queries, %.1f ms; %d being typed, "
					+ "%.1f ms; best of %d rounds; heap after each round %s MB%n", queries[0], best[0], queries[1], best[1],
					SpatialTextSearchBenchmarkTest.ROUNDS, heap);
		} finally {
			for (BinaryMapIndexReader r : readers) {
				r.close();
			}
			regions.getReader().close();
			// the names of the poi types were this test's
			MapPoiTypes.setDefault(new MapPoiTypes("src/test/resources/poi_types.xml"));
			net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(
					new net.osmand.shared.osm.MapPoiTypes("src/test/resources/poi_types.xml"));
		}
	}
}
