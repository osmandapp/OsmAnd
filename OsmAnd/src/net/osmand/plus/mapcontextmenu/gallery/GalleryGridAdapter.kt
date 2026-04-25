package net.osmand.plus.mapcontextmenu.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.GalleryItem
import net.osmand.plus.gallery.MediaProvider
import net.osmand.plus.mapcontextmenu.gallery.holders.GalleryMediaHolder
import net.osmand.plus.mapcontextmenu.gallery.holders.ImageHolderType
import net.osmand.plus.mapcontextmenu.gallery.holders.MediaCountHolder
import net.osmand.plus.mapcontextmenu.gallery.holders.MapillaryContributeHolder
import net.osmand.plus.mapcontextmenu.gallery.holders.NoImagesHolder
import net.osmand.plus.mapcontextmenu.gallery.holders.NoInternetHolder
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.media.domain.MediaItem

class GalleryGridAdapter(
	private val mapActivity: MapActivity,
	private val listener: GalleryListener,
	private val viewWidth: Int?,
	private val isOnlinePhotos: Boolean,
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
		if (isOnlinePhotos) {
			items.addAll(newItems)
		} else {
			val limitedItems = mutableListOf<GalleryItem>()
			var addedMapillaryCards = 0

			for (item in newItems) {
				if (item is GalleryItem.Media && item.mediaItem is MediaItem.Mapillary) {
					if (addedMapillaryCards < 5) {
						limitedItems.add(item)
						addedMapillaryCards++
					}
				} else {
					limitedItems.add(item)
				}
			}
			items.addAll(limitedItems)
		}
		notifyDataSetChanged()
	}

	fun getItems(): List<GalleryItem> = items

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			MAIN_IMAGE_TYPE, IMAGE_TYPE -> {
				val itemView = inflate(R.layout.gallery_card_item, parent)
				GalleryMediaHolder(app, itemView)
			}
			MAPILLARY_CONTRIBUTE_TYPE -> {
				val itemView = inflate(R.layout.context_menu_card_add_mapillary_images, parent)
				MapillaryContributeHolder(itemView)
			}
			NO_IMAGES_TYPE -> {
				val itemView = inflate(R.layout.no_image_card, parent)
				NoImagesHolder(itemView, app)
			}
			NO_INTERNET_TYPE -> {
				val itemView = inflate(R.layout.no_internet_card, parent)
				NoInternetHolder(itemView, app)
			}
			IMAGES_COUNT_TYPE -> {
				val itemView = inflate(R.layout.images_count_item, parent)
				MediaCountHolder(itemView, app)
			}
			else -> throw IllegalArgumentException("Unsupported view type: $viewType")
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]

		when {
			holder is GalleryMediaHolder && item is GalleryItem.Media -> {
				val type = when {
					resizeBySpanCount -> ImageHolderType.SPAN_RESIZABLE
					position == 0 -> ImageHolderType.MAIN
					else -> ImageHolderType.STANDARD
				}
				holder.bindView(mapActivity, listener, item, type, viewWidth, mediaProvider, nightMode)
			}
			holder is MapillaryContributeHolder -> {
				holder.bindView(nightMode, mapActivity) // todo don't need bind, just setup once
			}
			holder is NoImagesHolder -> {
				holder.bindView(nightMode, mapActivity, isOnlinePhotos) // todo don't need bind, just setup once
			}
			holder is NoInternetHolder && item is GalleryItem.NoInternet -> {
				holder.bindView(nightMode, listener, item.isLoading)
			}
			holder is MediaCountHolder && item is GalleryItem.ImagesCount -> {
				// TODO: show only media items count
				holder.bindView(items.size - 1, nightMode) // todo don't need bind, just setup once
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

	fun onLoadingImages(loadingImages: Boolean) {
		this.loadingImages = loadingImages
		for (i in items.indices) {
			val item = items[i]
			if (item is GalleryItem.NoInternet) {
				item.isLoading = loadingImages // TODO: changed logic
				notifyItemChanged(i, UPDATE_PROGRESS_BAR_PAYLOAD_TYPE)
			}
		}
	}

	override fun getItemCount(): Int = items.size

	override fun getItemViewType(position: Int): Int {
		return when (items[position]) {
			is GalleryItem.Media -> if (position == 0) MAIN_IMAGE_TYPE else IMAGE_TYPE
			is GalleryItem.MapillaryContribute -> MAPILLARY_CONTRIBUTE_TYPE
			is GalleryItem.NoImages -> NO_IMAGES_TYPE
			is GalleryItem.NoInternet -> NO_INTERNET_TYPE
			is GalleryItem.ImagesCount -> IMAGES_COUNT_TYPE
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

	fun inflate(resourceId: Int, root: ViewGroup, attachToRoot: Boolean = false): View {
		return themedInflater.inflate(resourceId, root, attachToRoot)
	}

	companion object {
		private const val MAIN_IMAGE_TYPE = 0
		const val IMAGE_TYPE = 1
		private const val MAPILLARY_CONTRIBUTE_TYPE = 3
		private const val NO_IMAGES_TYPE = 4
		const val NO_INTERNET_TYPE = 5
		const val IMAGES_COUNT_TYPE = 6

		private const val UPDATE_PROGRESS_BAR_PAYLOAD_TYPE = 1
	}
}