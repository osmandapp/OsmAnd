package net.osmand.plus.gallery.ui.motion

import android.view.animation.PathInterpolator
import net.osmand.plus.OsmandApplication

object GalleryMotion {
	val CURVE = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

	const val MOVE_DURATION_MS = 300L
	const val FADE_DURATION_MS = 150L
	const val STAGGER_STEP_MS = 15L
	const val MAX_STAGGER_MS = 120L
	const val APPEAR_SCALE = 0.92f

	fun stagger(index: Int): Long = (index * STAGGER_STEP_MS).coerceAtMost(MAX_STAGGER_MS)

	fun progress(elapsed: Long, delay: Long, duration: Long): Float {
		if (duration <= 0L) return 1f
		val linear = ((elapsed - delay).toFloat() / duration).coerceIn(0f, 1f)
		return CURVE.getInterpolation(linear)
	}

	fun animationsEnabled(app: OsmandApplication): Boolean = !app.settings.DO_NOT_USE_ANIMATIONS.get()
}
