package net.osmand.plus.gallery.online

import android.view.View
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.controller.GalleryRowController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.plus.gallery.controller.GalleryPagerController
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin

open class OnlinePhotosRowController(
	app: OsmandApplication,
	key: GalleryKey.Location
) : GalleryRowController(app, key) {

	override fun requiresInternet() = true

	override fun buildGalleryItems(holder: MediaHolder): List<GalleryItem> {
		return holder.getItems().map { mediaItem ->
			val showLoadingProgress = mediaItem.origin == MediaOrigin.OTHER
			GalleryItem.Media(mediaItem, showLoadingProgress)
		}
	}

	override fun collectActionButtons(): List<GalleryActionButton> {
		val showAllButton = GalleryActionButton(
			titleId = R.string.shared_string_show_all,
			action = SHOW_ALL_ACTION
		)
		return if (hasMediaItems()) listOf(showAllButton) else emptyList()
	}

	override fun handleGalleryAction(v: View, action: GalleryAction) {
		if (action == SHOW_ALL_ACTION) {
			view?.mapActivity?.let { GalleryGridController.showDialog(it, key) }
		}
	}

	override fun onMediaItemClicked(mediaItem: MediaItem) {
		val activity = view?.mapActivity ?: return
		GalleryPagerController.showDialog(activity, key, mediaItem.id)
	}

	companion object {
		private val SHOW_ALL_ACTION = GalleryAction("show_all")
	}
}