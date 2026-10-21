package net.osmand.shared.binary

/**
 * A copy of `Abbreviations` in OsmAnd-java: the short forms of street words and directions, and the
 * articles and conjunctions a search skips.
 *
 * `replaceAll`, which only the map creator calls, is not copied.
 */
object Abbreviations {

	private val abbreviations = HashMap<String, String>()

	private val conjunctions = HashSet<String>()

	private fun addDirectionWord(key: String, full: String) {
		abbreviations[key] = full
	}

	private fun addStreetStatus(key: String, full: String) {
		abbreviations[key] = full
	}

	private fun addConjunction(key: String) {
		conjunctions.add(key)
	}

	init {
		// articles
		addConjunction("the")
		addConjunction("de")
		addConjunction("du")
		addConjunction("der")
		addConjunction("den")
		addConjunction("die")
		addConjunction("das")
		addConjunction("la")
		addConjunction("le")
		addConjunction("el")
		addConjunction("il")
		addConjunction("of")

		// and
		addConjunction("and")
		addConjunction("und")
		addConjunction("en")
		addConjunction("et")
		addConjunction("y")
		addConjunction("и")

		// direction
		addDirectionWord("e", "East")
		addDirectionWord("w", "West")
		addDirectionWord("s", "South")
		addDirectionWord("n", "North")
		addDirectionWord("sw", "Southwest")
		addDirectionWord("se", "Southeast")
		addDirectionWord("nw", "Northwest")
		addDirectionWord("ne", "Northeast")

		// street status
		addStreetStatus("ln", "Lane")
		addStreetStatus("dr", "Drive")
		addStreetStatus("rd", "Road")
		addStreetStatus("av", "Avenue")
		addStreetStatus("st", "Street") // 2 values could be saint
		addStreetStatus("hwy", "Highway")
		addStreetStatus("blvd", "Boulevard")
	}

	// search-v1
	fun getAbbreviations(): Map<String, String> = abbreviations

	// search v-1
	fun replace(word: String): String = abbreviations[word.lowercase()] ?: word

	// search-v1
	fun isConjunction(lowerCase: String): Boolean = conjunctions.contains(lowerCase)
}
