package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.PoiSubType
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchSettings
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.TopIndexFilter
import net.osmand.shared.data.KLatLon
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiTranslator
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import okio.FileSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

/** `SpatialPoiSearchTest` of OsmAnd-java. */
class SpatialPoiSearchTest {

	private val rulesDir = SpatialSearchQueryRulesTest.rulesDir()

	@AfterTest
	fun deleteRules() {
		FileSystem.SYSTEM.deleteRecursively(rulesDir)
	}

	@Test
	fun testMatchedNameKeepsDiacritics() {
		val poiTypes = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		poiTypes.init()
		poiTypes.setPoiTranslator(
			PhrasesTranslator(
				mapOf(
					"sports_centre" to "Sports center;Спортивний центр;спорткомплекс",
					"shelter" to "Shelter;Unterstand;Schutzhütte"
				)
			)
		)
		val poiSearch = SpatialPoiSearch(poiTypes)

		assertEquals("Спортивний центр", getMatchedName(poiSearch, "спорт", "sports_centre"))
		assertEquals("Schutzhütte", getMatchedName(poiSearch, "schutz", "shelter"))
	}

	@Test
	fun brandRowUsesValueInsteadOfFirstAlternativeName() {
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		types.init()
		val poiSearch = SpatialPoiSearch(types)
		val subtype = PoiSubType().apply { name = MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX + "brand" }
		for ((value, alternative) in listOf("Lidl" to "لیدل", "Prisma" to "پریسما", "Muji" to "無印良品")) {
			val type = SpatialPoiSearch.SpatialPoiType(subtype, value, "brand_$value", 42)
			type.names.addAll(listOf(alternative, value))
			poiSearch.byId[42] = type
			val spatial = SpatialSearchResult(SpatialSearchResultsList(), 0, null, null, null)
			spatial.objs.add(SpatialSearchResult.SpatialSearchResultRef(
				SpatialSearchToken.NameIndexAtom(value, 42, 1)))
			val phrase = SearchPhrase.emptyPhrase(
				SearchSettings(emptyList()))
			val result = SearchResult(phrase)
			val api = SpatialTextSearchAPI(types, rulesDir.toString())
			assertNotNull(api.convertSpatialSearchResult(spatial, result, poiSearch, null, "en", false))
			assertEquals(value, result.localeName)
			assertEquals(value, (result.`object` as TopIndexFilter).getValue())
		}
	}

	private fun getMatchedName(poiSearch: SpatialPoiSearch, query: String, categoryKey: String): String? {
		val ctx = SpatialSearchContext(
			SpatialTextSearchSettings.suggestionSettings(), emptyList(), poiSearch, KLatLon(51.3397, 12.3731)
		)
		val res = SpatialTextSearch(rulesDir.toString()).searchAPI(query, ctx)
		val mainResults = res.mainResults
		assertNotNull(mainResults)
		for (r in mainResults) {
			val category = r.getPoiCategory(poiSearch)
			if (category != null && category.getKey() == categoryKey) {
				return r.getPoiCategoryMatchedName(poiSearch)
			}
		}
		fail("No category $categoryKey for $query")
	}

	private class PhrasesTranslator(private val phrases: Map<String, String>) : PoiTranslator {

		override fun getTranslation(type: AbstractPoiType): String? = getTranslation(type.getKeyName())

		override fun getTranslation(keyName: String): String? = phrases[keyName]?.split(";")?.get(0)

		override fun getEnTranslation(type: AbstractPoiType): String? = null

		override fun getEnTranslation(keyName: String): String? = null

		override fun getSynonyms(type: AbstractPoiType): String? = getSynonyms(type.getKeyName())

		override fun getSynonyms(keyName: String): String? {
			val phrase = phrases[keyName] ?: return null
			return phrase.substring(phrase.indexOf(';') + 1)
		}

		override fun getAllLanguagesTranslationSuffix(): String = ""
	}
}
