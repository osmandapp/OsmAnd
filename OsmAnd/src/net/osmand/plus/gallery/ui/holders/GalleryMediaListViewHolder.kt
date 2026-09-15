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
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GallerySectionBoundary
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils
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
import kotlin.math.roundToInt

class GalleryMediaListViewHolder(
	private val app: OsmandApplication,
	itemView: View,
	private val mediaProvider: MediaProvider,
	private val onMediaItemClicked: (MediaItem) -> Unit,
	private val onMediaItemLongClicked: (MediaItem) -> Unit,
	private val onToggleSelection: (MediaItem) -> Unit,
	posterLoader: MediaPosterLoader? = null,
	private val onMenuClicked: ((MediaItem, View) -> Unit)? = null
) : RecyclerView.ViewHolder(itemView), MorphableMediaHolder {

	private val ivImage: ImageView = itemView.findViewById(R.id.image)
	private val tvTitle: TextView = itemView.findViewById(R.id.title)
	private val tvDescription: TextView = itemView.findViewById(R.id.description)
	private val selectionCheck: CompoundButton = itemView.findViewById(R.id.selection_check)
	private val divider: View = itemView.findViewById(R.id.divider)
	private val rowContainer: View? = itemView.findViewById(R.id.row_container)
	private val overflowButton: ImageView? = itemView.findViewById(R.id.overflow_button)
	private val attachmentContainer: View? = itemView.findViewById(R.id.attachment_container)
	private val attachmentIcon: ImageView? = itemView.findViewById(R.id.attachment_icon)
	private val attachmentName: TextView? = itemView.findViewById(R.id.attachment_name)
	private val attachmentCount: TextView? = itemView.findViewById(R.id.attachment_count)
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
	private var nightMode = false
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

	override fun getFadeableContentViews(): List<View> =
		listOfNotNull(tvTitle, tvDescription, selectionCheck, divider, overflowButton, attachmentContainer).filter { it.isVisible }

	override fun getSelectionOverlayViews(): List<View> = emptyList()

	override fun beginMorph(standIn: Bitmap?, onPreviewArrived: (Bitmap) -> Unit) = previewDelegate.beginMorph(standIn, onPreviewArrived)

	override fun endMorph(revealed: Boolean) = previewDelegate.endMorph(revealed)

	fun bindSection(boundary: GallerySectionBoundary?, cardRadius: Float) {
		AndroidUiHelper.updateVisibility(divider, boundary?.isLast == false)
		val top = if (boundary?.roundTopCorners == true) cardRadius else 0f
		val bottom = if (boundary?.roundBottomCorners == true) cardRadius else 0f
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
		mapActivity: FragmentActivity,
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
		bindAttachment(galleryItem)
		bindPreview(galleryItem, nightMode)
		bindSelection(selectionMode, selected, nightMode, animate = false)
		overflowButton?.apply {
			setImageDrawable(app.uiUtilities.getPaintedIcon(R.drawable.ic_overflow_menu_white, ColorUtilities.getDefaultIconColor(app, nightMode)))
			setOnClickListener { onMenuClicked?.invoke(mediaItem, it) }
		}
	}

	fun updateMetadata(galleryItem: GalleryItem.Media) {
		if (boundMediaItem?.id != galleryItem.mediaItem.id) return
		bindDescription(galleryItem)
		bindAttachment(galleryItem)
		previewDelegate.updateDurationLabel(galleryItem.presentation?.durationLabel)
	}

	private fun bindAttachment(galleryItem: GalleryItem.Media) {
		val container = attachmentContainer ?: return
		val line = galleryItem.presentation?.attachment
		container.isVisible = line != null
		rowContainer?.minimumHeight = AndroidUtils.dpToPxF(app, if (line == null) ROW_MIN_HEIGHT_DP else ATTACHED_ROW_MIN_HEIGHT_DP).roundToInt()
		attachmentIcon?.setImageDrawable(line?.iconDrawable)
		attachmentName?.apply {
			text = line?.name
			setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
		}
		attachmentCount?.apply {
			isVisible = line != null && line.extraCount > 0
			text = line?.let { app.getString(R.string.ltr_or_rtl_combine_via_pipe_plus, "", it.extraCount) }
			setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
		}
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
		overflowButton?.visibility = if (selectionMode) View.INVISIBLE else View.VISIBLE
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		val bgColor = if (selectionMode && selected) {
			ColorUtilities.getColorWithAlpha(activeColor, ROW_SELECTED_ALPHA)
		} else {
			Color.TRANSPARENT
		}
		selectionTint.setColor(bgColor)

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
		private const val ROW_MIN_HEIGHT_DP = 68f
		private const val ATTACHED_ROW_MIN_HEIGHT_DP = 100f
		private const val ROW_SELECTED_ALPHA = 0.2f
		private const val CHECKBOX_ANIM_DURATION_MS = GalleryMotion.MOVE_DURATION_MS

		private val selectionInterpolator = GalleryMotion.CURVE
	}
}