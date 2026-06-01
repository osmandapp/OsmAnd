package net.osmand.plus.gallery.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.controller.GalleryMediaLoadStateProvider
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.holders.GalleryMediaViewHolder
import net.osmand.plus.gallery.ui.holders.MediaHolderType
import net.osmand.plus.gallery.ui.holders.MediaCountHolder
import net.osmand.plus.gallery.ui.holders.ActionViewHolder
import net.osmand.plus.gallery.ui.holders.NoMediaHolder
import net.osmand.plus.gallery.ui.holders.NoInternetHolder
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.media.MediaProvider

class GalleryGridAdapter(
	private val mapActivity: MapActivity,
	private val listener: GalleryListener,
	private val mediaLoadStateProvider: GalleryMediaLoadStateProvider,
	private val viewWidth: Int?,
	private val config: GalleryGridConfig = GalleryGridConfig(),
	private val nightMode: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private val app: OsmandApplication = mapActivity.app
	private val themedInflater: LayoutInflater = UiUtilities.getInflater(mapActivity, nightMode)

	private val mediaProvider = MediaProvider(app)
	private val items = mutableListOf<GalleryItem>()

	private var resizeBySpanCount = false
	private var loadingImages = false

	fun setItems(newItems: List<GalleryItem>) {
		items.clear()
		items.addAll(applyMediaItemsLimit(newItems))
		notifyDataSetChanged()
	}

	private fun applyMediaItemsLimit(newItems: List<GalleryItem>): List<GalleryItem> {
		val limit = config.mediaItemsLimit ?: return newItems

		val limitedItems = mutableListOf<GalleryItem>()
		var limitedMediaItemsCount = 0

		for (item in newItems) {
			if (item is GalleryItem.Media && config.mediaItemLimitPredicate(item.mediaItem)) {
				if (limitedMediaItemsCount < limit) {
					limitedItems.add(item)
					limitedMediaItemsCount++
				}
			} else {
				limitedItems.add(item)
			}
		}
		return limitedItems
	}

	fun getItems(): List<GalleryItem> = items

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			MAIN_MEDIA_TYPE, MEDIA_TYPE -> {
				val itemView = inflate(R.layout.gallery_card_item, parent)
				GalleryMediaViewHolder(app, itemView)
			}
			ACTION_VIEW_TYPE -> {
				val itemView = inflate(R.layout.context_menu_card_gallery_action_view, parent)
				ActionViewHolder(itemView)
			}
			NO_MEDIA_TYPE -> {
				val itemView = inflate(R.layout.no_image_card, parent)
				NoMediaHolder(itemView, app)
			}
			NO_INTERNET_TYPE -> {
				val itemView = inflate(R.layout.no_internet_card, parent)
				NoInternetHolder(itemView, app)
			}
			MEDIA_COUNT_TYPE -> {
				val itemView = inflate(R.layout.images_count_item, parent)
				MediaCountHolder(itemView, app)
			}
			else -> throw IllegalArgumentException("Unsupported view type: $viewType")
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]

		when {
			holder is GalleryMediaViewHolder && item is GalleryItem.Media -> {
				val type = when {
					resizeBySpanCount -> MediaHolderType.SPAN_RESIZABLE
					position == 0 -> MediaHolderType.MAIN
					else -> MediaHolderType.STANDARD
				}
				holder.bindView(
					mapActivity, listener, item, type, viewWidth,
					mediaProvider, mediaLoadStateProvider, nightMode
				)
			}
			holder is ActionViewHolder && item is GalleryItem.Action -> {
				holder.bindView(nightMode, mapActivity, item)
			}
			holder is NoMediaHolder && item is GalleryItem.NoMedia -> {
				holder.bindView(nightMode, item)
			}
			holder is NoInternetHolder && item is GalleryItem.NoInternet -> {
				holder.bindView(nightMode, listener, loadingImages)
			}
			holder is MediaCountHolder && item is GalleryItem.MediaCount -> {
				holder.bindView(getMediaItemsCount(), nightMode)
			}
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
		if (payloads.isNotEmpty() && payloads[0] == UPDATE_PROGRESS_BAR_PAYLOAD_TYPE) {
			if (holder is NoInternetHolder) {
				holder.updateProgressBar(loadingImages)
			}
		} else {
			super.onBindViewHolder(holder, position, payloads)
		}
	}

	override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
		if (holder is GalleryMediaViewHolder) {
			holder.cancelLoadingImage()
		}
		super.onViewRecycled(holder)
	}

	fun onLoadingImages(loadingImages: Boolean) {
		this.loadingImages = loadingImages
		for (i in items.indices) {
			val item = items[i]
			if (item is GalleryItem.NoInternet) {
				notifyItemChanged(i, UPDATE_PROGRESS_BAR_PAYLOAD_TYPE)
			}
		}
	}

	fun getItem(position: Int) = items[position]

	fun isRegularMediaItemOnPosition(position: Int) = getItemViewType(position) == MEDIA_TYPE

	override fun getItemCount(): Int = items.size

	override fun getItemViewType(position: Int): Int {
		return when (items[position]) {
			is GalleryItem.Media -> if (position == 0) MAIN_MEDIA_TYPE else MEDIA_TYPE
			is GalleryItem.Action -> ACTION_VIEW_TYPE
			is GalleryItem.NoMedia -> NO_MEDIA_TYPE
			is GalleryItem.NoInternet -> NO_INTERNET_TYPE
			is GalleryItem.MediaCount -> MEDIA_COUNT_TYPE
		}
	}

	fun getAnimator(): RecyclerView.ItemAnimator {
		return object : DefaultItemAnimator() {
			override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
				return true
			}
		}
	}

	fun setResizeBySpanCount(resizeBySpanCount: Boolean) {
		this.resizeBySpanCount = resizeBySpanCount
	}

	private fun inflate(resourceId: Int, root: ViewGroup, attachToRoot: Boolean = false): View {
		return themedInflater.inflate(resourceId, root, attachToRoot)
	}

	private fun getMediaItemsCount(): Int {
		return items.count { it is GalleryItem.Media }
	}

	companion object {
		private const val MAIN_MEDIA_TYPE = 0
		private const val MEDIA_TYPE = 1
		private const val ACTION_VIEW_TYPE = 3
		private const val NO_MEDIA_TYPE = 4
		private const val NO_INTERNET_TYPE = 5
		private const val MEDIA_COUNT_TYPE = 6

		private const val UPDATE_PROGRESS_BAR_PAYLOAD_TYPE = 1
	}
}
