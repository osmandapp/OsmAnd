package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchPhraseCompatTest.call;
import static net.osmand.shared.compat.SearchPhraseCompatTest.unhex;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
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
import java.util.Locale;
import java.util.Map;

/**
 * The java half of {@code SearchUICoreBenchmarkTest} in OsmAnd-shared: how long the core takes over
 * the phrases {@link SearchUICoreCompatTest} typed in, at the first two radius levels, for the cases of
 * {@code SearchUICoreTest} and the real maps it was given: to run the apis for a phrase, to keep what
 * they found sorted, united and without duplicates, and to merge it api by api as the ui shows it while
 * the search goes on. The phrases and the maps come from its dump, which this reads; each case gets a
 * core of its own every round.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_SEARCH_CORE_BENCHMARK} is
 * set, and measures java alone, in a jvm of its own; the copy is measured by the shared half.
 * <pre>
 * OSMAND_SEARCH_CORE_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*SearchUICoreBenchmarkTest" --rerun -i
 * </pre>
 */
public class SearchUICoreBenchmarkTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");
	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final File DUMP = new File("build/search-core-java.txt");
	private static final File MAPS = new File("build/search-obf");
	private static final int WARMUP_ROUNDS = 2;
	private static final int MEASURED_ROUNDS = 5;
	private static final String[] KINDS = {"apis run for each phrase", "results kept", "results merged api by api"};

	/** A case of the dump: its settings, files and the phrases typed in it. */
	private static final class Case {
		String settings;
		boolean poiTypes;
		final List<String> files = new ArrayList<>();
		final List<String> texts = new ArrayList<>();
		final List<Integer> radii = new ArrayList<>();
	}

	@Test
	public void benchmarkSearchUICore() throws Exception {
		Assume.assumeTrue("set OSMAND_SEARCH_CORE_BENCHMARK to run", System.getenv("OSMAND_SEARCH_CORE_BENCHMARK") != null);
		Assume.assumeTrue("run SearchUICoreCompatTest first", DUMP.exists());
		PlatformUtil.setOsmandRegions(new OsmandRegions(REGIONS.getPath()));
		Map<String, String> phrases = SearchApisCompatTest.poiPhrases;
		List<Case> cases = new ArrayList<>();
		try (BufferedReader in = Files.newBufferedReader(DUMP.toPath(), StandardCharsets.UTF_8)) {
			Case c = null;
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				String[] f = line.substring(2).split("\t");
				switch (line.charAt(0)) {
					case 'X' -> phrases.put(unhex(f[0]), unhex(f[1]));
					case 'C' -> {
						c = new Case();
						c.settings = unhex(f[2]);
						if (!f[3].equals("-")) {
							for (String h : f[3].split(",")) {
								c.files.add(unhex(h));
							}
						}
						c.poiTypes = f[4].equals("true");
						cases.add(c);
					}
					case 'Q' -> {
						if (f[2].equals("T")) {
							c.texts.add(unhex(f[4]));
							c.radii.add(Integer.parseInt(f[5]));
						}
					}
					default -> {
					}
				}
			}
		}
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		MapPoiTypes.getDefault().setPoiTranslator(new SearchApisCompatTest.JavaTranslator());
		Map<String, BinaryMapIndexReader> readers = new HashMap<>();
		for (Case c : cases) {
			for (String name : c.files) {
				if (!readers.containsKey(name)) {
					File f = name.startsWith("/") ? new File(name) : new File(MAPS, name);
					readers.put(name, new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
				}
			}
		}
		double[] best = new double[KINDS.length];
		java.util.Arrays.fill(best, Double.MAX_VALUE);
		long[] counts = new long[KINDS.length];
		try {
			for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
				long[] nanos = new long[KINDS.length];
				long[] found = new long[KINDS.length];
				for (Case c : cases) {
					SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = c.poiTypes;
					SearchSettings settings = SearchSettings.parseJSON(new JSONObject(c.settings));
					List<BinaryMapIndexReader> files = new ArrayList<>();
					for (String name : c.files) {
						files.add(readers.get(name));
					}
					settings.setOfflineIndexes(files);
					SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), "en", false, () -> false);
					core.init();
					for (int i = 0; i < c.texts.size(); i++) {
						SearchSettings s = c.radii.get(i) == 1 ? settings : settings.setRadiusLevel(c.radii.get(i));
						SearchPhrase phrase = SearchPhrase.emptyPhrase(s).generateNewPhrase(c.texts.get(i), s);
						List<List<Object>> byApi = new ArrayList<>();
						ResultMatcher<SearchResult> recorder = new ResultMatcher<SearchResult>() {
							@Override
							public boolean publish(SearchResult r) {
								if (r.objectType == ObjectType.SEARCH_API_FINISHED) {
									byApi.add(new ArrayList<>());
								} else if (!String.valueOf(r.objectType).startsWith("SEARCH_")) {
									if (byApi.isEmpty()) {
										byApi.add(new ArrayList<>());
									}
									byApi.get(byApi.size() - 1).add(r);
								}
								return true;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						};
						long start = System.nanoTime();
						SearchUICore.SearchResultMatcher matcher = new SearchUICore.SearchResultMatcher(recorder, phrase, 0,
								new java.util.concurrent.atomic.AtomicInteger(0), -1);
						call(core, "searchInternal", phrase, matcher);
						nanos[0] += System.nanoTime() - start;
						found[0] += matcher.getCount();

						List<Object> all = SearchUICoreCompatTest.copies(new ArrayList<>(matcher.getRequestResults()));
						List<List<Object>> apis = new ArrayList<>();
						for (List<Object> api : byApi) {
							apis.add(SearchUICoreCompatTest.copies(api));
						}
						start = System.nanoTime();
						SearchResultCollection kept = new SearchResultCollection(phrase).addSearchResults(results(all), true, true);
						nanos[1] += System.nanoTime() - start;
						found[1] += kept.getCurrentSearchResults().size();

						start = System.nanoTime();
						SearchResultCollection merged = new SearchResultCollection(phrase);
						for (List<Object> api : apis) {
							merged = merged.combineWithCollection(new SearchResultCollection(phrase).addSearchResults(results(api), true, true), true, true);
						}
						nanos[2] += System.nanoTime() - start;
						found[2] += merged.getCurrentSearchResults().size();
					}
				}
				if (round >= WARMUP_ROUNDS) {
					for (int k = 0; k < KINDS.length; k++) {
						best[k] = Math.min(best[k], nanos[k] / 1e6);
						counts[k] = found[k];
					}
				}
			}
		} finally {
			SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false;
			for (BinaryMapIndexReader r : readers.values()) {
				r.close();
			}
			MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));
		}
		int phraseCount = 0;
		for (Case c : cases) {
			phraseCount += c.texts.size();
		}
		System.out.println();
		System.out.println("### search core, java, " + cases.size() + " cases, " + phraseCount + " phrases, best of " + MEASURED_ROUNDS);
		double total = 0;
		for (int k = 0; k < KINDS.length; k++) {
			System.out.println(String.format(Locale.US, "  %-40s %9.1f %9d", KINDS[k], best[k], counts[k]));
			total += best[k];
		}
		System.out.println(String.format(Locale.US, "  %-40s %9.1f %9d", "all", total, 0));
	}

	@SuppressWarnings("unchecked")
	private static List<SearchResult> results(List<Object> l) {
		return (List<SearchResult>) (List<?>) l;
	}
}
