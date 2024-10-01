package net.osmand.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.filters.BaseTrackFilter
import net.osmand.shared.gpx.filters.FilterType
import net.osmand.shared.gpx.filters.TrackFilterType
import net.osmand.shared.gpx.filters.TrackFiltersHelper.createFilter
import kotlin.test.Test
import kotlin.test.assertTrue

class FiltersTest {

	@Test
	fun testSerialization() {
		val smartFolderCollection: MutableList<SmartFolder> = mutableListOf()
		val folder = SmartFolder("test folder")
		folder.creationTime = currentTimeMillis()
		folder.filters = createFilters()
		smartFolderCollection.add(folder)
		val jsonStr = json.encodeToString(smartFolderCollection)
		val readCollection = json.decodeFromString<List<SmartFolder>?>(jsonStr)
		assertTrue { readCollection != null }
		assertTrue { folder.folderName == readCollection!![0].folderName }
		assertTrue { folder.creationTime == readCollection!![0].creationTime }
		assertTrue { folder.filters == readCollection!![0].filters }
	}

	val json = Json {
		isLenient = true
		ignoreUnknownKeys = true
		classDiscriminator = "className"
	}

	private fun createFilters(): MutableList<BaseTrackFilter> {
		val newFiltersFilters = ArrayList<BaseTrackFilter>()
		for (trackFilterType in TrackFilterType.entries) {
			if (trackFilterType.filterType == FilterType.RANGE ||
				trackFilterType.filterType == FilterType.DATE_RANGE ||
				trackFilterType.filterType == FilterType.OTHER
			) {
				newFiltersFilters.add(createFilter(trackFilterType, null))
			}
		}
		return newFiltersFilters
	}
}