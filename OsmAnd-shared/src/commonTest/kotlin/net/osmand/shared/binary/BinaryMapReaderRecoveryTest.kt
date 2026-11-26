package net.osmand.shared.binary

import net.osmand.shared.routing.RouteRegion
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.search.core.SearchPhraseTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A failed lazy read must leave shared metadata untouched and allow the same reader to retry. */
class BinaryMapReaderRecoveryTest {
	private val fs = FileSystem.SYSTEM
	private val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "obf-recovery-${Random.nextLong().toULong()}.obf"
	private lateinit var bytes: ByteArray
	private lateinit var resource: BinaryMapReaderResource
	private lateinit var reader: BinaryMapIndexReader
	private lateinit var other: BinaryMapIndexReader
	private lateinit var expected: BinaryMapIndexReader

	@BeforeTest
	fun open() {
		SearchPhraseTest.setUp
		val fixture = RoutingTestFixtures.resource("Turn_lanes_test.obf")
		bytes = fs.read(fixture.toPath()) { readByteArray() }
		fs.write(path) { write(bytes) }
		resource = BinaryMapReaderResource(path.toString())
		reader = assertNotNull(resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH))
		other = assertNotNull(resource.getReader(BinaryMapReaderResourceType.POI))
		expected = BinaryMapIndexReader(fixture)
	}

	@AfterTest
	fun close() {
		if (::resource.isInitialized) resource.close()
		if (::expected.isInitialized) expected.close()
		fs.delete(path, mustExist = false)
	}

	private fun failThenRestore(cut: Long, read: () -> Unit, unchanged: () -> Unit) {
		// Discard bytes buffered before truncation, so the read must reach the damaged file.
		reader.codedIS.seek(bytes.size.toLong() + 1)
		val limit = reader.codedIS.getTotalBytesRead() + reader.codedIS.getBytesUntilLimit()
		fs.write(path) { write(bytes, 0, cut.toInt()) }
		try {
			assertFails { read() }
			assertEquals(limit, reader.codedIS.getTotalBytesRead() + reader.codedIS.getBytesUntilLimit(), "outer limit")
			unchanged()
		} finally {
			fs.write(path) { write(bytes) }
		}
		read()
	}

	@Test
	fun poiTablesArePublishedOnlyAfterACompleteRead() {
		val region = reader.getPoiIndexes().first()
		val adapter = BinaryMapPoiReaderAdapter(reader)
		val reference = expected.getPoiIndexes().first()
		BinaryMapPoiReaderAdapter(expected).initCategories(reference)
		failThenRestore(region.filePointer + region.length / 8, { adapter.initCategories(region) }) {
			assertTrue(region.categories.isEmpty())
			assertTrue(region.categoriesType.isEmpty())
			assertTrue(region.subcategories.isEmpty())
			assertTrue(region.categoryFreqs.isEmpty())
			assertTrue(region.subcategoryFreqs.isEmpty())
			assertTrue(region.subTypes.isEmpty())
			assertTrue(region.topIndexSubTypes.isEmpty())
		}
		assertEquals(reference.categories, region.categories)
		assertEquals(reference.subcategories, region.subcategories)
		assertEquals(reference.subTypes.map { it.name }, region.subTypes.map { it.name })
		assertEquals(expected.searchPoiCategoriesByName("a", HashMap()), other.searchPoiCategoriesByName("a", HashMap()))
	}

	private fun mapObjects(r: BinaryMapIndexReader, zoom: Int = 15): List<Long> =
		r.searchMapIndex(SearchRequest.buildSearchRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE, zoom, null), r.getMapIndexes().first())
			.map { it.id }

	private fun mapRules(index: MapIndex): List<Any> = listOf(
		index.nameEncodingType, index.nameEnEncodingType, index.refEncodingType,
		index.coastlineEncodingType, index.coastlineBrokenEncodingType, index.landEncodingType,
		index.onewayAttribute, index.onewayReverseAttribute, index.encodingRulesSizeBytes,
		index.positiveLayers.toSet(), index.negativeLayers.toSet(),
		index.decodingRules.keys().sorted().map { id ->
			val rule = index.decodingRules[id]!!
			listOf(id, rule.tag, rule.value)
		}
	)

	@Test
	fun mapRulesAndSpecialIdsArePublishedTogether() {
		val region = reader.getMapIndexes().first()
		val before = mapRules(region)
		val wanted = mapObjects(expected)
		val reference = expected.getMapIndexes().first()
		assertTrue(reference.encodingRulesSizeBytes > 0)
		failThenRestore(region.filePointer + reference.encodingRulesSizeBytes / 2, { mapObjects(reader) }) {
			assertTrue(region.encodingRules.isEmpty())
			assertEquals(before, mapRules(region))
		}
		assertEquals(mapRules(reference), mapRules(region))
		assertEquals(wanted, mapObjects(reader))
		assertEquals(wanted, mapObjects(other))
	}

	@Test
	fun mapTreesArePublishedOnlyAfterACompleteRead() {
		val region = reader.getMapIndexes().first()
		val root = region.getRoots().first()
		val wanted = mapObjects(expected, root.minZoom)
		val referenceRoot = expected.getMapIndexes().first().getRoots().first()
		val tree = assertNotNull(referenceRoot.trees).first()
		// Initialise the rule table without selecting a zoom level.
		mapObjects(reader, -1)
		assertNull(root.trees)
		failThenRestore(tree.filePointer + tree.length / 2, { mapObjects(reader, root.minZoom) }) {
			assertNull(root.trees)
		}
		assertEquals(referenceRoot.trees!!.size, root.trees!!.size)
		assertEquals(wanted, mapObjects(reader, root.minZoom))
		assertEquals(wanted, mapObjects(other, root.minZoom))
	}

	private fun routeRules(region: RouteRegion): List<Any?> = listOf(
		region.directionForward, region.directionBackward, region.maxheightForward, region.maxheightBackward,
		region.directionTrafficSignalsForward, region.directionTrafficSignalsBackward,
		region.trafficSignals, region.stopSign, region.stopMinor, region.giveWaySign,
		region.nameTypeRule, region.refTypeRule, region.destinationTypeRule, region.destinationRefTypeRule,
		region.routeEncodingRulesBytes, region.routeEncodingRules.map { it?.let { rule -> rule.getTag() to rule.getValue() } }
	)

	@Test
	fun routingRulesArePublishedOnlyAfterACompleteRead() {
		val reference = expected.getRoutingIndexes().first()
		// A section whose rules have not been loaded yet, with the same file position as the header.
		val region = RouteRegion().apply {
			filePointer = reference.filePointer
			length = reference.length
		}
		val before = routeRules(region)
		val adapter = BinaryMapRouteReaderAdapter(reader)
		failThenRestore(region.filePointer + reference.routeEncodingRulesBytes / 2, { adapter.initRouteRegion(region) }) {
			assertEquals(before, routeRules(region))
			assertEquals(-1, region.searchRouteEncodingRule("name", null))
		}
		assertEquals(routeRules(reference), routeRules(region))
		assertEquals(reference.searchRouteEncodingRule("name", null), region.searchRouteEncodingRule("name", null))
	}

	private fun routeBoxes(r: BinaryMapIndexReader): List<Long> = r.searchRouteIndexTree(
		SearchRequest.buildSearchRouteRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE), r.getRoutingIndexes().first().getSubregions()
	).map { it.filePointer }

	@Test
	fun routingTreesArePublishedOnlyAfterACompleteRead() {
		val region = reader.getRoutingIndexes().first()
		val root = region.getSubregions().first()
		val wanted = routeBoxes(expected)
		val referenceRoot = expected.getRoutingIndexes().first().getSubregions().first()
		val child = assertNotNull(referenceRoot.subregions).first()
		val before = region.regionsRead
		val shift = root.shiftToData
		assertNull(root.subregions)
		failThenRestore(child.filePointer + child.length / 2, { routeBoxes(reader) }) {
			assertNull(root.subregions)
			assertEquals(shift, root.shiftToData)
			assertEquals(before, region.regionsRead)
		}
		assertEquals(wanted, routeBoxes(reader))
		assertEquals(wanted, routeBoxes(other))
	}
}
