package net.osmand.shared.search

import net.osmand.shared.binary.BinaryAmenityIndexRepository
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.CityBlocks
import net.osmand.shared.binary.PoiSubType
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchPoiAdditionalFilter
import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.BaseDetailsObject
import net.osmand.shared.data.BaseDetailsObjectTest
import net.osmand.shared.data.City
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KLocation
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.map.WorldRegion
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.dumpBits
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [AmenitySearcher] wherever it runs, on Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `AmenitySearcherCompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/amenity-searcher-java.txt`, or wherever `OSMAND_AMENITY_SEARCHER_JAVA_DUMP`
 * points: the files it searched, and for amenities spread over them what java found around each,
 * the objects it made of each in every way, and the rest it asked. It asks the copy the same, over
 * the same files, and holds it to java: on the jvm exactly. On Kotlin/Native coordinates may be off
 * in their last digits, and the settlements and streets a unit of zoom 24 north, as the reader works
 * them out from tiles; an amenity on the edge of a box searched may be found on one side only: the
 * platform works the edge out a last digit off, which can put it in the next tile. The same goes for
 * an amenity on the edge of a tile a search thinned out by zoom keeps one amenity of.
 */
class AmenitySearcherTest {

	@Test
	fun sameAsJava() {
		val source = openJavaDump(
			"AmenitySearcherTest", "OSMAND_AMENITY_SEARCHER_JAVA_DUMP", "amenity-searcher-java.txt", "AmenitySearcherCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		val defaultTypes = MapPoiTypes.getDefault()
		val phrases = HashMap<String, String>()
		val readers = ArrayList<BinaryMapIndexReader>()
		val amenities = ArrayList<List<Amenity>>()
		val boxes = ArrayList<IntArray>()
		val compared = HashMap<Char, Int>()
		val different = ArrayList<String>()
		var inUlps = 0
		var shifted = 0
		var onTheEdge = 0
		var thinned = 0
		var searcher: AmenitySearcher? = null
		val exact = testPlatformName().startsWith("jvm")
		val settings = AmenitySearcher.Settings({ "en" }, { false }, null)
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				when (f[0]) {
					"X" -> phrases[dumpUnhex(f[1])] = dumpUnhex(f[2])
					"F" -> {
						val s = searcher ?: run {
							val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
							MapPoiTypes.setDefault(types)
							types.setPoiTranslator(SearchCoreFactoryTest.Translator(phrases))
							AmenitySearcher(types).also { searcher = it }
						}
						val path = path(dumpUnhex(f[2]))
						val reader = BinaryMapIndexReader(path)
						readers.add(reader)
						s.addAmenityRepository(path.substringAfterLast('/'), BinaryAmenityIndexRepository(path) { reader })
						val box = if (f[3] == "-") intArrayOf(0, Int.MAX_VALUE, 0, Int.MAX_VALUE) else f[3].split(",").map { it.toInt() }.toIntArray()
						boxes.add(box)
						amenities.add(reader.searchPoi(SearchRequest.buildSearchPoiRequest(box[0], box[1], box[2], box[3], -1, null, null, null)))
					}
					else -> {
						val s = searcher!!
						val kind = f[0][0]
						val file = f[1].toInt()
						val i = f[2].toInt()
						val extra = f[3]
						val java = dumpUnhex(f[4])
						val a = if (file >= 0 && i >= 0 && kind != 'C' && kind != 'S') amenities[file][i] else null
						val copy = outcome {
							answer(kind, s, settings, a, extra, file, i, readers, boxes)
						}
						compared[kind] = (compared[kind] ?: 0) + 1
						val radius = when (kind) {
							'P' -> AmenitySearcher.AMENITY_SEARCH_RADIUS
							'K' -> 1000
							'V' -> FILTERED_RADIUS
							'Q' -> 5000
							else -> 0
						}
						val differs = {
							if (different.size < 10) {
								val jl = java.split("\n")
								val kl = copy.split("\n")
								val at = jl.indices.firstOrNull { it >= kl.size || jl[it] != kl[it] } ?: jl.size
								val ids = { l: List<String> -> l.map { it.substringBefore(':') }.toSet() }
								different.add(
									"$kind $file $i $extra: ${jl.size} / ${kl.size} lines, first different $at\n" +
											"java ${jl.getOrNull(at)}\ncopy ${kl.getOrNull(at)}\n" +
											"only java ${jl.filter { it.substringBefore(':') !in ids(kl) }}\n" +
											"only copy ${kl.filter { it.substringBefore(':') !in ids(jl) }}"
								)
							}
						}
						if (java == copy) {
							// the same
						} else if (exact) {
							differs()
						} else if (radius > 0 && withoutTheEdge(java, copy, a!!.getLocation()!!, radius)) {
							onTheEdge++
						} else if (kind == 'V' && extra == "0" && thinnedOnATileEdge(java, copy, FILTERED_ZOOM + 3)) {
							thinned++
						} else {
							when (close(java, copy, kind == 'C' || kind == 'S')) {
								IN_ULPS -> inUlps++
								SHIFTED -> shifted++
								else -> differs()
							}
						}
					}
				}
			}
		} finally {
			source.close()
			MapPoiTypes.setDefault(defaultTypes)
			readers.forEach { it.close() }
		}
		if (different.isNotEmpty()) {
			fail("different from java:\n" + different.joinToString("\n"))
		}
		assertTrue(compared.values.sum() > 10000, "answers compared: $compared")
		println(
			"AmenitySearcherTest: the same as java on ${testPlatformName()}: " +
					compared.entries.sortedBy { it.key }.joinToString { "${it.value} ${it.key}" } +
					"; $inUlps with coordinates off in the last digits, $shifted settlements or streets a unit of zoom 24 off, " +
					"$onTheEdge searches with an amenity on the edge of the box found on one side only, " +
					"$thinned searches thinned out by zoom with an amenity on the edge of a tile"
		)
	}

	/** What `AmenitySearcherCompatTest` asked java for a line of [kind], asked of the copy. */
	private fun answer(
		kind: Char, s: AmenitySearcher, settings: AmenitySearcher.Settings, a: Amenity?, extra: String,
		file: Int, i: Int, readers: List<BinaryMapIndexReader>, boxes: List<IntArray>
	): String = when (kind) {
		'P' -> amenities(s.searchAmenities(a!!.getLocation()!!, settings))
		'D' -> details(s.searchDetailedObject(AmenitySearcher.Request(a!!), settings, null))
		'N' -> details(s.searchDetailedObject(byNames(a!!, extra.toInt()), settings))
		'O' -> details(s.searchDetailedObject(a as Any, settings))
		'B' -> details(s.searchDetailedObject(BaseDetailsObject(a, "en"), settings))
		'R' -> details(s.searchDetailedObject(AmenitySearcher.Request(BaseDetailsObjectTest.drawn(extra)), settings, null))
		'E' -> details(
			s.searchDetailedObject(
				AmenitySearcher.Request(BaseDetailsObjectTest.drawn(extra), listOf(a!!.toStringEn()), true), settings, null
			)
		)
		'Q' -> {
			val l = a!!.getLocation()!!
			val r = KMapUtils.calculateLatLonBbox(l.latitude, l.longitude, 5000)
			byFile(s.searchAmenitiesByName(dumpUnhex(extra), r.top, r.left, r.bottom, r.right, l.latitude, l.longitude, null))
		}
		'K' -> {
			val l = a!!.getLocation()!!
			val r = KMapUtils.calculateLatLonBbox(l.latitude, l.longitude, 1000)
			val nearest = compareBy<Amenity> { -KMapUtils.getDistance(it.getLocation()!!, l) }
			amenities(
				s.searchAmenities(
					SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER, null, r.top, r.left, r.bottom, r.right, -1, true,
					null, null, null, nearest, 5
				)
			)
		}
		'V' -> {
			val l = a!!.getLocation()!!
			filtered(s, extra.toInt(), a.getType()!!.getKeyName(), KMapUtils.calculateLatLonBbox(l.latitude, l.longitude, FILTERED_RADIUS))
		}
		'W' -> sorted(amenities(s.searchAmenitiesOnThePath(path(a!!.getLocation()!!), 100.0, SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER, null)))
		'M' -> {
			val list = if (file >= 0) fresh(readers, boxes, file) else readers.indices.flatMap { fresh(readers, boxes, it) }
			amenities(s.mergeAmenities(list, settings))
		}
		'Y' -> amenities(s.mergeAmenities(madeUp(), settings))
		'G' -> s.getAmenityRepositories(extra == "0", null).joinToString("") { it.getFile().name() + "\n" }
		'T' -> sorted(amenities(s.searchRoutePartOf(dumpUnhex(extra))))
		'U' -> s.searchRouteMembers(dumpUnhex(extra)).entries.sortedBy { it.key }.joinToString("") {
			it.key + "=" + (if (it.value == null) "null" else sorted(amenities(it.value))) + "\n"
		}
		'C', 'S' -> {
			val city = readers[file].getCities(null, CityBlocks.CITY_TOWN_TYPE)[i]
			readers[file].preloadStreets(city, null)
			details(s.searchDetailedObject(if (kind == 'C') city else city.getStreets()[0], settings))
		}
		else -> fail("a line of kind $kind")
	}

	companion object {
		private const val DIFFERENT = 0
		private const val IN_ULPS = 1
		private const val SHIFTED = 2

		/** A file the compat test searched: its own path, or one of OsmAnd-java. */
		internal fun path(path: String): String {
			if (path.startsWith("/")) {
				return path
			}
			return (RoutingTestFixtures.resourcesDir().parent!!.parent!!.parent!! / path).toString()
		}

		private fun fresh(readers: List<BinaryMapIndexReader>, boxes: List<IntArray>, f: Int): List<Amenity> {
			val b = boxes[f]
			return readers[f].searchPoi(SearchRequest.buildSearchPoiRequest(b[0], b[1], b[2], b[3], -1, null, null, null))
		}

		/** `AmenitySearcherCompatTest.javaByNames`. */
		internal fun byNames(a: Amenity, variant: Int): AmenitySearcher.Request {
			val l = KLatLon(a.getLocation()!!.latitude + 0.00018, a.getLocation()!!.longitude)
			return when (variant) {
				0 -> AmenitySearcher.Request(listOf(a.getName()), l, null, -1L, null)
				1 -> {
					val names = a.getNamesMap(true).values
					AmenitySearcher.Request(listOf(if (names.isEmpty()) a.getName() else names.first()), l, null, -1L, null)
				}
				2 -> {
					val subType = a.getSubType()
					val t = if (subType == null) null else MapPoiTypes.getDefault().getAnyPoiTypeByKey(subType)
					AmenitySearcher.Request(listOf(t?.getTranslation() ?: "-"), l, null, -1L, subType)
				}
				else -> AmenitySearcher.Request(listOf(a.getName()), l, a.getWikidata(), a.getOsmId(), null)
			}
		}

		/** `AmenitySearcherCompatTest.javaFiltered`. */
		private fun filtered(s: AmenitySearcher, variant: Int, category: String, r: KQuadRect): String {
			val all = SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER
			val empty = object : SearchPoiTypeFilter {
				override fun accept(type: PoiCategory?, subcategory: String): Boolean = false

				override fun isEmpty(): Boolean = true
			}
			val additional = object : SearchPoiAdditionalFilter {
				override fun accept(poiSubType: PoiSubType, value: String): Boolean = true

				override fun getName(): String? = null

				override fun getIconResource(): String? = null
			}
			var found = 0
			val third = object : ResultMatcher<Amenity> {
				override fun publish(obj: Amenity): Boolean = ++found <= 3

				override fun isCancelled(): Boolean = found >= 3
			}
			val emptyTakingAll = object : SearchPoiTypeFilter {
				override fun accept(type: PoiCategory?, subcategory: String): Boolean = true

				override fun isEmpty(): Boolean = true
			}
			val ofCategory = object : SearchPoiTypeFilter {
				override fun accept(type: PoiCategory?, subcategory: String): Boolean = type!!.getKeyName() == category

				override fun isEmpty(): Boolean = false
			}
			return ids(
				when (variant) {
					0 -> s.searchAmenities(ofCategory, null, r.top, r.left, r.bottom, r.right, FILTERED_ZOOM, true, null, null)
					1 -> s.searchAmenities(empty, additional, r.top, r.left, r.bottom, r.right, -1, true, null, null)
					2 -> s.searchAmenities(emptyTakingAll, null, r.top, r.left, r.bottom, r.right, -1, true, null, null)
					3 -> s.searchAmenities(
						all, null, r.top, r.left, r.bottom, r.right, -1, true, null, null,
						{ it.getFile().name().length % 2 == 0 }, null, -1
					)
					4 -> s.searchAmenities(all, null, r.top, r.left, r.bottom, r.right, -1, true, null, third)
					else -> s.searchWorldMapAmenities(all, r, true, null, null)
				}
			)
		}

		/** `AmenitySearcherCompatTest.mergesOfOneIdAndAnotherWikidataAreTheSame`. */
		private fun madeUp(): List<Amenity> {
			val made = arrayOf(
				longArrayOf(1, 1), longArrayOf(2, 2), longArrayOf(1, 2), longArrayOf(2, 0),
				longArrayOf(3, 2), longArrayOf(0, 1), longArrayOf(4, 3), longArrayOf(3, 4)
			)
			return made.mapIndexed { i, m ->
				Amenity().apply {
					setId(if (m[0] == 0L) -2L * (i + 1) else m[0] shl 1)
					setType(MapPoiTypes.getDefault().getOtherPoiCategory())
					setSubType("shop")
					setName("Shop $i")
					setLocation(52.5 + i / 1000.0, 13.4)
					if (m[1] != 0L) {
						setAdditionalInfo("wikidata", "Q${m[1]}")
					}
					setAdditionalInfo("opening_hours", "Mo-Fr $i:00-18:00")
				}
			}
		}

		private fun path(l: KLatLon): List<KLocation> =
			(-1..1).map { p -> KLocation("", l.latitude + p * 0.0018, l.longitude + p * 0.0018) }

		// `AmenitySearcherCompatTest`'s lines of the answers

		/** `AmenitySearcherCompatTest.FILTERED_RADIUS`. */
		private const val FILTERED_RADIUS = 300
		/** `AmenitySearcherCompatTest.FILTERED_ZOOM`. */
		private const val FILTERED_ZOOM = 15

		private fun ids(list: List<Amenity>): String = list.joinToString("") { "A" + it.getId() + ":" + location(it.getLocation()) + "\n" }

		private fun amenities(list: Collection<Amenity>?): String =
			list?.joinToString("") { amenity(it) + "\n" } ?: "null"

		private fun byFile(list: List<Amenity>): String {
			val s = StringBuilder()
			val file = ArrayList<String>()
			var region: String? = null
			for (a in list) {
				val r = a.getRegionName()
				if (file.isNotEmpty() && r != region) {
					file.sort()
					file.forEach { s.append(it) }
					file.clear()
				}
				region = r
				file.add(amenity(a) + "\n")
			}
			file.sort()
			file.forEach { s.append(it) }
			return s.toString()
		}

		private fun amenity(a: Amenity?): String {
			if (a == null) {
				return "null"
			}
			val info = a.getAdditionalInfoKeys().map { it to a.getAdditionalInfo(it) }.sortedBy { it.first }
			return "A" + a.getId() + ":" + (a.getType()?.getKeyName() ?: "-") + ":" + a.getSubType() + ":" + a.getName() + ":" +
					location(a.getLocation()) + ":" + a.getRegionName() + ":" + map(info) + ":" +
					map(a.getNamesMap(true).entries.map { it.key to it.value }.sortedBy { it.first }) + ":" + geometry(a)
		}

		private fun details(b: BaseDetailsObject?): String {
			if (b == null) {
				return "null"
			}
			val s = StringBuilder(if (b.isObjectFull()) "F" else if (b.isObjectCombined()) "C" else if (b.isObjectEmpty()) "E" else "?")
			s.append('|').append(b.getResourceType().name).append('|').append(b.getLang()).append('|')
				.append(b.getPointsLength()).append('|')
			for (o in b.getObjects()) {
				if (o is Amenity) {
					s.append(amenity(o))
				} else {
					val m = o as net.osmand.shared.data.MapObject
					s.append(o::class.simpleName).append(':').append(m.getId()).append(':').append(m.getName()).append(':')
						.append(location(m.getLocation()))
				}
				s.append('\n')
			}
			return s.append('|').append(amenity(b.getSyntheticAmenity())).toString()
		}

		/** java's `AbstractMap.toString` of a `TreeMap`. */
		private fun map(entries: List<Pair<String, String?>>): String = entries.joinToString(", ", "{", "}") { "${it.first}=${it.second}" }

		private fun geometry(a: Amenity): String {
			val xs = a.getX()
			val ys = a.getY()
			var sum = 0L
			for (p in 0 until xs.size()) {
				sum = sum * 31 + xs.get(p)
				sum = sum * 31 + ys.get(p)
			}
			return "${xs.size()}#$sum"
		}

		private fun location(l: KLatLon?): String =
			if (l == null) "null" else "@" + dumpBits(l.latitude) + ",@" + dumpBits(l.longitude)

		private fun sorted(lines: String): String = WorldRegion.splitLikeJava(lines, "\n").sorted().joinToString("\n")

		private val BITS = Regex("@([0-9a-f]+)")

		/**
		 * Whether two answers are equal but for doubles apart in their last digits; with [addresses],
		 * also for latitudes a unit of zoom 24 apart, which the reader loses for some settlements and
		 * streets on Kotlin/Native.
		 */
		private fun close(java: String, copy: String, addresses: Boolean): Int {
			val a = BITS.findAll(java).map { it.groupValues[1] }.toList()
			val b = BITS.findAll(copy).map { it.groupValues[1] }.toList()
			if (a.size != b.size || java.replace(BITS, "@") != copy.replace(BITS, "@")) {
				return DIFFERENT
			}
			var result = IN_ULPS
			for (k in a.indices) {
				val x = Double.fromBits(a[k].toULong(16).toLong())
				val y = Double.fromBits(b[k].toULong(16).toLong())
				if (x == y || abs(x - y) <= 1e-10 * maxOf(abs(x), abs(y))) {
					continue
				}
				val tiles = abs(KMapUtils.getTileNumberY(24.0, x) - KMapUtils.getTileNumberY(24.0, y))
				if (!addresses || k % 2 != 0 || tiles > 1.0 + 1e-6) {
					return DIFFERENT
				}
				result = SHIFTED
			}
			return result
		}

		/**
		 * Whether two lists of amenities are the same but for amenities on the edge of the box around
		 * [center] the search is made in: the platform works the edge out a last digit off, which may
		 * put it on the other side of a tile.
		 */
		private fun withoutTheEdge(java: String, copy: String, center: KLatLon, radius: Int): Boolean {
			val r = KMapUtils.calculateLatLonBbox(center.latitude, center.longitude, radius)
			val edges = intArrayOf(KMapUtils.get31TileNumberY(r.top), KMapUtils.get31TileNumberY(r.bottom))
			val sides = intArrayOf(KMapUtils.get31TileNumberX(r.left), KMapUtils.get31TileNumberX(r.right))
			val jl = java.split("\n")
			val kl = copy.split("\n")
			val jids = jl.map { it.substringBefore(':') }.toSet()
			val kids = kl.map { it.substringBefore(':') }.toSet()
			val edge = jl.filter { it.substringBefore(':') !in kids } + kl.filter { it.substringBefore(':') !in jids }
			val allOnTheEdge = edge.all { line ->
				val bits = BITS.findAll(line).map { Double.fromBits(it.groupValues[1].toULong(16).toLong()) }.toList()
				val y = KMapUtils.get31TileNumberY(bits[0])
				val x = KMapUtils.get31TileNumberX(bits[1])
				edges.any { abs(it - y) <= 1 } || sides.any { abs(it - x) <= 1 }
			}
			if (edge.isEmpty() || !allOnTheEdge) {
				return false
			}
			val rest = { l: List<String> -> l.filter { it !in edge }.joinToString("\n") }
			return close(rest(jl), rest(kl), false) != DIFFERENT
		}

		/**
		 * Whether two answers of a search thinned out to an amenity a tile of [zoom] differ only in
		 * amenities that are, or share a tile with, one on the edge of a tile: the reader works the tile
		 * out of coordinates the platform has off in their last digits, which can put that one in the
		 * tile on the other side of the edge, where it takes the place of another. One amenity at most
		 * for each on an edge.
		 */
		private fun thinnedOnATileEdge(java: String, copy: String, zoom: Int): Boolean {
			val jl = java.split("\n").filter { it.isNotEmpty() }
			val kl = copy.split("\n").filter { it.isNotEmpty() }
			val jids = jl.map { it.substringBefore(':') }.toSet()
			val kids = kl.map { it.substringBefore(':') }.toSet()
			val edge = jl.filter { it.substringBefore(':') !in kids } + kl.filter { it.substringBefore(':') !in jids }
			val tile = { line: String ->
				val bits = BITS.findAll(line).map { Double.fromBits(it.groupValues[1].toULong(16).toLong()) }.toList()
				KMapUtils.getTileNumberX(zoom.toDouble(), bits[1]) to KMapUtils.getTileNumberY(zoom.toDouble(), bits[0])
			}
			val onAnEdge = (jl + kl).map(tile).filter { (x, y) ->
				abs(x - round(x)) < 1e-6 || abs(y - round(y)) < 1e-6
			}
			if (edge.isEmpty() || onAnEdge.isEmpty()) {
				return false
			}
			val besideOne = edge.all { line ->
				val (x, y) = tile(line)
				onAnEdge.any { (ex, ey) ->
					val onY = abs(ey - round(ey)) < 1e-6
					val onX = abs(ex - round(ex)) < 1e-6
					onY && floor(x) == floor(ex) && floor(y) in listOf(round(ey) - 1, round(ey)) ||
							onX && floor(y) == floor(ey) && floor(x) in listOf(round(ex) - 1, round(ex))
				}
			}
			val rest = { l: List<String> -> l.filter { it !in edge }.joinToString("\n") }
			return besideOne && edge.size <= onAnEdge.size && close(rest(jl), rest(kl), false) != DIFFERENT
		}

		private fun outcome(c: () -> String): String = try {
			c()
		} catch (t: Throwable) {
			"threw:" + t::class.simpleName
		}
	}
}
