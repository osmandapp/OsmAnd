package net.osmand.shared.binary

import kotlin.jvm.JvmField

/**
 * The root of one zoom level of a map section: the box it covers and the zooms it is drawn at.
 *
 * A copy of `BinaryMapIndexReader.MapRoot` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS. Java's `getMapZoom` is left out, as `MapZooms` belongs to the map
 * creator and never crosses to a device.
 */
class MapRoot : MapTree() {

	@JvmField
	var minZoom: Int = 0

	@JvmField
	var maxZoom: Int = 0

	/** Top level boxes, read the first time this level is searched. */
	internal var trees: MutableList<MapTree>? = null

	fun getMinZoom(): Int = minZoom

	fun getMaxZoom(): Int = maxZoom
}
