package net.osmand.plus.gallery.online

import net.osmand.data.LatLon
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.shared.media.domain.MediaItem

class OnlinePhotosHolder(
	val latLon: LatLon,
	val params: Map<String, String>
) {

	private val itemsByGroup = linkedMapOf<OnlinePhotosGroup, LinkedHashMap<String, GalleryItem>>()

	fun getOrderedGalleryItems(): List<GalleryItem> {
		return getGalleryItemsWithGroups(
			OnlinePhotosGroup.MAPILLARY_AMENITY,
			OnlinePhotosGroup.WIKIDATA,
			OnlinePhotosGroup.WIKIMEDIA,
			OnlinePhotosGroup.OTHER,
			OnlinePhotosGroup.ASTRONOMY
		)
	}

	fun getMapillaryGalleryItems(): List<GalleryItem> {
		return getGalleryItemsWithGroups(OnlinePhotosGroup.MAPILLARY)
	}

	fun getAstronomyGalleryItems(): List<GalleryItem> {
		return getGalleryItemsWithGroups(OnlinePhotosGroup.ASTRONOMY)
	}

	private fun getGalleryItemsWithGroups(vararg groups: OnlinePhotosGroup): List<GalleryItem> {
		val result = mutableListOf<GalleryItem>()
		for (group in groups) {
			val items = itemsByGroup[group]
			if (!items.isNullOrEmpty()) {
				result.addAll(items.values)
			}
		}
		return result
	}

	fun addMediaItem(group: OnlinePhotosGroup, mediaItem: MediaItem) {
		addMediaItem(group, mediaItem, showLoadingProgress = false)
	}

	fun addMediaItem(
		group: OnlinePhotosGroup,
		mediaItem: MediaItem,
		showLoadingProgress: Boolean
	) {
		addGalleryItem(group, mediaItem.id, GalleryItem.Media(mediaItem, showLoadingProgress))
	}

	fun addGalleryItem(group: OnlinePhotosGroup, key: String, item: GalleryItem) {
		val items = itemsByGroup.getOrPut(group) { linkedMapOf() }
		items[key] = item
	}

	fun clear() {
		itemsByGroup.clear()
	}
}