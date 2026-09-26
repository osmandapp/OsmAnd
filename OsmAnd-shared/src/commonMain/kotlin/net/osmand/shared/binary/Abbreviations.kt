package net.osmand.shared.binary

import net.osmand.shared.util.KSearchAlgorithms

/**
 * A copy of `Abbreviations` in OsmAnd-java: the short forms of street words and directions, the
 * articles and conjunctions a search skips, and the words that go with a house number.
 *
 * `replaceAll`, which only the map creator calls, is not copied.
 */
object Abbreviations {

	private val abbreviations = HashMap<String, String>()

	// 2nd version search abbrevations for spatial search
	private val searchAbbreviations = HashMap<String, String>()

	// set of words to check for buidlings
	private val buildingAbbreviations = HashMap<String, String>()
	private val conjunctions = HashSet<String>()

	private val commonSkipOtherCnt = HashSet<String>()

	private fun addDirectionWord(key: String, full: String) {
		abbreviations[key] = full
		commonSkipOtherCnt.add(key)
		commonSkipOtherCnt.add(full.lowercase())
	}

	private fun addStreetStatus(key: String, full: String) {
		abbreviations[key] = full
		commonSkipOtherCnt.add(key)
		commonSkipOtherCnt.add(full.lowercase())
	}

	private fun addConjunction(key: String) {
		conjunctions.add(key)
		commonSkipOtherCnt.add(key)
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

		searchAbbreviations.putAll(abbreviations)
		searchAbbreviations["ave"] = "Avenue" // extra
		searchAbbreviations["st"] = "Street Saint" // 2 values could be saint
		// duplicates - synonyms and not abbrevations actually
		searchAbbreviations["о"] = "Остров"
		searchAbbreviations["остров"] = "о."
		searchAbbreviations["1st"] = "First"
		searchAbbreviations["2nd"] = "Second"
		searchAbbreviations["3rd"] = "Third"
		searchAbbreviations["first"] = "1st"
		searchAbbreviations["second"] = "2nd"
		searchAbbreviations["third"] = "3rd"
		searchAbbreviations["fourth"] = "4th"
		searchAbbreviations["fifth"] = "5th"
		searchAbbreviations["sixth"] = "6th"
		searchAbbreviations["seventh"] = "7th"

		// common housenumber additions
		// french
		buildingAbbreviations["bis"] = "Bis"
		buildingAbbreviations["ter"] = "Ter"
		buildingAbbreviations["quater"] = "Quater"
		// american
		buildingAbbreviations["bldg"] = "Building"
		buildingAbbreviations["ste"] = "Suite"
		buildingAbbreviations["unt"] = "Unit"
		buildingAbbreviations["apt"] = "Apartment"
		buildingAbbreviations["fl"] = "Floor"
		buildingAbbreviations["flr"] = "Floor"
		buildingAbbreviations["bsmt"] = "Basement"
	}

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

	// search v-2
	fun likelyPartOfBuilding(word: String, wordSplit: Set<String>?): Boolean {
		val bldNum = KSearchAlgorithms.isNumber2Letters(word) || word.length == 1
				|| buildingAbbreviations.containsKey(word)
		if (bldNum) {
			return true
		}
		if (wordSplit != null) {
			// recursion for 2bis
			for (w in wordSplit) {
				val likely = likelyPartOfBuilding(w, null)
				if (!likely) {
					return false
				}
			}
			return true
		}
		return false
	}

	// search-v2
	fun getSearchabbreviations(): Map<String, String> = searchAbbreviations

	// search-v2
	fun isCommonSkipOtherCnt(lowerCase: String): Boolean = commonSkipOtherCnt.contains(lowerCase)

	// search-v1
	fun getAbbreviations(): Map<String, String> = abbreviations

	// search v-1
	fun replace(word: String): String = abbreviations[word.lowercase()] ?: word

	// search-v1
	fun isConjunction(lowerCase: String): Boolean = conjunctions.contains(lowerCase)
}
