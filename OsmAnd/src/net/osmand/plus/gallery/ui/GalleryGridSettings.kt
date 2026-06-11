package net.osmand.plus.gallery.ui

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.helpers.AndroidUiHelper

object GalleryGridSettings {

	@JvmStatic
	fun getSpanCount(activity: FragmentActivity): Int {
		val app = activity.applicationContext as OsmandApplication
		return if (AndroidUiHelper.isOrientationPortrait(activity)) {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT.get()
		} else {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.get()
		}
	}

	@JvmStatic
	fun setSpanCount(activity: FragmentActivity, spanCount: Int) {
		val app = activity.applicationContext as OsmandApplication
		if (AndroidUiHelper.isOrientationPortrait(activity)) {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT.set(spanCount)
		} else {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.set(spanCount)
		}
	}
}