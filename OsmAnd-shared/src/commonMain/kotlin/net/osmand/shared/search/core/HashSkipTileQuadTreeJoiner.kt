package net.osmand.shared.search.core

import net.osmand.shared.search.core.HashSkipTileQuadTree.TileEntry
import net.osmand.shared.search.core.HashSkipTileQuadTree.ZoomBucket
import net.osmand.shared.search.core.HashSkipTileQuadTree.ZoomBucketIndexTreeIterator
import kotlin.math.abs
import kotlin.math.max

/**
 * The pairs of boxes of two [HashSkipTileQuadTree]s that intersect, each pair called back once: the
 * buckets of the two trees are walked side by side, zoom against zoom, and the index of each bucket
 * skips the parent tiles the other one has nothing in.
 *
 * A copy of `HashSkipTileQuadTreeJoiner` in OsmAnd-java, which stays there for android and tools;
 * this copy is for iOS. Without the statistics of the skips, as [HashSkipTileQuadTree].
 *
 * @param T payload type for the first tree.
 * @param R payload type for the second tree.
 */
class HashSkipTileQuadTreeJoiner<T, R>(
	private val tree1: HashSkipTileQuadTree<T>,
	private val tree2: HashSkipTileQuadTree<R>
) {

	fun interface JoinCallback<T, R> {
		fun onIntersection(e1: TileEntry<T>, e2: TileEntry<R>)
	}

	fun getTree1(): HashSkipTileQuadTree<T> = tree1

	fun getTree2(): HashSkipTileQuadTree<R> = tree2

	/** Performs spatial join across all zoom bucket pairs between tree1 and tree2. */
	fun joinAllBuckets(callback: JoinCallback<T, R>) {
		for (z1 in tree1.minZoom..tree1.maxZoom) {
			val b1 = tree1.getZoomBucket(z1)
			if (b1 == null || b1.len == 0) {
				continue
			}
			for (z2 in tree2.minZoom..tree2.maxZoom) {
				val b2 = tree2.getZoomBucket(z2)
				if (b2 == null || b2.len == 0) {
					continue
				}
				joinBuckets(z1, z2, callback)
			}
		}
	}

	/** Joins two specific zoom buckets z1 and z2 using tree skipping iterators. */
	fun joinBuckets(z1: Int, z2: Int, callback: JoinCallback<T, R>) {
		val b1 = tree1.getZoomBucket(z1)
		val b2 = tree2.getZoomBucket(z2)
		if (b1 == null || b2 == null || b1.len == 0 || b2.len == 0) {
			return
		}
		val itA = ZoomBucketIndexTreeIterator(b1)
		val itB = ZoomBucketIndexTreeIterator(b2)
		if (z1 > z2) {
			joinBucketsOrdered(b2, tree2.getTileEntries(), itB, b1, tree1.getTileEntries(), itA) { e2, e1 ->
				callback.onIntersection(e1, e2)
			}
		} else {
			joinBucketsOrdered(b1, tree1.getTileEntries(), itA, b2, tree2.getTileEntries(), itB, callback)
		}
	}

	private fun <A, B> joinBucketsOrdered(
		bA: ZoomBucket, entriesA: List<TileEntry<A>>, itA: ZoomBucketIndexTreeIterator,
		bB: ZoomBucket, entriesB: List<TileEntry<B>>, itB: ZoomBucketIndexTreeIterator,
		callback: JoinCallback<A, B>
	) {
		val zDiff = bB.z - bA.z
		val shiftBits = abs(zDiff) * 2

		var iA = bA.start
		val endA = bA.start + bA.len
		var iB = bB.start
		val endB = bB.start + bB.len

		while (iA < endA && iB < endB) {
			val eA = entriesA[iA]
			val eB = entriesB[iB]
			val codeA = eA.tileId
			val codeB = eB.tileId

			val normA = if (zDiff < 0) codeA shr shiftBits else codeA
			val normB = if (zDiff > 0) codeB shr shiftBits else codeB

			if (normA == normB) {
				var endMatchB = iB
				while (endMatchB < endB) {
					val cB = entriesB[endMatchB].tileId
					val nB = if (zDiff > 0) cB shr shiftBits else cB
					if (nB != normA) break
					endMatchB++
				}

				var currA = iA
				while (currA < endA) {
					val entryA = entriesA[currA]
					val cA = entryA.tileId
					val nA = if (zDiff < 0) cA shr shiftBits else cA
					if (nA != normA) break

					for (mB in iB until endMatchB) {
						val entryB = entriesB[mB]
						if (HashSkipTileQuadTree.intersectsBBox(entryA.bbox31, entryB.bbox31)) {
							val px = max(entryA.bbox31[0], entryB.bbox31[0])
							val py = max(entryA.bbox31[1], entryB.bbox31[1])
							val intersectAId = HashSkipTileQuadTree.encodeTileId(px shr (31 - entryA.z), py shr (31 - entryA.z))
							val intersectBId = HashSkipTileQuadTree.encodeTileId(px shr (31 - entryB.z), py shr (31 - entryB.z))
							if (intersectAId == entryA.tileId && intersectBId == entryB.tileId) {
								callback.onIntersection(entryA, entryB)
							}
						}
					}
					currA++
				}

				iA = currA
				iB = endMatchB
			} else if (normA < normB) {
				itA.sync(iA)
				val targetA = if (zDiff < 0) normB shl shiftBits else normB
				val nextA = itA.skipToTileId(iA, targetA)
				iA = if (nextA > iA && nextA < endA) nextA else iA + 1
			} else {
				itB.sync(iB)
				val targetB = if (zDiff > 0) normA shl shiftBits else normA
				val nextB = itB.skipToTileId(iB, targetB)
				iB = if (nextB > iB && nextB < endB) nextB else iB + 1
			}
		}
	}
}
