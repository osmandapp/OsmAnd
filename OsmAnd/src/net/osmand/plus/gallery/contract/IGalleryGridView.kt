package net.osmand.plus.gallery.contract

import androidx.fragment.app.FragmentActivity

interface IGalleryGridView {
	fun getActivity(): FragmentActivity?
	fun isNightMode(): Boolean
	fun isPortrait(): Boolean

	fun updateDisplayMode()
	fun updateToolbar()
	fun updateItems()
	fun updateSelection()
}