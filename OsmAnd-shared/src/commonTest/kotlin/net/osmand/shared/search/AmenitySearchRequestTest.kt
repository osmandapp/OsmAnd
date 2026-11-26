package net.osmand.shared.search

import net.osmand.shared.binary.AmenityIndexRepository
import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchPoiAdditionalFilter
import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.RenderedObject
import net.osmand.shared.io.KFile
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.edit.EntityType
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.travel.FakeAmenityIndexRepository
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KPriorityQueue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AmenitySearchRequestTest {

	private val location = KLatLon(52.0, 13.0)
	private val settings = AmenitySearcher.Settings({ "de" }, { false }, null)

	@BeforeTest
	fun setUp() {
		SearchPhraseTest.setUp
	}

	@Test
	fun relationAttributesReachAmenitiesOutsideTheNodeRadius() {
		val amenity = amenity(123, EntityType.RELATION, KLatLon(52.003, 13.0))
		val searcher = searcher(listOf(amenity))
		val request = request(osmId = 123, type = EntityType.RELATION)
		val result = assertNotNull(searcher.searchDetailedObject(request, settings, null))
		assertEquals(listOf(amenity), result.getObjects())

		val mapObject = amenity(123, EntityType.RELATION, location)
		assertEquals(result.getObjects(), searcher.searchDetailedObject(AmenitySearcher.Request(mapObject), settings, null)?.getObjects())
		assertNull(searcher.searchDetailedObject(request(osmId = 123, type = EntityType.NODE), settings, null))
		assertNull(searcher.searchDetailedObject(AmenitySearcher.Request(null, location, null, 123, null), settings, null))
	}

	@Test
	fun tagsRecoverAnObjectWithoutAnIdOrName() {
		val amenity = amenity(123)
		val searcher = searcher(listOf(amenity))
		val tags = mapOf("amenity" to "cafe")
		val result = assertNotNull(searcher.searchDetailedObject(request(tags = tags), settings, null))
		assertEquals(listOf(amenity), result.getObjects())
		assertNull(searcher.searchDetailedObject(request(), settings, null))

		val rendered = RenderedObject().apply {
			setLabelLatLon(location)
			putTag("amenity", "cafe")
		}
		assertEquals(result.getObjects(), searcher.searchDetailedObject(AmenitySearcher.Request(rendered), settings, null)?.getObjects())
	}

	@Test
	fun originNameMatchingIsExplicit() {
		val amenity = amenity(123).apply {
			setName("de", "Kaffeehaus")
			setName("en", "Coffee house")
		}
		val names = listOf(amenity.toStringEn())
		val searcher = searcher(listOf(amenity))
		assertNull(searcher.searchDetailedObject(request(names = names), settings, null))
		val result = assertNotNull(searcher.searchDetailedObject(request(names = names, checkOriginName = true), settings, null))
		assertEquals(listOf(amenity), result.getObjects())
		val unnamed = amenity(0).apply { setName("") }
		assertEquals(result.getObjects(), searcher.searchDetailedObject(AmenitySearcher.Request(unnamed, names, true), settings, null)?.getObjects())
	}

	@Test
	fun wikidataFindsAllRepresentationsAndPromotesTheRequestedType() {
		val cafe = amenity(123).apply { setAdditionalInfo("wikidata", "Q123") }
		val restaurant = amenity(456).apply {
			setSubType("restaurant")
			setAdditionalInfo("wikidata", "Q123")
		}
		val unrelated = amenity(789)
		val searcher = searcher(listOf(cafe, unrelated, restaurant))
		val result = assertNotNull(searcher.searchDetailedObject(request(wikidata = "Q123", mainAmenityType = "restaurant"), settings, null))
		assertEquals(listOf(restaurant, cafe), result.getObjects())
		assertEquals("restaurant;cafe", result.getSyntheticAmenity().getSubType())
	}

	@Test
	fun idTakesPrecedenceOverNamesAndNamesCanRecoverAnUnknownId() {
		val selected = amenity(123).apply { setName("Selected") }
		val named = amenity(456).apply { setName("Named") }
		val searcher = searcher(listOf(selected, named))
		assertEquals(listOf(selected), searcher.searchDetailedObject(request(osmId = 123, names = listOf("Named")), settings, null)?.getObjects())
		assertEquals(listOf(named), searcher.searchDetailedObject(request(names = listOf("Named")), settings, null)?.getObjects())
	}

	@Test
	fun aRequestWithoutALocationOrWithCancellationDoesNotSearch() {
		val searcher = AmenitySearcher(MapPoiTypes.getDefault())
		val repository = FakeAmenityIndexRepository(KFile("unused.obf")).repository
		searcher.addAmenityRepository("unused.obf", object : AmenityIndexRepository by repository {
			override fun checkContainsInt(top31: Int, left31: Int, bottom31: Int, right31: Int): Boolean =
				error("The request must not reach a repository")
		})
		val withoutLocation = AmenitySearcher.Request(null, 123, EntityType.NODE, null, null, null, null, false)
		assertNull(searcher.searchDetailedObject(withoutLocation, settings, null))
		assertNull(searcher.searchDetailedObject(request(osmId = 123), settings, object : ResultMatcher<Amenity> {
			override fun publish(obj: Amenity): Boolean = error("A cancelled request must not publish")
			override fun isCancelled(): Boolean = true
		}))
	}

	private fun request(
		osmId: Long = 0, type: EntityType? = null, wikidata: String? = null, names: List<String>? = null,
		tags: Map<String, String>? = null, mainAmenityType: String? = null, checkOriginName: Boolean = false
	) = AmenitySearcher.Request(location, osmId, type, wikidata, names, tags, mainAmenityType, checkOriginName)

	private fun amenity(id: Long, type: EntityType = EntityType.NODE, point: KLatLon = location): Amenity = Amenity().apply {
		setId(ObfConstants.createMapObjectIdFromCleanOsmId(id, type))
		setLocation(point.latitude, point.longitude)
		setName("Cafe")
		setType(MapPoiTypes.getDefault().getPoiCategoryByName("sustenance"))
		setSubType("cafe")
	}

	private fun searcher(amenities: List<Amenity>): AmenitySearcher {
		val repository = FakeAmenityIndexRepository(KFile("requests.obf")).repository
		val searcher = AmenitySearcher(MapPoiTypes.getDefault())
		searcher.addAmenityRepository("requests.obf", object : AmenityIndexRepository by repository {
			override fun searchAmenities(
				stop: Int, sleft: Int, sbottom: Int, sright: Int, zoom: Int,
				filter: SearchPoiTypeFilter?, additionalFilter: SearchPoiAdditionalFilter?,
				matcher: ResultMatcher<Amenity>?, priorityQueue: KPriorityQueue<Amenity>?, priorityQueueLimit: Int
			): List<Amenity> {
				assertNull(additionalFilter)
				assertNull(priorityQueue)
				return amenities.filter { amenity ->
					val point = assertNotNull(amenity.getLocation())
					val x = KMapUtils.get31TileNumberX(point.longitude)
					val y = KMapUtils.get31TileNumberY(point.latitude)
					x in sleft..sright && y in stop..sbottom
							&& filter?.accept(amenity.getType(), amenity.getSubType()!!) != false
							&& matcher?.isCancelled() != true && matcher?.publish(amenity) != false
				}
			}
		})
		return searcher
	}
}
