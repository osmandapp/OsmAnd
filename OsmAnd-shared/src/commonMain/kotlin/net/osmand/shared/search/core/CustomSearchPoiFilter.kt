package net.osmand.shared.search.core

import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.data.Amenity

/**
 * A poi filter of the app that the search offers as a result, such as a saved filter.
 *
 * A copy of `CustomSearchPoiFilter` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS.
 */
interface CustomSearchPoiFilter : SearchPoiTypeFilter {

	fun getFilterId(): String?

	fun getName(): String?

	fun getIconResource(): Any?

	fun wrapResultMatcher(matcher: ResultMatcher<Amenity>): ResultMatcher<Amenity>

	fun getDefaultSearchType(): SearchSettings.SortType? = null
}
