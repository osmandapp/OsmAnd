package net.osmand.shared.search.core

import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTLongObjectMap

/**
 * Objects by the tile they are put in, a map of tile ids for each zoom: what the spatial search
 * looks up for the objects in a tile and in the tiles around it at the zooms above.
 *
 * A copy of `HashQuadTree` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. The tile ids it encodes are also what the poi reader says a tag group belongs to.
 *
 * This is not exact QuadTree implementation -
 * It doesn't support search for containing objects for performance reasons.
 * In order to find all objects inside large polygon, reverse - HashQuadTree could be used
 * as it requires full scanning of objects and another index
 */
class HashQuadTree<T>(maxZoom: Int) {

	internal val indexByTileId: Array<KTLongObjectMap<MutableList<T>>?> = arrayOfNulls(maxZoom + 1)

	fun put(z: Int, tileId: Long, value: T) {
		val index = indexByTileId[z] ?: KTLongObjectMap<MutableList<T>>().also { indexByTileId[z] = it }
		var list = index[tileId]
		if (list == null) {
			list = ArrayList()
			index.put(tileId, list)
		}
		list.add(value)
	}

	fun delete(z: Int, tileId: Long, value: T) {
		indexByTileId[z]?.get(tileId)?.remove(value)
	}

	fun forEachMatchHigherZoom(startZoom: Int, tileId: Long, action: (List<T>) -> Unit) {
		forEachMatch(startZoom - 1, tileId shr 2, action)
	}

	fun forEachMatch(startZoom: Int, tileId: Long, action: (List<T>) -> Unit) {
		forEachMatch(startZoom, 0, tileId, action)
	}

	fun forEachMatch(startZoom: Int, endZoom: Int, tileId: Long, action: (List<T>) -> Unit) {
		var id = tileId
		for (z in startZoom downTo endZoom) {
			val res = indexByTileId[z]?.get(id)
			if (res != null) {
				action(res)
			}
			id = id shr 2
		}
	}

	companion object {

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
}
