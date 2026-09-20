package net.osmand.shared.binary

import net.osmand.shared.util.collections.KTIntArrayList

/**
 * Decides whether a map object is worth reading, from its type numbers alone, before the rest of
 * it is parsed. The numbers only mean something through [index], which is the section they came
 * from.
 *
 * A copy of `BinaryMapIndexReader.SearchFilter`, which stays in OsmAnd-java.
 */
interface SearchFilter {

	fun accept(types: KTIntArrayList, index: MapIndex): Boolean
}
