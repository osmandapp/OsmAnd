package net.osmand.plus.gallery.ui

import net.osmand.plus.OsmandApplication

object GalleryGridSettings {

	@JvmStatic
	fun getSpanCount(app: OsmandApplication, isPortrait: Boolean): Int {
		return if (isPortrait) {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT.get()
		} else {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.get()
		}
	}

	@JvmStatic
	fun setSpanCount(app: OsmandApplication, isPortrait: Boolean, spanCount: Int) {
		if (isPortrait) {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT.set(spanCount)
		} else {
			app.settings.CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE.set(spanCount)
		}
	}
}