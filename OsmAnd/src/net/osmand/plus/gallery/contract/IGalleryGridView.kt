package net.osmand.plus.gallery.contract

import net.osmand.plus.activities.MapActivity

interface IGalleryGridView {
	fun getMapActivity(): MapActivity?
	fun isNightMode(): Boolean
	fun isPortrait(): Boolean

	fun updateSpan()
	fun updateDisplayMode()
	fun updateToolbar()
	fun updateItems()
	fun updateSelection()
}