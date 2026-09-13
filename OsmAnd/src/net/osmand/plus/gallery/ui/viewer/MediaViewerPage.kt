package net.osmand.plus.gallery.ui.viewer

import android.graphics.RectF

interface MediaViewerPage {
	fun contentRect(): RectF?

	fun canScrollVertically(direction: Int): Boolean

	fun onPreviewSettled()
}
