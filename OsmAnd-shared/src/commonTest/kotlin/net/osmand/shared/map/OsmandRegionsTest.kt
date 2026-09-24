package net.osmand.shared.map

import net.osmand.shared.data.KLatLon
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.util.primaryCollator
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The regions of `regions.ocbf` read by the shared copy wherever it runs, on Kotlin/Native as well
 * as the jvm.
 *
 * The file is the one OsmAnd-java downloads into its resources, or wherever `OSMAND_REGIONS_OCBF`
 * points. It is rebuilt from time to time, so the checks here are the ones `OsmandRegionsTest` in
 * OsmAnd-java makes, which hold for any version of it.
 *
 * [sameAsJava] goes further: it builds the text `OsmandRegionsCompatTest` in OsmAnd-java writes
 * from the java classes, and compares the two. That text is at
 * `../OsmAnd-java/build/regions-java.txt` after a run of the compat test, or wherever
 * `OSMAND_REGIONS_JAVA_DUMP` points; without it that check says so and passes. On the jvm the two
 * are the same text. Kotlin/Native differs from java in two places, which the check allows for and
 * nothing else: the centre of a region comes out of its libm a few ulps apart, and the subregions
 * are sorted by the collator of the platform, which on iOS does not skip spaces as java's does, so
 * "North West" comes before "Northern Cape" there.
 */
class OsmandRegionsTest {

	@Test
	fun regionSearchMatching() {
		val pennsylvania = region("us_pennsylvania_northamerica")
		assertFalse(matches("1", pennsylvania)) // 1-char queries matched all regions
		assertTrue(matches("PA", pennsylvania)) // ref
		assertFalse(matches("55", region("france_great-east_meuse_europe"))) // ref
		assertFalse(matches("5", region("philippines_bicol-region_asia"))) // alt_name
		assertFalse(matches("7", region("philippines_central-visayas_asia"))) // alt_name
		assertFalse(matches("1", region("indonesia_bangka-belitung_asia"))) // ref
		assertFalse(matches("4", region("philippines_calabarzon_asia"))) // alt_name
		assertFalse(matches("22", region("indonesia_bali_asia"))) // ref
		assertFalse(matches("33", region("indonesia_nusa-tenggara-barat_asia"))) // ref
		assertFalse(matches("44", region("indonesia_irian-jaya-barat_asia"))) // ref
	}

	@Test
	fun removeDuplicates() {
		val cz = listOf(
			"czech-republic_jihovychod_europe",
			"czech-republic_jihozapad_europe",
			"czech-republic_moravskoslezsko_europe",
			"czech-republic_praha_europe",
			"czech-republic_severovychod_europe",
			"czech-republic_severozapad_europe",
			"czech-republic_stredni-cechy_europe",
			"czech-republic_stredni-morava_europe"
		)
		val deduplicated = WorldRegion.removeDuplicates(cz.map { region(it) }).map { it.getRegionDownloadName() }.toSet()
		assertEquals(cz.size, deduplicated.size)
		assertTrue(deduplicated.contains("czech-republic_praha_europe"))
	}

	@Test
	fun countryRegion() {
		// a country is the region right under a continent
		assertEquals("europe_germany", countryId("germany_berlin_europe"))
		assertEquals("europe_germany", countryId("germany_europe"))
		assertEquals("europe_netherlands", countryId("netherlands_noord-holland_europe"))
		assertEquals("europe_ukraine", countryId("ukraine_kyiv-city_europe"))
		assertEquals("northamerica_us", countryId("us_new-hampshire_northamerica"))

		// a country that is placed next to the continents instead of under one
		assertEquals("russia", countryId("russia_moscow_asia"))
		assertEquals("russia", countryId("russia_north-caucasus-federal-district_asia"))

		// continents and the world itself are not countries
		assertNull(regions.getRegionData("europe")!!.getCountryRegion())
		assertNull(regions.getWorldRegion().getCountryRegion())
	}

	@Test
	fun regionsToDownload() {
		// Prague not part of Central Bohemia
		includedExcluded(50.087463, 14.421259, "czech-republic_praha_europe", "czech-republic_stredni-cechy_europe")
		// ACT not part of New South Wales
		includedExcluded(
			-35.308056, 149.124444,
			"australia-oceania_australian-capital-territory_australia-oceania",
			"australia-oceania_new-south-wales_australia-oceania"
		)
		// Additional subregion query must return the main region name (Lienz, Tirol, Austria)
		includedExcluded(46.82987, 12.76812, "austria_tyrol_europe", "")
	}

	/** The continents hang under the world, sorted by name through the collator of the platform. */
	@Test
	fun continents() {
		assertEquals(
			listOf(
				"africa", "antarctica", "asia", "australia-oceania-all", "centralamerica", "europe",
				"northamerica", "russia", "southamerica"
			),
			regions.getWorldRegion().getSubregions().map { it.getRegionId() }
		)
	}

	@Test
	fun sameAsJava() {
		val path = testEnvironment("OSMAND_REGIONS_JAVA_DUMP") ?: "../OsmAnd-java/build/regions-java.txt"
		if (!FileSystem.SYSTEM.exists(path.toPath())) {
			println("OsmandRegionsTest: no java dump at $path, run OsmandRegionsCompatTest in OsmAnd-java first")
			return
		}
		val text = dump()
		testEnvironment("OSMAND_REGIONS_COPY_DUMP")?.let { out -> FileSystem.SYSTEM.write(out.toPath()) { writeUtf8(text) } }
		val java = Dump(FileSystem.SYSTEM.read(path.toPath()) { readUtf8() })
		val copy = Dump(text)

		assertEquals(java.regions.keys, copy.regions.keys, "regions")
		var centres = 0
		for ((id, j) in java.regions) {
			val k = copy.regions.getValue(id)
			val centre = j.size - 7
			assertEquals(j.filterIndexed { i, _ -> i != centre }, k.filterIndexed { i, _ -> i != centre }, id)
			if (j[centre] != k[centre]) {
				val jc = j[centre].split(",")
				val kc = k[centre].split(",")
				for (i in jc.indices) {
					val ulps = abs(jc[i].toULong(16).toLong() - kc[i].toULong(16).toLong())
					assertTrue(ulps <= CENTRE_ULPS, "$id centre ${j[centre]} ${k[centre]}")
				}
				centres++
			}
		}

		var moved = 0
		val collator = primaryCollator()
		for ((parent, children) in java.children) {
			val resorted = children.sortedWith { a, b ->
				collator.compare(java.regions.getValue(a)[2], java.regions.getValue(b)[2])
			}
			assertEquals<List<String>?>(resorted, copy.children[parent], "subregions of $parent")
			moved += children.indices.count { children[it] != resorted[it] }
		}

		assertEquals(java.points.keys, copy.points.keys, "points")
		for ((point, answers) in java.points) {
			assertEquals(answers.sorted(), copy.points.getValue(point).sorted(), "at $point")
		}
		println(
			"OsmandRegionsTest: ${java.regions.size} regions and ${java.points.size} points the same as java; " +
					"$centres centres off by up to $CENTRE_ULPS ulps, $moved subregions in another place"
		)
	}

	/**
	 * The text [dump] writes, read back: the regions by id, the ids of the subregions of each region
	 * in their order, and the answers at each point, the point rounded to 1e-7 degree so that a
	 * centre a few ulps off still finds its twin.
	 */
	private class Dump(text: String) {
		val regions = LinkedHashMap<String, List<String>>()
		val children = LinkedHashMap<String, MutableList<String>>()
		val points = LinkedHashMap<String, MutableList<String>>()

		init {
			val path = ArrayList<String>()
			for (line in text.split("\n")) {
				if (line.startsWith("R ")) {
					val fields = line.substring(2).split("|")
					val id = fields[0]
					val level = fields[fields.size - 4].toInt()
					while (path.size > level) {
						path.removeAt(path.size - 1)
					}
					if (path.isNotEmpty()) {
						children.getOrPut(path.last()) { ArrayList() }.add(id)
					}
					path.add(id)
					regions[id] = fields
				} else if (line.startsWith("P ")) {
					val fields = line.substring(2).split("|")
					val ll = fields[0].split(",").map { Double.fromBits(it.toULong(16).toLong()) }
					val key = ll.joinToString(",") { (kotlin.math.round(it * 1e7) / 1e7).toString() }
					points.getOrPut(key) { ArrayList() }.add(fields.drop(1).joinToString("|"))
				}
			}
		}
	}

	/** The same text as `OsmandRegionsCompatTest.javaDump` in OsmAnd-java. */
	private fun dump(): String {
		val sb = StringBuilder()
		val tree = ArrayList<WorldRegion>()
		flatten(regions.getWorldRegion(), tree)
		for (r in tree) {
			val c = r.getRegionCenter()
			val center = if (c == null) "~" else bits(c.latitude) + "," + bits(c.longitude)
			val b = r.getBoundingBox()
			val box = if (b == null) "~" else bits(b.left) + "," + bits(b.top) + "," + bits(b.right) + "," + bits(b.bottom)
			var hash = FNV_OFFSET
			for (polygon in r.getPolygons()) {
				hash = (hash xor polygon.size.toLong()) * FNV_PRIME
				for (f in polygon) {
					hash = (hash xor (f.toRawBits().toLong() and 0xffffffffL)) * FNV_PRIME
				}
			}
			val p = r.getParams()
			sb.append("R ").append(r.getRegionId()).append('|').append(or(r.getRegionDownloadName()))
				.append('|').append(r.getLocaleName()).append('|').append(or(r.regionName))
				.append('|').append(or(r.regionNameEn)).append('|').append(or(r.regionNameLocale))
				.append('|').append(or(r.regionParentFullName)).append('|').append(or(r.getRegionSearchText()))
				.append('|').append(flag(r.isRegionMapDownload())).append(flag(r.isRegionRoadsDownload()))
				.append(flag(r.isRegionJoinMapDownload())).append(flag(r.isRegionJoinRoadsDownload()))
				.append('|').append(or(p.getRegionLang())).append('|').append(or(p.getRegionLeftHandDriving()))
				.append('|').append(or(p.getRegionMetric())).append('|').append(or(p.getRegionRoadSigns()))
				.append('|').append(or(p.getWikiLink())).append('|').append(or(p.getPopulation()))
				.append('|').append(center).append('|').append(box).append('|').append(hash.toULong().toString(16))
				.append('|').append(r.getLevel()).append('|').append(flag(r.isContinent()))
				.append('|').append(or(r.getCountryRegion()?.getRegionId())).append('|').append(r.getSubregions().size)
				.append('\n')
		}
		val points = ArrayList<KLatLon>()
		var lat = -78
		while (lat <= 78) {
			var lon = -180
			while (lon < 180) {
				points.add(KLatLon(lat.toDouble(), lon.toDouble()))
				lon += 6
			}
			lat += 6
		}
		tree.mapNotNullTo(points) { it.getRegionCenter() }
		for (ll in points) {
			val smallest = regions.getSmallestBinaryMapDataObjectAt(ll)
			val names = regions.getRegionsToDownload(ll.latitude, ll.longitude, ArrayList())
			sb.append("P ").append(bits(ll.latitude)).append(',').append(bits(ll.longitude))
				.append('|').append(smallest?.key?.getRegionId() ?: "~")
				.append('|').append(names.joinToString(","))
				.append('|').append(or(regions.getCountryName(ll))).append('\n')
		}
		return sb.toString()
	}

	private fun flatten(r: WorldRegion, result: MutableList<WorldRegion>) {
		result.add(r)
		for (s in r.getSubregions()) {
			flatten(s, result)
		}
	}

	private fun bits(d: Double): String = d.toRawBits().toULong().toString(16)

	private fun or(s: String?): String = s ?: "~"

	private fun flag(b: Boolean): String = if (b) "1" else "0"

	private fun matches(query: String, region: WorldRegion): Boolean =
		OsmandRegions.isRegionNameMatched(query, region.getRegionSearchText())

	private fun region(downloadName: String): WorldRegion =
		assertNotNull(regions.getRegionDataByDownloadName(downloadName), "Unknown region $downloadName")

	private fun countryId(downloadName: String): String? = region(downloadName).getCountryRegion()?.getRegionId()

	private fun includedExcluded(lat: Double, lon: Double, included: String, excluded: String) {
		val downloadNames = regions.getRegionsToDownload(lat, lon).map { regions.getDownloadName(it) }.toSet()
		assertTrue(downloadNames.contains(included), "$included at $lat $lon: $downloadNames")
		assertFalse(downloadNames.contains(excluded), "$excluded at $lat $lon: $downloadNames")
	}

	companion object {
		private val FNV_OFFSET = 0xcbf29ce484222325UL.toLong()
		private const val FNV_PRIME = 0x100000001b3L

		/** How far Kotlin/Native's libm may put a centre from java's: 311 of 1723 are 1 to 3 ulps off. */
		private const val CENTRE_ULPS = 4L

		private val regions: OsmandRegions by lazy {
			val path = testEnvironment("OSMAND_REGIONS_OCBF") ?: "../OsmAnd-java/src/main/resources/net/osmand/map/regions.ocbf"
			if (!FileSystem.SYSTEM.exists(path.toPath())) {
				fail("regions.ocbf not found at $path; :OsmAnd-java:processResources downloads it, or set OSMAND_REGIONS_OCBF")
			}
			OsmandRegions(path)
		}
	}
}
