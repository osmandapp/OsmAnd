package net.osmand.plus.gallery.controller

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryGridFragment
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin

class GalleryGridController(
	app: OsmandApplication,
	val key: GalleryKey
) : BaseDialogController(app) {

	val galleryItems: List<GalleryItem>
		get() = app.galleryRepository.get(key)
			?.getItems()
			?.map { toGalleryItem(it) }
			?: emptyList()

	override fun getProcessId(): String = PROCESS_ID

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