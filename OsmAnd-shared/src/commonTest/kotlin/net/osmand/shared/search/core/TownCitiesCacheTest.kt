package net.osmand.shared.search.core

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.routing.RoutingTestFixtures
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** `TownCitiesCacheTest` of OsmAnd-java, for the copy of the cache. */
class TownCitiesCacheTest {

	// Files of different maps may share the same region name (ex. combined German state maps
	// of 2025 are all "Germany"), cities should be loaded from each file anyway (issue #25993)
	@Test
	fun cachePerFileNotPerRegionName() {
		val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "town-cities-cache-${Random.nextLong().toULong()}"
		FileSystem.SYSTEM.createDirectory(dir)
		// file names are different on purpose: getRegionName() falls back to the file name
		// when the file has no region names inside, such fallback would give "first" / "second"
		val first = copy(dir, "first_region_2.obf")
		val second = copy(dir, "second_region_2.obf")
		val firstReader = BinaryMapIndexReader(first.toString())
		val secondReader = BinaryMapIndexReader(second.toString())
		try {
			// region name is read from inside the file, so both copies report the same one
			assertEquals(firstReader.getRegionName(), secondReader.getRegionName())

			val cache = SearchCoreFactory.TownCitiesCache()
			assertFalse(cache.contains(firstReader))
			cache.add(firstReader)

			assertTrue(cache.contains(firstReader))
			assertFalse(cache.contains(secondReader))

			cache.add(secondReader)
			assertTrue(cache.contains(secondReader))
		} finally {
			firstReader.close()
			secondReader.close()
			FileSystem.SYSTEM.deleteRecursively(dir)
		}
	}

	private fun copy(dir: Path, name: String): Path {
		val file = dir / name
		FileSystem.SYSTEM.copy(RoutingTestFixtures.resource("Turn_lanes_test.obf").toPath(), file)
		return file
	}
}
