package net.osmand.plus.gallery.controller

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.getPhotoItems
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment

class GalleryPagerController(
	app: OsmandApplication,
	val key: GalleryKey
) : BaseDialogController(app) {

	val photoItems: List<GalleryItem.Media>
		get() = app.galleryHelper.repository.get(key)
			?.getPhotoItems()
			?.map { GalleryItem.Media(it) }
			?: emptyList()

	fun getIndexById(id: String): Int {
		val index = photoItems.indexOfFirst { it.mediaItem.id == id }
		return if (index >= 0) index else 0
	}

	override fun getProcessId(): String = PROCESS_ID

	companion object {
		const val PROCESS_ID = "gallery_pager"

		@JvmStatic
		fun showDialog(
			activity: FragmentActivity,
			key: GalleryKey,
			selectedItemId: String
		) {
			getInstance(activity.application as OsmandApplication, key)
			GalleryPhotoPagerFragment.showInstance(activity, selectedItemId)
		}

		@JvmStatic
		fun getInstance(
			app: OsmandApplication,
			key: GalleryKey
		): GalleryPagerController {
			val dialogManager = app.dialogManager
			val existing = dialogManager.findController(PROCESS_ID) as? GalleryPagerController
			if (existing != null && existing.key == key) {
				return existing
			}
			return GalleryPagerController(app, key).also {
				dialogManager.register(PROCESS_ID, it)
			}
		}

		@JvmStatic
		fun getExistingInstance(app: OsmandApplication): GalleryPagerController? {
			return app.dialogManager.findController(PROCESS_ID) as? GalleryPagerController
		}
	}
}