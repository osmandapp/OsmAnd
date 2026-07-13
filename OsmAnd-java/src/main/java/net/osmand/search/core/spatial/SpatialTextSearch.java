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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.NameIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

//////////////// SEARCH ALGORITHM //////////////////
// 1. Init files + read caches
// 2. Split tokens
// 3. Read tokens -> atoms (
// 4. Sort tokens to do combinations
// 5. Find combinations
// 6. Sort results, filter results
// 7. Expand poi categories if needed

////////////// FUTURE OPTIMIZATIONS ////////////////
// 1. PARTIAL SEARCH. Perform equals search and then with '.'
// 2. MAPS. Do search first with closest maps and then with others
// 3. ALL COMBINATIONS. Stop on one combination or find all
// 4. POI CATEGORIES. -? 
// 5. READ_ALL. Switch ALWAYS_READ_COMMON_WORDS_ATOMS=true (new results + school intersections)
// 6. OPTIMIZE POI READ. Read only 1 POI in block
////////////////////////////////////////////////////

public class SpatialTextSearch {

	public static class SpatialTextSearchSettings {
		
		// lang to deduplicate results
		public String LANG_DEDUPLICATE = ""; 

		public boolean SEARCH_ADDR = true;
		public boolean SEARCH_POI = true;
		public boolean SEARCH_BUILDINGS = true;
		public boolean SEARCH_STREET_INTERSECTIONS = true;
		public boolean SEARCH_POI_INTERSECTIONS = true;
		public boolean SEARCH_POI_CATEGORIES = true;
		// no intersection recorded but streets are nearby
		public boolean ALLOW_VIRTUAL_STREET_INTERSECTIONS = true;
		
		public int[] OPTIM_LIMIT_RADIUS = new int[] {10_000, 30_000, 80_000}; // 
//		public int[] OPTIM_LIMIT_RADIUS = new int[] {}; 
		public int OPTIM_LIMIT_INTERSECTIONS = 30_000; // 10K (fast enough) or 50K (slow) - in new york  26,630 (3) -> 2,502 unique
		
		// produces x10 less intersection and maintains x2-x4 ratio for DEDUPLICATE_RES
		// by deleting embedded or duplicate boundaries in each other
		public boolean OPTIM_DELETE_EMBEDDED_BOUNDARIES = true;
		
		// In case POI is called 'Bratislava' it will be restricted to be searched as POIxPOI, POIxStreet
		// Related frequent POIs like "City&Bike 4th Street..." or public transport stops
		public boolean OPTIM_FLAG_POI_SAME_AS_CITY_STREET = true;
		public boolean OPTIM_DELETE_POI_SAME_AS_CITY_STREET = false; // not correct for new york the plaza
		
		// max prefixes for each name reader
		public int AUTO_CLEAR_PREFIX_CACHE_LIMIT = 1000;

		// Deduplicate results in the end by checking osm id of the first object in combination
		public boolean DEDUPLICATE_RES = true;
		
		// we need to test performance and results (we need to turn on for <POI + Address> search)
		public boolean TEST_ALLOW_HOUSE_POI_TYPE_INTERSECTION = true;

		// READ OBJECTS before intersection to reduce number of duplicates from
		// different maps by osm id - needs to be tested performance mostly slows down
		// ! Potential issue READ_ADDR_OBJECTS could deduplicate streets and 
		//  building won't be found in case same street in cities
		public boolean DEV_READ_ADDR_OBJECTS = false;
		public boolean DEV_READ_POI_OBJECTS = false;

		
		// display only top 10
		public int LIMIT_POI_CATEGORY_BY_FREQ = 15;
		// print some poi cat
		public int DEV_PRINT_POI_CAT_LIMIT = 0; // 10
		public int DEV_PRINT_POI_CAT_RADIUS_KM = 10;
		
		// no need to find 3 street intersection or 3 POI intersection
		public int LIMIT_ATOMIC_OBJECTS = 2;

		// TODO incomplete Performance improvement 
		// 1. If object does have rare words and they are not in query - skip it 
		//    Automatically implemented for common via index, for frequent disabled for now
		// 2. If object does have other common words and they are not in query - skip it
		// Problem search: School On Street - some schools have specifiers and some don't   
		public boolean OPTIM_READ_COMMON_WORDS_ATOMS = true;

		// Limit evaluation intersection for unique objects
		public int LIMIT_ALL_GOALS_MAX_UNIQUE_OBJECTS = 1000;
		// if there are >= 10 results matching 5 words, 4 words match won't be considered
		public int LIMIT_GOAL_NEXT_LEVEL_MAX_UNIQUE_OBJECTS = 1; // could be 3
		// don't go level-2 if there are on level matching results
		public int LIMIT_GOAL_LEVEL_2 = 1;
		
		// Hide results under SHOW MORE
		public int[] SHOW_MORE_WORDS_COUNT = new int[] {3, 20, 100};
		
		// only do incomplete search with 2+ chars
		public int MIN_CHARACTERS_INCOMPLETE = 2;
		
		public int MIN_ELO_RATING = 1400; // see SearchResult.MIN_ELO_RATING
//		public int MAX_ELO_RATING = 4300; // not used now
		
		// > 300 km - x0, for 50km-300km - x0.5, 10-50km - x1.5, 10km - x3sorted!
		public Map<Integer, Double> ENLARGE_BOUNDARIES = new TreeMap<Integer, Double>(
				Map.of(-300_000, 0.2, -100_000, 0.5, -10_000, 1.0, -1_000, 20.0));
		
		public double evalEnlargeBoundary(Map<Integer, Double> mp, double dim) {
			Iterator<Entry<Integer, Double>> it = mp.entrySet().iterator();
			double val = 0;
			while (it.hasNext()) {
				Entry<Integer, Double> e = it.next();
				if (dim > -e.getKey()) {
					break;
				}
				val = e.getValue();
			}
			return val;
		}
		
	}

	public static class SpatialSearchFileCache {
		public int fileInd = -1; // changing each session - not concurrent !!!
		public int indexInd = -1; // changing each session - not concurrent !!!
		public final String file;
		public final long length;
		public final long edition;
		public final List<NameIndexReader> indexReaders = new ArrayList<NameIndexReader>();
		public Map<String, Integer> poiFrequencies = null;
		public SpatialPoiSearch poiSearch;

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
		
		public SpatialSearchResult getFirstResult() {
			return mainResults == null || mainResults.size() == 0 ? null : 
				mainResults.get(0);
		}
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
			cache.put(b, new SpatialSearchResultsList(ctx, t, root));
			ctx.stats.tokenObjs += t.atoms.size();
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
				if (uniqueObjects >= ctx.settings.LIMIT_GOAL_LEVEL_2) {
					maxDepth = depth;
				}
			} else if (goal.length() <= maxDepth - 2) {
				break;
			}
			// stop with condition on level - 1
			if (goal.length() < depth) {
				if (ctx.settings.LIMIT_GOAL_NEXT_LEVEL_MAX_UNIQUE_OBJECTS > 0
						&& uniqueObjects >= ctx.settings.LIMIT_GOAL_NEXT_LEVEL_MAX_UNIQUE_OBJECTS) {
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
						goalRes = new SpatialSearchResultsList(ctx, token, goalRes);
						ctx.stats.maxCombinations = Math.max(ctx.stats.maxCombinations, goalRes.getCombinations()); 
//						System.out.println("  EVALUATE STEP " + eval + " " + goalRes);
						cache.put((BitSet) eval.clone(), goalRes);
					} else {
						goalRes = (SpatialSearchResultsList) cache.get(eval);
//						System.out.println("  <CACHE> STEP " + eval + " " + goalRes);
					}
				}
			}
			goalRes.loadObjectsAndCalcBuildings(ctx);
			List<SpatialSearchResult> res = goalRes.sortResults(ctx, ctx.settings.DEDUPLICATE_RES);
			if (goal.equals(mainGoal) && res.size() == 0) {
				goalRes = reevalWithExtendedBoundary(ctx, goal, tokens);
				goalRes.loadObjectsAndCalcBuildings(ctx);
				res = goalRes.sortResults(ctx, ctx.settings.DEDUPLICATE_RES);
			}
			if (res.size() > 0) {
				uniqueObjects += res.size();
				fullResult.add(goalRes);
				if (ctx.settings.LIMIT_ALL_GOALS_MAX_UNIQUE_OBJECTS > 0
						&& uniqueObjects >= ctx.settings.LIMIT_ALL_GOALS_MAX_UNIQUE_OBJECTS) {
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

	private SpatialSearchResultsList reevalWithExtendedBoundary(SpatialSearchContext ctx, BitSet goal, List<SpatialSearchToken> tokens) throws IOException {
		// Extend boundary for united states addresses (use 50 km radius)
		int enlarge = 0;
		for (SpatialSearchToken t : tokens) {
			for (NameIndexAtom a : t.atoms) {
				if (a.isBoundary() || a.isCityVillage()) {
					double val = ctx.settings.evalEnlargeBoundary(ctx.settings.ENLARGE_BOUNDARIES, 
							a.coords.dimensionInM());
					if (val > 0) {
//						System.out.println("Enlarge " + a.name + " " + a.type + " x" + val);
						t.enlargeBbox(a, val);
						enlarge++;
					}
				}
			}
		}
		if (ctx.stats.printLogs) { 
			System.out.println("Enlarged boundaries " + enlarge);
		}
		SpatialSearchResultsList goalRes = new SpatialSearchResultsList();
		for (int i = goal.nextSetBit(0); i >= 0; i = goal.nextSetBit(i + 1)) {
			SpatialSearchToken token = tokens.get(i);
			goalRes = new SpatialSearchResultsList(ctx, token, goalRes);
		}
		return goalRes;
	}

	List<SpatialSearchResultsList> findObjCombinationsSimpleIteration(SpatialSearchContext ctx, List<SpatialSearchToken> tokens) {
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
					SpatialSearchResultsList next = new SpatialSearchResultsList(ctx, token, parent);
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
		res.tokens = splitWords(ctx, input);
		
		// 2. read atoms & poi categories
		ctx.stats.step1Atoms.start();
		ctx.setTokens(res.tokens);
		ctx.processPoiCategories();
		ctx.readAtoms();
		ctx.stats.step1Atoms.finish();

		// 3. sort tokens
		sortTokens(res.tokens);

		// 4. find combinations
		ctx.stats.step2Compute.start();
//		res.combinations = findObjCombinationsSimpleIteration(res.tokens);
		res.combinations = findLongestCombinations(ctx, res.tokens);
		ctx.stats.step2Compute.finish();
		// 5. sort combinations, load objects, objects and filter duplicate
		res.mainResults = new ArrayList<>();
		ctx.stats.step3Sort.start();
		if (res.combinations.size() > 0) {
			combineSortFilterResults(ctx, res);
		}
		ctx.stats.step3Sort.finish();
		return res;
	}

	private void combineSortFilterResults(SpatialSearchContext ctx, SpatialSearchResults res) throws IOException {
		SpatialSearchResultsList main = res.combinations.get(0);
		for (SpatialSearchResultsList m : res.combinations) {
			List<SpatialSearchResult> lst = m.getFinalResult();
			if (lst == null) {
				lst = m.sortResults(ctx, ctx.settings.DEDUPLICATE_RES);
			}
			res.mainResults.addAll(lst);
		}
		res.mainResults = main.sortResults(ctx, res.mainResults, ctx.settings.DEDUPLICATE_RES);
		int limitPoiCat = ctx.settings.DEV_PRINT_POI_CAT_LIMIT;
		if (res.mainResults.size() > 0) {
			int[] limits = ctx.settings.SHOW_MORE_WORDS_COUNT.clone();
			long cKey = SpatialSearchResult.compareKey(res.mainResults.get(0));
			int ind = 0, lind = 0;
			int level = 0; 
			for (SpatialSearchResult r : res.mainResults) {
				if (limitPoiCat > 0) {
					limitPoiCat = printPoiCategory(ctx, limitPoiCat, r);
				}
				long nextKey = SpatialSearchResult.compareKey(r);
				if (cKey != nextKey) {
					if (lind < limits.length && ind >= limits[lind]) {
						level++;
						ind = 0;
						if (lind < limits.length - 1) {
							lind++;
						}
					}
//					System.out.println(nextKey + " " + r);
					cKey = nextKey;
				}
				r.visibleLevel = level;
				ind++;
			}
		}
	}

	private int printPoiCategory(SpatialSearchContext ctx, int limitPoiCat, SpatialSearchResult r) throws IOException {
		if (r.getFirstRef() != null && r.getFirstRef().atom.isPoiCategory()) {
			long nt = System.nanoTime();
			System.out.printf("Loading poi type '%s' - limit %d...\n", r.getFirstRef().atom.name, limitPoiCat);
			LatLon latLon = r.getLatLon();
			List<Amenity> interRes = ctx.poiSearch.loadPOIObjects(ctx, r.getFirstRef().atom.id,
					latLon == null ? ctx.location : latLon, ctx.settings.DEV_PRINT_POI_CAT_RADIUS_KM * 1000, limitPoiCat);
			for (Amenity a : interRes) {
				double dist = ctx.location == null ? 0 : MapUtils.getDistance(ctx.location, a.getLocation());
				System.out.printf("\t %s (%s) %.2f km %s \n", a, a.getOsmId(), dist / 1000.0, a.getLocation());
			}
			System.out.printf("... Loaded %d pois %.1f ms (%.1f ms, %d tiles, %,d KB)\n", interRes.size(), 
					(System.nanoTime() - nt) / 1e6, ctx.stats.poiByTypeTime.ms(), ctx.stats.poiByTypeBboxes, 
					ctx.stats.poiByTypeBytes / 1024);
			limitPoiCat = 0;
		}
		return limitPoiCat;
	}

	public List<SpatialSearchToken> splitWords(SpatialSearchContext ctx, String input) {
		List<String> owords = new ArrayList<String>();
		// split by hyphen as we supposed to index them separately
		List<String> words = SearchAlgorithms.splitAndNormalize(input, owords, false);
		List<SpatialSearchToken> tokens = new ArrayList<>();
		for (int order = 0; order < words.size(); order++) {
			String w = words.get(order);
			SpatialSearchToken token = new SpatialSearchToken(ctx.settings.MIN_CHARACTERS_INCOMPLETE, w, owords.get(order), order);
			tokens.add(token);
		}
		return tokens;
	}

	public SpatialSearchResults searchTest(String input, SpatialSearchContext ctx, int limitPrint) throws IOException {
		ctx.stats.requestTime.start();
		SpatialSearchResults res = searchAPI(input, ctx);
		ctx.stats.requestTime.finish();
		if (res.mainResults != null && res.mainResults.size() > 0) {
			System.out.println("--------");
			System.out.printf("Main: %s\n", res.combinations.get(0));
			int all = res.mainResults.size();
			int level = 0;
			int sz = 0;
			for (SpatialSearchResult r : res.mainResults) {
				sz++;
				if (r.visibleLevel != level) {
					level++;
					System.out.printf("### %d - NEXT LEVEL %d (%s). "
							+ " Format - 75(words) 02(objects) 0(surplus) 1(sum other) 52(rating) 72(sum types)\n",
							sz, level, SpatialSearchResult.compareKeyString(r));
					sz = 0;
				}
				if (limitPrint-- < 0) {
					System.out.println(".............");
					break;
				}
				System.out.printf("Result %d (%s) - %s\n", r.matchedTokens(), SpatialSearchResult.compareKeyString(r), r);
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
		return res;
	}

	static void initFile(List<BinaryMapIndexReader> ls, File f) throws IOException, FileNotFoundException {
		if (f.exists() && (f.getName().endsWith(".obf") || f.getName().equals(OsmandRegions.REGIONS_OCBF))) {
			BinaryMapIndexReader bir = new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f);
			ls.add(bir);
		}
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
				if (!fl.getName().equals(OsmandRegions.REGIONS_OCBF)) {
					initFile(ls, fl);
				}
			} else {
				for (File f : fl.listFiles()) {
					initFile(ls, f);
				}
			}
		}
		System.out.println(String.format("Index files %.1f ms", (System.nanoTime() - t) / 1e6));
		SpatialTextSearch a = new SpatialTextSearch();
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(MapPoiTypes.getDefault());
		SpatialSearchContext searchContext = new SpatialSearchContext(new SpatialTextSearchSettings(), ls, poiSearch,
				null);
		a.searchTest(query, searchContext, 1000);
	}

}