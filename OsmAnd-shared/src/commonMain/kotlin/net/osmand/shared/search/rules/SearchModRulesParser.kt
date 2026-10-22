package net.osmand.shared.search.rules

import net.osmand.shared.search.rules.SearchModLocaleRules.Companion.ANY_OBJECT
import net.osmand.shared.search.rules.SearchModLocaleRules.Companion.BUILDING_OBJECT
import net.osmand.shared.search.rules.SearchModLocaleRules.Companion.VERSION
import net.osmand.shared.search.rules.SearchModLocaleRules.Form
import net.osmand.shared.search.rules.SearchModLocaleRules.SkipPenalty
import net.osmand.shared.search.rules.SearchModLocaleRules.WordRule
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.xml.XmlPullParser
import okio.Source

/**
 * Reads one rules file (`rules.xml`, `rules_<locale>.xml`) into a [Layer], see [SearchModLocaleRules].
 *
 * A copy of the search part of `SearchModRulesParser` in OsmAnd-java: what java reads with
 * `indexRules` false, the query rules, the mirror pairs and the map prefixes of `<locales>`. The
 * files are checked by the unit tests of OsmAnd-java (SearchModRulesValidator), not here.
 *
 * @param locales the table that `<locales>` of rules.xml fills, null for a file without it
 */
internal class SearchModRulesParser(private val file: String, private val locales: SearchModLocales?) {

	private val layer = Layer()

	/** The rules of one file; a disabled rule removes the rule of an upper layer with its key. */
	internal class Layer {
		val query = LinkedHashMap<String, WordRule>()
		val skipPenalty = LinkedHashMap<String, SkipPenalty>()
		val disabledQuery = LinkedHashSet<String>()
		val disabledSkipPenalty = LinkedHashSet<String>()

		fun add(rule: WordRule, enabled: Boolean) {
			if (enabled) {
				query[rule.word] = rule
			} else {
				disabledQuery.add(rule.word)
			}
		}

		fun addSkipPenalty(`object`: String, words: List<String>, enabled: Boolean) {
			for (w in words) {
				// a word of a name is looked up aligned ("straße" is "strasse")
				val entry = SkipPenalty(KSearchAlgorithms.alignChars(w), `object`)
				if (enabled) {
					skipPenalty[entry.key()] = entry
				} else {
					disabledSkipPenalty.add(entry.key())
				}
			}
		}
	}

	/** @return the rules of the file; `<locales>` of rules.xml go to the [SearchModLocales] of the parser */
	fun parse(input: Source): Layer {
		val parser = XmlPullParser()
		try {
			parser.setInput(input, "UTF-8")
			var section: String? = null
			while (true) {
				val event = parser.next()
				if (event == XmlPullParser.END_DOCUMENT) {
					break
				}
				if (event == XmlPullParser.START_TAG) {
					val tag = parser.getName()
					val depth = parser.getDepth()
					if (depth == 1 && "rules" == tag) {
						if (VERSION != parser.getAttributeValue(null, "version")) {
							throw IllegalArgumentException("Unsupported search rules version in $file, expected $VERSION")
						}
					} else if (depth == 2) {
						section = tag
						if ("rule" == tag) {
							parseMirrorRule(parser)
						}
					} else if (depth == 3 && "query" == section) {
						if ("rule" == tag) {
							parseQueryRule(parser)
						} else if ("skipPenalty" == tag) {
							val `object` = objectOf(parser.getAttributeValue(null, "object"))
							val enabled = enabled(parser)
							layer.addSkipPenalty(`object`, words(parser.nextText()), enabled)
						}
					} else if (depth == 3 && "locales" == section && locales != null) {
						// <group> and the statistics group and transliteration of <map> serve the OBF writer
						if ("map" == tag) {
							locales.addMap(parser.getAttributeValue(null, "locale"),
								words(parser.getAttributeValue(null, "prefixes")))
						}
					}
				} else if (event == XmlPullParser.END_TAG && parser.getDepth() == 2) {
					if ("locales" == section && locales != null) {
						locales.build()
					}
					section = null
				}
			}
		} finally {
			parser.close()
		}
		return layer
	}

	/**
	 * A mirror pair, a `<rule>` directly under `<rules>` (rules-spec.md, 3.4): one word and its one form,
	 * a query form both ways. It shares the key of a query rule in the layers.
	 */
	private fun parseMirrorRule(parser: XmlPullParser) {
		val word = parser.getAttributeValue(null, "from")!!.lowercase()
		if (!enabled(parser)) {
			layer.add(WordRule(word, emptyList()), false)
			return
		}
		val to = trimLikeJava(parser.getAttributeValue(null, "to")!!)
		layer.add(WordRule(word, listOf(form(parser.getAttributeValue(null, "object"), to))), true)
	}

	/**
	 * One form is the attributes of the rule (`to`, `object`; `object="building"` without `to`),
	 * several forms are `<to>` elements with their own `object`.
	 */
	private fun parseQueryRule(parser: XmlPullParser) {
		val word = parser.getAttributeValue(null, "from")!!.lowercase()
		val to = parser.getAttributeValue(null, "to")
		val `object` = parser.getAttributeValue(null, "object")
		val enabled = enabled(parser)
		val forms = ArrayList<Form>()
		while (parser.nextTag() == XmlPullParser.START_TAG) {
			forms.add(form(parser.getAttributeValue(null, "object"), trimLikeJava(parser.nextText())))
		}
		if (!enabled) {
			layer.add(WordRule(word, emptyList()), false)
			return
		}
		if (forms.isEmpty()) {
			forms.add(form(`object`, if (to == null) "" else trimLikeJava(to)))
		}
		layer.add(WordRule(word, forms), true)
	}

	// an empty text is a house-number qualifier (object="building") or an ignorable word
	private fun form(`object`: String?, text: String): Form {
		val o = objectOf(`object`)
		if (o == BUILDING_OBJECT || text.isEmpty()) {
			return Form(o, "")
		}
		return Form(o, KSearchAlgorithms.splitAndNormalize(text, false)[0])
	}

	private fun objectOf(`object`: String?): String = if (`object`.isNullOrEmpty()) ANY_OBJECT else `object`

	private fun enabled(parser: XmlPullParser): Boolean = "false" != parser.getAttributeValue(null, "enabled")

	// java's value.trim().split("\\s+") without the empty parts, each in lower case
	private fun words(value: String?): List<String> {
		val result = ArrayList<String>()
		if (value != null) {
			val s = trimLikeJava(value)
			var start = 0
			for (i in 0..s.length) {
				if (i == s.length || isJavaSpace(s[i])) {
					if (i > start) {
						result.add(s.substring(start, i).lowercase())
					}
					start = i + 1
				}
			}
		}
		return result
	}

	private fun isJavaSpace(c: Char): Boolean =
		c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\u000C' || c == '\r'

	companion object {
		/** java's `String.trim()`: what is not above a space, at both ends. */
		internal fun trimLikeJava(s: String): String = s.trim { it <= ' ' }
	}
}
