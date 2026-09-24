package net.osmand.shared.search.core

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KStringMatcher

/**
 * Whether a name matches a part of what was typed, the way the search compares names: through the
 * collator, in one of the [KStringMatcherMode]s.
 *
 * A copy of `SearchPhrase.NameStringMatcher` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS. Java nests it in `SearchPhrase`, and here it is a class of its own,
 * so that `OsmandRegions` can use it before the phrase is copied. An empty name matches nothing;
 * java also takes a null one, which callers here check themselves.
 */
class NameStringMatcher(namePart: String, mode: KStringMatcherMode) : KStringMatcher {

	private val sm = KCollatorStringMatcher(namePart, mode)

	fun matches(map: Collection<String>?): Boolean {
		if (map == null) {
			return false
		}
		for (v in map) {
			if (sm.matches(v)) {
				return true
			}
		}
		return false
	}

	fun getStringMatcher(): KCollatorStringMatcher = sm

	override fun matches(name: String): Boolean {
		if (name.isEmpty()) {
			return false
		}
		return sm.matches(name)
	}
}
