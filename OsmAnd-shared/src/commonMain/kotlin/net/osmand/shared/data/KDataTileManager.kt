package net.osmand.shared.data

import kotlin.jvm.JvmOverloads

import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Spatial index bucketing objects into map tiles of a fixed [zoom], port of
 * `net.osmand.data.DataTileManager`.
 *
 * Backed by [KTLongObjectMap], so tile keys are never boxed.
 */
class KDataTileManager<T> @JvmOverloads constructor(val zoom: Int = DEFAULT_ZOOM) {

	private val objects = KTLongObjectMap<MutableList<T>>()

	fun isEmpty(): Boolean = getObjectsCount() == 0

	fun getObjectsCount(): Int {
		var count = 0
		objects.forEachValue { list -> count += list.size }
		return count
	}

	fun getTilesCount(): Int = objects.size

	fun getAllObjects(): List<T> {
		val result = ArrayList<T>(getObjectsCount())
		objects.forEachValue { list -> result.addAll(list) }
		return result
	}

	/** Per tile lists, exposed for callers that want to walk tiles rather than objects. */
	fun getAllEditObjects(): List<MutableList<T>> = objects.values()

	fun getObjects(
		latitudeUp: Double,
		longitudeUp: Double,
		latitudeDown: Double,
		longitudeDown: Double
	): MutableList<T> {
		var tileXUp = KMapUtils.getTileNumberX(zoom.toDouble(), longitudeUp).toInt()
		var tileYUp = KMapUtils.getTileNumberY(zoom.toDouble(), latitudeUp).toInt()
		var tileXDown = KMapUtils.getTileNumberX(zoom.toDouble(), longitudeDown).toInt() + 1
		var tileYDown = KMapUtils.getTileNumberY(zoom.toDouble(), latitudeDown).toInt() + 1
		val result = ArrayList<T>()
		if (tileXUp > tileXDown) {
			tileXDown = tileXUp
			tileXUp = 0
		}
		if (tileYUp > tileYDown) {
			// the Java original resets tileXUp here, which looks like a typo for tileYUp
			tileYDown = tileYUp
			tileYUp = 0
		}
		for (x in tileXUp..tileXDown) {
			for (y in tileYUp..tileYDown) {
				putObjects(evTile(x, y), result)
			}
		}
		return result
	}

	fun getObjects(leftX31: Int, topY31: Int, rightX31: Int, bottomY31: Int): MutableList<T> =
		getObjects(leftX31, topY31, rightX31, bottomY31, ArrayList())

	fun getObjects(
		leftX31: Int,
		topY31: Int,
		rightX31: Int,
		bottomY31: Int,
		result: MutableList<T>
	): MutableList<T> {
		val shift = 31 - zoom
		val tileXUp = leftX31 shr shift
		val tileYUp = topY31 shr shift
		val tileXDown = (rightX31 shr shift) + 1
		val tileYDown = (bottomY31 shr shift) + 1
		for (x in tileXUp..tileXDown) {
			for (y in tileYUp..tileYDown) {
				putObjects(evTile(x, y), result)
			}
		}
		return result
	}

	/** Objects of every tile whose center falls within [radius] meters, unsorted. */
	fun getClosestObjects(latitude: Double, longitude: Double, radius: Double): MutableList<T> {
		if (isEmpty()) {
			return ArrayList()
		}
		val tileDist = radius / KMapUtils.getTileDistanceWidth(latitude, zoom.toDouble())
		val tileDistInt = ceil(tileDist).toInt()
		val px = KMapUtils.getTileNumberX(zoom.toDouble(), longitude)
		val py = KMapUtils.getTileNumberY(zoom.toDouble(), latitude)
		val stTileX = px.toInt()
		val stTileY = py.toInt()
		val result = ArrayList<T>()
		for (xTile in -tileDistInt..tileDistInt) {
			for (yTile in -tileDistInt..tileDistInt) {
				val dx = xTile + 0.5 - (px - stTileX)
				val dy = yTile + 0.5 - (py - stTileY)
				if (sqrt(dx * dx + dy * dy) <= tileDist) {
					putObjects(evTile(stTileX + xTile, stTileY + yTile), result)
				}
			}
		}
		return result
	}

	fun evaluateTile(latitude: Double, longitude: Double): Long {
		val tileX = KMapUtils.getTileNumberX(zoom.toDouble(), longitude).toInt()
		val tileY = KMapUtils.getTileNumberY(zoom.toDouble(), latitude).toInt()
		return evTile(tileX, tileY)
	}

	fun evaluateTileXY(x31: Int, y31: Int): Long = evTile(x31 shr (31 - zoom), y31 shr (31 - zoom))

	fun registerObject(latitude: Double, longitude: Double, obj: T): Long =
		addObject(obj, evaluateTile(latitude, longitude))

	fun registerObjectXY(x31: Int, y31: Int, obj: T): Long =
		addObject(obj, evTile(x31 shr (31 - zoom), y31 shr (31 - zoom)))

	fun unregisterObject(latitude: Double, longitude: Double, obj: T) {
		removeObject(obj, evaluateTile(latitude, longitude))
	}

	fun unregisterObjectXY(x31: Int, y31: Int, obj: T) {
		removeObject(obj, evaluateTileXY(x31, y31))
	}

	fun clear() {
		objects.clear()
	}

	/** Tile occupancy summary, useful when tuning [zoom]. */
	fun statsDistribution(name: String): String {
		var min = -1
		var max = -1
		var total = 0
		objects.forEachValue { list ->
			if (min == -1) {
				min = list.size
				max = list.size
			} else {
				min = minOf(min, list.size)
				max = maxOf(max, list.size)
			}
			total += list.size
		}
		val tiles = objects.size
		val avg = total / (tiles + 0.1)
		return "$name tiles stores $total in $tiles tiles. Tile size min $min, max $max, avg $avg."
	}

	private fun putObjects(tile: Long, result: MutableList<T>) {
		objects[tile]?.let { result.addAll(it) }
	}

	private fun addObject(obj: T, tile: Long): Long {
		objects.getOrPut(tile) { ArrayList() }.add(obj)
		return tile
	}

	private fun removeObject(obj: T, tile: Long) {
		objects[tile]?.remove(obj)
	}

	private fun evTile(tileX: Int, tileY: Int): Long = (tileX.toLong() shl zoom) + tileY

	companion object {
		const val DEFAULT_ZOOM = 15
	}
}
