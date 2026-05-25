package net.osmand.plus.gallery

import net.osmand.data.LatLon
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.controller.GalleryMediaLoadStateProvider
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.online.OnlinePhotosHolder
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.util.Algorithms

// TODO: don't use this like a singleton
object GallerySession : GalleryMediaLoadStateProvider {

	@JvmStatic
	var itemsHolder: OnlinePhotosHolder? = null

	private val failedMediaIds: MutableSet<String> = HashSet()

	@JvmStatic
	fun isCurrentHolderEquals(latLon: LatLon, params: Map<String, String>): Boolean {
		return itemsHolder != null && Algorithms.objectEquals(itemsHolder!!.latLon, latLon)
				&& Algorithms.objectEquals(itemsHolder!!.params, params)
	}

	override fun markMediaLoadFailed(mediaItem: MediaItem) {
		failedMediaIds.add(mediaItem.id)
	}

	override fun isMediaLoadFailed(mediaItem: MediaItem): Boolean {
		return failedMediaIds.contains(mediaItem.id)
	}

	@JvmStatic
	fun getPhotoItemIndexById(id: String): Int {
		val mediaItems = onlinePhotoItems
		for (i in mediaItems.indices) {
			val mediaItem = mediaItems[i].mediaItem
			if (Algorithms.stringsEqual(id, mediaItem.id)) {
				return i
			}
		}
		return 0
	}

	@JvmStatic
	val onlinePhotoItems: List<GalleryItem.Media>
		get() {
			val galleryItems: MutableList<GalleryItem.Media> =
				ArrayList()
			if (itemsHolder != null) {
				for (item in itemsHolder!!.getOrderedGalleryItems()) {
					if (item is GalleryItem.Media && isPhoto(item.mediaItem)) {
						galleryItems.add(item)
					}
				}
			}
			return galleryItems
		}

	private fun isPhoto(mediaItem: MediaItem): Boolean {
		return mediaItem.type == MediaType.PHOTO
	}

	@JvmStatic
	fun getSettingsSpanCount(mapActivity: MapActivity): Int {
		val app = mapActivity.app
		return if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT.get()
		} else {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.get()
		}
	}

	@JvmStatic
	fun setSpanSettings(mapActivity: MapActivity, newSpanCount: Int) {
		val app = mapActivity.app
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT.set(newSpanCount)
		} else {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.set(newSpanCount)
		}
	}

	@JvmStatic
	fun clearHolder() {
		itemsHolder = null
		failedMediaIds.clear()
	}
}