package net.osmand.search.rules;

import net.osmand.PlatformUtil;
import net.osmand.util.SearchAlgorithms;

import org.apache.commons.logging.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The search rules of every locale ({@code rules*.xml}): created once by the owner of the search (spatial search keeps
 * it in its global cache), reads {@code rules.xml} at once and the rules of a locale on first use. A missing or broken
 * file is logged and searched without its rules: rules refine the search, they must not stop it.
 */
public final class SearchModRules {

	private static final Log LOG = PlatformUtil.getLog(SearchModRules.class);

	// absolute: R8 -repackageclasses moves this class out of its package, a relative name would miss the files
	private static final String RESOURCE_DIR = "/net/osmand/search/rules/";

	/** The owner of a name, a value of the {@code object} of a rule (rules-spec.md, 3.2). */
	public enum SearchModRuleOwner {
		STREET("street"), LOCALITY("locality"), BOUNDARY("boundary"), POSTCODE("postcode"), POI("poi");

		public final String tag;

		SearchModRuleOwner(String tag) {
			this.tag = tag;
		}
	}

	private final String resourceDir;
	// false for the search: <index> and <poi> serve only the OBF writer
	private final boolean indexRules;
	private final SearchModLocales locales;
	private final SearchModRulesParser.Layer base;
	private final Map<String, SearchModLocaleRules> rules = new ConcurrentHashMap<>();
	private final Map<String, SearchModDictionary> dictionaries = new ConcurrentHashMap<>();
	// by the locale as callers pass it: normalize() is too slow for every word of a search
	private final Map<String, SearchModDictionary> byLocale = new ConcurrentHashMap<>();

	/** reads the rules files of OsmAnd-java resources, all sections (the OBF writer) */
	public SearchModRules() {
		this(RESOURCE_DIR, true);
	}

	/**
	 * The rules the search needs: {@code <query>} and the query forms of mirror pairs. {@code <index>} (alternative
	 * names, word classes), the index rules of mirror pairs and {@code <poi>} are read only by the OBF writer.
	 */
	public static SearchModRules forSearch() {
		return new SearchModRules(RESOURCE_DIR, false);
	}

	/** @param resourceDir resource directory of the rules files, another one only in tests */
	SearchModRules(String resourceDir) {
		this(resourceDir, true);
	}

	SearchModRules(String resourceDir, boolean indexRules) {
		this.resourceDir = resourceDir;
		this.indexRules = indexRules;
		SearchModLocales locales = new SearchModLocales();
		SearchModRulesParser.Layer base = null;
		try {
			base = read(SearchModLocaleRules.BASE_FILE, locales, true);
			if (!locales.isBuilt()) {
				throw new IllegalStateException("Missing <locales> in " + SearchModLocaleRules.BASE_FILE);
			}
		} catch (RuntimeException e) {
			LOG.error("Search runs without rules", e);
			// a half-read <locales> is dropped with the rest of the file
			locales = new SearchModLocales();
			locales.build();
			base = new SearchModRulesParser.Layer(SearchModLocaleRules.BASE_FILE);
		}
		this.locales = locales;
		this.base = base;
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
				String file = "rules" + suffix + ".xml";
				try {
					SearchModRulesParser.Layer layer = read(file, null, false);
					if (layer != null) {
						layers.add(layer);
					}
				} catch (RuntimeException e) {
					LOG.error("Search runs without " + file, e);
				}
			}
		}
		return new SearchModLocaleRules(locale, layers, indexRules);
	}

	private SearchModRulesParser.Layer read(String file, SearchModLocales locales, boolean required) {
		try (InputStream input = SearchModRules.class.getResourceAsStream(resourceDir + file)) {
			if (input == null) {
				if (required) {
					throw new IllegalStateException("Missing search rules: " + file);
				}
				return null;
			}
			return new SearchModRulesParser(file, locales, indexRules).parse(input);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot load search rules " + file, e);
		}
	}
}
