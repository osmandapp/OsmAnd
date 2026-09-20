package net.osmand.shared.search.core

import net.osmand.shared.util.KMapUtils

/**
 * The tile ids of `net.osmand.search.core.HashQuadTree` in OsmAnd-java.
 *
 * Only the encoding is here, which the poi reader needs to say which tile a tag group belongs to.
 * The tree itself belongs to the search and comes with it.
 */
object KHashQuadTree {

	private const val MAX_ZOOM = 20

	fun encodeTileId31(z: Int, x31: Int, y31: Int): Long {
		if (z > MAX_ZOOM) {
			throw UnsupportedOperationException()
		}
		return KMapUtils.interleaveBits((x31 shr (31 - z)).toLong(), (y31 shr (31 - z)).toLong())
	}

	fun encodeTileId(z: Int, x: Int, y: Int): Long {
		if (z > MAX_ZOOM) {
			throw UnsupportedOperationException()
		}
		return KMapUtils.interleaveBits(x.toLong(), y.toLong())
	}
}
