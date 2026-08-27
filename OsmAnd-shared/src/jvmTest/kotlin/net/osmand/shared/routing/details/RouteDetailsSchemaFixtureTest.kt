package net.osmand.shared.routing.details

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteDetailsSchemaFixtureTest {

	@Test
	fun schemaFixturePreservesAndroidBoundaryValuesAndOrderedTags() {
		val snapshot = Json.decodeFromString<RouteDetailsSnapshot>(readSchemaFixture())

		assertEquals(RouteDetailsSnapshot.CURRENT_SCHEMA_VERSION, snapshot.schemaVersion)
		assertEquals(snapshot.points.size, snapshot.currentRoutePointIndex)
		assertEquals(snapshot.points.size, snapshot.events.single().locationIndex)
		assertEquals(-1, snapshot.events.single().lastLocationIndex)
		assertEquals(0, snapshot.maneuvers.last().routeEndPointOffset)
		assertEquals(
			listOf("highway=residential", "access=yes", "access=destination"),
			snapshot.segments.single().routeTypes.map { "${it.tag}=${it.value}" },
		)
		assertEquals(listOf(0f, 5f, 111.319f, 6f), snapshot.segments.single().heightValues)
	}

	private fun readSchemaFixture(): String {
		val path = "/routing/details/android_route_details_schema_v1.json"
		return requireNotNull(javaClass.getResourceAsStream(path)) { "Missing schema fixture $path" }
			.bufferedReader()
			.use { it.readText() }
	}
}
