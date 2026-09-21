package net.osmand.shared.binary

/**
 * Which values of an additional attribute a poi search is after - "the cuisine is pizza", say.
 * Only the attributes the file marks as top index can be filtered this way, because only those
 * are written into the boxes of the tree.
 *
 * A copy of `BinaryMapIndexReader.SearchPoiAdditionalFilter`, which stays in OsmAnd-java.
 */
interface SearchPoiAdditionalFilter {

	fun accept(poiSubType: PoiSubType, value: String): Boolean

	fun getName(): String?

	fun getIconResource(): String?
}
