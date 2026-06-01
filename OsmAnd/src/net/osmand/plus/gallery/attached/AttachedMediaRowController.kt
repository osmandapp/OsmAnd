package net.osmand.plus.gallery.attached

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.controller.GalleryPagerController
import net.osmand.plus.gallery.controller.GalleryRowController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GalleryItem.NoMedia.ActionButtonStyle
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.shared.media.domain.MediaItem

class AttachedMediaRowController(
	app: OsmandApplication,
	key: GalleryKey
) : GalleryRowController(app, key) {

	override fun requiresInternet() = false

	override fun buildGalleryItems(holder: MediaHolder): List<GalleryItem> =
		holder.getItems().map { GalleryItem.Media(it, showLoadingProgress = false) }

	override fun collectActionButtons(): List<GalleryActionButton> =
		if (hasMediaItems()) {
			listOf(
				GalleryActionButton(R.string.shared_string_show_all, SHOW_ALL_ACTION),
				GalleryActionButton(R.string.shared_string_add, ADD_MEDIA_ACTION)
			)
		} else {
			emptyList()
		}

	override fun handleGalleryAction(action: GalleryAction) {
		if (action == SHOW_ALL_ACTION) {
			view?.mapActivity?.let { GalleryGridController.showDialog(it, key) }
		} else if (action == ADD_MEDIA_ACTION) {
			// TODO: open popup add media menu
		}
	}

	override fun onMediaItemClicked(mediaItem: MediaItem) {
		val activity = view?.mapActivity ?: return
		GalleryPagerController.showDialog(activity, key, mediaItem.id)
	}

	override fun emptyStateItems(): List<GalleryItem> {
		val noMedia = GalleryItem.NoMedia(
			action = ADD_MEDIA_ACTION,
			titleResId = R.string.no_media,
			descriptionResId = R.string.no_media_descr,
			iconResId = R.drawable.ic_action_image_disabled,
			buttonStyle = ActionButtonStyle.DIALOG
		)
		return listOf(noMedia)
	}

	companion object {
		private val SHOW_ALL_ACTION = GalleryAction("show_all_attached")
		private val ADD_MEDIA_ACTION = GalleryAction("add_media")
	}
}