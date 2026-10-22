package net.osmand.shared.search.rules

import co.touchlab.stately.collections.ConcurrentMutableMap
import net.osmand.shared.io.KFile
import net.osmand.shared.map.WorldRegion
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.LoggerFactory
import okio.use
import kotlin.concurrent.Volatile

/**
 * The search rules of every locale (`rules*.xml`): created once by the owner of the search (spatial
 * search keeps it in its global cache), reads `rules.xml` at once and the rules of a locale on first
 * use. A missing or broken file is logged and searched without its rules: rules refine the search,
 * they must not stop it.
 *
 * A copy of `SearchModRules.forSearch()` of OsmAnd-java, which stays there for android, the OBF
 * writer and tools; this copy is for iOS. It reads what the search reads there: `<query>`, the query
 * forms of mirror pairs and the map prefixes of `<locales>`. The files are read from a directory,
 * the one OsmAnd-java packs them from (`resources/abbr`), not from the classpath.
 */
class SearchModRules private constructor(private val resourceDir: String) {

	/** The owner of a name, a value of the `object` of a rule (rules-spec.md, 3.2). */
	enum class SearchModRuleOwner(val tag: String) {
		STREET("street"), LOCALITY("locality"), BOUNDARY("boundary"), POSTCODE("postcode"), POI("poi")
	}

	private val locales: SearchModLocales
	private val base: SearchModRulesParser.Layer
	private val rules = ConcurrentMutableMap<String, SearchModLocaleRules>()
	private val dictionaries = ConcurrentMutableMap<String, SearchModDictionary>()
	// by the locale as callers pass it: normalize() is too slow for every word of a search. Read without a lock, as
	// a lock on Kotlin/Native costs more than the lookup; a new locale replaces the map, and an entry lost to a race
	// is added again with the same dictionary
	@Volatile
	private var byLocale: Map<String, SearchModDictionary> = emptyMap()

	init {
		var locales = SearchModLocales()
		var base: SearchModRulesParser.Layer
		try {
			base = read(SearchModLocaleRules.BASE_FILE, locales, true)!!
			if (!locales.isBuilt()) {
				throw IllegalStateException("Missing <locales> in ${SearchModLocaleRules.BASE_FILE}")
			}
		} catch (e: Exception) {
			LOG.error("Search runs without rules", e)
			// a half-read <locales> is dropped with the rest of the file
			locales = SearchModLocales()
			locales.build()
			base = SearchModRulesParser.Layer()
		}
		this.locales = locales
		this.base = base
	}

	/** `<locales>` of rules.xml */
	fun locales(): SearchModLocales = locales

	/** @param locale rules locale; null, empty or malformed selects only the base rules */
	fun rules(locale: String?): SearchModLocaleRules =
		rules.computeIfAbsent(locales.normalize(locale)) { load(it) }

	/** @param locale rules locale; null, empty or malformed selects only the base rules */
	fun dictionary(locale: String?): SearchModDictionary {
		val raw = locale ?: ""
		byLocale[raw]?.let { return it }
		val dictionary = dictionaries.computeIfAbsent(locales.normalize(raw)) { SearchModDictionary(it, rules(it)) }
		byLocale = byLocale + (raw to dictionary)
		return dictionary
	}

	/** @return true when a query word is like a part of a POI ref: "A1", "12" */
	fun likelyPartOfRef(word: String, wordSplit: Set<String>): Boolean {
		val limit = 2
		var letters = KSearchAlgorithms.letters(word, limit + 1)
		if (letters < limit || (letters == limit && KSearchAlgorithms.startsWithDigit(word))) {
			return true
		}
		for (s in wordSplit) {
			letters = KSearchAlgorithms.letters(s, limit + 1)
			if (!(letters < limit || (letters == limit && KSearchAlgorithms.startsWithDigit(s)))) {
				return false
			}
		}
		return true
	}

	private fun load(locale: String): SearchModLocaleRules {
		val layers = ArrayList<SearchModRulesParser.Layer>()
		layers.add(base)
		if (locale.isNotEmpty()) {
			val suffix = StringBuilder()
			for (part in WorldRegion.splitLikeJava(locale, "_")) {
				suffix.append('_').append(part)
				val file = "rules$suffix.xml"
				try {
					read(file, null, false)?.let { layers.add(it) }
				} catch (e: Exception) {
					LOG.error("Search runs without $file", e)
				}
			}
		}
		return SearchModLocaleRules(layers)
	}

	private fun read(file: String, locales: SearchModLocales?, required: Boolean): SearchModRulesParser.Layer? {
		val input = KFile(KFile(resourceDir), file)
		if (!input.exists()) {
			if (required) {
				throw IllegalStateException("Missing search rules: $file")
			}
			return null
		}
		try {
			return input.source().use { SearchModRulesParser(file, locales).parse(it) }
		} catch (e: IllegalStateException) {
			throw e
		} catch (e: Exception) {
			throw IllegalStateException("Cannot load search rules $file", e)
		}
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("SearchModRules")

		/**
		 * The rules the search needs: `<query>`, the query forms of mirror pairs and the map prefixes.
		 *
		 * @param resourceDir directory of the rules files `rules*.xml`
		 */
		fun forSearch(resourceDir: String): SearchModRules = SearchModRules(resourceDir)
	}
}
