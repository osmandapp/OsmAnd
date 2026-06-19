package net.osmand.search.core.spatial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.NameIndexReader;
import net.osmand.map.OsmandRegions;
import net.osmand.util.SearchAlgorithms;

//////////// OTHER TASKS ///////
// Check file sizes: 
// 1. FILE SIZE: REVIEW ADD_TOP_X_FREQ_WORDS (many common?)
// 2. FILE SIZE: REVIEW added bbox31 size
// 3. REVIEW SPLIT: if POI / Address is searched correctly - split Words - splitAndNormalizeSearchQuery(SearchPhrase.ALLDELIMITERS_WITH_HYPHEN);
//    - 2-га Нова (2 Нова), Бульварно-Кудрявська
// 4. TEST / REVIEW duplicate words in query Pennsylvania Street in Pennsylvania +
// 6. TEST / REVIEW - TOKENIZER (split) - COLLATOR: '#3', 'str.', 'U.S. Bank' ,'2-st' vs '2'  (Unit tests)
// 7. TEST / REVIEW - Numbers - isNumber2Letters '#3', and other
// 8. DATA: English postcodes
// 9. TEST / REVIEW - Unit test (<common_word> <almost_number>) -('№25'??, '25', '#25'?) -- +('школа', 'школа №25',  'школа 25')

//////////// TESTING //////////
// - EMPTY_SUFFIX_DICTIONARY_SENTINEL used only on client?
// - don't compute all combinations... (!) and do it in the right order 2^7

// CACHE
// TODO Evict - NameIndexReader in caches ( > 200 - indexByRef, matchedKeys) full clear

// BUILDINGS
// TODO Postcode + building
// TODO Search Buildings (to search buildings most complete street is needed (largest city sort?))
// TODO Ignore same embedded boundary city / county - deduplicate on the fly
// TODO [[2, нова, вулиця] STREET_TYPE 2-га Нова вулиця (-2626) 50.5006 30.3798 ]


// FEATURES
// TODO Read all top poi categories for files
// TODO POI Categories implement categories
// TODO World basemap ! POI  
// TODO Street intersection match
// TODO Abbreviations Phase
// TODO Sugggestion-correction
// TODO Search in large parks, neighborhood same as in boundaries
// TODO Search near key objects (subway station artificial bbox)

// ISSUES
// TODO Progress / cancel
// TODO read poi tag groups ! Refactor MAP_HAS_TAG_GROUPS
// TODO Combine by osmid (poi type internet) & wikidata id ?

// TEST
// TODO relevant if results > 10K don't read all objects, sort by distance?
// TODO test: merge boundaries bbox - extend incomplete boundary same id...
// TODO ? review settings: read objects after some intersections (but not too early)
//      - Results 5 tokens 1,949 (139 unique) - compact objects during combinations?
// TODO ? in the end recheck bbox boundary (full?) after load coordinates 31 (not 15) - chernihiv sport life
// TODO Enable ALWAYS_READ_COMMON_WORDS_ATOMS = true to find new results (common word in City) or suggest POI category 

//////////////// SEARCH ALGORITHM /////////////////
// 1. Init files + read caches
// 2. Split tokens
// 3. Read tokens -> atoms (
// 4. Sort tokens to do combinations
// 5. Find combinations
// 6. Sort results, filter results
// 7. Expand poi categories if needed

////////////// TODO THINK OPTIMIZATIONS /////////////
// 1. PARTIAL SEARCH. Perform equals search and then with '.'
// 2. MAPS. Do search first with closest maps and then with others
// 3. ALL COMBINATIONS. Stop on one combination or find all
// 4. POI CATEGORIES. ? 
// 5. READ_ALL. Switch ALWAYS_READ_COMMON_WORDS_ATOMS=true (new results + school intersections)
// 6. OPTIMIZE POI READ. Read only 1 POI in block
public class SpatialTextSearch {

	private static final int LIMIT_PRINT = 300;

	public static class SpatialTextSearchSettings {

		public static boolean SEARCH_ADDR = true;
		public static boolean SEARCH_POI = true;

		// Deduplicate results in the end by checking osm id of the first object in combination
		public static boolean DEDUPLICATE_RES = true;

		// READ OBJECTS before intersection to reduce number of duplicates from
		// different maps by osm id - needs to be tested performance mostly slows down
		public static boolean READ_ADDR_OBJECTS = false;
		public static boolean READ_POI_OBJECTS = false;

		// no need to find 3 street intersection or 3 POI intersection
		public static int LIMIT_ATOMIC_OBJECTS = 2;

		// Performance improvement assuming for rare words we don't read common atoms (school on street)
		public static boolean ALWAYS_READ_COMMON_WORDS_ATOMS = false;
		public static boolean ALWAYS_READ_FREQ_WORDS_ATOMS = true;

		// Limit evaluation intersection for unique objects
		public static int LIMIT_ALL_GOALS_MAX_UNIQUE_OBJECTS = 1000;
		// if there are >= 10 results matching 5 words, 4 words match won't be considered
		public static int LIMIT_GOAL_NEXT_LEVEL_MAX_UNIQUE_OBJECTS = 1; // could be 3
		// don't go level-2 if there are on level matching results
		public static int LIMIT_GOAL_LEVEL_2 = 1;
		
	}

	public static class SpatialSearchFileCache {
		public int fileInd = -1; // changing each session - not concurrent !!!
		public int indexInd = -1; // changing each session - not concurrent !!!
		public final String file;
		public final long length;
		public final long edition;
		public final List<NameIndexReader> indexReaders = new ArrayList<NameIndexReader>();

		public SpatialSearchFileCache(BinaryMapIndexReader r) {
			file = r.getFile().getName();
			length = r.getFile().length();
			edition = r.getDateCreated();
			for (AddressRegion a : r.getAddressIndexes()) {
				indexReaders.add(new NameIndexReader(a));
			}
			for (PoiRegion a : r.getPoiIndexes()) {
				indexReaders.add(new NameIndexReader(a));
			}
		}

		public boolean test(BinaryMapIndexReader r) {
			return r.getFile().getName().equals(file) && r.getFile().length() == length
					&& r.getDateCreated() == edition;
		}
	}

	public static class SpatialSearchGlobalCache {

		public Map<String, SpatialSearchFileCache> filesCache = new HashMap<>();

	}

	public static class SpatialSearchResults {

		public String input;

		public List<SpatialSearchToken> tokens;

		public List<SpatialSearchResult> mainResults;

		public List<SpatialSearchResultsList> combinations;
	}

	SpatialSearchGlobalCache cache = new SpatialSearchGlobalCache(); // reusable between sessions

	private void sortTokens(List<SpatialSearchToken> tokens) {
		// sort from least atoms to do combinations as the most efficient
		Collections.sort(tokens, new Comparator<SpatialSearchToken>() {
			@Override
			public int compare(SpatialSearchToken o1, SpatialSearchToken o2) {
				int c1 = o1.atoms.size();
				int c2 = o2.atoms.size();
				if (c1 != c2) {
					return Integer.compare(c1, c2);
				}
				return o1.word.compareTo(o2.word);
			}

		});
		for (int i = 0; i < tokens.size(); i++) {
			tokens.get(i).sortedOrder = i;
		}
	}

	/**
	 * For [1, 2, 3, 4] Tokens evaluate with cache (- no cache, +in cache) longest chain 
	 * 1. Goal [1, 2, 3, 4]: -[1, 2], -[1, 2, 3], -[1, 2, 3, 4] 
	 * 2. Goal [1, 2, 3]: +[1, 2], +[1, 2, 3] 
	 * 3. Goal [1, 2, 4]: +[1, 2], -[1, 2, 4] 
	 * 4. Goal [1, 3, 4]: -[1, 3], -[1, 3, 4] 
	 * 5. Goal [2, 3, 4]: -[2, 3], -[1, 3, 4] 
	 * 6. Goal [1, 2]: +[1, 2] 
	 * 7. Goal [1, 3]: -[1, 3] ... 
	 * Once goal has enough results whole iteration stopped
	 * @param ctx
	 * @return
	 */
	List<SpatialSearchResultsList> findLongestCombinations(SpatialSearchContext ctx, List<SpatialSearchToken> tokens)
			throws IOException {
		List<SpatialSearchResultsList> fullResult = new ArrayList<SpatialSearchResultsList>();
		BitSet mainGoal = new BitSet();
		mainGoal.set(0, tokens.size());

		SpatialSearchResultsList root = new SpatialSearchResultsList();

		Map<BitSet, SpatialSearchResultsList> cache = new HashMap<BitSet, SpatialSearchResultsList>();

		int ind = 0;
		for (SpatialSearchToken t : tokens) {
			BitSet b = new BitSet();
			b.set(ind++);
			cache.put(b, new SpatialSearchResultsList(t, root));
		}

		LinkedList<BitSet> goals = new LinkedList<>();
		HashSet<BitSet> evaluated = new HashSet<>();
		goals.add(mainGoal);

		int uniqueObjects = 0;
		int depth = mainGoal.length();
		int maxDepth = 0;
		while (!goals.isEmpty()) {
			BitSet goal = goals.removeFirst();
			if (!evaluated.add(goal)) {
				continue;
			}
			// stop on level - 2
			if (maxDepth == 0) {
				if (uniqueObjects >= SpatialTextSearchSettings.LIMIT_GOAL_LEVEL_2) {
					maxDepth = depth;
				}
			} else if (goal.length() <= maxDepth - 2) {
				break;
			}
			// stop with condition on level - 1
			if (goal.length() < depth) {
				if (SpatialTextSearchSettings.LIMIT_GOAL_NEXT_LEVEL_MAX_UNIQUE_OBJECTS > 0
						&& uniqueObjects >= SpatialTextSearchSettings.LIMIT_GOAL_NEXT_LEVEL_MAX_UNIQUE_OBJECTS) {
					break;
				}
				depth = goal.length();
			}

			SpatialSearchResultsList goalRes = cache.get(goal);
//			System.out.println("EVALUATE GOAL " + goal + " " + (goalRes == null));
			if (goalRes == null) {
				BitSet eval = new BitSet();
				goalRes = root;
				for (int i = goal.nextSetBit(0); i >= 0; i = goal.nextSetBit(i + 1)) {
					SpatialSearchToken token = tokens.get(i);
					eval.set(i);
					if (!cache.containsKey(eval)) {
						goalRes = new SpatialSearchResultsList(token, goalRes);
//						System.out.println("  EVALUATE STEP " + eval + " " + goalRes);
						cache.put((BitSet) eval.clone(), goalRes);
					} else {
						goalRes = (SpatialSearchResultsList) cache.get(eval);
//						System.out.println("  <CACHE> STEP " + eval + " " + goalRes);
					}
				}
			}
			if (goalRes.getCombinations() > 0) {
				ctx.stats.atoms -= System.nanoTime();
				goalRes.loadObjects(ctx);
				ctx.stats.atoms += System.nanoTime();
				List<SpatialSearchResult> res = goalRes.sortResults(ctx, true);
				uniqueObjects += res.size();
				System.out.println(goalRes);
				fullResult.add(goalRes);
				if (SpatialTextSearchSettings.LIMIT_ALL_GOALS_MAX_UNIQUE_OBJECTS > 0
						&& uniqueObjects >= SpatialTextSearchSettings.LIMIT_ALL_GOALS_MAX_UNIQUE_OBJECTS) {
					break;
				}
			}
			BitSet nextGoal = (BitSet) goal.clone();
			for (int i = nextGoal.length(); (i = nextGoal.previousSetBit(i - 1)) >= 0;) {
				nextGoal.set(i, false);
				if (!nextGoal.isEmpty()) {
//					System.out.println("  <PUSH> GOAL " + nextGoal);
					goals.add((BitSet) nextGoal.clone());
				}
				nextGoal.set(i, true);
			}
		}
		return fullResult;
	}

	List<SpatialSearchResultsList> findObjCombinationsSimpleIteration(List<SpatialSearchToken> tokens) {
		LinkedList<SpatialSearchResultsList> candidates = new LinkedList<>();
		candidates.add(new SpatialSearchResultsList());
		List<SpatialSearchResultsList> result = new ArrayList<>();
//		System.out.println("TOKENS " + tokens);

		while (!candidates.isEmpty()) {
			SpatialSearchResultsList parent = candidates.removeLast();
			if (parent.getCombinations() > 0) {
				result.add(parent);
			}
			for (int k = tokens.size() - 1; k >= 0; k--) {
//			for (SpatialSearchToken token : tokens) {
				SpatialSearchToken token = tokens.get(k);
				if (parent.getTokenCount() == 0 || token.sortedOrder < parent.getFirstToken().sortedOrder) {
					SpatialSearchResultsList next = new SpatialSearchResultsList(token, parent);
//					next.calculateIntersection(token, parent);
//					System.out.printf("ITERATION Token [%s] + {%s} = {%s}\n", token, parent, next);
					candidates.push(next);
				}
			}
		}
		return result;

	}

	public SpatialSearchResults searchAPI(String input, SpatialSearchContext ctx) throws IOException {
		SpatialSearchResults res = new SpatialSearchResults();
		ctx.initFiles(cache);
		res.input = input;
		// 1. prepare tokens
		res.tokens = splitWords(input);

		// 2. read atoms
		ctx.stats.atoms -= System.nanoTime();
		ctx.readAtoms(res.tokens);
		ctx.stats.atoms += System.nanoTime();

		// 3. sort tokens
		sortTokens(res.tokens);

		// 4. find combinations
		ctx.stats.computeTime -= System.nanoTime();
//		res.combinations = findObjCombinationsSimpleIteration(res.tokens);
		res.combinations = findLongestCombinations(ctx, res.tokens);
		ctx.stats.computeTime += System.nanoTime();
		// 5. sort combinations, load objects, objects and filter duplicate
		if (res.combinations.size() > 0) {
			res.mainResults = new ArrayList<>();
			SpatialSearchResultsList main = res.combinations.get(0);
			for (SpatialSearchResultsList m : res.combinations) {
				List<SpatialSearchResult> lst = m.getFinalResult();
				if (lst == null) {
					lst = m.sortResults(ctx, true);
				}
				res.mainResults.addAll(lst);
			}
			res.mainResults = main.sortResults(ctx, res.mainResults, SpatialTextSearchSettings.DEDUPLICATE_RES);
		}
		return res;
	}

	public List<SpatialSearchToken> splitWords(String input) {
		List<String> owords = new ArrayList<String>();
		// split by hyphen as we supposed to index them separately
		List<String> words = SearchAlgorithms.splitAndNormalize(input, owords, false);
		List<SpatialSearchToken> tokens = new ArrayList<>();
		for (int order = 0; order < words.size(); order++) {
			String w = words.get(order);
			SpatialSearchToken token = new SpatialSearchToken(w, owords.get(order), order);
			tokens.add(token);
		}
		return tokens;
	}

	public void searchTest(String input, SpatialSearchContext ctx) throws IOException {
		SpatialSearchResults res = searchAPI(input, ctx);
		ctx.stats.finish();
		if (res.mainResults != null) {
			System.out.println("--------");
			System.out.println("Main: " + res.combinations.get(0));
			int limit = LIMIT_PRINT;
			int all = res.mainResults.size();
			for (SpatialSearchResult r : res.mainResults) {
				if (limit-- < 0) {
					System.out.println(".............");
					break;
				}
				System.out.println(r.matchedTokens() + " " + r);
			}
			System.out.printf("------ ALL %d results ------- \n ", all);
			System.out.println("---------------------------------------");
		}

		System.out.println("\nTokens: " + res.tokens);
		System.out.printf("All Combinations - %d: \n", res.combinations.size());
		for (SpatialSearchResultsList s : res.combinations) {
			if (s.getTokenCount() >= 2) {
				s.sortResults(ctx, true);
				System.out.println("  " + s.toString(false));
//				int limit = LIMIT_PRINT;
//				for (SpatialSearchResult r : s.getResult()) {
//					if (limit-- < 0) {
//						System.out.println(".............");
//						break;
//					}
//					System.out.println(r);
//				}
			}
		}

		System.out.println(ctx.stats);
		System.out.println();
	}

	private static void initFile(List<BinaryMapIndexReader> ls, File f) throws IOException, FileNotFoundException {
		if (f.exists() && (f.getName().endsWith(".obf") || f.getName().equals(OsmandRegions.REGIONS_OCBF))) {
			BinaryMapIndexReader bir = new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f);
			ls.add(bir);
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		SpatialTextSearchSettings.DEDUPLICATE_RES = true;
		File folder = new File("/Users/victorshcherb/osmand/maps/");
		String pattern = "Germany_b";
		String query = "Berlin hauptstrasse"; // slow
		query = "Kelterstraße Kernen im Remstal";
		query = "Germany Kelter. Kernen im Remstal";

		pattern = "Us_";
		query = "Salt Lake City Pennsylvania Place UT USA";
//		query = "Salt Lake City Lake";
//		query = "Salt Lake City Pennsylvania Street";
//		query = "Salt Lake City";
//		query = "USA Salt Lake City Pennsylvania Street 41";
//		query = "Pennsylvania Avenue Pennsylvania USA"; // 31372516
//		query = "Pennsylvania Avenue Philadelphia Pennsylvania USA"; 
//		query = "Pennsylvania Avenue Philadelphia Philadelphia County Pennsylvania USA";
//		query = "Pennsylvania Avenue White Oak Allegheny County Pennsylvania USA"; // 11947214
//		query ="Township";

		pattern = "Liechtenstein_europe.obf";
		query = "Vaduz Lettstrasse";
		query = "Vaduz ";
		query = "Jugendheim Malbun";

		pattern = "Netherlands_";
		query = "1186RM Logger 387";
		query = "Farm";
		
		pattern = "Ukraine_";
//		pattern = "Map";
//		query = "нова пошта Бульварно Кудрявська";
//		query = "Бульварно-кудрявс.";
//		query = "Ukraine kyiv saks.";
//		query = "пузата хата mcdonal.";
//		query = "Нова пошта 3 харків";
//		query = "2-га Нова вулиця"; // unit test
//		query = "2 Нова вулиця"; // unit test
//		query = "саксаг.";
		query = "25 Школа володимирська вулиця"; // ALWAYS_READ_COMMON_WORDS_ATOMS = true or show category (centre ?) ! 
		query = "андріівський узвіз Школа "; // ALWAYS_READ_COMMON_WORDS_ATOMS = true
//		query = "Школа А+";
//		query = "школа 25"; // test '№25', '25'? -- 'школа', 'школа №25', 'школа 25'
		

//		pattern = "Spain_aragon_europe_";
//		query = "Basílica de Nuestra Señora del Pilar";
//		query = "Catedral-Basílica de Nuestra Señora del Pilar"; // 7 words! 2^7 combinations

		long t = System.nanoTime();

		List<BinaryMapIndexReader> ls = new ArrayList<BinaryMapIndexReader>();
		for (File f : folder.listFiles()) {
			if (f.getName().startsWith(pattern) || f.getName().equals(OsmandRegions.REGIONS_OCBF)) {
				initFile(ls, f);
			}
		}
		SpatialTextSearch a = new SpatialTextSearch();
		System.out.println(String.format("Index files %.1f ms", (System.nanoTime() - t) / 1e6));

		SpatialSearchContext searchContext = new SpatialSearchContext(ls , null);
		a.searchTest(query, searchContext);

		searchContext = new SpatialSearchContext(ls, null);
		SpatialTextSearchSettings.ALWAYS_READ_COMMON_WORDS_ATOMS = true;
		a.searchTest(query, searchContext);
	}

	public static void mainTest(String[] subArgsArray) throws FileNotFoundException, IOException {
		long t = System.nanoTime();
		String query = subArgsArray[0];
		List<BinaryMapIndexReader> ls = new ArrayList<BinaryMapIndexReader>();
		for (int i = 1; i < subArgsArray.length; i++) {
			File fl = new File(subArgsArray[i]);
			if (fl.isFile()) {
				if (i == 1) {
					initFile(ls, new File(fl.getParentFile(), OsmandRegions.REGIONS_OCBF));
				}
				initFile(ls, fl);
			} else {
				for (File f : fl.listFiles()) {
					initFile(ls, f);
				}
			}
		}
		System.out.println(String.format("Index files %.1f ms", (System.nanoTime() - t) / 1e6));
		SpatialTextSearch a = new SpatialTextSearch();
		SpatialSearchContext searchContext = new SpatialSearchContext(ls, null);
		a.searchTest(query, searchContext);
	}

}