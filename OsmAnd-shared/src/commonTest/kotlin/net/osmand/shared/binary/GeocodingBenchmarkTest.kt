package net.osmand.shared.binary

import net.osmand.shared.binary.GeocodingUtilities.GeocodingResult
import net.osmand.shared.routing.RoutingContext
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.AmenitySearcherTest
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import kotlin.math.round
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long [GeocodingUtilities] takes at the points `GeocodingCompatTest` geocoded, in the default
 * context of each file: to find the roads near each point, and to find the addresses on them and sort
 * them, as the search tests do. The files and the points come from its dump.
 * `GeocodingBenchmarkTest` in OsmAnd-java measures java on the same.
 *
 * Not part of a normal run: it only does anything when `OSMAND_GEOCODING_BENCHMARK` is set.
 */
class GeocodingBenchmarkTest {

	@Test
	fun benchmarkGeocoding() {
		if (testEnvironment("OSMAND_GEOCODING_BENCHMARK") == null) {
			println("GeocodingBenchmarkTest: set OSMAND_GEOCODING_BENCHMARK to run")
			return
		}
		val source = openJavaDump("GeocodingBenchmarkTest", "OSMAND_GEOCODING_JAVA_DUMP", "geocoding-java.txt", "GeocodingCompatTest")
			?: return
		SearchPhraseTest.setUp
		val readers = ArrayList<BinaryMapIndexReader>()
		val contexts = ArrayList<RoutingContext>()
		val fileOf = ArrayList<Int>()
		val points = ArrayList<DoubleArray>()
		val best = DoubleArray(KINDS.size) { Double.MAX_VALUE }
		val counts = LongArray(KINDS.size)
		val utils = GeocodingUtilities()
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				if (line.startsWith("F\t")) {
					val reader = BinaryMapIndexReader(AmenitySearcherTest.path(dumpUnhex(line.split("\t")[2])))
					readers.add(reader)
					contexts.add(GeocodingUtilitiesTest.defaultContext(reader))
				} else if (line.startsWith("P\t")) {
					val f = line.split("\t", limit = 6)
					if (f[2] == "0") {
						fileOf.add(readers.size - 1)
						points.add(doubleArrayOf(Double.fromBits(f[3].toULong(16).toLong()), Double.fromBits(f[4].toULong(16).toLong())))
					}
				}
			}
			source.close()
			repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
				val nanos = LongArray(KINDS.size)
				val made = LongArray(KINDS.size)
				var mark = TimeSource.Monotonic.markNow()
				for (i in points.indices) {
					made[0] += utils.reverseGeocodingSearch(contexts[fileOf[i]], points[i][0], points[i][1], false).size
				}
				nanos[0] = mark.elapsedNow().inWholeNanoseconds
				// sorting changes the results it sorts: fresh ones, found before the clock starts
				val found: List<List<GeocodingResult>> = points.indices.map {
					utils.reverseGeocodingSearch(contexts[fileOf[it]], points[it][0], points[it][1], false)
				}
				mark = TimeSource.Monotonic.markNow()
				for (i in points.indices) {
					made[1] += utils.sortGeocodingResults(listOf(readers[fileOf[i]]), found[i]).size
				}
				nanos[1] = mark.elapsedNow().inWholeNanoseconds
				if (round >= WARMUP_ROUNDS) {
					for (k in KINDS.indices) {
						best[k] = minOf(best[k], nanos[k] / 1e6)
						counts[k] = made[k]
					}
				}
			}
		} finally {
			GeocodingUtilitiesTest.close(contexts)
			readers.forEach { it.close() }
		}
		println("")
		println("### geocoding, shared copy on ${testPlatformName()}, ${readers.size} files, ${points.size} points, best of $MEASURED_ROUNDS")
		for (k in KINDS.indices) {
			row(KINDS[k], best[k], counts[k])
		}
		row("all", best.sum(), 0)
	}

	private fun row(what: String, ms: Double, count: Long) {
		val rounded = (round(ms * 10) / 10).toString()
		println("  ${what.padEnd(40)} ${rounded.padStart(9)} ${count.toString().padStart(9)}")
	}

	companion object {
		const val WARMUP_ROUNDS = 2
		const val MEASURED_ROUNDS = 5
		private val KINDS = listOf("roads near a point", "addresses of the roads sorted")
	}
}
