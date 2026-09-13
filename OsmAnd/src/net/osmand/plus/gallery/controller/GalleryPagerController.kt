package net.osmand.plus.gallery.controller

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.data.getPagerItems
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment
import net.osmand.shared.media.domain.MediaItem

class GalleryPagerController(
	app: OsmandApplication,
	val key: GalleryKey
) : BaseDialogController(app) {

	var orderedIds: List<String>? = null
	private var autoPlayItemId: String? = null

	fun consumeAutoPlay(id: String): Boolean {
		if (autoPlayItemId != id) return false
		autoPlayItemId = null
		return true
	}

	/** Single entry point for both Details menu items, so the viewer state is decided in one place. */
	fun openDetails(activity: FragmentActivity, item: MediaItem) {
		if (activity.supportFragmentManager.findFragmentByTag(GalleryPhotoPagerFragment.TAG) == null) {
			autoPlayItemId = null
			GalleryPhotoPagerFragment.showInstance(activity, item.id)
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
			orderedIds: List<String>? = null
		) {
			val controller = getInstance(activity.application as OsmandApplication, key)
			controller.orderedIds = orderedIds
			controller.autoPlayItemId = if (key == GalleryKey.MediaLibrary) selectedItemId else null
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