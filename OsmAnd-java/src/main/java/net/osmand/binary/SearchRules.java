package net.osmand.binary;

import net.osmand.util.SearchAlgorithms;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The search rules of every locale ({@code rules*.xml}): created once by the owner of the search (spatial search keeps
 * it in its global cache), reads {@code rules.xml} at once and the rules of a locale on first use.
 */
public final class SearchRules {

	// rules file name -> its content, null when there is no such file
	private final Function<String, InputStream> files;
	private final SearchLocales locales = new SearchLocales();
	private final SearchRulesParser.Layer base;
	private final Map<String, SearchVariantRules> rules = new ConcurrentHashMap<>();
	private final Map<String, SearchRulesDictionary> dictionaries = new ConcurrentHashMap<>();
	// by the locale as callers pass it: normalize() is too slow for every word of a search
	private final Map<String, SearchRulesDictionary> byLocale = new ConcurrentHashMap<>();

	/** the rules files of OsmAnd-java resources */
	public SearchRules() {
		this(file -> SearchRules.class.getResourceAsStream(file));
	}

	public SearchRules(Function<String, InputStream> files) {
		this.files = files;
		this.base = read(SearchVariantRules.BASE_FILE, true);
		if (!locales.isBuilt()) {
			throw new IllegalStateException("Missing <locales> in " + SearchVariantRules.BASE_FILE);
		}
	}

	/** {@code <locales>} of rules.xml */
	public SearchLocales locales() {
		return locales;
	}

	/** @param locale rules locale; null, empty or malformed selects only the base rules */
	public SearchVariantRules rules(String locale) {
		return rules.computeIfAbsent(locales.normalize(locale), this::load);
	}

	/** @param locale rules locale; null, empty or malformed selects only the base rules */
	public SearchRulesDictionary dictionary(String locale) {
		String raw = locale == null ? "" : locale;
		SearchRulesDictionary dictionary = byLocale.get(raw);
		if (dictionary == null) {
			dictionary = dictionaries.computeIfAbsent(locales.normalize(raw),
					k -> new SearchRulesDictionary(k, rules(k)));
			byLocale.put(raw, dictionary);
		}
		return dictionary;
	}

	/** @return the dictionary of the rules locale of a map ("Us_new-york_northamerica" -> en_US) */
	public SearchRulesDictionary dictionaryForMap(String mapName) {
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

	private SearchVariantRules load(String locale) {
		List<SearchRulesParser.Layer> layers = new ArrayList<>();
		layers.add(base);
		if (!locale.isEmpty()) {
			StringBuilder suffix = new StringBuilder();
			for (String part : locale.split("_")) {
				suffix.append('_').append(part);
				SearchRulesParser.Layer layer = read("rules" + suffix + ".xml", false);
				if (layer != null) {
					layers.add(layer);
				}
			}
		}
		return new SearchVariantRules(locale, layers);
	}

	private SearchRulesParser.Layer read(String file, boolean required) {
		try (InputStream input = files.apply(file)) {
			if (input == null) {
				if (required) {
					throw new IllegalStateException("Missing search rules: " + file);
				}
				return null;
			}
			return new SearchRulesParser(file, SearchVariantRules.BASE_FILE.equals(file) ? locales : null).parse(input);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot load search rules " + file, e);
		}
	}
}
