package net.osmand.shared.search.rules

import net.osmand.shared.map.WorldRegion
import kotlin.math.max

/**
 * Locale of search variant rules (`rules_<language>_<COUNTRY>.xml`) for a map: `<locales>` of
 * `rules.xml`, read by [SearchModRules].
 *
 * The rules follow the data, not the user interface: a map is written in the language of its
 * country. The map locale comes from the download name of the map ("Us_new-york_northamerica" is
 * en_US, "Switzerland_ticino_europe" is it_CH); the longest prefix wins. A map this table does not
 * cover gets "" and only the base rules.
 *
 * A copy of the search part of `SearchModLocales` in OsmAnd-java, which stays there for android,
 * the OBF writer and tools; this copy is for iOS. The statistics groups, the transliteration of
 * names and the locale of a name of a map serve only the OBF writer.
 */
class SearchModLocales internal constructor() {

	// filled by the parser of rules.xml (addMap, build), read only afterwards
	private val localeByPrefix = LinkedHashMap<String, String>()
	private var built = false

	/**
	 * @return rules locale of a map ("Us_new-york_northamerica_2.obf" -> "en_US") or "" when no prefix covers the
	 * map name; accepts a download name, a file name or a path
	 */
	fun forMap(mapName: String?): String {
		val prefix = mapPrefix(mapName) ?: return ""
		// java answers null when lower case changes the length of the name before the prefix; "" selects the same rules
		return localeByPrefix[prefix.lowercase()] ?: ""
	}

	/**
	 * @return the longest prefix of a map name that has a rules locale, in the case of the name
	 * ("Switzerland_ticino_europe_2.obf" -> "Switzerland_ticino"), null when no prefix covers the map name
	 */
	fun mapPrefix(mapName: String?): String? {
		if (mapName == null) {
			return null
		}
		var name: String = mapName
		val slash = max(name.lastIndexOf('/'), name.lastIndexOf('\\'))
		if (slash >= 0) {
			name = name.substring(slash + 1)
		}
		val lower = name.lowercase()
		// the longest covering prefix: "switzerland_ticino" before "switzerland"
		var end = lower.length
		while (end > 0) {
			if (localeByPrefix.containsKey(lower.substring(0, end))) {
				return name.substring(0, end)
			}
			end = lower.lastIndexOf('_', end - 1)
		}
		return null
	}

	/** @return canonical form ("en-us" -> "en_US", "zh-hant-tw" -> "zh_Hant_TW"), "" for null */
	fun normalize(locale: String?): String {
		if (locale.isNullOrEmpty()) {
			return ""
		}
		val parts = WorldRegion.splitLikeJava(SearchModRulesParser.trimLikeJava(locale).replace('-', '_'), "_")
		val normalized = StringBuilder(parts[0].lowercase())
		for (i in 1 until parts.size) {
			normalized.append('_')
			if (parts[i].length == 4) {
				normalized.append(parts[i].substring(0, 1).uppercase()).append(parts[i].substring(1).lowercase())
			} else {
				normalized.append(parts[i].uppercase())
			}
		}
		return normalized.toString()
	}

	internal fun isBuilt(): Boolean = built

	/** `<map locale prefixes>` */
	internal fun addMap(locale: String?, prefixes: List<String>) {
		val normalized = normalize(locale)
		for (prefix in prefixes) {
			localeByPrefix[prefix] = normalized
		}
	}

	/** the end of `<locales>` */
	internal fun build() {
		built = true
	}
}
