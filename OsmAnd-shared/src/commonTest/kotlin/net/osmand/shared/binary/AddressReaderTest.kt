package net.osmand.shared.binary

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.data.MapObject
import net.osmand.shared.routing.RoutingTestFixtures
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The address section of `Turn_lanes_test.obf` read by the shared reader wherever it runs, on
 * Kotlin/Native as well as the jvm. The numbers are what the java reader in OsmAnd-java reads out
 * of the same file; `AddressReaderCompatTest` there compares the two object by object over many
 * more files.
 */
class AddressReaderTest {

	private fun <T> withReader(block: (BinaryMapIndexReader) -> T): T {
		val reader = BinaryMapIndexReader(RoutingTestFixtures.resource("Turn_lanes_test.obf"))
		try {
			return block(reader)
		} finally {
			reader.close()
		}
	}

	@Test
	fun region() = withReader { reader ->
		assertEquals(listOf("Turn_lanes_test"), reader.getRegionNames())
		assertEquals("Turn", reader.getCountryName())
		assertEquals("Turn lanes", reader.getRegionName())
		val center = reader.getRegionCenter()!!
		assertTrue(abs(center.latitude - 48.998615591146645) < 1e-9, "latitude ${center.latitude}")
		assertTrue(abs(center.longitude - 20.95779192633927) < 1e-9, "longitude ${center.longitude}")
	}

	@Test
	fun settlementsAndStreets() = withReader { reader ->
		val expected = mapOf(
			CityBlocks.BOUNDARY_TYPE to Pair(2, 0),
			CityBlocks.CITY_TOWN_TYPE to Pair(2, 49),
			CityBlocks.POSTCODES_TYPE to Pair(0, 0),
			CityBlocks.VILLAGES_TYPE to Pair(267, 452),
			CityBlocks.STREET_TYPE to Pair(0, 0)
		)
		var crossings = 0
		var houses = 0
		for (type in CityBlocks.allTypes()) {
			val cities = reader.getCities(null, type)
			var streets = 0
			for (c in cities) {
				reader.preloadStreets(c, null, true)
				streets += c.getStreets().size
				for (s in c.getStreets()) {
					crossings += s.getIntersectedStreets().size
					houses += s.getBuildings().size
				}
			}
			assertEquals(expected[type], Pair(cities.size, streets), "$type")
		}
		assertEquals(770, crossings, "crossings")
		assertEquals(0, houses, "houses")
		val first = reader.getCities(null, CityBlocks.BOUNDARY_TYPE)[0]
		assertEquals("Красно село", first.getName())
		assertEquals(4405745823744L, first.getId())
	}

	@Test
	fun searchByName() = withReader { reader ->
		fun search(query: String, mode: KStringMatcherMode): List<String> {
			val req = SearchRequest.buildAddressByNameRequest<MapObject>(null, query, mode)
			return reader.searchAddressDataByName(req).map { it::class.simpleName + ":" + it.getName() + ":" + it.getId() }
		}
		val krasno = listOf("City:Красно село:4405745823744", "Street:улица Краснолесья:377", "Street:улица Краснолесья:394")
		assertEquals(krasno, search("красно", KStringMatcherMode.CHECK_STARTS_FROM_SPACE))
		assertEquals(krasno, search("кра", KStringMatcherMode.CHECK_STARTS_FROM_SPACE))
		assertEquals(krasno.subList(0, 1), search("красно", KStringMatcherMode.CHECK_EQUALS_FROM_SPACE))
		assertEquals(emptyList<String>(), search("кра", KStringMatcherMode.CHECK_EQUALS_FROM_SPACE))
		assertEquals(emptyList<String>(), search("киев", KStringMatcherMode.CHECK_STARTS_FROM_SPACE))
		// the copy folds the case of the query itself, which java leaves to its callers
		assertEquals(krasno, search("Кра", KStringMatcherMode.CHECK_STARTS_FROM_SPACE))
	}
}
