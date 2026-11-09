package net.osmand.shared.data

import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.search.AmenitySearcher
import net.osmand.shared.search.core.SearchPhraseTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BaseDetailsObjectImportTest {

	@Test
	fun preservesCombinedDataAndCompleteness() {
		SearchPhraseTest.setUp
		val first = Amenity().apply {
			setId(246L)
			setAdditionalInfo(Amenity.WIKIDATA, "Q123")
		}
		val second = Amenity().apply { setId(912L) }
		val combined = Amenity().apply {
			setName("Combined name")
			setAdditionalInfo("phone", "123456")
			getX().add(100)
			getY().add(200)
		}
		for (state in BaseDetailsObject.ObjectCompleteness.entries) {
			val details = BaseDetailsObject(listOf(second, first), combined, "de", state)
			assertSame(combined, details.getSyntheticAmenity())
			assertEquals(listOf(second, first), details.getObjects())
			assertEquals("de", details.getLang())
			assertEquals(state == BaseDetailsObject.ObjectCompleteness.EMPTY, details.isObjectEmpty())
			assertEquals(state == BaseDetailsObject.ObjectCompleteness.COMBINED, details.isObjectCombined())
			assertEquals(state == BaseDetailsObject.ObjectCompleteness.FULL, details.isObjectFull())
			assertTrue(details.overlapsWith(Amenity().apply { setId(246L) }))
			assertTrue(details.overlapsWith(Amenity().apply { setAdditionalInfo(Amenity.WIKIDATA, "Q123") }))
		}
		assertFailsWith<IllegalArgumentException> {
			BaseDetailsObject(emptyList(), combined, "de", BaseDetailsObject.ObjectCompleteness.FULL)
		}
	}

	@Test
	fun repeatedResolutionWithoutRepositoriesPreservesSyntheticGeometry() {
		SearchPhraseTest.setUp
		val component = Amenity().apply {
			setId(246L)
			setLocation(52.0, 13.0)
		}
		val combined = Amenity().apply {
			setId(246L)
			setLocation(52.0, 13.0)
			setName("Combined name")
			setAdditionalInfo("phone", "123456")
			for (i in 0..2) {
				getX().add(100 + i)
				getY().add(200 + i)
			}
		}
		val searcher = AmenitySearcher(MapPoiTypes.getDefault())
		val settings = AmenitySearcher.Settings({ "en" }, { false }, null)
		for (state in listOf(BaseDetailsObject.ObjectCompleteness.FULL, BaseDetailsObject.ObjectCompleteness.COMBINED)) {
			val details = BaseDetailsObject(listOf(component), combined, "en", state)
			repeat(2) {
				assertSame(details, searcher.searchDetailedObject(details, settings))
				assertEquals(listOf(100, 101, 102), (0..2).map { details.getSyntheticAmenity().getX().get(it) })
				assertEquals(listOf(200, 201, 202), (0..2).map { details.getSyntheticAmenity().getY().get(it) })
				assertEquals("Combined name", details.getSyntheticAmenity().getName())
				assertEquals("123456", details.getSyntheticAmenity().getAdditionalInfo("phone"))
			}
		}
	}
}
