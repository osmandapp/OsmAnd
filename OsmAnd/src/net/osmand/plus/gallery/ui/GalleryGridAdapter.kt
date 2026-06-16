package net.osmand.plus.gallery.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.holders.ActionViewHolder
import net.osmand.plus.gallery.ui.holders.GalleryMediaListViewHolder
import net.osmand.plus.gallery.ui.holders.GalleryMediaViewHolder
import net.osmand.plus.gallery.ui.holders.MediaCountHolder
import net.osmand.plus.gallery.ui.holders.MediaHolderType
import net.osmand.plus.gallery.ui.holders.MediaStatsHolder
import net.osmand.plus.gallery.ui.holders.NoInternetHolder
import net.osmand.plus.gallery.ui.holders.NoMediaHolder
import net.osmand.plus.gallery.ui.holders.SortBarHolder
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.media.MediaProvider
import net.osmand.shared.media.domain.MediaItem

class GalleryGridAdapter(
	private val mapActivity: MapActivity,
	private val onMediaClicked: (MediaItem) -> Unit,
	private val onReloadMediaItems: () -> Unit,
	private val onActionClicked: (View, GalleryAction) -> Unit,
	private val mediaHolderType: (position: Int) -> MediaHolderType,
	private val resolveResizableImageSize: (() -> Int)? = null,
	private val isLoadFailed: (MediaItem) -> Boolean,
	private val onLoadFailed: (MediaItem) -> Unit,
	private val nightMode: Boolean,
	private val onMediaLongClicked: (MediaItem) -> Unit = {},
	private val isItemSelected: (MediaItem) -> Boolean = { false },
	private val onToggleSelection: (MediaItem) -> Unit = {},
	private val mediaProvider: MediaProvider = MediaProvider(mapActivity.app),
	private val posterLoader: MediaPosterLoader? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private val app: OsmandApplication = mapActivity.app
	private val themedInflater: LayoutInflater = UiUtilities.getInflater(mapActivity, nightMode)
	private val items = mutableListOf<GalleryItem>()

	private val mainPhotoSizePx = app.resources.getDimensionPixelSize(R.dimen.gallery_big_icon_size)
	private val standardPhotoSizePx = app.resources.getDimensionPixelSize(R.dimen.gallery_standard_icon_size)
	private val smallPhotoSizePx = app.resources.getDimensionPixelSize(R.dimen.gallery_small_icon_size)

	private var loadingImages = false

	var displayMode: GalleryDisplayMode = GalleryDisplayMode.GRID

	var selectionMode: Boolean = false

	@JvmOverloads
	fun setItems(newItems: List<GalleryItem>, animated: Boolean = false) {
		if (animated && items.isNotEmpty()) {
			val diff = DiffUtil.calculateDiff(GalleryDiffCallback(items.toList(), newItems))
			items.clear()
			items.addAll(newItems)
			diff.dispatchUpdatesTo(this)
		} else {
			items.clear()
			items.addAll(newItems)
			notifyDataSetChanged()
		}
	}

	private class GalleryDiffCallback(
		private val oldItems: List<GalleryItem>,
		private val newItems: List<GalleryItem>
	) : DiffUtil.Callback() {

		override fun getOldListSize(): Int = oldItems.size

		override fun getNewListSize(): Int = newItems.size

		override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
			val oldItem = oldItems[oldPosition]
			val newItem = newItems[newPosition]
			return when {
				oldItem is GalleryItem.Media && newItem is GalleryItem.Media ->
					oldItem.mediaItem.id == newItem.mediaItem.id
				oldItem is GalleryItem.Action && newItem is GalleryItem.Action ->
					oldItem.action == newItem.action
				else -> oldItem::class == newItem::class
			}
		}

		override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean =
			oldItems[oldPosition] == newItems[newPosition]

		override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
			val oldItem = oldItems[oldPosition]
			val newItem = newItems[newPosition]
			return if (oldItem is GalleryItem.Media && newItem is GalleryItem.Media) {
				METADATA_PAYLOAD_TYPE
			} else {
				null
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			MAIN_MEDIA_TYPE, MEDIA_TYPE -> {
				val itemView = inflate(R.layout.gallery_card_item, parent)
				GalleryMediaViewHolder(
					app, itemView, onMediaClicked, isLoadFailed,
					onLoadFailed, mediaProvider, onMediaLongClicked, onToggleSelection,
					posterLoader
				)
			}
			LIST_MEDIA_TYPE -> {
				val itemView = inflate(R.layout.gallery_list_item, parent)
				GalleryMediaListViewHolder(
					app, itemView, mediaProvider, onMediaClicked,
					onMediaLongClicked, onToggleSelection, posterLoader
				)
			}
			ACTION_VIEW_TYPE -> {
				val itemView = inflate(R.layout.context_menu_card_gallery_action_view, parent)
				ActionViewHolder(itemView, onActionClicked)
			}
			NO_MEDIA_TYPE -> {
				NoMediaHolder(inflate(R.layout.no_image_card, parent), app, onActionClicked)
			}
			NO_INTERNET_TYPE -> {
				val itemView = inflate(R.layout.no_internet_card, parent)
				NoInternetHolder(itemView, app, onReloadMediaItems)
			}
			MEDIA_COUNT_TYPE -> {
				MediaCountHolder(inflate(R.layout.images_count_item, parent), app)
			}
			MEDIA_STATS_TYPE -> {
				MediaStatsHolder(inflate(R.layout.gallery_media_stats_item, parent), app)
			}
			SORT_BAR_TYPE -> {
				SortBarHolder(inflate(R.layout.gallery_sort_bar_item, parent), app, onActionClicked)
			}
			else -> throw IllegalArgumentException("Unsupported view type: $viewType")
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]
		when {
			holder is GalleryMediaViewHolder && item is GalleryItem.Media -> {
				val holderType = mediaHolderType(position)
				val imageSizePx = when (holderType) {
					MediaHolderType.SPAN_RESIZABLE -> resolveResizableImageSize?.invoke() ?: standardPhotoSizePx
					MediaHolderType.MAIN -> mainPhotoSizePx
					MediaHolderType.SMALL -> smallPhotoSizePx
					else -> standardPhotoSizePx
				}
				holder.bindView(
					mapActivity, item, imageSizePx, holderType, nightMode,
					selectionMode, isItemSelected(item.mediaItem)
				)
			}
			holder is GalleryMediaListViewHolder && item is GalleryItem.Media -> {
				val showDivider = items.getOrNull(position + 1) is GalleryItem.Media
				holder.bindView(
					mapActivity, item, nightMode, selectionMode,
					isItemSelected(item.mediaItem), showDivider
				)
			}
			holder is ActionViewHolder && item is GalleryItem.Action ->
				holder.bindView(nightMode, mapActivity, item)
			holder is NoMediaHolder && item is GalleryItem.NoMedia ->
				holder.bindView(item, nightMode)
			holder is NoInternetHolder && item is GalleryItem.NoInternet ->
				holder.bindView(nightMode, loadingImages)
			holder is MediaCountHolder && item is GalleryItem.MediaCount ->
				holder.bindView(getMediaItemsCount(), nightMode)
			holder is MediaStatsHolder && item is GalleryItem.MediaStats ->
				holder.bindView(item, nightMode, displayMode == GalleryDisplayMode.GRID)
			holder is SortBarHolder && item is GalleryItem.SortBar ->
				holder.bindView(item, nightMode, displayMode == GalleryDisplayMode.GRID)
		}
	}

	override fun onBindViewHolder(
		holder: RecyclerView.ViewHolder,
		position: Int,
		payloads: MutableList<Any>
	) {
		when {
			payloads.isEmpty() -> super.onBindViewHolder(holder, position, payloads)

			payloads[0] == UPDATE_PROGRESS_BAR_PAYLOAD_TYPE ->
				if (holder is NoInternetHolder) holder.updateProgressBar(loadingImages)

			payloads[0] == SELECTION_PAYLOAD_TYPE -> {
				val item = items[position]
				if (item is GalleryItem.Media) {
					val selected = isItemSelected(item.mediaItem)
					when (holder) {
						is GalleryMediaViewHolder -> holder.updateSelection(selectionMode, selected, nightMode)
						is GalleryMediaListViewHolder -> holder.updateSelection(selectionMode, selected, nightMode)
					}
				}
			}

			payloads[0] == METADATA_PAYLOAD_TYPE -> {
				val item = items[position]
				if (item is GalleryItem.Media) {
					when (holder) {
						is GalleryMediaViewHolder -> holder.updateMetadata(item)
						is GalleryMediaListViewHolder -> holder.updateMetadata(item)
					}
				}
			}

			else -> super.onBindViewHolder(holder, position, payloads)
		}
	}

	fun notifySelectionChanged() {
		notifyItemRangeChanged(0, itemCount, SELECTION_PAYLOAD_TYPE)
	}

	override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
		when (holder) {
			is GalleryMediaViewHolder -> holder.cancelLoadingImage()
			is GalleryMediaListViewHolder -> holder.cancelLoadingImage()
		}
		super.onViewRecycled(holder)
	}

	fun onLoadingImages(loadingImages: Boolean) {
		this.loadingImages = loadingImages
		for (i in items.indices) {
			if (items[i] is GalleryItem.NoInternet) {
				notifyItemChanged(i, UPDATE_PROGRESS_BAR_PAYLOAD_TYPE)
			}
		}
	}

	fun getItem(position: Int) = items[position]

	fun isRegularMediaItemOnPosition(position: Int) = getItemViewType(position) == MEDIA_TYPE

	override fun getItemCount(): Int = items.size

	override fun getItemViewType(position: Int): Int = when (val item = items[position]) {
		is GalleryItem.Media ->
			if (displayMode == GalleryDisplayMode.LIST) {
				LIST_MEDIA_TYPE
			} else if (position == 0) {
				MAIN_MEDIA_TYPE
			} else {
				MEDIA_TYPE
			}
		is GalleryItem.Action -> ACTION_VIEW_TYPE
		is GalleryItem.NoMedia -> NO_MEDIA_TYPE
		is GalleryItem.NoInternet -> NO_INTERNET_TYPE
		is GalleryItem.MediaCount -> MEDIA_COUNT_TYPE
		is GalleryItem.MediaStats -> MEDIA_STATS_TYPE
		is GalleryItem.SortBar -> SORT_BAR_TYPE
	}

	fun getAnimator(): RecyclerView.ItemAnimator = object : DefaultItemAnimator() {
		override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder) = true
	}

	private fun inflate(resourceId: Int, root: ViewGroup, attachToRoot: Boolean = false): View =
		themedInflater.inflate(resourceId, root, attachToRoot)

	private fun getMediaItemsCount(): Int = items.count { it is GalleryItem.Media }

	companion object {
		private const val MAIN_MEDIA_TYPE = 0
		private const val MEDIA_TYPE = 1
		private const val ACTION_VIEW_TYPE = 2
		private const val NO_INTERNET_TYPE = 3
		private const val MEDIA_COUNT_TYPE = 4
		private const val NO_MEDIA_TYPE = 5
		private const val LIST_MEDIA_TYPE = 6
		private const val MEDIA_STATS_TYPE = 7
		private const val SORT_BAR_TYPE = 8

		private const val UPDATE_PROGRESS_BAR_PAYLOAD_TYPE = 1
		private const val SELECTION_PAYLOAD_TYPE = 2
		private const val METADATA_PAYLOAD_TYPE = 3
	}
}
