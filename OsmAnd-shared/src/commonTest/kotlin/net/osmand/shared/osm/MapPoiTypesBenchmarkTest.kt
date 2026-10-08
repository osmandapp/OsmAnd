package net.osmand.shared.osm

import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy takes to read poi_types.xml, which the app does once at startup before
 * anything can be read out of a poi section.
 *
 * The same file runs through the java registry and through this copy on the jvm in
 * `MapPoiTypesBenchmarkTest` in OsmAnd-java, so the columns can be put side by side. The copy is a
 * straight port, so nothing here defends a design decision; what it answers is what the xml parser
 * costs on Kotlin/Native, where it goes through libxml2 rather than through the jvm's.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_POI_TYPES_BENCHMARK` is set,
 * since a Kotlin/Native test binary cannot be told to run an ignored test. On the jvm, from
 * `android`:
 * ```
 * OSMAND_POI_TYPES_BENCHMARK=1 ./gradlew --no-daemon :OsmAnd-shared:jvmTest --tests "*MapPoiTypesBenchmarkTest" -i
 * ```
 * On Kotlin/Native only the **release** binary says anything about speed, and it does not run in
 * the repository, so it has to be told where the file is:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_POI_TYPES_BENCHMARK=1 \
 *   SIMCTL_CHILD_OSMAND_TEST_RESOURCES=<repo>/OsmAnd-java/src/test/resources \
 *   xcrun simctl spawn --standalone <udid> build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.osm.MapPoiTypesBenchmarkTest.*'
 * ```
 * Reading the 470 KB poi_types.xml of the tests, 13256 types:
 * ```
 * java (jvm)   copy (jvm)   copy (native)
 *    4.7 ms       4.8 ms         13.0 ms
 * ```
 * The two are the same on the jvm; Kotlin/Native pays about three times that, once at startup.
 */
class MapPoiTypesBenchmarkTest {

	@Test
	fun benchmarkPoiTypes() {
		if (testEnvironment("OSMAND_POI_TYPES_BENCHMARK") == null) {
			println("MapPoiTypesBenchmarkTest: set OSMAND_POI_TYPES_BENCHMARK to run")
			return
		}
		val path = file()
		if (path == null) {
			println("MapPoiTypesBenchmarkTest: poi_types.xml not found, looked in ${directories()}")
			return
		}
		var best = Double.MAX_VALUE
		var types = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val mark = TimeSource.Monotonic.markNow()
			val registry = MapPoiTypes(path)
			registry.init()
			val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0
			if (round >= WARMUP_ROUNDS && ms < best) {
				best = ms
				types = count(registry)
			}
		}
		println("")
		println("### poi_types.xml, shared copy on ${testPlatformName()}, " +
				"$WARMUP_ROUNDS warmup / $MEASURED_ROUNDS measured, best time")
		println("")
		println("  ${"read".padEnd(10)} ${best.format1().padStart(9)} ms   $types types")
		println("")
	}

	/** Every type of every filter of every category, plus their additional attributes. */
	private fun count(registry: MapPoiTypes): Int {
		var types = 0
		for (category in registry.getCategories()) {
			types += category.getPoiTypes().size
			for (type in category.getPoiTypes()) {
				types += type.getPoiAdditionals().size
			}
			types += category.getPoiAdditionals().size
		}
		return types
	}

	private fun directories(): List<String> {
		val directories = ArrayList(TEST_RESOURCE_DIRECTORIES)
		testEnvironment("OSMAND_TEST_RESOURCES")?.let { directories.add(0, it) }
		return directories
	}

	private fun file(): String? {
		for (directory in directories()) {
			val path = "$directory/poi_types.xml".toPath()
			if (FileSystem.SYSTEM.exists(path)) {
				return path.toString()
			}
		}
		return null
	}

	private fun Double.format1(): String = (kotlin.math.round(this * 10) / 10).toString()

	companion object {
		const val WARMUP_ROUNDS = 3
		const val MEASURED_ROUNDS = 5

		/** Relative to `OsmAnd-shared`, which is where the jvm test runs. */
		val TEST_RESOURCE_DIRECTORIES = listOf(
			"../OsmAnd-java/src/test/resources",
			"OsmAnd-java/src/test/resources",
		)
	}
}
