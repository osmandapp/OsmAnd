package net.osmand.shared.search.core

import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.util.readJavaDump
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy of the trees of tiles takes wherever it runs, over the boxes `QuadTreeCompatTest` in
 * OsmAnd-java wrote to `../OsmAnd-java/build/quad-tree-java.txt`: a tree of all of them built, 1 500 boxes asked for
 * and whether anything is there, the boxes smaller than a region joined with every third of them, and every box put
 * into a [HashQuadTree] and the queries looked up in it. The java half, `QuadTreeBenchmarkTest` in OsmAnd-java, puts
 * java next to the copy on the jvm over the same boxes.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_QUAD_TREE_BENCHMARK` is set. On Kotlin/Native only
 * the **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_QUAD_TREE_BENCHMARK=1 SIMCTL_CHILD_OSMAND_QUAD_TREE_JAVA_DUMP=<dump> \
 *   xcrun simctl spawn --standalone <udid> build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.search.core.QuadTreeBenchmarkTest.*'
 * ```
 */
class QuadTreeBenchmarkTest {

	@Test
	fun quadTrees() {
		if (testEnvironment("OSMAND_QUAD_TREE_BENCHMARK") == null) {
			println("QuadTreeBenchmarkTest: set OSMAND_QUAD_TREE_BENCHMARK to run")
			return
		}
		val lines = readJavaDump(
			"QuadTreeBenchmarkTest", "OSMAND_QUAD_TREE_JAVA_DUMP", "quad-tree-java.txt", "QuadTreeCompatTest"
		) ?: return
		val boxes = ArrayList<IntArray>()
		val queries = ArrayList<IntArray>()
		for (line in lines) {
			val f = line.split("\t")
			if (f[0] == "B" || f[0] == "Q") {
				val b = intArrayOf(f[1].toInt(), f[2].toInt(), f[3].toInt(), f[4].toInt())
				if (f[0] == "B") boxes.add(b) else queries.add(b)
			}
		}
		val first = boxes.filter { it[2].toLong() - it[0] < (1 shl 26) && it[3].toLong() - it[1] < (1 shl 26) }
		val second = first.filterIndexed { i, _ -> i % 3 == 0 }
		val best = LongArray(5) { Long.MAX_VALUE }
		val found = LongArray(5)
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val t = LongArray(5)
			found.fill(0)
			var mark = TimeSource.Monotonic.markNow()
			val k = HashSkipTileQuadTree<Int>()
			for (i in boxes.indices) {
				k.addObject(i, boxes[i], i.toLong())
			}
			found[0] = k.build().toLong()
			t[0] = mark.elapsedNow().inWholeNanoseconds

			mark = TimeSource.Monotonic.markNow()
			for (q in queries) {
				found[1] += k.get(q).size
			}
			t[1] = mark.elapsedNow().inWholeNanoseconds

			mark = TimeSource.Monotonic.markNow()
			for (q in queries) {
				found[2] += if (k.contains(q)) 1 else 0
			}
			t[2] = mark.elapsedNow().inWholeNanoseconds

			val k1 = HashSkipTileQuadTree<Int>()
			val k2 = HashSkipTileQuadTree<Int>()
			first.forEachIndexed { i, b -> k1.addObject(i, b, i.toLong()) }
			second.forEachIndexed { i, b -> k2.addObject(i, b, i.toLong()) }
			k1.build()
			k2.build()
			var pairs = 0L
			mark = TimeSource.Monotonic.markNow()
			HashSkipTileQuadTreeJoiner(k1, k2).joinAllBuckets { _, _ -> pairs++ }
			t[3] = mark.elapsedNow().inWholeNanoseconds
			found[3] = pairs

			mark = TimeSource.Monotonic.markNow()
			val kh = HashQuadTree<Int>(16)
			for (i in boxes.indices) {
				val z = k.getNativeZoom(boxes[i])
				kh.put(z, HashQuadTree.encodeTileId31(z, boxes[i][0], boxes[i][1]), i)
			}
			for (q in queries) {
				val z = k.getNativeZoom(q)
				var n = 0L
				kh.forEachMatch(z, HashQuadTree.encodeTileId31(z, q[0], q[1])) { n += it.size }
				found[4] += n
			}
			t[4] = mark.elapsedNow().inWholeNanoseconds
			if (round >= WARMUP_ROUNDS) {
				for (n in t.indices) {
					best[n] = minOf(best[n], t[n])
				}
			}
		}
		println("### quad trees on ${testPlatformName()}, best of $MEASURED_ROUNDS in ms")
		val names = listOf(
			"build of ${boxes.size} boxes", "get ${queries.size}", "contains ${queries.size}",
			"join ${first.size} x ${second.size}", "hash quad tree"
		)
		for (n in names.indices) {
			val ms = (best[n] / 10_000).toDouble() / 100
			println(names[n].padEnd(28) + ms.toString().padStart(10) + "   found " + found[n])
		}
	}

	companion object {
		private const val WARMUP_ROUNDS = 3
		private const val MEASURED_ROUNDS = 5
	}
}
