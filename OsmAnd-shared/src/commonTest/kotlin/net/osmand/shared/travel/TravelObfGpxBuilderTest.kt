package net.osmand.shared.travel

import net.osmand.shared.binary.AmenityIndexRepository
import net.osmand.shared.binary.BinaryMapDataObject
import net.osmand.shared.binary.MapIndex
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KLatLon
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.io.KFile
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiType
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTIntObjectMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What [TravelObfGpxBuilder] makes of what it reads: the track, the waypoints, the tags that end
 * up in the gpx, and the point groups.
 *
 * Its original, `TravelObfGpxFileReader`, lives in the android app, so - as with [TravelModelTest]
 * - there is no java-versus-copy test for it and the expectations below are read off that original
 * by hand. The obf files the tests ship with hold no travel data, so the input here is built by
 * hand as well, through [FakeAmenityIndexRepository].
 */
class TravelObfGpxBuilderTest {

	@Test
	fun trackIsBuiltOutOfSegmentsAndPoints() {
		val routeAmenity = amenity(
			subType = "route_track", name = "Cesta hrdinov SNP",
			tags = mapOf(
				"route_id" to "O7700604",
				"route_bbox_radius" to "C",
				"url" to "https://example.org/snp",
				"url_text" to "SNP",
				"distance" to "736000", // generated, must not reach the gpx
				"user" to "mapper"
			)
		)
		routeAmenity.setName("en", "Path of SNP Heroes")
		val first = waypoint("Station 5", "O7700604")
		val second = waypoint("Station 15", "O7700604")
		val third = waypoint("Station 9", "O7700604")

		val gpxFile = build(
			travelGpx(routeAmenity),
			listOf(routeAmenity, first, second, third),
			listOf(segment(1, "O7700604", 50.0, 14.0, 50.1, 14.1))
		)

		assertNotNull(gpxFile)
		assertEquals(1, gpxFile.tracks.size)
		assertEquals(1, gpxFile.tracks[0].segments.size)
		assertEquals(2, gpxFile.tracks[0].segments[0].points.size)
		assertEquals(50.0, gpxFile.tracks[0].segments[0].points[0].lat, 1e-6)
		assertEquals(14.1, gpxFile.tracks[0].segments[0].points[1].lon, 1e-6)

		// url and url_text become the metadata link and leave the extensions
		assertEquals("https://example.org/snp", gpxFile.metadata.link?.href)
		assertEquals("SNP", gpxFile.metadata.link?.text)
		assertNull(gpxFile.getExtensionsToWrite()["url"])
		assertNull(gpxFile.getExtensionsToWrite()["url_text"])

		// tags that are always generated are dropped, the rest are carried over
		assertNull(gpxFile.getExtensionsToWrite()["distance"])
		assertEquals("mapper", gpxFile.getExtensionsToWrite()["user"])
		assertEquals("Path of SNP Heroes", gpxFile.getExtensionsToWrite()["name:en"])

		// the waypoints come out in natural order, not in the order they were read
		assertEquals(
			listOf("Station 5", "Station 9", "Station 15"),
			gpxFile.getPointsList().map { it.name }
		)
	}

	@Test
	fun trackWithoutSegmentsHasNoTrackAndNoExtensions() {
		val routeAmenity = amenity(
			subType = "route_track", name = "Lonely",
			tags = mapOf("route_id" to "O1", "route_bbox_radius" to "C", "user" to "mapper")
		)
		val gpxFile = build(travelGpx(routeAmenity), listOf(routeAmenity), emptyList())

		assertNotNull(gpxFile)
		assertTrue(gpxFile.tracks.isEmpty())
		// java only writes the collected tags when there is something to hang them on
		assertNull(gpxFile.getExtensionsToWrite()["user"])
	}

	@Test
	fun elevationOfASegmentIsDecoded() {
		val routeAmenity = amenity(
			subType = "route_track", name = "Climb",
			tags = mapOf("route_id" to "O2", "route_bbox_radius" to "C")
		)
		val segment = segment(
			1, "O2", 50.0, 14.0, 50.1, 14.1,
			mapOf(TravelGpx.ELE_GRAPH to "ABAB", TravelGpx.START_ELEVATION to "100.0")
		)

		val gpxFile = build(travelGpx(routeAmenity), listOf(routeAmenity), listOf(segment))

		assertNotNull(gpxFile)
		assertTrue(gpxFile.hasAltitude, "the segment carries an ele_graph")
	}

	@Test
	fun pointGroupsAreRebuilt() {
		val routeAmenity = amenity(
			subType = "route_track", name = "Grouped",
			tags = mapOf(
				"route_id" to "O3",
				"route_bbox_radius" to "C",
				GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_NAMES to "Huts~~~Springs",
				GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_ICONS to "hut~~~water",
				GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_COLORS to "#ff0000~~~#0000ff",
				GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_BACKGROUNDS to "circle~~~octagon"
			)
		)
		val gpxFile = build(
			travelGpx(routeAmenity), listOf(routeAmenity),
			listOf(segment(1, "O3", 50.0, 14.0, 50.1, 14.1))
		)

		assertNotNull(gpxFile)
		assertEquals(listOf("Huts", "Springs"), gpxFile.pointsGroups.keys.toList())
		assertEquals("hut", gpxFile.pointsGroups["Huts"]?.iconName)
		assertEquals("octagon", gpxFile.pointsGroups["Springs"]?.backgroundType)
		// the group tags themselves are not gpx extensions
		assertNull(gpxFile.getExtensionsToWrite()[GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_NAMES])
	}

	@Test
	fun extraTagsAreReadOutOfTheirJson() {
		val routeAmenity = amenity(
			subType = "route_track", name = "Tagged",
			tags = mapOf(
				"route_id" to "O4",
				"route_bbox_radius" to "C",
				TravelObfGpxBuilder.EXTENSIONS_EXTRA_TAGS to "{\"source\":\"osm\",\"level\":\"3\"}",
				TravelObfGpxBuilder.METADATA_EXTRA_TAGS to "{\"copyright\":\"ODbL\"}"
			)
		)
		val gpxFile = build(
			travelGpx(routeAmenity), listOf(routeAmenity),
			listOf(segment(1, "O4", 50.0, 14.0, 50.1, 14.1))
		)

		assertNotNull(gpxFile)
		assertEquals("osm", gpxFile.getExtensionsToWrite()["source"])
		assertEquals("3", gpxFile.getExtensionsToWrite()["level"])
		assertEquals("ODbL", gpxFile.metadata.getExtensionsToWrite()["copyright"])
		assertNull(gpxFile.getExtensionsToWrite()[TravelObfGpxBuilder.EXTENSIONS_EXTRA_TAGS])
	}

	/** Malformed json is logged and skipped, not thrown - a file in the wild may hold anything. */
	@Test
	fun brokenExtraTagsAreSkipped() {
		val routeAmenity = amenity(
			subType = "route_track", name = "Broken",
			tags = mapOf(
				"route_id" to "O5", "route_bbox_radius" to "C",
				TravelObfGpxBuilder.EXTENSIONS_EXTRA_TAGS to "{not json"
			)
		)
		val gpxFile = build(
			travelGpx(routeAmenity), listOf(routeAmenity),
			listOf(segment(1, "O5", 50.0, 14.0, 50.1, 14.1))
		)

		assertNotNull(gpxFile)
		assertNull(gpxFile.getExtensionsToWrite()["not json"])
	}

	@Test
	fun activityIsReadBackOutOfTheSubType() {
		val types = MapPoiTypes(null)
		val category = PoiCategory(types, "routes", 0)
		val hiking = PoiType(types, category, category, "routes_hiking", null)
		hiking.setOsmValue("hiking")
		category.addPoiType(hiking)

		val routeAmenity = amenity(
			subType = "routes_hiking;route_track", name = "Hike",
			tags = mapOf(
				"route_id" to "O6", "route_bbox_radius" to "C",
				TravelGpx.ROUTE_ACTIVITY_TYPE + "_0" to "hiking"
			)
		)
		routeAmenity.setType(category)

		val gpxFile = build(
			travelGpx(routeAmenity), listOf(routeAmenity),
			listOf(segment(1, "O6", 50.0, 14.0, 50.1, 14.1))
		)

		assertNotNull(gpxFile)
		assertEquals("hiking", gpxFile.getExtensionsToWrite()[TravelGpx.ROUTE_TYPE])
	}

	@Test
	fun wikivoyageArticleTakesItsPointsAndItsTitle() {
		val article = TravelArticle()
		article.title = "Prague"
		article.lang = "en"
		article.content = "A city in Bohemia"
		article.routeId = "Q1085"
		article.lat = 50.08
		article.lon = 14.44

		val point = amenity(
			subType = MapPoiTypes.ROUTE_ARTICLE_POINT, name = "Charles Bridge",
			tags = mapOf(
				"route_id" to "Q1085", "route_name" to "Prague",
				Amenity.LANG_YES + ":en" to "yes"
			)
		)
		val otherLang = amenity(
			subType = MapPoiTypes.ROUTE_ARTICLE_POINT, name = "Karlův most",
			tags = mapOf(
				"route_id" to "Q1085", "route_name" to "Prague",
				Amenity.LANG_YES + ":cs" to "yes"
			)
		)

		val gpxFile = build(article, listOf(point, otherLang), emptyList())

		assertNotNull(gpxFile)
		assertEquals("Prague", gpxFile.metadata.getExtensionsToWrite()["article_title"])
		assertEquals("en", gpxFile.metadata.getExtensionsToWrite()["article_lang"])
		// only the points written in the article's language belong to it
		assertEquals(listOf("Charles Bridge"), gpxFile.getPointsList().map { it.name })
	}

	@Test
	fun aCancelledBuildReturnsNothing() {
		val routeAmenity = amenity(
			subType = "route_track", name = "Cancelled", tags = mapOf("route_id" to "O7", "route_bbox_radius" to "C")
		)
		val builder = TravelObfGpxBuilder(FakeTravelObfContext())
		val gpxFile = builder.buildGpxFile(
			listOf(repository(listOf(routeAmenity), emptyList())),
			travelGpx(routeAmenity)
		) { true }

		assertNull(gpxFile)
	}

	/** A file that holds gpx for the whole world is never worth opening for one route. */
	@Test
	fun theWorldFileIsSkipped() {
		val routeAmenity = amenity(
			subType = "route_track", name = "Anywhere", tags = mapOf("route_id" to "O8", "route_bbox_radius" to "C")
		)
		val world = FakeAmenityIndexRepository(
			KFile("World_basemap.obf"), listOf(routeAmenity), emptyList(), worldMap = true
		)
		val builder = TravelObfGpxBuilder(FakeTravelObfContext())
		builder.buildGpxFile(listOf(world.repository), travelGpx(routeAmenity)) { false }

		assertEquals(0, world.poiSearches)
		assertEquals(0, world.nameSearches)
	}

	@Test
	fun deletedObjectsAreRecognised() {
		val mapIndex = MapIndex()
		mapIndex.initMapEncodingRule(0, 1, Amenity.OSM_DELETE_TAG, Amenity.OSM_DELETE_VALUE)
		mapIndex.initMapEncodingRule(0, 2, "highway", "path")
		mapIndex.finishInitializingTags()

		val deleted = BinaryMapDataObject()
		deleted.mapIndex = mapIndex
		deleted.types = intArrayOf(1, 2)
		assertTrue(TravelObfGpxBuilder.isDeletedBinaryMapDataObject(deleted))

		val alive = BinaryMapDataObject()
		alive.mapIndex = mapIndex
		alive.types = intArrayOf(2, 1)
		assertTrue(!TravelObfGpxBuilder.isDeletedBinaryMapDataObject(alive), "only the first type counts")

		val untyped = BinaryMapDataObject()
		untyped.mapIndex = mapIndex
		untyped.types = intArrayOf()
		assertTrue(!TravelObfGpxBuilder.isDeletedBinaryMapDataObject(untyped))
	}

	@Test
	fun trackFilterAlsoTakesRoutesWithAnActivity() {
		val filter = TravelObfGpxBuilder.getSearchFilter(MapPoiTypes.ROUTE_TRACK)
		assertTrue(filter.accept(null, MapPoiTypes.ROUTE_TRACK))
		assertTrue(filter.accept(null, "routes_hiking"))
		assertTrue(filter.accept(null, "route_track;routes_cycling"))
		assertTrue(!filter.accept(null, MapPoiTypes.ROUTE_ARTICLE))
		assertTrue(!filter.isEmpty())

		val articleFilter = TravelObfGpxBuilder.getSearchFilter(MapPoiTypes.ROUTE_ARTICLE)
		assertTrue(articleFilter.accept(null, MapPoiTypes.ROUTE_ARTICLE))
		assertTrue(!articleFilter.accept(null, "routes_hiking"))
	}

	/** Digit runs count as numbers, so a list of numbered stops reads the way it was numbered. */
	@Test
	fun namesWithNumbersAreOrderedByTheNumber() {
		val builder = TravelObfGpxBuilder(FakeTravelObfContext())
		assertTrue(builder.compareWaypointNames("Station 9", "Station 15") < 0)
		assertTrue(builder.compareWaypointNames("Station 15", "Station 9") > 0)
		assertEquals(0, builder.compareWaypointNames("Station 9", "Station 9"))
		assertTrue(builder.compareWaypointNames("Station 007", "Station 8") < 0)
		assertTrue(builder.compareWaypointNames("Alpha", "Beta") < 0)
		assertTrue(builder.compareWaypointNames("Station 2", "Station 2a") < 0)
	}

	private fun build(
		article: TravelArticle, amenities: List<Amenity>, segments: List<BinaryMapDataObject>
	) = TravelObfGpxBuilder(FakeTravelObfContext())
		.buildGpxFile(listOf(repository(amenities, segments)), article) { false }

	private fun repository(
		amenities: List<Amenity>, segments: List<BinaryMapDataObject>
	): AmenityIndexRepository =
		FakeAmenityIndexRepository(KFile("Test.travel.obf"), amenities, segments).repository

	private fun travelGpx(amenity: Amenity): TravelGpx {
		val travelGpx = TravelGpx(amenity)
		travelGpx.file = KFile("Test.travel.obf")
		return travelGpx
	}

	private fun amenity(subType: String, name: String, tags: Map<String, String>): Amenity {
		val amenity = Amenity()
		amenity.setId(tags["route_id"].hashCode().toLong() + name.hashCode())
		amenity.setSubType(subType)
		amenity.setName(name)
		amenity.setLocation(50.05, 14.05)
		amenity.setRegionName("Czech Republic")
		for (entry in tags) {
			amenity.setAdditionalInfo(entry.key, entry.value)
		}
		return amenity
	}

	private fun waypoint(name: String, routeId: String): Amenity =
		amenity(MapPoiTypes.ROUTE_TRACK_POINT, name, mapOf("route_id" to routeId))

	/**
	 * A two point way carrying a route_id, which is how a track sits in the map section. The
	 * route_id and the elevation graph are names of the object, not types, so they go into the
	 * name table - that is where [BinaryMapDataObject.getTagValue] reads them from.
	 */
	private fun segment(
		id: Long, routeId: String, lat1: Double, lon1: Double, lat2: Double, lon2: Double,
		names: Map<String, String> = emptyMap()
	): BinaryMapDataObject {
		val mapIndex = MapIndex()
		mapIndex.initMapEncodingRule(0, 1, "highway", "path")
		val objectNames = KTIntObjectMap<String>()
		val namesOrder = KTIntArrayList()
		var rule = 2
		for ((tag, value) in mapOf(Amenity.ROUTE_ID to routeId) + names) {
			mapIndex.initMapEncodingRule(0, rule, tag, null)
			objectNames.put(rule, value)
			namesOrder.add(rule)
			rule++
		}
		mapIndex.finishInitializingTags()

		val obj = BinaryMapDataObject()
		obj.id = id
		obj.mapIndex = mapIndex
		obj.types = intArrayOf(1)
		obj.objectNames = objectNames
		obj.namesOrder = namesOrder
		obj.coordinates = intArrayOf(
			KMapUtils.get31TileNumberX(lon1), KMapUtils.get31TileNumberY(lat1),
			KMapUtils.get31TileNumberX(lon2), KMapUtils.get31TileNumberY(lat2)
		)
		return obj
	}

	private class FakeTravelObfContext : TravelObfContext {
		override fun getWikivoyageRepositories(): List<AmenityIndexRepository> = emptyList()

		override fun getTravelGpxRepositories(): List<AmenityIndexRepository> = emptyList()

		override fun getLanguage(): String = "en"

		override fun getMapLocation(): KLatLon = KLatLon(50.0, 14.0)

		override fun getAppVersion(): String = "OsmAnd test"
	}
}
