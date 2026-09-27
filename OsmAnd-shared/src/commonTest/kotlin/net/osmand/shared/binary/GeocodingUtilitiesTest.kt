package net.osmand.shared.binary

import net.osmand.shared.binary.GeocodingUtilities.GeocodingResult
import net.osmand.shared.data.Building
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.routing.RouteCalculationMode
import net.osmand.shared.routing.RoutePlannerFrontEnd
import net.osmand.shared.routing.RouteSegmentPoint
import net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits
import net.osmand.shared.routing.RoutingContext
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.AmenitySearcherTest
import net.osmand.shared.search.core.SearchCoreFactoryTest.Companion.v
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.dumpBits
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [GeocodingUtilities] wherever it runs, on Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `GeocodingCompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/geocoding-java.txt`, or wherever `OSMAND_GEOCODING_JAVA_DUMP` points: the
 * files, for points over them what java answered in each context, and what it made of roads named
 * as streets. It asks the copy the same,
 * in the same order, over contexts made the same way, and holds it to java; where java answered
 * otherwise only for the order of roads as far from the point to the last digit, which java takes
 * from a hash set of trove, to what the copy answered on the jvm. On the jvm exactly. On
 * Kotlin/Native, where the functions of the platform round otherwise, it allows and counts answers
 * with doubles off in their last digits; with a point of zoom 31 a unit off; with a building, a street
 * or a settlement read a last digit or a unit of zoom 24 off, and whatever the geocoding makes of the
 * distances after; and with a building a last digit off the point, where java has it at the point.
 */
class GeocodingUtilitiesTest {

	@Test
	fun sameAsJava() {
		val source = openJavaDump("GeocodingUtilitiesTest", "OSMAND_GEOCODING_JAVA_DUMP", "geocoding-java.txt", "GeocodingCompatTest")
			?: return
		SearchPhraseTest.setUp
		assertTrue(CommonWords.getInstance().getFrequentlyUsed("gelderland") >= 0, "the regions in the common words")
		val exact = testPlatformName().startsWith("jvm")
		val utils = GeocodingUtilities()
		val different = ArrayList<String>()
		var differentCount = 0
		var compared = 0
		var ordered = 0
		var inUlps = 0
		var offByATile = 0
		var addressesOff = 0
		var otherFirst = 0
		var atThePoint = 0
		var named = 0
		var opened: List<BinaryMapIndexReader> = emptyList()
		var contexts: List<RoutingContext> = emptyList()
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				when (f[0]) {
					"F" -> {
						close(contexts)
						val path = AmenitySearcherTest.path(dumpUnhex(f[2]))
						opened = listOf(BinaryMapIndexReader(path), BinaryMapIndexReader(path))
						contexts = contexts(opened[0], opened[1])
					}
					"S" -> {
						val name = dumpUnhex(f[2])
						val lat = Double.fromBits(f[3].toULong(16).toLong())
						val lon = Double.fromBits(f[4].toULong(16).toLong())
						val java = dumpUnhex(f[5])
						val copy = outcome { namedRoad(utils, opened[0], name, lat, lon) }
						named++
						if (java == copy) {
							// the same
						} else if (!exact && inUlps(java, copy)) {
							inUlps++
						} else if (!exact && sameStreets(java, copy) && addressesOff(java, copy)) {
							addressesOff++
						} else if (differentCount++ < 10) {
							different.add("${f[1]} road named $name: java\n$java\ncopy\n$copy")
						}
					}
					"P" -> {
						val c = f[2].toInt()
						val lat = Double.fromBits(f[3].toULong(16).toLong())
						val lon = Double.fromBits(f[4].toULong(16).toLong())
						val readers = if (c == TWICE) opened else opened.subList(0, 1)
						val java = dumpUnhex(if (f[6] == "-") f[5] else f[6])
						if (f[6] != "-") {
							ordered++
						}
						val copy = outcome { answer(utils, contexts[c], readers, lat, lon, c == WITH_EMPTY_NAMES) }
						compared++
						if (java == copy) {
							// the same
						} else if (!exact && inUlps(java, copy)) {
							inUlps++
						} else if (!exact && offByATile(java, copy)) {
							offByATile++
						} else if (!exact && sameRoads(java, copy) && addressesOff(java, copy)) {
							addressesOff++
							if (first(java) != first(copy)) {
								otherFirst++
							}
						} else if (!exact && sameRoads(java, copy) && aBuildingAtThePoint(utils, contexts[c], readers, lat, lon)) {
							atThePoint++
						} else if (differentCount++ < 10) {
							val jl = java.split("\n")
							val kl = copy.split("\n")
							val at = jl.indices.firstOrNull { it >= kl.size || !lineInUlps(jl[it], kl[it]) } ?: jl.size
							different.add("${f[1]} $c $lat,$lon: first different line $at of ${jl.size} / ${kl.size}\njava ${jl.getOrNull(at)}\ncopy ${kl.getOrNull(at)}")
						}
					}
				}
			}
		} finally {
			source.close()
			close(contexts)
		}
		if (differentCount > 0) {
			fail("$differentCount answers different from java:\n" + different.joinToString("\n"))
		}
		assertTrue(compared > 10000 && named > 500, "$compared answers compared, $named roads named as a street")
		println(
			"GeocodingUtilitiesTest: the same as java on ${testPlatformName()}: $compared answers and $named roads named as a street, " +
					"$ordered of them as the copy " +
					"answered on the jvm, for the order of roads as far; $inUlps with doubles off in the last digits, " +
					"$offByATile with a point a unit of zoom 31 off, " +
					"$addressesOff with an address read a last digit or a unit of zoom 24 off, $otherFirst of them with another address found first, " +
					"$atThePoint with a building a last digit off the point"
		)
	}

	companion object {
		/** `GeocodingCompatTest.CONTEXTS`: the index of the one with roads without names, and of the file opened twice. */
		private const val WITH_EMPTY_NAMES = 1
		private const val TWICE = 3
		/** `GeocodingCompatTest.JUSTIFIED` and `KNOWN_BUILDING_DISTANCE`. */
		private const val JUSTIFIED = 3
		private const val KNOWN_BUILDING_DISTANCE = 30.0

		/**
		 * The context [GeocodingUtilities.buildDefaultContextForPOI] makes, from the routing.xml of the
		 * tests, which Kotlin/Native has no other way to.
		 */
		internal fun defaultContext(reader: BinaryMapIndexReader): RoutingContext {
			val memory = GeocodingUtilities.GEOCODING_POI_MEMORY
			return RoutePlannerFrontEnd().buildRoutingContext(
				RoutingTestFixtures.defaultBuilder().build("car", RoutingMemoryLimits(memory, memory)),
				listOf(BinaryMapIndexReader(reader.getFile())), RouteCalculationMode.NORMAL
			)
		}

		/** Closes the files of [contexts], the ones [defaultContext] opens again among them. */
		internal fun close(contexts: List<RoutingContext>) {
			contexts.forEach { c -> c.map.keys.forEach { it.close() } }
		}

		/** The contexts of `GeocodingCompatTest.CONTEXTS`. */
		private fun contexts(reader: BinaryMapIndexReader, second: BinaryMapIndexReader): List<RoutingContext> {
			val builder = RoutingTestFixtures.defaultBuilder()
			val memory = GeocodingUtilities.GEOCODING_POI_MEMORY
			val planner = RoutePlannerFrontEnd()
			return listOf(
				defaultContext(reader), defaultContext(reader),
				planner.buildRoutingContext(builder.build("geocoding", RoutingMemoryLimits(10, 10), LinkedHashMap()), listOf(reader), null),
				planner.buildRoutingContext(builder.build("car", RoutingMemoryLimits(memory, memory)), listOf(reader, second), RouteCalculationMode.NORMAL)
			)
		}

		private val CANCELLED = object : ResultMatcher<GeocodingResult> {
			override fun publish(obj: GeocodingResult): Boolean = false

			override fun isCancelled(): Boolean = true
		}

		/** `GeocodingCompatTest.copyAnswer`. */
		internal fun answer(
			utils: GeocodingUtilities, ctx: RoutingContext, readers: List<BinaryMapIndexReader>,
			lat: Double, lon: Double, allowEmptyNames: Boolean
		): String {
			val sb = StringBuilder()
			val found = utils.reverseGeocodingSearch(ctx, lat, lon, allowEmptyNames)
			lines(sb, "A", found)
			for (i in 0 until minOf(JUSTIFIED, found.size)) {
				val r = found[i]
				val reader = readers.firstOrNull { b ->
					b.getRoutingIndexes().any { r.regionFP == it.getFilePointer() && r.regionLen == it.getLength() }
				}
				if (reader != null) {
					lines(sb, "B$i", utils.justifyReverseGeocodingSearch(r, reader, 0.0, null))
					lines(sb, "K$i", utils.justifyReverseGeocodingSearch(r, reader, KNOWN_BUILDING_DISTANCE, null))
					lines(sb, "X$i", utils.justifyReverseGeocodingSearch(r, reader, 0.0, CANCELLED))
				}
			}
			lines(sb, "C", utils.sortGeocodingResults(readers, utils.reverseGeocodingSearch(ctx, lat, lon, allowEmptyNames)))
			return sb.toString()
		}

		/** `GeocodingCompatTest.copyNamedRoad`. */
		private fun namedRoad(utils: GeocodingUtilities, reader: BinaryMapIndexReader, name: String, lat: Double, lon: Double): String {
			val road = GeocodingResult()
			road.searchPoint = KLatLon(lat, lon)
			road.connectionPoint = KLatLon(lat, lon)
			road.streetName = name
			val sb = StringBuilder()
			lines(sb, "B", utils.justifyReverseGeocodingSearch(road, reader, 0.0, null))
			lines(sb, "K", utils.justifyReverseGeocodingSearch(road, reader, KNOWN_BUILDING_DISTANCE, null))
			return sb.toString()
		}

		private fun lines(sb: StringBuilder, tag: String, list: List<GeocodingResult>) {
			sb.append(tag).append(' ').append(list.size).append('\n')
			list.forEach { sb.append(line(it)).append('\n') }
		}

		/** `GeocodingCompatTest.line`. */
		private fun line(r: GeocodingResult): String =
			v(r.streetName) + "|" + r.regionFP + "|" + r.regionLen + "|" + loc(r.searchPoint) + "|" + loc(r.connectionPoint) +
					"|" + point(r.point) + "|" + mapObject(r.building) + "|" + v(r.buildingInterpolation) +
					"|" + mapObject(r.street) + "|" + mapObject(r.city) +
					"|" + d(r.getDistance()) + "|" + d(r.getCityDistance()) + "|" + d(r.getSortDistance()) +
					"|" + v(r.getBuildingString()) + "|" + v(r.toString())

		private fun point(p: RouteSegmentPoint?): String =
			if (p == null) "null" else p.getRoad().getId().toString() + ":" + p.getSegmentStart() + ":" + p.getSegmentEnd() +
					":" + p.preciseX + ":" + p.preciseY + ":" + d(p.distToProj)

		private fun mapObject(o: MapObject?): String {
			if (o == null) {
				return "-"
			}
			var s = v(o.getName()) + ":" + o.getId() + ":" + loc(o.getLocation())
			if (o is Building) {
				s += ":" + loc(o.getLatLon2())
			}
			return s
		}

		private fun loc(l: KLatLon?): String = if (l == null) "null" else "(" + d(l.latitude) + "," + d(l.longitude) + ")"

		private fun d(x: Double): String = "#" + dumpBits(x)

		private fun outcome(c: () -> String): String = try {
			c()
		} catch (t: Throwable) {
			"threw:" + t::class.simpleName
		}

		private val BITS = Regex("#([0-9a-f]+)")

		/**
		 * Whether two answers are equal but for doubles apart in their last digits: by a ten billionth,
		 * and the distances of a result also by a millionth of a metre, which a distance of less than a
		 * metre is off by when its points are a last digit off.
		 */
		private fun inUlps(java: String, copy: String): Boolean {
			val jl = java.split("\n")
			val kl = copy.split("\n")
			if (jl.size != kl.size) {
				return false
			}
			return jl.indices.all { lineInUlps(jl[it], kl[it]) }
		}

		private fun lineInUlps(java: String, copy: String): Boolean {
			val jf = java.split("|")
			val kf = copy.split("|")
			return jf.size == kf.size && jf.indices.all { close(jf[it], kf[it], if (it in DISTANCES) 1e-6 else 0.0) }
		}

		/**
		 * Whether two answers are the same but for a point of zoom 31 a unit off, as the platform works
		 * out the tile of the point geocoded and the projection of it on a road with its own sines and
		 * logarithms: the precise point on the road may be a unit off, and with it the point the result
		 * connects to and the distances, by a few centimetres, and the whole metres of the text.
		 */
		private fun offByATile(java: String, copy: String): Boolean {
			val jl = java.split("\n")
			val kl = copy.split("\n")
			return jl.size == kl.size && jl.indices.all { lineInUlps(jl[it], kl[it]) || lineOffByATile(jl[it], kl[it]) }
		}

		private fun lineOffByATile(java: String, copy: String): Boolean {
			val jf = java.split("|")
			val kf = copy.split("|")
			if (jf.size != 15 || kf.size != 15) {
				return false
			}
			for (i in jf.indices) {
				val same = when (i) {
					4 -> close(jf[i], kf[i], 0.0) || withinATile(jf[i], kf[i])
					5 -> pointOffByATile(jf[i], kf[i])
					in DISTANCES -> close(jf[i], kf[i], CENTIMETRES)
					14 -> jf[i].substringBefore("\\sdist=") == kf[i].substringBefore("\\sdist=") &&
							abs(metres(jf[i]) - metres(kf[i])) <= 1
					else -> close(jf[i], kf[i], 0.0)
				}
				if (!same) {
					return false
				}
			}
			return true
		}

		/** How far off the platform may put a distance, when it has a point a unit of zoom 31 off. */
		private const val CENTIMETRES = 0.05

		private fun doubles(field: String): List<Double> =
			BITS.findAll(field).map { Double.fromBits(it.groupValues[1].toULong(16).toLong()) }.toList()

		/** Whether two points `(lat,lon)` are in the same or the next tile of zoom 31. */
		private fun withinATile(java: String, copy: String): Boolean {
			val a = doubles(java)
			val b = doubles(copy)
			return a.size == 2 && b.size == 2 && java.replace(BITS, "#") == copy.replace(BITS, "#") &&
					abs(KMapUtils.getTileNumberY(31.0, a[0]) - KMapUtils.getTileNumberY(31.0, b[0])) <= 1.0 + 1e-6 &&
					abs(KMapUtils.getTileNumberX(31.0, a[1]) - KMapUtils.getTileNumberX(31.0, b[1])) <= 1.0 + 1e-6
		}

		/** The road point `id:start:end:x:y:#distToProj`, with x and y a unit off and the distance a few centimetres. */
		private fun pointOffByATile(java: String, copy: String): Boolean {
			val a = java.split(":")
			val b = copy.split(":")
			if (a.size != 6 || b.size != 6 || a.subList(0, 3) != b.subList(0, 3)) {
				return false
			}
			if (abs(a[3].toLong() - b[3].toLong()) > 1 || abs(a[4].toLong() - b[4].toLong()) > 1) {
				return false
			}
			val x = doubles(a[5]).single()
			val y = doubles(b[5]).single()
			return abs(sqrt(x) - sqrt(y)) <= CENTIMETRES
		}

		/** The whole metres at the end of the text of a result: 0 without them. */
		private fun metres(text: String): Int = text.substringAfter("\\sdist=", "0").toIntOrNull() ?: -1000

		/**
		 * Whether the roads found, section A, are the same but for what [inUlps] and [offByATile]
		 * allow: they do not depend on the address reader.
		 */
		private fun sameRoads(java: String, copy: String): Boolean {
			val jl = java.split("\n")
			val kl = copy.split("\n")
			if (jl[0] != kl[0] || !jl[0].startsWith("A ")) {
				return jl[0] == kl[0] && !jl[0].startsWith("A ")
			}
			val n = jl[0].substringAfter(' ').toInt()
			return (1..n).all { lineInUlps(jl[it], kl[it]) || lineOffByATile(jl[it], kl[it]) }
		}

		/**
		 * Whether both found the same streets, by name and id, which the address reader finds by name
		 * wherever it puts them.
		 */
		private fun sameStreets(java: String, copy: String): Boolean =
			addresses(java).keys.filter { it.startsWith("S") }.toSet() == addresses(copy).keys.filter { it.startsWith("S") }.toSet()

		/**
		 * Whether the copy read a building, a street or a settlement of the answer a last digit or a unit
		 * of zoom 24 north or south of where java read it. The address reader works the tile of a street
		 * out again from its latitude and drops the fraction, and the functions of the platform drop it
		 * for other streets than java's; its buildings move with it, and a building may be a last digit
		 * off. What the geocoding does after goes by distances, and so may differ: the order of the
		 * results, which buildings are near enough, and even the result found first, as a building at
		 * the point, at a distance of 0, is one java takes for no building known yet.
		 */
		private fun addressesOff(java: String, copy: String): Boolean {
			val j = addresses(java)
			val k = addresses(copy)
			return j.any { (key, places) ->
				val others = k[key].orEmpty()
				places.any { p ->
					p !in others && others.any { q -> q !in places && (close(p, q, 0.0) || shiftedNorthOrSouth(p, q)) }
				}
			}
		}

		/**
		 * Whether the copy finds a building a last digit off the point, less than [AT_THE_POINT] away,
		 * for one of the roads at it. Java takes a distance of 0 for no building known yet: for a
		 * building at the point it adds the first building of every street after, and sorting takes the
		 * distance of the next result for the nearest building, so that a building a last digit off
		 * gets other results than one at the point.
		 */
		private fun aBuildingAtThePoint(
			utils: GeocodingUtilities, ctx: RoutingContext, readers: List<BinaryMapIndexReader>, lat: Double, lon: Double
		): Boolean {
			for (r in utils.reverseGeocodingSearch(ctx, lat, lon, true)) {
				val reader = readers.firstOrNull { b ->
					b.getRoutingIndexes().any { r.regionFP == it.getFilePointer() && r.regionLen == it.getLength() }
				} ?: continue
				if (utils.justifyReverseGeocodingSearch(r, reader, 0.0, null).any {
						it.building != null && it.getDistance() > 0 && it.getDistance() < AT_THE_POINT
					}) {
					return true
				}
			}
			return false
		}

		private const val AT_THE_POINT = 1e-6

		/** The building, street and settlement of the first result the geocoding sorted, without where they are. */
		private fun first(answer: String): String {
			val lines = answer.split("\n")
			val c = lines.indexOfFirst { it.startsWith("C ") }
			val f = lines.getOrNull(c + 1)?.split("|")
			if (c < 0 || f == null || f.size != 15) {
				return lines.getOrNull(c) ?: "-"
			}
			return f[0] + "|" + listOf(f[6], f[8], f[9]).joinToString("|") { it.split(":").take(2).joinToString(":") }
		}

		/**
		 * The buildings, streets and settlements of the results of an answer, with where they are: the
		 * streets and settlements by name and id, the buildings, which may have no id, by name and street;
		 * a street may have two buildings of one name.
		 */
		private fun addresses(answer: String): Map<String, Set<String>> {
			val found = HashMap<String, MutableSet<String>>()
			for (line in answer.split("\n")) {
				val f = line.split("|")
				if (f.size == 15) {
					val street = f[8].split(":")
					if (f[6] != "-") {
						val b = f[6].split(":")
						found.getOrPut("B" + b[0] + "@" + street[0] + ":" + street.getOrNull(1)) { HashSet() }.add(b[2])
					}
					for ((kind, o) in listOf("S" to f[8], "C" to f[9])) {
						if (o != "-") {
							val parts = o.split(":")
							found.getOrPut(kind + parts[0] + ":" + parts[1]) { HashSet() }.add(parts[2])
						}
					}
				}
			}
			return found
		}

		private fun shiftedNorthOrSouth(java: String, copy: String): Boolean {
			val a = doubles(java)
			val b = doubles(copy)
			return a.size == 2 && b.size == 2 && close(java.substringAfter(","), copy.substringAfter(","), 0.0) &&
					abs(KMapUtils.getTileNumberY(24.0, a[0]) - KMapUtils.getTileNumberY(24.0, b[0])) <= 1.0 + 1e-6
		}

		/** The fields of a result that are distances in metres. */
		private val DISTANCES = 10..12

		private fun close(java: String, copy: String, metres: Double): Boolean {
			val a = BITS.findAll(java).map { it.groupValues[1] }.toList()
			val b = BITS.findAll(copy).map { it.groupValues[1] }.toList()
			if (a.size != b.size || java.replace(BITS, "#") != copy.replace(BITS, "#")) {
				return false
			}
			return a.indices.all {
				val x = Double.fromBits(a[it].toULong(16).toLong())
				val y = Double.fromBits(b[it].toULong(16).toLong())
				x == y || abs(x - y) <= 1e-10 * maxOf(abs(x), abs(y)) || abs(x - y) <= metres
			}
		}
	}
}
