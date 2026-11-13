package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.CityBlocks
import net.osmand.shared.binary.TagValuePair
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.City
import net.osmand.shared.data.CityType
import net.osmand.shared.data.MapObject
import net.osmand.shared.search.core.spatial.SpatialSearchResult.SpatialSearchResultRef
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtomXY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpatialSearchRankingTest {
	private val ranking = SpatialSearchRanking()
	private fun city(name: String = "Espoo", type: CityType = CityType.CITY) = City(type).apply { setName(name) }
	private fun group(kind: String, name: String, nameTag: String = "name") = listOf(
		TagValuePair("place", kind, 0), TagValuePair(nameTag, name, 0))
	private fun result(groups: List<List<TagValuePair>>, vararg cities: City): SpatialSearchResult {
		val amenity = Amenity().apply { setName("Lidl"); setTagGroups(groups.mapIndexed { i, g -> i to g }.toMap().toMutableMap()) }
		val result = SpatialSearchResult(SpatialSearchResultsList(), 0, null, null, null)
		fun add(obj: MapObject, type: Int, id: Long) {
			result.objs.add(SpatialSearchResultRef(NameIndexAtom(obj.getName(), type, id, 0, obj, false, 0, 0,
				NameIndexAtomXY(null, null, null), 0, -1)))
		}
		add(amenity, SpatialSearchToken.POI_TYPE, 1)
		cities.forEachIndexed { i, c -> add(c, CityBlocks.CITY_TOWN_TYPE.index, i + 2L) }
		return result
	}

	@Test fun poiInAnotherCityLosesExactlyTheOutsideCityWeight() {
		val result = result(listOf(group("city", "Helsinki")), city())
		assertTrue(ranking.outsideMatchedCity(result))
		ranking.wOutsideCity = 0.0
		val withoutPenalty = ranking.score(result, null)
		ranking.wOutsideCity = 1.0
		assertEquals(withoutPenalty - 1.0, ranking.score(result, null), 1e-12)
	}

	@Test fun matchingLocalizedNameAndCaseAvoidPenalty() {
		val city = city().apply { setName("sv", "Esbo") }
		assertFalse(ranking.outsideMatchedCity(result(listOf(group("city", "ESBO", "name:sv")), city)))
		assertFalse(ranking.outsideMatchedCity(result(listOf(group("city", "Helsinki"), group("city", "Espoo")), city)))
	}

	@Test fun missingSettlementOrAnotherKindDoesNotProveOutsideCity() {
		assertFalse(ranking.outsideMatchedCity(result(emptyList(), city())))
		assertFalse(ranking.outsideMatchedCity(result(listOf(group("town", "Helsinki")), city())))
		assertFalse(ranking.outsideMatchedCity(result(listOf(group("city", "Helsinki")))))
	}

	@Test fun boundariesAndPostcodesAreNotMatchedCities() {
		for (type in listOf(CityType.BOUNDARY, CityType.POSTCODE, CityType.DISTRICT)) {
			assertFalse(ranking.outsideMatchedCity(result(listOf(group("city", "Helsinki")), city(type = type))))
		}
		assertTrue(ranking.outsideMatchedCity(result(listOf(group("city", "Helsinki")), city(type = CityType.BOUNDARY), city())))
	}
}
