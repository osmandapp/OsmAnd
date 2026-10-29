package net.osmand.shared.search

import co.touchlab.stately.concurrency.AtomicInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.data.City
import net.osmand.shared.data.CityType
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.Street
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.search.SearchUICore.SearchResultCollection
import net.osmand.shared.search.SearchUICore.SearchResultMatcher
import net.osmand.shared.search.core.ObjectType
import net.osmand.shared.search.core.SearchCoreAPI
import net.osmand.shared.search.core.SearchCoreFactory
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.SearchSettings
import net.osmand.shared.search.core.spatial.SpatialSearchQueryRulesTest
import net.osmand.shared.search.core.spatial.SpatialTextSearchAPI
import net.osmand.shared.util.KMapUtils
import okio.FileSystem
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The spatial search in [SearchUICore], `init(true)`: the tests of it of `SearchUICoreGenericTest` in OsmAnd-java, and that
 * the order and the levels the engine gives are kept where the core makes a collection of them.
 */
class SearchUICoreSpatialTest {

	private val rulesDir = SpatialSearchQueryRulesTest.rulesDir()

	@AfterTest
	fun deleteRules() {
		FileSystem.SYSTEM.deleteRecursively(rulesDir)
	}

	private fun core(): SearchUICore {
		SearchPhraseTest.setUp
		val core = SearchUICore(MapPoiTypes.getDefault(), "en", false)
		core.init(true, rulesDir.toString())
		return core
	}

	@Test
	fun spatialPoiSearchAfterSelectingCity() {
		val core = core()
		val api = core.getApiByClass(SpatialTextSearchAPI::class)
		assertNotNull(api)

		val unselectedPhrase = core.resetPhrase("Kyiv pizzeria")
		assertTrue(api.isSearchAvailable(unselectedPhrase))
		assertTrue(api.getSearchPriority(unselectedPhrase) != -1)

		val cityResult = selectCity(core, createCity())
		val cityApi = core.getApiByClass(SearchCoreFactory.SearchStreetByCityAPI::class)
		assertNotNull(cityApi)
		assertTrue(cityApi.getSearchPriority(core.getPhrase()) != -1, "Selecting a city must still suggest streets")

		val selectedPhrase = core.resetPhrase("Kyiv pizzeria")
		assertSame(cityResult, selectedPhrase.getLastSelectedWord()!!.getResult())
		assertEquals("pizzeria", selectedPhrase.getUnknownSearchPhrase())
		assertTrue(api.isSearchAvailable(selectedPhrase))
		assertTrue(api.getSearchPriority(selectedPhrase) != -1, "Selecting a city must not disable spatial POI text search")
		assertTrue(
			cityApi.getSearchPriority(selectedPhrase) == -1,
			"Typed text must be handled by spatial search instead of the street list API"
		)
		assertTrue(core.immediateSearch("Kyiv pizzeria", cityResult.location).isSkipSorting())
	}

	@Test
	fun spatialSearchAfterSelectingCityWithoutFile() {
		val core = core()
		val city = createCity()
		val cityResult = selectCity(core, city)
		assertNull(cityResult.file)
		assertNull(city.getReferenceFile())

		val street = Street(city)
		street.setName("Khreshchatyk")
		street.setLocation(cityResult.location)
		val api = core.getApiByClass(SpatialTextSearchAPI::class)
		assertNotNull(api)
		// Keep the real API selection rules, replacing only the OBF lookup.
		core.apis[core.apis.indexOf(api)] = object : SpatialTextSearchAPI(MapPoiTypes.getDefault(), rulesDir.toString()) {
			override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
				val result = SearchResult(phrase)
				result.`object` = street
				result.objectType = ObjectType.STREET
				result.localeName = street.getName()
				result.location = street.getLocation()
				return resultMatcher.publish(result)
			}
		}

		assertTrue(core.immediateSearch("Kyiv ", cityResult.location).getCurrentSearchResults().isEmpty())
		val results = core.immediateSearch("Kyiv Khreshchatyk", cityResult.location).getCurrentSearchResults()
		assertTrue(results.size == 1, "A city without a file must still allow spatial search results")
		val result = results[0]
		assertSame(street, result.`object`)
		assertSame(cityResult, result.requiredSearchPhrase.getLastSelectedWord()!!.getResult())
		assertEquals("Kyiv Khreshchatyk", result.requiredSearchPhrase.getFullSearchPhrase())
	}

	@Test
	fun spatialSearchAfterSelectingBoundary() {
		val core = core()
		val boundary = City(CityType.BOUNDARY)
		boundary.setName("Kyiv Oblast")
		boundary.setLocation(50.4501, 30.5234)
		val boundaryResult = selectCity(core, boundary)

		val api = core.getApiByClass(SpatialTextSearchAPI::class)
		assertNotNull(api)
		val phrase = core.resetPhrase("Kyiv Oblast pizzeria")
		assertSame(boundaryResult, phrase.getLastSelectedWord()!!.getResult())
		assertTrue(api.getSearchPriority(phrase) != -1, "Selecting a boundary must not disable spatial text search")
	}

	@Test
	fun spatialSearchApiForSearchTypes() {
		val core = core()
		val addressTabTypes = arrayOf(
			ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE, ObjectType.HOUSE,
			ObjectType.STREET_INTERSECTION, ObjectType.STREET, ObjectType.LOCATION, ObjectType.PARTIAL_LOCATION
		)
		val aidlAddressTypes = arrayOf(
			ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE, ObjectType.STREET,
			ObjectType.HOUSE, ObjectType.STREET_INTERSECTION
		)

		assertSpatialTextSearch(core, null, "Khreshchatyk 22", true)
		assertSpatialTextSearch(core, addressTabTypes, "Khreshchatyk 22", true)
		assertSpatialTextSearch(core, aidlAddressTypes, "Khreshchatyk 22", true)
		assertSpatialTextSearch(core, arrayOf(ObjectType.CITY, ObjectType.VILLAGE), "Kiev", false)
		assertSpatialTextSearch(core, arrayOf(ObjectType.CITY), "Kiev", false)
		assertSpatialTextSearch(core, arrayOf(ObjectType.POSTCODE), "0100", false)
		assertSpatialTextSearch(core, arrayOf(), "Khreshchatyk", false)
		assertFalse(
			isApiRunning(core, SearchCoreFactory.SearchAddressByNameAPI::class, setSearchTypes(core, arrayOf(), "Khreshchatyk"))
		)

		setSearchTypes(core, addressTabTypes, "")
		selectCity(core, createCity())
		val streetsPhrase = core.resetPhrase("Kyiv Khreshch")
		assertTrue(
			isApiRunning(core, SearchCoreFactory.SearchStreetByCityAPI::class, streetsPhrase),
			"Typed text must filter the streets of the city selected on the address tab"
		)
		assertFalse(isApiRunning(core, SpatialTextSearchAPI::class, streetsPhrase))

		setSearchTypes(core, null, "")
		val group = SearchResult(core.resetPhrase("Revgroup"))
		group.objectType = ObjectType.FAVORITE_GROUP
		group.localeName = "Revgroup"
		core.selectSearchResult(group)
		val groupPhrase = core.resetPhrase("Revgroup Khreshch")
		assertFalse(isApiRunning(core, SpatialTextSearchAPI::class, groupPhrase), "Only favorites are searched inside a favorites group")
	}

	// the lists of the address tab are searched by name near and far, as the classic search searches them
	@Test
	fun addressListsAreSearchedAsByTheClassicSearch() {
		val spatial = core()
		val classic = SearchUICore(MapPoiTypes.getDefault(), "en", false)
		classic.init()
		for (types in listOf(arrayOf(ObjectType.CITY, ObjectType.VILLAGE), arrayOf(ObjectType.POSTCODE))) {
			val expected = addressByNamePriorities(classic, setSearchTypes(classic, types, "Kiev"))
			assertEquals(2, expected.size)
			assertEquals(expected, addressByNamePriorities(spatial, setSearchTypes(spatial, types, "Kiev")), types.contentToString())
		}
	}

	private fun addressByNamePriorities(core: SearchUICore, phrase: SearchPhrase): List<Int> =
		core.apis.filter { it is SearchCoreFactory.SearchAddressByNameAPI && it.isSearchAvailable(phrase) }
			.map { it.getSearchPriority(phrase) }

	private fun assertSpatialTextSearch(core: SearchUICore, searchTypes: Array<ObjectType>?, text: String, spatial: Boolean) {
		val phrase = setSearchTypes(core, searchTypes, text)
		val types = searchTypes?.contentToString() ?: "null"
		assertEquals(spatial, isApiRunning(core, SpatialTextSearchAPI::class, phrase), "Spatial text search for types $types")
		if (searchTypes != null && searchTypes.isNotEmpty()) {
			assertEquals(
				!spatial, isApiRunning(core, SearchCoreFactory.SearchAddressByNameAPI::class, phrase),
				"Address by name search for types $types"
			)
		}
	}

	private fun setSearchTypes(core: SearchUICore, searchTypes: Array<ObjectType>?, text: String): SearchPhrase {
		val settings = if (searchTypes == null)
			core.getSearchSettings().resetSearchTypes()
		else
			core.getSearchSettings().setSearchTypes(*searchTypes)
		core.updateSettings(settings.setEmptyQueryAllowed(true))
		return core.resetPhrase(text)
	}

	private fun isApiRunning(core: SearchUICore, cl: KClass<out SearchCoreAPI>, phrase: SearchPhrase): Boolean {
		for (api in core.apis) {
			if (cl.isInstance(api) && api.isSearchAvailable(phrase) && api.getSearchPriority(phrase) != -1) {
				return true
			}
		}
		return false
	}

	@Test
	fun noSearchMoreAfterAllSpatialLevelsShown() = runBlocking {
		// #25940: the dialog shows the levels in its own collection; the core one stays at level 0 and must not
		// offer "Increase search radius", which reran the same spatial search and appended a copy of every row
		val core = core()
		core.registerAPI(object : SpatialTextSearchAPI(MapPoiTypes.getDefault(), rulesDir.toString()) {
			override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
				for (level in 0 until 2) {
					val r = SearchResult(phrase)
					r.localeName = "Tatranská magistrála"
					r.objectType = ObjectType.POI
					r.location = KLatLon(49.1 + level, 19.6)
					r.spatialSearchVisibleLevel = level
					resultMatcher.publish(r)
				}
				return true
			}
		})
		core.updateSettings(core.getSearchSettings().setOriginalLocation(KLatLon(49.08, 19.61)))
		val finished = AtomicInt(0)
		core.search("Tatranska magistrala", false, object : ResultMatcher<SearchResult> {
			override fun publish(obj: SearchResult): Boolean {
				if (obj.objectType == ObjectType.SEARCH_FINISHED) {
					finished.incrementAndGet()
				}
				return true
			}

			override fun isCancelled(): Boolean = false
		})
		until { finished.get() == 1 }

		val shown = SearchResultCollection(core.getPhrase(), true)
			.addSearchResults(core.getCurrentSearchResult().getCurrentSearchResults(), false, true)
		assertEquals(1, shown.getVisibleSpatialSearchResults().size)
		assertTrue(shown.showMoreSpatialSearchResults())
		assertEquals(2, shown.getVisibleSpatialSearchResults().size)
		assertFalse(shown.hasMoreSpatialSearchResults())
		assertFalse(core.isSearchMoreAvailable(core.getPhrase()), "All levels are shown: nothing more to search")
	}

	@Test
	fun combinedCollectionKeepsTheOrderOfTheEngine() {
		val phrase = originPhrase()
		val spatial = SearchResultCollection(phrase, true, 1)
		spatial.addSearchResults(listOf(result(phrase, "far", 300), result(phrase, "near", 100)), true, true)
		val classic = SearchResultCollection(phrase)
		classic.addSearchResults(listOf(result(phrase, "middle", 200)), true, true)
		val spatialFirst = spatial.combineWithCollection(classic, true, true)
		val classicFirst = classic.combineWithCollection(spatial, true, true)
		assertEquals(listOf("far", "near", "middle"), spatialFirst.getCurrentSearchResults().map { it.localeName })
		assertEquals(listOf("middle", "far", "near"), classicFirst.getCurrentSearchResults().map { it.localeName })
		for (combined in listOf(spatialFirst, classicFirst)) {
			assertTrue(combined.isSkipSorting())
			assertEquals(1, combined.getSpatialSearchVisibleLevel())
		}
	}

	@Test
	fun shallowSearchKeepsTheOrderOfTheEngine() {
		SearchPhraseTest.setUp
		val core = SearchUICore(MapPoiTypes.getDefault(), "en", false)
		core.registerAPI(object : SpatialTextSearchAPI(MapPoiTypes.getDefault(), rulesDir.toString()) {
			override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
				resultMatcher.publish(result(phrase, "far", 300))
				resultMatcher.publish(result(phrase, "near", 100))
				return true
			}
		})
		core.updateSettings(core.getSearchSettings().setOriginalLocation(KLatLon(0.0, 0.0)))
		val collection = core.shallowSearch(SpatialTextSearchAPI::class, "far near", null, true, true)!!
		assertTrue(collection.isSkipSorting())
		assertEquals(listOf("far", "near"), collection.getCurrentSearchResults().map { it.localeName })
	}

	@Test
	fun typedResultsKeepTheLevelsOfTheSpatialSearch() = runBlocking {
		// the next letter typed: the core shows what it kept that matches, the levels shown as the search left them
		val core = core()
		core.registerAPI(object : SpatialTextSearchAPI(MapPoiTypes.getDefault(), rulesDir.toString()) {
			override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
				for (level in 0 until 2) {
					val r = SearchResult(phrase)
					r.localeName = "Tatranská magistrála"
					r.objectType = ObjectType.POI
					r.location = KLatLon(49.1 + level, 19.6)
					r.spatialSearchVisibleLevel = level
					resultMatcher.publish(r)
				}
				return true
			}
		})
		core.updateSettings(core.getSearchSettings().setOriginalLocation(KLatLon(49.08, 19.61)))
		val finished = AtomicInt(0)
		val visibleWhileTyping = AtomicInt(-1)
		val matcher = object : ResultMatcher<SearchResult> {
			override fun publish(obj: SearchResult): Boolean {
				if (obj.objectType == ObjectType.FILTER_FINISHED) {
					visibleWhileTyping.set(core.getCurrentSearchResult().getVisibleSpatialSearchResults().size)
				} else if (obj.objectType == ObjectType.SEARCH_FINISHED) {
					finished.incrementAndGet()
				}
				return true
			}

			override fun isCancelled(): Boolean = false
		}
		core.search("Tatranska magistrala", false, matcher)
		until { finished.get() == 1 }
		assertEquals(1, core.getCurrentSearchResult().getVisibleSpatialSearchResults().size)
		core.search("Tatranska magistrala ", true, matcher)
		until { finished.get() == 2 }
		assertEquals(1, visibleWhileTyping.get())
	}

	private suspend fun until(condition: () -> Boolean) {
		withTimeout(10_000) {
			while (!condition()) {
				delay(5)
			}
		}
	}

	private fun originPhrase(): SearchPhrase {
		SearchPhraseTest.setUp
		return SearchPhrase.emptyPhrase(SearchSettings(null as SearchSettings?).setOriginalLocation(KLatLon(0.0, 0.0)))
	}

	// a result [dist] meters east of the origin
	private fun result(phrase: SearchPhrase, name: String, dist: Int): SearchResult {
		val res = SearchResult(phrase)
		res.localeName = name
		res.objectType = ObjectType.POI
		res.location = KLatLon(0.0, dist / KMapUtils.getDistance(0.0, 0.0, 0.0, 1.0))
		return res
	}

	private fun createCity(): City {
		val city = City(CityType.CITY)
		city.setName("Kyiv")
		city.setLocation(50.4501, 30.5234)
		return city
	}

	private fun selectCity(core: SearchUICore, city: City): SearchResult {
		val result = SearchResult(core.resetPhrase(city.getName()))
		result.`object` = city
		result.objectType = ObjectType.CITY
		result.localeName = city.getName()
		result.location = city.getLocation()
		core.selectSearchResult(result)
		return result
	}
}
