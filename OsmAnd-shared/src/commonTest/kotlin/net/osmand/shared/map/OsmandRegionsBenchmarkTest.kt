package net.osmand.shared.map

import net.osmand.shared.data.KLatLon
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy takes to read `regions.ocbf` into the tree, which an app does once at
 * start, and to look up the smallest region under a point and the regions to download there. The
 * java half, which puts the java classes next to this copy on the jvm, is
 * `OsmandRegionsBenchmarkTest` in OsmAnd-java, over the same file and phases.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_REGIONS_BENCHMARK` is set. The
 * file is OsmAnd-java's, or wherever `OSMAND_REGIONS_OCBF` points. On Kotlin/Native only the
 * **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_REGIONS_BENCHMARK=1 SIMCTL_CHILD_OSMAND_REGIONS_OCBF=<regions.ocbf> \
 *   xcrun simctl spawn --standalone <udid> build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.map.OsmandRegionsBenchmarkTest.*'
 * ```
 *
 * On the `regions.ocbf` of 24.09.2026, 7 MB with 1 724 regions, best of five in ms, next to the java
 * half:
 * ```
 * what                               java    copy  native   count
 * read the file into the tree        46.8    44.1   143.1    1724
 * smallest region, 500 points       406.3   284.4   714.8     498
 * regions to download, 500 points   406.4   283.2   712.3    1027
 * ```
 * Kotlin/Native reads the file at 3.2 times the copy on the jvm and looks a point up in 1.4 ms, 2.5
 * times. A lookup decodes the polygons of every region whose box holds the point, and most of that
 * time is in `CodedInputStream.readRawVarint32`, which every section of the shared reader reads
 * through.
 */
class OsmandRegionsBenchmarkTest {

	@Test
	fun benchmarkRegions() {
		if (testEnvironment("OSMAND_REGIONS_BENCHMARK") == null) {
			println("OsmandRegionsBenchmarkTest: set OSMAND_REGIONS_BENCHMARK to run")
			return
		}
		val path = testEnvironment("OSMAND_REGIONS_OCBF") ?: "../OsmAnd-java/src/main/resources/net/osmand/map/regions.ocbf"
		val best = DoubleArray(3) { Double.MAX_VALUE }
		var regions = 0
		var smallest = 0
		var toDownload = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			var mark = TimeSource.Monotonic.markNow()
			val copy = OsmandRegions(path)
			val open = mark.elapsedNow().inWholeMicroseconds / 1000.0

			val points = points(copy)
			mark = TimeSource.Monotonic.markNow()
			var found = 0
			for (ll in points) {
				if (copy.getSmallestBinaryMapDataObjectAt(ll) != null) {
					found++
				}
			}
			val smallestMs = mark.elapsedNow().inWholeMicroseconds / 1000.0

			mark = TimeSource.Monotonic.markNow()
			var download = 0
			for (ll in points) {
				download += copy.getRegionsToDownload(ll.latitude, ll.longitude).size
			}
			val downloadMs = mark.elapsedNow().inWholeMicroseconds / 1000.0
			copy.close()

			if (round >= WARMUP_ROUNDS) {
				val times = doubleArrayOf(open, smallestMs, downloadMs)
				for (i in times.indices) {
					if (times[i] < best[i]) {
						best[i] = times[i]
					}
				}
				regions = copy.getAllRegionData().size
				smallest = found
				toDownload = download
			}
		}
		println("")
		println("### regions.ocbf, shared copy on ${testPlatformName()}, best of $MEASURED_ROUNDS")
		println("  ${"what".padEnd(34)} ${"ms".padStart(9)} ${"count".padStart(9)}")
		row("read the file into the tree", best[0], regions)
		row("smallest region, $POINTS points", best[1], smallest)
		row("regions to download, $POINTS points", best[2], toDownload)
	}

	/** The centres of the first regions of the tree, which are on land and inside some region. */
	private fun points(regions: OsmandRegions): List<KLatLon> {
		val tree = ArrayList<WorldRegion>()
		collect(regions.getWorldRegion(), tree)
		return tree.mapNotNull { it.getRegionCenter() }.take(POINTS)
	}

	private fun collect(r: WorldRegion, result: MutableList<WorldRegion>) {
		result.add(r)
		for (s in r.getSubregions()) {
			collect(s, result)
		}
	}

	private fun row(what: String, ms: Double, count: Int) {
		val rounded = (kotlin.math.round(ms * 10) / 10).toString()
		println("  ${what.padEnd(34)} ${rounded.padStart(9)} ${count.toString().padStart(9)}")
	}

	companion object {
		const val WARMUP_ROUNDS = 3
		const val MEASURED_ROUNDS = 5
		const val POINTS = 500
	}
}
