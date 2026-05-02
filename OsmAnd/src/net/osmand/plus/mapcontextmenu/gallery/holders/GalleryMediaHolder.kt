package net.osmand.plus.mapcontextmenu.gallery.holders

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
import net.osmand.plus.gallery.GalleryItem
import net.osmand.plus.gallery.MediaProvider
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.mapcontextmenu.gallery.GalleryController
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridItemDecorator.GRID_SCREEN_ITEM_SPACE_DP
import net.osmand.plus.mapcontextmenu.gallery.GalleryListener
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.util.ImageLoadSource
import net.osmand.shared.util.ImageLoaderCallback
import net.osmand.shared.util.ImageRequestListener
import net.osmand.shared.util.LoadingImage

class GalleryMediaHolder(
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

	var holderType: ImageHolderType = ImageHolderType.STANDARD
		private set

	private var loadingImage: LoadingImage? = null

	fun bindView(
		mapActivity: MapActivity,
		listener: GalleryListener,
		galleryItem: GalleryItem.Media,
		type: ImageHolderType,
		viewWidth: Int?,
		mediaProvider: MediaProvider,
		nightMode: Boolean
	) {
		this.holderType = type
		setupView(mapActivity, viewWidth, nightMode)

		val mediaItem = galleryItem.mediaItem
		val iconName = mediaItem.origin.iconName
		val topIconId = if (iconName != null) AndroidUtils.getDrawableId(app, iconName) else 0

		if (holderType == ImageHolderType.MAIN && topIconId != 0) {
			setSourceTypeIcon(iconsCache.getIcon(topIconId))
		} else {
			setSourceTypeIcon(null)
		}

		AndroidUtils.setBackground(mapActivity, border, getBackgroundId(nightMode))
		progressBar.visibility = if (galleryItem.showProgress) View.VISIBLE else View.GONE
		ivImage.setImageDrawable(null)

		if (galleryItem.hasError) {
			bindUrl(mapActivity, mediaItem, nightMode)
		} else {
			tryLoadImage(mapActivity, listener, galleryItem, mediaProvider, nightMode)
		}
	}

	private fun tryLoadImage(
		mapActivity: MapActivity,
		listener: GalleryListener,
		galleryItem: GalleryItem.Media,
		mediaProvider: MediaProvider,
		nightMode: Boolean
	) {
		loadingImage?.cancel()
		val mediaItem = galleryItem.mediaItem

		loadingImage = mediaProvider.loadPreview(mediaItem, object : ImageLoaderCallback {
			override fun onStart(bitmap: Bitmap?) {}

			override fun onSuccess(bitmap: Bitmap) {
				bindImage(listener, mediaItem)
				ivImage.setImageDrawable(BitmapDrawable(ivImage.resources, bitmap))
			}

			override fun onError() {
				if (!app.settings.isInternetConnectionAvailable) {
					tryLoadCacheHiResImage(
						mapActivity, listener, galleryItem, mediaProvider, nightMode
					)
				} else {
					galleryItem.hasError = true
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
		nightMode: Boolean
	) {
		val mediaItem = galleryItem.mediaItem
		loadingImage = mediaProvider.loadFull(mediaItem, object : ImageLoaderCallback {
			override fun onStart(bitmap: Bitmap?) {}

			override fun onSuccess(bitmap: Bitmap) {
				bindImage(listener, mediaItem)
				ivImage.setImageDrawable(BitmapDrawable(ivImage.resources, bitmap))
			}

			override fun onError() {
				galleryItem.hasError = true
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

		val displayUrl = getDisplayUrl(mediaItem)
		tvUrl.text = displayUrl
		tvUrl.setOnClickListener { AndroidUtils.openUrl(mapActivity, displayUrl, nightMode) }

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
		val sizeInPx = if (holderType == ImageHolderType.SPAN_RESIZABLE) {
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
			if (holderType == ImageHolderType.MAIN) mainPhotoSizeDp else standardPhotoSizeDp
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

	private fun getDisplayUrl(mediaItem: MediaItem): String {
		val details = mediaItem.details
		val resource = mediaItem.resource

		return when {
			details.viewUrl.isNotEmpty() -> details.viewUrl
			!resource.fullUri.isNullOrEmpty() -> resource.fullUri ?: mediaItem.sourceUri
			else -> mediaItem.sourceUri
		}
	}

	private fun getBackgroundId(nightMode: Boolean) =
		if (nightMode) R.drawable.context_menu_card_dark else R.drawable.context_menu_card_light
}