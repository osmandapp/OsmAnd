package net.osmand.plus.gallery.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.contract.IGalleryActionListener
import net.osmand.plus.gallery.contract.IGalleryListener
import net.osmand.plus.gallery.data.MediaLoadStateRegistry
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.holders.ActionViewHolder
import net.osmand.plus.gallery.ui.holders.GalleryMediaViewHolder
import net.osmand.plus.gallery.ui.holders.MediaCountHolder
import net.osmand.plus.gallery.ui.holders.MediaHolderType
import net.osmand.plus.gallery.ui.holders.NoInternetHolder
import net.osmand.plus.gallery.ui.holders.NoMediaHolder
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.media.MediaProvider

class GalleryGridAdapter(
	private val mapActivity: MapActivity,
	private val galleryListener: IGalleryListener,
	private val actionListener: IGalleryActionListener,
	private val viewWidth: Int?,
	private val nightMode: Boolean,
	private val loadStateRegistry: MediaLoadStateRegistry
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private val app: OsmandApplication = mapActivity.app
	private val themedInflater: LayoutInflater = UiUtilities.getInflater(mapActivity, nightMode)
	private val mediaProvider = MediaProvider(app)
	private val items = mutableListOf<GalleryItem>()

	private var resizeBySpanCount = false
	private var loadingImages = false

	fun setItems(newItems: List<GalleryItem>) {
		items.clear()
		items.addAll(newItems)
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			MAIN_MEDIA_TYPE, MEDIA_TYPE -> {
				GalleryMediaViewHolder(app, inflate(R.layout.gallery_card_item, parent))
			}
			ACTION_VIEW_TYPE -> {
				ActionViewHolder(inflate(R.layout.context_menu_card_gallery_action_view, parent))
			}
			NO_MEDIA_TYPE -> {
				NoMediaHolder(app, inflate(R.layout.no_image_card, parent))
			}
			NO_INTERNET_TYPE -> {
				NoInternetHolder(inflate(R.layout.no_internet_card, parent), app)
			}
			MEDIA_COUNT_TYPE -> {
				MediaCountHolder(inflate(R.layout.images_count_item, parent), app)
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
					mapActivity, galleryListener, item, type, viewWidth,
					mediaProvider, nightMode, loadStateRegistry
				)
			}
			holder is ActionViewHolder && item is GalleryItem.Action ->
				holder.bindView(nightMode, mapActivity, item, actionListener)
			holder is NoMediaHolder && item is GalleryItem.NoMedia ->
				holder.bindView(item, nightMode, actionListener)
			holder is NoInternetHolder && item is GalleryItem.NoInternet ->
				holder.bindView(nightMode, galleryListener, loadingImages)
			holder is MediaCountHolder && item is GalleryItem.MediaCount ->
				holder.bindView(getMediaItemsCount(), nightMode)
		}
	}

	override fun onBindViewHolder(
		holder: RecyclerView.ViewHolder,
		position: Int,
		payloads: MutableList<Any>
	) {
		if (payloads.isNotEmpty() && payloads[0] == UPDATE_PROGRESS_BAR_PAYLOAD_TYPE) {
			if (holder is NoInternetHolder) holder.updateProgressBar(loadingImages)
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
			if (items[i] is GalleryItem.NoInternet) {
				notifyItemChanged(i, UPDATE_PROGRESS_BAR_PAYLOAD_TYPE)
			}
		}
	}

	fun getItem(position: Int) = items[position]

	fun isRegularMediaItemOnPosition(position: Int) = getItemViewType(position) == MEDIA_TYPE

	override fun getItemCount(): Int = items.size

	override fun getItemViewType(position: Int): Int = when (items[position]) {
		is GalleryItem.Media -> if (position == 0) MAIN_MEDIA_TYPE else MEDIA_TYPE
		is GalleryItem.Action -> ACTION_VIEW_TYPE
		is GalleryItem.NoMedia -> NO_MEDIA_TYPE
		is GalleryItem.NoInternet -> NO_INTERNET_TYPE
		is GalleryItem.MediaCount -> MEDIA_COUNT_TYPE
	}

	fun getAnimator(): RecyclerView.ItemAnimator = object : DefaultItemAnimator() {
		override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder) = true
	}

	fun setResizeBySpanCount(resizeBySpanCount: Boolean) {
		this.resizeBySpanCount = resizeBySpanCount
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

		private const val UPDATE_PROGRESS_BAR_PAYLOAD_TYPE = 1
	}
}
