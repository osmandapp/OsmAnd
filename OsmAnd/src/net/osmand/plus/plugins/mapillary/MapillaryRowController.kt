package net.osmand.plus.plugins.mapillary

import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.online.OnlinePhotosHolder
import net.osmand.plus.mapcontextmenu.MenuBuilder
import net.osmand.plus.mapcontextmenu.gallery.OnlinePhotosRowController
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin

private const val PREVIEW_LIMIT = 5

private val CONTRIBUTE_ACTION = GalleryAction(MapillaryPlugin.TYPE_MAPILLARY_CONTRIBUTE)

class MapillaryRowController(
	app: OsmandApplication,
	menuBuilder: MenuBuilder,
	val plugin: MapillaryPlugin
) : OnlinePhotosRowController(app, menuBuilder) {

	override fun onMediaItemClicked(mediaItem: MediaItem) {
		if (mediaItem.origin != MediaOrigin.MAPILLARY || mediaItem !is MediaItem.Remote) return

		mapActivity?.contextMenu?.close()

		var location: LatLon? = null
		val metadata = mediaItem.metadata
		if (metadata.latitude != null && metadata.longitude != null) {
			location = LatLon(metadata.latitude!!, metadata.longitude!!)
		}

		MapillaryImageDialog.show(
			mapActivity, metadata.key,
			mediaItem.downloadUri, mediaItem.sourceUri,
			location, metadata.cameraAngle,
			app.getString(R.string.mapillary), null, true
		)
	}

	override fun getGalleryItems(): List<GalleryItem> {
		return applyMediaItemsLimit(super.getGalleryItems())
	}

	private fun applyMediaItemsLimit(newItems: List<GalleryItem>): List<GalleryItem> {
		val limitedItems = mutableListOf<GalleryItem>()
		var limitedMediaItemsCount = 0

		for (item in newItems) {
			if (item is GalleryItem.Media && item.mediaItem.origin == MediaOrigin.MAPILLARY) {
				if (limitedMediaItemsCount < PREVIEW_LIMIT) {
					limitedItems.add(item)
					limitedMediaItemsCount++
				}
			} else {
				limitedItems.add(item)
			}
		}
		return limitedItems
	}

	override fun collectActionButtons(): List<GalleryActionButton> {
		val exploreButton = GalleryActionButton(
			titleId = R.string.shared_string_explore,
			action = CONTRIBUTE_ACTION
		)
		return if (hasMediaItems()) listOf(exploreButton) else emptyList()
	}

	override fun handleActionButtonClick(actionButton: GalleryActionButton) {
		if (actionButton.action == CONTRIBUTE_ACTION) {
			mapActivity?.let { MapillaryPlugin.openMapillary(it) }
		}
	}

	override fun onPhotosLoadFinished(holder: OnlinePhotosHolder) {
		view?.onLoadingImage(false)
		val items = holder.getMapillaryGalleryItems().toMutableList()
		if (items.isEmpty()) {
			items.add(GalleryItem.NoMedia(CONTRIBUTE_ACTION))
		}
		setItems(items)
	}
}