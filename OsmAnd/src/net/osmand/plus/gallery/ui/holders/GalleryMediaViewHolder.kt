package net.osmand.plus.gallery.ui.holders

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.provider.MediaProvider
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.gallery.controller.GalleryController
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator.GRID_SCREEN_ITEM_SPACE_DP
import net.osmand.plus.gallery.ui.GalleryListener
import net.osmand.plus.gallery.controller.GalleryMediaLoadStateProvider
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.media.MediaUriResolver
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.util.ImageLoadSource
import net.osmand.shared.util.ImageLoaderCallback
import net.osmand.shared.util.ImageRequestListener
import net.osmand.shared.util.LoadingImage

class GalleryMediaViewHolder(
	private val app: OsmandApplication,
	itemView: View
) : RecyclerView.ViewHolder(itemView) {

	private val ivImage: ImageView = itemView.findViewById(R.id.image)
	private val ivSourceType: ImageView = itemView.findViewById(R.id.source_type)
	private val ivLoadSourceType: ImageView = itemView.findViewById(R.id.load_source_type)

	private val tvUrl: TextView = itemView.findViewById(R.id.url)
	private val border: View = itemView.findViewById(R.id.card_outline)
	private val progressBar: ProgressBar = itemView.findViewById(R.id.progress)

	private val mainPhotoSizeDp = app.resources.getDimensionPixelSize(R.dimen.gallery_big_icon_size)
	private val standardPhotoSizeDp = app.resources.getDimensionPixelSize(R.dimen.gallery_standard_icon_size)

	private val iconsCache = app.uiUtilities

	var holderType: MediaHolderType = MediaHolderType.STANDARD
		private set

	private var loadingImage: LoadingImage? = null

	fun bindView(
		mapActivity: MapActivity,
		listener: GalleryListener,
		galleryItem: GalleryItem.Media,
		type: MediaHolderType,
		viewWidth: Int?,
		mediaProvider: MediaProvider,
		mediaLoadStateProvider: GalleryMediaLoadStateProvider,
		nightMode: Boolean
	) {
		this.holderType = type
		setupView(mapActivity, viewWidth, nightMode)

		val mediaItem = galleryItem.mediaItem
		val iconName = mediaItem.origin.iconName
		val topIconId = AndroidUtils.getDrawableId(app, iconName)

		if (holderType == MediaHolderType.MAIN && topIconId != 0) {
			setSourceTypeIcon(iconsCache.getIcon(topIconId))
		} else {
			setSourceTypeIcon(null)
		}

		AndroidUtils.setBackground(mapActivity, border, getBackgroundId(nightMode))
		progressBar.visibility = if (galleryItem.showLoadingProgress) View.VISIBLE else View.GONE
		ivImage.setImageDrawable(null)

		if (mediaLoadStateProvider.isMediaLoadFailed(mediaItem)) {
			bindUrl(mapActivity, mediaItem, nightMode)
		} else {
			tryLoadImage(
				mapActivity, listener, galleryItem, mediaProvider,
				mediaLoadStateProvider, nightMode
			)
		}
	}

	private fun tryLoadImage(
		mapActivity: MapActivity,
		listener: GalleryListener,
		galleryItem: GalleryItem.Media,
		mediaProvider: MediaProvider,
		mediaLoadStateProvider: GalleryMediaLoadStateProvider,
		nightMode: Boolean
	) {
		loadingImage?.cancel()
		val mediaItem = galleryItem.mediaItem

		loadingImage = mediaProvider.loadStandardSizeImage(mediaItem, object : ImageLoaderCallback {
			override fun onStart(bitmap: Bitmap?) {}

			override fun onSuccess(bitmap: Bitmap) {
				bindImage(listener, mediaItem)
				ivImage.setImageDrawable(BitmapDrawable(ivImage.resources, bitmap))
			}

			override fun onError() {
				if (!app.settings.isInternetConnectionAvailable) {
					tryLoadCacheHiResImage(
						mapActivity, listener, galleryItem, mediaProvider,
						mediaLoadStateProvider, nightMode
					)
				} else {
					mediaLoadStateProvider.markMediaLoadFailed(mediaItem)
					bindUrl(mapActivity, mediaItem, nightMode)
				}
			}
		}, object : ImageRequestListener {
			override fun onSuccess(source: ImageLoadSource) {
				updateLoadSource(source)
			}
		})
	}

	private fun tryLoadCacheHiResImage(
		mapActivity: MapActivity,
		listener: GalleryListener,
		galleryItem: GalleryItem.Media,
		mediaProvider: MediaProvider,
		mediaLoadStateProvider: GalleryMediaLoadStateProvider,
		nightMode: Boolean
	) {
		val mediaItem = galleryItem.mediaItem
		loadingImage = mediaProvider.loadFullSizeImage(mediaItem, object : ImageLoaderCallback {
			override fun onStart(bitmap: Bitmap?) {}

			override fun onSuccess(bitmap: Bitmap) {
				bindImage(listener, mediaItem)
				ivImage.setImageDrawable(BitmapDrawable(ivImage.resources, bitmap))
			}

			override fun onError() {
				mediaLoadStateProvider.markMediaLoadFailed(mediaItem)
				bindUrl(mapActivity, mediaItem, nightMode)
			}
		}, object : ImageRequestListener {
			override fun onSuccess(source: ImageLoadSource) {
				updateLoadSource(source)
			}
		})
	}

	private fun updateLoadSource(source: ImageLoadSource?) {
		if (!app.settings.isInternetConnectionAvailable && ImageLoadSource.NETWORK != source) {
			ivLoadSourceType.visibility = View.VISIBLE
		} else {
			ivLoadSourceType.visibility = View.GONE
		}
	}

	private fun bindImage(listener: GalleryListener, mediaItem: MediaItem) {
		val layoutParams = FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
		)
		layoutParams.gravity = Gravity.CENTER
		ivImage.visibility = View.VISIBLE
		ivImage.layoutParams = layoutParams
		ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
		ivImage.setOnClickListener { listener.onMediaItemClicked(mediaItem) }

		tvUrl.visibility = View.GONE
		border.visibility = View.GONE
		progressBar.visibility = View.GONE
	}

	private fun bindUrl(mapActivity: MapActivity, mediaItem: MediaItem, nightMode: Boolean) {
		ivImage.visibility = View.GONE
		tvUrl.visibility = View.VISIBLE

		val displayUri = MediaUriResolver.getFailedLoadDisplayUri(mediaItem)
		if (displayUri != null) {
			tvUrl.text = displayUri
			tvUrl.setOnClickListener {
				AndroidUtils.openUrl(mapActivity, displayUri, nightMode)
			}
		}

		border.visibility = View.VISIBLE
		progressBar.visibility = View.GONE
		updateLoadSource(null)
		setSourceTypeIcon(null)
	}

	private fun setSourceTypeIcon(icon: android.graphics.drawable.Drawable?) {
		AndroidUiHelper.updateVisibility(ivSourceType, icon != null)
		ivSourceType.setImageDrawable(icon)
	}

	private fun setupView(mapActivity: MapActivity, viewWidth: Int?, nightMode: Boolean) {
		val sizeInPx = if (holderType == MediaHolderType.SPAN_RESIZABLE) {
			val spanCount = GalleryController.getSettingsSpanCount(mapActivity)
			val recyclerViewPadding = AndroidUtils.dpToPx(app, 13f)
			val itemSpace = AndroidUtils.dpToPx(app, GRID_SCREEN_ITEM_SPACE_DP * 2f)
			val screenWidth = viewWidth ?: if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
				AndroidUtils.getScreenWidth(mapActivity)
			} else {
				AndroidUtils.getScreenHeight(mapActivity)
			}
			calculateItemSize(spanCount, recyclerViewPadding, itemSpace, screenWidth)
		} else {
			if (holderType == MediaHolderType.MAIN) mainPhotoSizeDp else standardPhotoSizeDp
		}

		val layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
		itemView.layoutParams = layoutParams
		itemView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode))
	}

	private fun calculateItemSize(
		spanCount: Int,
		recyclerViewPadding: Int,
		itemSpace: Int,
		screenWidth: Int
	): Int {
		val spaceForItems = screenWidth - (recyclerViewPadding * 2) - (spanCount * itemSpace)
		return spaceForItems / spanCount
	}

	private fun getBackgroundId(nightMode: Boolean) =
		if (nightMode) R.drawable.context_menu_card_dark else R.drawable.context_menu_card_light
}