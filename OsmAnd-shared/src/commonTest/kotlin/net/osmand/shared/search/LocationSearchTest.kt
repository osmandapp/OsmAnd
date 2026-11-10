package net.osmand.shared.search

import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicInt
import net.osmand.shared.data.KLatLon
import net.osmand.shared.search.SearchUICore.SearchResultMatcher
import net.osmand.shared.search.core.SearchCoreFactory
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.util.KLocationParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `LocationSearchTest` of OsmAnd-java: places typed or pasted, through the api of locations and links. */
class LocationSearchTest {

	private fun search(string: String, latLon: KLatLon) {
		SearchPhraseTest.setUp
		val srm = SearchResultMatcher(null, null, 0, AtomicInt(0), 100)
		SearchCoreFactory.SearchLocationAndUrlAPI(SearchCoreFactory.SearchAmenityByNameAPI())
			.search(SearchPhrase.emptyPhrase().generateNewPhrase(string, null), srm)
		assertEquals(1, srm.getRequestResults().size, string)
		assertEquals(latLon, srm.getRequestResults()[0].location, string)
	}

	@Test
	fun geo() {
		search("geo:34.99393,-106.61568 (Treasure Island, other irrelevant info) ", KLatLon(34.99393, -106.61568))
		search("http://download.osmand.net/go?lat=34.99393&lon=-106.61568&z=11", KLatLon(34.99393, -106.61568))
	}

	@Test
	fun gooGlRedirectSkippedWithoutInternetConnection() {
		SearchPhraseTest.setUp
		val internetConnectionChecked = AtomicBoolean(false)
		val srm = SearchResultMatcher(null, null, 0, AtomicInt(0), 100)
		SearchCoreFactory.SearchLocationAndUrlAPI(SearchCoreFactory.SearchAmenityByNameAPI()) {
			internetConnectionChecked.value = true
			false
		}.search(SearchPhrase.emptyPhrase().generateNewPhrase("http://goo.gl/maps/Cji0V", null), srm)
		assertTrue(internetConnectionChecked.value)
		assertEquals(0, srm.getRequestResults().size)
	}

	@Test
	fun basicCommaSearch() {
		search("5.0,3.0", KLatLon(5.0, 3.0))
		search("(5.0,3.0)", KLatLon(5.0, 3.0))
		search("5.445,3.523", KLatLon(5.445, 3.523))
		search("5:1:1,3:1", KLatLon((5 + 1 / 60f + 1 / 3600f).toDouble(), (3 + 1 / 60f).toDouble()))
	}

	@Test
	fun utmSearch() {
		search("17N6734294749123", KLatLon(42.875017, -78.87659050764749))
		search("17 N 673429 4749123", KLatLon(42.875017, -78.87659050764749))
		search("36N 609752 5064037", KLatLon(45.721184, 34.410328))
		search("35U 332274 5421365", KLatLon(48.922478, 24.71033))
	}

	@Test
	fun utmZoneOutOfRange() {
		// there is no UTM zone 61
		assertNull(KLocationParser.parseLocation("61 N 673429 4749123"))
		assertNull(KLocationParser.parseLocation("61N6734294749123"))
	}

	@Test
	fun basicSpaceSearch() {
		search("5.0 3.0", KLatLon(5.0, 3.0))
		search("-5.0 -3.0", KLatLon(-5.0, -3.0))
		search("-45.5 3.0S", KLatLon(-45.5, -3.0))
		search("45.5S 3.0 W", KLatLon(-45.5, -3.0))
		search("5.445 3.523", KLatLon(5.445, 3.523))
		search("5:1:1 3:1", KLatLon((5 + 1 / 60f + 1 / 3600f).toDouble(), (3 + 1 / 60f).toDouble()))
		search("5:1#1 3#1", KLatLon((5 + 1 / 60f + 1 / 3600f).toDouble(), (3 + 1 / 60f).toDouble()))
		search("5#1#1 3#1", KLatLon((5 + 1 / 60f + 1 / 3600f).toDouble(), (3 + 1 / 60f).toDouble()))
		search("5'1'1 3'1", KLatLon((5 + 1 / 60f + 1 / 3600f).toDouble(), (3 + 1 / 60f).toDouble()))
		search("Lat: 5.0 Lon: 3.0", KLatLon(5.0, 3.0))
		search("0 n, 78 w", KLatLon(0.0, -78.0))
		search("0 N, 78 W", KLatLon(0.0, -78.0))
		search("N 0 W 78", KLatLon(0.0, -78.0))
		search("n 0 w 78", KLatLon(0.0, -78.0))
	}

	@Test
	fun simpleUrlSearch() {
		search("ftp://simpleurl?lat=34.23&lon=-53.2&z=15", KLatLon(34.23, -53.2))
		search("ftp://simpleurl?z=15&lat=34.23&lon=-53.2", KLatLon(34.23, -53.2))
	}

	@Test
	fun advancedSpaceSearch() {
		search("5 30 30 N 4 30 W", KLatLon(5.5 + 30 / 3600f, -4.5))
		search("5 30  -4 30", KLatLon(5.5, -4.5))
		search("S 5 30  4 30 W", KLatLon(-5.5, -4.5))
		search("S5.4232  4.30W", KLatLon(-5.4232, -4.3))
		search("S5.4232  W4.30", KLatLon(-5.4232, -4.3))
		search("5.4232, W4.30", KLatLon(5.4232, -4.3))
		search("5.4232N, 45 30.5W", KLatLon(5.4232, -(45 + 30.5 / 60f)))
	}

	@Test
	fun arcgisSpaceSearch() {
		search("43°S 79°23′13.7″W", KLatLon(-43.0, -(79 + 23 / 60f + 13.7 / 3600f)))
		search("43°38′33.24″N 79°23′13.7″W", KLatLon(43 + 38 / 60f + 33.24 / 3600f, -(79 + 23 / 60f + 13.7 / 3600f)))
		search("45° 30'30\"W 3.0", KLatLon(45 + 0.5 + 1 / 120f, -3.0))
		search("43° 79°23′13.7″E", KLatLon(43.0, 79 + 23 / 60f + 13.7 / 3600f))
		search("43°38′ 79°23′13.7″E", KLatLon((43 + 38 / 60f).toDouble(), 79 + 23 / 60f + 13.7 / 3600f))
		search("43°38′23\" 79°23′13.7″E", KLatLon((43 + 38 / 60f + 23 / 3600f).toDouble(), 79 + 23 / 60f + 13.7 / 3600f))
	}

	@Test
	fun commaLatLonSearch() {
		search("(33,95060 °S, 151,14453° E)", KLatLon(-33.95060, 151.14453))
		search("33,95060 °S, 151,14453° E", KLatLon(-33.95060, 151.14453))
		search("33,95060, 151,14453", KLatLon(33.95060, 151.14453))
		search("33,95060 151,14453", KLatLon(33.95060, 151.14453))

		search("15,1235 S, 23,1244 W", KLatLon(-15.1235, -23.1244))
		search("-15,1235, 23,1244", KLatLon(-15.1235, 23.1244))
	}
}
