package net.osmand.plus.gallery.ui.holders

import android.graphics.Bitmap
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
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.helpers.AndroidUiHelper
import androidx.core.view.isVisible
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
) : RecyclerView.ViewHolder(itemView), MorphableMediaHolder {

	private val ivImage: ImageView = itemView.findViewById(R.id.image)
	private val ivSourceType: ImageView = itemView.findViewById(R.id.source_type)
	private val ivLoadSourceType: ImageView = itemView.findViewById(R.id.load_source_type)

	private val previewDelegate = MediaPreviewDelegate(
		app, ivImage,
		videoScrim = itemView.findViewById(R.id.video_scrim),
		playIcon = itemView.findViewById(R.id.play_icon),
		durationText = itemView.findViewById(R.id.duration_text),
		posterLoader = posterLoader,
		onPreviewShown = ::onPreviewShown
	)

	private val tvUrl: TextView = itemView.findViewById(R.id.url)
	private val border: View = itemView.findViewById(R.id.card_outline)
	private val progressBar: ProgressBar = itemView.findViewById(R.id.progress)
	private val selectionOverlay: View = itemView.findViewById(R.id.selection_overlay)
	private val selectionCheck: CompoundButton = itemView.findViewById(R.id.selection_check)
	private val clickOverlay: View = itemView.findViewById(R.id.click_overlay)

	private val iconsCache = app.uiUtilities

	private var loadingImage: LoadingImage? = null

	private var mapActivity: FragmentActivity? = null
	private var nightMode: Boolean = false
	private var imageSizePx: Int = 0
	var holderType: MediaHolderType = MediaHolderType.STANDARD

	private var boundMediaItem: MediaItem? = null
	private var selectionMode: Boolean = false

	override val boundItemId: String?
		get() = boundMediaItem?.id

	override val previewView: View
		get() = itemView

	override val morphSnapshotView
		get() = previewDelegate.morphPreviewSnapshotView

	override val morphPreviewBitmap
		get() = previewDelegate.morphPreviewBitmap

	override val morphCenterIcon
		get() = previewDelegate.morphCenterIcon

	override val morphShowsScrim
		get() = previewDelegate.morphShowsScrim

	override val morphDurationLabel
		get() = previewDelegate.morphDurationLabel

	override val morphShowsDuration
		get() = previewDelegate.morphShowsDuration

	override val morphDurationTextColor
		get() = previewDelegate.morphDurationTextColor

	override val morphBgColor: Int
		get() = previewDelegate.placeholderBgColor

	override fun getFadeableContentViews(): List<View> = emptyList()

	override fun getSelectionOverlayViews(): List<View> = listOf(selectionOverlay, selectionCheck).filter { it.isVisible }

	override fun beginMorph(standIn: Bitmap?, onPreviewArrived: (Bitmap) -> Unit) = previewDelegate.beginMorph(standIn, onPreviewArrived)

	override fun endMorph(revealed: Boolean) = previewDelegate.endMorph(revealed)

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
		mapActivity: FragmentActivity,
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
				previewDelegate.showPhotoPreview(bitmap)
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
				previewDelegate.showPhotoPreview(bitmap)
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

	private fun onPreviewShown() {
		val layoutParams = FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
		)
		layoutParams.gravity = Gravity.CENTER
		ivImage.visibility = View.VISIBLE
		ivImage.layoutParams = layoutParams

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
		val wasSelectionMode = this.selectionMode
		val overlayWasVisible = selectionOverlay.isVisible
		if (!GalleryMotion.animationsEnabled(app)) {
			bindSelection(selectionMode, selected, nightMode)
			return
		}
		bindSelection(selectionMode, selected, nightMode)
		val overlayVisible = selectionOverlay.isVisible
		if (selectionMode && !wasSelectionMode) {
			appear(selectionCheck)
			if (overlayVisible) appear(selectionOverlay)
		} else if (!selectionMode && wasSelectionMode) {
			disappear(selectionCheck)
			if (overlayWasVisible) disappear(selectionOverlay)
		} else if (overlayVisible && !overlayWasVisible) {
			appear(selectionOverlay)
		} else if (!overlayVisible && overlayWasVisible) {
			disappear(selectionOverlay)
		}
	}

	private fun appear(view: View) {
		view.animate().cancel()
		view.visibility = View.VISIBLE
		view.alpha = 0f
		view.scaleX = SELECTION_APPEAR_SCALE
		view.scaleY = SELECTION_APPEAR_SCALE
		view.animate().alpha(1f).scaleX(1f).scaleY(1f)
			.setDuration(GalleryMotion.FADE_DURATION_MS).setInterpolator(GalleryMotion.CURVE).withEndAction(null).start()
	}

	private fun disappear(view: View) {
		view.animate().cancel()
		view.visibility = View.VISIBLE
		view.animate().alpha(0f).scaleX(SELECTION_APPEAR_SCALE).scaleY(SELECTION_APPEAR_SCALE)
			.setDuration(GalleryMotion.FADE_DURATION_MS).setInterpolator(GalleryMotion.CURVE)
			.withEndAction {
				view.visibility = View.GONE
				resetAffordance(view)
			}.start()
	}

	private fun resetAffordance(view: View) {
		view.alpha = 1f
		view.scaleX = 1f
		view.scaleY = 1f
	}

	private fun bindSelection(selectionMode: Boolean, selected: Boolean, nightMode: Boolean) {
		this.selectionMode = selectionMode
		listOf(selectionCheck, selectionOverlay).forEach {
			it.animate().cancel()
			resetAffordance(it)
		}
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
		private const val SELECTION_APPEAR_SCALE = 0.8f
	}
}
