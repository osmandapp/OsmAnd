package net.osmand.plus.gallery.ui.holders

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GallerySectionBoundary
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
	protected val app: OsmandApplication,
	itemView: View,
	private val mediaProvider: MediaProvider,
	private val onMediaItemClicked: (MediaItem) -> Unit,
	private val onMediaItemLongClicked: (MediaItem) -> Unit,
	private val onToggleSelection: (MediaItem) -> Unit,
	posterLoader: MediaPosterLoader? = null
) : RecyclerView.ViewHolder(itemView), MorphableMediaHolder {

	private val ivImage: ImageView = itemView.findViewById(R.id.image)
	protected val tvTitle: TextView = itemView.findViewById(R.id.title)
	protected val tvDescription: TextView = itemView.findViewById(R.id.description)
	protected val selectionCheck: CompoundButton = itemView.findViewById(R.id.selection_check)
	protected val divider: View = itemView.findViewById(R.id.divider)
	private val selectionTint = GradientDrawable()

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
	protected var nightMode = false
		private set
	private var selectionMode: Boolean = false
	private var checkboxState = CheckboxState.HIDDEN

	override val previewView: View
		get() = ivImage.parent as View

	override fun captureMorphState(): MorphState = previewDelegate.captureMorphState()

	override fun getFadeableContentViews(): List<MorphContent> =
		listOf(tvTitle, tvDescription, selectionCheck).filter { it.isVisible }.map { MorphContent(it, slides = true) } +
			listOf(divider).filter { it.isVisible }.map { MorphContent(it, slides = false) }

	override fun getSelectionOverlayViews(): List<View> = emptyList()

	override fun beginMorph(standIn: Bitmap?, onPreviewArrived: (Bitmap) -> Unit) = previewDelegate.beginMorph(standIn, onPreviewArrived)

	override fun endMorph(revealed: Boolean) = previewDelegate.endMorph(revealed)

	fun bindSection(boundary: GallerySectionBoundary?, cardRadius: Float) {
		AndroidUiHelper.updateVisibility(divider, boundary?.isLast == false)
		val top = if (boundary?.isFirst == true) cardRadius else 0f
		val bottom = if (boundary?.isLast == true) cardRadius else 0f
		selectionTint.cornerRadii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
	}

	init {
		itemView.background = selectionTint
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
		galleryItem: GalleryItem.Media,
		nightMode: Boolean,
		selectionMode: Boolean,
		selected: Boolean
	) {
		val mediaItem = galleryItem.mediaItem
		boundMediaItem = mediaItem
		this.nightMode = nightMode
		cancelLoadingImage()

		tvTitle.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode))
		tvTitle.text = mediaItem.title
		tvDescription.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
		bindDescription(galleryItem)
		bindPreview(galleryItem, nightMode)
		bindSelection(selectionMode, selected, nightMode, animate = false)
		onItemBound(galleryItem)
	}

	fun updateMetadata(galleryItem: GalleryItem.Media) {
		if (boundMediaItem?.id != galleryItem.mediaItem.id) return
		bindDescription(galleryItem)
		previewDelegate.updateDurationLabel(galleryItem.presentation?.durationLabel)
		onItemBound(galleryItem)
	}

	protected open fun onItemBound(galleryItem: GalleryItem.Media) {}

	protected open fun onSelectionModeBound(selectionMode: Boolean) {}

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

	fun updateSelection(selectionMode: Boolean, selected: Boolean, nightMode: Boolean) {
		bindSelection(selectionMode, selected, nightMode, animate = GalleryMotion.animationsEnabled(app))
	}

	private fun bindSelection(
		selectionMode: Boolean,
		selected: Boolean,
		nightMode: Boolean,
		animate: Boolean
	) {
		this.selectionMode = selectionMode
		onSelectionModeBound(selectionMode)
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		val bgColor = if (selectionMode && selected) {
			ColorUtilities.getColorWithAlpha(activeColor, ROW_SELECTED_ALPHA)
		} else {
			Color.TRANSPARENT
		}
		selectionTint.setColor(bgColor)

		val shown = checkboxState == CheckboxState.SHOWN || checkboxState == CheckboxState.SHOWING
		if (animate && selectionMode != shown) {
			if (selectionMode) animateCheckboxIn() else animateCheckboxOut()
		} else if (!animate) {
			resetSelectionAnimation()
			checkboxState = if (selectionMode) CheckboxState.SHOWN else CheckboxState.HIDDEN
			AndroidUiHelper.updateVisibility(selectionCheck, selectionMode)
		}
		selectionCheck.isChecked = selected
		UiUtilities.setupCompoundButton(nightMode, activeColor, selectionCheck)
	}

	private fun animateCheckboxIn() {
		val wasLaidOut = checkboxState == CheckboxState.HIDING
		checkboxState = CheckboxState.SHOWING
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
				.withEndAction { if (checkboxState == CheckboxState.SHOWING) checkboxState = CheckboxState.SHOWN }
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
		checkboxState = CheckboxState.HIDING
		selectionCheck.animate().cancel()
		selectionCheck.visibility = View.VISIBLE
		selectionCheck.animate()
			.translationX(-shift)
			.alpha(0f)
			.setDuration(CHECKBOX_ANIM_DURATION_MS)
			.setInterpolator(selectionInterpolator)
			.withEndAction {
				if (checkboxState != CheckboxState.HIDING) return@withEndAction
				checkboxState = CheckboxState.HIDDEN
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

	private enum class CheckboxState { HIDDEN, SHOWING, SHOWN, HIDING }

	companion object {
		private const val ROW_SELECTED_ALPHA = 0.2f
		private const val CHECKBOX_ANIM_DURATION_MS = GalleryMotion.MOVE_DURATION_MS

		private val selectionInterpolator = GalleryMotion.CURVE
	}
}