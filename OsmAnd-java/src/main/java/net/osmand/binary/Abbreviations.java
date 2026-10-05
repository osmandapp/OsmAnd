package net.osmand.binary;

import net.osmand.util.SearchAlgorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
		// rule word -> its main form, both common words (CommonWords shares the frequency of the pair)
		final Map<String, String> abbreviations = new LinkedHashMap<>();
		final Set<String> buildingAbbreviations;
		final Set<String> conjunctions;
		final Set<String> commonSkipOtherCnt;
		// experiments: query word -> forms that replace the forms of the rules (empty list: none)
		final Map<String, List<String>> overrides;

		Dictionary(SearchVariantRules rules, Map<String, List<String>> overrides) {
			for (String word : rules.formWords()) {
				List<QueryForm> list = new ArrayList<>();
				for (SearchVariantRules.Form form : rules.forms(word)) {
					list.add(new QueryForm(form.object(), form.word()));
				}
				// a token is aligned (ß -> ss, no diacritics): "Straße" asks for "strasse"
				forms.put(SearchAlgorithms.alignChars(word), List.copyOf(list));
			}
			for (SearchVariantRules.WordRule rule : rules.query()) {
				SearchVariantRules.Form main = rule.mainForm();
				if (main != null && rules.common().contains(rule.word()) && rules.common().contains(main.word())) {
					abbreviations.put(rule.word(), main.word());
				}
			}
			Set<String> commonSkipOtherCnt = new TreeSet<>(rules.common());
			commonSkipOtherCnt.addAll(rules.ignorables());
			this.buildingAbbreviations = Collections.unmodifiableSet(new TreeSet<>(rules.buildings()));
			this.conjunctions = Collections.unmodifiableSet(new TreeSet<>(rules.ignorables()));
			this.commonSkipOtherCnt = Collections.unmodifiableSet(commonSkipOtherCnt);
			this.overrides = overrides;
		}

		private Dictionary(Dictionary base, Map<String, List<String>> overrides) {
			this.forms.putAll(base.forms);
			this.abbreviations.putAll(base.abbreviations);
			this.buildingAbbreviations = base.buildingAbbreviations;
			this.conjunctions = base.conjunctions;
			this.commonSkipOtherCnt = base.commonSkipOtherCnt;
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

	/** A form of a query word: the word of a name it stands for and the owners of names it applies to. */
	public record QueryForm(String object, String word) {
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
				forms.add(new QueryForm(SearchVariantRules.ANY_OBJECT, form));
			}
			return new ArrayList<>(forms);
		}
		return dictionary.forms.getOrDefault(SearchAlgorithms.alignChars(word.toLowerCase(Locale.ROOT)), List.of());
	}

	// search-v2
	public static boolean isCommonSkipOtherCnt(String lowerCase, String locale) {
		return dictionary(locale).commonSkipOtherCnt.contains(lowerCase);
	}

	/** @return a word of a rule -> its main form, for the rules whose word and main form are common words */
	public static Map<String, String> getAbbreviations(String locale) {
		return Collections.unmodifiableMap(dictionary(locale).abbreviations);
	}

	// search-v1
	public static boolean isConjunction(String lowerCase, String locale) {
		return dictionary(locale).conjunctions.contains(lowerCase);
	}
}
