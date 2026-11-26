package net.osmand.shared.search.core

import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.dumpFnv
import net.osmand.shared.util.readJavaDump
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [HashSkipTileQuadTree], [HashSkipTileQuadTreeJoiner] and [HashQuadTree] wherever they run, on
 * Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `QuadTreeCompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/quad-tree-java.txt`, or wherever `OSMAND_QUAD_TREE_JAVA_DUMP` points: the
 * boxes of the regions and the random boxes it fed the trees, and a hash of each of its answers. It
 * asks the copy the same and holds it to java exactly. [boxesFoundAreTheBoxesThatIntersect] needs
 * no java: the boxes a tree finds, also once rebuilt with objects moved and added, and the pairs the
 * joiner calls back are those that intersect.
 */
class HashSkipTileQuadTreeTest {

	@Test
	fun sameAsJava() {
		val lines = readJavaDump(
			"HashSkipTileQuadTreeTest", "OSMAND_QUAD_TREE_JAVA_DUMP", "quad-tree-java.txt", "QuadTreeCompatTest"
		) ?: return
		val boxes = ArrayList<IntArray>()
		val queries = ArrayList<IntArray>()
		val added = ArrayList<IntArray>()
		val java = LinkedHashMap<String, String>()
		for (line in lines) {
			val f = line.split("\t")
			when (f[0]) {
				"B", "Q", "A" -> {
					val b = intArrayOf(f[1].toInt(), f[2].toInt(), f[3].toInt(), f[4].toInt())
					when (f[0]) {
						"B" -> boxes.add(b)
						"Q" -> queries.add(b)
						else -> added.add(b)
					}
				}
				"E" -> java[f[1]] = f[2] + "\t" + f[3]
			}
		}
		val copy = LinkedHashMap<String, String>()
		fun same(name: String, answer: String) {
			copy[name] = answer.length.toString() + "\t" + dumpFnv(answer)
		}
		tileIds(boxes, queries, ::same)
		val default = HashSkipTileQuadTree<Int>()
		boxes.forEachIndexed { i, b -> default.addObject(i, b) }
		default.build()
		same("tree default entries", entries(default.getTileEntries(), true))
		same("tree default buckets", buckets(default))
		trees(boxes, queries, added, ::same)
		joins(boxes, added, ::same)
		hashQuadTree(boxes, ::same)
		val different = java.keys.filter { java[it] != copy[it] }
		if (different.isNotEmpty() || java.keys != copy.keys) {
			fail("different from java: $different; java has ${java.keys - copy.keys}, the copy ${copy.keys - java.keys}")
		}
		assertTrue(java.size > 20, "answers compared: ${java.size}")
		println("HashSkipTileQuadTreeTest: ${java.size} answers the same as java over ${boxes.size} boxes")
	}

	@Test
	fun boxesFoundAreTheBoxesThatIntersect() {
		val random = Random(21102026)
		val boxes = List(3000) { randomBox(random) }
		val queries = List(500) { randomBox(random) }
		for (settings in SETTINGS) {
			val tree = HashSkipTileQuadTree<Int>(settings.first[0], settings.first[1], settings.second)
			val current = HashMap<Long, IntArray>()
			boxes.forEachIndexed { i, b ->
				tree.addObject(i, b, i.toLong())
				current[i.toLong()] = b
			}
			for (pass in 0 until 2) {
				if (pass == 1) {
					// rebuilt: every 5th object moved to another box, and new objects
					for (i in boxes.indices step 5) {
						val b = randomBox(random)
						tree.addObject(i, b, i.toLong())
						current[i.toLong()] = b
					}
					for (i in 0 until 300) {
						val b = randomBox(random)
						tree.addObject(i, b, 10_000L + i)
						current[10_000L + i] = b
					}
				}
				tree.build()
				for (q in queries) {
					val found = tree.get(q).map { it.objId }.toSet()
					val expected = current.filter { HashSkipTileQuadTree.intersectsBBox(it.value, q) }.keys
					assertEquals(expected, found, "pass $pass found ${q.toList()}")
					assertEquals(expected.isNotEmpty(), tree.contains(q), "pass $pass contains ${q.toList()}")
				}
			}
		}
		for (s1 in SETTINGS.indices) {
			for (s2 in SETTINGS.indices) {
				val t1 = HashSkipTileQuadTree<Int>(SETTINGS[s1].first[0], SETTINGS[s1].first[1], SETTINGS[s1].second)
				val t2 = HashSkipTileQuadTree<Int>(SETTINGS[s2].first[0], SETTINGS[s2].first[1], SETTINGS[s2].second)
				boxes.forEachIndexed { i, b -> t1.addObject(i, b, i.toLong()) }
				queries.forEachIndexed { i, b -> t2.addObject(i, b, i.toLong()) }
				t1.build()
				t2.build()
				val pairs = ArrayList<Pair<Long, Long>>()
				HashSkipTileQuadTreeJoiner(t1, t2).joinAllBuckets { e1, e2 -> pairs.add(e1.objId to e2.objId) }
				val expected = HashSet<Pair<Long, Long>>()
				for (i in boxes.indices) {
					for (j in queries.indices) {
						if (HashSkipTileQuadTree.intersectsBBox(boxes[i], queries[j])) {
							expected.add(i.toLong() to j.toLong())
						}
					}
				}
				assertEquals(pairs.size, pairs.toSet().size, "pairs once, settings $s1 x $s2")
				assertEquals(expected, pairs.toSet(), "pairs, settings $s1 x $s2")
			}
		}
	}

	companion object {

		/** `QuadTreeCompatTest.SETTINGS`: minZoom, maxZoom and the indexed zooms. */
		private val SETTINGS = listOf(
			intArrayOf(HashSkipTileQuadTree.DEFALT_MIN_ZOOM, HashSkipTileQuadTree.DEFALT_MAX_ZOOM) to HashSkipTileQuadTree.DEFAULT_INDEXED_ZOOMS,
			intArrayOf(2, 14) to intArrayOf(),
			intArrayOf(0, 18) to intArrayOf(2, 4, 6, 8, 10, 12, 14)
		)

		private fun randomBox(random: Random): IntArray {
			val size = when (random.nextInt(5)) {
				0 -> 0
				1 -> random.nextInt(1 shl 14)
				2 -> random.nextInt(1 shl 20)
				3 -> random.nextInt(1 shl 26)
				else -> random.nextInt(1 shl 29)
			}
			val x = random.nextInt(Int.MAX_VALUE - size)
			val y = random.nextInt(Int.MAX_VALUE - size)
			return intArrayOf(x, y, x + size, y + random.nextInt(maxOf(1, size)))
		}

		/** `QuadTreeCompatTest.javaTree`. */
		private fun tree(settings: Pair<IntArray, IntArray>, list: List<IntArray>): HashSkipTileQuadTree<Int> {
			val t = HashSkipTileQuadTree<Int>(settings.first[0], settings.first[1], settings.second)
			for (i in list.indices) {
				if (i % 2 == 0) {
					t.addObject(i, list[i])
				} else {
					t.addObject(i, list[i], 1_000_000L + i)
				}
			}
			return t
		}

		/** `QuadTreeCompatTest.addMore`. */
		private fun addMore(t: HashSkipTileQuadTree<Int>, added: List<IntArray>, small: Boolean) {
			for (i in added.indices) {
				val b = added[i]
				if (small && (b[2].toLong() - b[0] >= (1 shl 26) || b[3].toLong() - b[1] >= (1 shl 26))) {
					continue
				}
				val id = if (i % 7 == 0) 1_000_000L + 2 * i + 1 else 2_000_000L + i
				t.addObject(-i, b, id)
			}
		}

		private fun entries(list: List<HashSkipTileQuadTree.TileEntry<Int>>, skips: Boolean): String {
			val sb = StringBuilder()
			for (e in list) {
				sb.append(e.objId).append(':').append(e.obj).append(':').append(e.z).append(':').append(e.tileId)
				if (skips) {
					sb.append(':').append(e.skipNextTileId)
				}
				sb.append(';')
			}
			return sb.toString()
		}

		/** `QuadTreeCompatTest.buckets`. */
		private fun buckets(t: HashSkipTileQuadTree<Int>): String {
			val sb = StringBuilder()
			for (b in t.zoomBuckets!!) {
				if (b == null) {
					sb.append("-;")
					continue
				}
				sb.append(b.z).append(':').append(b.start).append(':').append(b.len).append(':').append(b.indexedZooms.contentToString())
				var i = b.indx
				while (i != null) {
					sb.append('/').append(i.indxZoom).append(':').append(i.skipIndexes.toArray().contentToString()).append(':')
						.append(i.skipInSubIndexes.toArray().contentToString()).append(':').append(i.tileIds.toArray().contentToString())
					i = i.subTree
				}
				sb.append(';')
			}
			return sb.toString()
		}

		/** `QuadTreeCompatTest.sorted`. */
		private fun sorted(described: String): String {
			val items = described.split(";").toMutableList()
			items.remove("")
			return items.sorted().joinToString(";")
		}

		private fun outcome(call: () -> Any): String =
			try {
				call().toString()
			} catch (e: UnsupportedOperationException) {
				"unsupported"
			}

		/** `QuadTreeCompatTest.tileIdsAreTheSame`. */
		private fun tileIds(boxes: List<IntArray>, queries: List<IntArray>, same: (String, String) -> Unit) {
			val sb = StringBuilder()
			for (b in boxes) {
				val x = (b[0].toLong() shl 32) xor b[2].toLong()
				val y = (b[1].toLong() shl 32) xor b[3].toLong()
				val z = abs((b[0] xor b[1]) % 22)
				val x31 = b[0]
				val y31 = b[1]
				sb.append(KMapUtils.interleaveBits(x, y)).append(':').append(KMapUtils.interleaveBits(x.inv(), y.inv())).append(':')
					.append(KMapUtils.deinterleaveX(x)).append(':').append(KMapUtils.deinterleaveY(x)).append(':')
					.append(KMapUtils.deinterleaveX(x.inv())).append(':').append(KMapUtils.deinterleaveY(x.inv())).append(':')
				sb.append(outcome { HashQuadTree.encodeTileId31(z, x31, y31) }).append(':')
					.append(outcome { HashQuadTree.encodeTileId(z, x31 shr 15, y31 shr 15) }).append(':')
					.append(HashSkipTileQuadTree.encodeTileId(x31 shr 10, y31 shr 10)).append(';')
			}
			same("tile ids", sb.toString())
			sb.setLength(0)
			for (i in 0 until queries.size - 1) {
				sb.append(HashSkipTileQuadTree.intersectsBBox(queries[i], queries[i + 1]))
			}
			same("intersectsBBox", sb.toString())
		}

		/** `QuadTreeCompatTest.treesAreTheSame`. */
		private fun trees(boxes: List<IntArray>, queries: List<IntArray>, added: List<IntArray>, same: (String, String) -> Unit) {
			for (s in SETTINGS.indices) {
				val t = tree(SETTINGS[s], boxes)
				val zooms = StringBuilder()
				for (b in boxes) {
					zooms.append(t.getNativeZoom(b)).append(',')
				}
				same("tree $s native zooms", zooms.toString())
				t.build()
				same("tree $s entries", entries(t.getTileEntries(), true))
				same("tree $s buckets", buckets(t))
				same("tree $s queries", queries(t, queries, false))
				addMore(t, added, false)
				t.build()
				same("tree $s entries after more", sorted(entries(t.getTileEntries(), false)))
				same("tree $s buckets after more", buckets(t))
				same("tree $s after more queries", queries(t, queries, true))
			}
		}

		/** `QuadTreeCompatTest.sameQueries`: a hash of each answer. */
		private fun queries(t: HashSkipTileQuadTree<Int>, queries: List<IntArray>, anyOrder: Boolean): String {
			val hashes = StringBuilder()
			for (i in queries.indices) {
				val q = queries[i]
				val r = entries(if (i % 5 == 4) t.get(q, 3, 12) else t.get(q), false)
				val answer = (if (anyOrder) sorted(r) else r) + "|" + t.contains(q)
				hashes.append(dumpFnv(answer)).append(';')
			}
			return hashes.toString()
		}

		/** `QuadTreeCompatTest.joinsAreTheSame`. */
		private fun joins(boxes: List<IntArray>, added: List<IntArray>, same: (String, String) -> Unit) {
			val first = boxes.filter { it[2].toLong() - it[0] < (1 shl 26) && it[3].toLong() - it[1] < (1 shl 26) }
			val second = first.filterIndexed { i, _ -> i % 3 == 0 }
			for (p in listOf(0 to 0, 0 to 1, 1 to 2, 2 to 0)) {
				val t1 = tree(SETTINGS[p.first], first)
				val t2 = tree(SETTINGS[p.second], second)
				for (pass in 0 until 2) {
					if (pass == 1) {
						addMore(t1, added, true)
						addMore(t2, added, true)
					}
					t1.build()
					t2.build()
					val sb = StringBuilder()
					HashSkipTileQuadTreeJoiner(t1, t2).joinAllBuckets { e1, e2 ->
						sb.append(e1.objId).append(':').append(e1.z).append(':').append(e1.tileId).append('-')
							.append(e2.objId).append(':').append(e2.z).append(':').append(e2.tileId).append(';')
					}
					val name = "join ${p.first} x ${p.second}" + if (pass == 1) " after more" else ""
					same(name, if (pass == 1) sorted(sb.toString()) else sb.toString())
				}
			}
		}

		/** `QuadTreeCompatTest.hashQuadTreesAreTheSame`. */
		private fun hashQuadTree(boxes: List<IntArray>, same: (String, String) -> Unit) {
			val t = HashQuadTree<Int>(16)
			val sb = StringBuilder()
			for (i in 0 until 5 * boxes.size) {
				val b = boxes[i % boxes.size]
				val z = (b[0] % 17 + i % 17) % 17
				val mask = (1 shl min(z, 3)) - 1
				val tileId = HashQuadTree.encodeTileId(z, (b[1] shr 3) and mask, (b[2] shr 5) and mask)
				val value = (b[3] shr 7) % 50
				when ((i % 4 * 7 + b[0] % 4) % 4) {
					0 -> t.put(z, tileId, value)
					1 -> t.delete(z, tileId, value)
					2 -> t.forEachMatch(z, tileId) { sb.append(it).append(';') }
					else -> t.forEachMatchHigherZoom(z, tileId) { sb.append(it).append(';') }
				}
				sb.append('|')
			}
			same("hash quad tree", sb.toString())
		}
	}
}
