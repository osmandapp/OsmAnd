package net.osmand.shared.travel

import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KLatLon
import net.osmand.shared.io.KFile
import net.osmand.shared.osm.MapPoiTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What [TravelObfHelper] offers, finds and orders.
 *
 * Its original lives in the android app and needs an `OsmandApplication`, so - as with
 * [TravelModelTest] and [TravelObfGpxBuilderTest] - there is no java-versus-copy test and the
 * expectations below are read off that original by hand. The files are stood in for by
 * [FakeAmenityIndexRepository], because no obf the tests ship with holds travel data.
 */
class TravelObfHelperTest {

	@Test
	fun offeredArticlesAreTheNearestOnes() {
		val helper = helper(wikivoyage = listOf(repo(
			article("Vienna", 48.20, 16.37, "Q1741"),
			article("Prague", 50.08, 14.44, "Q1085"),
			article("Dresden", 51.05, 13.74, "Q1731"),
			article("Munich", 48.14, 11.58, "Q1726")
		)))

		helper.initializeDataToDisplay(true)

		assertEquals(listOf("Prague", "Dresden", "Vienna"), helper.getPopularArticles().map { it.title })
	}

	/**
	 * Java stops one short of the end - `foundAmenitiesIndex < foundAmenities.size() - 1` - so the
	 * last amenity found waits for the next widening of the search. Copied as it stands: against a
	 * real file hundreds are found at a time and nobody notices.
	 */
	@Test
	fun theLastAmenityFoundIsLeftForTheNextPass() {
		val helper = helper(wikivoyage = listOf(repo(
			article("Prague", 50.08, 14.44, "Q1085"),
			article("Dresden", 51.05, 13.74, "Q1731")
		)))

		helper.initializeDataToDisplay(true)

		assertEquals(listOf("Prague"), helper.getPopularArticles().map { it.title })
	}

	/** A track has to be five times nearer than an article to be offered before it. */
	@Test
	fun articlesAreOfferedBeforeTracksOfTheSameDistance() {
		val helper = helper(wikivoyage = listOf(repo(
			article("Prague", 50.80, 14.44, "Q1085"), // ~89 km away, counted as ~18 km
			track("Ridge walk", 50.30, 14.10, "O11"), // ~34 km away, counted in full
			article("Vienna", 48.20, 16.37, "Q1741") // far enough to stay last
		)))

		helper.initializeDataToDisplay(true)

		assertEquals(listOf("Prague", "Ridge walk"), helper.getPopularArticles().map { it.title })
	}

	@Test
	fun anArticleWithoutANameInTheLanguageIsNotOffered() {
		val czechOnly = amenity(MapPoiTypes.ROUTE_ARTICLE, "Plzeň", 50.07, 13.38,
			mapOf("route_id" to "Q43453", "description:cs" to "Město v Čechách"))
		val helper = helper(wikivoyage = listOf(repo(czechOnly)))

		helper.initializeDataToDisplay(true)

		assertTrue(helper.getPopularArticles().isEmpty())
	}

	@Test
	fun searchPutsAnExactTitleFirst() {
		val helper = helper(
			wikivoyage = listOf(
				repo(
					article("Prague Castle", 50.09, 14.40, "Q2"),
					article("Prague", 50.08, 14.44, "Q1"),
					article("Pragueville", 50.07, 14.41, "Q3")
				)
			)
		)

		val results = helper.search("Prague", 0)

		assertEquals(listOf("Prague", "Prague Castle", "Pragueville"), results.map { it.getArticleTitle() })
	}

	@Test
	fun searchOffersTheLanguagesOfAnArticleWithTheAppLanguageFirst() {
		val amenity = amenity(MapPoiTypes.ROUTE_ARTICLE, "Prag", 50.08, 14.44, mapOf(
			"route_id" to "Q1085",
			"description:en" to "A city",
			"description:de" to "Eine Stadt",
			"description:cs" to "Město"
		))
		amenity.setName("de", "Prag")
		val helper = helper(wikivoyage = listOf(repo(amenity)), language = "de")

		val results = helper.search("Prag", 0)

		assertEquals(1, results.size)
		assertEquals(listOf("de", "en", "cs"), results[0].langs)
		assertEquals("De, En, Cs", results[0].getFirstLangsString())
	}

	/** Every amenity of one route is one result, however many files it was found in. */
	@Test
	fun searchReturnsEachRouteOnce() {
		val one = article("Prague", 50.08, 14.44, "Q1085")
		val copyInAnotherFile = article("Prague", 50.08, 14.44, "Q1085")
		val helper = helper(
			wikivoyage = listOf(repo(one, name = "A"), repo(copyInAnotherFile, name = "B"))
		)

		assertEquals(1, helper.search("Prague", 0).size)
	}

	/** A search whose number is no longer the current one hands back nothing. */
	@Test
	fun aStaleSearchIsDropped() {
		val helper = helper(wikivoyage = listOf(repo(article("Prague", 50.08, 14.44, "Q1085"))))
		helper.requestNumber = 7

		assertTrue(helper.search("Prague", 6).isEmpty())
		assertEquals(1, helper.search("Prague", 7).size)
	}

	@Test
	fun travelGpxTagsAreRecognised() {
		val helper = helper()

		assertTrue(helper.isTravelGpxTags(mapOf("route_id" to "O1", "route" to "segment")))
		assertTrue(helper.isTravelGpxTags(mapOf("route_id" to "O1", "route_type" to "hiking")))
		assertTrue(!helper.isTravelGpxTags(mapOf("route_id" to "O1")))
		assertTrue(!helper.isTravelGpxTags(mapOf("route" to "segment")))
	}

	@Test
	fun aTrackIsFoundByItsRouteId() {
		val helper = helper(travelGpx = listOf(repo(track("Ridge walk", 50.05, 14.05, "O11"))))

		val found = helper.searchTravelGpx(KLatLon(50.05, 14.05), "O11")

		assertNotNull(found)
		assertEquals("Ridge walk", found.title)
		assertEquals("O11", found.routeId)
	}

	@Test
	fun anUnknownRouteIdFindsNothing() {
		val helper = helper(travelGpx = listOf(repo(track("Ridge walk", 50.05, 14.05, "O11"))))

		assertNull(helper.searchTravelGpx(KLatLon(50.05, 14.05), "O99"))
		assertNull(helper.searchTravelGpx(KLatLon(50.05, 14.05), ""))
	}

	/**
	 * Reading a map section reads its encoding rules first, so a file the tap cannot be in must not
	 * be asked at all - that is what a tap used to pay for every installed map.
	 */
	@Test
	fun aFileTheTapIsNotInIsNotRead() {
		val near = FakeAmenityIndexRepository(KFile("Near.obf"))
		val far = FakeAmenityIndexRepository(KFile("Far.obf"), coversMapSection = false)
		val helper = TravelObfHelper(
			FakeTravelObfContext(travelGpx = listOf(near.repository, far.repository))
		)

		helper.searchTravelGpxByRouteTypes(KLatLon(50.05, 14.05), setOf("hiking"))

		assertEquals(1, near.mapSearches)
		assertEquals(0, far.mapSearches)
	}

	/** The world file covers everything and holds no tracks, so it is never searched. */
	@Test
	fun theWorldFileIsSkippedWhenLookingForATrack() {
		val world = FakeAmenityIndexRepository(
			KFile("World_basemap.obf"), listOf(track("Ridge walk", 50.05, 14.05, "O11")),
			worldMap = true
		)
		val helper = TravelObfHelper(FakeTravelObfContext(travelGpx = listOf(world.repository)))

		assertNull(helper.searchTravelGpx(KLatLon(50.05, 14.05), "O11"))
		assertEquals(0, world.nameSearches)
		assertEquals(0, world.poiSearches)
	}

	@Test
	fun anArticleIsFoundByItsTitle() {
		val helper = helper(wikivoyage = listOf(repo(article("Prague", 50.08, 14.44, "Q1085"))))

		val found = helper.getArticleByTitle("Prague", "en", false)

		assertNotNull(found)
		assertEquals("Prague", found.title)
		assertEquals("Q1085", found.routeId)
		assertEquals("en", found.lang)
		assertNull(helper.getArticleByTitle("Brno", "en", false))
	}

	@Test
	fun theLanguagesOfAnArticleAreListed() {
		val amenity = amenity(MapPoiTypes.ROUTE_ARTICLE, "Prague", 50.08, 14.44, mapOf(
			"route_id" to "Q1085",
			"description:en" to "A city",
			"is_part:de" to "Böhmen"
		))
		val helper = helper(wikivoyage = listOf(repo(amenity)))

		val article = helper.getArticleByTitle("Prague", "en", false)
		assertNotNull(article)
		val langs = helper.getArticleLangs(article.generateIdentifier())

		assertEquals(setOf("en", "de"), langs.toSet())
	}

	@Test
	fun theNavigationMapHangsChildrenUnderTheirParent() {
		val bohemia = amenity(MapPoiTypes.ROUTE_ARTICLE, "Bohemia", 50.00, 14.50, mapOf(
			"route_id" to "Q39193",
			"description:en" to "A region",
			"is_parent_of:en" to "Prague;Plzeň;"
		))
		val prague = amenity(MapPoiTypes.ROUTE_ARTICLE, "Prague", 50.08, 14.44, mapOf(
			"route_id" to "Q1085",
			"description:en" to "A city",
			"is_aggr_part:en" to "Bohemia"
		))
		val helper = helper(wikivoyage = listOf(repo(bohemia, prague)))

		val article = helper.getArticleByTitle("Prague", "en", false)
		assertNotNull(article)
		val navigation = helper.getNavigationMap(article)

		assertEquals(listOf("Bohemia"), navigation.keys.map { it.getArticleTitle() })
		assertEquals(
			listOf("Plzeň", "Prague"),
			navigation.values.first().map { it.getArticleTitle() }
		)
	}

	@Test
	fun anArticleWithNoParentHasNoNavigation() {
		val helper = helper(wikivoyage = listOf(repo(article("Prague", 50.08, 14.44, "Q1085"))))
		val article = helper.getArticleByTitle("Prague", "en", false)

		assertNotNull(article)
		assertTrue(helper.getNavigationMap(article).isEmpty())
	}

	/** When the file an article came from is gone, the saved copy stands in for it. */
	@Test
	fun aSavedArticleIsUsedWhenNothingIsOpen() {
		val saved = TravelArticle()
		saved.title = "Prague"
		saved.routeId = "Q1085"
		saved.lang = "en"
		val bookmarks = RecordingBookmarks(saved)
		val helper = TravelObfHelper(FakeTravelObfContext(), bookmarks)

		val found = helper.getArticleById(saved.generateIdentifier(), "en", false)

		assertNotNull(found)
		assertEquals("Prague", found.title)

		helper.saveOrRemoveArticle(saved, true)
		assertEquals(listOf("add"), bookmarks.calls)
		helper.saveOrRemoveArticle(saved, false)
		assertEquals(listOf("add", "remove"), bookmarks.calls)
	}

	@Test
	fun gpxNamesEndInTheExtension() {
		val helper = helper()
		val article = TravelArticle()
		article.title = "Prague: a walk"

		// a colon cannot go into a file name, so it becomes an underscore
		assertEquals("Prague_ a walk.gpx", helper.getGPXName(article))
		assertEquals("World_wikivoyage.travel.obf", helper.getWikivoyageFileName())
	}

	@Test
	fun withoutFilesNothingIsOffered() {
		val helper = helper()

		helper.initializeDataToDisplay(true)

		assertTrue(!helper.isAnyTravelBookPresent())
		assertTrue(helper.getPopularArticles().isEmpty())
		assertTrue(helper.search("Prague", 0).isEmpty())
	}

	private fun helper(
		wikivoyage: List<net.osmand.shared.binary.AmenityIndexRepository> = emptyList(),
		travelGpx: List<net.osmand.shared.binary.AmenityIndexRepository> = emptyList(),
		language: String = "en"
	) = TravelObfHelper(FakeTravelObfContext(wikivoyage, travelGpx, language))

	/** One obf file holding these amenities. */
	private fun repo(
		vararg amenities: Amenity, name: String = "Test"
	): net.osmand.shared.binary.AmenityIndexRepository =
		FakeAmenityIndexRepository(KFile("$name.travel.obf"), amenities.toList()).repository

	private fun article(title: String, lat: Double, lon: Double, routeId: String): Amenity =
		amenity(MapPoiTypes.ROUTE_ARTICLE, title, lat, lon, mapOf(
			"route_id" to routeId, "description:en" to "About $title"
		))

	private fun track(title: String, lat: Double, lon: Double, routeId: String): Amenity =
		amenity(MapPoiTypes.ROUTE_TRACK, title, lat, lon, mapOf(
			"route_id" to routeId, "route_bbox_radius" to "C", "description:en" to "About $title"
		))

	private fun amenity(
		subType: String, name: String, lat: Double, lon: Double, tags: Map<String, String>
	): Amenity {
		val amenity = Amenity()
		amenity.setId(name.hashCode().toLong())
		amenity.setSubType(subType)
		amenity.setName(name)
		amenity.setName("en", name)
		amenity.setLocation(lat, lon)
		amenity.setRegionName("Czech Republic")
		for (entry in tags) {
			amenity.setAdditionalInfo(entry.key, entry.value)
		}
		return amenity
	}

	/** Storage that holds one article and remembers what was asked of it. */
	private class RecordingBookmarks(private val saved: TravelArticle) : TravelBookmarks {
		val calls = ArrayList<String>()

		override fun refreshCachedData() {
			calls.add("refresh")
		}

		override fun getSavedArticle(file: KFile?, routeId: String?, lang: String?): TravelArticle? =
			if (routeId == saved.routeId) saved else null

		override fun getSavedArticles(file: KFile?, routeId: String?): List<TravelArticle> =
			if (routeId == saved.routeId) listOf(saved) else emptyList()

		override fun addArticleToSaved(article: TravelArticle) {
			calls.add("add")
		}

		override fun removeArticleFromSaved(article: TravelArticle) {
			calls.add("remove")
		}
	}
}
