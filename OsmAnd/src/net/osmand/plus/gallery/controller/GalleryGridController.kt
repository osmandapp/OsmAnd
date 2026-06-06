package net.osmand.plus.gallery.controller

import android.view.View
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.gallery.attached.helpers.AttachedMediaUiHelper
import net.osmand.plus.gallery.contract.IGalleryGridController
import net.osmand.plus.gallery.contract.IGalleryGridView
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryGridFragment
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaType

class GalleryGridController(
	app: OsmandApplication,
	val key: GalleryKey
) : BaseDialogController(app), IGalleryGridController {

	private var view: IGalleryGridView? = null

	override fun getProcessId(): String = PROCESS_ID

	override fun attach(view: IGalleryGridView) {
		this.view = view
	}

	override fun detach() {
		this.view = null
	}

	override fun getGalleryItems(): List<GalleryItem> {
		return app.galleryHelper.repository.get(key)
			?.getItems()
			?.map { toGalleryItem(it) }
			?: emptyList()
	}

	override fun onMediaItemClicked(mediaItem: MediaItem) {
		val nightMode = view?.isNightMode() ?: false

		view?.getMapActivity()?.let {
			if (mediaItem.type == MediaType.PHOTO) {
				GalleryPagerController.showDialog(it, key, mediaItem.id)
			} else {
				AttachedMediaUiHelper(it).openMediaItem(mediaItem, nightMode)
			}
		}
	}

	override fun handleGalleryAction(v: View, action: GalleryAction) {
	}

	private fun toGalleryItem(mediaItem: MediaItem): GalleryItem.Media {
		return GalleryItem.Media(
			mediaItem = mediaItem,
			showLoadingProgress = mediaItem.origin == MediaOrigin.OTHER
		)
	}

	companion object {
		const val PROCESS_ID = "gallery_grid"

		@JvmStatic
		fun showDialog(
			activity: FragmentActivity,
			key: GalleryKey,
			title: String? = null
		) {
			getInstance(activity.application as OsmandApplication, key)
			GalleryGridFragment.showInstance(activity, title)
		}

		@JvmStatic
		fun getInstance(
			app: OsmandApplication,
			key: GalleryKey
		): GalleryGridController {
			val dialogManager = app.dialogManager
			val existing = dialogManager.findController(PROCESS_ID) as? GalleryGridController
			if (existing != null && existing.key == key) {
				return existing
			}
			return GalleryGridController(app, key).also {
				dialogManager.register(PROCESS_ID, it)
			}
		}

		@JvmStatic
		fun getExistingInstance(app: OsmandApplication): GalleryGridController? {
			return app.dialogManager.findController(PROCESS_ID) as? GalleryGridController
		}
	}
}