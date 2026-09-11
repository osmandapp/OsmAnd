package net.osmand.plus.gallery.contract

import net.osmand.plus.activities.MapActivity
import androidx.fragment.app.FragmentActivity

interface IGalleryGridView {
	fun getActivity(): FragmentActivity?
	fun getMapActivity(): MapActivity? = getActivity() as? MapActivity
	fun updateSections() = updateItems()
	fun isNightMode(): Boolean
	fun isPortrait(): Boolean

	fun updateDisplayMode()
	fun updateToolbar()
	fun updateItems()
	fun updateSelection()
}