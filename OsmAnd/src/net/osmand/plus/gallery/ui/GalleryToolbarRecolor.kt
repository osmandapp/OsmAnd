package net.osmand.plus.gallery.ui

import android.animation.ValueAnimator
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.ui.motion.GalleryMotion

class GalleryToolbarRecolor(private val app: OsmandApplication) {
	private var color: Int? = null
	private var animator: ValueAnimator? = null

	fun recolor(to: Int, apply: (Int) -> Unit) {
		cancel()
		val from = color
		color = to
		animator = GalleryMotion.recolor(app, from ?: to, to, animate = from != null, apply)
	}

	fun cancel() {
		animator?.cancel()
		animator = null
	}
}
