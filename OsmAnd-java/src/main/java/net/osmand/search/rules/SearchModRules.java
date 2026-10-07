package net.osmand.search.rules;

import net.osmand.util.SearchAlgorithms;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The search rules of every locale ({@code rules*.xml}): created once by the owner of the search (spatial search keeps
 * it in its global cache), reads {@code rules.xml} at once and the rules of a locale on first use.
 */
public final class SearchModRules {

	/** The owner of a name, a value of the {@code object} of a rule (rules-spec.md, 3.2). */
	public enum SearchModRuleOwner {
		STREET("street"), LOCALITY("locality"), BOUNDARY("boundary"), POSTCODE("postcode"), POI("poi");

		public final String tag;

		SearchModRuleOwner(String tag) {
			this.tag = tag;
		}
	}

	private final SearchModLocales locales = new SearchModLocales();
	private final SearchModRulesParser.Layer base;
	private final Map<String, SearchModLocaleRules> rules = new ConcurrentHashMap<>();
	private final Map<String, SearchModDictionary> dictionaries = new ConcurrentHashMap<>();
	// by the locale as callers pass it: normalize() is too slow for every word of a search
	private final Map<String, SearchModDictionary> byLocale = new ConcurrentHashMap<>();

	/** reads the rules files of OsmAnd-java resources */
	public SearchModRules() {
		this.base = read(SearchModLocaleRules.BASE_FILE, true);
		if (!locales.isBuilt()) {
			throw new IllegalStateException("Missing <locales> in " + SearchModLocaleRules.BASE_FILE);
		}
	}

	/** {@code <locales>} of rules.xml */
	public SearchModLocales locales() {
		return locales;
	}

	/** @param locale rules locale; null, empty or malformed selects only the base rules */
	public SearchModLocaleRules rules(String locale) {
		return rules.computeIfAbsent(locales.normalize(locale), this::load);
	}

	/** @param locale rules locale; null, empty or malformed selects only the base rules */
	public SearchModDictionary dictionary(String locale) {
		String raw = locale == null ? "" : locale;
		SearchModDictionary dictionary = byLocale.get(raw);
		if (dictionary == null) {
			dictionary = dictionaries.computeIfAbsent(locales.normalize(raw),
					k -> new SearchModDictionary(k, rules(k)));
			byLocale.put(raw, dictionary);
		}
		return dictionary;
	}

	/** @return the dictionary of the rules locale of a map ("Us_new-york_northamerica" -> en_US) */
	public SearchModDictionary dictionaryForMap(String mapName) {
		return dictionary(locales.forMap(mapName));
	}

	/** @return true when a query word is like a part of a POI ref: "A1", "12" */
	public boolean likelyPartOfRef(String word, Set<String> wordSplit) {
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

	private SearchModLocaleRules load(String locale) {
		List<SearchModRulesParser.Layer> layers = new ArrayList<>();
		layers.add(base);
		if (!locale.isEmpty()) {
			StringBuilder suffix = new StringBuilder();
			for (String part : locale.split("_")) {
				suffix.append('_').append(part);
				SearchModRulesParser.Layer layer = read("rules" + suffix + ".xml", false);
				if (layer != null) {
					layers.add(layer);
				}
			}
		}
		return new SearchModLocaleRules(locale, layers);
	}

	private SearchModRulesParser.Layer read(String file, boolean required) {
		try (InputStream input = SearchModRules.class.getResourceAsStream(file)) {
			if (input == null) {
				if (required) {
					throw new IllegalStateException("Missing search rules: " + file);
				}
				return null;
			}
			return new SearchModRulesParser(file, SearchModLocaleRules.BASE_FILE.equals(file) ? locales : null).parse(input);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot load search rules " + file, e);
		}
	}
}
