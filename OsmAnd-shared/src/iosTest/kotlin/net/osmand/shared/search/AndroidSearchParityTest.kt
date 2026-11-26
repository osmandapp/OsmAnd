package net.osmand.shared.search

import co.touchlab.stately.concurrency.AtomicInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.Amenity
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.search.core.NameStringMatcher
import net.osmand.shared.search.core.SearchCoreFactory.SearchAmenityByNameAPI
import net.osmand.shared.search.core.SearchCoreFactory.SearchLocationAndUrlAPI
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.SearchSettings
import okio.FileSystem
import okio.GzipSource
import okio.buffer
import okio.use
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Expected values from OsmAnd-java on Android API 37, using the same OBF extracts. */
class AndroidSearchParityTest {

	@BeforeTest
	fun prepareSearch() {
		SearchPhraseTest.setUp
	}

	@Test
	fun favoriteNameContainsPastedLodzQuery() {
		val matcher = NameStringMatcher("Keyaudit Lodz", KStringMatcherMode.CHECK_CONTAINS)
		assertTrue(matcher.matches("Keyaudit Łódź"))
	}

	@Test
	fun unicodeSpacesSeparateUnknownWords() {
		val settings = SearchSettings(emptyList())
		val words = listOf("a\u00a0b", "a\u2003b").map { text ->
			val phrase = SearchPhrase.emptyPhrase(settings).generateNewPhrase(text, settings)
			listOf(phrase.getFirstUnknownSearchWord()) + phrase.getUnknownSearchWords()
		}
		assertEquals(listOf(listOf("a", "b"), listOf("a", "b")), words)
	}

	// Formatting diagnostic tracked in the review of https://github.com/osmandapp/OsmAnd/pull/27156.
	@Ignore
	@Test
	fun coordinateLabelRoundsLikeAndroid() {
		val api = SearchLocationAndUrlAPI(SearchAmenityByNameAPI())
		assertEquals("52.12346, 4.00002", "${api.formatLatLon(52.123455)}, ${api.formatLatLon(4.000025)}")
	}

	@Test
	fun saintLysWithHyphen() = withMap("saintLys") { settings ->
		val names = search(settings, "Saint-lys").map { SearchUICore.formatSearchResultForTest(true, it, it.requiredSearchPhrase) }
		assertEquals(SearchUICoreTest.androidScenarioRows("saintLys.json", "Saint-lys"), names)
	}

	@Test
	fun claytonHouseNumbersAndStreetOrder() = withMap("Issue-16715-geo-NC-42") { settings ->
		for (number in listOf("2627", "2621")) {
			val query = "$number NC 42 Clayton"
			val results = search(settings, query)
			assertEquals(295, results.size, number)
			val expected = checkNotNull(SearchUICoreTest.androidScenarioRows("Issue-16715-geo-NC-42.json", query))
			assertEquals(expected.map { it.removePrefix("@") }, results.take(10).map {
				SearchUICore.formatSearchResultForTest(true, it, it.requiredSearchPhrase)
			}, number)
		}
	}

	@Test
	fun dorfstrasseResultOrder() = withMap("ge3") { settings ->
		assertEquals("Rosenberg", search(settings, "6 Dorfstraße R")[37].localeName)
	}

	@Test
	fun tempelhofResultOrder() = withMap("Issue-24832-geo-Tempelhof") { settings ->
		val names = search(settings, "1 M").drop(104).take(3).map { it.localeName }
		assertEquals(listOf("Mescherin", "Meßdorf", "Messingwerksiedlung"), names)
	}

	@Test
	fun kyivskaDentalClinicMatchWeight() = withMap("kyivska_10-d") { settings ->
		val phrase = SearchPhrase.emptyPhrase(settings).generateNewPhrase("Київська ", settings)
		val matcher = SearchUICore.SearchResultMatcher(null, phrase, 0, AtomicInt(0), -1)
		SearchAmenityByNameAPI().search(phrase, matcher)
		val clinic = matcher.getRequestResults().single { (it.`object` as? Amenity)?.getId() == 2004839760L }
		assertEquals(10.0, clinic.getUnknownPhraseMatchWeight())
	}

	private fun search(settings: SearchSettings, query: String): List<SearchResult> {
		val phrase = SearchPhrase.emptyPhrase(settings).generateNewPhrase(query, settings)
		val core = SearchUICore(MapPoiTypes.getDefault(), "en", false) { false }
		core.init()
		val matcher = SearchUICore.SearchResultMatcher(null, phrase, 0, AtomicInt(0), -1)
		core.searchInternal(phrase, matcher)
		return SearchUICore.SearchResultCollection(phrase)
			.addSearchResults(matcher.getRequestResults(), true, true).getCurrentSearchResults()
	}

	private fun withMap(name: String, check: (SearchSettings) -> Unit) {
		val fs = FileSystem.SYSTEM
		val fixtures = RoutingTestFixtures.resourcesDir() / "search"
		val settingsJson = fs.read(fixtures / "$name.json") { Json.parseToJsonElement(readUtf8()) }.jsonObject
		val settings = SearchSettings.parseJSON(settingsJson.getValue("settings").jsonObject)
		val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "search-parity-${Random.nextLong().toULong()}"
		fs.createDirectory(dir)
		try {
			val map = dir / "$name.obf"
			GzipSource(fs.source(fixtures / "$name.obf.gz")).use { source ->
				fs.sink(map).buffer().use { it.writeAll(source) }
			}
			val reader = BinaryMapIndexReader(map.toString())
			try {
				settings.setOfflineIndexes(listOf(reader))
				check(settings)
			} finally {
				reader.close()
			}
		} finally {
			fs.deleteRecursively(dir)
		}
	}
}
