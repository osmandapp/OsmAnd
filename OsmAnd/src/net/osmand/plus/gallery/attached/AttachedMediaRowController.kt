package net.osmand.plus.gallery.attached

import android.view.View
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.attached.helpers.AttachedMediaUiHelper
import net.osmand.plus.gallery.controller.GalleryPagerController
import net.osmand.plus.gallery.controller.GalleryRowController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GalleryItem.NoMedia.ActionButtonStyle
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.plus.gallery.ui.holders.MediaHolderType
import net.osmand.shared.gpx.primitives.Linkable
import net.osmand.shared.media.domain.MediaItem


class AttachedMediaRowController @JvmOverloads constructor(
	app: OsmandApplication,
	key: GalleryKey,
	private val target: Linkable,
	private val latLon: LatLon?,
	private val compactItems: Boolean = false
) : GalleryRowController(app, key) {

	private val mediaChangeListener: (Set<GalleryKey>) -> Unit = { keys ->
		if (key in keys) {
			onMediaChanged()
		}
	}

	init {
		app.galleryHelper.addAttachedMediaChangeListener(mediaChangeListener)
	}

	override fun requiresInternet() = false

	override fun detach() {
		app.galleryHelper.removeAttachedMediaChangeListener(mediaChangeListener)
		super.detach()
	}

	override fun resolveRowHolderType(position: Int): MediaHolderType =
		if (compactItems) MediaHolderType.SMALL else super.resolveRowHolderType(position)

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

	override fun handleGalleryAction(v: View, action: GalleryAction) {
		view?.mapActivity?.let {
			when (action) {
				SHOW_ALL_ACTION -> AttachedMediaGridController.show(it, key, target, latLon)
				ADD_MEDIA_ACTION -> AttachedMediaUiHelper(it).showAddMenu(v, target, latLon) {
					onMediaChanged()
				}
			}
		}
	}

	override fun onMediaItemClicked(mediaItem: MediaItem) {
		val activity = view?.mapActivity ?: return

		val orderedIds = getGalleryItems()
			.filterIsInstance<GalleryItem.Media>()
			.map { it.mediaItem.id }
		GalleryPagerController.show(activity, key, mediaItem.id, orderedIds)
	}

	override fun emptyStateItems(): List<GalleryItem> {
		if (compactItems) {
			return emptyList()
		}
		val noMedia = GalleryItem.NoMedia(
			action = ADD_MEDIA_ACTION,
			titleResId = R.string.no_media,
			descriptionResId = R.string.no_media_descr,
			iconResId = R.drawable.ic_action_image_disabled,
			buttonStyle = ActionButtonStyle.DIALOG
		)
		return listOf(noMedia)
	}

	fun showAddMenu(anchor: View) = handleGalleryAction(anchor, ADD_MEDIA_ACTION)

	fun showAllMedia(anchor: View) = handleGalleryAction(anchor, SHOW_ALL_ACTION)

	fun hasMedia(): Boolean = hasMediaItems()

	private fun onMediaChanged() {
		app.galleryHelper.mediaLoader.reload(key, loadListener)
	}

	fun matches(key: GalleryKey, target: Linkable, latLon: LatLon?): Boolean {
		return getGalleryKey() == key
				&& this.target == target
				&& this.latLon == latLon
	}

	companion object {
		private val SHOW_ALL_ACTION = GalleryAction("show_all_attached")
		private val ADD_MEDIA_ACTION = GalleryAction("add_media")
	}
}