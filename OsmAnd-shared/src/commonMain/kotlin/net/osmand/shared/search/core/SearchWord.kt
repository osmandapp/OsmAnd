package net.osmand.shared.search.core

import net.osmand.shared.data.KLatLon

/**
 * A word of a phrase that a search result was selected for.
 *
 * A copy of `SearchWord` in OsmAnd-java, which stays there for android and tools; this copy is for
 * iOS.
 */
class SearchWord(word: String, private val result: SearchResult?) {

	private var word: String = word.trim { it <= ' ' }

	fun getType(): ObjectType? = if (result == null) ObjectType.UNKNOWN_NAME_FILTER else result.objectType

	fun getWord(): String = word

	fun getResult(): SearchResult? = result

	fun syncWordWithResult() {
		word = result!!.wordsSpan ?: result.localeName!!.trim { it <= ' ' }
	}

	fun getLocation(): KLatLon? = result?.location

	override fun toString(): String = word
}
