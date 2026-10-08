package net.osmand.shared.binary

import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.util.collections.KTIntArrayList
import kotlin.jvm.JvmField

/**
 * One additional attribute a poi section can put on an amenity, with the values it uses for it.
 *
 * A copy of `BinaryMapPoiReaderAdapter.PoiSubType` in OsmAnd-java, which stays there for android
 * and tools; this copy is for iOS. A text subtype carries free text and has no value list; the
 * others encode a value as its position in [possibleValues].
 */
class PoiSubType {

	@JvmField
	var text: Boolean = false

	@JvmField
	var name: String? = null

	@JvmField
	var frequency: Int = 0

	@JvmField
	var possibleValues: MutableList<String>? = null

	@JvmField
	var possibleValuesFreqs: KTIntArrayList? = null

	@JvmField
	var wikidataIds: MutableList<String>? = null

	fun isTopIndex(): Boolean = name?.startsWith(MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX) == true
}
