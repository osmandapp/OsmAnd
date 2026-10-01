package net.osmand.shared.routing

import kotlin.jvm.JvmField

/**
 * One node of the routing r-tree of a [RouteRegion]: a bounding box and where its bytes are.
 *
 * A copy of `BinaryMapRouteReaderAdapter.RouteSubregion`, which stays in OsmAnd-java: android, tools and the C++ core keep using
 * the original, and this copy is for iOS. Keep the two identical.
 */
class RouteSubregion {

	@JvmField
	val routeReg: RouteRegion

	@JvmField
	var length: Long = 0

	@JvmField
	var filePointer: Long = 0

	@JvmField
	var left: Int = 0

	@JvmField
	var right: Int = 0

	@JvmField
	var top: Int = 0

	@JvmField
	var bottom: Int = 0

	@JvmField
	var shiftToData: Long = 0

	@JvmField
	var subregions: MutableList<RouteSubregion>? = null

	/** Objects of this node once it has been read, cleared as soon as they are handed out. */
	@JvmField
	var dataObjects: MutableList<RouteDataObject?>? = null

	constructor(routeReg: RouteRegion) {
		this.routeReg = routeReg
	}

	/** Copies the box and the file position, but not the loaded objects. */
	constructor(copy: RouteSubregion) {
		this.routeReg = copy.routeReg
		this.left = copy.left
		this.right = copy.right
		this.top = copy.top
		this.bottom = copy.bottom
		this.filePointer = copy.filePointer
		this.length = copy.length
	}

	fun getEstimatedSize(): Int {
		var shallow = 7 * INT_SIZE + 4 * 3
		val subregions = this.subregions
		if (subregions != null) {
			shallow += 8
			for (s in subregions) {
				shallow += s.getEstimatedSize()
			}
		}
		return shallow
	}

	fun countSubregions(): Int {
		var cnt = 1
		val subregions = this.subregions
		if (subregions != null) {
			for (s in subregions) {
				cnt += s.countSubregions()
			}
		}
		return cnt
	}

	companion object {
		private const val INT_SIZE = 4
	}
}
