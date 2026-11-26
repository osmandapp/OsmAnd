package net.osmand.shared.search.core.spatial

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.double
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.data.KLatLon
import net.osmand.shared.map.OsmandRegions
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.math.max
import kotlin.math.min
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Checks the engine against recorded human preferences instead of recorded output: `SpatialSearchPreferencesTest` of
 * OsmAnd-java, wherever the copy runs.
 *
 * `preferences.jsonl` of the test resources of OsmAnd-java stores what a person said about two specific objects:
 * for query Q from position L, object B should come before object A; these N objects are one place and belong in one
 * row. Objects are addressed by OSM id. A record whose objects are no longer returned at all is NOT APPLICABLE, never
 * a failure; an order record whose preferred object is a row on screen while the other one is not returned counts as
 * satisfied.
 *
 * Maps are not in the repository: it looks in `OSMAND_MAPS_DIR`, `$HOME/osmand/maps` by default, with the names the
 * records give them. Without maps the test does nothing. `OSMAND_SPATIAL_SCORE_RANKING=false` runs the same
 * preferences against the old lexicographic ladder, measured, not gated.
 */
class SpatialSearchPreferencesTest {

	private class Pref {
		var id = ""
		var kind = ""
		var query = ""
		var map = ""
		var prefer: String? = null
		var verdict: String? = null
		var supersedes: String? = null
		var location: KLatLon? = null
		val objects = ArrayList<Long>()
		var a = 0L
		var b = 0L
		var asserted = true
	}

	private class Score {
		var satisfied = 0
		var violated = 0
		var notApplicable = 0
		var notAsserted = 0
		var absorbed = 0
		var belowFold = 0
		var noMap = 0
		val failures = ArrayList<String>()
	}

	/** search with the suggestion settings, as the app does before the last word is complete */
	private var typing = false

	/** the ids each row shows in its own right, parallel to the rows returned by search() */
	private val headIds = ArrayList<List<Long>>()

	private val rulesDir = SpatialSearchQueryRulesTest.rulesDir()

	@AfterTest
	fun deleteRules() {
		FileSystem.SYSTEM.deleteRecursively(rulesDir)
	}

	@Test
	fun recordedPreferences() {
		checkRecordedPreferences(false)
	}

	/**
	 * The same judgements while the last word is still being typed: the app searches with the suggestion settings until
	 * the word is finished, so this is the list a person sees first.
	 */
	@Test
	fun recordedPreferencesWhileTyping() {
		checkRecordedPreferences(true)
	}

	private fun checkRecordedPreferences(typing: Boolean) {
		this.typing = typing
		val prefs = readPreferences()
		assertFalse(prefs.isEmpty(), "preferences.jsonl is empty")

		val mapsDir = mapsDir()
		if (mapsDir == null) {
			println("SpatialSearchPreferencesTest: no maps directory - set OSMAND_MAPS_DIR=<dir with .obf files>")
			return
		}
		// a record replaced by a later one is history, not a statement to test against
		val superseded = prefs.mapNotNull { it.supersedes }.toSet()
		for (p in prefs) {
			if (p.id in superseded) {
				p.asserted = false
			}
		}
		val defaultTypes = runCatching { MapPoiTypes.getDefault() }.getOrNull()
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		MapPoiTypes.setDefault(types)
		val sc = try {
			check(prefs, mapsDir, types)
		} finally {
			defaultTypes?.let { MapPoiTypes.setDefault(it) }
		}

		println(
			"SpatialSearchPreferencesTest on ${testPlatformName()}, ranking: ${if (SCORE_RANKING) "score" else "ladder (old)"}, " +
					(if (typing) "while typing" else "complete words") + "; preferences: ${sc.satisfied} satisfied, " +
					"${sc.violated} violated, ${sc.notApplicable} not applicable (object not returned), ${sc.absorbed} " +
					"absorbed by deduplication, ${sc.belowFold} below the top $VISIBLE_ROWS, ${sc.notAsserted} not asserted, " +
					"${sc.noMap} without a map"
		)
		for (f in sc.failures) {
			println("  violated $f")
		}
		if (sc.satisfied + sc.violated + sc.notApplicable == 0 || !SCORE_RANKING) {
			// none of the maps named by preferences.jsonl are present, or the old ranking is measured, not gated
			return
		}
		// gate on a contradicted judgement, not on the satisfied count: that count moves with the maps
		assertEquals(0, sc.violated, "${sc.violated} recorded human judgements were contradicted, see the list above")
	}

	private fun check(prefs: List<Pref>, mapsDir: Path, types: MapPoiTypes): Score {
		val sc = Score()
		// one engine run per distinct (query, map, position), not per preference
		val byRun = LinkedHashMap<String, MutableList<Pref>>()
		for (p in prefs) {
			byRun.getOrPut(p.query + " " + p.map + " " + latLonString(p.location)) { ArrayList() }.add(p)
		}

		for (group in byRun.values) {
			val head = group[0]
			// a record may name several maps ("Netherlands_....obf|World_basemap_2.obf"): the world basemap is where a
			// far city lives, and a judgement about it needs both loaded
			val mapNames = head.map.split("|")
			if (!mapNames.all { isFile(mapsDir / it) }) {
				sc.noMap += group.size
				continue
			}
			val ordered = search(head.query, mapNames, mapsDir, head.location, types)
			for (p in group) {
				if (!p.asserted) {
					sc.notAsserted++
					continue
				}
				if (p.kind == "note") {
					// recall/matching observations: kept because they are the reason a case was dropped, asserted by
					// nothing
					sc.notAsserted++
					continue
				}
				if (p.kind == "merge") {
					val rows = HashSet<Int>()
					for (id in p.objects) {
						val row = indexOf(ordered, id)
						if (row >= 0) {
							rows.add(row)
						}
					}
					when (rows.size) {
						0 -> sc.notApplicable++
						1 -> sc.satisfied++
						else -> {
							sc.violated++
							sc.failures.add("${p.id} '${p.query}': ${rows.size} rows for one place")
						}
					}
					continue
				}
				val ia = indexOf(ordered, p.a)
				val ib = indexOf(ordered, p.b)
				if (ia < 0 || ib < 0) {
					val preferred = if (p.prefer == "a") p.a else p.b
					val ip = if (p.prefer == "a") ia else ib
					val io = if (p.prefer == "a") ib else ia
					if (ip in 0 until VISIBLE_ROWS && io < 0 && preferred in headIds[ip]) {
						// "B before A" holds when B is a row on screen and A is not shown at all
						sc.satisfied++
					} else {
						sc.notApplicable++
					}
					continue
				}
				if (ia == ib) {
					// deduplication united them: there is no order left to assert
					sc.notAsserted++
					continue
				}
				if (min(ia, ib) >= VISIBLE_ROWS) {
					// both rows are far below what anybody scrolls to
					sc.belowFold++
					continue
				}
				if (p.a !in headIds[ia] || p.b !in headIds[ib]) {
					// the object the judgement is about is no longer a row of its own - it was absorbed into one
					sc.absorbed++
					continue
				}
				val aFirst = ia < ib
				if (aFirst == (p.prefer == "a")) {
					sc.satisfied++
				} else {
					sc.violated++
					sc.failures.add("${p.id} '${p.query}': wanted ${p.prefer} first, it is #${max(ia, ib) + 1} of ${ordered.size}")
				}
			}
		}
		return sc
	}

	private fun indexOf(ordered: List<List<Long>>, osmId: Long): Int = ordered.indexOfFirst { osmId in it }

	/**
	 * The osm ids of every row, in the order the engine returned them. A row that deduplication united carries the ids
	 * of everything inside it.
	 */
	private fun search(query: String, mapNames: List<String>, mapsDir: Path, location: KLatLon?, types: MapPoiTypes): List<List<Long>> {
		val files = ArrayList<BinaryMapIndexReader>()
		val regions = mapsDir / OsmandRegions.REGIONS_OCBF
		if (isFile(regions)) {
			files.add(BinaryMapIndexReader(regions.toString()))
		}
		for (m in mapNames) {
			files.add(BinaryMapIndexReader((mapsDir / m).toString()))
		}
		try {
			val settings = if (typing) SpatialTextSearchSettings.suggestionSettings() else SpatialTextSearchSettings.defaultSettings()
			settings.SCORE_RANKING = SCORE_RANKING
			val ctx = SpatialSearchContext(settings, files, SpatialPoiSearch(types), location)
			val res = SpatialTextSearch(rulesDir.toString()).searchAPI(query, ctx)
			val out = ArrayList<List<Long>>()
			headIds.clear()
			for (r in res.mainResults.orEmpty()) {
				val ids = ArrayList<Long>()
				for (o in r.getObjects()) {
					ids.add(ObfConstants.getOsmObjectId(o))
				}
				headIds.add(ArrayList(ids))
				r.unitedObject?.let { united ->
					for (o in united.getObjects()) {
						if (o is net.osmand.shared.data.MapObject) {
							ids.add(ObfConstants.getOsmObjectId(o))
						}
					}
				}
				out.add(ids)
			}
			return out
		} finally {
			files.forEach { it.close() }
		}
	}

	// a map or a link to one, as java's File.isFile
	private fun isFile(path: Path): Boolean =
		FileSystem.SYSTEM.exists(path) && FileSystem.SYSTEM.metadataOrNull(path)?.isDirectory != true

	private fun mapsDir(): Path? {
		val dir = testEnvironment("OSMAND_MAPS_DIR")?.takeIf { it.isNotEmpty() }
			?: testEnvironment("HOME")?.let { "$it/osmand/maps" }
			?: return null
		val path = dir.toPath()
		return if (FileSystem.SYSTEM.metadataOrNull(path)?.isDirectory == true) path else null
	}

	private fun readPreferences(): List<Pref> {
		val out = ArrayList<Pref>()
		val file = RoutingTestFixtures.resourcesDir() / "spatial_search" / "preferences.jsonl"
		FileSystem.SYSTEM.source(file).buffer().use { source ->
			while (true) {
				val line = source.readUtf8Line()?.trim() ?: break
				if (line.isEmpty() || line.startsWith("#")) {
					continue
				}
				val o = Json.parseToJsonElement(line).jsonObject
				val p = Pref()
				p.id = o.string("id")!!
				p.kind = o.string("kind")!!
				p.query = o.string("query")!!
				p.map = o.string("map")!!
				val lat = o["lat"]
				val lon = o["lon"]
				if (lat != null && lat !is JsonNull && lon != null && lon !is JsonNull) {
					p.location = KLatLon(lat.jsonPrimitive.double, lon.jsonPrimitive.double)
				}
				// a record contradicted by another one is kept but asserted by neither side: it is the measured noise
				// of a human judge, not a statement to test against
				val conflicts = o["conflictsWith"]
				p.asserted = conflicts == null || conflicts is JsonNull || conflicts.jsonArray.isEmpty()
				p.supersedes = o.string("supersedes")
				if (p.kind == "note") {
					// a case the reviewer refused to answer, with the reason: asserted by nothing
					p.asserted = false
					out.add(p)
					continue
				}
				if (p.kind == "merge") {
					p.verdict = o.string("verdict")
					p.asserted = p.asserted && p.verdict == "one_row"
					for (e in o["objects"]!!.jsonArray) {
						p.objects.add(e.jsonObject["osmId"]!!.jsonPrimitive.long)
					}
				} else {
					p.prefer = o.string("prefer")
					p.asserted = p.asserted && p.prefer != "either"
					p.a = o["a"]!!.jsonObject["osmId"]!!.jsonPrimitive.long
					p.b = o["b"]!!.jsonObject["osmId"]!!.jsonPrimitive.long
				}
				out.add(p)
			}
		}
		return out
	}

	private fun JsonObject.string(key: String): String? {
		val v = this[key]
		return if (v == null || v is JsonNull) null else v.jsonPrimitive.content
	}

	// the position of a run, as java groups the runs by it
	private fun latLonString(l: KLatLon?): String = l?.toString() ?: "null"

	companion object {
		/**
		 * How far down the list a judgement still means something. "I am only interested in changes in the top 10" -
		 * the reviewer, 2026-09-09, looking at a preference whose two objects sit at #52 and #102.
		 */
		private const val VISIBLE_ROWS = 10

		private val SCORE_RANKING = !"false".equals(testEnvironment("OSMAND_SPATIAL_SCORE_RANKING"), ignoreCase = true)
	}
}
