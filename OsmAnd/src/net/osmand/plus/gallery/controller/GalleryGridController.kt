package net.osmand.plus.gallery.controller

import android.view.View
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.gallery.contract.IGalleryGridController
import net.osmand.plus.gallery.contract.IGalleryGridView
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GallerySortMode
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper
import net.osmand.plus.gallery.ui.GalleryGridAdapter
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator.Companion.GRID_SCREEN_ITEM_SPACE_DP
import net.osmand.plus.gallery.ui.GalleryGridSettings
import net.osmand.plus.gallery.ui.holders.MediaHolderType
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin

abstract class GalleryGridController(
	app: OsmandApplication,
	open val key: GalleryKey
) : BaseDialogController(app), IGalleryGridController {

	protected var view: IGalleryGridView? = null

	private var displayMode = GalleryDisplayMode.GRID

	private var selectionMode = false
	protected val selectionHelper = ItemsSelectionHelper<MediaItem>(true)

	private val standardPhotoSizePx =
		app.resources.getDimensionPixelSize(R.dimen.gallery_standard_icon_size)

	override fun attach(view: IGalleryGridView) {
		this.view = view
	}

	override fun detach() {
		this.view = null
	}

	override fun onScreenDestroyed(activity: FragmentActivity?) {
		detach()
		finishProcessIfNeeded(activity)
	}

	abstract override fun getScreenTitle(): String?

	override fun getGalleryItems(): List<GalleryItem> {
		val items = mutableListOf<GalleryItem>()
		val mediaItems = getMediaItems().map { toGalleryItem(it) }

		if (mediaItems.isNotEmpty()) {
			items.add(GalleryItem.MediaCount)
		}
		items.addAll(mediaItems)
		return items
	}

	protected open fun getMediaItems(): List<MediaItem> =
		app.galleryHelper.repository.get(key)?.getItems() ?: emptyList()

	override fun getSpanCount(isPortrait: Boolean): Int {
		return GalleryGridSettings.getSpanCount(app, isPortrait)
	}

	open fun setSpanCount(isPortrait: Boolean, count: Int) {
		GalleryGridSettings.setSpanCount(app, isPortrait, count)
	}

	override fun getSpanBounds(isPortrait: Boolean): IntRange =
		if (isPortrait) MIN_SPAN_COUNT..MAX_SPAN_COUNT else MIN_SPAN_COUNT_LANDSCAPE..MAX_SPAN_COUNT_LANDSCAPE

	// --- Image size ---

	open fun resolveSpanResizableSize(viewWidth: Int?): Int {
		val isPortrait = view?.isPortrait() ?: return standardPhotoSizePx
		return resolveSpanResizableSize(viewWidth, getSpanCount(isPortrait))
	}

	open fun resolveSpanResizableSize(viewWidth: Int?, spanCount: Int): Int {
		val mapActivity = view?.getActivity() ?: return standardPhotoSizePx
		val isPortrait = view?.isPortrait() ?: return standardPhotoSizePx

		val padding = AndroidUtils.dpToPx(app, GalleryGridItemDecorator.GRID_SIDE_PADDING_DP)
		val itemSpace = AndroidUtils.dpToPx(app, GRID_SCREEN_ITEM_SPACE_DP * 2f)
		val screenWidth = viewWidth ?: if (isPortrait) {
			AndroidUtils.getScreenWidth(mapActivity)
		} else {
			AndroidUtils.getScreenHeight(mapActivity)
		}
		val spaceForItems = screenWidth - (padding * 2) - (spanCount * itemSpace)
		return spaceForItems / spanCount
	}

	// --- Display mode ---

	override fun getDisplayMode(): GalleryDisplayMode = displayMode

	override fun isListModeSupported(): Boolean = false

	open fun setDisplayMode(mode: GalleryDisplayMode) {
		if (displayMode != mode) {
			displayMode = mode
			view?.updateDisplayMode()
		}
	}

	// --- Selection mode ---

	override fun isSelectionModeSupported(): Boolean = false

	override fun isSelectionMode(): Boolean = selectionMode

	override fun enterSelectionMode(seed: MediaItem?) {
		if (!isSelectionModeSupported() || selectionMode) {
			seed?.let { toggleSelection(it) }
			return
		}
		selectionMode = true
		selectionHelper.setAllItems(getMediaItems())
		selectionHelper.clearSelectedItems()
		seed?.let { selectionHelper.onItemsSelected(listOf(it), true) }
		view?.updateToolbar()
		view?.updateSelection()
	}

	override fun exitSelectionMode() {
		if (!selectionMode) return
		selectionMode = false
		selectionHelper.clearSelectedItems()
		view?.updateToolbar()
		view?.updateSelection()
	}

	override fun toggleSelection(item: MediaItem) {
		if (!selectionMode) return
		val selected = selectionHelper.isItemSelected(item)
		selectionHelper.onItemsSelected(listOf(item), !selected)
		view?.updateToolbar()
		view?.updateSelection()
	}

	override fun isSelected(item: MediaItem): Boolean = selectionHelper.isItemSelected(item)

	override fun toggleSelectAll() {
		if (!selectionMode) return
		selectionHelper.setAllItems(getMediaItems())
		if (selectionHelper.isAllItemsSelected) {
			selectionHelper.clearSelectedItems()
		} else {
			selectionHelper.selectAllItems()
		}
		view?.updateToolbar()
		view?.updateSelection()
	}

	override fun isAllSelected(): Boolean =
		selectionHelper.hasAnyItems() && selectionHelper.isAllItemsSelected

	override fun getSelectedCount(): Int = selectionHelper.selectedItemsSize

	protected fun getSelectedItems(): Set<MediaItem> = selectionHelper.selectedItems

	override fun onMediaItemLongClicked(item: MediaItem) {
		if (!isSelectionModeSupported()) return
		if (selectionMode) {
			toggleSelection(item)
		} else {
			enterSelectionMode(item)
		}
	}

	// --- Media ---

	override fun onMediaItemClicked(mediaItem: MediaItem) {
		val activity = view?.getActivity() ?: return
		val orderedIds = getGalleryItems()
			.filterIsInstance<GalleryItem.Media>()
			.map { it.mediaItem.id }
		GalleryPagerController.show(activity, key, mediaItem.id, orderedIds)
	}

	override fun handleGalleryAction(v: View, action: GalleryAction) {}

	open fun onSortModeSelected(sortMode: GallerySortMode) {}

	private fun toGalleryItem(mediaItem: MediaItem): GalleryItem.Media {
		return GalleryItem.Media(
			mediaItem = mediaItem,
			showLoadingProgress = mediaItem.origin == MediaOrigin.OTHER
		)
	}

	open fun createAdapter(mapActivity: FragmentActivity, viewWidth: Int?, nightMode: Boolean): GalleryGridAdapter {
		val registry = app.galleryHelper.loadStateRegistry
		return GalleryGridAdapter(
			mapActivity = mapActivity,
			onMediaClicked = ::onMediaItemClicked,
			onReloadMediaItems = ::onReloadMediaItems,
			onActionClicked = ::handleGalleryAction,
			onSortModeSelected = ::onSortModeSelected,
			mediaHolderType = { MediaHolderType.SPAN_RESIZABLE },
			resolveResizableImageSize = { resolveSpanResizableSize(viewWidth) },
			isLoadFailed = registry::isFailed,
			onLoadFailed = registry::markFailed,
			nightMode = nightMode,
			onMediaLongClicked = ::onMediaItemLongClicked,
			isItemSelected = ::isSelected,
			onToggleSelection = ::toggleSelection,
			posterLoader = app.galleryHelper.posterLoader,
			onGroupHeaderClicked = ::onGroupHeaderClicked
		)
	}

	companion object {
		const val MIN_SPAN_COUNT = 2
		const val MAX_SPAN_COUNT = 5
		const val MIN_SPAN_COUNT_LANDSCAPE = 4
		const val MAX_SPAN_COUNT_LANDSCAPE = 8
	}
}