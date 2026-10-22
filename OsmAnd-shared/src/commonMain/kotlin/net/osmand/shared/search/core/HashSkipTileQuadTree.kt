package net.osmand.shared.search.core

import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTLongArrayList
import net.osmand.shared.util.collections.KTLongObjectMap

/**
 * Boxes by the tiles they cover at the zoom they fit in, sorted by zoom and tile, with an index of
 * the tiles of lower zooms over each zoom that lets a walk skip a whole parent tile at once: what
 * the spatial search finds the boxes of a box in and joins two sets of boxes with, see
 * [HashSkipTileQuadTreeJoiner].
 *
 * A copy of `HashSkipTileQuadTree` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS. Left out are the statistics of the skips, `SkipStats`, which only the
 * benchmarks of OsmAnd-java read, and `TreeExtraIndex`, which nothing uses.
 */
class HashSkipTileQuadTree<T>(minZoom: Int, maxZoom: Int, indexedZooms: IntArray) {

	internal val minZoom: Int = minZoom
	internal val maxZoom: Int = maxZoom
	internal val indxZooms: IntArray = indexedZooms
	internal var zoomBuckets: Array<ZoomBucket?>? = null
	internal var tileEntries: MutableList<TileEntry<T>> = ArrayList()
	internal var modified: KTLongObjectMap<MutableList<TileEntry<T>>>? = null
	// the ids of modified in the order they were added, which a hash map does not keep: java builds them in the order
	// of its hashes
	internal val modifiedOrder = KTLongArrayList()

	constructor() : this(DEFALT_MIN_ZOOM, DEFALT_MAX_ZOOM, DEFAULT_INDEXED_ZOOMS)

	class TileEntry<T> internal constructor(
		val objId: Long,
		val obj: T,
		val bbox31: IntArray,
		val z: Int,
		val tileId: Long
	) {
		var skipNextTileId = -1
	}

	/**
	 * The walk over the index of a bucket. Every read of its lists stands after a check of their size, so it reads
	 * them unchecked: on Kotlin/Native a checked read is a call that builds its message, a third of a join.
	 */
	internal class ZoomBucketIndexTreeIterator(private val bucket: ZoomBucket) {

		private val nodes: Array<ZoomBucketIndexTree?>
		private val blockIndices: IntArray

		init {
			val depth = bucket.indexedZooms.size
			nodes = arrayOfNulls(depth)
			blockIndices = IntArray(depth)
			var curr = bucket.indx
			for (i in 0 until depth) {
				nodes[i] = curr
				blockIndices[i] = 0
				curr = curr?.subTree
			}
		}

		fun checkSkip(currentIndex: Int, tileId: Long, tileZoom: Int, queryBBox: IntArray, endIndex: Int): Int {
			sync(currentIndex)
			for (level in nodes.indices) {
				val indxZoom = nodes[level]!!.indxZoom
				val parentTileId = tileId shr ((tileZoom - indxZoom) * 2)
				if (!intersectsTile(parentTileId, indxZoom, queryBBox)) {
					return skipBlock(nodes[0], level, 0, endIndex)
				}
			}
			return -1
		}

		fun sync(globalIndex: Int) {
			for (level in nodes.indices) {
				syncSingleLevel(level, globalIndex)
			}
		}

		private fun syncSingleLevel(level: Int, globalIndex: Int) {
			val tree = nodes[level]!!
			var blockIdx = blockIndices[level]
			val skipIndexes = tree.skipIndexes
			while (blockIdx + 1 < skipIndexes.size() && skipIndexes.getQuick(blockIdx + 1) <= globalIndex) {
				blockIdx++
			}
			blockIndices[level] = blockIdx
		}

		private fun skipBlock(tree: ZoomBucketIndexTree?, targetLevel: Int, currentLevel: Int, endIndex: Int): Int {
			if (currentLevel < targetLevel) {
				val nextGlobalIndex = skipBlock(tree!!.subTree, targetLevel, currentLevel + 1, endIndex)
				if (nextGlobalIndex != endIndex) {
					syncSingleLevel(currentLevel, nextGlobalIndex)
				}
				return nextGlobalIndex
			}
			val nextBlockIdx = blockIndices[targetLevel] + 1
			blockIndices[targetLevel] = nextBlockIdx
			if (nextBlockIdx >= tree!!.skipIndexes.size()) {
				return endIndex
			}
			val nextGlobalIndex = tree.skipIndexes.getQuick(nextBlockIdx)
			resetChildren(tree, targetLevel, nextBlockIdx)
			return nextGlobalIndex
		}

		private fun resetChildren(tree: ZoomBucketIndexTree?, level: Int, blockIdx: Int) {
			if (tree == null || tree.subTree == null || level + 1 >= nodes.size) {
				return
			}
			if (blockIdx < tree.skipInSubIndexes.size()) {
				val subBlockIdx = tree.skipInSubIndexes.getQuick(blockIdx)
				blockIndices[level + 1] = subBlockIdx
				resetChildren(tree.subTree, level + 1, subBlockIdx)
			}
		}

		fun skipToTileId(currentIndex: Int, targetTileId: Long): Int {
			if (nodes.isEmpty()) {
				return -1
			}
			sync(currentIndex)
			val bucketEndIndex = bucket.start + bucket.len
			for (level in nodes.indices) {
				val tree = nodes[level]!!
				val blockIdx = blockIndices[level]
				if (blockIdx < tree.tileIds.size()) {
					val shift = (bucket.z - tree.indxZoom) * 2
					val targetParentAtLevel = targetTileId shr shift
					val currentBlockParent = tree.tileIds.getQuick(blockIdx)
					if (currentBlockParent < targetParentAtLevel) {
						val levelEndIndex = if (blockIdx + 1 < tree.skipIndexes.size()) tree.skipIndexes.getQuick(blockIdx + 1) else bucketEndIndex
						val nextIndex = skipBlock(tree, level, level, levelEndIndex)
						if (nextIndex > currentIndex) {
							return nextIndex
						}
					}
				}
			}
			return -1
		}
	}

	internal class ZoomBucketIndexTree(indZoom: Int, indexedZooms: IntArray) {
		val indxZoom: Int = indexedZooms[indZoom]
		val subTree: ZoomBucketIndexTree? =
			if (indZoom < indexedZooms.size - 1) ZoomBucketIndexTree(indZoom + 1, indexedZooms) else null
		// start index in global list
		val skipIndexes = KTIntArrayList()
		// start index in sub tree
		val skipInSubIndexes = KTIntArrayList()
		// for debug only
		val tileIds = KTLongArrayList()
		// used only for build
		var lastTileId = -1L

		fun processEntry(tileIdZoom: Int, tileId: Long, globalIndex: Int): Boolean {
			var changed = false
			val currentParentTileId = tileId shr ((tileIdZoom - indxZoom) * 2)
			if (currentParentTileId != lastTileId) {
				if (subTree != null) {
					skipInSubIndexes.add(subTree.skipIndexes.size())
				}
				skipIndexes.add(globalIndex)
				lastTileId = currentParentTileId
				tileIds.add(currentParentTileId)
				changed = true
			}
			subTree?.processEntry(tileIdZoom, tileId, globalIndex)
			return changed
		}
	}

	internal class ZoomBucket(var z: Int, var start: Int, var indexedZooms: IntArray) {
		var len = 0
		var indx: ZoomBucketIndexTree? = if (indexedZooms.isNotEmpty()) ZoomBucketIndexTree(0, indexedZooms) else null
	}

	fun addObject(obj: T, bbox31: IntArray) {
		addObject(obj, bbox31, -1)
	}

	fun addObject(obj: T, bbox31: IntArray, externalId: Long) {
		val objId = if (externalId == -1L) tileEntries.size.toLong() else externalId
		addTileEntry(obj, bbox31, objId, getNativeZoom(bbox31))
	}

	fun getNativeZoom(bbox31: IntArray): Int {
		var targetZoom = maxZoom
		// Fit into 2x2 - maximum 4 tiles
		while (targetZoom > minZoom) {
			val shift = 31 - targetZoom
			val minX = bbox31[0] shr shift
			val maxX = bbox31[2] shr shift
			val minY = bbox31[1] shr shift
			val maxY = bbox31[3] shr shift
			if (maxX - minX <= 1 && maxY - minY <= 1) {
				if ((minX shr 1) == (maxX shr 1) && (minY shr 1) == (maxY shr 1)) {
					targetZoom--
				}
				break
			}
			targetZoom--
		}
		return targetZoom
	}

	private fun addTileEntry(obj: T, bbox31: IntArray, objId: Long, targetZoom: Int) {
		val shift = 31 - targetZoom
		val minX = bbox31[0] shr shift
		val maxX = bbox31[2] shr shift
		val minY = bbox31[1] shr shift
		val maxY = bbox31[3] shr shift
		for (x in minX..maxX) {
			for (y in minY..maxY) {
				val te = TileEntry(objId, obj, bbox31, targetZoom, encodeTileId(x, y))
				val modified = modified
				if (modified != null) {
					var list = modified[objId]
					if (list == null) {
						list = ArrayList()
						modified.put(objId, list)
						modifiedOrder.add(objId)
					}
					list.add(te)
				} else {
					tileEntries.add(te)
				}
			}
		}
	}

	fun build(): Int {
		val modified = modified
		if (modified != null) {
			val entries = ArrayList<TileEntry<T>>()
			for (e in tileEntries) {
				if (!modified.containsKey(e.objId)) {
					entries.add(e)
				}
			}
			for (i in 0 until modifiedOrder.size()) {
				entries.addAll(modified[modifiedOrder[i]]!!)
			}
			tileEntries = entries
		}
		val buckets = arrayOfNulls<ZoomBucket>(maxZoom + 1)
		zoomBuckets = buckets
		tileEntries.sortWith { e1, e2 ->
			if (e1.z != e2.z) e1.z.compareTo(e2.z) else e1.tileId.compareTo(e2.tileId)
		}
		var zoomBucket: ZoomBucket? = null
		var lastZoom = -1
		var lastTileId = -1L
		var lastTileIdFirstInd = -1
		var index = 0
		while (index < tileEntries.size) {
			val tileE = tileEntries[index]
			val tileId = tileE.tileId
			val tileZoom = tileE.z
			if (lastTileId != tileId || lastZoom != tileZoom) {
				if (lastTileIdFirstInd >= 0) {
					tileEntries[lastTileIdFirstInd].skipNextTileId = index
				}
				lastTileId = tileId
				lastTileIdFirstInd = index
			}
			if (lastZoom != tileZoom) {
				lastZoom = tileZoom
				val indexedZooms = KTIntArrayList()
				for (iz in indxZooms) {
					if (iz >= tileE.z) {
						break
					}
					indexedZooms.add(iz)
				}
				zoomBucket = ZoomBucket(lastZoom, index, indexedZooms.toArray())
				buckets[tileE.z] = zoomBucket
			}
			val bucket = zoomBucket!!
			bucket.indx?.processEntry(lastZoom, tileId, index)
			bucket.len++
			index++
		}
		if (lastTileIdFirstInd >= 0) {
			tileEntries[lastTileIdFirstInd].skipNextTileId = index
		}
		this.modified = KTLongObjectMap()
		modifiedOrder.clear()
		return tileEntries.size
	}

	internal inner class TileIterator(
		private val bucket: ZoomBucket,
		private val queryBBox: IntArray
	) : Iterator<TileEntry<T>> {

		private val endIndex = bucket.start + bucket.len
		private val treeIt = ZoomBucketIndexTreeIterator(bucket)
		private var currentIndex = bucket.start
		private var nextEntry: TileEntry<T>? = null

		init {
			advance()
		}

		override fun hasNext(): Boolean = nextEntry != null

		override fun next(): TileEntry<T> {
			val current = nextEntry ?: throw NoSuchElementException()
			advance()
			return current
		}

		private fun advance() {
			nextEntry = null
			while (currentIndex < endIndex) {
				val entry = tileEntries[currentIndex]
				val newIndex = treeIt.checkSkip(currentIndex, entry.tileId, entry.z, queryBBox, endIndex)
				if (newIndex >= 0) {
					currentIndex = newIndex
					continue
				}
				if (!intersectsTile(entry.tileId, entry.z, queryBBox)) {
					currentIndex = if (entry.skipNextTileId > 0) entry.skipNextTileId else currentIndex + 1
					continue
				}
				if (intersectsBBox(entry.bbox31, queryBBox)) {
					nextEntry = entry
					currentIndex++
					return
				}
				currentIndex++
			}
		}
	}

	internal fun getZoomBucket(z: Int): ZoomBucket? {
		checkIsBuilt()
		return zoomBuckets!![z]
	}

	fun isBuilt(): Boolean = modified != null

	private fun checkIsBuilt() {
		if (modified == null) {
			throw UnsupportedOperationException("Not yet built")
		}
	}

	fun contains(queryBBox: IntArray): Boolean {
		checkIsBuilt()
		for (z in minZoom..maxZoom) {
			val zoomBucket = zoomBuckets!![z]
			if (zoomBucket != null) {
				if (TileIterator(zoomBucket, queryBBox).hasNext()) {
					return true
				}
			}
		}
		return false
	}

	fun get(queryBBox: IntArray): List<TileEntry<T>> {
		checkIsBuilt()
		return get(queryBBox, minZoom, maxZoom)
	}

	fun get(queryBBox: IntArray, minZ: Int, maxZ: Int): List<TileEntry<T>> {
		checkIsBuilt()
		val res = ArrayList<TileEntry<T>>()
		for (z in minZ..maxZ) {
			val zoomBucket = zoomBuckets!![z]
			if (zoomBucket != null) {
				val ti = TileIterator(zoomBucket, queryBBox)
				while (ti.hasNext()) {
					res.add(ti.next())
				}
			}
		}
		return res
	}

	fun isEmpty(): Boolean = tileEntries.isEmpty()

	fun getSize(): Int = tileEntries.size

	fun getTileEntries(): List<TileEntry<T>> = tileEntries

	companion object {

		const val DEFALT_MAX_ZOOM = 16
		const val DEFALT_MIN_ZOOM = 0
		val DEFAULT_INDEXED_ZOOMS = intArrayOf(1, 3, 5, 8, 11)

		fun encodeTileId(x: Int, y: Int): Long = KMapUtils.interleaveBits(x.toLong(), y.toLong())

		fun intersectsBBox(a: IntArray, b: IntArray): Boolean =
			a[0] <= b[2] && a[2] >= b[0] && a[1] <= b[3] && a[3] >= b[1]

		private fun intersectsTile(parentTileId: Long, parentZoom: Int, queryBBox: IntArray): Boolean {
			val shift = 31 - parentZoom
			val tileX = KMapUtils.deinterleaveX(parentTileId).toInt()
			val tileY = KMapUtils.deinterleaveY(parentTileId).toInt()
			val tileMinX = tileX shl shift
			val tileMaxX = ((tileX + 1) shl shift) - 1
			val tileMinY = tileY shl shift
			val tileMaxY = ((tileY + 1) shl shift) - 1
			return tileMinX <= queryBBox[2] && tileMaxX >= queryBBox[0] && tileMinY <= queryBBox[3] &&
					tileMaxY >= queryBBox[1]
		}
	}
}
