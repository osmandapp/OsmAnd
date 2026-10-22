package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchPhraseCompatTest.unhex;
import static net.osmand.shared.compat.SearchPhraseCompatTest.unhexList;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.core.spatial.SpatialPoiSearch;
import net.osmand.search.core.spatial.SpatialSearchContext;
import net.osmand.search.core.spatial.SpatialTextSearch;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Timing of the spatial text search, {@code SpatialTextSearch.searchAPI}, over the queries
 * {@link SpatialTextSearchCompatTest} wrote to {@code build/spatial-search-java.txt}: those of the default settings
 * and of the old intersections, over the maps and from the centers of the dump, with the names of the languages it
 * added, by one {@code SpatialTextSearch} as the app keeps one, best of {@link #ROUNDS} rounds after one to warm up.
 * {@code SpatialTextSearchBenchmarkTest} of the commonTest of OsmAnd-shared times the copy over the same.
 * <p>
 * Behind {@code OSMAND_SEARCH_COMPAT}, see {@link SearchCompat}, and {@code OSMAND_SPATIAL_SEARCH_BENCHMARK}:
 * <pre>
 * OSMAND_SEARCH_COMPAT=1 OSMAND_SPATIAL_SEARCH_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*SpatialTextSearchBenchmarkTest" --rerun -i
 * </pre>
 */
public class SpatialTextSearchBenchmarkTest {

	static final int ROUNDS = 5;

	/** A map or the maps of a part of the dump: where it is searched from, the languages added before, the queries. */
	private static class Run {
		final List<String> paths;
		final LatLon center;
		final List<String> languages;
		final List<Integer> variants = new ArrayList<>();
		final List<String> phrases = new ArrayList<>();

		Run(List<String> paths, LatLon center, List<String> languages) {
			this.paths = paths;
			this.center = center;
			this.languages = languages;
		}
	}

	@Test
	public void search() throws Exception {
		SearchCompat.assumeEnabled();
		Assume.assumeTrue("set OSMAND_SPATIAL_SEARCH_BENCHMARK to run", System.getenv("OSMAND_SPATIAL_SEARCH_BENCHMARK") != null);
		String path = System.getenv("OSMAND_SPATIAL_SEARCH_JAVA_DUMP");
		File dump = new File(path == null ? "build/spatial-search-java.txt" : path);
		Assume.assumeTrue(dump + " is missing: run SpatialTextSearchCompatTest first", dump.exists());
		Map<String, Map<String, String>> langPhrases = new HashMap<>();
		List<Run> runs = new ArrayList<>();
		List<String> languages = new ArrayList<>();
		for (String line : Files.readAllLines(dump.toPath(), StandardCharsets.UTF_8)) {
			String[] f = line.split("\t");
			switch (f[0]) {
			case "Y":
				langPhrases.computeIfAbsent(f[1], k -> new HashMap<>()).put(unhex(f[2]), unhex(f[3]));
				break;
			case "L":
				languages.add(f[1]);
				break;
			case "C":
				runs.add(new Run(unhexList(f[1]), new LatLon(Double.longBitsToDouble(Long.parseUnsignedLong(f[2], 16)),
						Double.longBitsToDouble(Long.parseUnsignedLong(f[3], 16))), languages));
				languages = new ArrayList<>();
				break;
			case "Q":
				int variant = Integer.parseInt(f[1]);
				if (variant == 0 || variant == 3) {
					runs.get(runs.size() - 1).variants.add(variant);
					runs.get(runs.size() - 1).phrases.add(unhex(f[2]));
				}
				break;
			default:
				break;
			}
		}
		SearchApisCompatTest.setUpTypes();
		MapPoiTypes.getDefault().setPoiTranslator(new SearchApisCompatTest.JavaTranslator(SearchApisCompatTest.poiPhrases,
				langPhrases));
		try {
			SpatialPoiSearch poiSearch = new SpatialPoiSearch(MapPoiTypes.getDefault());
			SpatialTextSearch search = new SpatialTextSearch();
			int[] queries = new int[2];
			double[] best = {Double.MAX_VALUE, Double.MAX_VALUE};
			for (int round = 0; round <= ROUNDS; round++) {
				double[] ms = new double[2];
				for (Run run : runs) {
					for (String lang : run.languages) {
						poiSearch.addLanguage(lang);
					}
					List<BinaryMapIndexReader> readers = new ArrayList<>();
					try {
						for (String p : run.paths) {
							File f = new File(p);
							readers.add(new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
						}
						for (int i = 0; i < run.phrases.size(); i++) {
							int v = run.variants.get(i);
							SpatialSearchContext ctx = new SpatialSearchContext(SpatialTextSearchCompatTest.settings(v), readers,
									poiSearch, run.center);
							ctx.getStats().printLogs = false;
							long start = System.nanoTime();
							search.searchAPI(run.phrases.get(i), ctx);
							ms[v == 0 ? 0 : 1] += (System.nanoTime() - start) / 1e6;
							if (round == 0) {
								queries[v == 0 ? 0 : 1]++;
							}
						}
					} finally {
						for (BinaryMapIndexReader r : readers) {
							r.close();
						}
					}
				}
				if (round > 0) {
					best[0] = Math.min(best[0], ms[0]);
					best[1] = Math.min(best[1], ms[1]);
				}
			}
			System.out.printf("SpatialTextSearchBenchmarkTest on java: %d queries by default, %.1f ms; %d by the old "
					+ "intersections, %.1f ms; best of %d rounds%n", queries[0], best[0], queries[1], best[1], ROUNDS);
		} finally {
			// the names of the poi types were this test's
			MapPoiTypes.setDefault(new MapPoiTypes("src/test/resources/poi_types.xml"));
			net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(
					new net.osmand.shared.osm.MapPoiTypes("src/test/resources/poi_types.xml"));
		}
	}
}
