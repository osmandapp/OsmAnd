package net.osmand.shared.search.core

import net.osmand.shared.search.SearchUICore.SearchResultMatcher

/**
 * One kind of search the core runs for a phrase: addresses by name, pois by name or by type, a
 * location typed or pasted as a link, and so on.
 *
 * A copy of `SearchCoreAPI` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS.
 */
interface SearchCoreAPI {

	/**
	 * @param p
	 * @return order in which search core apis should be called, -1 means do not call
	 */
	fun getSearchPriority(p: SearchPhrase): Int

	fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean

	/**
	 * @param phrase
	 * @return true if search more available (should be consistent with -1 search priority)
	 */
	fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean

	fun isSearchAvailable(p: SearchPhrase): Boolean

	/**
	 * @param phrase
	 * @return minimal search radius in meters
	 */
	fun getMinimalSearchRadius(phrase: SearchPhrase): Int

	/**
	 * @param phrase
	 * @return next search radius in meters
	 */
	fun getNextSearchRadius(phrase: SearchPhrase): Int
}
