package net.osmand.shared.binary

import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.util.KMapUtils
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Search by name must not lose a POI inside the request bbox only because the top-left corner of
 * its name index data box is outside the bbox. Fixture: resources/test-resources/poi_name_index/README.md,
 * copied to OsmAnd-java test resources by its collectTestResources task.
 */
class PoiNameIndexBBoxTest {

	/** The poi section decodes its categories through the type registry. */
	@BeforeTest
	fun loadPoiTypes() {
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		types.init()
		MapPoiTypes.setDefault(types)
	}

	private fun <T> withReader(block: (BinaryMapIndexReader) -> T): T {
		val reader = BinaryMapIndexReader(RoutingTestFixtures.resource("poi_name_index/poi_name_index_corner.obf"))
		try {
			return block(reader)
		} finally {
			reader.close()
		}
	}

	@Test
	fun smallBBoxFindsPoiOfDataBoxWithCornerOutside() = withReader { reader ->
		assertEquals(listOf(TARGET_ID), searchSmall(reader))
	}

	@Test
	fun disjointBBoxDoesNotFindPoi() = withReader { reader ->
		// This bbox lies in the control POI tile, east of the target data box.
		assertEquals(emptyList(), search(reader, "Cornerbox", 49.7230, 13.4000, 49.7224, 13.4010))
		assertEquals(listOf(CONTROL_ID), search(reader, "Widebox", 49.7230, 13.4000, 49.7224, 13.4010))
	}

	@Test
	fun wideBBoxFindsControlPoi() = withReader { reader ->
		assertEquals(listOf(TARGET_ID), search(reader, "Cornerbox", 49.7250, 13.3920, 49.7200, 13.4010))
		assertEquals(listOf(CONTROL_ID), search(reader, "Widebox", 49.7250, 13.3920, 49.7200, 13.4010))
	}

	@Test
	fun smallBBoxAfterCacheWarmUp() = withReader { reader ->
		search(reader, "Cornerbox", 49.7250, 13.3920, 49.7200, 13.4010)
		assertEquals(listOf(TARGET_ID), searchSmall(reader))
		assertEquals(listOf(TARGET_ID), searchSmall(reader))
	}

	private fun searchSmall(reader: BinaryMapIndexReader): List<Long?> =
		search(reader, "Cornerbox", 49.7215672, 13.3968384, 49.7201466, 13.3990356)

	private fun search(
		reader: BinaryMapIndexReader, name: String, top: Double, left: Double, bottom: Double, right: Double
	): List<Long?> {
		val l = KMapUtils.get31TileNumberX(left)
		val r = KMapUtils.get31TileNumberX(right)
		val t = KMapUtils.get31TileNumberY(top)
		val b = KMapUtils.get31TileNumberY(bottom)
		// request point = bbox center, as the map layer does
		val req = SearchRequest.buildSearchPoiRequest(l / 2 + r / 2, t / 2 + b / 2, name, l, r, t, b)
		return reader.searchPoiByName(req).map { it.getId() }
	}

	companion object {
		private const val TARGET_ID = 2002L
		private const val CONTROL_ID = 2004L
	}
}
