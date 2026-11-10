package net.osmand.shared.compat;

import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;

import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The java half of {@code SearchPhraseBenchmarkTest} in OsmAnd-shared: how long {@link SearchPhrase}
 * takes to read what is typed, which the search does on every key; to count its words in results
 * and weigh them, which it does for every result it sorts; and to select a result and read on. The
 * settings, phrases and results are the ones {@link SearchPhraseCompatTest} writes to its dump,
 * which this reads; the results are made before each round, as the search makes them elsewhere.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_SEARCH_PHRASE_BENCHMARK}
 * is set, and measures java alone, in a jvm of its own; the copy is measured by the shared half.
 * <pre>
 * OSMAND_SEARCH_PHRASE_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*SearchPhraseBenchmarkTest" --rerun -i
 * </pre>
 */
public class SearchPhraseBenchmarkTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");
	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final File DUMP = new File("build/search-phrase-java.txt");
	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;

	@Test
	public void benchmarkSearchPhrase() throws Exception {
		Assume.assumeTrue("set OSMAND_SEARCH_PHRASE_BENCHMARK to run", System.getenv("OSMAND_SEARCH_PHRASE_BENCHMARK") != null);
		Assume.assumeTrue("run SearchPhraseCompatTest first", DUMP.exists());
		PlatformUtil.setOsmandRegions(new OsmandRegions(REGIONS.getPath()));
		MapPoiTypes.setDefault(new MapPoiTypes(POI_TYPES));

		Map<Integer, SearchSettings> settings = new HashMap<>();
		Map<Integer, SearchSettings> phraseSettings = new HashMap<>();
		Map<Integer, String> texts = new HashMap<>();
		List<Integer> phrases = new ArrayList<>();
		List<Integer> resultPhrases = new ArrayList<>();
		List<String> resultSpecs = new ArrayList<>();
		List<Integer> selectPhrases = new ArrayList<>();
		List<String> selectSpecs = new ArrayList<>();
		for (String line : Files.readAllLines(DUMP.toPath(), StandardCharsets.UTF_8)) {
			String[] f = line.split("\t");
			switch (line.charAt(0)) {
				case 'S':
					settings.put(Integer.parseInt(f[0].substring(2)), SearchSettings.parseJSON(new JSONObject(SearchPhraseCompatTest.unhex(f[1]))));
					break;
				case 'P':
					int p = Integer.parseInt(f[0].substring(2));
					phrases.add(p);
					phraseSettings.put(p, settings.get(Integer.parseInt(f[1])));
					texts.put(p, SearchPhraseCompatTest.unhex(f[2]));
					break;
				case 'R':
					resultPhrases.add(Integer.parseInt(f[0].substring(2)));
					resultSpecs.add(f[1]);
					break;
				case 'W':
					selectPhrases.add(Integer.parseInt(f[0].substring(2)));
					selectSpecs.add(f[1]);
					break;
				default:
			}
		}

		double[] best = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
		int[] counts = new int[3];
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long started = System.nanoTime();
			Map<Integer, SearchPhrase> typed = new HashMap<>();
			int words = 0;
			for (int p : phrases) {
				SearchSettings s = phraseSettings.get(p);
				SearchPhrase phrase = SearchPhrase.emptyPhrase(s).generateNewPhrase(texts.get(p), s);
				words += phrase.getUnknownWordToSearch().length() + phrase.getUnknownSearchWords().size();
				phrase.getMainUnknownNameStringMatcher();
				typed.put(p, phrase);
			}
			double type = (System.nanoTime() - started) / 1e6;

			List<SearchResult> results = new ArrayList<>();
			for (int i = 0; i < resultSpecs.size(); i++) {
				results.add(SearchPhraseCompatTest.buildJava(resultSpecs.get(i), typed.get(resultPhrases.get(i))));
			}
			started = System.nanoTime();
			int matched = 0;
			for (int i = 0; i < results.size(); i++) {
				SearchResult r = results.get(i);
				matched += typed.get(resultPhrases.get(i)).countUnknownWordsMatchMainResult(r);
				r.getUnknownPhraseMatchWeight();
				r.filterUnknownSearchWord(null);
			}
			double weigh = (System.nanoTime() - started) / 1e6;

			List<SearchResult> selected = new ArrayList<>();
			for (int i = 0; i < selectSpecs.size(); i++) {
				selected.add(SearchPhraseCompatTest.buildJava(selectSpecs.get(i), typed.get(selectPhrases.get(i))));
			}
			started = System.nanoTime();
			int selections = 0;
			for (int i = 0; i < selected.size(); i++) {
				SearchPhrase phrase = typed.get(selectPhrases.get(i));
				SearchPhrase next = phrase.selectWord(selected.get(i), phrase.getSettings());
				next = next.generateNewPhrase(next.getText(true) + "12 ", phrase.getSettings());
				selections += next.getWords().size();
			}
			double select = (System.nanoTime() - started) / 1e6;

			if (round >= WARMUP_ROUNDS) {
				double[] times = {type, weigh, select};
				for (int i = 0; i < times.length; i++) {
					best[i] = Math.min(best[i], times[i]);
				}
				counts = new int[] {words, matched, selections};
			}
		}
		System.out.println();
		System.out.println("### search phrase, java, best of " + MEASURED_ROUNDS);
		row("type " + phrases.size() + " phrases", best[0], counts[0]);
		row("weigh " + resultSpecs.size() + " results", best[1], counts[1]);
		row("select " + selectSpecs.size() + " results", best[2], counts[2]);
	}

	private static void row(String what, double ms, int count) {
		System.out.println(String.format(java.util.Locale.US, "  %-34s %9.1f %9d", what, ms, count));
	}
}
