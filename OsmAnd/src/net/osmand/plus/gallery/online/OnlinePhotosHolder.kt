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
		OnlinePhotosGroup.PANORAMAX_AMENITY,
		OnlinePhotosGroup.WIKIDATA,
		OnlinePhotosGroup.WIKIMEDIA,
		OnlinePhotosGroup.OTHER
	)

	fun getMapillaryItems(): List<MediaItem> =
		getItemsByGroups(OnlinePhotosGroup.MAPILLARY)

	fun addItem(group: OnlinePhotosGroup, item: MediaItem) {
		itemsByGroup.getOrPut(group) { linkedMapOf() }[item.id] = item
	}

	override var isComplete: Boolean = true
		private set

	fun markIncomplete() { isComplete = false }

	/** An incomplete result keeps the photos of [previous] it is missing, so a failed provider does not hide them. */
	fun keepItemsFrom(previous: MediaHolder?) {
		if (isComplete || previous !is OnlinePhotosHolder) return
		previous.itemsByGroup.forEach { (group, items) ->
			val target = itemsByGroup.getOrPut(group) { linkedMapOf() }
			items.forEach { (id, item) -> target.getOrPut(id) { item } }
		}
	}

	fun clear() { itemsByGroup.clear() }

	private fun getItemsByGroups(vararg groups: OnlinePhotosGroup): List<MediaItem> {
		return groups.flatMap { itemsByGroup[it]?.values ?: emptyList() }
	}
}