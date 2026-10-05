package net.osmand.shared.travel

import net.osmand.shared.data.Amenity
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.io.KFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What [TravelGpx] and [TravelArticle] read out of an amenity, and what they write into a gpx
 * point.
 *
 * Unlike the other steps of this port, these two have no java-versus-copy test: their originals
 * live in the android app, which the java test module cannot see, so there is nothing to compare
 * against from here. The expectations below are read off those originals by hand, which is worth
 * knowing when one of them changes.
 */
class TravelModelTest {

	@Test
	fun trackIsReadOutOfItsAmenity() {
		val amenity = amenity(
			subType = "routes_hiking;route_track",
			name = "Cesta hrdinov SNP",
			tags = mapOf(
				"name:en" to "Path of SNP Heroes",
				"description" to "A long distance trail",
				"route_id" to "O7700604",
				"user" to "mapper",
				"ref" to "E8",
				"distance" to "736000.5",
				"diff_ele_up" to "12000.25",
				"diff_ele_down" to "11000.75",
				"min_ele" to "150.5",
				"max_ele" to "2043.0",
				"avg_ele" to "900.125",
				"route_bbox_radius" to "C"
			)
		)
		val gpx = TravelGpx(amenity)

		// Java reads the English name into a local and then never uses it: the title it assigns is
		// chosen by "is the title empty", and at this point it always is. Copied as it stands.
		assertEquals("Cesta hrdinov SNP", gpx.title)
		assertEquals(50.1, gpx.lat)
		assertEquals(14.4, gpx.lon)
		assertEquals("A long distance trail", gpx.description)
		assertEquals("O7700604", gpx.routeId)
		assertEquals("mapper", gpx.user)
		assertEquals("E8", gpx.ref)
		assertEquals(736000.5f, gpx.totalDistance)
		assertEquals(12000.25, gpx.diffElevationUp)
		assertEquals(11000.75, gpx.diffElevationDown)
		assertEquals(150.5, gpx.minElevation)
		assertEquals(2043.0, gpx.maxElevation)
		assertEquals(900.125, gpx.avgElevation)
		assertEquals("routes_hiking;route_track", gpx.getAmenitySubType())
		assertEquals("Czech Republic", gpx.getAmenityRegionName())
		assertEquals("hiking", gpx.getRouteType())
		assertTrue(gpx.hasOsmRouteId())
		assertFalse(gpx.isSuperRoute)
		// The radius is written as a letter: A is 5 km, and each letter after it multiplies the
		// previous by two, then by five, alternately - so B is 10 km and C is 50 km.
		assertEquals(50000, gpx.routeRadius)
	}

	@Test
	fun theRadiusLetterIsRead() {
		assertEquals(5000, TravelGpx(amenity(tags = mapOf("route_bbox_radius" to "A"))).routeRadius)
		assertEquals(10000, TravelGpx(amenity(tags = mapOf("route_bbox_radius" to "B"))).routeRadius)
		assertEquals(50000, TravelGpx(amenity(tags = mapOf("route_bbox_radius" to "C"))).routeRadius)
		assertEquals(100000, TravelGpx(amenity(tags = mapOf("route_bbox_radius" to "D"))).routeRadius)
	}

	@Test
	fun trackWithoutARadiusFallsBackToTheDefault() {
		val gpx = TravelGpx(amenity(tags = mapOf("route_id" to "O42")))
		assertEquals(TravelArticle.TRAVEL_GPX_DEFAULT_SEARCH_RADIUS, gpx.routeRadius)

		val noRouteId = TravelGpx(amenity(tags = emptyMap()))
		assertEquals(-1, noRouteId.routeRadius)
	}

	@Test
	fun activityTypeIsFoundUnderItsSuffix() {
		val plain = TravelGpx(amenity(tags = mapOf("route_activity_type" to "hiking")))
		assertEquals("hiking", plain.activityType)

		// when the plain tag is missing, any tag starting with it will do
		val suffixed = TravelGpx(amenity(tags = mapOf("route_activity_type_walking" to "yes")))
		assertEquals("yes", suffixed.activityType)

		val none = TravelGpx(amenity(tags = emptyMap()))
		assertEquals("", none.activityType)
	}

	@Test
	fun aRouteWithMembersIsASuperRoute() {
		val gpx = TravelGpx(amenity(tags = mapOf("route_members_ids" to "O1,O2")))
		assertTrue(gpx.isSuperRoute)
	}

	@Test
	fun shortlinkTilesBecomeTheBox() {
		val without = TravelGpx(amenity(tags = mapOf("route_id" to "O42")))
		assertFalse(without.hasBbox31())
		assertNull(without.getBbox31())

		val gpx = TravelGpx(amenity(tags = mapOf("route_shortlink_tiles" to "0_2NLM,0_2NLN")))
		assertTrue(gpx.hasBbox31())
		val box = gpx.getBbox31()!!
		assertTrue(box.left <= box.right, "left ${box.left} right ${box.right}")
		assertTrue(box.top <= box.bottom, "top ${box.top} bottom ${box.bottom}")
	}

	@Test
	fun trackPointCarriesEveryTagOver() {
		val point = amenity(
			name = "Chata",
			tags = mapOf(
				"name:de" to "Hütte",
				"points_groups_category" to "Shelters",
				"description" to "A hut",
				"note" to "locked in winter",
				"url" to "https://example.org",
				"url_text" to "Example",
				"color" to "red",
				"colour" to "blue",
				"route_id" to "O7700604",
				"route_name" to "Cesta",
				"ele" to "1200",
				"wpt_extra_tags" to """{"operator":"KST","capacity":40}"""
			)
		)
		val wpt = TravelGpx().createWptPt(point, null)

		assertEquals("Chata", wpt.name)
		assertEquals("Shelters", wpt.category)
		assertEquals("A hut", wpt.desc)
		assertEquals("locked in winter", wpt.comment)
		assertEquals("https://example.org", wpt.link?.href)
		assertEquals("Example", wpt.link?.text)

		val extensions = wpt.getExtensionsToWrite()
		assertEquals("Hütte", extensions["name:de"])
		assertEquals("red", extensions["color"])
		// "colour" is dropped when "color" is there as well
		assertFalse(extensions.containsKey("colour"))
		// the route's own tags are not repeated on its points
		assertFalse(extensions.containsKey("route_id"))
		assertFalse(extensions.containsKey("route_name"))
		assertEquals("1200", extensions["ele"])
		// wpt_extra_tags is a json object, and its numbers come over as text
		assertEquals("KST", extensions["operator"])
		assertEquals("40", extensions["capacity"])
		assertFalse(extensions.containsKey("wpt_extra_tags"))
	}

	@Test
	fun brokenExtraTagsLeaveThePointWithoutThem() {
		val point = amenity(tags = mapOf("wpt_extra_tags" to "not json at all", "ele" to "10"))
		val wpt = TravelGpx().createWptPt(point, null)
		assertEquals("10", wpt.getExtensionsToWrite()["ele"])
		assertEquals(1, wpt.getExtensionsToWrite().size)
	}

	@Test
	fun articlePointKeepsOnlyTheWikivoyageTags() {
		val point = amenity(
			name = "Prague Castle",
			tags = mapOf(
				"description" to "The castle",
				"website" to "https://hrad.cz",
				"color" to "purple",
				"gpx_icon" to "special_building",
				"category_see" to "yes",
				"phone" to "+420 1",
				"email" to "a@b.c",
				"opening_hours" to "Mo-Su 09:00-17:00",
				"cuisine" to "czech"
			)
		)
		val wpt = TravelArticle().createWptPt(point, null)

		assertEquals("Prague Castle", wpt.name)
		assertEquals("The castle", wpt.desc)
		assertEquals("https://hrad.cz", wpt.link?.href)
		assertEquals("See", wpt.category)
		assertEquals("special_building", wpt.getIconName())

		val extensions = wpt.getExtensionsToWrite()
		assertEquals("+420 1", extensions["phone"])
		assertEquals("a@b.c", extensions["email"])
		assertEquals("Mo-Su 09:00-17:00", extensions["opening_hours"])
		// not a wikivoyage tag, so it is left behind
		assertFalse(extensions.containsKey("cuisine"))
	}

	@Test
	fun coloursAreTheOnesOsmAndWritesIntoGpx() {
		assertEquals(0xffd00d0d.toInt(), TravelArticle.defaultColor("red"))
		assertEquals(0xff00842b.toInt(), TravelArticle.defaultColor("green"))
		assertEquals(0xff000001.toInt(), TravelArticle.defaultColor("black"))
		assertEquals(0, TravelArticle.defaultColor("no such colour"))
	}

	@Test
	fun filterStringsSayWhichPointsBelongToWhat() {
		assertEquals("route_track", TravelGpx().getMainFilterString())
		assertEquals("route_track_point", TravelGpx().getPointFilterString())
		assertEquals("route_article", TravelArticle().getMainFilterString())
		assertEquals("route_article_point", TravelArticle().getPointFilterString())

		val filter = TravelArticle().getSearchFilter("route_article_point")
		assertTrue(filter.accept(null, "route_article_point"))
		assertFalse(filter.accept(null, "route_track_point"))
		assertFalse(filter.isEmpty())
	}

	@Test
	fun theFileNameIsSafeToSaveUnder() {
		val article = TravelArticle()
		article.title = "A/B: \"C\" <D>"
		assertEquals("A_B_ _C_ _D_", article.getGpxFileName())

		val byRouteId = TravelArticle()
		byRouteId.routeId = "O7700604"
		assertEquals("O7700604", byRouteId.getGpxFileName())

		assertEquals("Travel Article File", TravelArticle().getGpxFileName())
	}

	@Test
	fun theImageUrlPointsAtWikimedia() {
		assertEquals(
			"https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Prague_castle.jpg/" +
					"1280px-Prague_castle.jpg",
			TravelArticle.getImageUrl("Prague castle.jpg", false)
		)
		assertEquals(
			"https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Prague_castle.jpg/" +
					"330px-Prague_castle.jpg",
			TravelArticle.getImageUrl("Prague castle.jpg", true)
		)
		// an svg is shown as the png wikimedia renders for it
		assertTrue(TravelArticle.getImageUrl("Karlův most.svg", false).endsWith(".svg.png"))
	}

	@Test
	fun articlesAreIdentifiedByWhereTheyCameFromAndWhereTheyAre() {
		val article = TravelArticle()
		article.file = KFile("/maps/Slovakia.travel.obf")
		article.lat = 48.1
		article.lon = 17.1
		article.routeId = "O7700604"
		article.routeSource = "osm"
		val id = article.generateIdentifier()

		val same = TravelArticle()
		same.file = KFile("/maps/Slovakia.travel.obf")
		// the same route read out of another file does not land on exactly the same point
		same.lat = 48.100005
		same.lon = 17.100005
		same.routeId = "O7700604"
		same.routeSource = "osm"
		assertEquals(id, same.generateIdentifier())
		// Equality allows the two to be a metre apart, hashCode does not, so these two are equal
		// and hash differently - as they do in java, whose hash is over the raw coordinates too.
		assertNotEquals(id.hashCode(), same.generateIdentifier().hashCode())
		assertEquals(id.hashCode(), article.generateIdentifier().hashCode())

		val elsewhere = TravelArticle()
		elsewhere.file = KFile("/maps/Slovakia.travel.obf")
		elsewhere.lat = 48.2
		elsewhere.lon = 17.1
		elsewhere.routeId = "O7700604"
		elsewhere.routeSource = "osm"
		assertNotEquals(id, elsewhere.generateIdentifier())

		val another = TravelArticle()
		another.file = KFile("/maps/Slovakia.travel.obf")
		another.lat = 48.1
		another.lon = 17.1
		another.routeId = "O99"
		another.routeSource = "osm"
		assertNotEquals(id, another.generateIdentifier())

		// a route with no coordinates at all still matches itself
		val nowhere = TravelArticle().generateIdentifier()
		assertEquals(nowhere, TravelArticle().generateIdentifier())
	}

	@Test
	fun theNumbersComeFromThePoiSectionWhenTheGpxHasNoAltitude() {
		val gpx = TravelGpx(
			amenity(
				tags = mapOf(
					"distance" to "1000",
					"diff_ele_up" to "10",
					"diff_ele_down" to "20",
					"min_ele" to "100",
					"max_ele" to "200",
					"avg_ele" to "150"
				)
			)
		)
		val analysis = gpx.getAnalysis()!!
		assertEquals(1000f, analysis.totalDistance)
		assertEquals(1000f, analysis.totalDistanceWithoutGaps)
		assertEquals(10.0, analysis.diffElevationUp)
		assertEquals(20.0, analysis.diffElevationDown)
		assertEquals(100.0, analysis.minElevation)
		assertEquals(200.0, analysis.maxElevation)
		assertEquals(150.0, analysis.avgElevation)
		assertTrue(analysis.hasData(GpxUtilities.POINT_ELEVATION))
	}

	@Test
	fun theWikivoyageTagListIsWhatItWas() {
		assertTrue(WikivoyageOSMTags.contains("wikidata"))
		assertTrue(WikivoyageOSMTags.contains("phone"))
		assertFalse(WikivoyageOSMTags.contains("cuisine"))
		assertFalse(WikivoyageOSMTags.contains(null))
		assertEquals("opening_hours", WikivoyageOSMTags.TAG_OPENING_HOURS.tag())
	}

	private fun amenity(
		subType: String = "route_track",
		name: String = "",
		tags: Map<String, String> = emptyMap()
	): Amenity {
		val amenity = Amenity()
		amenity.setSubType(subType)
		amenity.setName(name)
		amenity.setLocation(50.1, 14.4)
		amenity.setRegionName("Czech Republic")
		for (entry in tags) {
			amenity.setAdditionalInfo(entry.key, entry.value)
		}
		return amenity
	}
}
