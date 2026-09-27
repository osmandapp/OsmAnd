package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchApisCompatTest.resultLine;
import static net.osmand.shared.compat.SearchApisCompatTest.v;
import static net.osmand.shared.compat.SearchPhraseCompatTest.call;
import static net.osmand.shared.compat.SearchPhraseCompatTest.field;
import static net.osmand.shared.compat.SearchPhraseCompatTest.hex;
import static net.osmand.shared.compat.SearchPhraseCompatTest.hexList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchWord;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The core of the search, {@code SearchUICore} in OsmAnd-shared, against the original in OsmAnd-java:
 * what it makes of what the apis find.
 *
 * The cases of {@code SearchUICoreTest} are searched as {@code SearchApisCompatTest} searches them for
 * the apis on their own: the first phrase of a case typed in, every phrase in whole at the first two
 * radius levels, and the first result of each type selected, twice over; the phrases of poi types as
 * {@code SearchUICoreTest} makes them. The core runs the apis for each phrase. What they publish goes
 * into the results of both sides in one order, by what the results are, so that the order in which a
 * reader hands out pois at one distance, which java takes from a hash map, does not count. Then both
 * sides make of them:
 * <ul>
 * <li>the results as the core keeps them: sorted, united by osm id or wikidata, without duplicates;</li>
 * <li>the results as the ui shows them while the search goes on, merged api by api, sorted again as
 * android does and without sorting them again;</li>
 * <li>the results sorted with the duplicates kept.</li>
 * </ul>
 * Each list is compared with all the fields of its results, the address given to each poi and the text
 * the tests of the search compare; with them, which neighbours are the same place, and what the core
 * answers about searching further.
 *
 * {@link #coreAnswersTheSame} then sends phrases through the core as the ui does: searched at once,
 * selected, cleared, one after another with each cancelling the one before, and to one api.
 *
 * It writes the phrases, with what java made of them, to {@code build/search-core-java.txt}, which
 * {@code SearchUICoreTest} in OsmAnd-shared holds the copy to on Kotlin/Native, with the collation keys
 * of the words java compared, and the cases of {@code SearchUICoreTest} with what they expect. It takes
 * {@code OSMAND_SEARCH_APIS_CASE} and {@code OSMAND_SEARCH_APIS_MAPS} as {@code SearchApisCompatTest}
 * does.
 */
public class SearchUICoreCompatTest {

	/** Read by {@code SearchUICoreTest} in OsmAnd-shared, from {@code ../OsmAnd-java/build}. */
	private static final File JAVA_DUMP = new File("build/search-core-java.txt");
	/** How far apart two results can be for the dump to say whether they are the same place. */
	private static final int SAME_DEPTH = 3;

	private static java.io.Writer out;
	/** Whether the results of the phrase being run go into the dump in full, not only their order. */
	private static boolean dumping = true;
	private static List<String> caseLines;
	private static final Map<String, BinaryMapIndexReader> javaReaders = new LinkedHashMap<>();
	private static final Map<String, net.osmand.shared.binary.BinaryMapIndexReader> copyReaders = new LinkedHashMap<>();
	private static final List<SearchApisCompatTest.Case> cases = new ArrayList<>();
	private static int phrasesRun;
	private static int resultsCompared;
	private static int selections;
	private static int united;
	private static int sameTies;
	private static int otherDuplicates;

	@BeforeClass
	public static void open() throws Exception {
		SearchApisCompatTest.setUpTypes();
		for (File f : TestObf.searchFiles()) {
			javaReaders.put(f.getName(), new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f));
			copyReaders.put(f.getName(), new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath()));
		}
		cases.addAll(SearchApisCompatTest.loadCases(javaReaders.keySet()));
	}

	@AfterClass
	public static void close() throws IOException {
		MapPoiTypes.setDefault(new MapPoiTypes("src/test/resources/poi_types.xml"));
		net.osmand.shared.osm.MapPoiTypes.Companion.setDefault(
				new net.osmand.shared.osm.MapPoiTypes("src/test/resources/poi_types.xml"));
		System.out.println("SearchUICoreCompatTest: " + cases.size() + " cases, " + phrasesRun + " phrases, "
				+ selections + " selections, " + resultsCompared + " results compared, " + united + " results united or dropped as duplicates, "
				+ sameTies + " results the apis publish twice alike, " + otherDuplicates
				+ " phrases not compared, with the other record of a poi stored twice");
		for (BinaryMapIndexReader r : javaReaders.values()) {
			r.close();
		}
		for (net.osmand.shared.binary.BinaryMapIndexReader r : copyReaders.values()) {
			r.close();
		}
	}

	@Test
	public void collectionsAreTheSame() throws IOException {
		String only = System.getenv("OSMAND_SEARCH_APIS_CASE");
		JAVA_DUMP.getParentFile().mkdirs();
		// written aside and moved in place only when every case passed: a dump cut short would pass for a whole one
		File written = new File(JAVA_DUMP.getPath() + ".part");
		try (java.io.Writer w = only != null ? null : Files.newBufferedWriter(written.toPath(), StandardCharsets.UTF_8)) {
			out = w;
			for (Map.Entry<String, String> e : SearchApisCompatTest.poiPhrases.entrySet()) {
				write("X " + hex(e.getKey()) + "\t" + hex(e.getValue()));
			}
			for (int i = 0; i < cases.size(); i++) {
				if (only == null || cases.get(i).name.startsWith(only)) {
					runCase(i, cases.get(i));
				}
			}
			String maps = System.getenv("OSMAND_SEARCH_APIS_MAPS");
			if (maps != null && !maps.isEmpty()) {
				String[] paths = maps.split(":");
				for (int i = 0; i < paths.length; i++) {
					runCase(1000 + i, SearchApisCompatTest.mapCase(paths[i], javaReaders, copyReaders));
				}
			}
			if (only == null) {
				assertTrue("phrases run: " + phrasesRun, phrasesRun > 1000);
				assertTrue("results united: " + united, united > 0);
			}
		} finally {
			out = null;
		}
		if (only == null) {
			Files.move(written.toPath(), JAVA_DUMP.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void emit(String line) {
		if (out != null) {
			caseLines.add(line);
		}
	}

	private static void write(String line) {
		if (out != null) {
			try {
				out.write(line);
				out.write('\n');
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		}
	}

	private void runCase(int index, SearchApisCompatTest.Case c) {
		boolean poiTypes = c.hasPoiTypePhrase();
		SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = poiTypes;
		net.osmand.shared.search.core.SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = poiTypes;
		caseLines = new ArrayList<>();
		try {
			SearchPhraseCompatTest.NotingCollator collator = new SearchPhraseCompatTest.NotingCollator();
			SearchSettings js = SearchSettings.parseJSON(new JSONObject(c.settings));
			net.osmand.shared.search.core.SearchSettings ks = SearchApisCompatTest.copySettings(c.settings);
			List<BinaryMapIndexReader> jr = new ArrayList<>();
			List<net.osmand.shared.binary.BinaryMapIndexReader> kr = new ArrayList<>();
			for (String f : c.files) {
				jr.add(javaReaders.get(f));
				kr.add(copyReaders.get(f));
			}
			js.setOfflineIndexes(jr);
			ks.setOfflineIndexes(kr);
			Run run = new Run(c.name, js, ks, collator);
			for (String text : c.phrases) {
				if (text.startsWith("POI_TYPE:")) {
					String[] arr = text.split("[\\\\{}]");
					int empty = run.typed("", 1, true);
					List<Object> found = run.j.results.get(empty);
					for (int r = 0; r < found.size(); r++) {
						if (arr.length > 1 && arr[1].equals(field(found.get(r), "localeName"))) {
							run.withWord(empty, r, arr.length > 2 ? arr[2] : "");
							break;
						}
					}
					continue;
				}
				// the first phrase of a case is typed in as well, every third prefix of it dumped in full
				List<Integer> cuts = text.equals(c.phrases.get(0)) ? SearchApisCompatTest.cuts(text) : new ArrayList<>();
				for (int n = 0; n < cuts.size(); n++) {
					dumping = n % 3 == 0;
					run.typed(text.substring(0, cuts.get(n)), 1, false);
				}
				dumping = true;
				int whole = run.typed(text, 1, true);
				run.typed(text, 2, false);
				List<Integer> children = run.selectEach(whole, true);
				for (int i = 0; i < children.size() && i < 3; i++) {
					run.selectEach(children.get(i), false);
				}
			}
			if (c.json != null) {
				asSearchUICoreTest(c, collator);
			}
			List<String> lines = caseLines;
			caseLines = null;
			write("C " + index + "\t" + hex(c.name) + "\t" + hex(c.settings) + "\t" + hexList(c.files) + "\t" + poiTypes
					+ "\t" + (c.json == null ? "-" : hex(c.json)));
			write("K " + collator.keys());
			for (String l : lines) {
				write(l);
			}
		} finally {
			caseLines = null;
			dumping = true;
			SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false;
			net.osmand.shared.search.core.SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false;
		}
	}

	/**
	 * The phrases of the case as {@code SearchUICoreTest} runs them, on a core of their own, for the
	 * words they compare: {@code SearchUICoreTest} in OsmAnd-shared runs them with the keys of these.
	 */
	private static void asSearchUICoreTest(SearchApisCompatTest.Case c, SearchPhraseCompatTest.NotingCollator collator) {
		JSONObject settings = new JSONObject(c.settings);
		if (settings.optBoolean("disabled", false)) {
			return;
		}
		SearchSettings s = SearchSettings.parseJSON(settings);
		if (settings.optBoolean("useData", true) && !c.files.isEmpty()) {
			List<BinaryMapIndexReader> readers = new ArrayList<>();
			for (String f : c.files) {
				readers.add(javaReaders.get(f));
			}
			s.setOfflineIndexes(readers);
		}
		SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), "en", false);
		core.init();
		SearchPhrase emptyPhrase = SearchPhrase.emptyPhrase(s, collator);
		for (String text : c.phrases) {
			String[] arr = text.split("[\\\\{}]");
			if (arr.length > 0 && arr[0].equals("POI_TYPE:")) {
				SearchPhrase phrase = emptyPhrase.generateNewPhrase("", s);
				for (SearchResult r : kept(core, phrase)) {
					if (arr.length > 1 && arr[1].equals(r.localeName)) {
						phrase = emptyPhrase.generateNewPhrase(arr.length > 2 ? arr[2] : "", s);
						phrase.getWords().add(new SearchWord(r.localeName, r));
						kept(core, phrase);
						break;
					}
				}
			} else {
				kept(core, emptyPhrase.generateNewPhrase(text, s));
			}
		}
	}

	/** The results the core keeps for [phrase], as {@code SearchUICoreTest} gets them. */
	private static List<SearchResult> kept(SearchUICore core, SearchPhrase phrase) {
		List<Object> found = new ArrayList<>();
		SearchUICore.SearchResultMatcher matcher = SearchApisCompatTest.javaMatcher(found, phrase);
		call(core, "searchInternal", phrase, matcher);
		SearchResultCollection collection = new SearchResultCollection(phrase);
		collection.addSearchResults(matcher.getRequestResults(), true, true);
		return collection.getCurrentSearchResults();
	}

	/** One side of a case: its core, the phrases it searched, and the results it kept for each. */
	private static final class Side {
		final boolean java;
		final Object core;
		final List<Object> phrases = new ArrayList<>();
		final List<List<Object>> results = new ArrayList<>();

		Side(boolean java, Object core) {
			this.java = java;
			this.core = core;
		}

		Object collection(Object phrase) {
			return java ? new SearchResultCollection((SearchPhrase) phrase)
					: new net.osmand.shared.search.SearchUICore.SearchResultCollection(
					(net.osmand.shared.search.core.SearchPhrase) phrase);
		}

		/** The results of each api, in the order the core ran the apis, each list as the api published it. */
		List<List<Object>> search(Object phrase) {
			List<Object> recorded = new ArrayList<>();
			Object matcher = java ? SearchApisCompatTest.javaMatcher(recorded, (SearchPhrase) phrase)
					: SearchApisCompatTest.copyMatcher(recorded, phrase);
			call(core, "searchInternal", phrase, matcher);
			List<List<Object>> byApi = new ArrayList<>();
			List<Object> api = new ArrayList<>();
			for (Object r : recorded) {
				String type = String.valueOf(field(r, "objectType"));
				if (type.equals("SEARCH_API_FINISHED")) {
					byApi.add(api);
					api = new ArrayList<>();
				} else if (!type.startsWith("SEARCH_")) {
					api.add(r);
				}
			}
			if (!api.isEmpty()) {
				// an api that threw, after it published
				byApi.add(api);
			}
			return byApi;
		}
	}

	private static SearchUICore javaCore() {
		SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), "en", false, () -> false);
		core.init();
		return core;
	}

	private static Object copyCore() {
		try {
			Object core = net.osmand.shared.search.SearchUICore.class.getConstructor(net.osmand.shared.osm.MapPoiTypes.class,
					String.class, boolean.class, SearchApisCompatTest.FUNCTION0).newInstance(
					net.osmand.shared.osm.MapPoiTypes.Companion.getDefault(), "en", false, SearchApisCompatTest.offline());
			call(core, "init");
			return core;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** The phrases of a case on both sides, in the order they are run. */
	private static final class Run {
		final String name;
		final SearchSettings js;
		final net.osmand.shared.search.core.SearchSettings ks;
		final SearchPhraseCompatTest.NotingCollator collator;
		final Side j = new Side(true, javaCore());
		final Side k = new Side(false, copyCore());

		Run(String name, SearchSettings js, net.osmand.shared.search.core.SearchSettings ks,
				SearchPhraseCompatTest.NotingCollator collator) {
			this.name = name;
			this.js = js;
			this.ks = ks;
			this.collator = collator;
		}

		/** Types [text] in at [radius]; the id of the phrase. */
		int typed(String text, int radius, boolean keep) {
			SearchSettings jsr = radius == 1 ? js : js.setRadiusLevel(radius);
			net.osmand.shared.search.core.SearchSettings ksr = radius == 1 ? ks : ks.setRadiusLevel(radius);
			SearchPhrase jp = SearchPhrase.emptyPhrase(jsr, collator).generateNewPhrase(text, jsr);
			Object kp = net.osmand.shared.search.core.SearchPhrase.Companion.emptyPhrase(ksr).generateNewPhrase(text, ksr);
			return run("T", -1, -1, text, radius, keep, jp, kp);
		}

		/**
		 * Selects the result [r] of phrase [parent] as the ui does, and searches the text of the phrase
		 * that makes; the id of the phrase.
		 */
		int selected(int parent, int r, boolean keep) {
			SearchPhrase jparent = (SearchPhrase) j.phrases.get(parent);
			Object kparent = k.phrases.get(parent);
			SearchPhrase js = jparent.selectWord((SearchResult) j.results.get(parent).get(r), jparent.getSettings());
			Object ks = call(kparent, "selectWord", k.results.get(parent).get(r), call(kparent, "getSettings"));
			SearchPhrase jp = js.generateNewPhrase(js.getText(true), jparent.getSettings());
			Object kp = call(ks, "generateNewPhrase", call(ks, "getText", true), call(kparent, "getSettings"));
			selections++;
			return run("S", parent, r, "", jp.getRadiusLevel(), keep, jp, kp);
		}

		/** The poi type [r] of phrase [parent] as a word before [text], as {@code SearchUICoreTest} makes it. */
		void withWord(int parent, int r, String text) {
			SearchResult jres = (SearchResult) j.results.get(parent).get(r);
			Object kres = k.results.get(parent).get(r);
			SearchPhrase jp = SearchPhrase.emptyPhrase(js, collator).generateNewPhrase(text, js);
			jp.getWords().add(new SearchWord(jres.localeName, jres));
			Object kp = net.osmand.shared.search.core.SearchPhrase.Companion.emptyPhrase(ks).generateNewPhrase(text, ks);
			@SuppressWarnings("unchecked")
			List<Object> words = (List<Object>) call(kp, "getWords");
			words.add(new net.osmand.shared.search.core.SearchWord((String) field(kres, "localeName"),
					(net.osmand.shared.search.core.SearchResult) kres));
			run("W", parent, r, text, 1, false, jp, kp);
		}

		/** Selects the first result of each type phrase [parent] found; the ids of the phrases. */
		List<Integer> selectEach(int parent, boolean keep) {
			List<Integer> children = new ArrayList<>();
			Set<Object> seen = new LinkedHashSet<>();
			List<Object> found = j.results.get(parent);
			for (int r = 0; found != null && r < found.size(); r++) {
				SearchResult res = (SearchResult) found.get(r);
				if (res.objectType == null || res.localeName == null || !seen.add(res.objectType)) {
					continue;
				}
				children.add(selected(parent, r, keep));
			}
			return children;
		}

		private int run(String kind, int parent, int r, String text, int radius, boolean keep, SearchPhrase jp, Object kp) {
			int qid = j.phrases.size();
			j.phrases.add(jp);
			k.phrases.add(kp);
			String m = name + " " + qid + " " + jp;
			List<List<Object>> jb = j.search(jp);
			List<List<Object>> kb = k.search(kp);
			List<Object> jc = canonical(jb);
			List<Object> kc = canonical(kb);
			// what the apis published, each api's results in the order of their lines
			List<String> jraw = lines(jc, false);
			List<String> kraw = lines(kc, false);
			boolean compared = jraw.equals(kraw);
			if (!compared) {
				// a poi stored twice under one id, which the api of pois by name takes once: the one the reader hands it first
				assertEquals(m, SearchApisCompatTest.withoutSubtypes(jraw), SearchApisCompatTest.withoutSubtypes(kraw));
				otherDuplicates++;
			}
			int[] sizes = new int[jb.size()];
			for (int a = 0; a < jb.size(); a++) {
				sizes[a] = jb.get(a).size();
				assertEquals(m, jb.get(a).size(), kb.get(a).size());
			}
			sameTies += ties(jraw);
			Object byType = ((SearchUICore) j.core).getApiByClass(SearchCoreFactory.SearchAmenityByTypeAPI.class);
			String tie = !compared ? "other" : out != null && dumping ? SearchApisCompatTest.tie(byType, jp) : "-";
			emit("Q " + qid + "\t" + parent + "\t" + kind + "\t" + r + "\t" + hex(text) + "\t" + radius + "\t"
					+ (keep && compared) + "\t" + dumping + "\t" + joined(sizes) + "\t" + tie);

			List<List<Object>> kept = compare(m + " kept", "F", qid, jp, kp, jc, kc, true, true, 0, compared);
			compare(m + " merged api by api", "I", qid, jp, kp, jb, kb, true, true, 1, compared);
			compare(m + " merged without sorting", "J", qid, jp, kp, jb, kb, false, true, 1, compared);
			compare(m + " sorted", "N", qid, jp, kp, jc, kc, true, false, 0, compared);
			same(m, qid, jc, kc, compared);
			String jq = queries(j.core, jp);
			assertEquals(m, jq, queries(k.core, kp));
			emit("M " + qid + "\t" + hex(jq));
			j.results.add(keep && compared ? kept.get(0) : null);
			k.results.add(keep && compared ? kept.get(1) : null);
			phrasesRun++;
			return qid;
		}

		/**
		 * Makes a collection of the results on both sides and compares it. [merged] 0 adds the results at
		 * once; 1 makes a collection of each api's results and merges it into the ones before, as the ui
		 * does. The lists both sides made; with [compared] false, when the apis did not find the same,
		 * they are only made.
		 */
		@SuppressWarnings("unchecked")
		private List<List<Object>> compare(String m, String kind, int qid, SearchPhrase jp, Object kp, Object jr, Object kr,
				boolean resort, boolean removeDuplicates, int merged, boolean compared) {
			List<Object> jl;
			List<Object> kl;
			List<Object> jc;
			List<Object> kc;
			if (merged == 0) {
				jc = copies((List<Object>) jr);
				kc = copies((List<Object>) kr);
				jl = results(call(j.collection(jp), "addSearchResults", jc, resort, removeDuplicates));
				kl = results(call(k.collection(kp), "addSearchResults", kc, resort, removeDuplicates));
			} else {
				jc = new ArrayList<>();
				kc = new ArrayList<>();
				Object ja = j.collection(jp);
				Object ka = k.collection(kp);
				List<List<Object>> jb = (List<List<Object>>) jr;
				List<List<Object>> kb = (List<List<Object>>) kr;
				for (int a = 0; a < jb.size(); a++) {
					List<Object> jl0 = copies(canonical(Collections.singletonList(jb.get(a))));
					List<Object> kl0 = copies(canonical(Collections.singletonList(kb.get(a))));
					jc.addAll(jl0);
					kc.addAll(kl0);
					Object jac = call(j.collection(jp), "addSearchResults", jl0, true, true);
					Object kac = call(k.collection(kp), "addSearchResults", kl0, true, true);
					ja = call(ja, "combineWithCollection", jac, resort, removeDuplicates);
					ka = call(ka, "combineWithCollection", kac, resort, removeDuplicates);
				}
				jl = results(ja);
				kl = results(ka);
			}
			if (!compared) {
				return Arrays.asList(jl, kl);
			}
			List<String> jlines = finalLines(jl, jp);
			List<String> klines = finalLines(kl, kp);
			if (!jlines.equals(klines)) {
				for (int i = 0; i < Math.max(jlines.size(), klines.size()); i++) {
					String x = i < jlines.size() ? jlines.get(i) : "-";
					String y = i < klines.size() ? klines.get(i) : "-";
					if (!x.equals(y)) {
						System.out.println("MISMATCH " + m + " at " + i + "\n  J " + x + "\n  K " + y);
						break;
					}
				}
			}
			assertEquals(m, jlines, klines);
			resultsCompared += jl.size();
			String indices = indices(jl, jc);
			assertEquals(m, indices, indices(kl, kc));
			emit(kind + " " + qid + "\t" + indices);
			if (kind.equals("F")) {
				united += jc.size() - jl.size();
				if (dumping) {
					for (int i = 0; i < jlines.size(); i++) {
						emit("R " + qid + "\t" + i + "\t" + jlines.get(i));
					}
				}
			}
			return Arrays.asList(jl, kl);
		}

		/** Whether each result is the same place as the ones after it, for the dump, as far as {@link #SAME_DEPTH}. */
		private void same(String m, int qid, List<Object> jc, List<Object> kc, boolean compared) {
			if (!compared) {
				return;
			}
			Object jcol = j.collection(j.phrases.get(qid));
			Object kcol = k.collection(k.phrases.get(qid));
			StringBuilder sb = new StringBuilder();
			for (int a = 0; a < jc.size(); a++) {
				for (int b = a + 1; b < jc.size() && b <= a + SAME_DEPTH; b++) {
					Object ja = jc.get(a);
					Object jb = jc.get(b);
					Object ka = kc.get(a);
					Object kb = kc.get(b);
					String jsame = outcome(() -> call(jcol, "sameSearchResult", ja, jb));
					String ksame = outcome(() -> call(kcol, "sameSearchResult", ka, kb));
					assertEquals(m + " " + a + " " + b, jsame, ksame);
					sb.append(jsame.equals("true") ? '1' : jsame.equals("false") ? '0' : 'x');
				}
			}
			if (dumping) {
				emit("Y " + qid + "\t" + (sb.length() == 0 ? "-" : sb));
			}
		}
	}

	/**
	 * The results of each api in an order every side gives them in, by their lines without the weight
	 * of the match; the core asks for it later.
	 */
	static List<Object> canonical(List<List<Object>> byApi) {
		List<Object> all = new ArrayList<>();
		for (List<Object> api : byApi) {
			List<Object> sorted = new ArrayList<>(api);
			IdentityHashMap<Object, String> lines = new IdentityHashMap<>();
			for (Object r : sorted) {
				lines.put(r, resultLine(r, false));
			}
			sorted.sort(Comparator.comparing(lines::get));
			all.addAll(sorted);
		}
		return all;
	}

	/**
	 * Copies of the results as the apis published them, for a collection of their own: a collection
	 * changes the results it unites, and the weight of a match stays as it is first asked for.
	 */
	static List<Object> copies(List<Object> results) {
		return copies(results, false);
	}

	/** [copies], with the weights of the matches of results made up with one. */
	private static List<Object> copies(List<Object> results, boolean weight) {
		List<Object> l = new ArrayList<>();
		for (Object r : results) {
			l.add(copyOf(r, weight));
		}
		return l;
	}

	@SuppressWarnings("unchecked")
	private static Object copyOf(Object r, boolean weight) {
		Object phrase = field(r, "requiredSearchPhrase");
		Object c = r instanceof SearchResult ? new SearchResult((SearchPhrase) phrase)
				: new net.osmand.shared.search.core.SearchResult((net.osmand.shared.search.core.SearchPhrase) phrase);
		try {
			for (java.lang.reflect.Field f : r.getClass().getDeclaredFields()) {
				String n = f.getName();
				if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || (n.equals("unknownPhraseMatchWeight") && !weight)
						|| n.equals("completeMatchRes") || n.equals("searchResultResource")) {
					continue;
				}
				f.setAccessible(true);
				Object v = f.get(r);
				if (v != null && n.equals("otherNames")) {
					v = new ArrayList<>((Collection<Object>) v);
				} else if (v instanceof java.util.SortedSet) {
					v = new java.util.TreeSet<>((java.util.SortedSet<Object>) v);
				} else if (v != null && v.getClass().getSimpleName().equals("KTreeSet")) {
					// as SearchUICoreTest in OsmAnd-shared copies it: in the order of the collator of the phrase
					net.osmand.shared.util.KCollator collator = ((net.osmand.shared.search.core.SearchPhrase) phrase).getCollator();
					Comparator<String> order = collator::compare;
					Collection<String> words = (Collection<String>) v.getClass().getConstructor(Comparator.class).newInstance(order);
					words.addAll((Collection<String>) v);
					v = words;
				} else if (v instanceof Collection) {
					throw new AssertionError("a collection in " + n + " not copied");
				}
				f.set(c, v);
			}
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
		return c;
	}

	/** Neighbours in the canonical order that are alike: which of them comes first is the reader's. */
	private static int ties(List<String> lines) {
		int ties = 0;
		for (int i = 1; i < lines.size(); i++) {
			if (lines.get(i - 1).equals(lines.get(i))) {
				ties++;
			}
		}
		return ties;
	}

	private static List<String> lines(List<Object> results, boolean weight) {
		List<String> l = new ArrayList<>();
		for (Object r : results) {
			l.add(resultLine(r, weight));
		}
		return l;
	}

	@SuppressWarnings("unchecked")
	private static List<Object> results(Object collection) {
		return new ArrayList<>((List<Object>) call(collection, "getCurrentSearchResults"));
	}

	/** A result the core keeps: all of it, the address it gave a poi, and the lines the tests of the search compare. */
	static List<String> finalLines(List<Object> results, Object phrase) {
		List<String> l = new ArrayList<>();
		for (Object r : results) {
			l.add(resultLine(r, true) + " " + v(field(r, "addressName")) + " " + v(format(true, r, phrase)) + " "
					+ v(format(false, r, phrase)));
		}
		return l;
	}

	private static String format(boolean simple, Object r, Object phrase) {
		return outcome(() -> r instanceof SearchResult
				? SearchUICore.formatSearchResultForTest(simple, (SearchResult) r, (SearchPhrase) phrase)
				: net.osmand.shared.search.SearchUICore.Companion.formatSearchResultForTest(simple,
				(net.osmand.shared.search.core.SearchResult) r, (net.osmand.shared.search.core.SearchPhrase) phrase));
	}

	/** Where each of [kept] is among [results], by identity. */
	private static String indices(List<Object> kept, List<Object> results) {
		IdentityHashMap<Object, Integer> at = new IdentityHashMap<>();
		for (int i = 0; i < results.size(); i++) {
			at.put(results.get(i), i);
		}
		StringBuilder sb = new StringBuilder();
		for (Object r : kept) {
			Integer i = at.get(r);
			assertTrue("a result that is none of the ones found", i != null);
			sb.append(sb.length() > 0 ? "," : "").append(i);
		}
		return sb.length() == 0 ? "-" : sb.toString();
	}

	private static String joined(int[] values) {
		StringBuilder sb = new StringBuilder();
		for (int v : values) {
			sb.append(sb.length() > 0 ? "," : "").append(v);
		}
		return sb.length() == 0 ? "-" : sb.toString();
	}

	/** What the core answers about searching [phrase] further, and the poi type and name its api of pois by type took. */
	static String queries(Object core, Object phrase) {
		return outcome(() -> call(core, "isSearchMoreAvailable", phrase)) + " "
				+ outcome(() -> call(core, "getMinimalSearchRadius", phrase)) + " "
				+ outcome(() -> call(core, "getNextSearchRadius", phrase)) + " "
				+ SearchApisCompatTest.describe(call(core, "getUnselectedPoiType")) + " "
				+ v(call(core, "getCustomNameFilter"));
	}

	private static String outcome(java.util.concurrent.Callable<Object> c) {
		try {
			return String.valueOf(c.call());
		} catch (Throwable t) {
			Throwable cause = t;
			while ((cause instanceof AssertionError || cause instanceof java.lang.reflect.InvocationTargetException)
					&& cause.getCause() != null) {
				cause = cause.getCause();
			}
			return "threw:" + cause.getClass().getSimpleName();
		}
	}

	/**
	 * Results made up for what the cases do not have, kept by both sides:
	 * <ul>
	 * <li>a result of a heavier type one point of weight below the one it is the same place as, which
	 * takes its place, and a result the same place as two that are not the same as each other;</li>
	 * <li>pois with an address in a settlement four and five times over, where it begins to be
	 * written after the street;</li>
	 * <li>travel articles about one place in other languages than the one searched in, some with
	 * other words of the phrase they matched.</li>
	 * </ul>
	 */
	@Test
	public void madeUpResultsAreKeptTheSame() {
		SearchSettings js = new SearchSettings((SearchSettings) null).setLang("en", false).setOriginalLocation(new net.osmand.data.LatLon(52.5, 13.4));
		net.osmand.shared.search.core.SearchSettings ks = new net.osmand.shared.search.core.SearchSettings(
				(net.osmand.shared.search.core.SearchSettings) null).setLang("en", false)
				.setOriginalLocation(new net.osmand.shared.data.KLatLon(52.5, 13.4));
		for (String text : new String[] {"Foo", "Berlin"}) {
			SearchPhrase jp = SearchPhrase.emptyPhrase(js).generateNewPhrase(text, js);
			Object kp = net.osmand.shared.search.core.SearchPhrase.Companion.emptyPhrase(ks).generateNewPhrase(text, ks);
			for (double[] weights : new double[][] {{3, 2}, {3, 2.5}, {3, 1.9}, {2, 2}}) {
				List<Object> jr = new ArrayList<>();
				List<Object> kr = new ArrayList<>();
				// a settlement and a street of its name 180 m apart, and a house between them, which is the same place as both
				String[] types = {"CITY", "STREET", "HOUSE"};
				double[] meters = {0, 180, 90};
				for (int i = 0; i < types.length; i++) {
					double lon = 13.4 + meters[i] / 67_800;
					double weight = weights[i / 2];
					jr.add(address(new SearchResult(jp), types[i], text, 52.5, lon, weight));
					kr.add(address(new net.osmand.shared.search.core.SearchResult((net.osmand.shared.search.core.SearchPhrase) kp),
							types[i], text, 52.5, lon, weight));
				}
				assertSameKept(text + " " + Arrays.toString(weights), jp, kp, jr, kr);
			}
			for (int count = 3; count <= 6; count++) {
				List<Object> jr = new ArrayList<>();
				List<Object> kr = new ArrayList<>();
				for (int i = 0; i < count + 2; i++) {
					String city = i < count ? "Foo, Bar" : "Baz";
					jr.add(poi(new SearchResult(jp), new net.osmand.data.Amenity(), i, text, city));
					kr.add(poi(new net.osmand.shared.search.core.SearchResult((net.osmand.shared.search.core.SearchPhrase) kp),
							new net.osmand.shared.data.Amenity(), i, text, city));
				}
				assertSameKept(text + " addresses " + count, jp, kp, jr, kr);
			}
			List<Object> ja = new ArrayList<>();
			List<Object> ka = new ArrayList<>();
			String[] langs = {"de", "fr", "it", "en"};
			for (int i = 0; i < langs.length; i++) {
				ja.add(article(new SearchResult(jp), new net.osmand.data.Amenity(), i, langs[i], text));
				ka.add(article(new net.osmand.shared.search.core.SearchResult((net.osmand.shared.search.core.SearchPhrase) kp),
						new net.osmand.shared.data.Amenity(), i, langs[i], text));
				if (i % 2 == 1) {
					java.util.TreeSet<String> words = new java.util.TreeSet<>(jp.getCollator());
					words.add("Mitte " + langs[i]);
					set(ja.get(i), "otherWordsMatch", words);
					set(ka.get(i), "otherWordsMatch", copyWords(kp, words));
				}
				assertSameKept(text + " articles " + (i + 1), jp, kp, ja, ka);
			}
		}
	}

	private static void assertSameKept(String m, SearchPhrase jp, Object kp, List<Object> jr, List<Object> kr) {
		for (boolean resort : new boolean[] {true, false}) {
			List<Object> jl = results(call(new SearchResultCollection(jp), "addSearchResults", copies(jr, true), resort, true));
			List<Object> kl = results(call(new net.osmand.shared.search.SearchUICore.SearchResultCollection(
					(net.osmand.shared.search.core.SearchPhrase) kp), "addSearchResults", copies(kr, true), resort, true));
			assertEquals(m + " " + resort, finalLines(jl, jp), finalLines(kl, kp));
		}
	}

	private static Object address(Object r, String type, String name, double lat, double lon, double weight) {
		boolean java = r instanceof SearchResult;
		set(r, "objectType", java ? net.osmand.search.core.ObjectType.valueOf(type) : net.osmand.shared.search.core.ObjectType.valueOf(type));
		set(r, "localeName", name);
		set(r, "location", java ? new net.osmand.data.LatLon(lat, lon) : new net.osmand.shared.data.KLatLon(lat, lon));
		call(r, "setUnknownPhraseMatchWeight", weight);
		return r;
	}

	/** A poi [i] named [name] in [city], with a street and a house number. */
	private static Object poi(Object r, Object amenity, int i, String name, String city) {
		boolean java = r instanceof SearchResult;
		call(amenity, "setId", (long) (2 * (i + 1)));
		call(amenity, "setType", java ? MapPoiTypes.getDefault().getOtherPoiCategory()
				: net.osmand.shared.osm.MapPoiTypes.Companion.getDefault().getOtherPoiCategory());
		call(amenity, "setSubType", "shop");
		call(amenity, "setName", name + " " + i);
		call(amenity, "setLocation", 52.5 + i / 100.0, 13.4);
		call(amenity, "setAdditionalInfo", "addr_street", "Main Street");
		call(amenity, "setAdditionalInfo", "addr_housenumber", String.valueOf(i + 1));
		set(r, "object", amenity);
		set(r, "objectType", java ? net.osmand.search.core.ObjectType.POI : net.osmand.shared.search.core.ObjectType.POI);
		set(r, "localeName", name + " " + i);
		set(r, "cityName", city);
		set(r, "location", java ? new net.osmand.data.LatLon(52.5 + i / 100.0, 13.4) : new net.osmand.shared.data.KLatLon(52.5 + i / 100.0, 13.4));
		return r;
	}

	/** [words] in a set of the copy, in the order of the collator of [phrase], as the copy keeps them. */
	@SuppressWarnings("unchecked")
	private static Object copyWords(Object phrase, Collection<String> words) {
		try {
			net.osmand.shared.util.KCollator collator = ((net.osmand.shared.search.core.SearchPhrase) phrase).getCollator();
			Comparator<String> order = collator::compare;
			Collection<String> set = (Collection<String>) Class.forName("net.osmand.shared.util.collections.KTreeSet")
					.getConstructor(Comparator.class).newInstance(order);
			set.addAll(words);
			return set;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** A travel article about [name] in [lang], with one wikidata for all of them. */
	private static Object article(Object r, Object amenity, int i, String lang, String name) {
		boolean java = r instanceof SearchResult;
		call(amenity, "setId", (long) (2 * (i + 1)));
		call(amenity, "setType", java ? MapPoiTypes.getDefault().getOtherPoiCategory()
				: net.osmand.shared.osm.MapPoiTypes.Companion.getDefault().getOtherPoiCategory());
		call(amenity, "setSubType", "route_article");
		call(amenity, "setName", name + " " + lang);
		call(amenity, "setLocation", 52.5, 13.4);
		call(amenity, "setRegionName", "World_wikivoyage_travel");
		call(amenity, "setAdditionalInfo", "wikidata", "Q64");
		call(amenity, "setAdditionalInfo", "lang_yes:" + lang, "yes");
		set(r, "object", amenity);
		set(r, "objectType", java ? net.osmand.search.core.ObjectType.POI : net.osmand.shared.search.core.ObjectType.POI);
		set(r, "localeName", name + " " + lang);
		set(r, "location", java ? new net.osmand.data.LatLon(52.5, 13.4) : new net.osmand.shared.data.KLatLon(52.5, 13.4));
		return r;
	}

	private static void set(Object o, String name, Object value) {
		try {
			java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(o, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	// the core as the ui calls it

	/**
	 * The phrases of each case through the core as the ui sends them: searched at once, then the first
	 * result of each type selected and searched on, the phrase cleared and made again, and the
	 * categories of pois asked for; one after another with each cancelling the one before; and at
	 * once to one api, in the background.
	 */
	@Test
	public void coreAnswersTheSame() throws Exception {
		int searched = 0;
		for (SearchApisCompatTest.Case c : cases) {
			SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = c.hasPoiTypePhrase();
			net.osmand.shared.search.core.SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = c.hasPoiTypePhrase();
			try {
				SearchUICore jcore = javaCore();
				Object kcore = copyCore();
				SearchSettings js = SearchSettings.parseJSON(new JSONObject(c.settings));
				net.osmand.shared.search.core.SearchSettings ks = SearchApisCompatTest.copySettings(c.settings);
				List<BinaryMapIndexReader> jr = new ArrayList<>();
				List<net.osmand.shared.binary.BinaryMapIndexReader> kr = new ArrayList<>();
				for (String f : c.files) {
					jr.add(javaReaders.get(f));
					kr.add(copyReaders.get(f));
				}
				js.setOfflineIndexes(jr);
				ks.setOfflineIndexes(kr);
				jcore.updateSettings(js);
				call(kcore, "updateSettings", ks);
				jcore.resetSearch();
				call(kcore, "resetSearch");
				for (String text : c.phrases) {
					if (text.startsWith("POI_TYPE:")) {
						continue;
					}
					String m = c.name + " " + text;
					Object jres = jcore.immediateSearch(text, null);
					Object kres = call(kcore, "immediateSearch", text, null);
					List<Object> jl = results(jres);
					List<Object> kl = results(kres);
					assertSameResults(m, jres, kres);
					searched++;
					Set<Object> seen = new LinkedHashSet<>();
					for (int r = 0; r < jl.size() && seen.size() < 3; r++) {
						SearchResult res = (SearchResult) jl.get(r);
						Object kr0 = find(kl, res);
						if (res.objectType == null || res.localeName == null || kr0 == null || !seen.add(res.objectType)) {
							continue;
						}
						jcore.resetPhrase(text);
						call(kcore, "resetPhrase", text);
						jcore.selectSearchResult(res);
						call(kcore, "selectSearchResult", kr0);
						String jtext = jcore.getPhrase().getText(true);
						assertEquals(m, jtext, call(call(kcore, "getPhrase"), "getText", true));
						String ms = m + " selected " + resultLine(res, true);
						assertSameResults(ms, jcore.immediateSearch(jtext, null), call(kcore, "immediateSearch", jtext, null));
						assertEquals(ms, jcore.getPhrase().toString(), call(kcore, "getPhrase").toString());
						jcore.resetPhrase(res);
						call(kcore, "resetPhrase", kr0);
						assertEquals(ms, jcore.getPhrase().getText(true), call(call(kcore, "getPhrase"), "getText", true));
						jcore.resetPhrase(text);
						call(kcore, "resetPhrase", text);
						assertEquals(ms, jcore.getPhrase().getText(true), call(call(kcore, "getPhrase"), "getText", true));
						jcore.resetPhrase();
						call(kcore, "resetPhrase");
						assertEquals(ms, jcore.getPhrase().getText(true), call(call(kcore, "getPhrase"), "getText", true));
					}
					jcore.resetSearch();
					call(kcore, "resetSearch");
				}
				SearchResultCollection jtypes = jcore.shallowSearch(SearchCoreFactory.SearchAmenityTypesAPI.class, "", null);
				Object ktypes = call(kcore, "shallowSearch", kotlinClass(net.osmand.shared.search.core.SearchCoreFactory.SearchAmenityTypesAPI.class), "", null);
				assertSameResults(c.name + " poi categories", jtypes, ktypes);
				assertEquals(c.name, jcore.isOnlineSearch(), call(kcore, "isOnlineSearch"));
			} finally {
				SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false;
				net.osmand.shared.search.core.SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false;
			}
		}
		assertTrue("phrases searched: " + searched, searched > 50);
		searchInTurn();
	}

	/**
	 * Two results lists of the core, which the order of the readers can change: the ones alike in all
	 * but where they are, and what the core unites and drops of them, may come in another order.
	 */
	private static void assertSameResults(String m, Object jc, Object kc) {
		List<Object> jl = results(jc);
		List<String> jlines = finalLines(jl, call(jc, "getPhrase"));
		List<String> klines = finalLines(results(kc), call(kc, "getPhrase"));
		if (!sorted(jlines).equals(sorted(klines))) {
			// a poi stored twice under one id, which the api of pois by name takes once: the one the reader hands it first
			assertEquals(m, SearchApisCompatTest.withoutSubtypes(jlines), SearchApisCompatTest.withoutSubtypes(klines));
			otherDuplicates++;
		}
		resultsCompared += jl.size();
	}

	private static List<String> sorted(List<String> l) {
		List<String> s = new ArrayList<>(l);
		Collections.sort(s);
		return s;
	}

	/** The copy's result with the line of [res]. */
	private static Object find(List<Object> results, Object res) {
		String line = resultLine(res, true);
		for (Object r : results) {
			if (resultLine(r, true).equals(line)) {
				return r;
			}
		}
		return null;
	}

	/** The kotlin class of [c], which OsmAnd-java sees only when it runs. */
	private static Object kotlinClass(Class<?> c) {
		try {
			return Class.forName("kotlin.jvm.JvmClassMappingKt").getMethod("getKotlinClass", Class.class).invoke(null, c);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * The first phrase of a few cases searched one after another, as the ui searches what is typed: a
	 * search waiting for the next letter, cancelled by the one that comes, which shows the results kept
	 * before it that still match, then searches. And one api searched in the background.
	 */
	private void searchInTurn() throws Exception {
		for (SearchApisCompatTest.Case c : cases.subList(0, Math.min(cases.size(), 6))) {
			String text = c.phrases.get(0);
			if (text.startsWith("POI_TYPE:") || text.length() < 3) {
				continue;
			}
			SearchUICore jcore = javaCore();
			Object kcore = copyCore();
			SearchSettings js = SearchSettings.parseJSON(new JSONObject(c.settings));
			net.osmand.shared.search.core.SearchSettings ks = SearchApisCompatTest.copySettings(c.settings);
			List<BinaryMapIndexReader> jr = new ArrayList<>();
			List<net.osmand.shared.binary.BinaryMapIndexReader> kr = new ArrayList<>();
			for (String f : c.files) {
				jr.add(javaReaders.get(f));
				kr.add(copyReaders.get(f));
			}
			js.setOfflineIndexes(jr);
			ks.setOfflineIndexes(kr);
			String m = c.name + " " + text;
			List<String> jevents = searchInTurn(jcore, js, text, (t, d, e) -> jcore.search(t, d, (ResultMatcher<SearchResult>) e));
			List<String> kevents = searchInTurn(kcore, ks, text, (t, d, e) -> call(kcore, "search", t, d, e));
			assertEquals(m, jevents, kevents);
			assertSameResults(m, jcore.getCurrentSearchResult(), call(kcore, "getCurrentSearchResult"));

			CountDownLatch jdone = new CountDownLatch(1);
			CountDownLatch kdone = new CountDownLatch(1);
			Object[] found = new Object[2];
			jcore.shallowSearchAsync(SearchCoreFactory.SearchAmenityByNameAPI.class, text, null, true, true, js, res -> {
				found[0] = res;
				jdone.countDown();
				return true;
			});
			call(kcore, "shallowSearchAsync", kotlinClass(net.osmand.shared.search.core.SearchCoreFactory.SearchAmenityByNameAPI.class),
					text, null, true, true, ks, callback(res -> {
						found[1] = res;
						kdone.countDown();
					}));
			assertTrue(m, jdone.await(60, TimeUnit.SECONDS));
			assertTrue(m, kdone.await(60, TimeUnit.SECONDS));
			assertSameResults(m + " in the background", found[0], found[1]);
		}
	}

	private interface Search {
		void search(String text, boolean delayed, Object matcher) throws Exception;
	}

	/**
	 * Types [text] in two letters short, then in whole, each search waiting for the next letter, and
	 * waits for the last one; what the core published to each, with the results of each api in the
	 * order of their lines.
	 */
	private static List<String> searchInTurn(Object core, Object settings, String text, Search search) throws Exception {
		call(core, "updateSettings", settings);
		List<String> events = new ArrayList<>();
		CountDownLatch done = new CountDownLatch(1);
		java.util.concurrent.atomic.AtomicInteger started = new java.util.concurrent.atomic.AtomicInteger();
		java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger();
		CountDownLatch complete = new CountDownLatch(1);
		call(core, "setOnSearchStart", runnable(core, started::incrementAndGet));
		call(core, "setOnResultsComplete", runnable(core, () -> {
			completed.incrementAndGet();
			complete.countDown();
		}));
		Object first = matcher(core, events, "first", null);
		Object last = matcher(core, events, "last", done);
		search.search(text.substring(0, text.length() - 2), true, first);
		search.search(text, true, last);
		assertTrue(text, done.await(60, TimeUnit.SECONDS));
		// called after the search has published that it finished
		assertTrue(text, complete.await(60, TimeUnit.SECONDS));
		synchronized (events) {
			List<String> l = new ArrayList<>(events);
			l.add("started " + started.get() + " complete " + completed.get());
			return l;
		}
	}

	/** A matcher on either side that notes what is published to it, the results of an api sorted. */
	private static Object matcher(Object core, List<String> events, String name, CountDownLatch done) {
		List<String> api = new ArrayList<>();
		java.util.function.Function<Object, Boolean> publish = r -> {
			synchronized (events) {
				String type = String.valueOf(field(r, "objectType"));
				if (type.startsWith("SEARCH_") || type.equals("FILTER_FINISHED")) {
					Collections.sort(api);
					events.addAll(api);
					api.clear();
					events.add(name + " " + type + (type.equals("SEARCH_API_FINISHED") ? " " + SearchApisCompatTest.describe(field(r, "object")) : ""));
				} else {
					api.add(name + " " + resultLine(r, false));
				}
				if (type.equals("SEARCH_FINISHED") && done != null) {
					done.countDown();
				}
			}
			return true;
		};
		if (core instanceof SearchUICore) {
			return new ResultMatcher<SearchResult>() {
				@Override
				public boolean publish(SearchResult object) {
					return publish.apply(object);
				}

				@Override
				public boolean isCancelled() {
					return false;
				}
			};
		}
		return new net.osmand.shared.binary.ResultMatcher<net.osmand.shared.search.core.SearchResult>() {
			@Override
			public boolean publish(net.osmand.shared.search.core.SearchResult obj) {
				return publish.apply(obj);
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};
	}

	/** A java runnable, or a kotlin lambda of no arguments. */
	private static Object runnable(Object core, Runnable r) {
		if (core instanceof SearchUICore) {
			return r;
		}
		return java.lang.reflect.Proxy.newProxyInstance(SearchApisCompatTest.FUNCTION0.getClassLoader(),
				new Class<?>[] {SearchApisCompatTest.FUNCTION0}, (proxy, method, args) -> {
					switch (method.getName()) {
						case "invoke":
							r.run();
							return kotlinUnit();
						case "hashCode":
							return System.identityHashCode(proxy);
						case "equals":
							return proxy == args[0];
						default:
							return "runnable";
					}
				});
	}

	/** A kotlin lambda of one argument that answers true. */
	private static Object callback(java.util.function.Consumer<Object> c) {
		try {
			Class<?> function1 = Class.forName("kotlin.jvm.functions.Function1");
			return java.lang.reflect.Proxy.newProxyInstance(function1.getClassLoader(), new Class<?>[] {function1},
					(proxy, method, args) -> {
						switch (method.getName()) {
							case "invoke":
								c.accept(args[0]);
								return Boolean.TRUE;
							case "hashCode":
								return System.identityHashCode(proxy);
							case "equals":
								return proxy == args[0];
							default:
								return "callback";
						}
					});
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	private static Object kotlinUnit() {
		try {
			return Class.forName("kotlin.Unit").getField("INSTANCE").get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
