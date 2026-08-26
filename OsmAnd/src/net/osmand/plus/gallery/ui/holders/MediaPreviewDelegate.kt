package net.osmand.plus.gallery.ui.holders

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType

class MediaPreviewDelegate(
	private val app: OsmandApplication,
	private val imageView: ImageView,
	private val videoScrim: View? = null,
	private val playIcon: ImageView? = null,
	private val durationText: TextView? = null,
	var large: Boolean = false,
	private val posterLoader: MediaPosterLoader? = null
) {

	var showsPreviewBitmap = false
		private set

	var placeholderBgColor = 0
		private set

	private var generation = 0
	private var boundType: MediaType? = null
	private var durationLabel: String? = null
	private var nightMode = false

	val morphPreviewSnapshotView: View?
		get() = imageView.takeIf { showsPreviewBitmap && it.isVisible }

	val morphCenterIcon: Drawable?
		get() = when {
			!showsPreviewBitmap && imageView.isVisible -> imageView.drawable
			playIcon?.isVisible == true -> playIcon.drawable
			else -> null
		}

	val morphShowsScrim: Boolean
		get() = videoScrim?.isVisible == true

	val morphDurationLabel: String?
		get() = durationLabel

	val morphShowsDuration: Boolean
		get() = durationText?.isVisible == true

	val morphDurationTextColor: Int
		get() = getDurationTextColor()

	fun bind(
		item: MediaItem,
		nightMode: Boolean,
		durationLabel: String?,
		onPosterShown: (() -> Unit)? = null
	) {
		generation++
		val gen = generation
		this.nightMode = nightMode
		this.durationLabel = durationLabel
		boundType = item.type
		bindPlaceholder(item.type, nightMode)
		when (item.type) {
			MediaType.VIDEO -> posterLoader?.loadPoster(item) { poster ->
				if (gen == generation && poster != null) {
					showVideoPoster(poster)
					onPosterShown?.invoke()
				}
			}
			MediaType.AUDIO -> applyDurationLabel()
			else -> {}
		}
	}

	fun cancel() {
		generation++
	}

	fun onPhotoPreviewShown() {
		showsPreviewBitmap = true
		hideVideoChrome()
	}

	fun updateDurationLabel(label: String?) {
		durationLabel = label
		applyDurationLabel()
	}

	private fun bindPlaceholder(type: MediaType, nightMode: Boolean) {
		showsPreviewBitmap = false
		placeholderBgColor = ColorUtilities.getActivityBgColor(app, nightMode)
		imageView.visibility = View.VISIBLE
		imageView.setBackgroundColor(placeholderBgColor)
		imageView.scaleType = ImageView.ScaleType.CENTER
		imageView.setImageDrawable(
			app.uiUtilities.getIcon(
				getPlaceholderIconId(type),
				ColorUtilities.getDefaultIconColorId(nightMode)
			)
		)
		hideVideoChrome()
	}

	private fun showVideoPoster(poster: Bitmap) {
		showsPreviewBitmap = true
		imageView.scaleType = ImageView.ScaleType.CENTER_CROP
		imageView.setImageDrawable(poster.toDrawable(imageView.resources))
		AndroidUiHelper.updateVisibility(videoScrim, true)
		playIcon?.let {
			it.setImageDrawable(app.uiUtilities.getIcon(R.drawable.ic_action_play_in_shape))
			AndroidUiHelper.updateVisibility(it, true)
		}
		applyDurationLabel()
	}

	private fun applyDurationLabel() {
		val text = durationText ?: return
		val label = durationLabel
		val visible = large && label != null && when (boundType) {
			MediaType.AUDIO -> true
			MediaType.VIDEO -> showsPreviewBitmap
			else -> false
		}
		if (visible) {
			text.text = label
			text.setTextColor(getDurationTextColor())
			updateDurationPosition(text)
		} else {
			text.translationY = 0f
		}
		AndroidUiHelper.updateVisibility(text, visible)
	}

	private fun getDurationTextColor(): Int =
		if (boundType == MediaType.VIDEO) {
			ContextCompat.getColor(app, R.color.active_buttons_and_links_text_light)
		} else {
			ColorUtilities.getSecondaryTextColor(app, nightMode)
		}

	private fun updateDurationPosition(text: TextView) {
		val iconSize = app.resources.getDimensionPixelSize(R.dimen.standard_icon_size)
		val gap = app.resources.displayMetrics.density * DURATION_GAP_DP
		text.translationY = iconSize / 2f + gap + text.lineHeight / 2f
	}

	private fun hideVideoChrome() {
		AndroidUiHelper.updateVisibility(videoScrim, false)
		AndroidUiHelper.updateVisibility(playIcon, false)
		AndroidUiHelper.updateVisibility(durationText, false)
	}

	companion object {
		private const val DURATION_GAP_DP = 2f

		fun getPlaceholderIconId(type: MediaType): Int = when (type) {
			MediaType.VIDEO -> R.drawable.ic_type_video
			MediaType.AUDIO -> R.drawable.ic_action_music_note
			else -> R.drawable.ic_action_photo
		}
	}
}
