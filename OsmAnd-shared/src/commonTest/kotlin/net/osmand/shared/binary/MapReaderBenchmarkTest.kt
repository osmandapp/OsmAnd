package net.osmand.shared.binary

import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy takes to read a map section, which is the question the port is asked:
 * whether an obf can be searched on Kotlin/Native without the C++ core.
 *
 * The same corpus and the same two phases run through the java reader and through this copy on the
 * jvm in `MapSectionBenchmarkTest` in OsmAnd-java, so the columns can be put side by side. The
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
 * search      15.8 ms       9.5 ms         22.0 ms     47393 objects
 * per object   0.33 us      0.20 us         0.46 us
 * ```
 * Kotlin/Native reads an object in about the time the java reader takes on the jvm, and in about
 * twice the time this same code takes there. The open column is not a like for like: java also
 * reads the address, poi and transport headers, which the copy skips.
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
		println("")
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

		/** Relative to `OsmAnd-shared`, which is where both the jvm test and the native binary run. */
		val TEST_RESOURCE_DIRECTORIES = listOf(
			"../OsmAnd-java/src/test/resources",
			"OsmAnd-java/src/test/resources",
		)
	}
}
