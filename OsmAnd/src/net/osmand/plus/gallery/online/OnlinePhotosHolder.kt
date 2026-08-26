package net.osmand.plus.gallery.online

import net.osmand.data.LatLon
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.shared.media.domain.MediaItem

class OnlinePhotosHolder(
	val latLon: LatLon,
	val params: Map<String, String>
) : MediaHolder {

	private val itemsByGroup = linkedMapOf<OnlinePhotosGroup, LinkedHashMap<String, MediaItem>>()

	override fun getItems(): List<MediaItem> = getItemsByGroups(
		OnlinePhotosGroup.MAPILLARY_AMENITY,
		OnlinePhotosGroup.WIKIDATA,
		OnlinePhotosGroup.WIKIMEDIA,
		OnlinePhotosGroup.OTHER
	)

	fun getMapillaryItems(): List<MediaItem> =
		getItemsByGroups(OnlinePhotosGroup.MAPILLARY)

	fun addItem(group: OnlinePhotosGroup, item: MediaItem) {
		itemsByGroup.getOrPut(group) { linkedMapOf() }[item.id] = item
	}

	fun clear() { itemsByGroup.clear() }

	private fun getItemsByGroups(vararg groups: OnlinePhotosGroup): List<MediaItem> {
		return groups.flatMap { itemsByGroup[it]?.values ?: emptyList() }
	}
}