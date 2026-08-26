package net.osmand.plus.gallery.ui.holders

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.media.MediaProvider
import net.osmand.shared.media.MediaUriResolver
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.ImageLoadSource
import net.osmand.shared.util.ImageLoaderCallback
import net.osmand.shared.util.ImageRequestListener
import net.osmand.shared.util.LoadingImage

class GalleryMediaViewHolder(
	private val app: OsmandApplication,
	itemView: View,
	private val onMediaItemClicked: (MediaItem) -> Unit,
	private val isLoadFailed: (MediaItem) -> Boolean,
	private val onLoadFailed: (MediaItem) -> Unit,
	private val mediaProvider: MediaProvider,
	private val onMediaItemLongClicked: (MediaItem) -> Unit = {},
	private val onToggleSelection: (MediaItem) -> Unit = {},
	posterLoader: MediaPosterLoader? = null
) : RecyclerView.ViewHolder(itemView) {

	private val ivImage: ImageView = itemView.findViewById(R.id.image)
	private val ivSourceType: ImageView = itemView.findViewById(R.id.source_type)
	private val ivLoadSourceType: ImageView = itemView.findViewById(R.id.load_source_type)

	private val previewDelegate = MediaPreviewDelegate(
		app, ivImage,
		videoScrim = itemView.findViewById(R.id.video_scrim),
		playIcon = itemView.findViewById(R.id.play_icon),
		durationText = itemView.findViewById(R.id.duration_text),
		posterLoader = posterLoader
	)

	private val tvUrl: TextView = itemView.findViewById(R.id.url)
	private val border: View = itemView.findViewById(R.id.card_outline)
	private val progressBar: ProgressBar = itemView.findViewById(R.id.progress)
	private val selectionOverlay: View = itemView.findViewById(R.id.selection_overlay)
	private val selectionCheck: CompoundButton = itemView.findViewById(R.id.selection_check)
	private val clickOverlay: View = itemView.findViewById(R.id.click_overlay)

	private val iconsCache = app.uiUtilities

	private var loadingImage: LoadingImage? = null

	private var mapActivity: MapActivity? = null
	private var nightMode: Boolean = false
	private var imageSizePx: Int = 0
	var holderType: MediaHolderType = MediaHolderType.STANDARD

	private var boundMediaItem: MediaItem? = null
	private var selectionMode: Boolean = false

	val boundItemId: String?
		get() = boundMediaItem?.id

	val morphPreviewSnapshotView
		get() = previewDelegate.morphPreviewSnapshotView

	val morphCenterIcon
		get() = previewDelegate.morphCenterIcon

	val morphShowsScrim
		get() = previewDelegate.morphShowsScrim

	val morphDurationLabel
		get() = previewDelegate.morphDurationLabel

	val morphShowsDuration
		get() = previewDelegate.morphShowsDuration

	val morphDurationTextColor
		get() = previewDelegate.morphDurationTextColor

	val morphBgColor: Int
		get() = previewDelegate.placeholderBgColor

	init {
		clickOverlay.setOnClickListener {
			val item = boundMediaItem ?: return@setOnClickListener
			if (selectionMode) onToggleSelection(item) else onMediaItemClicked(item)
		}
		clickOverlay.setOnLongClickListener {
			val item = boundMediaItem ?: return@setOnLongClickListener false
			onMediaItemLongClicked(item)
			true
		}
	}

	fun bindView(
		mapActivity: MapActivity,
		galleryItem: GalleryItem.Media,
		imageSizePx: Int,
		holderType: MediaHolderType,
		nightMode: Boolean,
		selectionMode: Boolean = false,
		selected: Boolean = false
	) {
		this.mapActivity = mapActivity
		this.nightMode = nightMode
		this.imageSizePx = imageSizePx
		this.holderType = holderType

		val mediaItem = galleryItem.mediaItem
		boundMediaItem = mediaItem
		cancelLoadingImage()
		setupView(imageSizePx, nightMode)
		bindSelection(selectionMode, selected, nightMode)

		val iconName = mediaItem.origin.iconName
		val topIconId = AndroidUtils.getDrawableId(app, iconName)

		if (holderType == MediaHolderType.MAIN && topIconId != 0) {
			setSourceTypeIcon(iconsCache.getIcon(topIconId))
		} else {
			setSourceTypeIcon(null)
		}

		AndroidUtils.setBackground(mapActivity, border, getBackgroundId(nightMode))
		progressBar.visibility = if (galleryItem.showLoadingProgress) View.VISIBLE else View.GONE
		ivImage.setOnClickListener(null)
		ivLoadSourceType.visibility = View.GONE

		previewDelegate.large = holderType == MediaHolderType.MAIN
				|| holderType == MediaHolderType.SPAN_RESIZABLE

		previewDelegate.bind(mediaItem, nightMode, galleryItem.presentation?.durationLabel) {
			onPosterShown()
		}
		tvUrl.visibility = View.GONE
		border.visibility = View.GONE

		if (isLoadFailed(mediaItem)) {
			bindUrl(mediaItem)
		} else if (mediaItem.type == MediaType.PHOTO) {
			tryLoadImage(mediaItem)
		}
	}

	fun updateMetadata(galleryItem: GalleryItem.Media) {
		if (boundMediaItem?.id != galleryItem.mediaItem.id) return
		previewDelegate.updateDurationLabel(galleryItem.presentation?.durationLabel)
	}

	private fun onPosterShown() {
		val layoutParams = FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
		)
		layoutParams.gravity = Gravity.CENTER
		ivImage.layoutParams = layoutParams
		border.visibility = View.GONE
		progressBar.visibility = View.GONE
	}

	private fun tryLoadImage(mediaItem: MediaItem) {
		loadingImage = mediaProvider.loadStandardSizeImage(mediaItem, object : ImageLoaderCallback {
			override fun onStart(bitmap: Bitmap?) {}

			override fun onSuccess(bitmap: Bitmap) {
				bindImage(mediaItem)
				ivImage.setImageDrawable(BitmapDrawable(ivImage.resources, bitmap))
			}

			override fun onError() {
				if (!app.settings.isInternetConnectionAvailable) {
					tryLoadCacheHiResImage(mediaItem)
				} else {
					onLoadFailed(mediaItem)
					bindUrl(mediaItem)
				}
			}
		}, object : ImageRequestListener {
			override fun onSuccess(source: ImageLoadSource) {
				updateLoadSource(source)
			}
		}, imageSizePx)
	}

	private fun tryLoadCacheHiResImage(mediaItem: MediaItem) {
		loadingImage = mediaProvider.loadFullSizeImage(mediaItem, object : ImageLoaderCallback {
			override fun onStart(bitmap: Bitmap?) {}

			override fun onSuccess(bitmap: Bitmap) {
				bindImage(mediaItem)
				ivImage.setImageDrawable(BitmapDrawable(ivImage.resources, bitmap))
			}

			override fun onError() {
				onLoadFailed(mediaItem)
				bindUrl(mediaItem)
			}
		}, object : ImageRequestListener {
			override fun onSuccess(source: ImageLoadSource) {
				updateLoadSource(source)
			}
		}, imageSizePx)
	}

	private fun updateLoadSource(source: ImageLoadSource?) {
		if (!app.settings.isInternetConnectionAvailable && ImageLoadSource.NETWORK != source) {
			ivLoadSourceType.visibility = View.VISIBLE
		} else {
			ivLoadSourceType.visibility = View.GONE
		}
	}

	private fun bindImage(mediaItem: MediaItem) {
		val layoutParams = FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
		)
		layoutParams.gravity = Gravity.CENTER
		ivImage.visibility = View.VISIBLE
		ivImage.layoutParams = layoutParams
		ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
		previewDelegate.onPhotoPreviewShown()

		tvUrl.visibility = View.GONE
		border.visibility = View.GONE
		progressBar.visibility = View.GONE
	}

	private fun bindUrl(mediaItem: MediaItem) {
		ivImage.visibility = View.GONE
		tvUrl.visibility = View.VISIBLE

		val displayUri = MediaUriResolver.getFailedLoadDisplayUri(mediaItem)
		if (displayUri != null) {
			tvUrl.text = displayUri
			tvUrl.setOnClickListener {
				mapActivity?.let { AndroidUtils.openUrl(it, displayUri, nightMode) }
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

	fun updateSelection(selectionMode: Boolean, selected: Boolean, nightMode: Boolean) {
		bindSelection(selectionMode, selected, nightMode)
	}

	private fun bindSelection(selectionMode: Boolean, selected: Boolean, nightMode: Boolean) {
		this.selectionMode = selectionMode
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		selectionOverlay.setBackgroundColor(ColorUtilities.getColorWithAlpha(activeColor, SELECTION_TINT_ALPHA))
		AndroidUiHelper.updateVisibility(selectionOverlay, selectionMode && selected)
		AndroidUiHelper.updateVisibility(selectionCheck, selectionMode)
		selectionCheck.isChecked = selected
		UiUtilities.setupCompoundButton(nightMode, activeColor, selectionCheck)
	}

	fun cancelLoadingImage() {
		loadingImage?.cancel()
		loadingImage = null
		previewDelegate.cancel()
	}

	private fun setupView(sizeInPx: Int, nightMode: Boolean) {
		val layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
		itemView.layoutParams = layoutParams
		itemView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode))
	}

	private fun getBackgroundId(nightMode: Boolean) =
		if (nightMode) R.drawable.context_menu_card_dark else R.drawable.context_menu_card_light

	companion object {
		private const val SELECTION_TINT_ALPHA = 0.3f
	}
}
