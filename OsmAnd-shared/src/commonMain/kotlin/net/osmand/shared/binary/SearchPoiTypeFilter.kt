package net.osmand.shared.binary

import net.osmand.shared.osm.PoiCategory

/**
 * Which kinds of amenity a poi search is after. It is asked twice for each: once for a whole box
 * of the tree, to decide whether the box is worth opening at all, and then for the amenity itself.
 *
 * A copy of `BinaryMapIndexReader.SearchPoiTypeFilter`, which stays in OsmAnd-java.
 */
interface SearchPoiTypeFilter {

	fun accept(type: PoiCategory?, subcategory: String): Boolean

	fun isEmpty(): Boolean
}
