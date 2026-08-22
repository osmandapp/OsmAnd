package net.osmand.shared.gpx.filters

import net.osmand.shared.gpx.GpxDataItem
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.GpxParameter.ACTIVITY_TYPE
import net.osmand.shared.gpx.GpxParameter.COLOR
import net.osmand.shared.gpx.GpxParameter.FILE_DIR
import net.osmand.shared.gpx.GpxParameter.NEAREST_CITY_NAME
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListTrackFilterTest {

	@Test
	fun activityCollectionUsesTrackItemsAndSortsByCount() {
		val filter = createListFilter(TrackFilterType.ACTIVITY)
		val items = listOf(
			createTrackItem("enduro-1", ACTIVITY_TYPE to "enduro_motorcycle"),
			createTrackItem("enduro-2", ACTIVITY_TYPE to "enduro_motorcycle"),
			createTrackItem("hiking", ACTIVITY_TYPE to "hiking"),
			createTrackItem("unspecified")
		)

		filter.setFullItemsCollection(items)

		assertEquals(listOf("enduro_motorcycle", "", "hiking"), filter.allItems)
		assertEquals(2, filter.getTracksCountForItem("enduro_motorcycle"))
		assertEquals(1, filter.getTracksCountForItem("hiking"))
		assertEquals(1, filter.getTracksCountForItem(""))
	}

	@Test
	fun colorCollectionUsesFilterValueConversion() {
		val filter = createListFilter(TrackFilterType.COLOR)
		val color = 0xffe044bb.toInt()
		val colorName = KAlgorithms.colorToString(color)
		val item = createTrackItem("colored", COLOR to color)

		filter.setFullItemsCollection(listOf(item))
		filter.setSelectedItems(listOf(colorName))

		assertEquals(1, filter.getTracksCountForItem(colorName))
		assertTrue(filter.isTrackAccepted(item))
	}

	@Test
	fun cityCollectionExcludesUnspecifiedValues() {
		val filter = createListFilter(TrackFilterType.CITY)
		val items = listOf(
			createTrackItem("unknown-city"),
			createTrackItem("known-city", NEAREST_CITY_NAME to "Chania")
		)

		filter.setFullItemsCollection(items)

		assertEquals(listOf("Chania"), filter.allItems)
		assertFalse(filter.allItemsCollection.containsKey(""))
	}

	@Test
	fun scopedUpdateKeepsVariantsAndSelectedItems() {
		val filter = createListFilter(TrackFilterType.ACTIVITY)
		val enduro = createTrackItem("enduro", ACTIVITY_TYPE to "enduro_motorcycle")
		val hiking = createTrackItem("hiking", ACTIVITY_TYPE to "hiking")
		filter.setFullItemsCollection(listOf(enduro, hiking))
		filter.setSelectedItems(listOf("hiking"))

		filter.updateFullCollection(listOf(enduro))

		assertEquals(listOf("enduro_motorcycle", "hiking"), filter.allItems)
		assertTrue(filter.isItemSelected("hiking"))
		assertEquals(1, filter.getTracksCountForItem("enduro_motorcycle"))
		assertEquals(0, filter.getTracksCountForItem("hiking"))
	}

	@Test
	fun folderCollectionSortsByName() {
		val filter = createListFilter(TrackFilterType.FOLDER)
		val items = listOf(
			createTrackItem("z-1", FILE_DIR to "Zulu"),
			createTrackItem("z-2", FILE_DIR to "Zulu"),
			createTrackItem("a", FILE_DIR to "Alpha")
		)

		filter.setFullItemsCollection(items)

		assertEquals(listOf("Alpha", "Zulu"), filter.allItems)
	}

	private fun createListFilter(type: TrackFilterType): ListTrackFilter {
		return TrackFiltersHelper.createFilter(type, null) as ListTrackFilter
	}

	private fun createTrackItem(
		name: String,
		vararg parameters: Pair<GpxParameter, Any?>
	): TrackItem {
		val file = KFile("/tracks/$name.gpx")
		val dataItem = GpxDataItem.fromDatabase(file)
		dataItem.setParameter(FILE_DIR, "")
		for ((parameter, value) in parameters) {
			assertTrue(dataItem.setParameter(parameter, value))
		}
		return TrackItem(file).apply { this.dataItem = dataItem }
	}
}
