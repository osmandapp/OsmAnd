package net.osmand.shared.binary

import net.osmand.shared.data.Amenity
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy takes to read a map section and a poi section, which is the question
 * the port is asked: whether an obf can be searched on Kotlin/Native without the C++ core.
 *
 * The same corpus and the same phases run through the java reader and through this copy on the jvm
 * in `MapSectionBenchmarkTest` and `PoiSearchBenchmarkTest` in OsmAnd-java, so the columns can be
 * put side by side. The
 * copy is a straight port of java's algorithm, so nothing here defends a design decision; what it
 * answers is the cost of the collections that replaced trove, of the strings java interns and this
 * does not, and of Kotlin/Native itself.
 *
 * The maps are the obf files the tests ship with, found next to the repository. **Not part of a
 * normal run**: it only does anything when `OSMAND_MAP_BENCHMARK` is set, since a Kotlin/Native
 * test binary cannot be told to run an ignored test. On the jvm, from `android`:
 * ```
 * OSMAND_MAP_BENCHMARK=1 ./gradlew --no-daemon :OsmAnd-shared:jvmTest --tests "*MapReaderBenchmarkTest" -i
 * ```
 * On Kotlin/Native only the **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_MAP_BENCHMARK=1 xcrun simctl spawn --standalone <udid> \
 *   build/bin/iosSimulatorArm64/releaseTest/test.kexe --ktest_filter='net.osmand.shared.binary.MapReaderBenchmarkTest.*'
 * ```
 * The native binary does not run in the repository, so it has to be told where the maps are with
 * `SIMCTL_CHILD_OSMAND_OBF_DIRECTORY`.
 *
 * Over the 26 obf files of the tests, reading every object of every zoom level once:
 * ```
 * phase     java (jvm)   copy (jvm)   copy (native)
 * open         4.6 ms       3.8 ms          2.4 ms
 * search      15.8 ms       9.5 ms         22.0 ms     47393 map objects
 * poi           2.1 ms      2.3 ms          3.5 ms      6285 amenities
 * ```
 * Kotlin/Native reads a map object in about the time the java reader takes on the jvm, and in
 * about twice the time this same code takes there; an amenity costs it only half again as much.
 * The open column is not a like for like: java also reads the address and transport headers,
 * which the copy skips.
 */
class MapReaderBenchmarkTest {

	@Test
	fun benchmarkMapSection() {
		if (testEnvironment("OSMAND_MAP_BENCHMARK") == null) {
			println("MapReaderBenchmarkTest: set OSMAND_MAP_BENCHMARK to run")
			return
		}
		val files = files()
		if (files.isEmpty()) {
			println("MapReaderBenchmarkTest: no obf files found, looked in ${directories()}")
			return
		}
		println("")
		println("### map section, shared copy on ${testPlatformName()}, ${files.size} files, " +
				"$WARMUP_ROUNDS warmup / $MEASURED_ROUNDS measured, best time")
		println("")
		println("  ${"phase".padEnd(10)} ${"ms".padStart(9)} ${"objects".padStart(9)} ${"us/object".padStart(10)}")

		var openMs = Double.MAX_VALUE
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val mark = TimeSource.Monotonic.markNow()
			val readers = files.map { BinaryMapIndexReader(it) }
			val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0
			readers.forEach { it.close() }
			if (round >= WARMUP_ROUNDS && ms < openMs) {
				openMs = ms
			}
		}
		println("  ${"open".padEnd(10)} ${openMs.format1().padStart(9)} ${"".padStart(9)} ${"".padStart(10)}")

		var searchMs = Double.MAX_VALUE
		var objects = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val readers = files.map { BinaryMapIndexReader(it) }
			val mark = TimeSource.Monotonic.markNow()
			var read = 0
			for (reader in readers) {
				for (zoom in zooms(reader)) {
					val req = SearchRequest.buildSearchRequest(
						0, Int.MAX_VALUE, 0, Int.MAX_VALUE, zoom, null
					)
					read += reader.searchMapIndex(req).size
				}
			}
			val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0
			readers.forEach { it.close() }
			if (round >= WARMUP_ROUNDS && ms < searchMs) {
				searchMs = ms
				objects = read
			}
		}
		println("  ${"search".padEnd(10)} ${searchMs.format1().padStart(9)} ${objects.toString().padStart(9)} " +
				(searchMs * 1000 / objects).format2().padStart(10))

		val poiTypes = poiTypes()
		if (poiTypes == null) {
			println("  poi        poi_types.xml not found, skipped")
			println("")
			return
		}
		MapPoiTypes.setDefault(poiTypes)
		var poiMs = Double.MAX_VALUE
		var amenities = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val readers = files.map { BinaryMapIndexReader(it) }
			val mark = TimeSource.Monotonic.markNow()
			var read = 0
			repeat(POI_PASSES_PER_ROUND) {
				read = 0
				for (reader in readers) {
					val req = SearchRequest.buildSearchPoiRequest(
						0, Int.MAX_VALUE, 0, Int.MAX_VALUE, -1, null, null, null
					)
					read += reader.searchPoi(req).size
				}
			}
			val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0 / POI_PASSES_PER_ROUND
			readers.forEach { it.close() }
			if (round >= WARMUP_ROUNDS && ms < poiMs) {
				poiMs = ms
				amenities = read
			}
		}
		println("  ${"poi".padEnd(10)} ${poiMs.format1().padStart(9)} ${amenities.toString().padStart(9)} " +
				(poiMs * 1000 / amenities).format2().padStart(10))
		println("")
	}

	/** The poi search needs the type registry, which the poi section decodes its numbers through. */
	private fun poiTypes(): MapPoiTypes? {
		for (directory in directories()) {
			val path = "$directory/poi_types.xml".toPath()
			if (FileSystem.SYSTEM.exists(path)) {
				val types = MapPoiTypes(path.toString())
				types.init()
				return types
			}
		}
		return null
	}

	/** Every zoom the file has a level for, so that the whole section is read. */
	private fun zooms(reader: BinaryMapIndexReader): List<Int> {
		val zooms = ArrayList<Int>()
		for (index in reader.getMapIndexes()) {
			for (root in index.getRoots()) {
				if (!zooms.contains(root.getMinZoom())) {
					zooms.add(root.getMinZoom())
				}
			}
		}
		return zooms
	}

	private fun directories(): List<String> {
		val directories = ArrayList(TEST_RESOURCE_DIRECTORIES)
		testEnvironment("OSMAND_OBF_DIRECTORY")?.let { directories.add(0, it) }
		testEnvironment("OSMAND_TEST_RESOURCES")?.let { directories.add(0, it) }
		return directories
	}

	/** The obf files of the test resources, the turn lanes map first and then the routing maps. */
	private fun files(): List<String> {
		for (directory in directories()) {
			val root = directory.toPath()
			if (!FileSystem.SYSTEM.exists(root)) {
				continue
			}
			val found = ArrayList<String>()
			val turnLanes = root.resolve("Turn_lanes_test.obf")
			if (FileSystem.SYSTEM.exists(turnLanes)) {
				found.add(turnLanes.toString())
			}
			val routing = root.resolve("routing")
			if (FileSystem.SYSTEM.exists(routing)) {
				found.addAll(FileSystem.SYSTEM.list(routing)
					.filter { it.name.endsWith(".obf") }
					.map { it.toString() }
					.sorted())
			}
			if (found.isNotEmpty()) {
				return found
			}
		}
		return emptyList()
	}

	private fun Double.format1(): String = (kotlin.math.round(this * 10) / 10).toString()

	private fun Double.format2(): String = (kotlin.math.round(this * 100) / 100).toString()

	companion object {
		const val WARMUP_ROUNDS = 3
		const val MEASURED_ROUNDS = 5

		/** One pass over the poi sections is only a few milliseconds, too little to time. */
		const val POI_PASSES_PER_ROUND = 10

		/** Relative to `OsmAnd-shared`, which is where both the jvm test and the native binary run. */
		val TEST_RESOURCE_DIRECTORIES = listOf(
			"../OsmAnd-java/src/test/resources",
			"OsmAnd-java/src/test/resources",
		)
	}
}
