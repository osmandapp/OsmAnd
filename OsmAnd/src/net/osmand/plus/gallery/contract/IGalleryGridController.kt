package net.osmand.plus.gallery.contract

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GalleryToolbarAction
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import android.view.View

interface IGalleryGridController : IGalleryListener, IGalleryActionListener {
	fun isGroupingSupported(): Boolean = false
	fun isGrouped(): Boolean = false
	fun onMediaItemMenuClicked(item: MediaItem, anchor: View) {}
	fun onGroupHeaderClicked(type: MediaType) {}
	fun attach(view: IGalleryGridView)
	fun detach()
	fun onScreenDestroyed(activity: FragmentActivity?)
	fun getScreenTitle(): String?
	fun getGalleryItems(): List<GalleryItem>
	fun getSpanCount(isPortrait: Boolean): Int

	fun getSpanBounds(isPortrait: Boolean): IntRange = 2..4

	fun getDisplayMode(): GalleryDisplayMode = GalleryDisplayMode.GRID
	fun isListModeSupported(): Boolean = false
	fun getToolbarActions(): List<GalleryToolbarAction> = emptyList()

	fun isSelectionModeSupported(): Boolean = false
	fun isSelectionMode(): Boolean = false
	fun enterSelectionMode(seed: MediaItem?) {}
	fun exitSelectionMode() {}
	fun toggleSelection(item: MediaItem) {}
	fun isSelected(item: MediaItem): Boolean = false
	fun toggleSelectAll() {}
	fun isAllSelected(): Boolean = false
	fun getSelectedCount(): Int = 0
	fun onMediaItemLongClicked(item: MediaItem) {}
}
