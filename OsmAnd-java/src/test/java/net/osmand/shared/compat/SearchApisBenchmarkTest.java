package net.osmand.shared.compat;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.SearchCoreAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchSettings;

import org.json.JSONObject;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The java half of {@code SearchApisBenchmarkTest} in OsmAnd-shared: how long each search api of
 * {@code SearchCoreFactory} takes over the phrases {@link SearchApisCompatTest} typed in, at the first
 * two radius levels, for the cases of {@code SearchUICoreTest} and the real maps it was given. The
 * phrases and the maps come from its dump, which this reads; an api searches only when the core
 * would have it search, and publishes to a matcher that keeps what it gets, as the core's does.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_SEARCH_APIS_BENCHMARK}
 * is set, and measures java alone, in a jvm of its own; the copy is measured by the shared half.
 * <pre>
 * OSMAND_SEARCH_APIS_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*SearchApisBenchmarkTest" --rerun -i
 * </pre>
 */
public class SearchApisBenchmarkTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");
	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final File DUMP = new File("build/search-apis-java.txt");
	private static final File MAPS = new File("build/search-obf");
	private static final int WARMUP_ROUNDS = 2;
	private static final int MEASURED_ROUNDS = 5;

	/** A case of the dump: its settings, files and the phrases typed in it. */
	private static final class Case {
		String settings;
		final List<String> files = new ArrayList<>();
		final List<String> texts = new ArrayList<>();
		final List<Integer> radii = new ArrayList<>();
	}

	@Test
	public void benchmarkSearchApis() throws Exception {
		Assume.assumeTrue("set OSMAND_SEARCH_APIS_BENCHMARK to run", System.getenv("OSMAND_SEARCH_APIS_BENCHMARK") != null);
		Assume.assumeTrue("run SearchApisCompatTest first", DUMP.exists());
		PlatformUtil.setOsmandRegions(new OsmandRegions(REGIONS.getPath()));
		net.osmand.shared.binary.CommonWords.Companion.setOsmandRegions(
				new net.osmand.shared.map.OsmandRegions(REGIONS.getPath()));
		List<Case> cases = new ArrayList<>();
		try (BufferedReader in = Files.newBufferedReader(DUMP.toPath(), StandardCharsets.UTF_8)) {
			Case c = null;
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				String[] f = line.split("\t");
				if (line.startsWith("X ")) {
					SearchApisCompatTest.poiPhrases.put(SearchPhraseCompatTest.unhex(f[0].substring(2)), SearchPhraseCompatTest.unhex(f[1]));
				} else if (line.startsWith("C ") && !line.startsWith("C -1")) {
					c = new Case();
					c.settings = SearchPhraseCompatTest.unhex(f[2]);
					c.files.addAll(SearchPhraseCompatTest.unhexList(f[3]));
					cases.add(c);
				} else if (line.startsWith("C -1")) {
					c = null;
				} else if (line.startsWith("Q ") && c != null && f[2].equals("T")) {
					c.texts.add(SearchPhraseCompatTest.unhex(f[5]));
					c.radii.add(Integer.parseInt(f[6]));
				}
			}
		}
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		MapPoiTypes.getDefault().setPoiTranslator(new SearchApisCompatTest.JavaTranslator());
		Map<String, BinaryMapIndexReader> readers = new HashMap<>();
		int phrases = 0;
		for (Case c : cases) {
			for (String name : c.files) {
				File f = name.startsWith("/") ? new File(name) : new File(MAPS, name);
				readers.computeIfAbsent(name, n -> {
					try {
						return new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f);
					} catch (Exception e) {
						throw new AssertionError(e);
					}
				});
			}
			phrases += c.texts.size();
		}

		String[] names = SearchApisCompatTest.APIS;
		double[] best = new double[names.length];
		long[] counts = new long[names.length];
		java.util.Arrays.fill(best, Double.MAX_VALUE);
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long[] nanos = new long[names.length];
			long[] published = new long[names.length];
			for (Case c : cases) {
				SearchSettings settings = SearchSettings.parseJSON(new JSONObject(c.settings));
				List<BinaryMapIndexReader> files = new ArrayList<>();
				for (String name : c.files) {
					files.add(readers.get(name));
				}
				settings.setOfflineIndexes(files);
				List<Object> apis = SearchApisCompatTest.javaApis();
				for (int i = 0; i < c.texts.size(); i++) {
					SearchSettings s = c.radii.get(i) == 1 ? settings : settings.setRadiusLevel(c.radii.get(i));
					SearchPhrase phrase = SearchPhrase.emptyPhrase(s).generateNewPhrase(c.texts.get(i), s);
					phrase.sortFiles();
					for (int a = 0; a < apis.size(); a++) {
						SearchCoreAPI api = (SearchCoreAPI) apis.get(a);
						long start = System.nanoTime();
						if (api.isSearchAvailable(phrase) && api.getSearchPriority(phrase) != -1) {
							SearchResultMatcher matcher = new SearchResultMatcher(null, phrase, 0, new AtomicInteger(0), -1);
							try {
								api.search(phrase, matcher);
							} catch (Exception e) {
								// the core goes on to the next api
							}
							published[a] += matcher.getCount();
						}
						nanos[a] += System.nanoTime() - start;
					}
				}
			}
			if (round >= WARMUP_ROUNDS) {
				for (int a = 0; a < names.length; a++) {
					best[a] = Math.min(best[a], nanos[a] / 1e6);
					counts[a] = published[a];
				}
			}
		}
		for (BinaryMapIndexReader r : readers.values()) {
			r.close();
		}
		System.out.println();
		System.out.println("### search apis, java, " + cases.size() + " cases, " + phrases + " phrases, best of " + MEASURED_ROUNDS);
		double total = 0;
		for (int a = 0; a < names.length; a++) {
			row(names[a] + (a == 7 ? " (far)" : ""), best[a], counts[a]);
			total += best[a];
		}
		row("all", total, 0);
	}

	private static void row(String what, double ms, long count) {
		System.out.println(String.format(java.util.Locale.US, "  %-48s %9.1f %9d", what, ms, count));
	}
}
