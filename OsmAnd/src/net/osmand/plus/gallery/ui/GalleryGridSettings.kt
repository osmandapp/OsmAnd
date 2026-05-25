package net.osmand.plus.gallery.ui

import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.AndroidUiHelper

object GalleryGridSettings {

	fun getSpanCount(mapActivity: MapActivity): Int {
		val app = mapActivity.app
		return if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT.get()
		} else {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.get()
		}
	}

	fun setSpanCount(mapActivity: MapActivity, spanCount: Int) {
		val app = mapActivity.app
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT.set(spanCount)
		} else {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.set(spanCount)
		}
	}
}