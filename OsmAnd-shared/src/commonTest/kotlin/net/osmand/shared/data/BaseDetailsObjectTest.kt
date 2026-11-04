package net.osmand.shared.data

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.CityBlocks
import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.util.dumpBits
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.fail

/**
 * The copies of `BaseDetailsObject` and `RenderedObject` against what java made of the same objects,
 * on the jvm and on Kotlin/Native: the lines `BaseDetailsObjectCompatTest` in OsmAnd-java writes, each
 * with how to make the objects again out of the amenities of the obf files of the search tests.
 *
 * Doubles are compared up to a ten billionth: the reader works coordinates out from the tiles
 * through the platform's sines and cosines. The box of a street is worked out from the places of its
 * houses and so may be a unit of the 31 tiles off, or a unit of zoom 24 where the reader put the
 * street a unit north, as `SearchCoreFactoryTest` explains.
 */
class BaseDetailsObjectTest {

	@Test
	fun sameAsJava() {
		val source = openJavaDump(
			"BaseDetailsObjectTest", "OSMAND_DETAILS_OBJECTS_JAVA_DUMP", "details-objects-java.txt",
			"BaseDetailsObjectCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		val defaultTypes = MapPoiTypes.getDefault()
		val phrases = HashMap<String, String>()
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		MapPoiTypes.setDefault(types)
		val files = Files()
		val counts = LinkedHashMap<String, Int>()
		var inUlps = 0
		var shifted = 0
		val failures = ArrayList<String>()
		fun check(what: String, java: String, copy: String) {
			if (java == copy) {
				return
			}
			when (close(java, copy, what.startsWith("A\t"))) {
				IN_ULPS -> {
					inUlps++
					return
				}
				SHIFTED -> {
					shifted++
					return
				}
			}
			if (failures.size < 10) {
				failures.add("$what\n  java: $java\n  copy: $copy")
			}
		}
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				val kind = f[0]
				if (kind == "X") {
					phrases[dumpUnhex(f[1])] = dumpUnhex(f[2])
					continue
				}
				if (types.getPoiTranslator() == null) {
					types.setPoiTranslator(SearchCoreFactoryTest.Translator(phrases))
				}
				when (kind) {
					"S" -> {
						val a = files.amenities(f[1])[f[2].toInt()]
						check(line, f[3], esc(describe(BaseDetailsObject(a, "en"), listOf(a))))
					}
					"U" -> {
						val group = f[1].split(",").map { m ->
							val (file, i) = m.split(":")
							files.amenities(file)[i.toInt()]
						}
						checkGroup(line, group, null, "en", f[2], f[3], f[4], ::check)
					}
					"N" -> {
						val all = files.amenities(f[1])
						val start = f[2].toInt()
						val region = f[4]
						val group = (0 until f[3].toInt()).map { n ->
							val a = copyOf(all[start + n])
							if (region == "other") {
								if (n == 0) {
									a.setType(MapPoiTypes.getDefault().getOtherPoiCategory())
								}
							} else if (region == "icons") {
								if (n == 0) {
									a.setRegionName(null)
								} else if (n == 1) {
									a.setMapIconName("tourism_museum")
									a.setAdditionalInfo(Amenity.TRAVEL_ELO, "1234")
								}
							} else if (region != "null") {
								a.setRegionName(region)
								a.setAdditionalInfo(Amenity.LANG_YES + ":" + (if (n % 2 == 0) "de" else "en"), "yes")
							}
							a
						}
						val resource = if (region == "null" || region == "other" || region == "icons") null else region
						checkGroup(line, group, resource, "de", f[5], f[6], f[7], ::check)
					}
					"R" -> {
						val a = files.amenities(f[1])[f[2].toInt()]
						val d = drawn(f[3])
						check("$line drawn", f[4], esc(describe(d)))
						check("$line as amenity", f[5], esc(describe(BaseDetailsObject.convertRenderedObjectToAmenity(d, MapPoiTypes.getDefault()))))
						check("$line alone", f[6], esc(describe(BaseDetailsObject(d, "en"), listOf(d))))
						val b = BaseDetailsObject(a, "en")
						val other = BaseDetailsObject(d, "en")
						val overlaps = "${b.overlapsWith(d)},${b.overlapsWith(a)},${b.overlapsWith(other)}"
						b.addObject(d)
						check("$line with the amenity", f[7], esc(describe(b, listOf(a, d))))
						check("$line overlaps", f[8], overlaps)
						val merged = BaseDetailsObject(a, "de")
						merged.merge(d)
						merged.merge(other)
						check("$line merged", f[9], "${order(merged.getObjects(), listOf(a, d))},${merged.overlapsWith(other)}")
					}
					"A" -> {
						val a = files.amenities(f[1])[0]
						val city = files.city(f[1], f[2].toInt())
						val list = ArrayList<MapObject>(listOf(city, a))
						val street = city.getStreets().firstOrNull()
						if (street != null) {
							check("$line street", f[3], esc(describe(BaseDetailsObject(listOf(street, a), "en"), listOf(street, a))))
							list.add(0, street)
							val house = street.getBuildings().firstOrNull()
							if (house != null) {
								check("$line house", f[4], esc(describe(BaseDetailsObject(house, "en"), listOf(house))))
								list.add(house)
							} else {
								check("$line house", f[4], "-")
							}
						} else {
							check("$line street", f[3] + f[4], "--")
						}
						check("$line all", f[5], esc(describe(BaseDetailsObject(list, "en"), list)))
						check("$line alone", f[6], esc(describe(BaseDetailsObject(listOf(city), "de"), listOf(city))))
					}
					else -> fail("unknown line $line")
				}
				counts[kind] = (counts[kind] ?: 0) + 1
			}
		} finally {
			source.close()
			files.close()
			MapPoiTypes.setDefault(defaultTypes)
		}
		println("BaseDetailsObjectTest: the same as java on ${testPlatformName()}: $counts; $inUlps with doubles apart in their last digits, $shifted with the box of a street off")
		if (failures.isNotEmpty()) {
			fail(failures.joinToString("\n"))
		}
		if ((counts["U"] ?: 0) < 1000 || (counts["R"] ?: 0) < 1000) {
			fail("too little compared: $counts")
		}
	}

	private fun checkGroup(
		line: String, group: List<Amenity>, resource: String?, lang: String,
		added: String, listed: String, nested: String, check: (String, String, String) -> Unit
	) {
		val b = BaseDetailsObject(group[0], lang)
		if (resource != null) {
			b.setObfResourceName(resource)
		}
		for (i in 1 until group.size) {
			b.addObject(group[i])
		}
		check("$line added", added, esc(describe(b, group)))
		check("$line listed", listed, esc(describe(BaseDetailsObject(group, lang), group)))
		check("$line nested", nested, esc(describe(BaseDetailsObject(b, lang), group)))
	}

	/** The amenities of each file and its settlements with their streets, read once. */
	internal class Files {
		private val dir = RoutingTestFixtures.resourcesDir().parent!!.parent!!.parent!! / "build" / "search-obf"
		private val readers = HashMap<String, BinaryMapIndexReader>()
		private val amenities = HashMap<String, List<Amenity>>()
		private val cities = HashMap<String, List<City>>()
		private val preloaded = HashSet<String>()

		private fun reader(file: String): BinaryMapIndexReader =
			readers.getOrPut(file) { BinaryMapIndexReader((dir / dumpUnhex(file)).toString()) }

		fun amenities(file: String): List<Amenity> = amenities.getOrPut(file) {
			reader(file).searchPoi(SearchRequest.buildSearchPoiRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE, -1, null, null, null))
		}

		fun city(file: String, index: Int): City {
			val city = cities.getOrPut(file) { reader(file).getCities(null, CityBlocks.CITY_TOWN_TYPE) }[index]
			if (preloaded.add("$file:$index")) {
				reader(file).preloadStreets(city, null)
				city.getStreets().firstOrNull()?.let { reader(file).preloadBuildings(it, null) }
			}
			return city
		}

		fun close() {
			readers.values.forEach { it.close() }
		}
	}

	/** An amenity to change the region and the tags of, as `BaseDetailsObjectCompatTest` makes one. */
	private fun copyOf(a: Amenity): Amenity {
		val c = Amenity()
		c.setId(a.getId())
		c.setType(a.getType())
		c.setSubType(a.getSubType())
		c.setLocation(a.getLocation())
		c.setRegionName(a.getRegionName())
		c.copyNames(a)
		c.copyAdditionalInfo(a, false)
		c.setTagGroups(a.getTagGroups()?.let { HashMap(it) })
		c.setX(a.getX())
		c.setY(a.getY())
		return c
	}

	companion object {

		/** The drawn object `BaseDetailsObjectCompatTest` wrote. */
		internal fun drawn(spec: String): RenderedObject {
			val f = spec.split(",")
			val d = RenderedObject()
			if (f[3].isNotEmpty()) {
				for (tag in f[3].split(";")) {
					val (k, v) = tag.split("=")
					d.putTag(dumpUnhex(k), dumpUnhex(v))
				}
			}
			d.setName(dumpUnhex(f[1]))
			d.setId(if (f[0] == "-") null else f[0].toLong())
			if (f[2] != "-") {
				d.setLocation(latLon(f[2]))
			}
			if (f[4].isNotEmpty()) {
				val xy = f[4].split(" ").map { it.toInt() }
				for (p in xy.indices step 2) {
					d.addLocation(xy[p], xy[p + 1])
				}
			}
			d.setLabelX(f[5].toInt())
			d.setLabelY(f[6].toInt())
			if (f[7] != "-") {
				d.setLabelLatLon(latLon(f[7]))
			}
			return d
		}

		private fun latLon(bits: String): KLatLon {
			val (lat, lon) = bits.split(" ")
			return KLatLon(Double.fromBits(lat.toULong(16).toLong()), Double.fromBits(lon.toULong(16).toLong()))
		}

		fun describe(b: BaseDetailsObject, input: List<Any?>): String =
			(if (b.isObjectFull()) "F" else if (b.isObjectCombined()) "C" else if (b.isObjectEmpty()) "E" else "?") +
					"|" + b.getResourceType().name + "|" + b.getLang() + "|" + BaseDetailsObject.getLangForTravel(b) +
					"|" + b.hasGeometry() + "|" + b.getPointsLength() + "|" + b.getAmenities().size +
					"|" + b.getRenderedObjects().size + "|" + index(input, b.getAddressObject()) +
					"|" + order(b.getObjects(), input) + "|" + b + "|" + location(b.getLocation()) +
					"|" + describe(b.getSyntheticAmenity())

		fun describe(a: Amenity): String {
			val s = StringBuilder()
			s.append(a.getId()).append('|').append(a.getType()?.getKeyName())
				.append('|').append(a.getSubType()).append('|').append(a.getName()).append('|').append(a.getEnName(false))
				.append('|').append(a.getNamesMap(true)).append('|').append(location(a.getLocation())).append('|')
			for (key in a.getAdditionalInfoKeys()) {
				s.append(key).append('=').append(a.getAdditionalInfo(key)).append(';')
			}
			val groups = a.getTagGroups()
			val tagGroups = if (groups == null) "{}" else groups.keys.sorted().joinToString(", ", "{", "}") { key ->
				"$key=" + groups.getValue(key).joinToString("") { it.tag + "=" + it.value + ";" }
			}
			s.append('|').append(tagGroups).append('|').append(a.getMapIconName()).append('|').append(a.getRegionName())
				.append('|').append(a.getTravelEloNumber()).append('|').append(a.getX().toArray().contentToString())
				.append('|').append(a.getY().toArray().contentToString()).append('|')
				.append(a.getSupportedContentLocales().sorted()).append('|')
				.append(a.getBbox31()?.joinToString(",") { "#$it" })
				.append('|').append(a)
			return s.toString()
		}

		fun describe(d: RenderedObject): String {
			val r = d.getRectLatLon()
			val polygon = StringBuilder()
			for (l in d.getPolygon()) {
				polygon.append(location(l)).append(';')
			}
			return "$d|${d.toStringEn()}|${ObfConstants.getPrintTags(d)}|${d.getOriginalNames()}|${d.getRouteID()}" +
					"|${d.isText()}|${d.isSimplePoint()}|${location(d.getLatLon())}|" +
					(if (r == null) "null" else "@${dumpBits(r.left)},@${dumpBits(r.top)},@${dumpBits(r.right)},@${dumpBits(r.bottom)}") +
					"|" + polygon
		}

		fun order(objects: List<Any>, input: List<Any?>): List<Int> = objects.map { index(input, it) }

		private fun index(list: List<Any?>, o: Any?): Int {
			for (i in list.indices) {
				if (list[i] === o) {
					return i
				}
			}
			return if (o == null) -1 else -2
		}

		private fun location(l: KLatLon?): String =
			if (l == null) "null" else "@" + dumpBits(l.latitude) + ",@" + dumpBits(l.longitude)

		fun esc(s: String): String = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

		private const val DIFFERENT = 0
		private const val IN_ULPS = 1
		private const val SHIFTED = 2

		private val BITS = Regex("@([0-9a-f]+)")
		private val TILES = Regex("#(-?[0-9]+)")

		/** Whether two lines are equal but for doubles apart in their last digits, or boxes of streets. */
		private fun close(java: String, copy: String, streets: Boolean): Int {
			val a = BITS.findAll(java).map { it.groupValues[1] }.toList()
			val b = BITS.findAll(copy).map { it.groupValues[1] }.toList()
			val ta = TILES.findAll(java).map { it.groupValues[1].toInt() }.toList()
			val tb = TILES.findAll(copy).map { it.groupValues[1].toInt() }.toList()
			if (a.size != b.size || ta.size != tb.size
				|| java.replace(BITS, "@").replace(TILES, "#") != copy.replace(BITS, "@").replace(TILES, "#")
			) {
				return DIFFERENT
			}
			val inUlps = a.indices.all {
				val x = Double.fromBits(a[it].toULong(16).toLong())
				val y = Double.fromBits(b[it].toULong(16).toLong())
				x == y || abs(x - y) <= 1e-10 * maxOf(abs(x), abs(y))
			}
			if (!inUlps) {
				return DIFFERENT
			}
			if (ta == tb) {
				return IN_ULPS
			}
			// a unit of zoom 24 is 128 units of the 31 tiles
			return if (streets && ta.indices.all { abs(ta[it] - tb[it]) <= 128 }) SHIFTED else DIFFERENT
		}
	}
}
