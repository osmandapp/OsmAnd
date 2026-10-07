package net.osmand.binary;

import net.osmand.util.SearchAlgorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Word dictionaries of {@link SearchVariantRules} by rules locale. Every method takes the locale of the data
 * ({@link SearchLocales}); null or an unknown locale selects the base rules. Dictionaries are immutable.
 */
public class Abbreviations {

    private Abbreviations() {
    }

    private static final Map<String, Dictionary> DICTIONARIES = new ConcurrentHashMap<>();
	// by the locale as callers pass it: search asks per name word, normalize() is too slow for that
	private static final Map<String, Dictionary> BY_LOCALE = new ConcurrentHashMap<>();

	private static Dictionary dictionary(String locale) {
		String raw = locale == null ? "" : locale;
		Dictionary dictionary = BY_LOCALE.get(raw);
		if (dictionary == null) {
			dictionary = DICTIONARIES.computeIfAbsent(SearchLocales.normalize(raw),
					k -> new Dictionary(SearchVariantRules.forLocale(k), Map.of()));
			BY_LOCALE.put(raw, dictionary);
		}
		return dictionary;
	}

	private static final class Dictionary {
		// query word -> its forms and reverse forms, in the order of the rules
		final Map<String, List<QueryForm>> forms = new HashMap<>();
		final Set<String> buildingAbbreviations;
		// aligned ignorable words (to="")
		final Set<String> ignorables;
		// aligned word of a name -> owners whose names it does not penalize (<skipPenalty>, ignorable words, <class1/2>)
		final Map<String, List<String>> penaltyFree;
		// experiments: query word -> forms that replace the forms of the rules (empty list: none)
		final Map<String, List<String>> overrides;

		Dictionary(SearchVariantRules rules, Map<String, List<String>> overrides) {
			Map<String, SearchVariantRules.WordRule> ruleOfWord = new HashMap<>();
			for (SearchVariantRules.WordRule rule : rules.query()) {
				ruleOfWord.put(rule.word(), rule);
			}
			for (String word : rules.formWords()) {
				List<QueryForm> list = new ArrayList<>();
				for (SearchVariantRules.Form form : rules.forms(word)) {
					// a reverse form leads to the word of a rule that has this word as its form ("street" -> "st")
					SearchVariantRules.WordRule other = ruleOfWord.get(form.word());
					list.add(new QueryForm(form.object(), form.word(), other != null && other.hasForm(word)));
				}
				// a token is aligned (ß -> ss, no diacritics): "Straße" asks for "strasse"
				forms.put(SearchAlgorithms.alignChars(word), List.copyOf(list));
			}
			// the words of <skipPenalty> and the classes are aligned by the rules, the ignorable words are query words
			Map<String, Set<String>> owners = new HashMap<>();
			rules.skipPenalty().forEach((word, list) -> owners.computeIfAbsent(word, k -> new LinkedHashSet<>())
					.addAll(list));
			Set<String> ignorables = new TreeSet<>();
			for (String word : rules.ignorables()) {
				String aligned = SearchAlgorithms.alignChars(word);
				ignorables.add(aligned);
				owners.computeIfAbsent(aligned, k -> new LinkedHashSet<>()).add(SearchVariantRules.ANY_OBJECT);
			}
			for (Map.Entry<String, Integer> e : rules.classes().entrySet()) {
				if (e.getValue() != SearchVariantRules.CLASS_ALWAYS) {
					// a service or frequent word only names the kind of an object
					owners.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>()).add(SearchVariantRules.ANY_OBJECT);
				}
			}
			Map<String, List<String>> penaltyFree = new HashMap<>();
			owners.forEach((word, set) -> penaltyFree.put(word, set.contains(SearchVariantRules.ANY_OBJECT)
					? List.of(SearchVariantRules.ANY_OBJECT) : List.copyOf(set)));
			this.buildingAbbreviations = Collections.unmodifiableSet(new TreeSet<>(rules.buildings()));
			this.ignorables = Collections.unmodifiableSet(ignorables);
			this.penaltyFree = Collections.unmodifiableMap(penaltyFree);
			this.overrides = overrides;
		}

		private Dictionary(Dictionary base, Map<String, List<String>> overrides) {
			this.forms.putAll(base.forms);
			this.buildingAbbreviations = base.buildingAbbreviations;
			this.ignorables = base.ignorables;
			this.penaltyFree = base.penaltyFree;
			this.overrides = overrides;
		}
	}

	/**
	 * Experiments only (AbbreviationMetrics): replaces the forms of one query word of a locale for the next searches
	 * of this process. A search that has started keeps the dictionary it read.
	 *
	 * @param value forms separated by spaces; blank removes the forms of the rules, null returns the rules
	 * @return the previous forms separated by spaces (of the override, else of the rules), null when there were none
	 */
	public static String overrideSearchAbbreviation(String locale, String key, String value) {
		String normalized = SearchLocales.normalize(locale);
		String[] previous = new String[1];
		DICTIONARIES.compute(normalized, (k, current) -> {
			Dictionary base = current != null ? current : new Dictionary(SearchVariantRules.forLocale(k), Map.of());
			List<String> old = base.overrides.get(key);
			if (old == null) {
				old = new ArrayList<>();
				for (QueryForm form : queryForms(base, key)) {
					old.add(form.word());
				}
			}
			previous[0] = old.isEmpty() ? null : String.join(" ", old);
			Map<String, List<String>> overrides = new HashMap<>(base.overrides);
			if (value == null) {
				overrides.remove(key);
			} else {
				overrides.put(key, List.copyOf(SearchAlgorithms.splitAndNormalize(value, true)));
			}
			return new Dictionary(base, Map.copyOf(overrides));
		});
		BY_LOCALE.clear();
		return previous[0];
	}

	public static boolean likelyPartOfRef(String word, Set<String> wordSplit) {
		int limit = 2;
		int letters = SearchAlgorithms.letters(word, limit + 1);
		if (letters < limit || (letters == limit && SearchAlgorithms.startsWithDigit(word))) {
			return true;
		}
		for (String s : wordSplit) {
			letters = SearchAlgorithms.letters(s, limit + 1);
			if (!(letters < limit || (letters == limit && SearchAlgorithms.startsWithDigit(s)))) {
				return false;
			}
		}
		return true;
	}
	
	// search v-2
	public static boolean likelyPartOfBuilding(String word, Set<String> wordSplit, String locale) {
		Dictionary rules = dictionary(locale);
		boolean bldNum = (SearchAlgorithms.isNumber2Letters(word) || word.length() == 1
				|| rules.buildingAbbreviations.contains(word));
		if (bldNum) {
			return true;
		}
		if (wordSplit != null) {
			// recursion for 2bis
			for (String w : wordSplit) {
				boolean likely = likelyPartOfBuilding(w, null, locale);
				if (!likely) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * A form of a query word: the word of a name it stands for and the owners of names it applies to; a reverse form
	 * leads from a full word to its abbreviation ("street" -> "st").
	 */
	public record QueryForm(String object, String word, boolean reverse) {
		public boolean appliesTo(String owner) {
			return SearchVariantRules.appliesTo(object, owner);
		}

		/** true when the form applies to every owner of a name */
		public boolean isUnscoped() {
			return SearchVariantRules.ANY_OBJECT.equals(object);
		}
	}

	/** Only one-word forms can be matched against one name-index atom, the rules load only such rules. */
	public static List<QueryForm> getQueryForms(String word, String locale) {
		return queryForms(dictionary(locale), word);
	}

	private static List<QueryForm> queryForms(Dictionary dictionary, String word) {
		List<String> override = dictionary.overrides.get(word);
		if (override != null) {
			Set<QueryForm> forms = new LinkedHashSet<>();
			for (String form : override) {
				forms.add(new QueryForm(SearchVariantRules.ANY_OBJECT, form, false));
			}
			return new ArrayList<>(forms);
		}
		return dictionary.forms.getOrDefault(SearchAlgorithms.alignChars(word.toLowerCase(Locale.ROOT)), List.of());
	}

	/**
	 * search-v2: a word of a name that the query does not have does not penalize the object: {@code <skipPenalty>}
	 * for the owner, an ignorable word ({@code to=""}) or a word of {@code <class1>}/{@code <class2>} of the locale
	 *
	 * @param owner owner of the name: street, locality, boundary, postcode, poi
	 */
	public static boolean isCommonSkipOtherCnt(String lowerCase, String locale, String owner) {
		List<String> owners = dictionary(locale).penaltyFree.get(aligned(lowerCase));
		if (owners == null) {
			return false;
		}
		for (String object : owners) {
			if (SearchVariantRules.appliesTo(object, owner)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * search-v2: an ignorable word ({@code to=""}) of the locale of the map. Search v1 does not read the rules, its
	 * conjunctions are {@link CommonWords#isConjunction}.
	 */
	public static boolean isIgnorable(String lowerCase, String locale) {
		return dictionary(locale).ignorables.contains(aligned(lowerCase));
	}

	// the dictionaries keep aligned words: "école" is "ecole", "straße" is "strasse"; most words of names need no work
	private static String aligned(String lowerCase) {
		for (int i = 0; i < lowerCase.length(); i++) {
			char c = lowerCase.charAt(i);
			if (c >= 128 || c == '\'' || c == '`') {
				return SearchAlgorithms.alignChars(lowerCase);
			}
		}
		return lowerCase;
	}
}
