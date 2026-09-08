package net.osmand.shared.routing

import java.util.TreeMap
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The java original built the maps [RouteCalculationProgress.getInfo] returns with a `TreeMap`, and
 * their iteration order reaches the user in the routing log. Common code has no sorted map, so the
 * shared version sorts the keys itself; this checks that the two agree.
 */
class RouteCalculationProgressOrderJvmTest {

	@Test
	fun testInfoMatchesTreeMapOrder() {
		val progress = RouteCalculationProgress()
		progress.timeToCalculate = 5_000_000_000L
		progress.timeToLoad = 1_000_000_000L
		progress.loadedTiles = 300
		progress.visitedSegments = 1000

		val info = progress.getInfo(null)
		assertEquals(TreeMap(info).keys.toList(), info.keys.toList())

		@Suppress("UNCHECKED_CAST")
		val tiles = info["tiles"] as Map<String, Any>
		assertEquals(TreeMap(tiles).keys.toList(), tiles.keys.toList())
	}
}
