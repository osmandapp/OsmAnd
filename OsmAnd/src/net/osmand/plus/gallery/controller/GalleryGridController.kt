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

	private var newScaleFactor = 0f
	private var zoomedForPinch = false

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

	protected fun getMediaItems(): List<MediaItem> =
		app.galleryHelper.repository.get(key)?.getItems() ?: emptyList()

	override fun getSpanCount(isPortrait: Boolean): Int {
		return GalleryGridSettings.getSpanCount(app, isPortrait)
	}

	private fun setSpanCount(isPortrait: Boolean, count: Int) {
		GalleryGridSettings.setSpanCount(app, isPortrait, count)
	}

	// --- Image size ---

	fun resolveSpanResizableSize(viewWidth: Int?): Int {
		val mapActivity = view?.getMapActivity() ?: return standardPhotoSizePx
		val isPortrait = view?.isPortrait() ?: return standardPhotoSizePx

		val spanCount = getSpanCount(isPortrait)
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

	// --- Scale ---

	override fun onScaleBegin() {
		newScaleFactor = 0f
	}

	override fun onScaleEnd() {
		// Only reset the accumulated factor: zoomedForPinch stays latched until
		// the touch gesture fully ends, so a single pinch can change the zoom
		// by at most one step even if the detector restarts mid-gesture.
		newScaleFactor = 0f
	}

	override fun onPinchGestureFinished() {
		newScaleFactor = 0f
		zoomedForPinch = false
	}

	override fun onScaleChanged(scaleFactor: Float): Boolean {
		if (zoomedForPinch) return false

		newScaleFactor += if (scaleFactor < 1f) {
			-(scaleFactor - 1f) * SCALE_MULTIPLIER
		} else {
			(1f - scaleFactor) * SCALE_MULTIPLIER
		}

		val isPortrait = view?.isPortrait() ?: return false

		// In list mode the only zoom gesture we react to is zoom-out, which
		// returns to the most zoomed-in grid state.
		if (displayMode == GalleryDisplayMode.LIST) {
			if (newScaleFactor >= 1f) {
				newScaleFactor = 0f
				zoomedForPinch = true
				setDisplayMode(GalleryDisplayMode.GRID)
				return true
			}
			return false
		}

		val previousCount = getSpanCount(isPortrait)
		val newCount = newScaleFactor.toInt() + previousCount
		if (newCount == previousCount) {
			return false
		}

		// Further zoom-in past the most zoomed-in grid switches to the list.
		if (newCount < MIN_SPAN_COUNT && previousCount == MIN_SPAN_COUNT && isListModeSupported()) {
			newScaleFactor = 0f
			zoomedForPinch = true
			setDisplayMode(GalleryDisplayMode.LIST)
			return true
		}

		if (newCount in MIN_SPAN_COUNT..MAX_SPAN_COUNT) {
			newScaleFactor = 0f
			setSpanCount(isPortrait, newCount)
			zoomedForPinch = true
			view?.updateSpan()
			return true
		}
		return false
	}

	// --- Display mode ---

	override fun getDisplayMode(): GalleryDisplayMode = displayMode

	override fun isListModeSupported(): Boolean = false

	private fun setDisplayMode(mode: GalleryDisplayMode) {
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
		val activity = view?.getMapActivity() ?: return
		val orderedIds = getGalleryItems()
			.filterIsInstance<GalleryItem.Media>()
			.map { it.mediaItem.id }
		GalleryPagerController.show(activity, key, mediaItem.id, orderedIds)
	}

	override fun handleGalleryAction(v: View, action: GalleryAction) {}

	private fun toGalleryItem(mediaItem: MediaItem): GalleryItem.Media {
		return GalleryItem.Media(
			mediaItem = mediaItem,
			showLoadingProgress = mediaItem.origin == MediaOrigin.OTHER
		)
	}

	fun createAdapter(mapActivity: MapActivity, viewWidth: Int?, nightMode: Boolean): GalleryGridAdapter {
		val registry = app.galleryHelper.loadStateRegistry
		return GalleryGridAdapter(
			mapActivity = mapActivity,
			onMediaClicked = ::onMediaItemClicked,
			onReloadMediaItems = ::onReloadMediaItems,
			onActionClicked = ::handleGalleryAction,
			mediaHolderType = { MediaHolderType.SPAN_RESIZABLE },
			resolveResizableImageSize = { resolveSpanResizableSize(viewWidth) },
			isLoadFailed = registry::isFailed,
			onLoadFailed = registry::markFailed,
			nightMode = nightMode,
			onMediaLongClicked = ::onMediaItemLongClicked,
			isItemSelected = ::isSelected,
			onToggleSelection = ::toggleSelection,
			posterLoader = app.galleryHelper.posterLoader
		)
	}

	companion object {
		const val MIN_SPAN_COUNT = 2
		const val MAX_SPAN_COUNT = 4
		const val SCALE_MULTIPLIER = 3f
	}
}