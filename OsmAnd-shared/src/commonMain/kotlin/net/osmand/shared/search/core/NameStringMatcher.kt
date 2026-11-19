package net.osmand.shared.search.core

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KStringMatcher

/**
 * Whether a name matches a part of what was typed, the way the search compares names: through the
 * collator, in one of the [KStringMatcherMode]s.
 *
 * Based on `SearchPhrase.NameStringMatcher` from OsmAnd-java. It is a separate class here,
 * available to both `SearchPhrase` and `OsmandRegions`. An empty name matches nothing; java
 * also takes a null one, which [matchesName] does.
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

	/** Matches a non-empty source name whose normalized forms are already available. */
	fun matchesPrepared(name: KCollatorStringMatcher.PreparedName): Boolean =
		KCollatorStringMatcher.cmatchesPrepared(name, sm.part, sm.mode, sm.collator)
}

/** [NameStringMatcher.matches] for a name that may be null, which matches nothing, as in java. */
internal fun NameStringMatcher.matchesName(name: String?): Boolean = name != null && matches(name)
