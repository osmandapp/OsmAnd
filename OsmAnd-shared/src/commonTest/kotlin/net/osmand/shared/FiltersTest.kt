package net.osmand.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.filters.BaseTrackFilter
import net.osmand.shared.gpx.filters.FilterType
import net.osmand.shared.gpx.filters.ListTrackFilter
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

	val oldFilters = "[{\"creationTime\":1711549807641,\"filters\":[{\"valueFrom\":1388586638305,\"valueTo\":1420036231189,\"filterType\":\"DATE_CREATION\"}],\"folderName\":\"2014\"},{\"creationTime\":1713438258313,\"filters\":[{\"isSelectAllItemsSelected\":true,\"selectedItems\":[\"Göynük\"],\"filterType\":\"CITY\"}],\"folderName\":\"Turkey \"},{\"creationTime\":1715850761874,\"filters\":[{\"maxValue\":83.33332824707031,\"minValue\":0.0,\"valueFrom\":0.0,\"valueTo\":23.33333396911621,\"filterType\":\"AVERAGE_SPEED\"}],\"folderName\":\"Crash\"}]"
//	val oldFilters = "[{\"creationTime\":1711549807641,\"filters\":[{\"valueFrom\":1388586638305,\"valueTo\":1420036231189,\"filterType\":\"DATE_CREATION\"}],\"folderName\":\"2014\"},{\"creationTime\":1713438258313,\"filters\":[{\"isSelectAllItemsSelected\":false,\"selectedItems\":[\"Göynük\"],\"filterType\":\"CITY\"}],\"folderName\":\"Turkey \"},{\"creationTime\":1715850761874,\"filters\":[{\"maxValue\":83.33332824707031,\"minValue\":0.0,\"valueFrom\":0.0,\"valueTo\":23.33333396911621,\"filterType\":\"AVERAGE_SPEED\"}],\"folderName\":\"Crash\"}] cached=[{\"folderName\":\"2014\",\"creationTime\":1711549807641,\"filters\":[{\"className\":\"net.osmand.shared.gpx.filters.DateTrackFilter\",\"filterType\":\"DATE_CREATION\",\"valueFrom\":1251682533000,\"valueTo\":1730300878211}]},{\"folderName\":\"Turkey \",\"creationTime\":1713438258313,\"filters\":[{\"className\":\"net.osmand.shared.gpx.filters.ListTrackFilter\",\"filterType\":\"CITY\",\"selectedItems\":[\"Göynük\"],\"allItems\":[\"Göynük\"],\"allItemsCollection\":{\"Göynük\":0}}]},{\"folderName\":\"Crash\",\"creationTime\":1715850761874,\"filters\":[{\"className\":\"RangeTrackFilter\",\"filterType\":\"AVERAGE_SPEED\",\"minValue\":0.0,\"maxValue\":83.33332824707031,\"valueFrom\":0.0,\"valueTo\":23.33333396911621}]},{\"folderName\":\"Kyiv\",\"creationTime\":1728309759264,\"filters\":[{\"className\":\"net.osmand.shared.gpx.filters.ListTrackFilter\",\"filterType\":\"CITY\",\"selectedItems\":[\"Київ\"],\"allItems\":[\"Київ\",\"Amstelveen\",\"Berlin\",\"Aosta - Aoste\",\"Leiden\",\"Bolzano - Bozen\",\"Dresden\",\"Christchurch\",\"בית שמש\",\"Владимир\",\"Waltrop\",\"Waltham\",\"Pamplona / Iruña\",\"Marlborough\",\"Göynük\",\"Firenze\",\"Falkensee\",\"Emsdetten\",\"Cuneo\",\"Ahrensdorf\"],\"allItemsCollection\":{\"Cuneo\":1,\"Christchurch\":2,\"Marlborough\":1,\"Dresden\":2,\"Berlin\":6,\"Pamplona / Iruña\":1,\"Amstelveen\":8,\"Aosta - Aoste\":4,\"Leiden\":3,\"Waltrop\":1,\"Waltham\":1,\"Falkensee\":1,\"Київ\":21,\"Firenze\":1,\"Bolzano - Bozen\":3,\"בית שמש\":1,\"Göynük\":1,\"Ahrensdorf\":1,\"Emsdetten\":1,\"Владимир\":1}}]}]"
//	val oldFilters = "[{\"creationTime\":1711549807641,\"filters\":[{\"valueFrom\":1388586638305,\"valueTo\":1420036231189,\"filterType\":\"DATE_CREATION\"}],\"folderName\":\"2014\"},{\"creationTime\":1713438258313,\"filters\":[{\"isSelectAllItemsSelected\":false,\"selectedItems\":[\"Göynük\"],\"filterType\":\"CITY\"}],\"folderName\":\"Turkey \"},{\"creationTime\":1715850761874,\"filters\":[{\"maxValue\":83.33332824707031,\"minValue\":0.0,\"valueFrom\":0.0,\"valueTo\":23.33333396911621,\"filterType\":\"AVERAGE_SPEED\"}],\"folderName\":\"Crash\"}]"
//	val oldFilters = "[{\"folderName\":\"2014\",\"creationTime\":1711549807641,\"filters\":[{\"className\":\"net.osmand.shared.gpx.filters.DateTrackFilter\",\"filterType\":\"DATE_CREATION\",\"valueFrom\":1251682533000,\"valueTo\":1730300878211}]},{\"folderName\":\"Turkey \",\"creationTime\":1713438258313,\"filters\":[{\"className\":\"net.osmand.shared.gpx.filters.ListTrackFilter\",\"filterType\":\"CITY\",\"selectedItems\":[\"Göynük\"],\"allItems\":[\"Göynük\"],\"allItemsCollection\":{\"Göynük\":0}}]},{\"folderName\":\"Crash\",\"creationTime\":1715850761874,\"filters\":[{\"className\":\"RangeTrackFilter\",\"filterType\":\"AVERAGE_SPEED\",\"minValue\":0.0,\"maxValue\":83.33332824707031,\"valueFrom\":0.0,\"valueTo\":23.33333396911621}]},{\"folderName\":\"Kyiv\",\"creationTime\":1728309759264,\"filters\":[{\"className\":\"net.osmand.shared.gpx.filters.ListTrackFilter\",\"filterType\":\"CITY\",\"selectedItems\":[\"Київ\"],\"allItems\":[\"Київ\",\"Amstelveen\",\"Berlin\",\"Aosta - Aoste\",\"Leiden\",\"Bolzano - Bozen\",\"Dresden\",\"Christchurch\",\"בית שמש\",\"Владимир\",\"Waltrop\",\"Waltham\",\"Pamplona / Iruña\",\"Marlborough\",\"Göynük\",\"Firenze\",\"Falkensee\",\"Emsdetten\",\"Cuneo\",\"Ahrensdorf\"],\"allItemsCollection\":{\"Cuneo\":1,\"Christchurch\":2,\"Marlborough\":1,\"Dresden\":2,\"Berlin\":6,\"Pamplona / Iruña\":1,\"Amstelveen\":8,\"Aosta - Aoste\":4,\"Leiden\":3,\"Waltrop\":1,\"Waltham\":1,\"Falkensee\":1,\"Київ\":21,\"Firenze\":1,\"Bolzano - Bozen\":3,\"בית שמש\":1,\"Göynük\":1,\"Ahrensdorf\":1,\"Emsdetten\":1,\"Владимир\":1}}]}]"

	@Test
	fun testLoadOldFilters() {
		val readCollection = json.decodeFromString<List<SmartFolder>?>(oldFilters)
		assertTrue { readCollection != null }
		assertTrue { readCollection?.size == 3 }
		val folder = readCollection?.get(1)
		assertTrue { folder?.filters?.size == 1 }
		assertTrue { folder?.folderName == "Turkey " }
		val filter = readCollection?.get(1)?.filters?.get(0)
		assertTrue { filter?.trackFilterType == TrackFilterType.CITY }
		assertTrue { filter is ListTrackFilter }
		val listFilter = filter as ListTrackFilter
		assertTrue { listFilter.isSelectAllItemsSelected }
		assertTrue { listFilter.selectedItems.size == 1 }
		assertTrue { listFilter.selectedItems[0] == "Göynük" }
	}
}