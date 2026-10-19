package net.osmand.search.core.spatial;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiAdditionalFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiAdditionalLabels;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.search.core.TopIndexFilter;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchFileCache;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchGlobalCache;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

public class SpatialPoiSearch {

	final MapPoiTypes poiTypes;
	StringPrefixTree<SpatialPoiType> poiTypesIndex = new StringPrefixTree<>();
	ReentrantReadWriteLock poiTypesIndexLock = new ReentrantReadWriteLock();
	AtomicInteger ids = new AtomicInteger();
	Map<String, SpatialPoiType> byKey = new ConcurrentHashMap<>();
	Map<String, List<SpatialPoiType>> byWikidataKey = new ConcurrentHashMap<>();
	Map<Integer, SpatialPoiType> byId = new ConcurrentHashMap<>();
	// languages whose phrases are in the index: those of the maps searched so far
	Set<String> loadedLanguages = ConcurrentHashMap.newKeySet();

	public static class SpatialPoiType {
		public final AbstractPoiType singleType;
		public final String poiAdditional;
		public final PoiSubType poiSubType;
		final List<String> names = new ArrayList<String>();
		// language -> phrases of the language of the data of a map that are not already among the names
		final Map<String, List<String>> languageNames = new ConcurrentHashMap<>();
		final String key;
		final int id;
		int tokensInName = 0;
		boolean place;
		String wikidataId;
		List<AbstractPoiType> parentTypes;

		public SpatialPoiType(AbstractPoiType pt, int id) {
			this.singleType = pt;
			this.key = pt.getKeyName();
			this.id = id;
			this.poiAdditional = null;
			this.poiSubType = null;
		}
		
		public SpatialPoiType(String additional, String key, int id) {
			this(null, additional, key, id);
		}

		public SpatialPoiType(PoiSubType poiSubType, String additional, String key, int id) {
			this.singleType = null;
			this.key = key;
			this.id = id;
			this.poiAdditional = additional;
			this.poiSubType = poiSubType;
		}
		
		public boolean isPlace() {
			return place;
		}
		
		public String getWikidataId() {
			return wikidataId;
		}

		public String getKey() {
			return key;
		}

		public List<AbstractPoiType> getParentTypes() {
			return parentTypes;
		}

		public boolean accept(Amenity a) {
			if (key.equals(a.getType().getKeyName())) {
				return true;
			}
			// contains for ';'
			boolean multi = a.getSubType().indexOf(';') != -1;
			if (key.equals(a.getSubType()) 
					|| (multi && key.startsWith(a.getSubType() + ";"))
					|| (multi && key.endsWith(";" + a.getSubType()))
					|| (multi && key.contains(";" + a.getSubType() + ";"))) {
				return true;
			}
			return false;
		}

		public boolean addName(String name) {
			List<String> nms = SearchAlgorithms.split(name);
			if (tokensInName == 0 || nms.size() < tokensInName) {
				tokensInName = nms.size();
			}
			names.add(name);
			return true;
		}

		private boolean hasName(String name) {
			String aligned = SearchAlgorithms.alignChars(name);
			for (String n : names) {
				if (SearchAlgorithms.alignChars(n).equalsIgnoreCase(aligned)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Fewest words of a name of the type that has a word starting with one of the query words. A one-word synonym
		 * ("cistern", "temple") must not make a partial match of another name ("water" of "Water tank", "place" of
		 * "Place of worship") look complete.
		 */
		public int tokensInMatchedName(Collection<String> queryWords) {
			int min = tokensInMatchedName(names, queryWords, 0);
			// the phrases of the language of a map: "Wasserhahn" is a whole name of water_tap ("Water tap")
			for (List<String> langNames : languageNames.values()) {
				min = tokensInMatchedName(langNames, queryWords, min);
			}
			return min == 0 ? tokensInName : min;
		}

		private static int tokensInMatchedName(List<String> names, Collection<String> queryWords, int min) {
			for (String name : names) {
				List<String> words = SearchAlgorithms.split(name);
				boolean matched = false;
				for (String w : words) {
					String aligned = SearchAlgorithms.alignChars(w.toLowerCase());
					for (String q : queryWords) {
						matched |= aligned.startsWith(q);
					}
				}
				if (matched && (min == 0 || words.size() < min)) {
					min = words.size();
				}
			}
			return min;
		}
		
		@Override
		public String toString() {
			return key;
		}
		
	}

	public SpatialPoiSearch(MapPoiTypes types) {
		this.poiTypes = types;
		for (PoiCategory pc : poiTypes.getCategories()) {
			if (pc == poiTypes.getOtherMapCategory()) {
				continue;
			}
			addToIndex(pc, null);
			for (PoiFilter pt : pc.getPoiFilters()) {
				if (pt.isTopVisible()) {
					addToIndex(pt, null);
				}
			}
			for (PoiType pt : pc.getPoiTypes()) {
				if (pt.isReference()) {
					continue;
				}
				addToIndex(pt, null);
				for (PoiType add : pt.getPoiAdditionals()) {
					if (add.isTopVisible() && PoiAdditionalLabels.isAcceptedValue(add)) {
						addToIndex(add, pt);
					}
				}
			}
		}
	}


	private void addToIndex(AbstractPoiType pt, PoiType parent) {
		if (byKey.containsKey(pt.getKeyName())) {
			if (pt.isAdditional()) {
				byKey.get(pt.getKeyName()).parentTypes.add(parent); 
				return;
			} else {
				throw new IllegalStateException();
			}
		}
		String basePoiName = poiTypes.getBasePoiName(pt);
		SpatialPoiType poiType = new SpatialPoiType(pt, ids.getAndIncrement());
		if (pt instanceof PoiType poitype) {
			poiType.place = "place".equals(poitype.getOsmTag());
		}
		if (parent != null) {
			poiType.parentTypes = new ArrayList<>();
			poiType.parentTypes.add(parent);
		}
		if (!basePoiName.equals(pt.getTranslation())) {
			String[] split = pt.getTranslation().split(";");
			for (String tr : split) {
				poiType.addName(SearchAlgorithms.alignChars(tr.trim()));
				// only first
				break;
			}
		}
		String synonyms = pt.getSynonyms();
		if (!Algorithms.isEmpty(synonyms)) {
			String[] split = synonyms.split(";");
			for (String tr : split) {
				if (tr.trim().length() > 0) {
					poiType.addName(SearchAlgorithms.alignChars(tr.trim()));
				}
			}
		}
		addToIndex(basePoiName, poiType);
	}


	private void updateNamesWikidataId(SpatialPoiType poiType, String names) {
		WriteLock wl = poiTypesIndexLock.writeLock();
		try {
			wl.lock();
			String[] snames = names.split(";");
			if (poiType.wikidataId == null && snames[0].length() > 0) {
				poiType.wikidataId = snames[0];
				if (!byWikidataKey.containsKey(poiType.wikidataId)) {
					byWikidataKey.put(poiType.wikidataId, new ArrayList<>());
				}
				byWikidataKey.get(poiType.wikidataId).add(poiType);
			}
			for (int i = 1; i < snames.length; i++) {
				String nameToAdd = snames[i];
				if (!poiType.names.contains(nameToAdd)) {
					if (poiType.addName(nameToAdd)) {
						poiTypesIndex.put(snames[i], poiType);
					}
				}
			}
		} finally {
			wl.unlock();
		}
	}
	private void addToIndex(String basePoiName, SpatialPoiType poiType) {
		poiType.addName(basePoiName);
		WriteLock wl = poiTypesIndexLock.writeLock();
		try {
			wl.lock();
			SpatialSearchContext.checkPoiTypeId(poiType.id);
			byId.put(poiType.id, poiType);
			byKey.put(poiType.key, poiType);
			if (poiType.wikidataId != null && poiType.wikidataId.length() > 0) {
				if (!byWikidataKey.containsKey(poiType.wikidataId)) {
					byWikidataKey.put(poiType.wikidataId, new ArrayList<>());
				}
				byWikidataKey.get(poiType.wikidataId).add(poiType);
			}
			for (String name : poiType.names) {
				poiTypesIndex.put(name, poiType);
			}
		} finally {
			wl.unlock();
		}
	}

	public void init(SpatialSearchGlobalCache cache, SpatialSearchFileCache fc, BinaryMapIndexReader bir,
			PoiRegion poiRegion) {
		List<String> cats = poiRegion.getCategories();
		List<List<String>> subcategories = poiRegion.getSubcategories();
		TIntArrayList categoryFreqs = poiRegion.getCategoryFreqs();
		List<TIntArrayList> subcatFreqs = poiRegion.getSubcategoryFreqs();
		for (int i = 0; i < cats.size(); i++) {
			List<String> lst = subcategories.get(i);
			int f = categoryFreqs != null && i < categoryFreqs.size() ? categoryFreqs.get(i) : 0;
			fc.poiFrequencies.put(cats.get(i), f);
			for (int j = 0; j < lst.size(); j++) {
				int ft = subcatFreqs != null && i < subcatFreqs.size() && j < subcatFreqs.get(i).size() ? subcatFreqs.get(i).get(j) : 0;
				String vkey = lst.get(j);
				fc.poiFrequencies.put(vkey, ft);
			}
		}
		
		
		for (PoiSubType subType : poiRegion.getSubTypes()) {
			if (subType.text) {
				continue;
			}
			if (subType.isTopIndex()) {
				List<String> possibleValues = subType.possibleValues;
				List<String> wikidataIds = subType.wikidataIds;
				TIntArrayList possibleValuesFreqs = subType.possibleValuesFreqs;
				for (int k = 0; k < possibleValues.size(); k++) {
					String topValueName = possibleValues.get(k);
					String valueKey = TopIndexFilter.getValueKey(topValueName);
					String fullKey = subType.name + "_" + valueKey;
					String wikidataId = wikidataIds != null && k < wikidataIds.size() ? wikidataIds.get(k) : "";
					int freq = possibleValuesFreqs != null && k < possibleValuesFreqs.size() ? possibleValuesFreqs.get(k) : 0;
					if (possibleValuesFreqs == null && possibleValues.size() == 1) {
						// the writer stores per-value frequencies only for 2+ values; one value has the subtype total
						freq = subType.frequency;
					}
					SpatialPoiType topValue = byKey.get(fullKey);
					if (topValue == null) {
						topValue = new SpatialPoiType(subType, topValueName, fullKey, ids.getAndIncrement());
						if (wikidataId.length() > 0) {
							String[] otherNames = wikidataId.split(";");
							topValue.wikidataId = otherNames[0];
							for (int ts = 1; ts < otherNames.length; ts++) {
								topValue.addName(otherNames[ts]);
							}
						}
						addToIndex(topValueName, topValue);
					} else if (wikidataId != null && wikidataId.length() > 0) {
						updateNamesWikidataId(topValue, wikidataId);
					}
					Integer fit = fc.poiFrequencies.get(topValue.key);
					if (fit != null) {
						freq += fit;
					}
					fc.poiFrequencies.put(topValue.key, freq);
				}
			}
			SpatialPoiType indSubType = byKey.get(subType.name);
			if (indSubType == null) {
				// skip top level additional
				continue;
			}
			fc.poiFrequencies.put(indSubType.key, subType.frequency);
		}
	}

	/**
	 * Adds the phrases of a language to the index once: the names of the categories in the language of the data
	 * of a map ("de" for Austria_vienna, "fr" for France_ile-de-france), the way rules.xml gives the query forms of
	 * the locale of a map. A phrase the type already has among its names stays a name.
	 */
	void loadLanguage(String lang) {
		if (Algorithms.isEmpty(lang) || loadedLanguages.contains(lang)) {
			return;
		}
		WriteLock wl = poiTypesIndexLock.writeLock();
		try {
			wl.lock();
			if (loadedLanguages.contains(lang)) {
				return;
			}
			for (SpatialPoiType poiType : byKey.values()) {
				String phrases = poiType.singleType == null ? null : poiTypes.getPhrases(poiType.singleType, lang);
				if (Algorithms.isEmpty(phrases)) {
					continue;
				}
				List<String> names = new ArrayList<>();
				for (String phrase : phrases.split(";")) {
					String name = phrase.trim();
					if (!name.isEmpty() && !poiType.hasName(name) && !names.contains(name)) {
						names.add(name);
						poiTypesIndex.put(SearchAlgorithms.alignChars(name), poiType);
					}
				}
				if (!names.isEmpty()) {
					poiType.languageNames.put(lang, names);
				}
			}
			loadedLanguages.add(lang);
		} finally {
			wl.unlock();
		}
	}

	/**
	 * A phrase of the language of a map names the category only when the query has all its words in a row:
	 * "Preneur de paris" is a bookmaker on a French map, "Paris" is not. The names of the translator keep the
	 * match of any word ("tap" of "Water tap").
	 */
	private static Map<String, Integer> matchLanguagePhrases(List<String> phrases, SpatialSearchToken t,
			List<SpatialSearchToken> tokens, int rankShift, Map<String, Integer> matched) {
		if (phrases == null) {
			return matched;
		}
		int pos = tokens.indexOf(t);
		for (int p = 0; p < phrases.size(); p++) {
			if (inRow(SearchAlgorithms.splitAndNormalize(phrases.get(p), false), tokens, pos)) {
				matched = addPhrase(matched, phrases.get(p), rankShift + p);
			}
		}
		return matched;
	}

	/** the query has all the words of a phrase in a row, one of them is the word at pos */
	private static boolean inRow(List<String> words, List<SpatialSearchToken> tokens, int pos) {
		for (int j = 0; j < words.size(); j++) {
			int start = pos - j;
			if (start < 0 || start + words.size() > tokens.size()) {
				continue;
			}
			boolean all = true;
			for (int k = 0; k < words.size() && all; k++) {
				all = tokens.get(start + k).getMainCollator().matches(words.get(k));
			}
			if (all) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A name of several words that the query has whole, in a row, takes its words: "store" of "Hardware store" is
	 * the hardware category only, not one more of the shops named "... store" ("Liquor store", "Store") that any word
	 * of a name of the translator matches. Read so, the web search for "Bratislava Hardware store" found 4 shops
	 * instead of the 62 of "Bratislava hardware". A word alone keeps every category it names ("Store" is the shops).
	 * The name of an additional keeps the category it is of: "Fuel diesel" (fuel_diesel) leaves "fuel" the gas
	 * stations, so "Metro Gas &amp; Diesel" without the diesel tag is still found.
	 */
	private static void keepCategoriesOfWholeNames(Map<SpatialPoiType, PoiCatSearch> res,
			List<SpatialSearchToken> tokens) {
		// word -> the categories named by a whole name of several words that has it
		Map<SpatialSearchToken, Set<SpatialPoiType>> whole = new HashMap<>();
		// word -> languages of the maps those whole names hold for, null for every map
		Map<SpatialSearchToken, Set<String>> wholeLanguages = new HashMap<>();
		for (PoiCatSearch pc : res.values()) {
			if (pc.tokens.size() < 2) {
				continue;
			}
			for (int i = 0; i < pc.tokens.size(); i++) {
				SpatialSearchToken t = pc.tokens.get(i);
				for (String phrase : pc.phrases.get(i).keySet()) {
					List<String> words = SearchAlgorithms.splitAndNormalize(phrase, false);
					if (words.size() > 1 && inRow(words, tokens, tokens.indexOf(t))) {
						whole.computeIfAbsent(t, k -> new HashSet<>()).add(pc.pt);
						wholeLanguages.put(t, unionOfLanguages(wholeLanguages, t, pc.languages.get(i)));
						break;
					}
				}
			}
		}
		if (whole.isEmpty()) {
			return;
		}
		Iterator<PoiCatSearch> it = res.values().iterator();
		while (it.hasNext()) {
			PoiCatSearch pc = it.next();
			for (int i = pc.tokens.size() - 1; i >= 0; i--) {
				SpatialSearchToken t = pc.tokens.get(i);
				Set<SpatialPoiType> types = whole.get(t);
				// a whole name of German maps leaves the word its category on French maps ("Fitness-Zentrum")
				if (types != null && !types.contains(pc.pt) && !isAdditionalOf(types, pc.pt)
						&& coversLanguages(wholeLanguages.get(t), pc.languages.get(i))) {
					pc.tokens.get(i).removePoiCategoryMatch(pc.pt.id, pc.pt.key);
					pc.tokens.remove(i);
					pc.atoms.remove(i);
					pc.languages.remove(i);
					pc.phrases.remove(i);
				}
			}
			if (pc.tokens.isEmpty()) {
				it.remove();
			}
		}
	}

	/**
	 * Names come raw (top-index values, Wikidata names: "Поїхали з нами") or aligned (translator: "поіхали з нами"):
	 * one name is one key whatever its case and diacritics.
	 */
	private static Map<String, Integer> addPhrase(Map<String, Integer> matched, String phrase, int rank) {
		if (matched == null) {
			matched = new LinkedHashMap<>();
		}
		String key = Normalizer.normalize(SearchAlgorithms.alignChars(phrase).toLowerCase(), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		matched.merge(key, rank, Math::min);
		return matched;
	}

	/**
	 * Two words of the query that are different names of one category ("Church Friedhofskapelle": en "church" and de
	 * "friedhofskapelle" of religion_christian) say what the object is once and which one it is once: the category
	 * stays with the words of the most general name, the others are words of a name. Read as the category twice, the
	 * second word narrows nothing: the category alone covers the query and lists every church before the chapels
	 * named "Friedhofskapelle". Words of one name of the category ("Hardware store", "Preneur de paris") stay the
	 * category together. Different categories of one object ("Pizza restaurant", "Vegan restaurant") are not touched.
	 * <p>
	 * The most general name is the one most words of the query share, then the one listed first: the names of the
	 * translator before the phrases of the language of a map, and in each the translation and synonyms in their
	 * order in phrases.xml ("Kirche" before "friedhofskapelle").
	 */
	private static void keepCategoryNamedOnce(PoiCatSearch pc) {
		if (pc.tokens.size() < 2) {
			return;
		}
		Map<String, Integer> count = new LinkedHashMap<>();
		Map<String, Integer> rank = new LinkedHashMap<>();
		for (Map<String, Integer> phrases : pc.phrases) {
			for (Map.Entry<String, Integer> e : phrases.entrySet()) {
				count.merge(e.getKey(), 1, Integer::sum);
				rank.merge(e.getKey(), e.getValue(), Math::min);
			}
		}
		String best = null;
		for (String p : count.keySet()) {
			if (best == null || count.get(p) > count.get(best)
					|| (count.get(p).equals(count.get(best)) && rank.get(p) < rank.get(best))) {
				best = p;
			}
		}
		if (best == null || count.get(best) == pc.tokens.size()) {
			return;
		}
		// the maps the best name holds for: "Kino" (de) does not take "ciné" (fr) on French maps
		Map<String, Set<String>> bestLanguages = new HashMap<>();
		for (int i = 0; i < pc.tokens.size(); i++) {
			if (pc.phrases.get(i).containsKey(best)) {
				bestLanguages.put(best, unionOfLanguages(bestLanguages, best, pc.languages.get(i)));
			}
		}
		for (int i = pc.tokens.size() - 1; i >= 0; i--) {
			if (!pc.phrases.get(i).containsKey(best)
					&& coversLanguages(bestLanguages.get(best), pc.languages.get(i))) {
				pc.tokens.get(i).removePoiCategoryMatch(pc.pt.id, pc.pt.key);
				pc.tokens.remove(i);
				pc.atoms.remove(i);
				pc.languages.remove(i);
				pc.phrases.remove(i);
			}
		}
	}

	/** languages of a match joined with those kept for the key; null is every map and absorbs the others */
	private static <K> Set<String> unionOfLanguages(Map<K, Set<String>> kept, K key, Set<String> languages) {
		if (!kept.containsKey(key)) {
			return languages == null ? null : new LinkedHashSet<>(languages);
		}
		Set<String> res = kept.get(key);
		if (res == null || languages == null) {
			return null;
		}
		res.addAll(languages);
		return res;
	}

	/**
	 * A word loses a category only where the other name holds: the match is removed from the word for every map at
	 * once, so it stays when the word is the only name of the category on some of its maps.
	 */
	private static boolean coversLanguages(Set<String> covering, Set<String> languages) {
		return covering == null || (languages != null && covering.containsAll(languages));
	}

	/** one of the types is an additional of the category (fuel_diesel of fuel) */
	private static boolean isAdditionalOf(Set<SpatialPoiType> types, SpatialPoiType category) {
		for (SpatialPoiType t : types) {
			if (t.parentTypes != null && category.singleType != null && t.parentTypes.contains(category.singleType)) {
				return true;
			}
		}
		return false;
	}

	private record PoiCatSearch(SpatialPoiType pt, List<SpatialSearchToken> tokens, List<NameIndexAtom> atoms,
			// per token: languages of the maps the category is named for, null for all maps
			List<Set<String>> languages,
			// per token: the names of the category it matched -> rank (order of the name, see keepCategoryNamedOnce)
			List<Map<String, Integer>> phrases, int freq) implements Comparable<PoiCatSearch> {

		@Override
		public int compareTo(PoiCatSearch o) {
			int i1 = -Integer.compare(tokens.size(), o.tokens.size());
			if (i1 != 0) {
				return i1;
			}
			return -Integer.compare(freq, o.freq);
		}
	}
	
	private void addPoiCategoryMatch(SpatialSearchContext ctx, SpatialPoiType a, SpatialSearchToken t,
			Set<String> languages, Map<String, Integer> phrases, Map<SpatialPoiType, PoiCatSearch> res) {
		int total = 0;
		for (SpatialSearchFileCache l : ctx.internalFile) {
			if (languages != null && !languages.contains(t.globalRules.locales().language(l.locale))) {
				continue;
			}
			if (l.poiFrequencies != null) {
				Integer freq = l.poiFrequencies.get(a.key);
				if (freq != null) {
					total += freq;
				}
				if (a.singleType instanceof PoiFilter pf) {
					for (PoiType p : pf.getPoiTypes()) {
						freq = l.poiFrequencies.get(p.getKeyName());
						if (freq != null) {
							total += freq;
						}
					}
				}
				// additional could be on top
//				if (a.parentTypes != null) {
//					for (AbstractPoiType p : a.parentTypes) {
//						freq = l.poiFrequencies.get(p.getKeyName());
//						if (freq != null) {
//							total += freq;
//						}
//					}
//				}
			}
		}
//		System.out.println(a.names + " " + a.key + " " + total);
		PoiCatSearch cs = res.get(a);
		if (cs == null) {
			cs = new PoiCatSearch(a, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), total);
			res.put(a, cs);
		}
		int existing = cs.tokens.indexOf(t);
		if (existing >= 0) {
			// matched again (its own name and a name of a type with the same Wikidata id): both are its names
			Map<String, Integer> merged = new LinkedHashMap<>(cs.phrases.get(existing));
			phrases.forEach((p, r) -> merged.merge(p, r, Math::min));
			cs.phrases.set(existing, merged);
			return;
		}

		NameIndexAtom atom = new NameIndexAtom(a.key, a.id, total);
		cs.atoms.add(atom);
		cs.tokens.add(t);
		cs.languages.add(languages);
		cs.phrases.add(phrases);
		t.setPoiCategoryLanguages(a.id, a.key, languages);
		if (typedInFull(t, a, languages)) {
			t.addPoiCategoryMatch(a.id);
		}
	}

	// a category matched by a word still being typed is only a row: "ca" does not list every catholic church
	private static boolean typedInFull(SpatialSearchToken t, SpatialPoiType a, Set<String> languages) {
		if (!t.incomplete) {
			return true;
		}
		if (typedInFull(t, a.names)) {
			return true;
		}
		if (languages != null) {
			for (String lang : languages) {
				if (typedInFull(t, a.languageNames.get(lang))) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean typedInFull(SpatialSearchToken t, List<String> names) {
		if (names == null) {
			return false;
		}
		for (String n : names) {
			for (String w : SearchAlgorithms.splitAndNormalize(n, false)) {
				if (SearchAlgorithms.alignChars(w).equals(t.wordNoDot)) {
					return true;
				}
			}
		}
		return false;
	}

	public int getCategoryFrequency(SpatialSearchContext ctx, String categoryKey) {
		SpatialPoiType a = getByKey(categoryKey);
		if (a == null) {
			return 0;
		}
		int total = 0;
		for (SpatialSearchFileCache l : ctx.internalFile) {
			if (l.poiFrequencies != null) {
				Integer freq = l.poiFrequencies.get(a.key);
				if (freq != null) {
					total += freq;
				}
				if (a.singleType instanceof PoiFilter pf) {
					for (PoiType p : pf.getPoiTypes()) {
						freq = l.poiFrequencies.get(p.getKeyName());
						if (freq != null) {
							total += freq;
						}
					}
				}
			}
		}
		return total;
	}

	public void processPoiCategories(SpatialSearchContext ctx, List<SpatialSearchToken> tokens) {
		Set<String> languages = new LinkedHashSet<>();
		for (SpatialSearchFileCache fc : ctx.internalFile) {
			if (!tokens.isEmpty()) {
				String lang = tokens.get(0).globalRules.locales().language(fc.locale);
				if (!Algorithms.isEmpty(lang)) {
					languages.add(lang);
				}
			}
		}
		processPoiCategories(ctx, tokens, languages);
	}

	/**
	 * @param languages languages of the data of the maps searched (rules locale of the map): their phrases name
	 *                  categories only for the objects of those maps
	 */
	void processPoiCategories(SpatialSearchContext ctx, List<SpatialSearchToken> tokens, Set<String> languages) {
		for (String lang : languages) {
			loadLanguage(lang);
		}
		Map<SpatialPoiType, PoiCatSearch> res = new LinkedHashMap<>();
		for (SpatialSearchToken t : tokens) {
			ReadLock readLock = poiTypesIndexLock.readLock();
			List<SpatialPoiType> poiTypes;
			try {
				readLock.lock();
				poiTypes = poiTypesIndex.match(t.getPrefixMatcher(ctx.stats));
			} finally {
				readLock.unlock();
			}
			for (SpatialPoiType a : poiTypes) {
				// names of the translator rank by their order, the phrases of the language of a map after them
				Map<String, Integer> phrases = null;
				for (int k = 0; k < a.names.size(); k++) {
					if (t.getMainCollator().matches(a.names.get(k))) {
						phrases = addPhrase(phrases, a.names.get(k), k);
					}
				}
				boolean match = phrases != null;
				Set<String> matchedLanguages = null;
				// a word of a name of the translator still records the whole phrase of a map it is in: "restaurant" and
				// "café" of "Restaurant et café" are English names too, and without the French phrase "et" alone
				// would lose the category (keepCategoryNamedOnce); the name of the translator keeps it for every map
				for (String lang : languages) {
					Map<String, Integer> lp = matchLanguagePhrases(a.languageNames.get(lang), t, tokens,
							a.names.size(), null);
					if (lp != null) {
						if (!match) {
							matchedLanguages = matchedLanguages == null ? new LinkedHashSet<>() : matchedLanguages;
							matchedLanguages.add(lang);
						}
						for (Map.Entry<String, Integer> e : lp.entrySet()) {
							phrases = addPhrase(phrases, e.getKey(), e.getValue());
						}
					}
				}
				if (phrases != null) {
					addPoiCategoryMatch(ctx, a, t, matchedLanguages, phrases, res);
					if (a.wikidataId != null) {
						List<SpatialPoiType> otherTypes = byWikidataKey.get(a.wikidataId);
						for (SpatialPoiType otherA : otherTypes) {
							if (otherA.id != a.id) {
								addPoiCategoryMatch(ctx, otherA, t, matchedLanguages, phrases, res);
							}
						}
					}
				}
			}
		}
		for (PoiCatSearch pc : res.values()) {
			keepCategoryNamedOnce(pc);
		}
		keepCategoriesOfWholeNames(res, tokens);

		List<PoiCatSearch> finalRes = new ArrayList<>(res.values());
		Collections.sort(finalRes);
		Set<SpatialSearchToken> uncoveredTokens = new HashSet<>();
		for (PoiCatSearch pc : finalRes) {
			uncoveredTokens.addAll(pc.tokens);
		}
		int free = ctx.settings.LIMIT_POI_CATEGORY_BY_FREQ;
		for (PoiCatSearch pc : finalRes) {
			boolean coversNewToken = uncoveredTokens.removeAll(pc.tokens);
			if (free == 0 || (!coversNewToken && free <= uncoveredTokens.size())) {
				continue;
			}
			free--;
			for (int i = 0; i < pc.tokens.size(); i++) {
				SpatialSearchToken token = pc.tokens.get(i);
				NameIndexAtom atom = pc.atoms.get(i);
				token.addAtom(atom);
				if (!typedInFull(token, pc.pt(), pc.languages().get(i))) {
					token.poiCategoryKeysToAutocomplete.remove(atom.name);
					token.poiCategoryIds.remove((int) atom.id);
				}
			}
			// Problem "Helipad 32" (doesn't list object because no 32 ref is found"
			// Categories are not needed if exact result is found (there is always option to go in category and filter later)
//			if (ctx.settings.SUGGEST_SEARCH_POI_CATEGORY_WITH_REF) {
//				ctx.addBuildingRefAtoms(token, tokens, pc.tokens, false, atom, SpatialSearchToken.POI_CATEGORY_TYPE);
//			}
		}
	}
	
	public SpatialPoiType getById(int id) {
		return byId.get(id);
	}
	
	public SpatialPoiType getByKey(String key) {
		return byKey.get(key);
	}


	private List<BinaryMapIndexReader> filterByRadius(LatLon l, int rad, List<BinaryMapIndexReader> oFiles,
			List<BinaryMapIndexReader> res) {
		QuadRect rect = MapUtils.calculate31BboxUsingRhumb(rad, l);
		Iterator<BinaryMapIndexReader> it = oFiles.iterator();
		while (it.hasNext()) {
			BinaryMapIndexReader next = it.next();
			if (next.containsPoiData((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom)) {
				res.add(next);
				it.remove();
			}
		}
		return res;
	}

	public List<Amenity> loadPOIObjects(SpatialSearchContext ctx, SpatialPoiType spt, QuadRect r, int zoom, int limit) throws IOException {
		List<Amenity> results = new ArrayList<Amenity>();
		if (spt != null && ctx.files != null) {
			int[] alimit = new int[] { limit };
			SearchRequest<Amenity> req = prepareRequest(spt, results, MapUtils.get31TileNumberX(r.left),
					MapUtils.get31TileNumberY(r.top), MapUtils.get31TileNumberX(r.right),
					MapUtils.get31TileNumberY(r.bottom), zoom, alimit);
			iterateSearch(ctx, req, ctx.files, true);
		}
		return filterByZoomTile(ctx, results);
	}

	private List<Amenity> filterByZoomTile(SpatialSearchContext ctx, List<Amenity> amenities) {
		int z = 16 - ctx.settings.SEARCH_POI_BY_CATEGORY_ZOOM;
		if (z < 0) {
			return amenities; // dedup disabled at this zoom, keep everything
		}
		TLongHashSet tiles = new TLongHashSet();
		List<Amenity> res = new ArrayList<>(amenities.size());
		for (Amenity a : amenities) {
			int x16 = MapUtils.get31TileNumberX(a.getLocation().getLongitude()) >> 15;
			int y16 = MapUtils.get31TileNumberY(a.getLocation().getLatitude()) >> 15;
			long tileId = MapUtils.interleaveBits(x16 >> z, y16 >> z);
			boolean elo = a.getTravelEloNumber() > Amenity.DEFAULT_ELO;
			if (tiles.contains(tileId) && !elo) {
				continue;
			}
			tiles.add(tileId);
			res.add(a);
		}
		return res;
	}
	
	public List<Amenity> loadPOIObjects(SpatialSearchContext ctx, SpatialPoiType spt,  LatLon latLon, int radMeters,
			int limit) throws IOException {
		List<Amenity> results = new ArrayList<Amenity>();
		if (spt != null && ctx.files != null) {
			int[] alimit = new int[] { limit };
			QuadRect rect = MapUtils.calculate31BboxUsingRhumb(radMeters, latLon);
			SearchRequest<Amenity> req = prepareRequest(spt, results, (int) rect.left, (int) rect.top, (int) rect.right,
					(int) rect.bottom, -1, alimit);
			List<BinaryMapIndexReader> oFiles = new LinkedList<BinaryMapIndexReader>(ctx.files);
			iterateSearch(ctx, req, filterByRadius(latLon, 5_000, oFiles, new ArrayList<BinaryMapIndexReader>()), false);
			if (alimit[0] != 0 && radMeters > 5_000) {
				iterateSearch(ctx, req, filterByRadius(latLon, 50_000, oFiles, new ArrayList<BinaryMapIndexReader>()), false);
			}
			if (alimit[0] != 0 && radMeters > 50_000) {
				iterateSearch(ctx, req, oFiles, false);
			}
		}
		return results;
	}

	private static Set<String> groupChildTypes(SpatialPoiType spt) {
		if (spt.singleType instanceof PoiFilter pf && !(spt.singleType instanceof PoiCategory)) {
			Set<String> types = new HashSet<>();
			for (PoiType p : pf.getPoiTypes()) {
				types.add(p.getKeyName());
			}
			return types;
		}
		return Collections.emptySet();
	}

	private SearchRequest<Amenity> prepareRequest(SpatialPoiType spt, List<Amenity> results, int sleft, int stop, int sright,
			int sbottom, int zoom, int[] alimit) {
		final Set<String> groupTypes = groupChildTypes(spt);
		SearchPoiTypeFilter typeFilter = spt.poiAdditional != null ? null : new SearchPoiTypeFilter() {

			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				if (spt.key.equals(type.getKeyName()) || spt.key.equals(subcategory)) {
					return true;
				}
				if (groupTypes.contains(subcategory)) {
					return true;
				}
				if (spt.parentTypes != null) {
					for (AbstractPoiType a : spt.parentTypes) {
						if (a.getKeyName().equals(type.getKeyName()) || a.getKeyName().equals(subcategory)) {
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
		SearchPoiAdditionalFilter addFilter = spt.poiAdditional == null ? null : new SearchPoiAdditionalFilter() {

			@Override
			public String getName() {
				return spt.names.get(0);
			}

			@Override
			public String getIconResource() {
				return null;
			}

			@Override
			public boolean accept(PoiSubType poiSubType, String value) {
//					spt.key.startsWith(poiSubType.name)
				if (spt.poiAdditional.equals(value)) {
					return true;
				}
				return false;
			}
		};
		ResultMatcher<Amenity> matcher = new ResultMatcher<Amenity>() {

			@Override
			public boolean publish(Amenity object) {
				if (spt.parentTypes != null) {
					boolean match = object.getAdditionalInfo(spt.key) != null;
					if (!match) {
						return false;
					}
				}
				if (alimit[0] > 0) {
					alimit[0]--;
				}
				results.add(object);
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false; // allow to read all for proper sorting
//					return alimit[0] == 0;
			}
		};
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
				0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, typeFilter, addFilter, matcher);
			req = BinaryMapIndexReader.buildSearchPoiRequest((int) sleft, (int) sright, (int) stop,
					sbottom, zoom, typeFilter, addFilter, matcher);
		return req;
	}


	private void iterateSearch(SpatialSearchContext ctx, SearchRequest<Amenity> req, List<BinaryMapIndexReader> res,
			boolean check) throws IOException {
		for (BinaryMapIndexReader bir : res) {
			if (check && !bir.containsPoiData(req.getLeft(), req.getTop(), req.getRight(), req.getBottom())) {
				continue;
			}
			ctx.stats.poiByTypeTime.start();
			long br = bir.getBytesRead();
			bir.searchPoi(req);
			ctx.stats.poiByTypeBytes += (bir.getBytesRead() - br);
			ctx.stats.poiByTypeTime.finish();
			ctx.stats.poiByTypeBboxes += req.numberOfReadSubtrees;
		}
	}

}
