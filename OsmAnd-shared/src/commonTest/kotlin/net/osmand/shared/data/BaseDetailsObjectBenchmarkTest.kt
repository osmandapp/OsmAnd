package net.osmand.shared.data

import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import kotlin.math.round
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long it takes to make details objects of the objects `BaseDetailsObjectCompatTest` wrote to its
 * dump: single amenities, amenities with one osm id or wikidata as the search unites them, and drawn
 * objects with their amenity. `BaseDetailsObjectBenchmarkTest` in OsmAnd-java measures java on the same.
 *
 * Not part of a normal run: it only does anything when `OSMAND_DETAILS_OBJECTS_BENCHMARK` is set.
 */
class BaseDetailsObjectBenchmarkTest {

	@Test
	fun benchmarkDetailsObjects() {
		if (testEnvironment("OSMAND_DETAILS_OBJECTS_BENCHMARK") == null) {
			println("BaseDetailsObjectBenchmarkTest: set OSMAND_DETAILS_OBJECTS_BENCHMARK to run")
			return
		}
		val source = openJavaDump(
			"BaseDetailsObjectBenchmarkTest", "OSMAND_DETAILS_OBJECTS_JAVA_DUMP", "details-objects-java.txt",
			"BaseDetailsObjectCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		val defaultTypes = MapPoiTypes.getDefault()
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		MapPoiTypes.setDefault(types)
		val files = BaseDetailsObjectTest.Files()
		val phrases = HashMap<String, String>()
		val singles = ArrayList<Amenity>()
		val groups = ArrayList<List<Amenity>>()
		val drawnAmenities = ArrayList<Amenity>()
		val drawn = ArrayList<RenderedObject>()
		val best = DoubleArray(KINDS.size) { Double.MAX_VALUE }
		var made = 0L
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				when (f[0]) {
					"X" -> phrases[dumpUnhex(f[1])] = dumpUnhex(f[2])
					"S" -> singles.add(files.amenities(f[1])[f[2].toInt()])
					"U" -> groups.add(f[1].split(",").map { m ->
						val (file, i) = m.split(":")
						files.amenities(file)[i.toInt()]
					})
					"R" -> {
						drawnAmenities.add(files.amenities(f[1])[f[2].toInt()])
						drawn.add(BaseDetailsObjectTest.drawn(f[3]))
					}
				}
			}
			types.setPoiTranslator(SearchCoreFactoryTest.Translator(phrases))
			repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
				val nanos = LongArray(KINDS.size)
				made = 0
				var mark = TimeSource.Monotonic.markNow()
				for (a in singles) {
					made += BaseDetailsObject(a, "en").getObjects().size
				}
				nanos[0] = mark.elapsedNow().inWholeNanoseconds
				mark = TimeSource.Monotonic.markNow()
				for (group in groups) {
					val b = BaseDetailsObject(group[0], "en")
					for (i in 1 until group.size) {
						b.addObject(group[i])
					}
					made += b.getObjects().size
				}
				nanos[1] = mark.elapsedNow().inWholeNanoseconds
				mark = TimeSource.Monotonic.markNow()
				for (i in drawn.indices) {
					val d = drawn[i]
					val b = BaseDetailsObject(drawnAmenities[i], "en")
					b.addObject(d)
					made += b.getObjects().size + BaseDetailsObject.convertRenderedObjectToAmenity(d, MapPoiTypes.getDefault())
						.getAdditionalInfoKeys().size
				}
				nanos[2] = mark.elapsedNow().inWholeNanoseconds
				if (round >= WARMUP_ROUNDS) {
					for (k in KINDS.indices) {
						best[k] = minOf(best[k], nanos[k] / 1e6)
					}
				}
			}
		} finally {
			source.close()
			files.close()
			MapPoiTypes.setDefault(defaultTypes)
		}
		println("")
		println("### details objects, shared copy on ${testPlatformName()}, ${singles.size} single, ${groups.size} united, ${drawn.size} drawn, best of $MEASURED_ROUNDS ($made objects)")
		for (k in KINDS.indices) {
			row(KINDS[k], best[k])
		}
		row("all", best.sum())
	}

	private fun row(what: String, ms: Double) {
		println("  ${what.padEnd(24)} ${(round(ms * 10) / 10).toString().padStart(9)}")
	}

	companion object {
		const val WARMUP_ROUNDS = 2
		const val MEASURED_ROUNDS = 5
		private val KINDS = listOf("single amenities", "united amenities", "drawn objects")
	}
}
