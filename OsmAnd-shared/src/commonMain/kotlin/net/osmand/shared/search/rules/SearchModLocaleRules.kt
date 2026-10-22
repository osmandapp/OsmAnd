package net.osmand.shared.search.rules

import net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner

/**
 * The query rules of one locale (schema v5, see `resources/abbr/rules-spec.md`): the forms of query
 * words, the words of house numbers, the ignorable words and the words of names that do not
 * penalize an object.
 *
 * Inside `<query>` the search gives a query word its forms: a word with one meaning has the
 * attributes `to` and `object` (`<rule from="pl" to="Place" object="street"/>`), a word with several
 * meanings lists them as `<to>` elements with their own `object`, the first is the main one. Every
 * form gets a reverse form of the same owner; a reverse form is one step. `object="building"`
 * without `to` marks the word as a part of a house number, `to=""` makes it an ignorable word of
 * every owner (an optional query token that never penalizes a name). `<skipPenalty>` lists the
 * words of names that do not penalize an object when the query does not have them, optionally for
 * some owners only. A mirror pair, a `<rule>` directly under `<rules>`, is a query word and its one
 * form.
 *
 * Rules are layered from general to specific: `rules.xml` (every map), `rules_<language>.xml`, then
 * `rules_<language>_<COUNTRY>.xml`. A rule of a lower layer with the key of an upper one replaces it
 * whole, `enabled="false"` removes it.
 *
 * A copy of the query part of `SearchModLocaleRules` in OsmAnd-java, which stays there for android,
 * the OBF writer and tools; this copy is for iOS. The rules of `<index>`, `<unglue>`, the word
 * classes and `<poi>` serve only the OBF writer, as do the index rules of mirror pairs.
 *
 * @param layers rules files from general to specific: a lower layer replaces or disables rules of upper ones
 */
class SearchModLocaleRules internal constructor(layers: List<SearchModRulesParser.Layer>) {

	private val query: List<WordRule>
	// aligned word -> owners of the <skipPenalty> entries of the word
	private val skipPenalty: Map<String, List<String>>
	// word -> its forms and then the reverse forms that lead to it
	private val forms: Map<String, List<Form>>
	private val buildings: Set<String>
	private val ignorables: Set<String>

	init {
		val query = LinkedHashMap<String, WordRule>()
		val skipPenalty = LinkedHashMap<String, SkipPenalty>()
		for (layer in layers) {
			query.keys.removeAll(layer.disabledQuery)
			skipPenalty.keys.removeAll(layer.disabledSkipPenalty)
			query.putAll(layer.query)
			skipPenalty.putAll(layer.skipPenalty)
		}
		val buildings = LinkedHashSet<String>()
		val ignorables = LinkedHashSet<String>()
		val forms = LinkedHashMap<String, MutableSet<Form>>()
		for (rule in query.values) {
			for (form in rule.forms) {
				if (form.isBuilding()) {
					buildings.add(rule.word)
				} else if (form.isIgnorable()) {
					ignorables.add(rule.word)
				} else {
					forms.getOrPut(rule.word) { LinkedHashSet() }.add(form)
				}
			}
		}
		for (rule in query.values) {
			for (form in rule.forms) {
				if (form.isBuilding() || form.isIgnorable()) {
					continue
				}
				forms.getOrPut(form.word) { LinkedHashSet() }.add(Form(form.`object`, rule.word))
			}
		}
		val skip = LinkedHashMap<String, MutableList<String>>()
		for (entry in skipPenalty.values) {
			skip.getOrPut(entry.word) { ArrayList() }.add(entry.`object`)
		}
		val lists = LinkedHashMap<String, List<Form>>()
		for ((word, set) in forms) {
			lists[word] = ArrayList(set)
		}
		this.query = ArrayList(query.values)
		this.skipPenalty = skip
		this.forms = lists
		this.buildings = buildings
		this.ignorables = ignorables
	}

	/** Rules of query words in the order of the files. */
	fun query(): List<WordRule> = query

	/** Forms of a query word: the forms of its rule, then the reverse forms of the rules whose form it is. */
	fun forms(word: String): List<Form> = forms[word.lowercase()] ?: emptyList()

	/** Words with a form or a reverse form. */
	fun formWords(): Set<String> = forms.keys

	/** Words (`object="building"`) that are a part of a house number. */
	fun buildings(): Set<String> = buildings

	/** Words with an empty `to`: optional query words that never penalize a name (conjunctions, articles). */
	fun ignorables(): Set<String> = ignorables

	/** Words of `<skipPenalty>` aligned (`KSearchAlgorithms.alignChars`) -> the owners they apply to. */
	fun skipPenalty(): Map<String, List<String>> = skipPenalty

	/** A rule part with an `object`: the owners of names it applies to ("street", "*", "street,poi"). */
	interface Scoped {
		val `object`: String

		/** @return the owners of [object]; a part checked per name keeps them */
		fun owners(): Set<SearchModRuleOwner> = parseOwners()

		/** @return the owners of [object] in the order of [SearchModRuleOwner] */
		fun parseOwners(): Set<SearchModRuleOwner> {
			val owners = BooleanArray(SearchModRuleOwner.entries.size)
			for (type in `object`.split(',')) {
				// java's trim(): what is not above a space
				val t = type.trim { it <= ' ' }
				for (o in SearchModRuleOwner.entries) {
					if (t == ANY_OBJECT || t == o.tag) {
						owners[o.ordinal] = true
					}
				}
			}
			return SearchModRuleOwner.entries.filterTo(LinkedHashSet()) { owners[it.ordinal] }
		}

		/** @return true when it applies to the owner of a name */
		fun appliesTo(owner: SearchModRuleOwner): Boolean = owners().contains(owner)
	}

	/** The owners of names of a `<skipPenalty>` entry or of a word that never penalizes a name. */
	data class Scope(override val `object`: String) : Scoped

	internal data class SkipPenalty(val word: String, override val `object`: String) : Scoped {
		fun key(): String = word + '\n' + `object`
	}

	/**
	 * A form of a query word: the word of a name it stands for and the owners of names it applies to. An empty word is
	 * a house-number qualifier (`object="building"`) or an ignorable word.
	 */
	data class Form(override val `object`: String, val word: String) : Scoped {
		fun isBuilding(): Boolean = `object` == BUILDING_OBJECT

		fun isIgnorable(): Boolean = word.isEmpty() && !isBuilding()
	}

	/** The rule of a query word: every meaning of the word, the first is the main one. */
	data class WordRule(val word: String, val forms: List<Form>) {

		internal fun hasForm(form: String): Boolean = forms.any { it.word == form }
	}

	companion object {
		const val ANY_OBJECT = "*"
		const val BUILDING_OBJECT = "building"
		const val VERSION = "5"
		const val BASE_FILE = "rules.xml"
	}
}
