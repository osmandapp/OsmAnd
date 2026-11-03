package net.osmand.shared.binary

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.osmand.shared.binary.GeocodingUtilities.GeocodingResult
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.routing.RoutePlannerFrontEnd
import net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.search.SearchUICoreTest
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.util.KMapUtils
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GeocodingServiceTest {

	private val fs = FileSystem.SYSTEM
	private val registry = ObfReaderRegistry()
	private lateinit var directory: Path
	private lateinit var path: String
	private val config get() = RoutingTestFixtures.defaultBuilder()
	private val service get() = registry.getGeocodingService()
	private var lat = 0.0
	private var lon = 0.0

	@BeforeTest
	fun setUp() {
		SearchPhraseTest.setUp
		directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "geocoding-service-${Random.nextLong().toULong()}"
		fs.createDirectories(directory)
		path = (directory / "vienna.obf").toString()
		restoreMap()
		val reader = BinaryMapIndexReader(path)
		try {
			val streets = ArrayList<Street>()
			reader.searchAddressDataByName(SearchRequest.buildAddressByNameRequest(object : ResultMatcher<MapObject> {
				override fun publish(obj: MapObject): Boolean {
					if (obj is Street) streets.add(obj)
					return false
				}
				override fun isCancelled(): Boolean = false
			}, "gießaufgasse", KStringMatcherMode.CHECK_EQUALS_FROM_SPACE))
			val street = assertNotNull(streets.firstOrNull())
			reader.preloadBuildings(street, null)
			val location = assertNotNull(street.getBuildings().firstOrNull()?.getLocation() ?: street.getLocation())
			lat = location.latitude
			lon = location.longitude
		} finally {
			reader.close()
		}
	}

	@AfterTest
	fun tearDown() {
		registry.close()
		fs.deleteRecursively(directory)
	}

	private fun restoreMap() {
		fs.copy(SearchUICoreTest.obf("austria_wien.obf").toPath(), path.toPath())
		register()
	}

	private fun register() = registry.setFiles(listOf(ObfReaderRegistry.ObfFile(path, false, false)))

	private fun lookup(cancel: ResultMatcher<GeocodingResult>? = null) = service.lookupAddress(lat, lon, config, cancel)

	private fun describe(result: GeocodingResult?): String = result?.let {
		listOf(it.streetName, it.street?.getName(), it.getBuildingString(), it.city?.getName(),
			it.point?.getRoad()?.getId(), it.getDistance()).joinToString("|")
	} ?: "none"

	private fun matcher(cancel: () -> Boolean) = object : ResultMatcher<GeocodingResult> {
		override fun publish(obj: GeocodingResult): Boolean = error("The lookup must return its result")
		override fun isCancelled(): Boolean = cancel()
	}

	@Test
	fun repeatedLookupsReuseReadersAndMatchTheGeocodingProfile() {
		assertSame(service, registry.getGeocodingService())
		val reader = BinaryMapIndexReader(path)
		try {
			val context = RoutePlannerFrontEnd().buildRoutingContext(config.build("geocoding", RoutingMemoryLimits(10, 10)), listOf(reader))
			val utilities = GeocodingUtilities()
			val expected = utilities.findAddresses(listOf(reader), utilities.reverseGeocodingSearch(context, lat, lon, false)).firstOrNull()
			assertNotNull(expected)
			assertEquals(describe(expected), describe(lookup()))
			val resource = registry.getFileReader(path)!!
			val opened = resource.getReader(BinaryMapReaderResourceType.STREET_LOOKUP)
			register()
			assertEquals(describe(expected), describe(lookup()))
			assertSame(opened, resource.getReader(BinaryMapReaderResourceType.STREET_LOOKUP))
		} finally {
			reader.close()
		}
	}

	@Test
	fun removalAndReplacementInvalidateRoadTiles() {
		val expected = describe(assertNotNull(lookup()))
		val before = registry.getFileReader(path)
		registry.setFiles(emptyList())
		assertNull(lookup(), "cached roads from a removed map must not supply an address")
		register()
		assertNotSame(before, registry.getFileReader(path))
		assertEquals(expected, describe(lookup()))
		fs.write(path.toPath()) { writeUtf8("not an obf") }
		register()
		assertNull(lookup())
		restoreMap()
		assertEquals(expected, describe(lookup()))
	}

	@Test
	fun aMapClosedDuringLookupDoesNotPublishCachedResults() {
		assertNotNull(lookup())
		var polls = 0
		assertNull(lookup(matcher {
			if (++polls == 3) registry.setFiles(emptyList())
			false
		}))
		register()
		assertNotNull(lookup(), "a failed lookup must not poison the next context")
	}

	@Test
	fun cancellationAtEveryPollReturnsNoAddressAndLeavesTheNextLookupUsable() {
		var polls = 0
		val expected = describe(assertNotNull(lookup(matcher { polls++; false })))
		assertTrue(polls > 4, "the real address lookup must reach cancellation checks inside the reader")
		for (cancelAt in 1..polls) {
			var current = 0
			assertNull(lookup(matcher { ++current >= cancelAt }), "cancelled at poll $cancelAt")
		}
		assertEquals(expected, describe(lookup()))
	}

	@Test
	fun concurrentCallersGetTheSameAnswersAsSequentialLookups() = runBlocking {
		val points = listOf(lat to lon, (lat + .001) to lon, lat to (lon + .001))
		val expected = points.map { (lat, lon) -> describe(service.lookupAddress(lat, lon, config, null)) }
		assertTrue(expected.none { it == "none" })
		val start = CompletableDeferred<Unit>()
		val tasks = (0 until 6).map { worker ->
			async(Dispatchers.Default) {
				start.await()
				repeat(4) { round ->
					val index = (worker + round) % points.size
					val (lat, lon) = points[index]
					assertEquals(expected[index], describe(service.lookupAddress(lat, lon, config, null)))
				}
			}
		}
		start.complete(Unit)
		tasks.awaitAll()
		Unit
	}

	@Test
	fun noCoveringMapReturnsNoAddress() {
		assertNull(service.lookupAddress(-45.0, -130.0, config, null))
		assertNotNull(lookup())
		assertNull(service.lookupAddress(-45.0, -130.0, config, null))
	}

	@Test
	fun aRoadOnlyMapIsNotUsedForAddressLookups() {
		val roadOnly = RoutingTestFixtures.resource("routing/Routing_test_75.obf")
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(roadOnly, true, false)))
		val header = assertNotNull(registry.getFileReader(roadOnly)?.getShallowReader())
		assertTrue(header.containsRouteData())
		assertFalse(header.containsAddressData())
		val reader = assertNotNull(registry.getFileReader(roadOnly)?.getReader(BinaryMapReaderResourceType.REVERSE_GEOCODING))
		val request = SearchRequest.buildSearchRouteRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE)
		val road = reader.getRoutingIndexes().flatMap { reader.searchRouteIndexTree(request, it.getSubregions()) }
			.flatMap { reader.loadRouteIndexData(it) }.filterNotNull()
			.first { !it.getName().isNullOrEmpty() && it.getPointsLength() > 1 }
		val point = (0 until road.getPointsLength()).first {
			header.containsRouteData(road.getPoint31XTile(it), road.getPoint31YTile(it), road.getPoint31XTile(it), road.getPoint31YTile(it))
		}
		val lat = KMapUtils.get31LatitudeY(road.getPoint31YTile(point))
		val lon = KMapUtils.get31LongitudeX(road.getPoint31XTile(point))
		assertNull(service.lookupAddress(lat, lon, config, null))
	}
}
