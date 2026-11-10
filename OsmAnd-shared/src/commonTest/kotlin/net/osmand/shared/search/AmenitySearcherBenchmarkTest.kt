package net.osmand.shared.search

import net.osmand.shared.binary.BinaryAmenityIndexRepository
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.BaseDetailsObjectTest
import net.osmand.shared.data.RenderedObject
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import kotlin.math.round
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long [AmenitySearcher] takes for the amenities `AmenitySearcherCompatTest` searched around:
 * to find the amenities around each, to make the object it stands for from itself, from its name
 * and from an object the renderer could have drawn, to search by the first letters of its name,
 * and to merge the amenities of each file. The files and the amenities come from its dump.
 * `AmenitySearcherBenchmarkTest` in OsmAnd-java measures java on the same.
 *
 * Not part of a normal run: it only does anything when `OSMAND_AMENITY_SEARCHER_BENCHMARK` is set.
 */
class AmenitySearcherBenchmarkTest {

	@Test
	fun benchmarkAmenitySearcher() {
		if (testEnvironment("OSMAND_AMENITY_SEARCHER_BENCHMARK") == null) {
			println("AmenitySearcherBenchmarkTest: set OSMAND_AMENITY_SEARCHER_BENCHMARK to run")
			return
		}
		val source = openJavaDump(
			"AmenitySearcherBenchmarkTest", "OSMAND_AMENITY_SEARCHER_JAVA_DUMP", "amenity-searcher-java.txt", "AmenitySearcherCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		val defaultTypes = MapPoiTypes.getDefault()
		val phrases = HashMap<String, String>()
		val readers = ArrayList<BinaryMapIndexReader>()
		val boxes = ArrayList<IntArray>()
		val samples = ArrayList<Amenity>()
		val drawn = ArrayList<RenderedObject>()
		val prefixes = ArrayList<Pair<Amenity, String>>()
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		val searcher = AmenitySearcher(types)
		val settings = AmenitySearcher.Settings({ "en" }, { false }, null)
		val amenities = ArrayList<List<Amenity>>()
		val best = DoubleArray(KINDS.size) { Double.MAX_VALUE }
		val counts = LongArray(KINDS.size)
		try {
			MapPoiTypes.setDefault(types)
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				when (f[0]) {
					"X" -> phrases[dumpUnhex(f[1])] = dumpUnhex(f[2])
					"F" -> {
						val path = AmenitySearcherTest.path(dumpUnhex(f[2]))
						val reader = BinaryMapIndexReader(path)
						readers.add(reader)
						searcher.addAmenityRepository(path.substringAfterLast('/'), BinaryAmenityIndexRepository(path) { reader })
						val box = if (f[3] == "-") intArrayOf(0, Int.MAX_VALUE, 0, Int.MAX_VALUE) else f[3].split(",").map { it.toInt() }.toIntArray()
						boxes.add(box)
						amenities.add(reader.searchPoi(SearchRequest.buildSearchPoiRequest(box[0], box[1], box[2], box[3], -1, null, null, null)))
					}
					"D" -> samples.add(amenities[f[1].toInt()][f[2].toInt()])
					"R" -> drawn.add(BaseDetailsObjectTest.drawn(f[3]))
					"Q" -> prefixes.add(amenities[f[1].toInt()][f[2].toInt()] to dumpUnhex(f[3]))
				}
			}
			source.close()
			types.setPoiTranslator(SearchCoreFactoryTest.Translator(phrases))
			repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
				val nanos = LongArray(KINDS.size)
				val made = LongArray(KINDS.size)
				var mark = TimeSource.Monotonic.markNow()
				for (a in samples) {
					made[0] += searcher.searchAmenities(a.getLocation()!!, settings).size
				}
				nanos[0] = mark.elapsedNow().inWholeNanoseconds
				mark = TimeSource.Monotonic.markNow()
				for (a in samples) {
					made[1] += searcher.searchDetailedObject(AmenitySearcher.Request(a), settings, null)?.getObjects()?.size ?: 0
				}
				nanos[1] = mark.elapsedNow().inWholeNanoseconds
				mark = TimeSource.Monotonic.markNow()
				for (a in samples) {
					made[2] += searcher.searchDetailedObject(AmenitySearcherTest.byNames(a, 0), settings)?.getObjects()?.size ?: 0
				}
				nanos[2] = mark.elapsedNow().inWholeNanoseconds
				mark = TimeSource.Monotonic.markNow()
				for (d in drawn) {
					made[3] += searcher.searchDetailedObject(AmenitySearcher.Request(d), settings, null)?.getObjects()?.size ?: 0
				}
				nanos[3] = mark.elapsedNow().inWholeNanoseconds
				mark = TimeSource.Monotonic.markNow()
				for ((a, prefix) in prefixes) {
					val l = a.getLocation()!!
					val r = KMapUtils.calculateLatLonBbox(l.latitude, l.longitude, 5000)
					made[4] += searcher.searchAmenitiesByName(prefix, r.top, r.left, r.bottom, r.right, l.latitude, l.longitude, null).size
				}
				nanos[4] = mark.elapsedNow().inWholeNanoseconds
				// merging changes the amenities it merges into: fresh ones, read before the clock starts
				val fresh = readers.indices.map { f ->
					val b = boxes[f]
					readers[f].searchPoi(SearchRequest.buildSearchPoiRequest(b[0], b[1], b[2], b[3], -1, null, null, null))
				}
				mark = TimeSource.Monotonic.markNow()
				for (list in fresh) {
					made[5] += searcher.mergeAmenities(list, settings)?.size ?: 0
				}
				nanos[5] = mark.elapsedNow().inWholeNanoseconds
				if (round >= WARMUP_ROUNDS) {
					for (k in KINDS.indices) {
						best[k] = minOf(best[k], nanos[k] / 1e6)
						counts[k] = made[k]
					}
				}
			}
		} finally {
			readers.forEach { it.close() }
			MapPoiTypes.setDefault(defaultTypes)
		}
		println("")
		println("### amenity searcher, shared copy on ${testPlatformName()}, ${readers.size} files, ${samples.size} amenities, best of $MEASURED_ROUNDS")
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
		private val KINDS = listOf(
			"amenities around a place", "object of an amenity", "object of a name", "object of a drawn object",
			"amenities by name", "amenities of a file merged"
		)
	}
}
