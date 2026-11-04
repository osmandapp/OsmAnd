package net.osmand.shared.travel

import net.osmand.shared.binary.BinaryMapDataObject
import net.osmand.shared.binary.MapIndex
import net.osmand.shared.data.Amenity
import net.osmand.shared.gpx.TravelObfGpxTrackOptimizer
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.io.KFile
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTIntObjectMap
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy takes to turn what the reader found into a track, which is the part of
 * loading a travel route that is not reading the file.
 *
 * Three things are timed, all of which run once per track opened and grow with its length. Joining
 * the pieces is what the indexer's cut per region costs: a long route arrives as several pieces
 * that overlap near each boundary and have to be trimmed and put back in order. Building the
 * points is the other half: each point of the track is an amenity that becomes a gpx point with
 * its tags carried over. Building the gpx file is the two of them together plus everything else
 * the builder does per route - the tags, the point groups, the ordering of the waypoints - and is
 * what a screen actually waits for once the files have been read.
 *
 * The route the issue is about, Cesta hrdinov SNP, is 736 km from two maps, which at the spacing a
 * recorded track has is on the order of twenty thousand points - the size used here.
 *
 * The same work runs through the java classes and through this copy on the jvm in
 * `TravelBenchmarkTest` in OsmAnd-java, so the columns can be put side by side.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_TRAVEL_BENCHMARK` is set, since
 * a Kotlin/Native test binary cannot be told to run an ignored test. On the jvm, from `android`:
 * ```
 * OSMAND_TRAVEL_BENCHMARK=1 ./gradlew --no-daemon :OsmAnd-shared:jvmTest --tests "*TravelBenchmarkTest" -i
 * ```
 * On Kotlin/Native only the **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_TRAVEL_BENCHMARK=1 xcrun simctl spawn --standalone <udid> \
 *   build/bin/iosSimulatorArm64/releaseTest/test.kexe --ktest_filter='net.osmand.shared.travel.TravelBenchmarkTest.*'
 * ```
 * Over a route of 20000 points in 8 pieces, with 20000 points built on their own and the whole
 * file built from 8 ways and 500 waypoints:
 * ```
 * what                          java (jvm)   copy (jvm)   copy (native)
 * join the pieces of a route       2.3 ms       2.9 ms          6.4 ms
 * build the points of a track         -      6.5 - 10.5 ms      8.7 ms
 * build the gpx file of a route       -       4.4 - 5.6 ms      7.9 ms
 * ```
 * The last two have no java column: their originals, `TravelGpx` and `TravelObfGpxFileReader`,
 * are in the android app, which the java test module cannot see. Building the points swings by
 * half from run to run on the jvm - each round allocates twenty thousand points with their tag
 * maps, and the garbage collector lands inside the measured window - while Kotlin/Native holds
 * steady, which is why it ends up the faster of the two there.
 *
 * The rows do not add up: the first two measure one part each at the full length of the route,
 * while the last is the whole build at the mix a real route has, where only a few hundred of the
 * points are waypoints. Eight milliseconds on Kotlin/Native for the whole build is nothing against
 * the budget the issue sets for opening a route: what that second is spent on is reading the
 * files, not this.
 */
class TravelBenchmarkTest {

	@Test
	fun benchmarkTravel() {
		if (testEnvironment("OSMAND_TRAVEL_BENCHMARK") == null) {
			println("TravelBenchmarkTest: set OSMAND_TRAVEL_BENCHMARK to run")
			return
		}
		println("")
		println("### travel, shared copy on ${testPlatformName()}, " +
				"$WARMUP_ROUNDS warmup / $MEASURED_ROUNDS measured, best time")
		println("")
		println("  ${"what".padEnd(34)} ${"ms".padStart(9)} ${"count".padStart(9)} ${"us each".padStart(9)}")

		var joinMs = Double.MAX_VALUE
		var joined = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val track = routeCutIntoPieces(POINTS, PIECES)
			val mark = TimeSource.Monotonic.markNow()
			val result = TravelObfGpxTrackOptimizer.mergeOverlappedSegmentsAtEdges(track)
			val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0
			if (round >= WARMUP_ROUNDS && ms < joinMs) {
				joinMs = ms
				joined = result.segments.sumOf { it.points.size }
			}
		}
		row("join the pieces of a route", joinMs, joined)

		val amenities = trackPoints(POINTS)
		var buildMs = Double.MAX_VALUE
		var built = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val gpx = TravelGpx()
			val mark = TimeSource.Monotonic.markNow()
			var points = 0
			for (amenity in amenities) {
				gpx.createWptPt(amenity, "en")
				points++
			}
			val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0
			if (round >= WARMUP_ROUNDS && ms < buildMs) {
				buildMs = ms
				built = points
			}
		}
		row("build the points of a track", buildMs, built)

		val builder = TravelObfGpxBuilder(FakeTravelObfContext())
		val routeAmenity = routeAmenity()
		val waypoints = trackPoints(WAYPOINTS)
		val segments = routeSegments(POINTS, PIECES)
		val repositories = listOf(
			FakeAmenityIndexRepository(
				KFile("Benchmark.travel.obf"), listOf(routeAmenity) + waypoints, segments
			).repository
		)
		var gpxMs = Double.MAX_VALUE
		var gpxPoints = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val article = TravelGpx(routeAmenity)
			val mark = TimeSource.Monotonic.markNow()
			val gpx = builder.buildGpxFile(repositories, article) { false }
			val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0
			if (round >= WARMUP_ROUNDS && ms < gpxMs) {
				gpxMs = ms
				gpxPoints = (gpx?.tracks?.sumOf { t -> t.segments.sumOf { it.points.size } } ?: 0) +
						(gpx?.getPointsList()?.size ?: 0)
			}
		}
		row("build the gpx file of a route", gpxMs, gpxPoints)
		println("")
	}

	/** The amenity that stands for the route itself, with the tags a built route carries. */
	private fun routeAmenity(): Amenity {
		val amenity = Amenity()
		amenity.setId(1)
		amenity.setSubType("routes_hiking;route_track")
		amenity.setName("Cesta hrdinov SNP")
		amenity.setName("en", "Path of SNP Heroes")
		amenity.setLocation(48.0, 17.0)
		amenity.setRegionName("Slovakia")
		amenity.setAdditionalInfo("route_id", "O7700604")
		amenity.setAdditionalInfo("route_bbox_radius", "C")
		amenity.setAdditionalInfo("route_name", "Cesta hrdinov SNP")
		amenity.setAdditionalInfo("url", "https://example.org/snp")
		amenity.setAdditionalInfo("url_text", "SNP")
		amenity.setAdditionalInfo("distance", "736000")
		amenity.setAdditionalInfo("ref", "E8")
		amenity.setAdditionalInfo("user", "mapper")
		return amenity
	}

	/** The route as the map section holds it: several ways, each tagged with the route_id. */
	private fun routeSegments(points: Int, pieces: Int): List<BinaryMapDataObject> {
		val mapIndex = MapIndex()
		mapIndex.initMapEncodingRule(0, 1, "highway", "path")
		mapIndex.initMapEncodingRule(0, 2, Amenity.ROUTE_ID, null)
		mapIndex.finishInitializingTags()

		val segments = ArrayList<BinaryMapDataObject>(pieces)
		val per = points / pieces
		for (piece in 0 until pieces) {
			val from = maxOf(0, piece * per - OVERLAP)
			val to = minOf(points, (piece + 1) * per)
			val coordinates = IntArray((to - from) * 2)
			for (i in from until to) {
				coordinates[(i - from) * 2] = KMapUtils.get31TileNumberX(17.0 + i * 0.00005)
				coordinates[(i - from) * 2 + 1] = KMapUtils.get31TileNumberY(48.0 + i * 0.0001)
			}
			val names = KTIntObjectMap<String>()
			names.put(2, "O7700604")
			val namesOrder = KTIntArrayList()
			namesOrder.add(2)

			val obj = BinaryMapDataObject()
			obj.id = piece.toLong()
			obj.mapIndex = mapIndex
			obj.types = intArrayOf(1)
			obj.objectNames = names
			obj.namesOrder = namesOrder
			obj.coordinates = coordinates
			segments.add(obj)
		}
		return segments
	}

	private fun row(what: String, ms: Double, count: Int) {
		val each = if (count > 0) (ms * 1000 / count).format2() else ""
		println("  ${what.padEnd(34)} ${ms.format1().padStart(9)} ${count.toString().padStart(9)} " +
				each.padStart(9))
	}

	/** One long route as the indexer leaves it: several pieces, overlapping near each cut. */
	private fun routeCutIntoPieces(points: Int, pieces: Int): Track {
		val whole = ArrayList<WptPt>(points)
		for (i in 0 until points) {
			whole.add(WptPt(48.0 + i * 0.0001, 17.0 + i * 0.00005))
		}
		val track = Track()
		val segments = ArrayList<TrkSegment>()
		val per = points / pieces
		for (piece in 0 until pieces) {
			val from = maxOf(0, piece * per - OVERLAP)
			val to = minOf(points, (piece + 1) * per)
			val segment = TrkSegment()
			for (i in from until to) {
				segment.points.add(WptPt(whole[i].lat, whole[i].lon))
			}
			segments.add(segment)
		}
		// the pieces arrive in the order the files were read, not in the order of the route
		segments.reverse()
		track.segments = segments
		return track
	}

	/** The points of a track as the poi section gives them: an amenity with a handful of tags. */
	private fun trackPoints(points: Int): List<Amenity> {
		val amenities = ArrayList<Amenity>(points)
		for (i in 0 until points) {
			val amenity = Amenity()
			amenity.setId((i + 1).toLong())
			amenity.setSubType("route_track_point")
			amenity.setName("Point $i")
			amenity.setLocation(48.0 + i * 0.0001, 17.0 + i * 0.00005)
			amenity.setAdditionalInfo("name:de", "Punkt $i")
			amenity.setAdditionalInfo("description", "A point of the route")
			amenity.setAdditionalInfo("ele", (100 + i % 900).toString())
			amenity.setAdditionalInfo("color", "red")
			amenity.setAdditionalInfo("route_id", "O7700604")
			if (i % 10 == 0) {
				amenity.setAdditionalInfo("wpt_extra_tags", """{"operator":"KST","capacity":40}""")
			}
			amenities.add(amenity)
		}
		return amenities
	}

	private fun Double.format1(): String = (kotlin.math.round(this * 10) / 10).toString()

	private fun Double.format2(): String = (kotlin.math.round(this * 100) / 100).toString()

	companion object {
		const val WARMUP_ROUNDS = 3
		const val MEASURED_ROUNDS = 5

		/** About what a 736 km route comes to at the spacing of a recorded track. */
		const val POINTS = 20000
		const val PIECES = 8
		const val OVERLAP = 20

		/** The waypoints of a route: huts, springs, viewpoints, not one per track point. */
		const val WAYPOINTS = 500
	}
}
