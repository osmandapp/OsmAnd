package net.osmand.shared.search.rules

import net.osmand.shared.search.rules.SearchModLocaleRules.Companion.ANY_OBJECT
import net.osmand.shared.search.rules.SearchModLocaleRules.Scope
import net.osmand.shared.search.rules.SearchModLocaleRules.Scoped
import net.osmand.shared.search.rules.SearchModLocaleRules.WordRule
import net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner
import net.osmand.shared.util.KSearchAlgorithms

/**
 * Spatial search (v2) word dictionaries of the [SearchModLocaleRules] of one locale, see
 * [SearchModRules.dictionary]. Immutable.
 *
 * A copy of `SearchModDictionary` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS. Without `isIgnorable`, which nothing calls.
 */
class SearchModDictionary internal constructor(private val locale: String, rules: SearchModLocaleRules) {

	// query word -> its forms and reverse forms, in the order of the rules
	private val forms = HashMap<String, List<QueryForm>>()
	private val buildingWords: Set<String>
	// aligned word of a name -> owners whose names it does not penalize (<skipPenalty>, ignorable words)
	private val penaltyFree: Map<String, Set<SearchModRuleOwner>>

	init {
		val ruleOfWord = HashMap<String, WordRule>()
		for (rule in rules.query()) {
			ruleOfWord[rule.word] = rule
		}
		for (word in rules.formWords()) {
			val list = ArrayList<QueryForm>()
			for (form in rules.forms(word)) {
				// a reverse form leads to the word of a rule that has this word as its form ("street" -> "st")
				val other = ruleOfWord[form.word]
				list.add(QueryForm(form.`object`, form.word, other != null && other.hasForm(word), form.parseOwners()))
			}
			// a token is aligned (ß -> ss, no diacritics): "Straße" asks for "strasse"
			forms[KSearchAlgorithms.alignChars(word)] = list
		}
		// the words of <skipPenalty> are aligned by the rules, the ignorable words are query words
		val owners = HashMap<String, MutableSet<String>>()
		for ((word, list) in rules.skipPenalty()) {
			owners.getOrPut(word) { LinkedHashSet() }.addAll(list)
		}
		for (word in rules.ignorables()) {
			owners.getOrPut(KSearchAlgorithms.alignChars(word)) { LinkedHashSet() }.add(ANY_OBJECT)
		}
		val penaltyFree = HashMap<String, Set<SearchModRuleOwner>>()
		for ((word, set) in owners) {
			val wordOwners = LinkedHashSet<SearchModRuleOwner>()
			for (`object` in set) {
				wordOwners.addAll(Scope(`object`).parseOwners())
			}
			penaltyFree[word] = wordOwners
		}
		this.buildingWords = rules.buildings()
		this.penaltyFree = penaltyFree
	}

	/** rules locale of the dictionary, "" for the base rules */
	fun locale(): String = locale

	/**
	 * A form of a query word: the word of a name it stands for and the owners of names it applies to; a reverse form
	 * leads from a full word to its abbreviation ("street" -> "st").
	 */
	data class QueryForm(
		override val `object`: String,
		val word: String,
		val reverse: Boolean,
		private val owners: Set<SearchModRuleOwner>
	) : Scoped {

		override fun owners(): Set<SearchModRuleOwner> = owners

		/** true when the form applies to every owner of a name */
		fun isUnscoped(): Boolean = ANY_OBJECT == `object`
	}

	/** Only one-word forms can be matched against one name-index atom, the rules load only such rules. */
	fun getQueryForms(word: String): List<QueryForm> =
		forms[KSearchAlgorithms.alignChars(word.lowercase())] ?: emptyList()

	/** @return true when a query word ("12", "2b", "apt", "bis" of the locale) is a part of a house number */
	fun likelyPartOfBuilding(word: String, wordSplit: Set<String>?): Boolean {
		if (KSearchAlgorithms.isNumber2Letters(word) || word.length == 1 || buildingWords.contains(word)) {
			return true
		}
		if (wordSplit != null) {
			// recursion for 2bis
			for (w in wordSplit) {
				if (!likelyPartOfBuilding(w, null)) {
					return false
				}
			}
			return true
		}
		return false
	}

	/**
	 * search-v2: a word of a name that the query does not have does not penalize the object: `<skipPenalty>`
	 * for the owner or an ignorable word (`to=""`) of the locale
	 *
	 * @param owner owner of the name: street, locality, boundary, postcode, poi
	 */
	fun isCommonSkipOtherCnt(lowerCase: String, owner: SearchModRuleOwner): Boolean {
		val owners = penaltyFree[aligned(lowerCase)]
		return owners != null && owners.contains(owner)
	}

	// the dictionaries keep aligned words: "école" is "ecole", "straße" is "strasse"; most words of names need no work
	private fun aligned(lowerCase: String): String {
		for (c in lowerCase) {
			if (c.code >= 128 || c == '\'' || c == '`') {
				return KSearchAlgorithms.alignChars(lowerCase)
			}
		}
		return lowerCase
	}

	override fun toString(): String = "rules '$locale'"
}
