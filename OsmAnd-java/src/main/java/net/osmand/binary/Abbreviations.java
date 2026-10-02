package net.osmand.binary;

import net.osmand.search.core.SearchPhrase;
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
		// word of a normalized name -> variants of this word, a stored name is rewritten by them
		final Map<String, List<SearchVariantRules.Rule>> normalizations = new HashMap<>();
		// rules that give a query word its forms: <query> and the rules outside <index> and <query>
		final List<SearchVariantRules.Rule> forms = new ArrayList<>();
		final Set<String> buildingAbbreviations;
		final Set<String> conjunctions;
		final Set<String> commonSkipOtherCnt;
		// experiments: query word -> forms that replace the forms of the rules (empty list: none)
		final Map<String, List<String>> overrides;

		Dictionary(SearchVariantRules rules, Map<String, List<String>> overrides) {
			Set<String> commonSkipOtherCnt = new TreeSet<>();
			Set<String> buildingAbbreviations = new TreeSet<>();
			Set<String> conjunctions = new TreeSet<>();
			for (SearchVariantRules.Rule variant : rules.normalizations()) {
				normalizations.computeIfAbsent(variant.word, k -> new ArrayList<>()).add(variant);
			}
			forms.addAll(rules.query());
			for (SearchVariantRules.Rule variant : rules.buildings()) {
				buildingAbbreviations.add(variant.word);
			}
			for (SearchVariantRules.Rule variant : rules.ignorables()) {
				conjunctions.add(variant.word);
				commonSkipOtherCnt.add(variant.word);
			}
			addCommon(commonSkipOtherCnt, rules.normalizations());
			addCommon(commonSkipOtherCnt, rules.index());
			addCommon(commonSkipOtherCnt, rules.query());
			this.buildingAbbreviations = Collections.unmodifiableSet(buildingAbbreviations);
			this.conjunctions = Collections.unmodifiableSet(conjunctions);
			this.commonSkipOtherCnt = Collections.unmodifiableSet(commonSkipOtherCnt);
			this.overrides = overrides;
		}

		private Dictionary(Dictionary base, Map<String, List<String>> overrides) {
			this.normalizations.putAll(base.normalizations);
			this.forms.addAll(base.forms);
			this.buildingAbbreviations = base.buildingAbbreviations;
			this.conjunctions = base.conjunctions;
			this.commonSkipOtherCnt = base.commonSkipOtherCnt;
			this.overrides = overrides;
		}

		private static void addCommon(Set<String> common, List<SearchVariantRules.Rule> variants) {
			for (SearchVariantRules.Rule variant : variants) {
				if (variant.common) {
					common.add(variant.word);
					common.add(variant.to().toLowerCase(Locale.ROOT));
				}
			}
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
		Set<QueryForm> forms = new LinkedHashSet<>();
		List<String> override = dictionary.overrides.get(word);
		if (override != null) {
			for (String form : override) {
				forms.add(new QueryForm(SearchVariantRules.ANY_OBJECT, form));
			}
			return new ArrayList<>(forms);
		}
		for (SearchVariantRules.Rule rule : dictionary.forms) {
			String replacement = rule.apply(word);
			if (replacement != null) {
				List<String> words = SearchAlgorithms.splitAndNormalize(replacement, false);
				if (words.size() == 1 && !words.get(0).equals(word)) {
					forms.add(new QueryForm(rule.object, words.get(0)));
				}
			}
		}
		return new ArrayList<>(forms);
	}

	// search-v2
	public static boolean isCommonSkipOtherCnt(String lowerCase, String locale) {
		return dictionary(locale).commonSkipOtherCnt.contains(lowerCase);
	}

	/**
	 * Indexing data: rewrites the words of a name of {@code owner} ("street") by the normalizations of the locale.
	 * A query word gets the same rewrite as one of its forms (the rule is in {@link SearchVariantRules#query()} too).
	 */
	public static String replaceAll(String phrase, String locale, String owner) {
		Map<String, List<SearchVariantRules.Rule>> normalizations = dictionary(locale).normalizations;
		if (normalizations.isEmpty()) {
			return phrase;
		}
		String[] words = phrase.split(SearchPhrase.DELIMITER);
		StringBuilder r = new StringBuilder();
		boolean changed = false;
		for (String w : words) {
			if (r.length() > 0) {
				r.append(SearchPhrase.DELIMITER);
			}
			String abbrRes = normalized(normalizations, w, owner);
			if (abbrRes == null) {
				r.append(w);
			} else {
				changed = true;
				r.append(abbrRes);
			}
		}
		return changed ? r.toString() : phrase;
	}

	private static String normalized(Map<String, List<SearchVariantRules.Rule>> normalizations, String word,
			String owner) {
		List<SearchVariantRules.Rule> variants = normalizations.get(word.toLowerCase(Locale.ROOT));
		if (variants != null) {
			for (SearchVariantRules.Rule variant : variants) {
				if (owner == null || variant.appliesTo(owner)) {
					return variant.to();
				}
			}
		}
		return null;
	}

	/** @return normalized word -> its rewrite, for every owner of names */
	public static Map<String, String> getAbbreviations(String locale) {
		Map<String, String> abbreviations = new LinkedHashMap<>();
		for (Map.Entry<String, List<SearchVariantRules.Rule>> e : dictionary(locale).normalizations.entrySet()) {
			abbreviations.put(e.getKey(), e.getValue().get(0).to());
		}
		return Collections.unmodifiableMap(abbreviations);
	}

	// search v-1
	public static String replace(String word, String locale) {
		String value = normalized(dictionary(locale).normalizations, word, null);
		return value != null ? value : word;
	}

	// search-v1
	public static boolean isConjunction(String lowerCase, String locale) {
		return dictionary(locale).conjunctions.contains(lowerCase);
	}
}
