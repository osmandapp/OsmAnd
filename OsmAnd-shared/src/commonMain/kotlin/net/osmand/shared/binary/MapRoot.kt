package net.osmand.shared.binary

import kotlin.jvm.JvmField

/**
 * The root of one zoom level of a map section: the box it covers and the zooms it is drawn at.
 *
 * Based on `BinaryMapIndexReader.MapRoot` from OsmAnd-java. `getMapZoom` is not implemented
 * here; it depends on the map creator's `MapZooms` model, which has not been ported.
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
