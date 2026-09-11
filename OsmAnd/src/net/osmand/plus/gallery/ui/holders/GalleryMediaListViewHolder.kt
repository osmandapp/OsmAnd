package net.osmand.plus.gallery.ui.holders

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.core.view.marginEnd
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.motion.GalleryMotion
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
import androidx.core.view.isVisible

open class GalleryMediaListViewHolder(
	private val app: OsmandApplication,
	itemView: View,
	private val mediaProvider: MediaProvider,
	private val onMediaItemClicked: (MediaItem) -> Unit,
	private val onMediaItemLongClicked: (MediaItem) -> Unit,
	private val onToggleSelection: (MediaItem) -> Unit,
	posterLoader: MediaPosterLoader? = null
) : RecyclerView.ViewHolder(itemView), MorphableMediaHolder {

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
	private var checkboxLeaving = false

	override val boundItemId: String?
		get() = boundMediaItem?.id

	override val previewView: View
		get() = ivImage.parent as View

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

	/** Row content that fades in after the preview morph (everything except the preview). */
	override fun getFadeableContentViews(): List<View> =
		listOf(tvTitle, tvDescription, selectionCheck, divider)
			.filter { it.visibility == View.VISIBLE }

	override fun getSelectionOverlayViews(): List<View> = emptyList()

	override fun beginMorph(standIn: Bitmap?, onPreviewArrived: (Bitmap) -> Unit) = previewDelegate.beginMorph(standIn, onPreviewArrived)

	override fun endMorph(revealed: Boolean) = previewDelegate.endMorph(revealed)

	fun updateDivider(showDivider: Boolean) {
		AndroidUiHelper.updateVisibility(divider, showDivider)
	}

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

	open fun bindView(
		mapActivity: FragmentActivity,
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

	open fun updateMetadata(galleryItem: GalleryItem.Media) {
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
				previewDelegate.showPhotoPreview(bitmap)
			}

			override fun onError() {
				// The delegate placeholder is already bound; nothing to do.
			}
		}, object : ImageRequestListener {
			override fun onSuccess(source: ImageLoadSource) {}
		}, previewSizePx)
	}

	open fun updateSelection(selectionMode: Boolean, selected: Boolean, nightMode: Boolean) {
		bindSelection(selectionMode, selected, nightMode, animate = GalleryMotion.animationsEnabled(app))
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

		val shown = selectionCheck.isVisible && !checkboxLeaving
		if (animate && selectionMode != shown) {
			if (selectionMode) animateCheckboxIn() else animateCheckboxOut()
		} else if (!animate) {
			resetSelectionAnimation()
			AndroidUiHelper.updateVisibility(selectionCheck, selectionMode)
		}
		selectionCheck.isChecked = selected
		UiUtilities.setupCompoundButton(nightMode, activeColor, selectionCheck)
	}

	private fun animateCheckboxIn() {
		checkboxLeaving = false
		val wasLaidOut = selectionCheck.isVisible
		selectionCheck.animate().cancel()
		selectionCheck.visibility = View.VISIBLE
		val row = selectionCheck.parent as ViewGroup
		row.doOnPreDraw {
			val shift = -selectionShift()
			if (!wasLaidOut) {
				selectionCheck.translationX = shift
				selectionCheck.alpha = 0f
				rowSiblings(row).forEach { it.translationX = shift }
			}
			selectionCheck.animate()
				.translationX(0f)
				.alpha(1f)
				.setDuration(CHECKBOX_ANIM_DURATION_MS)
				.setInterpolator(selectionInterpolator)
				.withEndAction(null)
				.start()
			rowSiblings(row).forEach { view ->
				view.animate()
					.translationX(0f)
					.setDuration(CHECKBOX_ANIM_DURATION_MS)
					.setInterpolator(selectionInterpolator)
					.start()
			}
		}
	}

	private fun animateCheckboxOut() {
		val shift = selectionShift()
		val row = selectionCheck.parent as ViewGroup
		checkboxLeaving = true
		selectionCheck.animate().cancel()
		selectionCheck.visibility = View.VISIBLE
		selectionCheck.animate()
			.translationX(-shift)
			.alpha(0f)
			.setDuration(CHECKBOX_ANIM_DURATION_MS)
			.setInterpolator(selectionInterpolator)
			.withEndAction {
				if (!checkboxLeaving) return@withEndAction
				checkboxLeaving = false
				selectionCheck.visibility = View.GONE
				selectionCheck.translationX = 0f
				selectionCheck.alpha = 1f
				row.doOnPreDraw { rowSiblings(row).forEach { it.translationX = 0f } }
			}
			.start()
		rowSiblings(row).forEach { view ->
			view.animate()
				.translationX(-shift)
				.setDuration(CHECKBOX_ANIM_DURATION_MS)
				.setInterpolator(selectionInterpolator)
				.start()
		}
	}

	private fun rowSiblings(row: ViewGroup): List<View> =
		(0 until row.childCount).map(row::getChildAt).filter { it !== selectionCheck }

	private fun selectionShift(): Float = (selectionCheck.width + selectionCheck.marginEnd).toFloat() *
		if (itemView.layoutDirection == View.LAYOUT_DIRECTION_RTL) -1 else 1

	private fun resetSelectionAnimation() {
		checkboxLeaving = false
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
		private const val CHECKBOX_ANIM_DURATION_MS = GalleryMotion.MOVE_DURATION_MS

		private val selectionInterpolator = GalleryMotion.CURVE
	}
}