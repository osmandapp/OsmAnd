package net.osmand.shared.routing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.osmand.shared.data.KLatLon
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.fail

/**
 * The routing test cases OsmAnd-java keeps - `test_routing.json`, `test_turn_lanes.json` and the
 * obf files they route over - read for the shared planner.
 *
 * The files are not copied: they are read from the OsmAnd-java source tree next to this module,
 * or from wherever `OSMAND_JAVA_TEST_RESOURCES` points when a test binary runs elsewhere, as a
 * Kotlin/Native one does. `routing.xml` likewise, from OsmAnd-java's resources or
 * `OSMAND_ROUTING_XML`. The expectations are the same json the java tests check against, so a
 * route the shared planner gets wrong fails here the way it would fail there.
 */
object RoutingTestFixtures {

	/** One case: where to route, with what, and what the route must and must not contain. */
	class Entry(
		val testName: String,
		val params: Map<String, String>,
		val startPoint: KLatLon,
		val endPoint: KLatLon,
		val transitPoints: List<KLatLon>,
		val ignore: Boolean,
		val expectedResults: Map<String, String>?,
		val expectedExits: Map<String, String>?
	)

	private val json = Json { ignoreUnknownKeys = true; isLenient = true }

	fun resourcesDir(): Path {
		val dir = testEnvironment("OSMAND_JAVA_TEST_RESOURCES") ?: "../OsmAnd-java/src/test/resources"
		val path = dir.toPath()
		if (!FileSystem.SYSTEM.exists(path)) {
			fail("OsmAnd-java test resources not found at $path; set OSMAND_JAVA_TEST_RESOURCES")
		}
		return path
	}

	fun resource(name: String): String = (resourcesDir() / name).toString()

	fun routingXml(): String {
		val file = testEnvironment("OSMAND_ROUTING_XML") ?: "../OsmAnd-java/src/main/resources/net/osmand/router/routing.xml"
		if (!FileSystem.SYSTEM.exists(file.toPath())) {
			fail("routing.xml not found at $file; set OSMAND_ROUTING_XML")
		}
		return file
	}

	private var builder: RoutingConfiguration.Builder? = null

	/** The default routing profiles, parsed once per test binary. */
	fun defaultBuilder(): RoutingConfiguration.Builder {
		var b = builder
		if (b == null) {
			b = RoutingConfiguration.parseFromFile(routingXml())
			builder = b
		}
		return b
	}

	/** The cases of a json file, with the ignored ones left out. */
	fun entries(fileName: String): List<Entry> {
		val text = FileSystem.SYSTEM.read(resourcesDir() / fileName) { readUtf8() }
		val entries = ArrayList<Entry>()
		for (element in json.parseToJsonElement(text).jsonArray) {
			val o = element.jsonObject
			val entry = Entry(
				testName = o.string("testName") ?: "",
				params = o.strings("params"),
				startPoint = o.point("startPoint")!!,
				endPoint = o.point("endPoint")!!,
				transitPoints = listOfNotNull(o.point("transitPoint1"), o.point("transitPoint2"), o.point("transitPoint3")),
				ignore = o.string("ignore") == "true",
				expectedResults = if (o.containsKey("expectedResults")) o.strings("expectedResults") else null,
				expectedExits = if (o.containsKey("expectedExits")) o.strings("expectedExits") else null
			)
			if (!entry.ignore) {
				entries.add(entry)
			}
		}
		return entries
	}

	/** Gson gave the java tests every scalar as a string; so does this. */
	private fun JsonObject.string(key: String): String? {
		val v = this[key] ?: return null
		if (v is JsonNull) {
			return null
		}
		return (v as JsonPrimitive).content
	}

	private fun JsonObject.strings(key: String): Map<String, String> {
		val v = this[key] ?: return emptyMap()
		if (v is JsonNull) {
			return emptyMap()
		}
		val map = LinkedHashMap<String, String>()
		for ((k, e) in v.jsonObject) {
			// a null value stays out, as a java Map from Gson would answer null for it
			if (e !is JsonNull) {
				map[k] = (e as JsonPrimitive).content
			}
		}
		return map
	}

	private fun JsonObject.point(key: String): KLatLon? {
		val v = this[key] ?: return null
		if (v is JsonNull) {
			return null
		}
		val o = v.jsonObject
		return KLatLon(o.string("latitude")!!.toDouble(), o.string("longitude")!!.toDouble())
	}

	// ---- what the java tests compute with ObfConstants and RouterUtilTest ----

	private const val SHIFT_ID = 6
	private const val DUPLICATE_SPLIT = 5
	private const val RELATION_BIT = 1L shl 42
	private const val SPLIT_BIT = 1L shl 40
	private const val ROAD_INFO_DELIMITER = ":"

	/** `ObfConstants.getOsmObjectId(RouteDataObject)`: the osm way id a road id encodes. */
	fun osmObjectId(road: RouteDataObject?): Long {
		if (road == null) {
			return 0
		}
		var id = road.getId()
		val shifted = id > 0 && ((id and RELATION_BIT) == RELATION_BIT || (id and SPLIT_BIT) == SPLIT_BIT)
		if (shifted) {
			id = (id and (RELATION_BIT or SPLIT_BIT).inv()) shr DUPLICATE_SPLIT
		}
		return id shr SHIFT_ID
	}

	/** `RouterUtilTest.getRoadId`: the id in a "id" or "id:point" key. */
	fun roadId(roadInfo: String): Long {
		if (roadInfo.contains(ROAD_INFO_DELIMITER)) {
			return roadInfo.split(ROAD_INFO_DELIMITER)[0].toLong()
		}
		return roadInfo.toLong()
	}

	/** `RouterUtilTest.getRoadStartPoint`: the point in a "id:point" key, -1 without one. */
	fun roadStartPoint(roadInfo: String): Int {
		if (roadInfo.contains(ROAD_INFO_DELIMITER)) {
			return roadInfo.split(ROAD_INFO_DELIMITER)[1].toInt()
		}
		return -1
	}

	fun memoryLimits(): RoutingConfiguration.RoutingMemoryLimits = RoutingConfiguration.RoutingMemoryLimits(
		RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3, RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT
	)
}
