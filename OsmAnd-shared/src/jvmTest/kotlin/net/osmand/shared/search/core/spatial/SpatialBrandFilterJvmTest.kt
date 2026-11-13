package net.osmand.shared.search.core.spatial

import net.osmand.shared.data.Amenity
import net.osmand.shared.binary.PoiSubType
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.search.core.SearchPhraseTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SpatialBrandFilterJvmTest {
	@Test
	fun requestFilterUsesBrandValueInsteadOfItsAlternativeName() {
		SearchPhraseTest.setUp
		val search = SpatialPoiSearch(MapPoiTypes.getDefault())
		val subtype = PoiSubType().apply { name = MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX + "brand" }
		val type = SpatialPoiSearch.SpatialPoiType(subtype, "Lidl", "brand_lidl", 42)
		type.names.addAll(listOf("لیدل", "Lidl"))
		val prepare = SpatialPoiSearch::class.java.declaredMethods.single { it.name == "prepareRequest" }
		prepare.isAccessible = true
		val request = prepare.invoke(search, type, arrayListOf<Amenity>(),
			0, 0, 1, 1, 16, intArrayOf(10)) as SearchRequest<*>
		val filter = request.poiAdditionalFilter!!
		assertEquals("Lidl", filter.getName())
		assertTrue(filter.accept(subtype, "Lidl"))
		assertFalse(filter.accept(subtype, "لیدل"))
		assertFalse(filter.accept(subtype, "Prisma"))
	}
}
