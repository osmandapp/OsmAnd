package net.osmand.shared.binary

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.util.KCollationKey
import net.osmand.shared.util.KCollatorStringMatcher.Companion.cmatchesKey
import net.osmand.shared.util.collections.KTIntArrayList

/**
 * Copy of `net.osmand.binary.QueryToken` in OsmAnd-java: one word of the query, the prefixes of the
 * poi name index it could sit under, and which of that prefix's suffixes it can still match.
 *
 * The query is reduced to a [KCollationKey] once here, where java rebuilds the comparison from the
 * string on every suffix of every prefix.
 *
 * Internal like the java original, whose constructor is package private: it is scaffolding for
 * reading the name index, not something a caller outside the reader builds.
 */
internal class QueryToken(
	val query: String?,
	val matcherMode: KStringMatcherMode,
	prefixes: List<Prefix>?
) {

	private val queryKey: KCollationKey? = query?.let { KCollationKey.of(it) }

	/** Longest first, then alphabetically, so that the most specific prefix is tried first. */
	val prefixes: List<Prefix> = if (prefixes.isNullOrEmpty()) {
		emptyList()
	} else {
		prefixes.sortedWith(compareByDescending<Prefix> { it.key.length }.thenBy { it.key })
	}

	data class Prefix(val key: String, val offset: Int)

	fun matchFullPrefix(key: String): Boolean = matches(key)

	private fun matches(fullName: String): Boolean {
		val part = queryKey ?: return false
		return cmatchesKey(fullName, part, alignFull = true, mode = matcherMode)
	}

	inner class SuffixMask(private val prefix: Prefix) {

		private var masks: KTIntArrayList? = null
		private var passThrough: Boolean = false

		/** Not exactly correct to maintain state here, as the java original says. */
		private var prevMask: Int = 0

		fun setDictionary(suffixDictionary: List<String>?) {
			passThrough = suffixDictionary == null
			if (suffixDictionary == null) {
				return
			}
			if (suffixDictionary.size == 1 && suffixDictionary[0].isEmpty()) {
				passThrough = query != null && matches(prefix.key)
				return
			}
			if (masks == null) {
				masks = KTIntArrayList()
			}
			if (query == null) {
				return
			}
			for (index in suffixDictionary.indices) {
				addSuffix(index, suffixDictionary[index])
			}
		}

		fun shouldPassThrough(): Boolean = passThrough

		fun isMatched(maskIndex: Int, mask: Int): Boolean {
			val masks = this.masks ?: return true
			if (maskIndex == 0) {
				prevMask = 0
			}
			var res = false
			// use only masks for first and after delimiter
			if (prevMask == 0 && mask % 2 == 0 && masks.contains(mask / 2 - 1)) {
				res = true
			}
			prevMask = mask
			return res
		}

		private fun addSuffix(index: Int, suffix: String?) {
			if (suffix == null || index < 0) {
				return
			}
			if (matches(prefix.key + suffix)) {
				masks?.add(index)
			}
		}
	}
}
