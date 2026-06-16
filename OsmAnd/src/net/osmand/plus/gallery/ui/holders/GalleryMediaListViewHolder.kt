package net.osmand.plus.gallery.ui.holders

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.core.view.marginEnd
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.media.MediaProvider
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.ImageLoadSource
import net.osmand.shared.util.ImageLoaderCallback
import net.osmand.shared.util.ImageRequestListener
import net.osmand.shared.util.LoadingImage
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible

class GalleryMediaListViewHolder(
	private val app: OsmandApplication,
	itemView: View,
	private val mediaProvider: MediaProvider,
	private val onMediaItemClicked: (MediaItem) -> Unit,
	private val onMediaItemLongClicked: (MediaItem) -> Unit,
	private val onToggleSelection: (MediaItem) -> Unit,
	posterLoader: MediaPosterLoader? = null
) : RecyclerView.ViewHolder(itemView) {

	private val ivImage: ImageView = itemView.findViewById(R.id.image)
	private val tvTitle: TextView = itemView.findViewById(R.id.title)
	private val tvDescription: TextView = itemView.findViewById(R.id.description)
	private val selectionCheck: CompoundButton = itemView.findViewById(R.id.selection_check)
	private val divider: View = itemView.findViewById(R.id.divider)

	private val previewDelegate = MediaPreviewDelegate(
		app, ivImage,
		videoScrim = itemView.findViewById(R.id.video_scrim),
		playIcon = itemView.findViewById(R.id.play_icon),
		large = false,
		posterLoader = posterLoader
	)

	private val previewSizePx = app.resources.getDimensionPixelSize(R.dimen.gallery_list_preview_size)

	private var loadingImage: LoadingImage? = null

	private var boundMediaItem: MediaItem? = null
	private var selectionMode: Boolean = false

	val boundItemId: String?
		get() = boundMediaItem?.id

	val previewView: View
		get() = ivImage.parent as View

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

	/** Row content that fades in after the preview morph (everything except the preview). */
	fun getFadeableContentViews(): List<View> =
		listOf(tvTitle, tvDescription, selectionCheck, divider)
			.filter { it.visibility == View.VISIBLE }

	init {
		itemView.setOnClickListener {
			val item = boundMediaItem ?: return@setOnClickListener
			if (selectionMode) onToggleSelection(item) else onMediaItemClicked(item)
		}
		itemView.setOnLongClickListener {
			val item = boundMediaItem ?: return@setOnLongClickListener false
			onMediaItemLongClicked(item)
			true
		}
	}

	fun bindView(
		mapActivity: MapActivity,
		galleryItem: GalleryItem.Media,
		nightMode: Boolean,
		selectionMode: Boolean,
		selected: Boolean,
		showDivider: Boolean
	) {
		val mediaItem = galleryItem.mediaItem
		boundMediaItem = mediaItem
		cancelLoadingImage()

		tvTitle.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode))
		tvTitle.text = mediaItem.title
		tvDescription.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
		bindDescription(galleryItem)
		AndroidUiHelper.updateVisibility(divider, showDivider)

		bindPreview(galleryItem, nightMode)
		bindSelection(selectionMode, selected, nightMode, animate = false)
	}

	fun updateMetadata(galleryItem: GalleryItem.Media) {
		if (boundMediaItem?.id != galleryItem.mediaItem.id) return
		bindDescription(galleryItem)
		previewDelegate.updateDurationLabel(galleryItem.presentation?.durationLabel)
	}

	private fun bindDescription(galleryItem: GalleryItem.Media) {
		val description = galleryItem.presentation?.description
		tvDescription.text = description
		AndroidUiHelper.updateVisibility(tvDescription, !description.isNullOrEmpty())
	}

	private fun bindPreview(galleryItem: GalleryItem.Media, nightMode: Boolean) {
		val mediaItem = galleryItem.mediaItem
		previewDelegate.bind(mediaItem, nightMode, galleryItem.presentation?.durationLabel)
		if (mediaItem.type != MediaType.PHOTO) {
			return
		}
		loadingImage = mediaProvider.loadStandardSizeImage(mediaItem, object : ImageLoaderCallback {
			override fun onStart(bitmap: Bitmap?) {}

			override fun onSuccess(bitmap: Bitmap) {
				ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
				ivImage.setImageDrawable(bitmap.toDrawable(ivImage.resources))
				previewDelegate.onPhotoPreviewShown()
			}

			override fun onError() {
				// The delegate placeholder is already bound; nothing to do.
			}
		}, object : ImageRequestListener {
			override fun onSuccess(source: ImageLoadSource) {}
		}, previewSizePx)
	}

	fun updateSelection(selectionMode: Boolean, selected: Boolean, nightMode: Boolean) {
		bindSelection(selectionMode, selected, nightMode, animate = true)
	}

	private fun bindSelection(
		selectionMode: Boolean,
		selected: Boolean,
		nightMode: Boolean,
		animate: Boolean
	) {
		this.selectionMode = selectionMode
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		val bgColor = if (selectionMode && selected) {
			ColorUtilities.getColorWithAlpha(activeColor, ROW_SELECTED_ALPHA)
		} else {
			Color.TRANSPARENT
		}
		itemView.setBackgroundColor(bgColor)

		val wasVisible = selectionCheck.isVisible
		if (animate && selectionMode != wasVisible) {
			if (selectionMode) animateCheckboxIn() else animateCheckboxOut()
		} else if (!animate) {
			resetSelectionAnimation()
			AndroidUiHelper.updateVisibility(selectionCheck, selectionMode)
		}
		selectionCheck.isChecked = selected
		UiUtilities.setupCompoundButton(nightMode, activeColor, selectionCheck)
	}

	private fun animateCheckboxIn() {
		selectionCheck.visibility = View.VISIBLE
		val row = selectionCheck.parent as ViewGroup
		row.doOnPreDraw {
			val shift = -(selectionCheck.width + selectionCheck.marginEnd).toFloat()
			selectionCheck.translationX = shift
			selectionCheck.alpha = 0f
			selectionCheck.animate()
				.translationX(0f)
				.alpha(1f)
				.setDuration(CHECKBOX_ANIM_DURATION_MS)
				.setInterpolator(selectionInterpolator)
				.start()
			rowSiblings(row).forEach { view ->
				view.translationX = shift
				view.animate()
					.translationX(0f)
					.setDuration(CHECKBOX_ANIM_DURATION_MS)
					.setInterpolator(selectionInterpolator)
					.start()
			}
		}
	}

	private fun animateCheckboxOut() {
		val shift = (selectionCheck.width + selectionCheck.marginEnd).toFloat()
		selectionCheck.visibility = View.GONE
		val row = selectionCheck.parent as ViewGroup
		row.doOnPreDraw {
			rowSiblings(row).forEach { view ->
				view.translationX = shift
				view.animate()
					.translationX(0f)
					.setDuration(CHECKBOX_ANIM_DURATION_MS)
					.setInterpolator(selectionInterpolator)
					.start()
			}
		}
	}

	private fun rowSiblings(row: ViewGroup): List<View> =
		(0 until row.childCount).map(row::getChildAt).filter { it !== selectionCheck }

	private fun resetSelectionAnimation() {
		val row = selectionCheck.parent as ViewGroup
		for (i in 0 until row.childCount) {
			val child = row.getChildAt(i)
			child.animate().cancel()
			child.translationX = 0f
		}
		selectionCheck.alpha = 1f
	}

	fun cancelLoadingImage() {
		loadingImage?.cancel()
		loadingImage = null
		previewDelegate.cancel()
	}

	companion object {
		private const val ROW_SELECTED_ALPHA = 0.2f
		private const val CHECKBOX_ANIM_DURATION_MS = 200L

		private val selectionInterpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
	}
}