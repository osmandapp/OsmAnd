package net.osmand.plus.gallery.controller

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.getPagerItems
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment
import net.osmand.plus.gallery.ui.viewer.MediaViewerSheetLayout
import net.osmand.shared.media.domain.MediaItem

class GalleryPagerController(
	app: OsmandApplication,
	val key: GalleryKey
) : BaseDialogController(app) {

	var orderedIds: List<String>? = null
	private var autoPlayOnOpenItemId: String? = null

	fun takeAutoPlayOnOpen(id: String): Boolean {
		if (autoPlayOnOpenItemId != id) return false
		autoPlayOnOpenItemId = null
		return true
	}

	@JvmOverloads
	fun openDetails(activity: FragmentActivity, item: MediaItem, orderedIds: List<String>? = null) {
		val viewer = activity.supportFragmentManager.findFragmentByTag(GalleryPhotoPagerFragment.TAG) as? GalleryPhotoPagerFragment
		if (viewer != null) {
			viewer.showDetails(item.id)
		} else {
			this.orderedIds = orderedIds
			autoPlayOnOpenItemId = null
			GalleryPhotoPagerFragment.showInstance(activity, item.id, MediaViewerSheetLayout.STATE_PREVIEW)
		}
	}

	val mediaItems: List<GalleryItem.Media>
		get() {
			val items = app.galleryHelper.repository.get(key)
				?.getPagerItems()
				?: emptyList()
			val order = orderedIds
			val sorted = if (order.isNullOrEmpty()) {
				items
			} else {
				val indexById = order.withIndex().associate { (index, id) -> id to index }
				items.sortedBy { indexById[it.id] ?: Int.MAX_VALUE }
			}
			return sorted.map { GalleryItem.Media(it) }
		}

	fun getIndexById(id: String): Int {
		val index = mediaItems.indexOfFirst { it.mediaItem.id == id }
		return if (index >= 0) index else 0
	}

	override fun getProcessId(): String = PROCESS_ID

	companion object {
		const val PROCESS_ID = "gallery_pager"

		@JvmStatic
		@JvmOverloads
		fun show(
			activity: FragmentActivity,
			key: GalleryKey,
			selectedItemId: String,
			orderedIds: List<String>? = null,
			autoPlay: Boolean = false
		) {
			val controller = getInstance(activity.application as OsmandApplication, key)
			controller.orderedIds = orderedIds
			controller.autoPlayOnOpenItemId = if (autoPlay) selectedItemId else null
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