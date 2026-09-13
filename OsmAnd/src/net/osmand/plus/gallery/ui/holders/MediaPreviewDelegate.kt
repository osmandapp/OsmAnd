package net.osmand.plus.gallery.ui.holders

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
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
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import kotlin.math.roundToInt

class MediaPreviewDelegate(
	private val app: OsmandApplication,
	private val imageView: ImageView,
	private val videoScrim: View? = null,
	private val playIcon: ImageView? = null,
	private val durationText: TextView? = null,
	var large: Boolean = false,
	private val posterLoader: MediaPosterLoader? = null,
	private val onPreviewShown: (() -> Unit)? = null
) {

	var showsPreviewBitmap = false
		private set

	var placeholderBgColor = 0
		private set

	private var generation = 0
	private var boundType: MediaType? = null
	private var durationLabel: String? = null
	private var nightMode = false

	private var preview: Bitmap? = null
	private var standIn: Bitmap? = null
	private var pendingPoster: Bitmap? = null
	private var morphing = false
	private var onPreviewArrived: ((Bitmap) -> Unit)? = null
	private var crossFade: ValueAnimator? = null

	val morphPreviewSnapshotView: View?
		get() = imageView.takeIf { showsPreviewBitmap && it.isVisible }

	val morphPreviewBitmap: Bitmap?
		get() = preview ?: standIn

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
		endCrossFade()
		preview = null
		standIn = null
		pendingPoster = null
		morphing = false
		onPreviewArrived = null
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
		morphing = false
		onPreviewArrived = null
	}

	fun showPhotoPreview(bitmap: Bitmap) {
		preview = bitmap
		if (morphing) {
			onPreviewArrived?.invoke(bitmap)
			return
		}
		showPreview(bitmap, crossFadeFrom = standIn)
		standIn = null
	}

	fun beginMorph(standIn: Bitmap?, onPreviewArrived: (Bitmap) -> Unit) {
		morphing = true
		this.onPreviewArrived = onPreviewArrived
		preview?.let {
			onPreviewArrived(it)
			return
		}
		if (standIn != null && !showsPreviewBitmap) {
			this.standIn = standIn
			showPreview(standIn, crossFadeFrom = null)
		}
	}

	fun endMorph(revealed: Boolean) {
		if (!morphing) return
		morphing = false
		onPreviewArrived = null
		val poster = pendingPoster
		pendingPoster = null
		val arrived = preview ?: return
		val crossFadeFrom = if (revealed) null else standIn
		if (poster != null) {
			applyVideoPoster(poster, crossFadeFrom)
		} else if ((imageView.drawable as? BitmapDrawable)?.bitmap !== arrived) {
			showPreview(arrived, crossFadeFrom)
		}
		standIn = null
	}

	fun updateDurationLabel(label: String?) {
		durationLabel = label
		applyDurationLabel()
	}

	private fun showPreview(bitmap: Bitmap, crossFadeFrom: Bitmap?) {
		endCrossFade()
		val wasPreview = showsPreviewBitmap
		showsPreviewBitmap = true
		hideVideoChrome()
		imageView.scaleType = ImageView.ScaleType.CENTER_CROP
		if (crossFadeFrom != null && crossFadeFrom !== bitmap && GalleryMotion.animationsEnabled(app)) {
			val drawable = CrossFadeDrawable(crossFadeFrom, bitmap)
			imageView.setImageDrawable(drawable)
			crossFade = ValueAnimator.ofFloat(0f, 1f).apply {
				duration = GalleryMotion.FADE_DURATION_MS
				interpolator = GalleryMotion.CURVE
				addUpdateListener { drawable.progress = it.animatedFraction }
				addListener(object : AnimatorListenerAdapter() {
					override fun onAnimationEnd(animation: Animator) {
						if (crossFade === animation) {
							crossFade = null
							if (imageView.drawable === drawable) imageView.setImageDrawable(bitmap.toDrawable(imageView.resources))
						}
					}
				})
				start()
			}
		} else {
			imageView.setImageDrawable(bitmap.toDrawable(imageView.resources))
		}
		if (!wasPreview) onPreviewShown?.invoke()
	}

	private fun endCrossFade() {
		crossFade?.let { animator ->
			crossFade = null
			animator.cancel()
			(imageView.drawable as? CrossFadeDrawable)?.let { imageView.setImageDrawable(it.to.toDrawable(imageView.resources)) }
		}
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
		preview = poster
		if (morphing) {
			pendingPoster = poster
			onPreviewArrived?.invoke(poster)
			return
		}
		applyVideoPoster(poster, crossFadeFrom = standIn)
		standIn = null
	}

	private fun applyVideoPoster(poster: Bitmap, crossFadeFrom: Bitmap?) {
		showPreview(poster, crossFadeFrom)
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

	private class CrossFadeDrawable(private val from: Bitmap, val to: Bitmap) : Drawable() {
		private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

		var progress = 0f
			set(value) {
				field = value
				invalidateSelf()
			}

		override fun getIntrinsicWidth(): Int = to.width

		override fun getIntrinsicHeight(): Int = to.height

		override fun draw(canvas: Canvas) {
			val bounds: Rect = bounds
			if (progress < 1f) {
				paint.alpha = 255
				canvas.drawBitmap(from, null, bounds, paint)
			}
			paint.alpha = (progress * 255f).roundToInt().coerceIn(0, 255)
			canvas.drawBitmap(to, null, bounds, paint)
		}

		override fun setAlpha(alpha: Int) {}

		override fun setColorFilter(colorFilter: ColorFilter?) {}

		@Deprecated("Deprecated in Java")
		override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
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
