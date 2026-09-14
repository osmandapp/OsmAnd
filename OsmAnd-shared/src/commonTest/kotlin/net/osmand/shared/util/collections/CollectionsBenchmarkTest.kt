package net.osmand.shared.util.collections

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Head to head benchmark of the primitive collections against the boxing stdlib equivalents they
 * replace. Runs on every target, so the same workload can be compared between JVM, Android and
 * Kotlin/Native.
 *
 * **Disabled on purpose.** Measuring takes seconds and the timings are noise in a normal test run,
 * so this class is not part of the suite. It is a tool you reach for when you change a collection
 * or want platform numbers, not a regression gate — correctness is covered by [KTLongObjectMapTest],
 * [KTLongHashSetTest], [KTIntArrayListTest] and [KBitSetTest], which do run.
 *
 * To measure, remove the `@Ignore` below and put it back afterwards.
 *
 * On the JVM:
 * ```
 * ./gradlew :OsmAnd-shared:jvmTest --tests "*CollectionsBenchmarkTest"
 * ```
 *
 * On Kotlin/Native you must use the **release** binary. The default `iosSimulatorArm64Test` task
 * builds `debugTest`, which is linked without LLVM optimisations and reports numbers 5 to 13 times
 * worse than a real build:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * xcrun simctl spawn --standalone <simulator-udid> \
 *   OsmAnd-shared/build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.util.collections.CollectionsBenchmarkTest.*'
 * ```
 *
 * The comparison against the real `gnu.trove` collections lives in
 * `OsmAnd-java/src/test/java/net/osmand/shared/util/collections/CollectionsTroveBenchmarkTest.java`,
 * because that is the only module where trove and OsmAnd-shared share a classpath.
 *
 * Nothing here asserts a timing. The assertions only check that both implementations computed the
 * same answer, so a slow machine can never turn the build red.
 */
@Ignore
class CollectionsBenchmarkTest {

	@Test
	fun benchmarkLongObjectMap() {
		val report = BenchmarkReport("long -> object map, ${ENTRIES} entries, ${LOOKUPS} lookups")

		for (distribution in KeyDistribution.entries) {
			val keys = distribution.keys(ENTRIES)
			val probes = probeKeys(keys, LOOKUPS)
			val value = Any()

			var ktChecksum = 0L
			val ktTime = measure {
				val map = KTLongObjectMap<Any>(ENTRIES)
				for (key in keys) {
					map.put(key, value)
				}
				var hits = 0L
				for (probe in probes) {
					if (map[probe] != null) {
						hits++
					}
				}
				var iterated = 0L
				map.forEach { key, _ -> iterated += key }
				ktChecksum = hits * 31 + iterated
			}

			var refChecksum = 0L
			val refTime = measure {
				val map = HashMap<Long, Any>(ENTRIES * 2)
				for (key in keys) {
					map[key] = value
				}
				var hits = 0L
				for (probe in probes) {
					if (map[probe] != null) {
						hits++
					}
				}
				var iterated = 0L
				for (key in map.keys) {
					iterated += key
				}
				refChecksum = hits * 31 + iterated
			}

			assertEquals(refChecksum, ktChecksum, "checksum mismatch for $distribution")
			report.add(distribution.label, "KTLongObjectMap", ktTime, "HashMap<Long,V>", refTime)
		}

		report.print()
	}

	@Test
	fun benchmarkLongHashSet() {
		val report = BenchmarkReport("long set, ${ENTRIES} entries, ${LOOKUPS} lookups")

		for (distribution in KeyDistribution.entries) {
			val keys = distribution.keys(ENTRIES)
			val probes = probeKeys(keys, LOOKUPS)

			var ktHits = 0L
			val ktTime = measure {
				val set = KTLongHashSet(ENTRIES)
				for (key in keys) {
					set.add(key)
				}
				var hits = 0L
				for (probe in probes) {
					if (probe in set) {
						hits++
					}
				}
				ktHits = hits
			}

			var refHits = 0L
			val refTime = measure {
				val set = HashSet<Long>(ENTRIES * 2)
				for (key in keys) {
					set.add(key)
				}
				var hits = 0L
				for (probe in probes) {
					if (probe in set) {
						hits++
					}
				}
				refHits = hits
			}

			assertEquals(refHits, ktHits, "checksum mismatch for $distribution")
			report.add(distribution.label, "KTLongHashSet", ktTime, "HashSet<Long>", refTime)
		}

		report.print()
	}

	@Test
	fun benchmarkIntArrayList() {
		val report = BenchmarkReport("int list, ${LIST_ENTRIES} appends plus a full scan")

		var ktSum = 0L
		val ktTime = measure {
			val list = KTIntArrayList()
			for (i in 0 until LIST_ENTRIES) {
				list.add(i)
			}
			var sum = 0L
			for (i in 0 until list.size) {
				sum += list.getQuick(i)
			}
			ktSum = sum
		}

		var refSum = 0L
		val refTime = measure {
			val list = ArrayList<Int>()
			for (i in 0 until LIST_ENTRIES) {
				list.add(i)
			}
			var sum = 0L
			for (i in 0 until list.size) {
				sum += list[i]
			}
			refSum = sum
		}

		assertEquals(refSum, ktSum)
		report.add("sequential", "KTIntArrayList", ktTime, "ArrayList<Int>", refTime)
		report.print()
	}

	@Test
	fun benchmarkBitSet() {
		// shaped like GeneralRouter rule evaluation: build a tag mask, intersect it with a road's
		// types, read the first matching bit
		val report = BenchmarkReport("bit mask evaluation, ${RULE_EVALUATIONS} rule evaluations")
		val universe = 512
		val random = XorShiftRandom(20260907L)

		val masks = Array(64) { KBitSet(universe) }
		val modelMasks = Array(64) { HashSet<Int>() }
		for (i in masks.indices) {
			repeat(8) {
				val bit = random.nextInt(universe)
				masks[i].set(bit)
				modelMasks[i].add(bit)
			}
		}
		val roadTypes = KBitSet(universe)
		val modelRoadTypes = HashSet<Int>()
		repeat(24) {
			val bit = random.nextInt(universe)
			roadTypes.set(bit)
			modelRoadTypes.add(bit)
		}

		var ktChecksum = 0L
		val ktTime = measure {
			var checksum = 0L
			val scratch = KBitSet(universe)
			for (i in 0 until RULE_EVALUATIONS) {
				val mask = masks[i and 63]
				if (mask.intersects(roadTypes)) {
					scratch.clear()
					scratch.or(mask)
					scratch.and(roadTypes)
					checksum += scratch.nextSetBit(0)
				}
			}
			ktChecksum = checksum
		}

		var refChecksum = 0L
		val refTime = measure {
			var checksum = 0L
			for (i in 0 until RULE_EVALUATIONS) {
				val mask = modelMasks[i and 63]
				val intersection = mask.intersect(modelRoadTypes)
				if (intersection.isNotEmpty()) {
					checksum += intersection.min()
				}
			}
			refChecksum = checksum
		}

		assertEquals(refChecksum, ktChecksum)
		assertTrue(ktChecksum != 0L, "the workload must actually do something")
		report.add("GeneralRouter shape", "KBitSet", ktTime, "HashSet<Int>", refTime)
		report.print()
	}

	// ------------------------------------------------------------------ harness

	private enum class KeyDistribution(val label: String) {

		/** `roadId << 6 | segment`, exactly what routing uses as a map key. */
		ROUTING("routing ids"),

		/** Uniformly spread keys, the friendly case for any hash function. */
		RANDOM("random     ");

		fun keys(count: Int): LongArray {
			val random = XorShiftRandom(20260907L)
			return when (this) {
				ROUTING -> LongArray(count) { i ->
					// road ids grow in blocks, segments are small: highly structured low bits
					((100_000_000L + i * 17L) shl 6) or (i % 48).toLong()
				}
				RANDOM -> LongArray(count) { random.nextLong() }
			}
		}
	}

	/** Half of the probes hit an existing key, half miss. */
	private fun probeKeys(keys: LongArray, count: Int): LongArray {
		val random = XorShiftRandom(1234L)
		return LongArray(count) { i ->
			if (i and 1 == 0) keys[random.nextInt(keys.size)] else (random.nextLong() or (1L shl 62))
		}
	}

	private fun measure(block: () -> Unit): Double {
		repeat(WARMUP_ROUNDS) { block() }
		var best = Double.MAX_VALUE
		repeat(MEASURED_ROUNDS) {
			val mark = TimeSource.Monotonic.markNow()
			block()
			val elapsed = mark.elapsedNow().inWholeMicroseconds / 1000.0
			if (elapsed < best) {
				best = elapsed
			}
		}
		return best
	}

	private class BenchmarkReport(private val title: String) {

		private val rows = ArrayList<String>()

		fun add(case: String, name: String, time: Double, refName: String, refTime: Double) {
			val ratio = if (time > 0) refTime / time else 0.0
			rows.add(
				"  ${case.padEnd(22)} ${name.padEnd(16)} ${format(time)} ms" +
						"   vs ${refName.padEnd(16)} ${format(refTime)} ms" +
						"   speedup ${format(ratio)}x"
			)
		}

		fun print() {
			println("")
			println("### $title")
			println("    best of $MEASURED_ROUNDS rounds after $WARMUP_ROUNDS warmup rounds")
			rows.forEach { println(it) }
			println("")
		}

		private fun format(value: Double): String {
			val scaled = (value * 100).toLong()
			return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}".padStart(9)
		}
	}

	companion object {
		/** Kept modest so the suite stays usable on a phone and on a simulator. */
		private const val ENTRIES = 200_000
		private const val LOOKUPS = 400_000
		private const val LIST_ENTRIES = 2_000_000
		private const val RULE_EVALUATIONS = 200_000
		private const val WARMUP_ROUNDS = 2
		private const val MEASURED_ROUNDS = 3
	}
}
