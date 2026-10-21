package net.osmand.plus.gallery.ui.motion

import android.animation.ValueAnimator
import android.graphics.RectF
import android.view.animation.PathInterpolator
import net.osmand.plus.OsmandApplication
import kotlin.math.max
import kotlin.math.min

data class OpenEdges(val top: Boolean, val bottom: Boolean) {
	companion object {
		val CLOSED = OpenEdges(top = false, bottom = false)
	}
}

object GalleryMotion {
	val CURVE = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

	const val MOVE_DURATION_MS = 300L
	const val FADE_DURATION_MS = 150L
	const val RECOLOR_DURATION_MS = 200L
	const val STAGGER_STEP_MS = 15L
	const val MAX_STAGGER_MS = 120L
	const val CONTENT_FADE_BASE_DELAY_MS = 80L
	const val CROSSFADE_MIN_MS = 100L
	const val APPEAR_SCALE = 0.92f

	fun stagger(index: Int): Long = (index * STAGGER_STEP_MS).coerceAtMost(MAX_STAGGER_MS)

	fun progress(elapsed: Long, delay: Long, duration: Long): Float {
		if (duration <= 0L) return 1f
		val linear = ((elapsed - delay).toFloat() / duration).coerceIn(0f, 1f)
		return CURVE.getInterpolation(linear)
	}

	fun animationsEnabled(app: OsmandApplication): Boolean = !app.settings.DO_NOT_USE_ANIMATIONS.get()

	fun extendOffscreen(rect: RectF, open: OpenEdges, viewportTop: Float, viewportBottom: Float, radius: Float) {
		if (open.top) rect.top = min(rect.top, viewportTop - radius)
		if (open.bottom) rect.bottom = max(rect.bottom, viewportBottom + radius)
	}

	fun recolor(app: OsmandApplication, from: Int, to: Int, animate: Boolean, apply: (Int) -> Unit): ValueAnimator? {
		if (!animate || !animationsEnabled(app) || from == to) {
			apply(to)
			return null
		}
		return ValueAnimator.ofArgb(from, to).apply {
			duration = RECOLOR_DURATION_MS
			interpolator = CURVE
			addUpdateListener { apply(it.animatedValue as Int) }
			start()
		}
	}
}
