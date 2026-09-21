package net.osmand.shared.binary

import net.osmand.shared.util.KMapUtils
import kotlin.jvm.JvmField

/**
 * One node of the r-tree of a map section: a box in 31 coordinates, where its own bytes are, and
 * where the block of map objects hanging off it starts.
 *
 * A copy of `BinaryMapIndexReader.MapTree` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS. Java can keep the class private because [MapRoot] is nested beside
 * it; here it has to be visible, since [MapRoot] extends it and is part of the reader's surface.
 */
open class MapTree {

	@JvmField
	var filePointer: Long = 0

	@JvmField
	var length: Long = 0

	@JvmField
	var mapDataBlock: Long = 0

	/** True for a box wholly at sea, false for one wholly on land, null when it is mixed. */
	@JvmField
	var ocean: Boolean? = null

	@JvmField
	var left: Int = 0

	@JvmField
	var right: Int = 0

	@JvmField
	var top: Int = 0

	@JvmField
	var bottom: Int = 0

	fun getLeft(): Int = left

	fun getRight(): Int = right

	fun getTop(): Int = top

	fun getBottom(): Int = bottom

	fun getLength(): Long = length

	fun getFilePointer(): Long = filePointer

	override fun toString(): String {
		return "Top Lat " + KMapUtils.get31LatitudeY(top).toFloat() +
				" lon " + KMapUtils.get31LongitudeX(left).toFloat() +
				" Bottom lat " + KMapUtils.get31LatitudeY(bottom).toFloat() +
				" lon " + KMapUtils.get31LongitudeX(right).toFloat()
	}
}
