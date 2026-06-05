package net.osmand.plus.plugins.mapillary

import android.view.View
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.controller.GalleryRowController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.plus.gallery.online.OnlinePhotosHolder
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin

class MapillaryRowController(
	app: OsmandApplication,
	key: GalleryKey.Location,
) : GalleryRowController(app, key) {

	override fun requiresInternet() = true

	override fun buildGalleryItems(holder: MediaHolder): List<GalleryItem> {
		val onlinePhotosHolder = holder as? OnlinePhotosHolder ?: return emptyStateItems()

		val mediaItems = onlinePhotosHolder.getMapillaryItems()
			.take(PREVIEW_LIMIT)
			.map { GalleryItem.Media(it) }

		return if (mediaItems.isEmpty()) {
			emptyStateItems()
		} else {
			mediaItems + GalleryItem.Action(CONTRIBUTE_ACTION)
		}
	}

	override fun emptyStateItems(): List<GalleryItem> {
		return listOf(GalleryItem.NoMedia(action = CONTRIBUTE_ACTION))
	}

	override fun collectActionButtons(): List<GalleryActionButton> {
		val exploreButton = GalleryActionButton(
			titleId = R.string.shared_string_explore,
			action = CONTRIBUTE_ACTION
		)
		return if (hasMediaItems()) listOf(exploreButton) else emptyList()
	}

	override fun handleGalleryAction(v: View, action: GalleryAction) {
		if (action == CONTRIBUTE_ACTION) {
			view?.mapActivity?.let { MapillaryPlugin.openMapillary(it) }
		}
	}

	override fun onMediaItemClicked(mediaItem: MediaItem) {
		if (mediaItem.origin != MediaOrigin.MAPILLARY || mediaItem !is MediaItem.Remote) {
			return
		}
		val mapActivity = view?.mapActivity ?: return
		mapActivity.contextMenu.close()

		val metadata = mediaItem.metadata
		val location = if (metadata.latitude != null && metadata.longitude != null) {
			LatLon(metadata.latitude!!, metadata.longitude!!)
		} else {
			null
		}

		MapillaryImageDialog.show(
			mapActivity, metadata.key,
			mediaItem.downloadUri, mediaItem.sourceUri,
			location, metadata.cameraAngle,
			app.getString(R.string.mapillary), null, true
		)
	}

	companion object {
		private const val PREVIEW_LIMIT = 5

		private val CONTRIBUTE_ACTION = GalleryAction(
			MapillaryPlugin.TYPE_MAPILLARY_CONTRIBUTE
		)
	}
}
